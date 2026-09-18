package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import jakarta.mail.Flags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The `args` parsers of the folder and message ops (host protocol document, roadmap P2.3). */
class MessageArgsTest {

    @Test
    fun listDefaultsAndCursors() {
        val defaults = MessageArgs.list("{}")
        assertEquals("INBOX", defaults.folder)
        assertEquals(MailLimits.DEFAULT_PAGE_SIZE, defaults.limit)
        assertNull(defaults.before)
        assertNull(defaults.after)
        assertTrue(defaults.descending)
        assertFalse(defaults.unseenOnly)

        val page = MessageArgs.list("""{"folder": "Archive", "limit": 10, "before": "500", "after": 100, "order": "asc", "unseenOnly": true}""")
        assertEquals("Archive", page.folder)
        assertEquals(10, page.limit)
        assertEquals(500L, page.before)
        assertEquals(100L, page.after)
        assertFalse(page.descending)
        assertTrue(page.unseenOnly)

        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.list("""{"before": 5, "after": 5}""") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.list("""{"order": "newest"}""") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.list("""{"limit": 0}""") }.code)
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, failure { MessageArgs.list("""{"limit": ${MailLimits.MAX_PAGE_SIZE + 1}}""") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.list("""{"before": 0}""") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.list("""{"before": [1, 2]}""") }.code)
        assertTrue(failure { MessageArgs.list("""{"page": 2}""") }.message.contains("args.page"))
        assertTrue(failure { MessageArgs.list("[]") }.message.contains("JSON object"))
        assertTrue(failure { MessageArgs.list("{") }.message.contains("not valid JSON"))
    }

    @Test
    fun searchNeedsAQueryAndKnowsItsFallbackModes() {
        val args = MessageArgs.search("""{"query": {"from": "alice"}, "folder": "Sent", "limit": 5, "before": 900}""")
        assertEquals("Sent", args.folder)
        assertEquals(5, args.limit)
        assertEquals(900L, args.before)
        assertTrue(args.clientFallback)
        assertTrue(args.serverSearch)
        val none = MessageArgs.search("""{"query": {"from": "a"}, "fallback": "none"}""")
        assertFalse(none.clientFallback)
        assertTrue(none.serverSearch)
        val always = MessageArgs.search("""{"query": {"from": "a"}, "fallback": "always"}""")
        assertTrue(always.clientFallback)
        assertFalse(always.serverSearch)
        assertTrue(failure { MessageArgs.search("{}") }.message.contains("'query' is required"))
        assertTrue(failure { MessageArgs.search("""{"query": "alice"}""") }.message.contains("'query' must be an object"))
        assertTrue(failure { MessageArgs.search("""{"query": {}}""") }.message.contains("at least one condition"))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.search("""{"query": {"from": "a"}, "fallback": "server"}""") }.code)
    }

    @Test
    fun getRawAndDownloadAddressOneMessage() {
        val get = MessageArgs.get("""{"uid": 42}""")
        assertEquals("INBOX", get.folder)
        assertEquals(42L, get.uid)
        assertTrue(get.peek)
        assertFalse(get.includeRaw)
        val marked = MessageArgs.get("""{"folder": "Work", "uid": "7", "peek": false, "includeRaw": true}""")
        assertEquals("Work", marked.folder)
        assertFalse(marked.peek)
        assertTrue(marked.includeRaw)
        assertTrue(failure { MessageArgs.get("{}") }.message.contains("'uid' is required"))
        assertTrue(failure { MessageArgs.get("""{"uid": [1, 2]}""") }.message.contains("single UID"))
        assertTrue(failure { MessageArgs.get("""{"uid": "abc"}""") }.message.contains("not a valid IMAP UID"))

        assertEquals(9L, MessageArgs.raw("""{"uid": 9}""").uid)
        val download = MessageArgs.download("""{"uid": 3, "partId": "2.1"}""")
        assertEquals("2.1", download.partId)
        assertTrue(failure { MessageArgs.download("""{"uid": 3}""") }.message.contains("'partId' is required"))
        listOf("0", "1.", ".1", "a", "1.0", "1..2", "-1").forEach { bad ->
            assertTrue(bad, failure { MessageArgs.download("""{"uid": 3, "partId": "$bad"}""") }.message.contains("not a part id"))
        }
    }

    @Test
    fun flagsRequireUidsAndKnowTheirModes() {
        val add = MessageArgs.flags("""{"uids": [3, 1, 3, "2"], "flags": "seen"}""")
        assertEquals(listOf(3L, 1L, 2L), add.uids)
        assertEquals(FlagMode.ADD, add.mode)
        assertTrue(add.flags.contains(Flags.Flag.SEEN))
        val set = MessageArgs.flags("""{"uids": 5, "flags": [], "mode": "set"}""")
        assertEquals(FlagMode.SET, set.mode)
        assertTrue(set.flags.systemFlags.isEmpty() && set.flags.userFlags.isEmpty())
        val custom = MessageArgs.flags("""{"uids": 5, "flags": ["flagged", "label1"], "mode": "remove"}""")
        assertEquals(FlagMode.REMOVE, custom.mode)
        assertTrue(custom.flags.contains("label1"))
        assertTrue(failure { MessageArgs.flags("""{"uids": 5, "flags": []}""") }.message.contains("at least one flag"))
        assertTrue(failure { MessageArgs.flags("""{"flags": "seen"}""") }.message.contains("at least one UID"))
        assertTrue(failure { MessageArgs.flags("""{"uids": [], "flags": "seen"}""") }.message.contains("at least one UID"))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.flags("""{"uids": 5, "flags": "seen", "mode": "toggle"}""") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { MessageArgs.flags("""{"uids": [0], "flags": "seen"}""") }.code)
        val tooMany = (1..MailLimits.MAX_PAGE_SIZE + 1).joinToString(",")
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, failure { MessageArgs.flags("""{"uids": [$tooMany], "flags": "seen"}""") }.code)
    }

    @Test
    fun moveCopyDeleteAndExpungeArgs() {
        val move = MessageArgs.target("""{"uids": [1, 2], "target": "Archive"}""")
        assertEquals("INBOX", move.folder)
        assertEquals("Archive", move.target)
        assertTrue(failure { MessageArgs.target("""{"uids": [1]}""") }.message.contains("'target' is required"))
        val delete = MessageArgs.delete("""{"folder": "Junk", "uids": 4, "expunge": true}""")
        assertEquals("Junk", delete.folder)
        assertTrue(delete.expunge)
        assertFalse(MessageArgs.delete("""{"uids": 4}""").expunge)
        assertEquals("INBOX", MessageArgs.folderOnly("{}"))
        assertEquals("Trash", MessageArgs.folderOnly("""{"folder": "Trash"}"""))
        assertTrue(failure { MessageArgs.folderOnly("{}", required = true) }.message.contains("'folder' is required"))
        assertTrue(failure { MessageArgs.folderOnly("""{"path": "x"}""") }.message.contains("args.path"))
    }

    @Test
    fun folderArgs() {
        val list = MessageArgs.foldersList("{}")
        assertFalse(list.subscribedOnly)
        assertFalse(list.status)
        assertTrue(MessageArgs.foldersList("""{"subscribedOnly": true, "status": true}""").status)
        assertEquals("Work/2026", MessageArgs.path("""{"path": "Work/2026"}"""))
        assertTrue(failure { MessageArgs.path("{}") }.message.contains("'path' is required"))
        assertTrue(failure { MessageArgs.path("""{"path": "  "}""") }.message.contains("blank"))
        val rename = MessageArgs.rename("""{"path": "Old", "newPath": "New"}""")
        assertEquals("Old", rename.path)
        assertEquals("New", rename.newPath)
        assertTrue(failure { MessageArgs.rename("""{"path": "Same", "newPath": "Same"}""") }.message.contains("equals"))
        assertTrue(failure { MessageArgs.rename("""{"path": "Old"}""") }.message.contains("'newPath' is required"))
    }

    private fun failure(block: () -> Any?): MailException {
        try {
            block()
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException")
        throw AssertionError()
    }
}
