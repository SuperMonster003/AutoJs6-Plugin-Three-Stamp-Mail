package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract

internal enum class AutoJs6HostAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    DISABLED,
    CONTRACT_UNAVAILABLE,
}

internal enum class AutoJs6DarkModePolicy {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
}

internal data class AutoJs6HostSettingsSnapshot(
    val hostVersionCode: Long,
    val hostVersionName: String,
    val themeColorPrimary: Int,
    val themeColorPrimaryDark: Int,
    val themeColorAccent: Int,
    val darkModePolicy: AutoJs6DarkModePolicy,
    val darkModeActive: Boolean,
    val languageTag: String,
    val resolvedLanguageTag: String,
)

internal data class AutoJs6HostSettingsResult(
    val availability: AutoJs6HostAvailability,
    val snapshot: AutoJs6HostSettingsSnapshot? = null,
) {
    init {
        require((availability == AutoJs6HostAvailability.AVAILABLE) == (snapshot != null))
    }

    val selectable: Boolean
        get() = availability == AutoJs6HostAvailability.AVAILABLE

    val definitiveAbsence: Boolean
        get() = availability == AutoJs6HostAvailability.NOT_INSTALLED ||
            availability == AutoJs6HostAvailability.DISABLED
}

/** Client for AutoJs6's versioned, read-only official-plugin settings contract. */
internal object AutoJs6HostSettingsClient {
    private val settingsUri = Uri.parse(AutoJs6HostSettingsContract.CONTENT_URI)

    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "host-appearance") }
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val listeners = java.util.concurrent.CopyOnWriteArraySet<() -> Unit>()
    private val lock = Any()
    @Volatile private var cached = AutoJs6HostSettingsResult(AutoJs6HostAvailability.CONTRACT_UNAVAILABLE)
    private var inFlight = false
    private var again = false
    private var generation = 0L
    private var lastCompleted = 0L
    private val completions = mutableListOf<() -> Unit>()

    /** Cache-only on the calling thread. Package lookup, acquisition and Binder IPC run on worker. */
    fun query(context: Context): AutoJs6HostSettingsResult {
        refresh(context)
        return cached
    }

    fun addListener(listener: () -> Unit) { listeners += listener }
    fun removeListener(listener: () -> Unit) { listeners -= listener }

    fun refresh(context: Context, force: Boolean = false, onComplete: (() -> Unit)? = null) {
        val app = context.applicationContext
        val request = synchronized(lock) {
            if (inFlight) {
                onComplete?.let(completions::add)
                if (force) { generation++; again = true }
                return
            }
            if (!force && android.os.SystemClock.elapsedRealtime() - lastCompleted < 1_000L) {
                onComplete?.let { main.post(it) }
                return
            }
            onComplete?.let(completions::add)
            inFlight = true
            ++generation
        }
        worker.execute {
            val fresh = runCatching { queryBlocking(app) }.getOrElse {
                AutoJs6HostSettingsResult(AutoJs6HostAvailability.CONTRACT_UNAVAILABLE)
            }
            var changed = false
            val callbacks = mutableListOf<() -> Unit>()
            val repeat = synchronized(lock) {
                if (request == generation) {
                    changed = cached != fresh
                    cached = fresh // An unavailable host clears the prior snapshot; never overwrite local choices.
                    callbacks += completions
                    completions.clear()
                }
                lastCompleted = android.os.SystemClock.elapsedRealtime()
                inFlight = false
                again.also { again = false }
            }
            main.post {
                if (changed) listeners.forEach { it() }
                callbacks.forEach { it() }
            }
            if (repeat) refresh(app, force = true)
        }
    }

    @androidx.annotation.WorkerThread
    private fun queryBlocking(context: Context): AutoJs6HostSettingsResult {
        check(android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) { "Host appearance IPC must not run on the UI thread" }
        val installed = inspectHostPackage(context)
        if (installed != AutoJs6HostAvailability.AVAILABLE) {
            return AutoJs6HostSettingsResult(installed)
        }
        val bundle = runCatching {
            context.contentResolver.call(
                settingsUri,
                AutoJs6HostSettingsContract.METHOD_GET_SETTINGS,
                null,
                null,
            )
        }.getOrNull() ?: return AutoJs6HostSettingsResult(
            AutoJs6HostAvailability.CONTRACT_UNAVAILABLE,
        )
        val snapshot = runCatching {
            require(
                bundle.getInt(AutoJs6HostSettingsContract.KEY_PROTOCOL_VERSION, 0) ==
                    AutoJs6HostSettingsContract.PROTOCOL_VERSION,
            )
            require(
                bundle.getString(AutoJs6HostSettingsContract.KEY_HOST_PACKAGE_NAME) ==
                    AutoJs6HostSettingsContract.HOST_PACKAGE_NAME,
            )
            require(bundle.containsKey(AutoJs6HostSettingsContract.KEY_THEME_COLOR_PRIMARY))
            require(bundle.containsKey(AutoJs6HostSettingsContract.KEY_THEME_COLOR_PRIMARY_DARK))
            require(bundle.containsKey(AutoJs6HostSettingsContract.KEY_THEME_COLOR_ACCENT))
            AutoJs6HostSettingsSnapshot(
                hostVersionCode = bundle.getLong(
                    AutoJs6HostSettingsContract.KEY_HOST_VERSION_CODE,
                    0L,
                ),
                hostVersionName = bundle.getString(
                    AutoJs6HostSettingsContract.KEY_HOST_VERSION_NAME,
                ).orEmpty(),
                themeColorPrimary = bundle.getInt(
                    AutoJs6HostSettingsContract.KEY_THEME_COLOR_PRIMARY,
                ),
                themeColorPrimaryDark = bundle.getInt(
                    AutoJs6HostSettingsContract.KEY_THEME_COLOR_PRIMARY_DARK,
                ),
                themeColorAccent = bundle.getInt(
                    AutoJs6HostSettingsContract.KEY_THEME_COLOR_ACCENT,
                ),
                darkModePolicy = AutoJs6DarkModePolicy.valueOf(
                    bundle.getString(AutoJs6HostSettingsContract.KEY_DARK_MODE_POLICY).orEmpty(),
                ),
                darkModeActive = bundle.getBoolean(
                    AutoJs6HostSettingsContract.KEY_DARK_MODE_ACTIVE,
                ),
                languageTag = bundle.getString(
                    AutoJs6HostSettingsContract.KEY_LANGUAGE_TAG,
                ).orEmpty(),
                resolvedLanguageTag = bundle.getString(
                    AutoJs6HostSettingsContract.KEY_RESOLVED_LANGUAGE_TAG,
                ).orEmpty(),
            )
        }.getOrNull() ?: return AutoJs6HostSettingsResult(
            AutoJs6HostAvailability.CONTRACT_UNAVAILABLE,
        )
        return AutoJs6HostSettingsResult(AutoJs6HostAvailability.AVAILABLE, snapshot)
    }

    private fun inspectHostPackage(context: Context): AutoJs6HostAvailability {
        val packageManager = context.packageManager
        val applicationInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    AutoJs6HostSettingsContract.HOST_PACKAGE_NAME,
                    PackageManager.ApplicationInfoFlags.of(
                        PackageManager.MATCH_DISABLED_COMPONENTS.toLong(),
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(
                    AutoJs6HostSettingsContract.HOST_PACKAGE_NAME,
                    PackageManager.MATCH_DISABLED_COMPONENTS,
                )
            }
        }.getOrNull() ?: return AutoJs6HostAvailability.NOT_INSTALLED
        val enabled = when (
            packageManager.getApplicationEnabledSetting(
                AutoJs6HostSettingsContract.HOST_PACKAGE_NAME,
            )
        ) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            -> false
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            else -> applicationInfo.enabled
        }
        return if (enabled) AutoJs6HostAvailability.AVAILABLE else AutoJs6HostAvailability.DISABLED
    }
}
