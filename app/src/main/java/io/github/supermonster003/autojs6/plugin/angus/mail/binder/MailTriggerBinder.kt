package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerEventDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerStatusDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerFilter
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.TriggerSubscriber
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.WatchKeeper
import org.autojs.plugin.mail.api.IMailTrigger
import org.autojs.plugin.mail.api.IMailTriggerCallback
import org.autojs.plugin.mail.api.MailContract
import java.util.concurrent.atomic.AtomicBoolean

/**
 * `IMailTrigger` (contract version 2, roadmap P8): one subscription of the host to a background
 * watch of the [WatchKeeper]. Events and status changes reach the host's `oneway` callback with
 * the host's `generation` and the watch's `seq`; the host's death (`linkToDeath`), `stop` and
 * a dead callback end the subscription, never the watch. Every method checks the owner UID like
 * the session Binder does.
 */
internal class MailTriggerBinder(
    override val triggerId: String,
    private val generation: Long,
    @Volatile override var filter: TriggerFilter,
    private val callback: IMailTriggerCallback,
    private val guard: CallerGuard,
    private val ownerUid: Int,
    private val keeper: WatchKeeper,
) : IMailTrigger.Stub(), TriggerSubscriber {

    private val hostDeath = IBinder.DeathRecipient { end() }
    private val linked = AtomicBoolean(false)
    private val ended = AtomicBoolean(false)

    /** Links the host's death; false when the host is already gone. */
    fun link(): Boolean = try {
        callback.asBinder().linkToDeath(hostDeath, 0)
        linked.set(true)
        true
    } catch (_: RemoteException) {
        false
    }

    // ------------------------------------------------------------------ keeper side

    override fun onMail(seq: Long, event: TriggerEventDocument, json: String): Boolean {
        if (ended.get()) return false
        return try {
            callback.onMail(generation, seq, MailBundles.json(MailContract.KEY_EVENT_JSON, json))
            true
        } catch (_: DeadObjectException) {
            end()
            false
        } catch (_: RemoteException) {
            true
        }
    }

    override fun onStatus(status: TriggerStatusDocument): Boolean {
        if (ended.get()) return false
        return try {
            callback.onStatus(MailBundles.json(MailContract.KEY_STATUS_JSON, status.toJson()))
            true
        } catch (_: DeadObjectException) {
            end()
            false
        } catch (_: RemoteException) {
            true
        }
    }

    // ------------------------------------------------------------------ AIDL

    override fun getStatus(): Bundle {
        guard.enforceOwner(ownerUid)
        return MailBundles.json(MailContract.KEY_STATUS_JSON, keeper.status(triggerId).toJson())
    }

    /** Replaces the subscriber's filter; the options must name this subscription's watch. */
    override fun update(options: Bundle?) {
        guard.enforceOwner(ownerUid)
        val parsed = try {
            TriggerOptions.parse(options?.getString(MailContract.KEY_TRIGGER_OPTIONS_JSON))
        } catch (e: MailException) {
            throw IllegalArgumentException(e.message)
        }
        require(parsed.triggerId == triggerId) { "the subscription belongs to the background watch '$triggerId', not '${parsed.triggerId}'" }
        filter = parsed.filter
    }

    override fun stop() {
        guard.enforceOwner(ownerUid)
        end()
    }

    private fun end() {
        if (!ended.compareAndSet(false, true)) return
        if (linked.compareAndSet(true, false)) runCatching { callback.asBinder().unlinkToDeath(hostDeath, 0) }
        keeper.unsubscribe(this)
    }
}
