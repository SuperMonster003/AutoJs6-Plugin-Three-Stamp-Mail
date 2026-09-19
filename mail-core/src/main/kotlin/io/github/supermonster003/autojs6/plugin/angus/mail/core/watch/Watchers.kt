package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException

/** Chooses the watcher for an account (roadmap D3 / D7): IMAP idles unless `mode: "poll"` was asked for; POP3 polling follows. */
object Watchers {

    /** A watcher that is not started yet; the caller starts it and stops it. */
    fun open(account: MailAccount, secret: MailSecret, options: WatchOptions, listener: WatchListener, config: WatchConfig = WatchConfig()): Watcher {
        if (account.receiveEndpoint == null) throw MailException.invalidArgument("account ${account.address} has no ${account.receive.id} endpoint")
        return when {
            account.receive == MailProtocol.POP3 -> throw MailException.unsupported("POP3 accounts cannot be watched yet (roadmap P5 PollWatcher)")
            options.mode == WatchMode.POLL -> PollWatcher(account, secret, options, listener, config, ImapPollSource(account, secret, options.folder, options.fetchBody))
            else -> IdleWatcher(account, secret, options, listener, config)
        }
    }
}
