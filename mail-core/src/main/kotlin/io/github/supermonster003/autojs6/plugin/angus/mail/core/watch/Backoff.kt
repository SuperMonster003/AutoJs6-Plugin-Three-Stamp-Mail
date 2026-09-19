package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

/**
 * Reconnect delays of a watch (roadmap D17): [minMs] doubling per attempt up to [maxMs], each
 * drawn from the upper half of the current ceiling so that the watches of one account do not
 * reconnect in lockstep after a network change. [reset] once a connection worked.
 */
class Backoff(
    private val minMs: Long,
    private val maxMs: Long,
    private val random: (Long) -> Long,
) {
    init {
        require(minMs > 0 && maxMs >= minMs) { "backoff range must be positive and ordered: $minMs..$maxMs" }
    }

    /** Failed attempts since the last [reset]. */
    var attempts: Int = 0
        private set

    /** The ceiling of attempt [attempt] (0-based): `min * 2^attempt`, capped at [maxMs]. */
    fun ceilingFor(attempt: Int): Long = if (attempt >= 30) maxMs else minOf(maxMs, minMs shl attempt)

    /** The next delay in milliseconds, between half of the current ceiling and the ceiling itself. */
    fun next(): Long {
        val ceiling = ceilingFor(attempts)
        attempts++
        val floor = ceiling / 2
        return floor + random(ceiling - floor + 1)
    }

    fun reset() {
        attempts = 0
    }
}
