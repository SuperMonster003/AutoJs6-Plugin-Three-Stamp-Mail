package io.github.supermonster003.autojs6.plugin.angus.mail.core.error

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSessionFactory
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSessionProperties
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ProtocolTrace
import jakarta.mail.AuthenticationFailedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Secret audit of the plugin (mail roadmap P6): the sources of the mail core and of the app carry
 * no logging, no console output and no Jakarta debug switch; the Jakarta session never debugs
 * even when the account asks for the redacted trace (D28); the value objects, the exception
 * mapper, the options parser and the protocol trace never print the secret in any of the forms
 * it travels in. `docs/dev/p6-secret-audit.md` records the manual part of the audit.
 */
class SecretAuditTest {

    private val secretText = "Sw0rdf1sh-Authorization-Code"

    private companion object {
        /** The application files whose `Log` lines are state lines (watch ids, states, counts, durations), reviewed here. */
        val STATE_LOGGERS = setOf(
            "WatchKeeper.kt", "MailWatchService.kt", "HostTriggerSender.kt", "BootReceiver.kt",
            "AccountSecrets.kt", "OAuthSignInActivity.kt", "TokenRevoker.kt",
        )
        val STATE_LOG_CALL = Regex("""\b(Log\.[iw]\(|Log\.println\(|import android\.util\.Log|log\(Log\.(INFO|WARN),)""")
        val INTERPOLATION = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_.]*""")
        val SECRET_WORDS = Regex("""(?i)secret|password|token|verifier|(?<![.\w])code\b|address|subject|body|email|refresh[A-Z]|access[A-Z]|chars|grant\b""")
    }
    private val accountJson = """{"provider":"qq","address":"alice@qq.com","debug":true}"""

    @Test
    fun theSourcesCarryNoLoggingNoConsoleOutputAndNoJakartaDebugSwitch() {
        val roots = listOf(File("src/main/kotlin"), File("../app/src/main/java"))
        roots.forEach { assertTrue("${it.path} exists", it.isDirectory) }
        val forbidden = listOf(
            Regex("""\bLog\.[vdiwe]\("""), Regex("""\bprintln\("""), Regex("""\bprintStackTrace\("""), Regex("""\bSystem\.(out|err)\b"""),
            Regex("""\bsetDebug\("""), Regex("""setDebugOut\("""), Regex("""["']mail\.debug"""), Regex("""\bdebug\s*=\s*true\b"""), Regex("""\bTimber\."""),
            Regex("""android\.util\.Log"""), Regex("""\blog\(Log\."""),
        )
        val hits = ArrayList<String>()
        val stateLines = ArrayList<String>()
        var files = 0
        roots.forEach { root ->
            root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.forEach { file ->
                files++
                val stateLogger = file.name in STATE_LOGGERS && file.path.replace('\\', '/').contains("/app/src/main/java/")
                file.readLines().forEachIndexed { index, line ->
                    val code = line.substringBefore("//")
                    forbidden.forEach { pattern ->
                        if (pattern.containsMatchIn(code)) {
                            if (stateLogger && STATE_LOG_CALL.containsMatchIn(code)) stateLines += "${file.name}:${index + 1}: ${line.trim()}" else hits += "${file.path}:${index + 1}: ${line.trim()}"
                        }
                    }
                }
            }
        }
        assertTrue("at least the core and the app were scanned", files > 60)
        assertEquals("forbidden statements:\n" + hits.joinToString("\n"), emptyList<String>(), hits)
        // the state loggers of the watch service and the browser sign-in (roadmap P8 / P9) log ids, states, counts and
        // durations: no interpolated value of theirs may carry a secret, an address, a subject or a token
        assertTrue("the state loggers were seen", stateLines.size >= 8)
        val leaky = stateLines.filter { line ->
            INTERPOLATION.findAll(line.substringAfter(':')).any { match -> SECRET_WORDS.containsMatchIn(match.value) }
        }
        assertEquals("state log lines interpolating a secret-like value:\n" + leaky.joinToString("\n"), emptyList<String>(), leaky)
    }

    @Test
    fun theSecretLeavesMailSecretOnlyForTheThreeJakartaAuthenticationCalls() {
        val callers = File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> file.readLines().mapIndexedNotNull { index, line -> if (line.contains("reveal()") && !line.trimStart().startsWith("//")) "${file.name}:${index + 1}" else null } }
            .toList()
        val allowed = setOf("MailAccount.kt", "MailSessionFactory.kt", "Redactor.kt")
        assertEquals("reveal() callers: $callers", 5, callers.size)
        assertTrue("reveal() callers: $callers", callers.all { it.substringBefore(':') in allowed })
        assertEquals(3, callers.count { it.startsWith("MailSessionFactory.kt") })
        val appSources = File("../app/src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("the app never materializes the secret itself", appSources.none { it.readText().contains("reveal()") })
    }

    @Test
    fun theJakartaSessionNeverDebugsEvenWhenTheAccountAsksForTheTrace() {
        val account = MailAccountOptions.parse(accountJson, SecretKind.PASSWORD)
        assertTrue("the account asked for the redacted trace", account.debug)
        val previous = System.setProperty("mail.debug", "true")
        try {
            MailProtocol.entries.forEach { protocol ->
                val properties = MailSessionProperties.build(account, protocol)
                assertTrue("$protocol properties carry no debug switch: ${properties.keys}", properties.keys.none { it.toString().contains("debug") })
                val session = MailSessionFactory.session(account, protocol, MailSecret(secretText))
                assertFalse("$protocol session debug", session.debug)
                assertTrue(session.properties.keys.none { it.toString().contains("debug") })
            }
        } finally {
            if (previous == null) System.clearProperty("mail.debug") else System.setProperty("mail.debug", previous)
        }
    }

    @Test
    fun theValueObjectsNeverPrintTheSecret() {
        val secret = MailSecret(secretText)
        assertEquals("MailSecret(***)", secret.toString())
        val account = MailAccountOptions.parse(accountJson, SecretKind.PASSWORD)
        assertFalse(account.toString().contains(secretText))
        secret.clear()
        assertEquals("a cleared secret reveals blanks only", "", secret.reveal().trim())
    }

    @Test
    fun secretsInsideTheAccountJsonAreRefusedWithoutEchoingThem() {
        MailAccountOptions.Fields.FORBIDDEN.forEach { key ->
            try {
                MailAccountOptions.parse("""{"provider":"qq","address":"alice@qq.com","$key":"$secretText"}""", SecretKind.PASSWORD)
                fail("'$key' must be refused")
            } catch (e: MailException) {
                assertEquals(MailErrorCode.INVALID_ARGUMENT, e.code)
                assertTrue(e.message, e.message.contains("'$key'"))
                assertFalse("the refusal must not echo the value: ${e.message} ${e.details}", (e.message + e.details.orEmpty()).contains(secretText))
            }
        }
    }

    @Test
    fun theRedactorMasksEveryFormTheSecretTravelsIn() {
        val redactor = Redactor("alice@qq.com", MailSecret(secretText))
        val encoder = Base64.getEncoder()
        val forms = listOf(
            "raw" to secretText,
            "base64" to encoder.encodeToString(secretText.toByteArray()),
            "sasl-plain" to encoder.encodeToString("\u0000alice@qq.com\u0000$secretText".toByteArray()),
            "xoauth2" to encoder.encodeToString("user=alice@qq.comauth=Bearer $secretText".toByteArray()),
        )
        forms.forEach { (name, form) ->
            val scrubbed = redactor.scrub("A1 LOGIN alice@qq.com $form\r\n* BAD echo $form")
            assertFalse("$name form leaks: $scrubbed", scrubbed.contains(form))
            assertFalse("$name form leaks the secret: $scrubbed", scrubbed.contains(secretText))
            assertTrue(scrubbed.contains(Redactor.MASK))
        }
        val trace = ProtocolTrace(enabled = true, redactor = redactor)
        trace.record("imap", "login failed: server said $secretText and ${forms[1].second}")
        val lines = trace.drain()
        assertEquals(1, lines.size)
        assertFalse(lines[0], lines[0].contains(secretText) || lines[0].contains(forms[1].second))
        assertTrue(ProtocolTrace.disabled().let { it.record("imap", secretText); it.drain().isEmpty() })
    }

    @Test
    fun theExceptionMapperScrubsMessagesDetailsAndToString() {
        val mapper = ExceptionMapper(Redactor("alice@qq.com", MailSecret(secretText)))
        val cause = IllegalStateException("server echoed $secretText in the reply")
        val mapped = mapper.map(AuthenticationFailedException("AUTHENTICATE failed for $secretText", cause), "connect imap.qq.com")
        assertEquals(MailErrorCode.AUTH_FAILED, mapped.code)
        listOf(mapped.message, mapped.details.orEmpty(), mapped.toString()).forEach { text ->
            assertFalse(text, text.contains(secretText))
        }
        assertTrue(mapped.message.startsWith("connect imap.qq.com: "))
    }
}
