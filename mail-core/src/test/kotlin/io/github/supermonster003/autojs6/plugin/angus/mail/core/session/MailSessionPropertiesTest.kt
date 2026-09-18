package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MailSessionPropertiesTest {

    private val account = MailAccount(
        address = "me@example.com",
        imap = MailEndpoint("imap.example.com", 993, TlsMode.SSL),
        pop3 = MailEndpoint("pop.example.com", 110, TlsMode.STARTTLS),
        smtp = MailEndpoint("smtp.example.com", 25, TlsMode.NONE),
        timeouts = MailTimeouts.uniform(12_345),
    )

    @Test
    fun implicitTlsUsesTheSecureProviderAndVerifiesTheServer() {
        val properties = MailSessionProperties.build(account, MailProtocol.IMAP)
        assertEquals("imaps", properties.getProperty("mail.store.protocol"))
        assertEquals("imap.example.com", properties.getProperty("mail.imaps.host"))
        assertEquals("993", properties.getProperty("mail.imaps.port"))
        assertEquals("true", properties.getProperty("mail.imaps.ssl.enable"))
        assertEquals("true", properties.getProperty("mail.imaps.ssl.checkserveridentity"))
        assertNull(properties.getProperty("mail.imaps.ssl.trust"))
        assertEquals("LOGIN PLAIN", properties.getProperty("mail.imaps.auth.mechanisms"))
        assertEquals("true", properties.getProperty("mail.imaps.peek"))
        assertEquals("12345", properties.getProperty("mail.imaps.connectiontimeout"))
        assertEquals("12345", properties.getProperty("mail.imaps.timeout"))
        assertEquals("12345", properties.getProperty("mail.imaps.writetimeout"))
        assertNull(properties.getProperty("mail.transport.protocol"))
    }

    @Test
    fun starttlsIsRequiredNotOpportunistic() {
        val properties = MailSessionProperties.build(account, MailProtocol.POP3)
        assertEquals("pop3", properties.getProperty("mail.store.protocol"))
        assertEquals("false", properties.getProperty("mail.pop3.ssl.enable"))
        assertEquals("true", properties.getProperty("mail.pop3.starttls.enable"))
        assertEquals("true", properties.getProperty("mail.pop3.starttls.required"))
        assertEquals("true", properties.getProperty("mail.pop3.ssl.checkserveridentity"))
    }

    @Test
    fun plainEndpointsDisableEveryTlsSwitch() {
        val properties = MailSessionProperties.build(account, MailProtocol.SMTP)
        assertEquals("smtp", properties.getProperty("mail.transport.protocol"))
        assertEquals("false", properties.getProperty("mail.smtp.ssl.enable"))
        assertEquals("false", properties.getProperty("mail.smtp.starttls.enable"))
        assertNull(properties.getProperty("mail.smtp.ssl.trust"))
        assertEquals("true", properties.getProperty("mail.smtp.auth"))
        assertEquals("me@example.com", properties.getProperty("mail.smtp.from"))
        assertTrue(account.insecure)
    }

    @Test
    fun trustAllOnlyRelaxesEncryptedEndpoints() {
        val trusting = account.copy(trustAll = true)
        val imap = MailSessionProperties.build(trusting, MailProtocol.IMAP)
        assertEquals("*", imap.getProperty("mail.imaps.ssl.trust"))
        assertEquals("false", imap.getProperty("mail.imaps.ssl.checkserveridentity"))
        val smtp = MailSessionProperties.build(trusting, MailProtocol.SMTP)
        assertNull(smtp.getProperty("mail.smtp.ssl.trust"))
        assertTrue(trusting.insecure)
        assertFalse(
            MailAccount("me@example.com", imap = MailEndpoint("imap.example.com", 993), smtp = MailEndpoint("smtp.example.com", 465)).insecure,
        )
    }

    @Test
    fun xoauth2SelectsOnlyThatMechanism() {
        val properties = MailSessionProperties.build(account.copy(auth = AuthMethod.XOAUTH2), MailProtocol.IMAP)
        assertEquals("XOAUTH2", properties.getProperty("mail.imaps.auth.mechanisms"))
    }

    @Test
    fun sessionPropertiesNeverCarryTheSecret() {
        val secret = MailSecret("hunter2-authorization-code")
        val session = MailSessionFactory.session(account, MailProtocol.SMTP, secret)
        assertTrue(session.properties.values.none { it.toString().contains("hunter2") })
        assertFalse(session.debug)
        assertEquals("MailSecret(***)", secret.toString())
        secret.clear()
        assertEquals("                          ", secret.reveal())
    }

    @Test
    fun missingEndpointsFailEarly() {
        try {
            MailSessionProperties.build(MailAccount("me@example.com"), MailProtocol.IMAP)
            throw AssertionError("an account without an IMAP endpoint must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("imap"))
        }
    }
}
