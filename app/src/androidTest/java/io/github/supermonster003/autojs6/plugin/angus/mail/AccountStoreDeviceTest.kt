package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AndroidKeystoreAccountCipher
import io.github.supermonster003.autojs6.plugin.angus.mail.store.EncryptedAccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.FileAccountRecordStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * The saved-account store on a device (roadmap P4.1): the Android Keystore master key, the record
 * files under `noBackupFilesDir`, a second store instance over the same files (the settings UI and
 * the Binder service may live in different processes) and the deleted-key failure mode. Everything
 * lives under a throw-away directory and a test key alias, so the real store of the installed
 * plugin is never touched.
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

        assertTrue(secret.all { it.code == 0 })
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

    private fun ByteArray.containsSequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty()) return true
        return indices.any { start ->
            start <= size - candidate.size && candidate.indices.all { offset -> this[start + offset] == candidate[offset] }
        }
    }

    private companion object {
        const val TEST_KEY_ALIAS = "io.github.supermonster003.autojs6.plugin.angus.mail.accounts.test"
        const val ACCOUNT_JSON = """{"address":"alice@localhost","provider":"qq"}"""
    }
}
