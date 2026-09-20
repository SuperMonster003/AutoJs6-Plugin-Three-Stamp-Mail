package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.PresetEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.imapUid
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.BytesAttachmentSource
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MailAddressSpec
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingAttachment
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchEvent
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchOptions
import jakarta.mail.Folder
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.util.Properties
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manual provider matrix against real accounts (mail roadmap P6 "provider matrix"): skipped
 * unless the git-ignored `mail-test-accounts.properties` and `build/p6/matrix.properties`
 * (`profiles`, optional `idleSeconds`, `pollIntervalMs`, `pop3`, `keep`) exist. For every
 * profile it runs, through the same [MailSession] the Binder uses, `session.test`, the folder
 * list, a send with a Chinese subject / body / file name to the account itself, the inbox
 * listing until delivery, Chinese and ASCII searches on the server and on the client, the
 * body, the attachment bytes, flags, a move (into a created folder, or an existing one where
 * the server refuses CREATE), a watch in automatic mode with a second delivery, and an
 * optional POP3 leg; then it deletes what it created. Every outcome is a row in
 * `build/p6/matrix-<profile>.log` and `build/p6/matrix-summary.txt`; addresses are printed as
 * `***@domain`, secrets never (the session's redactor scrubs the protocol trace). The probe
 * never fails on a provider's behaviour: the rows are the evidence, `.python/run_provider_matrix.py`
 * drives it and masks the Gradle output.
 */
class ProviderMatrixProbe {

    private class Row(val op: String, val outcome: String, val ms: Long, val note: String) {
        override fun toString(): String = "$op | $outcome | $ms ms | $note"
    }

    private class Skip(message: String) : RuntimeException(message)

    private lateinit var log: PrintWriter
    private var only: Set<String>? = null
    private var noPreset = false
    private var mask: (String) -> String = { it }
    private var diag: List<String> = emptyList()
    private val rows = mutableListOf<Row>()

    @Test
    fun runTheMatrix() {
        val accounts = File("../mail-test-accounts.properties")
        val settings = File("../build/p6/matrix.properties")
        assumeTrue("no real accounts / matrix settings", accounts.isFile && settings.isFile)
        val props = Properties().apply { accounts.inputStream().use { load(it) } }
        val config = Properties().apply { settings.reader(Charsets.UTF_8).use { load(it) } }
        diag = config.getProperty("diag")?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val profiles = config.getProperty("profiles", "QQ_A").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val idleSeconds = config.getProperty("idleSeconds", "120").toLong()
        val pollIntervalMs = config.getProperty("pollIntervalMs", "30000").toLong()
        val pop3 = config.getProperty("pop3", "true").toBoolean()
        val keep = config.getProperty("keep", "false").toBoolean()
        only = config.getProperty("ops")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
        noPreset = config.getProperty("noPreset", "false").toBoolean()
        val summary = File("../build/p6/matrix-summary.txt").printWriter()
        try {
            profiles.forEach { profile ->
                rows.clear()
                log = File("../build/p6/matrix-$profile.log").printWriter()
                try {
                    runProfile(profile, props, idleSeconds, pollIntervalMs, pop3, keep)
                } catch (e: Throwable) {
                    rows += Row("profile", "ERROR", 0, "${e.javaClass.simpleName}: ${e.message?.take(300)}")
                } finally {
                    log.close()
                }
                summary.println("## $profile")
                rows.forEach { summary.println(mask(it.toString())); println("$profile: ${mask(it.toString())}") }
                summary.println()
                summary.flush()
            }
        } finally {
            summary.close()
        }
    }

    private fun runProfile(profile: String, props: Properties, idleSeconds: Long, pollIntervalMs: Long, pop3Leg: Boolean, keep: Boolean) {
        val (kind, letter) = profile.split("_")
        val address = requireNotNull(props.getProperty("${kind}_USER_NAME_$letter")) { "no address for $profile" }
        // A profile with an access token key (GMAIL, or OUTLOOK / HOTMAIL from .python/outlook_oauth_login.py) uses XOAUTH2.
        val tokenAuth = props.getProperty("${kind}_ACCESS_TOKEN_$letter") != null
        val secret = requireNotNull(props.getProperty(if (tokenAuth) "${kind}_ACCESS_TOKEN_$letter" else "${kind}_AUTH_CODE_$letter")) { "no secret for $profile" }
        val domain = address.substringAfterLast('@').lowercase()
        val preset = ProviderPresets.all.firstOrNull { domain in it.domains }
            ?: if (domain == "yeah.net") ProviderPresets.all.first { it.id == "163" } else error("no preset for domain $domain")
        fun endpoint(name: String, endpoint: PresetEndpoint?) = endpoint?.let { ""","$name":{"host":"${it.host}","port":${it.port},"tls":"${it.tls}"}""" }.orEmpty()
        val hosts = when {
            domain == "yeah.net" -> ""","imap":{"host":"imap.yeah.net"},"pop3":{"host":"pop.yeah.net"},"smtp":{"host":"smtp.yeah.net"}"""
            // no `provider`: the preset's hosts spelled out, so its authentication restriction does not apply and the
            // server's own answer to the login is what gets mapped (Outlook.com with an app password)
            noPreset -> endpoint("imap", preset.imap) + endpoint("pop3", preset.pop3) + endpoint("smtp", preset.smtp)
            else -> ""
        }
        val providerField = if (noPreset) "" else """"provider":"${preset.id}","""
        val masked = "***@$domain"
        val local = address.substringBeforeLast('@')
        mask = { text -> text.replace(address, masked).replace(local, "***") }
        out("# $profile provider=${if (noPreset) "none (hosts of ${preset.id})" else preset.id} address=$masked auth=${if (tokenAuth) "xoauth2" else "password"} idlePush=${preset.idlePush} autoSavesSent=${preset.autoSavesSent} sentFolder=${preset.sentFolder} requiresClientId=${preset.requiresClientId}")
        fun account(receive: String): MailAccount = MailAccountOptions.parse(
            """{$providerField"address":"$address","receive":"$receive","debug":true,"timeout":{"connect":20000,"read":60000}$hosts}""",
            if (tokenAuth) SecretKind.ACCESS_TOKEN else SecretKind.PASSWORD,
        )
        val stamp = System.currentTimeMillis().toString().takeLast(8)
        val subject = "AutoJs6 矩阵 ${preset.id} $stamp"
        val text = "中文正文 matrix body $stamp\n第二行: 附件为文本文件.\n"
        val fileName = "报表 $stamp.txt"
        val attachmentBytes = "附件内容 attachment $stamp\n".toByteArray()
        var delivered: MessageDocument? = null
        var movedTo: String? = null
        var createdFolder: String? = null
        val session = MailSession(account("imap"), MailSecret(secret.toCharArray()))
        try {
            op("test") {
                val report = session.test()
                listOfNotNull(report.imap, report.pop3, report.smtp).joinToString("; ") { e ->
                    "${e.protocol} ${e.host}:${e.port}/${e.tls} ${if (e.ok) "ok" else "FAIL ${e.error?.code}: ${e.error?.message}"} ${e.elapsedMs} ms caps=${e.capabilities}"
                } to report.ok
            }
            fun rawDiag(name: String, commands: List<String>) {
                if (commands.isEmpty()) return
                op(name) {
                    val lines = session.imap { mailbox ->
                        mailbox.withFolder(MessageArgs.INBOX, Folder.READ_ONLY) { inbox ->
                            commands.flatMap { command ->
                                val responses = inbox.doCommand { protocol -> protocol.command(command, null) } as Array<*>
                                listOf("> $command") + responses.map { "< ${session.redactor.scrub(it.toString())}" }
                            }
                        }
                    }
                    lines.forEach { out("  [$name] ${mask(it)}") }
                    "${commands.size} raw commands, ${lines.size - commands.size} response lines (see the log)" to true
                }
            }
            rawDiag("diag", diag.filter { !it.contains("{uid}") && !it.startsWith("@late:") })
            var sentFolder: String? = null
            op("folders") {
                val started = System.currentTimeMillis()
                val tree = session.listFolders(MessageArgs.foldersList("""{"status":false}"""))
                val listMs = System.currentTimeMillis() - started
                val flat = flatten(tree)
                sentFolder = session.imap { session.resolveSentFolder(it) }
                val roles = flat.filter { it.specialUse != null }.joinToString(", ") { "${it.specialUse}=${it.path}" }
                val delimiters = flat.map { it.delimiter }.distinct()
                val unselectable = flat.filter { !it.selectable }.map { it.path }
                "${flat.size} folders in $listMs ms, delimiter=$delimiters, roles [$roles], sent folder=$sentFolder, unselectable=$unselectable, names=${flat.map { it.path }.take(30)}" to true
            }
            var messageId: String? = null
            op("send") {
                val message = OutgoingMessage(
                    to = listOf(MailAddressSpec(address, "矩阵收件人")),
                    subject = subject,
                    text = text,
                    attachments = listOf(OutgoingAttachment(fileName, "text/plain", source = BytesAttachmentSource(attachmentBytes))),
                    from = MailAddressSpec(address, "AutoJs6 矩阵"),
                )
                val sent = session.send(message)
                messageId = sent.messageId
                "accepted=${sent.accepted.size} sentCopy=${sent.sentCopy} sentFolder=${sent.sentFolder} savedToSent=${sent.savedToSent} saveError=${sent.saveError?.code ?: "-"} in ${sent.elapsedMs} ms" to sent.rejected.isEmpty()
            }
            op("list") {
                val started = System.currentTimeMillis()
                var polls = 0
                var listMs = 0L
                while (delivered == null && System.currentTimeMillis() - started < DELIVERY_WAIT_MS) {
                    polls++
                    val t = System.currentTimeMillis()
                    val page = session.listMessages(MessageArgs.list("""{"limit":10}"""))
                    listMs = System.currentTimeMillis() - t
                    delivered = page.firstOrNull { it.subject == subject }
                    if (delivered == null) Thread.sleep(DELIVERY_POLL_MS)
                }
                val d = delivered ?: throw Skip("not delivered within ${DELIVERY_WAIT_MS / 1000} s ($polls polls)")
                "delivered after ${System.currentTimeMillis() - started} ms ($polls polls, last list of 10 in $listMs ms): uid=${d.uid} size=${d.size} seen=${d.seen} hasAttachments=${d.hasAttachments} from.name=${d.from?.name} messageIdKept=${d.messageId == messageId}" to true
            }
            val uid = delivered?.imapUid
            if (uid != null) {
                op("search-server") {
                    val cn = search(session, """{"query":{"subject":"矩阵"},"limit":10,"fallback":"none"}""", uid)
                    val ascii = search(session, """{"query":{"subject":"AutoJs6"},"limit":10,"fallback":"none"}""", uid)
                    val from = search(session, """{"query":{"from":${JsonPrimitive(domain)}},"limit":10,"fallback":"none"}""", uid)
                    val body = search(session, """{"query":{"body":"matrix body"},"limit":10,"fallback":"none"}""", uid)
                    val id = search(session, """{"query":{"messageId":${JsonPrimitive(delivered!!.messageId ?: messageId!!)}},"limit":10,"fallback":"none"}""", uid)
                    val since = search(session, """{"query":{"since":${System.currentTimeMillis() - 86_400_000L}},"limit":10,"fallback":"none"}""", uid)
                    "subject 中文: $cn; subject ASCII: $ascii; from: $from; body: $body; messageId: $id; since 24h: $since" to true
                }
                op("search-client") {
                    val auto = search(session, """{"query":{"subject":"矩阵"},"limit":10}""", uid)
                    val always = search(session, """{"query":{"subject":"矩阵"},"limit":10,"fallback":"always"}""", uid)
                    val bodyAlways = search(session, """{"query":{"body":"中文正文"},"limit":10,"fallback":"always"}""", uid)
                    "subject 中文 default: $auto; subject 中文 always: $always; body 中文 always: $bodyAlways" to true
                }
                var partId: String? = null
                op("body") {
                    val started = System.currentTimeMillis()
                    val full = session.getMessage(MessageArgs.get("""{"uid":$uid}"""))
                    partId = full.attachments.firstOrNull()?.partId
                    val ok = full.subject == subject && full.text?.contains("中文正文") == true && full.attachments.size == 1 && full.attachments.single().fileName == fileName && !full.seen
                    "get in ${System.currentTimeMillis() - started} ms: subject=${full.subject == subject} text=${full.text?.contains("中文正文")} html=${full.html != null} headers=${full.headers.size} from.name=${full.from?.name} to.name=${full.to.firstOrNull()?.name} to.header=${full.headers["To"]} attachments=${full.attachments.map { "${it.partId}:${it.fileName}:${it.mimeType}:${it.size}" }} seenAfterPeek=${full.seen}" to ok
                }
                rawDiag("diag-uid", diag.filter { it.contains("{uid}") && !it.startsWith("@late:") }.map { it.replace("{uid}", uid.toString()) })
                op("attachment") {
                    val id = partId ?: throw Skip("no attachment part")
                    val sink = ByteArrayOutputStream()
                    val started = System.currentTimeMillis()
                    val result = session.downloadAttachment(MessageArgs.download("""{"uid":$uid,"partId":"$id"}"""), sink)
                    val same = sink.toByteArray().contentEquals(attachmentBytes)
                    "download in ${System.currentTimeMillis() - started} ms: bytes=${result.bytes} fileName=${result.fileName} mimeType=${result.mimeType} bytesEqual=$same" to (same && result.fileName == fileName)
                }
                op("flags") {
                    val added = session.setFlags(MessageArgs.flags("""{"uids":[$uid],"flags":["flagged","AutoJs6Matrix"],"mode":"add"}"""))
                    val afterAdd = session.getMessage(MessageArgs.get("""{"uid":$uid}"""))
                    val removed = session.setFlags(MessageArgs.flags("""{"uids":[$uid],"flags":["flagged"],"mode":"remove"}"""))
                    val afterRemove = session.getMessage(MessageArgs.get("""{"uid":$uid}"""))
                    val seen = session.setFlags(MessageArgs.flags("""{"uids":[$uid],"flags":["seen"],"mode":"add"}"""))
                    val afterSeen = session.getMessage(MessageArgs.get("""{"uid":$uid}"""))
                    val ok = afterAdd.flagged && !afterRemove.flagged && afterSeen.seen
                    "add flagged+keyword -> ${added.uidStrings} flags=${afterAdd.flags}; remove flagged -> ${removed.uidStrings} flags=${afterRemove.flags}; add seen -> ${seen.uidStrings} flags=${afterSeen.flags}" to ok
                }
                op("move") {
                    val roles = flatten(session.listFolders(MessageArgs.foldersList("{}")))
                    val trash = roles.firstOrNull { it.specialUse == "trash" }?.path ?: roles.firstOrNull { it.specialUse == "junk" }?.path
                    val notes = mutableListOf<String>()
                    var target: String? = null
                    try {
                        val created = session.createFolder(MATRIX_FOLDER)
                        val listed = flatten(session.listFolders(MessageArgs.foldersList("{}"))).firstOrNull { it.name == MATRIX_FOLDER }
                        notes += "CREATE ok: path=${created.path} listed as ${listed?.path}"
                        Thread.sleep(3000)
                        val stillThere = session.imap { it.folderExists(listed?.path ?: created.path) }
                        notes += "exists 3 s later=$stillThere"
                        if (stillThere) {
                            target = listed?.path ?: created.path
                            createdFolder = target
                        } else {
                            notes += "created folder vanished (QQ behaviour), moving to $trash instead"
                        }
                    } catch (e: MailException) {
                        notes += "CREATE refused (${e.code}: ${e.message?.take(120)}), moving to $trash instead"
                    }
                    val destination = target ?: trash ?: throw Skip("${notes.joinToString("; ")}; no trash / junk folder either")
                    val moveCapability = session.imap { it.hasCapability("MOVE") }
                    val started = System.currentTimeMillis()
                    val moved = session.move(MessageArgs.target("""{"uids":[$uid],"target":${JsonPrimitive(destination)}}"""))
                    val moveMs = System.currentTimeMillis() - started
                    movedTo = destination
                    val inTarget = runCatching { session.listMessages(MessageArgs.list("""{"folder":${JsonPrimitive(destination)},"limit":10}""")).firstOrNull { it.subject == subject } }
                        .getOrElse { notes += "target listing failed: ${(it as? MailException)?.code ?: it.javaClass.simpleName}"; null }
                    val stillInInbox = session.listMessages(MessageArgs.list("""{"limit":10}""")).any { it.subject == subject }
                    "${notes.joinToString("; ")}; MOVE capability=$moveCapability; moved to $destination in $moveMs ms, target uids=${moved.uids}; found in target uid=${inTarget?.uid}; still in INBOX=$stillInInbox" to (inTarget != null && !stillInInbox)
                }
            }
            op("watch") {
                val events = CopyOnWriteArrayList<Pair<Long, WatchEvent>>()
                val started = System.currentTimeMillis()
                val watcher = session.watch(WatchOptions(folder = MessageArgs.INBOX, mode = null, pollIntervalMs = pollIntervalMs), { event -> events += (System.currentTimeMillis() - started) to event })
                watcher.start()
                // A Mode event is only emitted on an IDLE -> poll fallback; give the watcher its first EXAMINE, then deliver.
                Thread.sleep(2_000)
                val mode = events.firstOrNull { it.second is WatchEvent.Mode }
                val watchSubject = "AutoJs6 矩阵 watch ${preset.id} $stamp"
                val sentAt = System.currentTimeMillis()
                session.send(OutgoingMessage(to = listOf(MailAddressSpec(address)), subject = watchSubject, text = "watch $stamp\n"))
                val deadline = sentAt + idleSeconds * 1000
                while (events.none { (it.second as? WatchEvent.Message)?.message?.subject == watchSubject } && System.currentTimeMillis() < deadline) Thread.sleep(1000)
                val hit = events.firstOrNull { (it.second as? WatchEvent.Message)?.message?.subject == watchSubject }
                val status = watcher.status()
                watcher.stop()
                Thread.sleep(500)
                val summary = events.joinToString(", ") { (t, e) ->
                    "+${t}ms " + when (e) {
                        is WatchEvent.Message -> "message uid=${e.message.imapUid}"
                        is WatchEvent.Mode -> "mode=${e.mode.id}"
                        is WatchEvent.Error -> "error ${e.error.code}"
                        is WatchEvent.Resync -> "resync ${e.reason}"
                        is WatchEvent.Closed -> "closed ${e.reason}"
                    }
                }
                val idleAdvertised = session.imap { it.hasCapability("IDLE") }
                val latency = hit?.let { it.first + started - sentAt }
                "IDLE advertised=$idleAdvertised, preset idlePush=${preset.idlePush}, effective mode=${(mode?.second as? WatchEvent.Mode)?.mode?.id ?: status.mode.id}, poll interval ${pollIntervalMs / 1000} s, second delivery seen after ${latency?.let { "$it ms" } ?: "never (${idleSeconds} s)"}; events: $summary" to (hit != null)
            }
            if (pop3Leg && session.account.pop3 != null) {
                val pop3 = MailSession(account("pop3"), MailSecret(secret.toCharArray()))
                try {
                    op("pop3") {
                        val report = pop3.test()
                        val started = System.currentTimeMillis()
                        val page = pop3.listMessages(MessageArgs.list("""{"limit":5}""", MailProtocol.POP3))
                        val listMs = System.currentTimeMillis() - started
                        val newest = page.firstOrNull() ?: throw Skip("empty POP3 listing")
                        val t = System.currentTimeMillis()
                        val full = pop3.getMessage(MessageArgs.get("""{"uid":${JsonPrimitive(newest.uid.content)}}""", MailProtocol.POP3))
                        val getMs = System.currentTimeMillis() - t
                        val ours = page.any { it.subject == subject || it.subject.startsWith("AutoJs6 矩阵") }
                        "pop3 ${report.pop3?.host}:${report.pop3?.port} ${if (report.pop3?.ok == true) "ok" else "FAIL ${report.pop3?.error?.code}"} ${report.pop3?.elapsedMs} ms caps=${report.pop3?.capabilities}; list 5 in $listMs ms (uidl length ${newest.uid.content.length}, matrix mail visible=$ours); get newest in $getMs ms textLength=${full.text?.length} attachments=${full.attachments.size}" to (report.pop3?.ok == true)
                    }
                } finally {
                    drain(pop3)
                    pop3.close()
                }
            }
            rawDiag("diag-late", diag.filter { it.startsWith("@late:") }.map { it.removePrefix("@late:") })
            if (!keep) {
                op("cleanup") {
                    var deleted = 0
                    fun purge(folder: String?, label: String): String {
                        if (folder == null) return "$label: none"
                        val page = session.listMessages(MessageArgs.list("""{"folder":${JsonPrimitive(folder)},"limit":50}"""))
                        val ours = page.filter { it.subject.startsWith("AutoJs6 矩阵") }.mapNotNull { it.imapUid }
                        if (ours.isEmpty()) return "$label: 0 of ${page.size}"
                        val result = session.delete(MessageArgs.delete("""{"folder":${JsonPrimitive(folder)},"uids":${ours.joinToString(",", "[", "]")},"expunge":true}"""))
                        deleted += result.uids.size
                        return "$label: ${result.uids.size} (listed $ours)"
                    }
                    fun safely(label: String, block: () -> String): String = runCatching(block).getOrElse { "$label: ${(it as? MailException)?.code ?: it.javaClass.simpleName} ${it.message?.take(120)}" }
                    val parts = mutableListOf(safely("INBOX") { purge(MessageArgs.INBOX, "INBOX") })
                    val sent = sentFolder ?: session.imap { session.resolveSentFolder(it) }
                    if (sent != null) parts += safely("sent") { purge(sent, "sent ($sent)") }
                    // Messages are purged from every folder touched; only folders the probe created (name == MATRIX_FOLDER) are ever deleted.
                    val strays = flatten(session.listFolders(MessageArgs.foldersList("{}"))).filter { it.name == MATRIX_FOLDER }.map { it.path }
                    (strays + listOfNotNull(movedTo, createdFolder)).distinct().filter { it != MessageArgs.INBOX && it != sent }.forEach { path ->
                        parts += safely(path) { purge(path, path) }
                        if (path.substringAfterLast('/') == MATRIX_FOLDER || path == MATRIX_FOLDER) {
                            parts += safely("folder $path") { "folder $path deleted=${session.deleteFolder(path)}" }
                        }
                    }
                    "deleted $deleted messages: ${parts.joinToString("; ")}" to true
                }
            }
        } finally {
            drain(session)
            session.close()
        }
    }

    private fun search(session: MailSession, args: String, uid: Long): String {
        val started = System.currentTimeMillis()
        return try {
            val result = session.searchMessages(MessageArgs.search(args))
            "${result.messages.size} hits via ${result.fallback} in ${System.currentTimeMillis() - started} ms (ours=${result.messages.any { it.imapUid == uid }})"
        } catch (e: MailException) {
            "FAIL ${e.code}: ${e.message?.take(160)} ${e.details?.take(160) ?: ""} in ${System.currentTimeMillis() - started} ms"
        }
    }

    private fun op(name: String, block: () -> Pair<String, Boolean>) {
        if (only?.contains(name) == false) return
        val started = System.currentTimeMillis()
        val row = try {
            val (note, ok) = block()
            Row(name, if (ok) "ok" else "unexpected", System.currentTimeMillis() - started, note)
        } catch (e: Skip) {
            Row(name, "skipped", System.currentTimeMillis() - started, e.message ?: "")
        } catch (e: MailException) {
            Row(name, "FAIL ${e.code}", System.currentTimeMillis() - started, "${e.message?.take(300)} ${e.details?.take(200) ?: ""}")
        } catch (e: Throwable) {
            Row(name, "ERROR", System.currentTimeMillis() - started, "${e.javaClass.simpleName}: ${e.message?.take(300)}")
        }
        rows += row
        out(mask(row.toString()))
    }

    private fun drain(session: MailSession) {
        if (!session.trace.enabled) return
        session.trace.drain().forEach { out("  [trace ${session.receiveProtocol.id}] $it") }
    }

    private fun out(line: String) {
        log.println(line)
        log.flush()
    }

    private fun flatten(tree: List<FolderDocument>): List<FolderDocument> = tree.flatMap { listOf(it) + flatten(it.children) }

    private companion object {
        const val MATRIX_FOLDER = "AutoJs6Matrix"
        const val DELIVERY_WAIT_MS = 180_000L
        const val DELIVERY_POLL_MS = 5_000L
    }
}
