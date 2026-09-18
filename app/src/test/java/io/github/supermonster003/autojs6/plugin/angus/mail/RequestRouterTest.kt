package io.github.supermonster003.autojs6.plugin.angus.mail

import io.github.supermonster003.autojs6.plugin.angus.mail.binder.RequestRouter
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.AttachmentSource
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import org.autojs.plugin.mail.api.MailContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.OutputStream

/** Snapshot of the op table against `MailContract.OPS` (roadmap P2.5 acceptance, complete since P2.3). */
class RequestRouterTest {

    private val session = MailSession(
        MailAccount("alice@example.org", imap = MailEndpoint("imap.example.org", 993, TlsMode.SSL), smtp = MailEndpoint("smtp.example.org", 465, TlsMode.SSL)),
        MailSecret("not-a-real-secret"),
    )
    private val router = RequestRouter(session)

    @Test
    fun everyContractOpHasAHandler() {
        assertEquals(MailContract.OPS.toSet(), RequestRouter.SUPPORTED_OPS)
        assertTrue(RequestRouter.PENDING_OPS.toString(), RequestRouter.PENDING_OPS.isEmpty())
        RequestRouter.SUPPORTED_OPS.forEach { assertTrue(it, MailContract.isKnownOp(it)) }
        assertEquals(19, RequestRouter.SUPPORTED_OPS.size)
    }

    @Test
    fun routingDistinguishesUnknownOps() {
        MailContract.OPS.forEach { op -> assertNull(op, RequestRouter.errorCodeFor(router.route(op))) }
        assertEquals(MailErrorCode.INVALID_ARGUMENT, RequestRouter.errorCodeFor(router.route("messages.purge")))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, RequestRouter.errorCodeFor(router.route(null)))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, RequestRouter.errorCodeFor(router.route("")))
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, RequestRouter.errorCodeFor(RequestRouter.Route.Unsupported))
    }

    @Test
    fun executeReportsRoutingFailuresAsMailExceptions() {
        assertEquals("true", router.execute(MailContract.OP_SESSION_CLOSE, "{}"))
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
        assertTrue(RequestRouter.takesDescriptors(MailContract.OP_MESSAGES_RAW))
        assertFalse(RequestRouter.takesDescriptors(MailContract.OP_SESSION_TEST))
        assertFalse(RequestRouter.takesDescriptors(MailContract.OP_MESSAGES_GET))
        assertFalse(RequestRouter.takesDescriptors(null))
    }

    @Test
    fun descriptorRulesApplyAfterRoutingAndBeforeTheHandler() {
        val untouched = FakeIo(count = 1)
        val misplaced = failure(MailContract.OP_SESSION_TEST, "{}", untouched)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, misplaced.code)
        assertTrue(misplaced.message, misplaced.message.contains("does not take descriptors"))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("messages.purge", "{}", untouched).code)
        val overLimit = failure(MailContract.OP_MAIL_SEND, "{}", FakeIo(count = MailContract.MAX_DESCRIPTORS + 1))
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, overLimit.code)
        assertFalse(overLimit.retryable)
        assertFalse("no descriptor was opened", untouched.touched)
        val descriptorProblem = failure(MailContract.OP_MAIL_SEND, """{"message":{"to":"bob@example.org","subject":"s"}}""", FakeIo(count = 1, sourcesError = "descriptor 0 is not open"))
        assertTrue(descriptorProblem.message, descriptorProblem.message.contains("descriptor 0"))
    }

    @Test
    fun argumentsAreValidatedBeforeDescriptorsAndTheNetwork() {
        val send = failure(MailContract.OP_MAIL_SEND, "{}")
        assertEquals(MailErrorCode.INVALID_ARGUMENT, send.code)
        assertTrue(send.message, send.message.contains("'message' is required"))
        val unbound = failure(
            MailContract.OP_MESSAGES_APPEND,
            """{"folder":"Drafts","message":{"to":"bob@example.org","subject":"s","attachments":[{"descriptorIndex":0,"fileName":"a.txt"}]}}""",
        )
        assertTrue(unbound.message, unbound.message.contains("has no descriptor (0 supplied)"))

        val neverOpened = FakeIo(count = 1)
        assertTrue(failure(MailContract.OP_MESSAGES_RAW, "{}", neverOpened).message.contains("'uid' is required"))
        assertTrue(failure(MailContract.OP_ATTACHMENTS_DOWNLOAD, """{"uid": 1, "partId": "x"}""", neverOpened).message.contains("not a part id"))
        assertFalse("argument errors come before the sink is opened", neverOpened.touched)
        assertTrue(failure(MailContract.OP_MESSAGES_RAW, """{"uid": 1}""").message.contains("single descriptor (0 supplied)"))

        assertTrue(failure(MailContract.OP_MESSAGES_LIST, """{"limit": 0}""").message.contains("'limit'"))
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, failure(MailContract.OP_MESSAGES_LIST, """{"limit": 5000}""").code)
        assertTrue(failure(MailContract.OP_MESSAGES_SEARCH, "{}").message.contains("'query' is required"))
        assertTrue(failure(MailContract.OP_MESSAGES_GET, "{}").message.contains("'uid' is required"))
        assertTrue(failure(MailContract.OP_MESSAGES_SET_FLAGS, """{"uids": 1, "flags": []}""").message.contains("at least one flag"))
        assertTrue(failure(MailContract.OP_MESSAGES_MOVE, """{"uids": [1]}""").message.contains("'target' is required"))
        assertTrue(failure(MailContract.OP_MESSAGES_COPY, """{"target": "Archive"}""").message.contains("at least one UID"))
        assertTrue(failure(MailContract.OP_MESSAGES_DELETE, """{"uids": [0]}""").message.contains("invalid IMAP UID"))
        assertTrue(failure(MailContract.OP_MESSAGES_EXPUNGE, """{"path": "x"}""").message.contains("args.path"))
        assertTrue(failure(MailContract.OP_FOLDERS_STATUS, "{}").message.contains("'folder' is required"))
        assertTrue(failure(MailContract.OP_FOLDERS_CREATE, "{}").message.contains("'path' is required"))
        assertTrue(failure(MailContract.OP_FOLDERS_DELETE, """{"path": " "}""").message.contains("blank"))
        assertTrue(failure(MailContract.OP_FOLDERS_RENAME, """{"path": "A", "newPath": "A"}""").message.contains("equals"))
        assertTrue(failure(MailContract.OP_FOLDERS_LIST, """{"status": "yes"}""").message.contains("must be a boolean"))
        assertTrue(failure(MailContract.OP_FOLDERS_LIST, "[]").message.contains("JSON object"))
        assertEquals(0, session.connectCount(MailProtocol.SMTP))
        assertEquals(0, session.connectCount(MailProtocol.IMAP))
    }

    private class FakeIo(val count: Int, private val sourcesError: String? = null) : RequestRouter.CallIo {
        var touched = false
        override val descriptorCount: Int get() = count
        override fun sources(): List<AttachmentSource> {
            touched = true
            sourcesError?.let { throw MailException.invalidArgument(it) }
            return emptyList()
        }

        override fun sink(): OutputStream {
            touched = true
            throw AssertionError("the sink must not be opened in this test")
        }

        override fun progress(transferred: Long, total: Long?) = Unit
    }

    private fun failure(op: String?, args: String = "{}", io: RequestRouter.CallIo = RequestRouter.CallIo.NONE): MailException {
        try {
            router.execute(op, args, io)
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException for $op")
        throw AssertionError()
    }
}
