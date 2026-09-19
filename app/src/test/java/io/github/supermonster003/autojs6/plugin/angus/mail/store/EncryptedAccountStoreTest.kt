package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The saved-account store over an in-memory storage and a JVM AES-GCM cipher (roadmap P4.1). */
class EncryptedAccountStoreTest {

    @Test
    fun putStoresCiphertextOnlyWipesTheInputAndReleasesTheSecretToTheCallback() {
        val storage = MemoryAccountRecordStorage()
        val clock = AtomicInteger(1000)
        val store = EncryptedAccountStore(storage, JvmAesGcmAccountCipher()) { clock.get().toLong() }
        val secret = "authorization-code-snow".toCharArray()

        val saved = store.put(" Work ", PRETTY_JSON, SecretKind.PASSWORD, secret)

        assertTrue(secret.all { it == '\u0000' })
        assertEquals("work", saved.alias)
        assertEquals(COMPACT_JSON, saved.accountJson)
        assertEquals(SecretKind.PASSWORD, saved.secretKind)
        assertEquals(1000L, saved.updatedAt)
        assertFalse(saved.isDefault)
        assertEquals("SavedAccount(alias=work, secretKind=PASSWORD, default=false)", saved.toString())

        val persisted = requireNotNull(storage.raw("work"))
        assertFalse(persisted.containsSequence("authorization-code-snow".toByteArray(StandardCharsets.UTF_8)))
        assertTrue(persisted.containsSequence(COMPACT_JSON.toByteArray(StandardCharsets.UTF_8)))

        lateinit var observed: CharArray
        val result = store.withSecret("WORK") { account, chars ->
            observed = chars
            assertEquals("work", account.alias)
            assertEquals(COMPACT_JSON, account.accountJson)
            String(chars)
        }
        assertEquals("authorization-code-snow", result)
        assertTrue(observed.all { it == '\u0000' })
        assertEquals("work", requireNotNull(store.get("work")).alias)
        assertTrue(store.has("work"))
        assertFalse(store.has("home"))
    }

    @Test
    fun secretIsWipedWhenTheCallbackThrowsAndItsErrorIsKept() {
        val store = storeWith("work")
        lateinit var observed: CharArray
        val expected = MarkerException()
        val actual = assertThrows(MarkerException::class.java) {
            store.withSecret("work") { _, chars ->
                observed = chars
                throw expected
            }
        }
        assertSame(expected, actual)
        assertTrue(observed.all { it == '\u0000' })
    }

    @Test
    fun putReplacesTheRecordAndKeepsTheDefaultMark() {
        val storage = MemoryAccountRecordStorage()
        val store = EncryptedAccountStore(storage, JvmAesGcmAccountCipher())
        store.put("work", COMPACT_JSON, SecretKind.PASSWORD, "one".toCharArray())
        store.setDefault("work")

        val replaced = store.put("work", TOKEN_JSON, SecretKind.ACCESS_TOKEN, "ya29.token".toCharArray())

        assertTrue(replaced.isDefault)
        assertEquals(SecretKind.ACCESS_TOKEN, replaced.secretKind)
        assertEquals("ya29.token", store.withSecret("work") { _, chars -> String(chars) })
        assertEquals(1, store.list().size)
    }

    @Test
    fun refusesBadAliasesDocumentsAndSecrets() {
        val store = EncryptedAccountStore(MemoryAccountRecordStorage(), JvmAesGcmAccountCipher())

        fun assertRefused(code: String, label: String, block: () -> Unit) {
            val error = assertThrows(label, MailException::class.java, block)
            assertEquals(label, code, error.code)
        }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "blank alias") { store.put(" ", COMPACT_JSON, SecretKind.PASSWORD, "s".toCharArray()) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "alias with slash") { store.put("a/b", COMPACT_JSON, SecretKind.PASSWORD, "s".toCharArray()) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "no secret kind") { store.put("work", COMPACT_JSON, SecretKind.NONE, "s".toCharArray()) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "empty secret") { store.put("work", COMPACT_JSON, SecretKind.PASSWORD, CharArray(0)) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "lone surrogate") { store.put("work", COMPACT_JSON, SecretKind.PASSWORD, charArrayOf('\uD800')) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "oversized secret") {
            store.put("work", COMPACT_JSON, SecretKind.PASSWORD, CharArray(AccountEnvelopeLimits.MAXIMUM_SECRET_BYTES + 1) { 'x' })
        }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "secret inside JSON") {
            store.put("work", """{"address":"alice@example.com","provider":"qq","password":"x"}""", SecretKind.PASSWORD, "s".toCharArray())
        }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "unknown field") {
            store.put("work", """{"address":"alice@example.com","provider":"qq","alias":"work"}""", SecretKind.PASSWORD, "s".toCharArray())
        }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "not an object") { store.put("work", "[]", SecretKind.PASSWORD, "s".toCharArray()) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "not JSON") { store.put("work", "{", SecretKind.PASSWORD, "s".toCharArray()) }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "no endpoint") { store.put("work", """{"address":"alice@example.com"}""", SecretKind.PASSWORD, "s".toCharArray()) }
        assertRefused(MailErrorCode.PROVIDER_UNKNOWN, "unknown provider") {
            store.put("work", """{"address":"alice@example.com","provider":"nope"}""", SecretKind.PASSWORD, "s".toCharArray())
        }
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "auth mismatch") {
            store.put("work", """{"address":"alice@example.com","provider":"qq","auth":"xoauth2"}""", SecretKind.PASSWORD, "s".toCharArray())
        }
        assertRefused(MailErrorCode.AUTH_MECHANISM_UNSUPPORTED, "provider rejects token") {
            store.put("work", COMPACT_JSON, SecretKind.ACCESS_TOKEN, "s".toCharArray())
        }
        assertTrue(store.list().isEmpty())
        assertNull(store.get("work"))
        assertRefused(MailErrorCode.INVALID_ARGUMENT, "get with bad alias") { store.get("a b") }
        assertRefused(MailErrorCode.ACCOUNT_NOT_FOUND, "secret of unknown alias") { store.withSecret("work") { _, _ -> Unit } }
        assertRefused(MailErrorCode.ACCOUNT_NOT_FOUND, "default of unknown alias") { store.setDefault("work") }
    }

    @Test
    fun detectsTamperingWithTheEnvelopeAliasAndDocument() {
        val storage = MemoryAccountRecordStorage()
        val store = EncryptedAccountStore(storage, JvmAesGcmAccountCipher())
        store.put("work", COMPACT_JSON, SecretKind.PASSWORD, "secret".toCharArray())
        val original = requireNotNull(storage.raw("work"))

        // A flipped ciphertext byte fails the GCM tag.
        storage.mutate("work") { bytes -> bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte() }
        assertDamaged(store, "work")
        assertEquals("work", requireNotNull(store.get("work")).alias)

        // The same envelope under another alias's file: the record names its own alias.
        storage.replace("work", original)
        storage.copy("work", "home")
        assertDamaged(store, "home")
        assertEquals(listOf("work"), store.list().map { it.alias })

        // The non-secret document edited on disk: the associated data no longer matches.
        val envelope = AccountRecordCodec.decode(original)
        val redirected = AccountEnvelope(
            envelope.alias,
            envelope.accountJson.replace("alice@example.com", "mallory@example.com"),
            envelope.secretKind,
            envelope.updatedAt,
            envelope.copyInitializationVector(),
            envelope.copyCiphertext(),
        )
        storage.replace("work", AccountRecordCodec.encode(redirected))
        assertDamaged(store, "work")

        // A record that is not decodable at all is skipped by list and reported by get; the copy under
        // 'home' no longer reads back through its own alias, so it is skipped too but still removable.
        storage.replace("work", byteArrayOf(1, 2, 3))
        assertEquals(emptyList<String>(), store.list().map { it.alias })
        assertEquals(MailErrorCode.INTERNAL, assertThrows(MailException::class.java) { store.get("work") }.code)
        assertTrue(store.remove("work"))
        assertNull(store.get("work"))
        assertTrue(store.remove("home"))
        assertFalse(store.remove("home"))
    }

    @Test
    fun defaultMarkFollowsSetDefaultAndRemove() {
        val store = EncryptedAccountStore(MemoryAccountRecordStorage(), JvmAesGcmAccountCipher())
        store.put("work", COMPACT_JSON, SecretKind.PASSWORD, "a".toCharArray())
        store.put("home", COMPACT_JSON, SecretKind.PASSWORD, "b".toCharArray())
        assertNull(store.defaultAlias())
        assertEquals(listOf(false, false), store.list().map { it.isDefault })

        store.setDefault("HOME")
        assertEquals("home", store.defaultAlias())
        assertEquals(listOf("home" to true, "work" to false), store.list().map { it.alias to it.isDefault })
        assertTrue(requireNotNull(store.get("home")).isDefault)
        assertTrue(store.withSecret("home") { account, _ -> account.isDefault })

        store.setDefault("work")
        assertEquals("work", store.defaultAlias())
        store.setDefault(null)
        assertNull(store.defaultAlias())

        store.setDefault("work")
        assertTrue(store.remove("work"))
        assertNull(store.defaultAlias())
        assertFalse(store.remove("work"))
        assertEquals(listOf("home"), store.list().map { it.alias })
    }

    @Test
    fun storageFailuresNeverLeakTheirText() {
        val store = EncryptedAccountStore(ThrowingAccountRecordStorage("secret-path-text"), JvmAesGcmAccountCipher())
        listOf<() -> Any?>(
            { store.put("work", COMPACT_JSON, SecretKind.PASSWORD, "s".toCharArray()) },
            { store.get("work") },
            { store.list() },
            { store.remove("work") },
            { store.setDefault(null) },
            { store.defaultAlias() },
            { store.withSecret("work") { _, _ -> Unit } },
        ).forEach { call ->
            val error = assertThrows(MailException::class.java) { call() }
            assertEquals(MailErrorCode.INTERNAL, error.code)
            assertEquals("the saved-account store is unavailable", error.message)
            assertNull(error.details)
            assertFalse(error.retryable)
        }
    }

    @Test
    fun listIsSortedByAlias() {
        val store = EncryptedAccountStore(MemoryAccountRecordStorage(), JvmAesGcmAccountCipher())
        listOf("zeta", "alpha", "Mid").forEach { alias -> store.put(alias, COMPACT_JSON, SecretKind.PASSWORD, "s".toCharArray()) }
        assertEquals(listOf("alpha", "mid", "zeta"), store.list().map { it.alias })
    }

    private fun assertDamaged(store: AccountStore, alias: String) {
        val error = assertThrows(MailException::class.java) { store.withSecret(alias) { _, _ -> Unit } }
        assertEquals(MailErrorCode.INTERNAL, error.code)
        assertTrue(error.message, error.message.contains("'$alias' cannot be read"))
    }

    private fun storeWith(alias: String): AccountStore =
        EncryptedAccountStore(MemoryAccountRecordStorage(), JvmAesGcmAccountCipher()).also { store ->
            store.put(alias, COMPACT_JSON, SecretKind.PASSWORD, "secret".toCharArray())
        }

    private class MemoryAccountRecordStorage : AccountRecordStorage {
        private val records = LinkedHashMap<String, ByteArray>()
        private var defaultAlias: String? = null

        override fun <T> withExclusiveAccess(action: (AccountRecordAccess) -> T): T = synchronized(records) {
            action(
                object : AccountRecordAccess {
                    override fun read(alias: String): ByteArray? = records[alias]?.copyOf()

                    override fun readAll(): List<ByteArray> = records.values.map { it.copyOf() }

                    override fun write(alias: String, encodedRecord: ByteArray) {
                        records.put(alias, encodedRecord.copyOf())?.fill(0)
                    }

                    override fun delete(alias: String): Boolean = records.remove(alias)?.let { removed ->
                        removed.fill(0)
                        true
                    } ?: false

                    override fun readDefaultAlias(): String? = defaultAlias

                    override fun writeDefaultAlias(alias: String?) {
                        defaultAlias = alias
                    }
                },
            )
        }

        fun raw(alias: String): ByteArray? = synchronized(records) { records[alias]?.copyOf() }

        fun mutate(alias: String, mutation: (ByteArray) -> Unit) = synchronized(records) { mutation(requireNotNull(records[alias])) }

        fun replace(alias: String, bytes: ByteArray) = synchronized(records) { records[alias] = bytes.copyOf() }

        fun copy(source: String, destination: String) = synchronized(records) { records[destination] = requireNotNull(records[source]).copyOf() }
    }

    private class ThrowingAccountRecordStorage(private val sensitiveText: String) : AccountRecordStorage {
        override fun <T> withExclusiveAccess(action: (AccountRecordAccess) -> T): T = throw IllegalStateException(sensitiveText)
    }

    private class JvmAesGcmAccountCipher : AccountCipher {
        private val key = SecretKeySpec(ByteArray(32) { index -> (index + 1).toByte() }, "AES")
        private val sequence = AtomicInteger()

        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): EncryptedSecret {
            val iv = ByteArray(AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES)
            ByteBuffer.wrap(iv).putInt(iv.size - Int.SIZE_BYTES, sequence.incrementAndGet())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.updateAAD(associatedData)
            return EncryptedSecret(iv, cipher.doFinal(plaintext))
        }

        override fun decrypt(initializationVector: ByteArray, ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, initializationVector))
            cipher.updateAAD(associatedData)
            return cipher.doFinal(ciphertext)
        }
    }

    private class MarkerException : RuntimeException()

    private fun ByteArray.containsSequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty()) return true
        return indices.any { start ->
            start <= size - candidate.size && candidate.indices.all { offset -> this[start + offset] == candidate[offset] }
        }
    }

    private companion object {
        const val PRETTY_JSON = """{
            "address": "alice@example.com",
            "provider": "qq",
            "timeout": { "connect": 5000 }
        }"""
        const val COMPACT_JSON = """{"address":"alice@example.com","provider":"qq","timeout":{"connect":5000}}"""
        const val TOKEN_JSON = """{"address":"alice@gmail.com","provider":"gmail"}"""
    }
}
