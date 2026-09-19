package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.imapUid
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * Manual probe of a watch against a real account (mail roadmap P5 evidence): skipped unless the
 * git-ignored `mail-test-accounts.properties` and `build/p5/probe.properties` (profile, mode,
 * seconds) exist. Prints only the redacted protocol trace, event types, UIDs and subjects to
 * `build/p5/probe-jvm.log`; the sender runs separately (`build/p5/idle_probe.py` and friends).
 */
class RealProviderWatchProbe {

    @Test
    fun watchARealAccount() {
        val accounts = File("../mail-test-accounts.properties")
        val settings = File("../build/p5/probe.properties")
        assumeTrue("no real accounts / probe settings", accounts.isFile && settings.isFile)
        val props = Properties().apply { accounts.inputStream().use { load(it) } }
        val config = Properties().apply { settings.inputStream().use { load(it) } }
        val profile = config.getProperty("profile", "QQ_A")
        val mode = config.getProperty("mode", "poll")
        val seconds = config.getProperty("seconds", "150").toLong()
        val interval = config.getProperty("pollIntervalMs", "15000").toLong()
        val (kind, letter) = profile.split("_")
        val address = props.getProperty("${kind}_USER_NAME_$letter")
        val secret = props.getProperty("${kind}_AUTH_CODE_$letter")
        val domain = address.substringAfterLast('@').lowercase()
        val provider = ProviderPresets.all.first { domain in it.domains }
        val account = MailAccountOptions.parse("""{"provider":"${provider.id}","address":"$address","debug":true}""", SecretKind.PASSWORD)
        val log = File("../build/p5/probe-jvm.log").printWriter()
        fun out(line: String) { println(line); log.println(line); log.flush() }
        out("probe provider=${provider.id} mode=$mode seconds=$seconds interval=$interval")
        val options = WatchOptions(folder = "INBOX", mode = WatchMode.fromId(mode), pollIntervalMs = interval)
        val started = System.currentTimeMillis()
        val watcher = Watchers.open(account, MailSecret(secret.toCharArray()), options, { event ->
            val text = when (event) {
                is WatchEvent.Message -> "message uid=${event.message.imapUid} subject=${event.message.subject}"
                is WatchEvent.Error -> "error ${event.error.code} ${event.error.message}"
                else -> event.toString()
            }
            out("+${System.currentTimeMillis() - started}ms [event] $text")
        })
        watcher.start()
        val deadline = started + seconds * 1000
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(2000)
            (watcher as AbstractWatcher).trace.drain().forEach { out("[trace] $it") }
        }
        watcher.stop()
        Thread.sleep(500)
        (watcher as AbstractWatcher).trace.drain().forEach { out("[trace] $it") }
        out("probe done pollCount=${(watcher as? PollWatcher)?.pollCount} idleCount=${(watcher as? IdleWatcher)?.idleCount}")
        log.close()
    }
}
