package io.github.supermonster003.autojs6.plugin.angus.mail.core.error

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.FolderClosedException
import jakarta.mail.FolderNotFoundException
import jakarta.mail.MessagingException
import jakarta.mail.SendFailedException
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import org.eclipse.angus.mail.iap.ProtocolException
import org.eclipse.angus.mail.util.MailConnectException
import org.eclipse.angus.mail.util.SocketConnectException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Base64
import javax.net.ssl.SSLHandshakeException

class ExceptionMapperTest {

    private val secret = "hunter2-authorization-code"
    private val redactor = Redactor("alice@example.org", MailSecret(secret))
    private val mapper = ExceptionMapper(redactor)

    @Test
    fun authenticationFailuresAreNotRetryableAndCarryNoSecret() {
        val error = mapper.map(AuthenticationFailedException("[AUTHENTICATIONFAILED] Invalid credentials for $secret"), "imap connect")
        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertFalse(error.retryable)
        assertEquals("imap connect: authentication failed", error.message)
        assertEquals("[AUTHENTICATIONFAILED] Invalid credentials for ***", error.details)
    }

    @Test
    fun aServerWithoutAMechanismForThePasswordIsNotAServerError() {
        // Angus IMAPStore against Outlook.com (LOGINDISABLED, AUTH=XOAUTH2 only) with a password, before any credential is sent.
        val error = mapper.map(MessagingException("No login methods supported!", ProtocolException("No login methods supported!")), "imap connect")
        assertEquals(MailErrorCode.AUTH_MECHANISM_UNSUPPORTED, error.code)
        assertFalse(error.retryable)
        assertTrue(error.details.orEmpty(), error.details.orEmpty().contains("xoauth2"))
    }

    @Test
    fun timeoutsWinOverConnectFailures() {
        val timeout = MessagingException("Couldn't connect", MailConnectException(SocketConnectException("Connection timed out", SocketTimeoutException("connect timed out"), "127.0.0.1", 993, 1000)))
        assertEquals(MailErrorCode.TIMEOUT, mapper.map(timeout).code)
        assertTrue(mapper.map(timeout).retryable)
        val refused = MessagingException("Couldn't connect", MailConnectException(SocketConnectException("Connection refused", ConnectException("Connection refused"), "127.0.0.1", 993, 1000)))
        assertEquals(MailErrorCode.CONNECT_FAILED, mapper.map(refused).code)
        assertTrue(mapper.map(refused).retryable)
        assertEquals(MailErrorCode.CONNECT_FAILED, mapper.map(MessagingException("x", UnknownHostException("imap.nowhere.invalid"))).code)
    }

    @Test
    fun tlsProblemsAndMissingStarttlsMapToTlsFailed() {
        assertEquals(MailErrorCode.TLS_FAILED, mapper.map(MessagingException("Could not convert socket", SSLHandshakeException("bad cert"))).code)
        val starttls = mapper.map(MessagingException("STARTTLS is required but host does not support STARTTLS"))
        assertEquals(MailErrorCode.TLS_FAILED, starttls.code)
        assertFalse(starttls.retryable)
        assertEquals(MailErrorCode.TLS_FAILED, mapper.map(MessagingException("STLS command not supported")).code)
        // Angus's POP3 store wraps the EOF of a missing STLS upgrade and of a timed-out greeting in AuthenticationFailedException.
        val pop3Stls = mapper.map(AuthenticationFailedException("STLS required but not supported"))
        assertEquals(MailErrorCode.TLS_FAILED, pop3Stls.code)
        assertFalse(pop3Stls.retryable)
        val pop3Timeout = mapper.map(AuthenticationFailedException("Read timed out"))
        assertEquals(MailErrorCode.TIMEOUT, pop3Timeout.code)
        assertTrue(pop3Timeout.retryable)
        assertEquals(MailErrorCode.AUTH_FAILED, mapper.map(AuthenticationFailedException("[AUTH] Invalid credentials")).code)
    }

    @Test
    fun folderAndMessageProblemsKeepTheirCodes() {
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, mapper.map(FolderNotFoundException()).code)
        assertEquals(MailErrorCode.CONNECT_FAILED, mapper.map(FolderClosedException(null, "* BYE")).code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, mapper.map(AddressException("Missing domain", "bob")).code)
        assertEquals(MailErrorCode.IO_FAILED, mapper.map(IOException("disk full")).code)
        val popDisabled = mapper.map(MessagingException("Open failed", IOException("STAT command failed: [SYS/PERM] Your account is not enabled for POP access. Please visit your Gmail settings page and enable your account for POP access.")))
        assertEquals("Gmail with POP disabled (real account, 2026-09-19)", MailErrorCode.UNSUPPORTED_OPERATION, popDisabled.code)
        assertFalse(popDisabled.retryable)
        assertTrue(popDisabled.message, popDisabled.message.contains("POP access is not enabled"))
        assertEquals(MailErrorCode.CANCELLED, mapper.map(InterruptedException()).code)
        assertEquals(MailErrorCode.SERVER_ERROR, mapper.map(MessagingException("NO [CANNOT] ...")).code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, mapper.map(IllegalArgumentException("limit")).code)
        assertEquals(MailErrorCode.INTERNAL, mapper.map(IllegalStateException("bug")).code)
        assertEquals(MailErrorCode.INTERNAL, mapper.map(NullPointerException()).code)
    }

    @Test
    fun sendFailuresListTheAddresses() {
        val failure = SendFailedException("Invalid Addresses", null, null, arrayOf(InternetAddress("ok@example.org")), arrayOf(InternetAddress("bad@example.org")))
        val error = mapper.map(failure, "smtp send")
        assertEquals(MailErrorCode.SEND_REJECTED, error.code)
        assertFalse(error.retryable)
        assertTrue(error.details!!.startsWith("rejected: bad@example.org; not sent: ok@example.org"))
    }

    @Test
    fun mailExceptionsPassThroughUntouched() {
        val original = MailException.invalidArgument("bad")
        assertTrue(original === mapper.map(original))
    }

    @Test
    fun secretsAreMaskedInEveryEncodedForm() {
        val encoder = Base64.getEncoder()
        val plain = encoder.encodeToString("\u0000alice@example.org\u0000$secret".toByteArray())
        val xoauth2 = encoder.encodeToString("user=alice@example.org\u0001auth=Bearer $secret\u0001\u0001".toByteArray())
        val scrubbed = redactor.scrub("LOGIN $secret / AUTH PLAIN $plain / AUTHENTICATE XOAUTH2 $xoauth2 / raw ${encoder.encodeToString(secret.toByteArray())}")
        assertFalse(scrubbed.contains(secret))
        assertFalse(scrubbed.contains(plain))
        assertFalse(scrubbed.contains(xoauth2))
        assertEquals("LOGIN *** / AUTH PLAIN *** / AUTHENTICATE XOAUTH2 *** / raw ***", scrubbed)
        assertEquals("<abc@example.org> ok", redactor.scrub("<abc@example.org> ok"))
        assertEquals("blob *** end", redactor.scrub("blob ${"Q".repeat(40)}== end"))
        assertNull(redactor.scrubOrNull(null))
    }

    @Test
    fun longMessagesAreTruncatedOnACharacterBoundary() {
        val text = "汉字".repeat(MailLimits.MAX_ERROR_MESSAGE_BYTES)
        val scrubbed = redactor.scrub(text)
        assertTrue(scrubbed.toByteArray(Charsets.UTF_8).size <= MailLimits.MAX_ERROR_MESSAGE_BYTES)
        assertTrue(scrubbed.endsWith("..."))
        assertTrue(scrubbed.dropLast(3).all { it == '汉' || it == '字' })
    }

    @Test
    fun emptySecretsMaskNothing() {
        val open = Redactor("alice", null)
        assertEquals("hello world", open.scrub("hello world"))
        assertEquals("hello world", Redactor("alice", MailSecret("")).scrub("hello world"))
    }
}
