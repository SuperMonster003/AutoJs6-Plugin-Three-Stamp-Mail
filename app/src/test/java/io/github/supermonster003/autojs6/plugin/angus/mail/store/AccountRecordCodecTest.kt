package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/** The binary record envelope of the saved-account store (roadmap P4.1 `AccountRecordCodec`). */
class AccountRecordCodecTest {

    @Test
    fun roundTripsEveryFieldAndKeepsArraysDefensive() {
        val iv = ByteArray(AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES) { it.toByte() }
        val ciphertext = ByteArray(40) { (it * 3).toByte() }
        val envelope = AccountEnvelope("work", JSON, SecretKind.ACCESS_TOKEN, 1_700_000_000_123L, iv, ciphertext)
        iv.fill(9)
        ciphertext.fill(9)

        val encoded = AccountRecordCodec.encode(envelope)
        val decoded = AccountRecordCodec.decode(encoded)

        assertEquals(envelope, decoded)
        assertEquals("work", decoded.alias)
        assertEquals(JSON, decoded.accountJson)
        assertEquals(SecretKind.ACCESS_TOKEN, decoded.secretKind)
        assertEquals(1_700_000_000_123L, decoded.updatedAt)
        assertArrayEquals(ByteArray(AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES) { it.toByte() }, decoded.copyInitializationVector())
        assertArrayEquals(ByteArray(40) { (it * 3).toByte() }, decoded.copyCiphertext())
        assertEquals(0x414D4143, ByteBuffer.wrap(encoded).int)
        assertTrue(encoded.toString(StandardCharsets.ISO_8859_1).contains(JSON))
        assertFalse(envelope.toString(), envelope.toString().contains("alice"))
        assertTrue(envelope.toString(), envelope.toString().contains("ciphertextBytes=40"))
    }

    @Test
    fun rejectsDamagedEnvelopes() {
        val encoded = AccountRecordCodec.encode(sample())

        fun assertRejected(label: String, mutate: (ByteArray) -> ByteArray) {
            assertThrows(label, IllegalArgumentException::class.java) { AccountRecordCodec.decode(mutate(encoded.copyOf())) }
        }
        assertRejected("magic") { it.also { bytes -> bytes[0] = 0 } }
        assertRejected("version") { it.also { bytes -> bytes[4] = 2 } }
        assertRejected("algorithm") { it.also { bytes -> bytes[5] = 2 } }
        assertRejected("secret kind") { it.also { bytes -> bytes[6] = 4 } }
        assertRejected("flags") { it.also { bytes -> bytes[7] = 1 } }
        assertRejected("truncated") { it.copyOf(it.size - 1) }
        assertRejected("trailing") { it + byteArrayOf(0) }
        assertRejected("empty") { ByteArray(0) }
        assertRejected("too large") { ByteArray(AccountRecordCodec.MAXIMUM_ENCODED_BYTES + 1) }
        assertRejected("alias not canonical") { AccountRecordCodec.encode(sample()).also { bytes -> bytes[16 + 2] = 'W'.code.toByte() } }
    }

    @Test
    fun envelopeValidatesItsSizes() {
        val iv = ByteArray(AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES)
        val tag = ByteArray(AccountEnvelopeLimits.AUTHENTICATION_TAG_BYTES)
        assertThrows(IllegalArgumentException::class.java) { AccountEnvelope("work", JSON, SecretKind.NONE, 0, iv, tag) }
        assertThrows(IllegalArgumentException::class.java) { AccountEnvelope("", JSON, SecretKind.PASSWORD, 0, iv, tag) }
        assertThrows(IllegalArgumentException::class.java) { AccountEnvelope("work", "", SecretKind.PASSWORD, 0, iv, tag) }
        assertThrows(IllegalArgumentException::class.java) { AccountEnvelope("work", JSON, SecretKind.PASSWORD, 0, ByteArray(11), tag) }
        assertThrows(IllegalArgumentException::class.java) { AccountEnvelope("work", JSON, SecretKind.PASSWORD, 0, iv, ByteArray(15)) }
        assertThrows(IllegalArgumentException::class.java) {
            AccountEnvelope("work", JSON, SecretKind.PASSWORD, 0, iv, ByteArray(AccountEnvelopeLimits.MAXIMUM_CIPHERTEXT_BYTES + 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            AccountEnvelope("work", "x".repeat(AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES + 1), SecretKind.PASSWORD, 0, iv, tag)
        }
        val largest = AccountEnvelope(
            "a".repeat(AccountAlias.MAX_LENGTH),
            "x".repeat(AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES),
            SecretKind.PASSWORD,
            0,
            iv,
            ByteArray(AccountEnvelopeLimits.MAXIMUM_CIPHERTEXT_BYTES),
        )
        assertTrue(AccountRecordCodec.encode(largest).size <= AccountRecordCodec.MAXIMUM_ENCODED_BYTES)
        assertEquals(largest, AccountRecordCodec.decode(AccountRecordCodec.encode(largest)))
    }

    @Test
    fun associatedDataBindsAliasKindAndDocument() {
        val base = AccountAssociatedData.forRecord("work", SecretKind.PASSWORD, JSON)
        assertArrayEquals(base, AccountAssociatedData.forRecord("work", SecretKind.PASSWORD, JSON))
        assertFalse(base.contentEquals(AccountAssociatedData.forRecord("home", SecretKind.PASSWORD, JSON)))
        assertFalse(base.contentEquals(AccountAssociatedData.forRecord("work", SecretKind.ACCESS_TOKEN, JSON)))
        assertFalse(base.contentEquals(AccountAssociatedData.forRecord("work", SecretKind.PASSWORD, JSON.replace("alice", "mallory"))))
        assertNotEquals(
            AccountAssociatedData.forRecord("ab", SecretKind.PASSWORD, "{}").toList(),
            AccountAssociatedData.forRecord("a", SecretKind.PASSWORD, "b{}").toList(),
        )
        assertThrows(IllegalArgumentException::class.java) { AccountAssociatedData.forRecord("work", SecretKind.NONE, JSON) }
    }

    private fun sample(): AccountEnvelope = AccountEnvelope(
        "work",
        JSON,
        SecretKind.PASSWORD,
        42L,
        ByteArray(AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES) { 1 },
        ByteArray(AccountEnvelopeLimits.AUTHENTICATION_TAG_BYTES + 4) { 2 },
    )

    private companion object {
        const val JSON = """{"address":"alice@example.com","provider":"qq"}"""
    }
}
