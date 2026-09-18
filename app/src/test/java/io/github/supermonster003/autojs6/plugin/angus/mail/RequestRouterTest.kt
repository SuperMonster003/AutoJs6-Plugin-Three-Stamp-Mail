package io.github.supermonster003.autojs6.plugin.angus.mail

import io.github.supermonster003.autojs6.plugin.angus.mail.binder.RequestRouter
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import org.autojs.plugin.mail.api.MailContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Snapshot of the op table against `MailContract.OPS` (roadmap P2.5 acceptance, started in P2.1). */
class RequestRouterTest {

    private val session = MailSession(
        MailAccount("alice@example.org", imap = MailEndpoint("imap.example.org", 993, TlsMode.SSL), smtp = MailEndpoint("smtp.example.org", 465, TlsMode.SSL)),
        MailSecret("not-a-real-secret"),
    )
    private val router = RequestRouter(session)

    @Test
    fun everyHandledOpBelongsToTheContract() {
        assertEquals(
            setOf(MailContract.OP_SESSION_TEST, MailContract.OP_SESSION_CLOSE, MailContract.OP_MAIL_SEND, MailContract.OP_MESSAGES_APPEND),
            RequestRouter.SUPPORTED_OPS,
        )
        RequestRouter.SUPPORTED_OPS.forEach { assertTrue(it, MailContract.isKnownOp(it)) }
        assertEquals(MailContract.OPS.size, RequestRouter.SUPPORTED_OPS.size + RequestRouter.PENDING_OPS.size)
        assertTrue(RequestRouter.PENDING_OPS.none { it in RequestRouter.SUPPORTED_OPS })
    }

    @Test
    fun routingDistinguishesUnknownAndPendingOps() {
        assertNull(RequestRouter.errorCodeFor(router.route(MailContract.OP_SESSION_TEST)))
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, RequestRouter.errorCodeFor(router.route(MailContract.OP_MESSAGES_LIST)))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, RequestRouter.errorCodeFor(router.route("messages.purge")))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, RequestRouter.errorCodeFor(router.route(null)))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, RequestRouter.errorCodeFor(router.route("")))
    }

    @Test
    fun executeReportsRoutingFailuresAsMailExceptions() {
        assertEquals("true", router.execute(MailContract.OP_SESSION_CLOSE, "{}"))
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, failure(MailContract.OP_FOLDERS_LIST).code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("messages.purge").code)
        assertTrue(failure(null).message.contains("no op"))
    }

    @Test
    fun descriptorsBelongToTheTransferOpsOnly() {
        MailContract.OPS.forEach { op ->
            assertEquals(op, op in MailContract.OPS_WITH_SOURCES || op in MailContract.OPS_WITH_SINK, RequestRouter.takesDescriptors(op))
        }
        assertTrue(RequestRouter.takesDescriptors(MailContract.OP_MAIL_SEND))
        assertTrue(RequestRouter.takesDescriptors(MailContract.OP_MESSAGES_APPEND))
        assertTrue(RequestRouter.takesDescriptors(MailContract.OP_ATTACHMENTS_DOWNLOAD))
        assertFalse(RequestRouter.takesDescriptors(MailContract.OP_SESSION_TEST))
        assertFalse(RequestRouter.takesDescriptors(null))
    }

    @Test
    fun sourcesAreResolvedOnlyForHandledOps() {
        val neverAsked: () -> List<Nothing> = { throw AssertionError("routing failures must precede the descriptor work") }
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, failure(MailContract.OP_MESSAGES_LIST, "{}", neverAsked).code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("messages.purge", "{}", neverAsked).code)
        val descriptorProblem = failure(MailContract.OP_MAIL_SEND, "{}") { throw MailException.invalidArgument("descriptor 0 is not open") }
        assertTrue(descriptorProblem.message, descriptorProblem.message.contains("descriptor 0"))
    }

    @Test
    fun sendAndAppendValidateTheirArgumentsBeforeTouchingTheNetwork() {
        val send = failure(MailContract.OP_MAIL_SEND, "{}")
        assertEquals(MailErrorCode.INVALID_ARGUMENT, send.code)
        assertTrue(send.message, send.message.contains("'message' is required"))
        val unbound = failure(
            MailContract.OP_MESSAGES_APPEND,
            """{"folder":"Drafts","message":{"to":"bob@example.org","subject":"s","attachments":[{"descriptorIndex":0,"fileName":"a.txt"}]}}""",
        )
        assertEquals(MailErrorCode.INVALID_ARGUMENT, unbound.code)
        assertTrue(unbound.message, unbound.message.contains("has no descriptor (0 supplied)"))
        assertEquals(0, session.connectCount(MailProtocol.SMTP))
        assertEquals(0, session.connectCount(MailProtocol.IMAP))
    }

    private fun failure(op: String?, args: String = "{}", sources: () -> List<Nothing> = { emptyList() }): MailException {
        try {
            router.execute(op, args, sources)
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException for $op")
        throw AssertionError()
    }
}
