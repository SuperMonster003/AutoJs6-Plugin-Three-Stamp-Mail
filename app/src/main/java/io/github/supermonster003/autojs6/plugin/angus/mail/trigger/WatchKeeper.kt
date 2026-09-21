package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailAccountDefaults
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.Limits
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.WatchNetworkMonitor
import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerEntryDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerEventDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerListDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecord
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerStatusDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerFilter
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchEvent
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchListener
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.Watcher
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.Watchers
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.AccountSecrets
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import org.autojs.plugin.mail.api.MailContract
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** A live `openTrigger` subscription as the keeper sees it; [MailTriggerBinder] implements it over the AIDL callback. */
internal interface TriggerSubscriber {
    val triggerId: String
    val filter: TriggerFilter

    /** One event; false once the subscriber is dead. */
    fun onMail(seq: Long, event: TriggerEventDocument, json: String): Boolean

    /** A status change; false once the subscriber is dead. */
    fun onStatus(status: TriggerStatusDocument): Boolean
}

/**
 * Runs the background watches (roadmap P8): one P5 [Watcher] per enabled [TriggerConfig] of
 * the [TriggerStore], on the saved account the config names (the secret is decrypted inside this
 * process and copied into the watcher only). [sync] reconciles the running watches with the
 * store; a watch restarts when its account, folder, mode or interval changed, keeps running
 * when only the filter changed, and stops when it was disabled or removed. Events arrive on the
 * watcher threads and are handled on the keeper's own single thread: a `message` that passes the
 * watch's filter becomes a trigger record and a `mail` event for every live subscriber whose own
 * filter matches, or a host wake-up through [HostTriggerSender] when no subscriber is alive. A
 * fatal failure (`FATAL_CODES`: wrong password, missing folder, missing account) marks the watch
 * `failed` and retries after [retryDelayMs]; connection losses are the watcher's own business
 * (`connecting` until the next connection). The [WatchNetworkMonitor] kicks the watchers on
 * network changes like the P5 session watches.
 */
internal class WatchKeeper(
    context: Context,
    private val store: TriggerStore,
    private val accounts: AccountStore,
    private val secrets: AccountSecrets,
    private val network: WatchNetworkMonitor?,
    private val sender: HostTriggerSender,
    private val defaults: () -> MailAccountOptions.Defaults,
    private val clock: () -> Long = System::currentTimeMillis,
    private val watchConfig: WatchConfig = WatchConfig(),
    private val retryDelayMs: Long = MailLimits.WATCH_BACKOFF_MAX_MS,
) {
    private val application = context.applicationContext
    private val lock = Any()
    private val kept = LinkedHashMap<String, KeptWatch>()
    private val executor = ScheduledThreadPoolExecutor(1) { runnable -> Thread(runnable, THREAD_NAME).apply { isDaemon = true } }
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /** Told (on the keeper thread) after any state, mode, error or mail change; the service updates its notification. */
    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    /** Watches that are running (connecting or connected). */
    val running: Int get() = synchronized(lock) { kept.values.count { it.isRunning } }

    /** Watches with a live connection. */
    val connected: Int get() = synchronized(lock) { kept.values.count { it.state == TriggerStatusDocument.STATE_CONNECTED } }

    /** Reconciles the running watches with the store; returns how many are running afterwards. */
    fun sync(): Int = synchronized(lock) {
        val configs = runCatching { store.list() }.getOrDefault(emptyList())
        val wanted = configs.filter { it.enabled }.associateBy { it.triggerId }
        kept.values.toList().forEach { watch ->
            val next = wanted[watch.config.triggerId]
            when {
                next == null -> {
                    watch.stop(if (configs.any { it.triggerId == watch.config.triggerId }) TriggerStatusDocument.REASON_DISABLED else TriggerStatusDocument.REASON_REMOVED)
                    kept.remove(watch.config.triggerId)
                }
                watch.config.needsRestartFor(next) -> {
                    watch.stop(TriggerStatusDocument.REASON_DISABLED, silent = true)
                    kept.remove(watch.config.triggerId)
                }
                else -> watch.config = next
            }
        }
        wanted.values.forEach { config ->
            if (config.triggerId !in kept) {
                val watch = KeptWatch(config)
                kept[config.triggerId] = watch
                watch.start()
            }
        }
        kept.values.count { it.isRunning }
    }

    /** Stops every watch (the service is going away); subscribers hear `stopped` with [reason]. */
    fun stopAll(reason: String) {
        synchronized(lock) {
            kept.values.toList().forEach { it.stop(reason) }
            kept.clear()
        }
    }

    /** Restarts a failed watch now instead of after [retryDelayMs]. */
    fun retry(triggerId: String) {
        synchronized(lock) {
            val watch = kept[triggerId] ?: return
            if (watch.state != TriggerStatusDocument.STATE_FAILED) return
            watch.retry?.cancel(false)
            watch.stop(TriggerStatusDocument.REASON_DISABLED, silent = true)
            kept.remove(triggerId)
        }
        sync()
    }

    fun status(triggerId: String): TriggerStatusDocument {
        synchronized(lock) { kept[triggerId]?.let { return it.status() } }
        val config = runCatching { store.get(triggerId) }.getOrNull()
        return stoppedStatus(triggerId, config)
    }

    fun list(): TriggerListDocument {
        val configs = runCatching { store.list() }.getOrDefault(emptyList())
        val entries = configs.map { config ->
            val (address, status) = synchronized(lock) {
                val watch = kept[config.triggerId]
                (watch?.account?.address ?: savedAddress(config.alias)) to (watch?.status() ?: stoppedStatus(config.triggerId, config))
            }
            val records = runCatching { store.records(config.triggerId).size }.getOrDefault(0)
            TriggerEntryDocument.of(config, address, records, status)
        }
        return TriggerListDocument(entries)
    }

    /**
     * Adds a subscriber to a running watch; null when accepted (the current status is sent to it
     * at once), else the refusal: `INVALID_ARGUMENT` for an unknown watch, `WATCH_CLOSED` for a
     * disabled one, `LIMIT_EXCEEDED` beyond `MAX_TRIGGER_SUBSCRIBERS`.
     */
    fun subscribe(options: TriggerOptions, subscriber: TriggerSubscriber): MailException? {
        val config = runCatching { store.get(options.triggerId) }.getOrNull()
            ?: return MailException.invalidArgument("no background watch named '${options.triggerId}'")
        if (!config.enabled) return MailException(MailErrorCode.WATCH_CLOSED, "the background watch '${config.triggerId}' is disabled", retryable = false)
        if (synchronized(lock) { kept[config.triggerId] } == null) sync()
        val watch = synchronized(lock) { kept[config.triggerId] }
            ?: return MailException(MailErrorCode.WATCH_CLOSED, "the background watch '${config.triggerId}' is not running", retryable = false)
        val status = synchronized(watch.subscribers) {
            watch.subscribers.removeAll { it === subscriber }
            if (watch.subscribers.size >= MailLimits.MAX_TRIGGER_SUBSCRIBERS) {
                return MailException(MailErrorCode.LIMIT_EXCEEDED, "the background watch '${config.triggerId}' already has ${MailLimits.MAX_TRIGGER_SUBSCRIBERS} subscribers (MAX_TRIGGER_SUBSCRIBERS)", retryable = false)
            }
            watch.subscribers += subscriber
            watch.status()
        }
        executor.execute { if (!subscriber.onStatus(status)) unsubscribe(subscriber) }
        return null
    }

    fun unsubscribe(subscriber: TriggerSubscriber) {
        synchronized(lock) { kept[subscriber.triggerId] }?.subscribers?.remove(subscriber)
    }

    /** Live subscribers of a watch (tests and the settings page). */
    fun subscribers(triggerId: String): Int = synchronized(lock) { kept[triggerId]?.subscribers?.size ?: 0 }

    private fun savedAddress(alias: String): String? = runCatching {
        accounts.get(alias)?.let { MailAccountOptions.parse(it.accountJson, it.secretKind, defaults()).address }
    }.getOrNull()

    private fun stoppedStatus(triggerId: String, config: TriggerConfig?): TriggerStatusDocument = TriggerStatusDocument(
        triggerId = triggerId,
        state = TriggerStatusDocument.STATE_STOPPED,
        reason = when {
            config == null -> TriggerStatusDocument.REASON_REMOVED
            !config.enabled -> TriggerStatusDocument.REASON_DISABLED
            else -> TriggerStatusDocument.REASON_SERVICE_STOPPED
        },
        since = config?.since ?: 0L,
    )

    private fun notifyListeners() {
        listeners.forEach { runCatching { it() } }
    }

    // ------------------------------------------------------------------ one watch

    private inner class KeptWatch(@Volatile var config: TriggerConfig) : WatchListener {

        @Volatile
        var account: MailAccount? = null

        @Volatile
        var watcher: Watcher? = null

        @Volatile
        var state: String = TriggerStatusDocument.STATE_CONNECTING

        @Volatile
        var mode: WatchMode? = null

        @Volatile
        var reason: String? = null

        @Volatile
        var lastError: MailException? = null

        @Volatile
        var since: Long = clock()

        @Volatile
        var lastMailAt: Long? = null

        @Volatile
        var retry: ScheduledFuture<*>? = null

        @Volatile
        private var ended = false

        val seq = AtomicLong()
        val subscribers = CopyOnWriteArrayList<TriggerSubscriber>()

        val isRunning: Boolean get() = !ended && state != TriggerStatusDocument.STATE_FAILED && state != TriggerStatusDocument.STATE_STOPPED

        fun start() {
            val opened = try {
                val (parsed, secret) = secrets.withUsableSecret(config.alias) { saved, chars ->
                    MailAccountOptions.parse(saved.accountJson, saved.secretKind, defaults()) to MailSecret(chars)
                }
                account = parsed
                Watchers.open(parsed, secret, config.watchOptions(parsed.receive), this, watchConfig.copy(onConnected = ::onConnected))
            } catch (e: MailException) {
                fail(e)
                return
            }
            watcher = opened
            enter(TriggerStatusDocument.STATE_CONNECTING)
            network?.attach(opened)
            opened.start()
        }

        fun stop(reason: String, silent: Boolean = false) {
            ended = true
            retry?.cancel(false)
            watcher?.let { watcher ->
                network?.detach(watcher)
                watcher.stop(Watcher.REASON_STOPPED)
            }
            this.reason = reason
            enter(TriggerStatusDocument.STATE_STOPPED)
            if (!silent) executor.execute { publishStatus() }
        }

        fun status(): TriggerStatusDocument = TriggerStatusDocument(
            triggerId = config.triggerId,
            state = state,
            mode = mode?.takeIf { state == TriggerStatusDocument.STATE_CONNECTED }?.id,
            reason = reason?.takeIf { state == TriggerStatusDocument.STATE_STOPPED },
            lastError = lastError?.toDocument(),
            since = since,
            lastMailAt = lastMailAt,
        )

        /** From the watcher thread: hand over to the keeper thread, never block. */
        override fun onEvent(event: WatchEvent) {
            executor.execute { handle(event) }
        }

        private fun onConnected(mode: WatchMode) {
            executor.execute {
                if (ended) return@execute
                this.mode = mode
                enter(TriggerStatusDocument.STATE_CONNECTED)
                publishStatus()
            }
        }

        private fun handle(event: WatchEvent) {
            if (ended && event !is WatchEvent.Closed) return
            when (event) {
                is WatchEvent.Message -> onMessage(event.message)
                is WatchEvent.Mode -> {
                    mode = event.mode
                    publishStatus()
                }
                is WatchEvent.Error -> {
                    lastError = event.error
                    if (state == TriggerStatusDocument.STATE_CONNECTED) enter(TriggerStatusDocument.STATE_CONNECTING)
                    publishStatus()
                }
                is WatchEvent.Resync -> Unit
                is WatchEvent.Closed -> {
                    if (ended) return
                    watcher?.let { network?.detach(it) }
                    if (event.reason == Watcher.REASON_ERROR) fail(lastError ?: MailException(MailErrorCode.WATCH_CLOSED, "the watch closed", retryable = false)) else {
                        reason = event.reason
                        enter(TriggerStatusDocument.STATE_STOPPED)
                        publishStatus()
                    }
                }
            }
        }

        private fun onMessage(message: MessageDocument) {
            if (!config.filter.matches(message)) return
            val now = clock()
            lastMailAt = now
            runCatching { store.record(config.triggerId, TriggerRecord.of(message, now)) }
            val event = TriggerEventDocument(
                triggerId = config.triggerId,
                alias = config.alias,
                address = account?.address.orEmpty(),
                folder = message.folder,
                message = message,
                receivedAt = now,
            )
            val json = event.toJson()
            if (Limits.utf8Length(json) <= MailContract.MAX_ENVELOPE_BYTES) {
                val seq = seq.incrementAndGet()
                subscribers.forEach { subscriber ->
                    if (!subscriber.filter.matches(message)) return@forEach
                    if (!subscriber.onMail(seq, event, json)) subscribers.remove(subscriber)
                }
                val woken = if (subscribers.isEmpty()) sender.send(config.triggerId, json) else false
                Log.i(TAG, "watch=${config.triggerId} mail folder=${message.folder} uid=${message.uid} subscribers=${subscribers.size} host=$woken")
            }
            notifyListeners()
        }

        /** A fatal failure: the watch stays `failed` until [retryDelayMs] passed or the user asks for a retry. */
        private fun fail(error: MailException) {
            lastError = error
            enter(TriggerStatusDocument.STATE_FAILED)
            retry = executor.schedule({ restartIfStillWanted() }, retryDelayMs, TimeUnit.MILLISECONDS)
            executor.execute { publishStatus() }
        }

        private fun restartIfStillWanted() {
            synchronized(lock) {
                if (kept[config.triggerId] !== this || ended) return
                kept.remove(config.triggerId)
            }
            sync()
        }

        private fun enter(next: String) {
            if (state != next) {
                state = next
                since = clock()
                Log.i(TAG, "watch=${config.triggerId} state=$next mode=${mode?.id ?: "-"} reason=${reason ?: "-"} error=${lastError?.code ?: "-"}")
            }
        }

        private fun publishStatus() {
            val status = status()
            subscribers.forEach { subscriber -> if (!subscriber.onStatus(status)) subscribers.remove(subscriber) }
            notifyListeners()
        }
    }

    companion object {
        const val THREAD_NAME = "angus-mail-trigger"

        @Volatile
        private var instance: WatchKeeper? = null

        private const val TAG = "MailWatchKeeper"

        fun of(context: Context): WatchKeeper = instance ?: synchronized(this) {
            instance ?: WatchKeeper(
                context = context.applicationContext,
                store = TriggerStores.of(context),
                accounts = AccountStores.of(context),
                secrets = AccountSecrets.of(context),
                network = WatchNetworkMonitor.of(context),
                sender = HostTriggerSender(context),
                defaults = { context.applicationContext.angusMailAccountDefaults() },
            ).also { instance = it }
        }

        /** A restart is needed when anything the watcher was opened with changed; the filter is read per message. */
        private fun TriggerConfig.needsRestartFor(next: TriggerConfig): Boolean =
            alias != next.alias || folder != next.folder || mode != next.mode || pollIntervalMs != next.pollIntervalMs
    }
}
