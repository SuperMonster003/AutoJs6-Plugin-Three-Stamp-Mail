package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.Pop3Mailbox

/**
 * Chooses the watcher for an account (roadmap D3 / D7 / D38): POP3 polls, IMAP idles unless
 * `mode: "poll"` was asked for or the preset says the server does not push through IDLE
 * (`idlePush = false`: QQ and Sina accept IDLE but stay silent, 163 and 126 have no IDLE) and
 * the mode was left to `auto`; an explicit `mode: "idle"` still idles.
 */
object Watchers {

    /** A watcher that is not started yet; the caller starts it and stops it. */
    fun open(account: MailAccount, secret: MailSecret, options: WatchOptions, listener: WatchListener, config: WatchConfig = WatchConfig()): Watcher {
        if (account.receiveEndpoint == null) throw MailException.invalidArgument("account ${account.address} has no ${account.receive.id} endpoint")
        return when {
            account.receive == MailProtocol.POP3 -> {
                Pop3Mailbox.checkFolder(options.folder)
                if (options.mode == WatchMode.IDLE) throw MailException.unsupported("POP3 accounts have no IDLE; watch with mode 'poll' (roadmap D3)")
                PollWatcher(account, secret, options, listener, config, Pop3PollSource(account, secret, options.fetchBody))
            }
            options.mode == WatchMode.POLL || (options.mode == null && account.provider?.idlePush == false) ->
                PollWatcher(account, secret, options, listener, config, ImapPollSource(account, secret, options.folder, options.fetchBody))
            else -> IdleWatcher(account, secret, options, listener, config)
        }
    }
}
