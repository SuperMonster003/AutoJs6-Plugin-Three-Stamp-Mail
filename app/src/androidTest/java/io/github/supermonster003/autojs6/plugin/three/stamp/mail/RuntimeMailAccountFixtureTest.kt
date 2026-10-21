package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.AccountStores
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Seeds only a unique synthetic loopback alias; never edits an existing account. */
class RuntimeMailAccountFixtureTest {
    @Test fun seedSyntheticAccount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val args = InstrumentationRegistry.getArguments()
        val store = AccountStores.of(context)
        val alias = "runtime-mail-fixture"
        assertFalse("Existing alias must not be replaced", store.has(alias))
        fun endpoint(key: String) = JSONObject().put("host", "127.0.0.1")
            .put("port", requireNotNull(args.getString(key)).toInt().also { require(it in 1024..65535) })
            .put("tls", "none")
        val account = JSONObject().put("address", "runtime@example.invalid")
            .put("auth", "password").put("receive", "imap")
            .put("imap", endpoint("imapPort")).put("smtp", endpoint("smtpPort"))
        store.put(alias, account.toString(), SecretKind.PASSWORD, "runtime-mail-synthetic".toCharArray())
        assertTrue(store.has(alias))
        assertFalse(store.get(alias)!!.accountJson.contains("runtime-mail-synthetic"))
    }
}
