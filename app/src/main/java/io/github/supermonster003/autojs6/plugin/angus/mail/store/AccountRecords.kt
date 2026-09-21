package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.nio.charset.StandardCharsets

/** Size ceilings of one saved-account record (roadmap P4.1). */
object AccountEnvelopeLimits {
    const val INITIALIZATION_VECTOR_BYTES = 12
    const val AUTHENTICATION_TAG_BYTES = 16

    /** UTF-8 bytes of the secret (password, authorization code or access token). */
    const val MAXIMUM_SECRET_BYTES = 16 * 1024

    /** UTF-8 bytes of the non-secret account JSON. */
    const val MAXIMUM_ACCOUNT_JSON_BYTES = 16 * 1024

    /** UTF-8 bytes of a normalized alias ([AccountAlias.MAX_LENGTH] code points of up to 4 bytes). */
    const val MAXIMUM_ALIAS_BYTES = AccountAlias.MAX_LENGTH * 4

    const val MINIMUM_CIPHERTEXT_BYTES = AUTHENTICATION_TAG_BYTES
    const val MAXIMUM_CIPHERTEXT_BYTES = MAXIMUM_SECRET_BYTES + AUTHENTICATION_TAG_BYTES
}

/**
 * The non-secret view of a saved account: what `listSavedAccounts` and the settings page show.
 * [accountJson] is the compact account document of `mail.connect(options)` without any secret
 * field; [secretKind] tells which secret key the record holds. [toString] names the alias only.
 */
class SavedAccount(
    val alias: String,
    val accountJson: String,
    val secretKind: SecretKind,
    val updatedAt: Long,
    val isDefault: Boolean,
) {
    override fun toString(): String = "SavedAccount(alias=$alias, secretKind=$secretKind, default=$isDefault)"
}

/**
 * One saved-account record as stored on disk: the plaintext non-secret fields plus the AES-GCM
 * envelope of the secret. Byte arrays are defensive copies; [toString] exposes sizes only.
 */
class AccountEnvelope(
    val alias: String,
    val accountJson: String,
    val secretKind: SecretKind,
    val updatedAt: Long,
    initializationVector: ByteArray,
    ciphertext: ByteArray,
) {
    private val initializationVector = initializationVector.copyOf()
    private val ciphertext = ciphertext.copyOf()

    init {
        require(secretKind != SecretKind.NONE) { "saved account record has no secret kind" }
        require(alias.isNotEmpty() && alias.toByteArray(StandardCharsets.UTF_8).size <= AccountEnvelopeLimits.MAXIMUM_ALIAS_BYTES) {
            "saved account alias size is invalid"
        }
        require(accountJson.toByteArray(StandardCharsets.UTF_8).size in 1..AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES) {
            "saved account JSON size is invalid"
        }
        require(this.initializationVector.size == AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES) {
            "saved account initialization vector is invalid"
        }
        require(this.ciphertext.size in AccountEnvelopeLimits.MINIMUM_CIPHERTEXT_BYTES..AccountEnvelopeLimits.MAXIMUM_CIPHERTEXT_BYTES) {
            "saved account ciphertext is invalid"
        }
    }

    fun copyInitializationVector(): ByteArray = initializationVector.copyOf()

    fun copyCiphertext(): ByteArray = ciphertext.copyOf()

    /** The same non-secret fields with a different envelope (used when the record is re-encrypted). */
    fun withSecret(initializationVector: ByteArray, ciphertext: ByteArray): AccountEnvelope =
        AccountEnvelope(alias, accountJson, secretKind, updatedAt, initializationVector, ciphertext)

    override fun equals(other: Any?): Boolean =
        other is AccountEnvelope &&
            alias == other.alias &&
            accountJson == other.accountJson &&
            secretKind == other.secretKind &&
            updatedAt == other.updatedAt &&
            initializationVector.contentEquals(other.initializationVector) &&
            ciphertext.contentEquals(other.ciphertext)

    override fun hashCode(): Int {
        var result = alias.hashCode()
        result = 31 * result + accountJson.hashCode()
        result = 31 * result + secretKind.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + initializationVector.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }

    override fun toString(): String =
        "AccountEnvelope(alias=$alias, secretKind=$secretKind, jsonBytes=${accountJson.length}, ivBytes=${initializationVector.size}, ciphertextBytes=${ciphertext.size})"
}

/**
 * Additional authenticated data of the envelope: the store domain, the alias, the secret kind and
 * the non-secret account JSON. Editing any of them on disk (pointing a saved password at another
 * server, moving a record to another alias) makes the AES-GCM tag fail, so the secret is only ever
 * released for the exact account it was saved for.
 */
object AccountAssociatedData {
    private val DOMAIN = "io.github.supermonster003.autojs6.plugin.angus.mail/account/v1".toByteArray(StandardCharsets.UTF_8)

    fun forRecord(alias: String, secretKind: SecretKind, accountJson: String): ByteArray {
        val aliasBytes = alias.toByteArray(StandardCharsets.UTF_8)
        val jsonBytes = accountJson.toByteArray(StandardCharsets.UTF_8)
        val bytes = ByteArrayOutputStream(DOMAIN.size + aliasBytes.size + jsonBytes.size + 16)
        DataOutputStream(bytes).use { output ->
            output.write(DOMAIN)
            output.writeShort(aliasBytes.size)
            output.write(aliasBytes)
            output.writeByte(AccountRecordCodec.secretKindId(secretKind))
            output.writeInt(jsonBytes.size)
            output.write(jsonBytes)
        }
        return bytes.toByteArray()
    }
}

/**
 * Versioned binary encoding of an [AccountEnvelope] (roadmap P4.1 `AccountRecordCodec`):
 *
 * ```
 * int32 magic "AMAC" | u8 version | u8 algorithm | u8 secret kind | u8 reserved | int64 updatedAt
 * | u16 alias length | alias (UTF-8) | int32 json length | json (UTF-8)
 * | u16 iv length | iv | int32 ciphertext length | ciphertext
 * ```
 *
 * Every length is bounded by [AccountEnvelopeLimits] and the record must end exactly after the
 * ciphertext; anything else is rejected with `IllegalArgumentException`.
 */
object AccountRecordCodec {
    private const val MAGIC = 0x414D4143 // "AMAC"
    private const val VERSION = 1
    private const val ALGORITHM_AES_GCM = 1
    private const val SECRET_KIND_PASSWORD = 1
    private const val SECRET_KIND_ACCESS_TOKEN = 2
    private const val SECRET_KIND_OAUTH2 = 3

    private const val HEADER_BYTES = 4 + 1 + 1 + 1 + 1 + 8
    private const val LENGTH_FIELD_BYTES = 2 + 4 + 2 + 4

    const val MAXIMUM_ENCODED_BYTES =
        HEADER_BYTES +
            LENGTH_FIELD_BYTES +
            AccountEnvelopeLimits.MAXIMUM_ALIAS_BYTES +
            AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES +
            AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES +
            AccountEnvelopeLimits.MAXIMUM_CIPHERTEXT_BYTES

    private const val MINIMUM_ENCODED_BYTES =
        HEADER_BYTES +
            LENGTH_FIELD_BYTES +
            1 +
            1 +
            AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES +
            AccountEnvelopeLimits.MINIMUM_CIPHERTEXT_BYTES

    fun secretKindId(kind: SecretKind): Int = when (kind) {
        SecretKind.PASSWORD -> SECRET_KIND_PASSWORD
        SecretKind.ACCESS_TOKEN -> SECRET_KIND_ACCESS_TOKEN
        SecretKind.OAUTH2 -> SECRET_KIND_OAUTH2
        SecretKind.NONE -> throw IllegalArgumentException("saved account record has no secret kind")
    }

    private fun secretKindOf(id: Int): SecretKind = when (id) {
        SECRET_KIND_PASSWORD -> SecretKind.PASSWORD
        SECRET_KIND_ACCESS_TOKEN -> SecretKind.ACCESS_TOKEN
        SECRET_KIND_OAUTH2 -> SecretKind.OAUTH2
        else -> throw IllegalArgumentException("saved account record secret kind is invalid")
    }

    fun encode(envelope: AccountEnvelope): ByteArray {
        val aliasBytes = envelope.alias.toByteArray(StandardCharsets.UTF_8)
        val jsonBytes = envelope.accountJson.toByteArray(StandardCharsets.UTF_8)
        val initializationVector = envelope.copyInitializationVector()
        val ciphertext = envelope.copyCiphertext()
        return try {
            val bytes = ByteArrayOutputStream(HEADER_BYTES + LENGTH_FIELD_BYTES + aliasBytes.size + jsonBytes.size + initializationVector.size + ciphertext.size)
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeByte(VERSION)
                output.writeByte(ALGORITHM_AES_GCM)
                output.writeByte(secretKindId(envelope.secretKind))
                output.writeByte(0)
                output.writeLong(envelope.updatedAt)
                output.writeShort(aliasBytes.size)
                output.write(aliasBytes)
                output.writeInt(jsonBytes.size)
                output.write(jsonBytes)
                output.writeShort(initializationVector.size)
                output.write(initializationVector)
                output.writeInt(ciphertext.size)
                output.write(ciphertext)
            }
            bytes.toByteArray()
        } finally {
            initializationVector.fill(0)
            ciphertext.fill(0)
        }
    }

    fun decode(encodedRecord: ByteArray): AccountEnvelope {
        require(encodedRecord.size in MINIMUM_ENCODED_BYTES..MAXIMUM_ENCODED_BYTES) { "saved account record size is invalid" }
        try {
            DataInputStream(ByteArrayInputStream(encodedRecord)).use { input ->
                require(input.readInt() == MAGIC) { "saved account record magic is invalid" }
                require(input.readUnsignedByte() == VERSION) { "saved account record version is invalid" }
                require(input.readUnsignedByte() == ALGORITHM_AES_GCM) { "saved account record algorithm is invalid" }
                val secretKind = secretKindOf(input.readUnsignedByte())
                require(input.readUnsignedByte() == 0) { "saved account record flags are invalid" }
                val updatedAt = input.readLong()

                val aliasSize = input.readUnsignedShort()
                require(aliasSize in 1..AccountEnvelopeLimits.MAXIMUM_ALIAS_BYTES) { "saved account alias size is invalid" }
                val aliasBytes = ByteArray(aliasSize)
                input.readFully(aliasBytes)

                val jsonSize = input.readInt()
                require(jsonSize in 1..AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES) { "saved account JSON size is invalid" }
                val jsonBytes = ByteArray(jsonSize)
                input.readFully(jsonBytes)

                val initializationVectorSize = input.readUnsignedShort()
                require(initializationVectorSize == AccountEnvelopeLimits.INITIALIZATION_VECTOR_BYTES) { "saved account initialization vector size is invalid" }
                val initializationVector = ByteArray(initializationVectorSize)
                input.readFully(initializationVector)

                val ciphertextSize = input.readInt()
                require(ciphertextSize in AccountEnvelopeLimits.MINIMUM_CIPHERTEXT_BYTES..AccountEnvelopeLimits.MAXIMUM_CIPHERTEXT_BYTES) { "saved account ciphertext size is invalid" }
                val ciphertext = ByteArray(ciphertextSize)
                input.readFully(ciphertext)

                check(input.read() == -1) { "saved account record has trailing data" }
                val alias = String(aliasBytes, StandardCharsets.UTF_8)
                val accountJson = String(jsonBytes, StandardCharsets.UTF_8)
                require(AccountAlias.isValid(alias) && AccountAlias.normalize(alias) == alias) { "saved account alias is invalid" }
                return AccountEnvelope(alias, accountJson, secretKind, updatedAt, initializationVector, ciphertext)
            }
        } catch (_: EOFException) {
            throw IllegalArgumentException("saved account record is truncated")
        } catch (e: IllegalStateException) {
            throw IllegalArgumentException(e.message ?: "saved account record is invalid")
        }
    }
}
