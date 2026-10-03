package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.Context
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.AndroidKeystoreAccountCipher
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.EncryptedAccountStore
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.FileAccountRecordStorage
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The saved-account store on a device (roadmap P4.1 / P4.3): the Android Keystore master key, the
 * record files under `noBackupFilesDir`, a second store instance over the same files (the settings
 * UI and the Binder service may live in different processes), the deleted-key failure mode, and
 * `openSession(alias)` resolving the secret inside the plugin against a scripted loopback IMAP
 * server. Everything lives under a throw-away directory and a test key alias, so the real store of
 * the installed plugin is never touched.
 */
@RunWith(AndroidJUnit4::class)
class AccountStoreDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var root: File
    private lateinit var cipher: AndroidKeystoreAccountCipher

    @Before
    fun prepare() {
        root = File(context.noBackupFilesDir, "account-store-test-${UUID.randomUUID()}").also { it.mkdirs() }
        cipher = AndroidKeystoreAccountCipher(TEST_KEY_ALIAS)
        cipher.deleteKey()
    }

    @After
    fun cleanUp() {
        cipher.deleteKey()
        root.deleteRecursively()
    }

    private fun newStore(): AccountStore = EncryptedAccountStore(FileAccountRecordStorage(root), cipher)

    @Test
    fun keystoreRoundTripSharesFilesBetweenInstancesAndKeepsSecretsOutOfThem() {
        val store = newStore()
        val secret = "device-secret-中文-42".toCharArray()

        val saved = store.put("Work", ACCOUNT_JSON, SecretKind.PASSWORD, secret)

        assertTrue(secret.all { it == '\u0000' })
        assertEquals("work", saved.alias)
        val directory = File(root, FileAccountRecordStorage.DIRECTORY_NAME)
        val records = requireNotNull(directory.listFiles { file -> file.name.startsWith("account-") && file.name.endsWith(".bin") })
        assertEquals(1, records.size)
        val bytes = records.single().readBytes()
        assertFalse(bytes.containsSequence("device-secret-".toByteArray(StandardCharsets.UTF_8)))
        assertTrue(bytes.containsSequence("alice@localhost".toByteArray(StandardCharsets.UTF_8)))
        assertTrue(directory.listFiles().orEmpty().none { it.name.endsWith(".tmp") })

        // A second instance over the same directory reads what the first one wrote.
        val other = newStore()
        assertEquals("device-secret-中文-42", other.withSecret("work") { account, chars ->
            assertEquals("work", account.alias)
            String(chars)
        })
        other.setDefault("work")
        assertEquals("work", store.defaultAlias())
        assertTrue(requireNotNull(store.get("work")).isDefault)
        assertEquals(listOf("work"), store.list().map { it.alias })

        assertTrue(store.remove("work"))
        assertNull(other.get("work"))
        assertNull(other.defaultAlias())
        assertEquals(0, requireNotNull(directory.listFiles { file -> file.name.endsWith(".bin") }).size)
    }

    @Test
    fun aDeletedMasterKeyLeavesRecordsListableButUnreadableUntilTheyAreSavedAgain() {
        val store = newStore()
        store.put("work", ACCOUNT_JSON, SecretKind.PASSWORD, "first".toCharArray())

        cipher.deleteKey()

        assertEquals(listOf("work"), store.list().map { it.alias })
        val error = try {
            store.withSecret("work") { _, _ -> Unit }
            throw AssertionError("a record encrypted under a deleted key must not decrypt")
        } catch (e: MailException) {
            e
        }
        assertEquals(MailErrorCode.INTERNAL, error.code)
        assertTrue(error.message, error.message.contains("'work' cannot be read"))
        assertFalse(error.message, error.message.contains("first"))

        store.put("work", ACCOUNT_JSON, SecretKind.PASSWORD, "second".toCharArray())
        assertEquals("second", store.withSecret("work") { _, chars -> String(chars) })
    }

    @Test
    fun openSessionByAliasDecryptsInsideThePluginAndListsWithoutSecrets() {
        val server = ScriptedImapServer()
        try {
            val store = newStore()
            val json = """{"address":"alice@localhost","user":"alice","imap":{"host":"127.0.0.1","port":${server.port},"tls":"none"},"timeout":{"connect":5000,"read":10000}}"""
            store.put("smoke", json, SecretKind.PASSWORD, ALIAS_SECRET.toCharArray())
            val plugin = MailPluginBinder(context, CallerGuard.trusting(), store)

            val listed = requireNotNull(plugin.listSavedAccounts().getString(MailContract.KEY_ACCOUNTS_JSON))
            assertFalse(listed, listed.contains(ALIAS_SECRET))
            val entry = JSONArray(listed).getJSONObject(0)
            assertEquals("smoke", entry.getString("alias"))
            assertEquals("alice@localhost", entry.getString("address"))
            assertEquals("password", entry.getString("auth"))
            assertEquals("imap", entry.getString("receive"))
            assertEquals(server.port, entry.getJSONObject("imap").getInt("port"))
            assertFalse(entry.getBoolean("default"))

            val statuses = Statuses()
            val session = requireNotNull(plugin.openSession(aliasBundle("SMOKE"), statuses.callback)) { "the saved alias must open" }
            val results = Results()
            submit(session, results, "t1", "session.test")
            val (_, response) = results.next()
            assertTrue(response.toString(), response.getBoolean(MailContract.FIELD_OK))
            val imap = response.getJSONObject(MailContract.FIELD_RESULT).getJSONObject("imap")
            assertTrue(imap.toString(), imap.getBoolean("ok"))
            val login = requireNotNull(server.logins.poll(5, TimeUnit.SECONDS)) { "the server saw no LOGIN" }
            assertTrue(login, login.contains(ALIAS_SECRET))
            assertFalse(response.toString(), response.toString().contains(ALIAS_SECRET))
            session.close()
            val closed = requireNotNull(statuses.statuses.poll(5, TimeUnit.SECONDS))
            assertEquals(MailContract.STATE_CLOSED, closed.getString(MailContract.FIELD_STATE))

            // The alias form refuses inline secrets and unknown aliases through the callback.
            val withSecret = aliasBundle("smoke").apply { putString(MailContract.KEY_SECRET_PASSWORD, "x") }
            assertRefused(plugin, withSecret, MailErrorCode.INVALID_ARGUMENT)
            assertRefused(plugin, aliasBundle("nobody"), MailErrorCode.ACCOUNT_NOT_FOUND)
            assertRefused(plugin, aliasBundle("bad alias"), MailErrorCode.INVALID_ARGUMENT)
        } finally {
            server.close()
        }
    }

    private fun assertRefused(plugin: MailPluginBinder, account: Bundle, code: String) {
        val statuses = Statuses()
        assertNull(plugin.openSession(account, statuses.callback))
        val status = requireNotNull(statuses.statuses.poll(5, TimeUnit.SECONDS)) { "no closed status for $code" }
        assertEquals(MailContract.STATE_CLOSED, status.getString(MailContract.FIELD_STATE))
        val error = status.getJSONObject(MailContract.FIELD_LAST_ERROR)
        assertEquals(status.toString(), code, error.getString(MailContract.FIELD_ERROR_CODE))
        assertFalse(status.toString(), status.toString().contains(ALIAS_SECRET))
    }

    private fun aliasBundle(alias: String): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putLong(MailContract.KEY_HOST_VERSION_CODE, ThreeStampMailPlugin.REQUIRED_HOST_VERSION)
        putString(MailContract.KEY_ACCOUNT_ALIAS, alias)
    }

    private fun submit(session: IMailSession, results: Results, id: String, op: String, args: String = "{}") {
        val request = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putString(MailContract.KEY_REQUEST_JSON, """{"id":"$id","op":"$op","args":$args}""")
        }
        assertEquals(id, session.call(request, null, results.callback))
    }

    private class Results {
        val results = LinkedBlockingQueue<Pair<String, JSONObject>>()
        val callback = object : IMailCallCallback.Stub() {
            override fun onProgress(progress: Bundle?) = Unit
            override fun onResult(response: Bundle?) {
                val json = JSONObject(response?.getString(MailContract.KEY_RESPONSE_JSON).orEmpty())
                results.add(json.getString(MailContract.FIELD_ID) to json)
            }
        }

        fun next(timeoutSeconds: Long = 20): Pair<String, JSONObject> = requireNotNull(results.poll(timeoutSeconds, TimeUnit.SECONDS)) { "no onResult within ${timeoutSeconds}s" }
    }

    private class Statuses {
        val statuses = LinkedBlockingQueue<JSONObject>()
        val callback = object : IMailSessionCallback.Stub() {
            override fun onStatus(status: Bundle?) {
                statuses.add(JSONObject(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty()))
            }
        }
    }

    private fun ByteArray.containsSequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty()) return true
        return indices.any { start ->
            start <= size - candidate.size && candidate.indices.all { offset -> this[start + offset] == candidate[offset] }
        }
    }

    private companion object {
        const val TEST_KEY_ALIAS = "io.github.supermonster003.autojs6.plugin.three.stamp.mail.accounts.test"
        const val ACCOUNT_JSON = """{"address":"alice@localhost","provider":"qq"}"""
        const val ALIAS_SECRET = "alias-secret-snow-42"
    }
}
