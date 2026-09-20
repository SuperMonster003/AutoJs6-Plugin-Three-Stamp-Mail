package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SearchResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Gmail advertises `UTF8=ACCEPT` and, once the extension is enabled, rejects a `SEARCH` that
 * carries non-ASCII text the RFC 6855 way (`BAD Could not parse command`); the same search with
 * `CHARSET UTF-8` and a literal succeeds (real account, 2026-09-20). The mail core therefore never
 * enables the extension, so a Chinese subject search on Gmail runs on the server instead of
 * falling back to the client's 200-message window.
 */
class Utf8SearchTest {
    private lateinit var server: FakeImapServer

    @Before
    fun start() {
        server = FakeImapServer(advertiseId = false, utf8Accept = true).also { it.start() }
    }

    @After
    fun stop() {
        server.close()
    }

    @Test
    fun nonAsciiSearchesCarryTheCharsetAndTheExtensionStaysOff() {
        server.messages = 3
        ImapMailbox.connect(account(), MailSecret(PASSWORD)).use { mailbox ->
            val result = mailbox.search(MessageArgs.search("""{"query":{"subject":"矩阵"},"limit":10,"fallback":"none"}"""))
            assertEquals(SearchResult.FALLBACK_SERVER, result.fallback)
            assertTrue(result.messages.isEmpty())
        }
        val session = server.sessions.first { it.searches.isNotEmpty() }
        assertTrue(session.enabled.toString(), session.enabled.isEmpty())
        assertEquals(listOf("CHARSET UTF-8 SUBJECT 矩阵 ALL"), session.searches.map { it.replace(Regex("\\s+"), " ") })
    }

    private fun account(): MailAccount = MailAccountOptions.parse(
        """{"address":"alice@example.org","user":"alice",
            "imap":{"host":"127.0.0.1","port":${server.port},"tls":"none"},
            "smtp":{"host":"127.0.0.1","port":${server.port},"tls":"none"},
            "clientId":{},
            "timeout":{"connect":5000,"read":10000}}""",
        SecretKind.PASSWORD,
    )

    private companion object {
        const val PASSWORD = "alice-secret"
    }
}
