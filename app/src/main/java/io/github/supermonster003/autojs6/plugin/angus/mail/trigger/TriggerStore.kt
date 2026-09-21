package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.content.Context
import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecord
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecordsDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfigsDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerId
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID

/** The process-wide [TriggerStore] of the plugin, under `noBackupFilesDir` like the account store (roadmap P8). */
object TriggerStores {

    @Volatile
    private var instance: TriggerStore? = null

    fun of(context: Context): TriggerStore = instance ?: synchronized(this) {
        instance ?: TriggerStore(context.applicationContext.noBackupFilesDir).also { instance = it }
    }
}

/**
 * The background watches and their trigger records (roadmap P8), as plain JSON files under
 * `<privateRoot>/mail-triggers/`: `triggers.json` holds the [TriggerConfig] list (at most
 * `MAX_TRIGGERS`), `records-<sha256(triggerId)>.json` the last `MAX_TRIGGER_RECORDS` arrivals of
 * one watch (envelope summaries, never a body). Nothing here is a secret: a watch names a saved
 * account by alias and the account store keeps the credentials. Every operation runs under a JVM
 * mutex plus an exclusive file lock, and every publication is fsync + atomic rename, like the
 * account records. Pure JVM apart from the constructor, so the JVM tests cover it on a temp dir.
 */
class TriggerStore(
    privateRoot: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val directory = File(privateRoot, DIRECTORY)
    private val lockFile = File(directory, LOCK_FILE)
    private val configsFile = File(directory, CONFIGS_FILE)
    private val mutex = Any()

    /** Every valid watch ordered by trigger id; damaged entries are left out. */
    fun list(): List<TriggerConfig> = access { readConfigs() }

    fun get(triggerId: String): TriggerConfig? {
        val id = TriggerId.normalize(triggerId)
        return list().firstOrNull { it.triggerId == id }
    }

    fun hasEnabled(): Boolean = list().any { it.enabled }

    /**
     * Saves or replaces the watch named by [config]'s trigger id (the id and the alias are
     * normalized, `since` is kept from the existing record or set now); refuses the seventeenth
     * watch with `LIMIT_EXCEEDED` and an unusable configuration with `INVALID_ARGUMENT`.
     */
    fun put(config: TriggerConfig): TriggerConfig {
        val canonical = config.validated()
        return access {
            val current = readConfigs()
            val existing = current.firstOrNull { it.triggerId == canonical.triggerId }
            val others = current.filter { it.triggerId != canonical.triggerId }
            if (existing == null && others.size >= MailLimits.MAX_TRIGGERS) {
                throw MailException(MailErrorCode.LIMIT_EXCEEDED, "at most ${MailLimits.MAX_TRIGGERS} background watches (MAX_TRIGGERS)", retryable = false)
            }
            val stored = canonical.copy(since = existing?.since?.takeIf { it > 0 } ?: canonical.since.takeIf { it > 0 } ?: clock())
            writeConfigs(others + stored)
            stored
        }
    }

    /** Switches the watch on or off; null when there is no such watch. */
    fun setEnabled(triggerId: String, enabled: Boolean): TriggerConfig? {
        val id = TriggerId.normalize(triggerId)
        return access {
            val current = readConfigs()
            val existing = current.firstOrNull { it.triggerId == id } ?: return@access null
            val updated = existing.copy(enabled = enabled)
            writeConfigs(current.filter { it.triggerId != id } + updated)
            updated
        }
    }

    /** Deletes the watch and its records; false when there was none. */
    fun remove(triggerId: String): Boolean {
        val id = TriggerId.normalize(triggerId)
        return access {
            val current = readConfigs()
            val remaining = current.filter { it.triggerId != id }
            if (remaining.size == current.size) return@access false
            writeConfigs(remaining)
            recordsFile(id).delete()
            true
        }
    }

    /** The records of one watch, newest first. */
    fun records(triggerId: String): List<TriggerRecord> {
        val id = TriggerId.normalize(triggerId)
        return access { readRecords(id).records }
    }

    /** Puts [record] in front of the watch's records and drops the oldest beyond `MAX_TRIGGER_RECORDS`; returns the new count. */
    fun record(triggerId: String, record: TriggerRecord): Int {
        val id = TriggerId.normalize(triggerId)
        return access {
            val updated = readRecords(id).prepend(record)
            publish(recordsFile(id), updated.toJson())
            updated.records.size
        }
    }

    fun clearRecords(triggerId: String) {
        val id = TriggerId.normalize(triggerId)
        access { recordsFile(id).delete() }
    }

    // ------------------------------------------------------------------ files

    private fun readConfigs(): List<TriggerConfig> {
        val text = readText(configsFile) ?: return emptyList()
        return TriggerConfigsDocument.parseLenient(text).triggers
            .distinctBy { it.triggerId }
            .sortedBy { it.triggerId }
    }

    private fun writeConfigs(configs: List<TriggerConfig>) {
        publish(configsFile, TriggerConfigsDocument(configs.sortedBy { it.triggerId }).toJson())
    }

    private fun readRecords(id: String): TriggerRecordsDocument {
        val text = readText(recordsFile(id)) ?: return TriggerRecordsDocument()
        return runCatching { TriggerRecordsDocument.parse(text) }.getOrNull() ?: TriggerRecordsDocument()
    }

    private fun readText(file: File): String? {
        if (!file.isFile) return null
        require(file.length() <= MAX_FILE_BYTES) { "trigger file is too large" }
        return String(file.readBytes(), StandardCharsets.UTF_8)
    }

    /** fsync + atomic rename, so a reader sees either the old or the new document. */
    private fun publish(target: File, text: String) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_FILE_BYTES) { "trigger document is too large" }
        val temporary = File(directory, ".publish-${UUID.randomUUID()}.tmp")
        try {
            FileChannel.open(temporary.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
                val buffer = java.nio.ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            syncDirectory()
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    /** Directory fsync; not every file system offers it (Windows in the JVM tests), so a failure is ignored. */
    private fun syncDirectory() {
        runCatching { FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) } }
    }

    private fun recordsFile(id: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(id.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        return File(directory, "$RECORDS_PREFIX$digest$RECORDS_SUFFIX")
    }

    private inline fun <T> access(action: () -> T): T = synchronized(mutex) {
        try {
            require(directory.isDirectory || directory.mkdirs()) { "trigger storage is unavailable" }
            RandomAccessFile(lockFile, "rw").use { handle ->
                val lock = handle.channel.lock()
                try {
                    action()
                } finally {
                    lock.release()
                }
            }
        } catch (e: MailException) {
            throw e
        } catch (_: Exception) {
            throw MailException(MailErrorCode.INTERNAL, UNAVAILABLE_MESSAGE, retryable = false)
        }
    }

    companion object {
        const val DIRECTORY = "mail-triggers"
        const val CONFIGS_FILE = "triggers.json"
        const val LOCK_FILE = ".triggers.lock"
        const val RECORDS_PREFIX = "records-"
        const val RECORDS_SUFFIX = ".json"

        /** A generous ceiling on either file; the documents stay far below it (100 records of a few hundred bytes). */
        const val MAX_FILE_BYTES = 4 * 1024 * 1024

        const val UNAVAILABLE_MESSAGE = "the background watch store is unavailable"
    }
}
