package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.Redactor

/**
 * The redacted protocol summary of one session (roadmap D28, `options.debug`). The mail core
 * writes one line per command it issues (protocol, command name, outcome, elapsed time) instead
 * of enabling Jakarta's `mail.debug`, so no raw protocol data and no credential can ever appear;
 * every line still passes through the [Redactor] and is cut to [MailLimits.MAX_TRACE_LINE_LENGTH].
 * The app drains the buffer after each operation and forwards it through `onProgress`.
 */
class ProtocolTrace(
    val enabled: Boolean,
    private val redactor: Redactor,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val startedAt = clock()
    private val lines = ArrayDeque<String>()

    /** Lines dropped because the buffer was full; reported so a reader knows the trace has a gap. */
    var dropped: Long = 0
        private set

    val size: Int get() = synchronized(lines) { lines.size }

    fun record(protocol: String, summary: String) {
        if (!enabled) return
        val line = redactor.scrub("+${clock() - startedAt}ms $protocol $summary").take(MailLimits.MAX_TRACE_LINE_LENGTH)
        synchronized(lines) {
            if (lines.size >= MailLimits.MAX_TRACE_LINES) {
                lines.removeFirst()
                dropped++
            }
            lines.addLast(line)
        }
    }

    /** Runs [block] and records `command ok 12ms` or `command failed ExceptionType 12ms`. */
    fun <T> timed(protocol: String, command: String, block: () -> T): T {
        if (!enabled) return block()
        val started = clock()
        try {
            return block().also { record(protocol, "$command ok ${clock() - started}ms") }
        } catch (e: Throwable) {
            record(protocol, "$command failed ${e.javaClass.simpleName} ${clock() - started}ms")
            throw e
        }
    }

    /** Removes and returns every buffered line. */
    fun drain(): List<String> = synchronized(lines) {
        val copy = lines.toList()
        lines.clear()
        copy
    }

    fun snapshot(): List<String> = synchronized(lines) { lines.toList() }

    companion object {
        /** A trace that records nothing; for sessions without `debug` and for tests. */
        fun disabled(): ProtocolTrace = ProtocolTrace(false, Redactor("", null))
    }
}
