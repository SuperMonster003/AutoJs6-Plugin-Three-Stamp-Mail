package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import java.util.concurrent.ThreadLocalRandom

/** Timing knobs of the watchers, defaulting to the roadmap D17 values; the JVM tests shorten them. */
data class WatchConfig(
    val backoffMinMs: Long = MailLimits.WATCH_BACKOFF_MIN_MS,
    val backoffMaxMs: Long = MailLimits.WATCH_BACKOFF_MAX_MS,
    val idleRenewMs: Long = MailLimits.IDLE_RENEW_MS,
    val idleHealthyMs: Long = MailLimits.IDLE_HEALTHY_MS,
    val idleFailureLimit: Int = MailLimits.IDLE_FAILURE_LIMIT,
    val clock: () -> Long = System::currentTimeMillis,
    /** A value in `0 until bound`, for the reconnect jitter. */
    val random: (Long) -> Long = { bound -> ThreadLocalRandom.current().nextLong(bound) },
)
