package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import kotlinx.serialization.json.JsonObject
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Plugin-private saved accounts (roadmap D8 / P4.1): the non-secret account document of
 * `mail.connect(options)` plus one encrypted secret, addressed by an [AccountAlias].
 *
 * Secrets are handed in as `CharArray` and wiped before [put] returns; [withSecret] releases the
 * decrypted characters to a synchronous callback and wipes them afterwards, so plaintext never
 * outlives the call. Every failure is a [MailException]: `INVALID_ARGUMENT` for a bad alias,
 * document or secret, `ACCOUNT_NOT_FOUND` for an unknown alias, and `INTERNAL` with a fixed
 * message when the storage or the cipher fails, so no cause text can carry a secret outward.
 */
interface AccountStore {

    /** Saves or replaces the record of [alias]; [secret] is wiped before this returns. */
    fun put(alias: String, accountJson: String, secretKind: SecretKind, secret: CharArray): SavedAccount

    /** The non-secret view of [alias], or null when no record exists. */
    fun get(alias: String): SavedAccount?

    fun has(alias: String): Boolean = get(alias) != null

    /** Every readable record ordered by alias; damaged or misplaced records are left out. */
    fun list(): List<SavedAccount>

    /** Deletes the record of [alias] (and its default mark); false when there was none. */
    fun remove(alias: String): Boolean

    /** Marks [alias] as the default account, or clears the mark with null. */
    fun setDefault(alias: String?)

    fun defaultAlias(): String?

    /** Decrypts the secret of [alias] for the duration of [action]; the characters are wiped afterwards. */
    fun <T> withSecret(alias: String, action: (SavedAccount, CharArray) -> T): T
}

/** One AES-GCM envelope of a secret; both arrays are owned by the caller. */
class EncryptedSecret(val initializationVector: ByteArray, val ciphertext: ByteArray)

/** Authenticated encryption of one secret under the store's master key. */
interface AccountCipher {
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): EncryptedSecret

    fun decrypt(initializationVector: ByteArray, ciphertext: ByteArray, associatedData: ByteArray): ByteArray
}

/**
 * Runs record access under one transaction lock. The Android implementation holds both a
 * process mutex and an OS file lock because the settings UI and the Binder service may live in
 * different processes.
 */
interface AccountRecordStorage {
    fun <T> withExclusiveAccess(action: (AccountRecordAccess) -> T): T
}

interface AccountRecordAccess {
    fun read(alias: String): ByteArray?

    /** Every stored record, in no particular order. */
    fun readAll(): List<ByteArray>

    /** Implementations must consume or copy [encodedRecord] before returning. */
    fun write(alias: String, encodedRecord: ByteArray)

    fun delete(alias: String): Boolean

    fun readDefaultAlias(): String?

    fun writeDefaultAlias(alias: String?)
}

/** [AccountStore] over pluggable storage and cipher; pure JVM so the JVM tests cover every rule. */
class EncryptedAccountStore(
    private val storage: AccountRecordStorage,
    private val cipher: AccountCipher,
    private val clock: () -> Long = System::currentTimeMillis,
) : AccountStore {

    override fun put(alias: String, accountJson: String, secretKind: SecretKind, secret: CharArray): SavedAccount {
        try {
            val canonicalAlias = AccountAlias.normalize(alias)
            if (secretKind == SecretKind.NONE) throw MailException.invalidArgument("a password or an access token is required")
            val canonicalJson = canonicalAccountJson(accountJson, secretKind)
            val plaintext = SecretText.encode(secret)
            try {
                val associatedData = AccountAssociatedData.forRecord(canonicalAlias, secretKind, canonicalJson)
                val updatedAt = clock()
                return storeFailure {
                    storage.withExclusiveAccess { records ->
                        val encrypted = cipher.encrypt(plaintext, associatedData)
                        val envelope = try {
                            AccountEnvelope(canonicalAlias, canonicalJson, secretKind, updatedAt, encrypted.initializationVector, encrypted.ciphertext)
                        } finally {
                            encrypted.initializationVector.fill(0)
                            encrypted.ciphertext.fill(0)
                        }
                        val encodedRecord = AccountRecordCodec.encode(envelope)
                        try {
                            records.write(canonicalAlias, encodedRecord)
                        } finally {
                            encodedRecord.fill(0)
                        }
                        envelope.toSavedAccount(records.readDefaultAlias())
                    }
                }
            } finally {
                plaintext.fill(0)
            }
        } finally {
            secret.fill('\u0000')
        }
    }

    override fun get(alias: String): SavedAccount? {
        val canonicalAlias = AccountAlias.normalize(alias)
        return storeFailure {
            storage.withExclusiveAccess { records ->
                records.read(canonicalAlias)?.let { encodedRecord ->
                    decodeOwn(canonicalAlias, encodedRecord).toSavedAccount(records.readDefaultAlias())
                }
            }
        }
    }

    override fun list(): List<SavedAccount> = storeFailure {
        storage.withExclusiveAccess { records ->
            val defaultAlias = records.readDefaultAlias()
            records.readAll()
                .mapNotNull { encodedRecord ->
                    // A record counts only when its own alias reads back to the very same bytes, so a
                    // copy stored under another alias, a damaged file or a stale duplicate is left out.
                    val envelope = runCatching { AccountRecordCodec.decode(encodedRecord) }.getOrNull() ?: return@mapNotNull null
                    envelope.takeIf { records.read(it.alias)?.contentEquals(encodedRecord) == true }
                }
                .distinctBy { it.alias }
                .map { envelope -> envelope.toSavedAccount(defaultAlias) }
                .sortedBy { it.alias }
        }
    }

    override fun remove(alias: String): Boolean {
        val canonicalAlias = AccountAlias.normalize(alias)
        return storeFailure {
            storage.withExclusiveAccess { records ->
                val removed = records.delete(canonicalAlias)
                if (records.readDefaultAlias() == canonicalAlias) records.writeDefaultAlias(null)
                removed
            }
        }
    }

    override fun setDefault(alias: String?) {
        val canonicalAlias = alias?.let(AccountAlias::normalize)
        storeFailure {
            storage.withExclusiveAccess { records ->
                if (canonicalAlias != null && records.read(canonicalAlias) == null) throw notFound(canonicalAlias)
                records.writeDefaultAlias(canonicalAlias)
            }
        }
    }

    override fun defaultAlias(): String? = storeFailure {
        storage.withExclusiveAccess { records ->
            records.readDefaultAlias()?.takeIf { AccountAlias.isValid(it) && records.read(it) != null }
        }
    }

    override fun <T> withSecret(alias: String, action: (SavedAccount, CharArray) -> T): T {
        val canonicalAlias = AccountAlias.normalize(alias)
        val (account, plaintext) = storeFailure {
            storage.withExclusiveAccess { records ->
                val encodedRecord = records.read(canonicalAlias) ?: throw notFound(canonicalAlias)
                val envelope = decodeOwn(canonicalAlias, encodedRecord)
                val associatedData = AccountAssociatedData.forRecord(envelope.alias, envelope.secretKind, envelope.accountJson)
                val initializationVector = envelope.copyInitializationVector()
                val ciphertext = envelope.copyCiphertext()
                val plaintext = try {
                    cipher.decrypt(initializationVector, ciphertext, associatedData)
                } catch (e: Exception) {
                    throw damaged(canonicalAlias)
                } finally {
                    initializationVector.fill(0)
                    ciphertext.fill(0)
                }
                envelope.toSavedAccount(records.readDefaultAlias()) to plaintext
            }
        }
        val secret = try {
            SecretText.decode(plaintext)
        } catch (_: MailException) {
            throw damaged(canonicalAlias)
        } finally {
            plaintext.fill(0)
        }
        return try {
            action(account, secret)
        } finally {
            secret.fill('\u0000')
        }
    }

    private fun decodeOwn(alias: String, encodedRecord: ByteArray): AccountEnvelope {
        val envelope = try {
            AccountRecordCodec.decode(encodedRecord)
        } catch (_: IllegalArgumentException) {
            throw damaged(alias)
        }
        if (envelope.alias != alias) throw damaged(alias)
        return envelope
    }

    private fun AccountEnvelope.toSavedAccount(defaultAlias: String?): SavedAccount =
        SavedAccount(alias, accountJson, secretKind, updatedAt, isDefault = defaultAlias == alias)

    /** Parses the document through the mail core (so a saved account is one `openSession` accepts) and compacts it. */
    private fun canonicalAccountJson(accountJson: String, secretKind: SecretKind): String {
        MailAccountOptions.parse(accountJson, secretKind)
        val root = MailJson.format.parseToJsonElement(accountJson) as? JsonObject
            ?: throw MailException.invalidArgument("account must be a JSON object")
        val compact = root.toString()
        if (compact.toByteArray(StandardCharsets.UTF_8).size > AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES) {
            throw MailException.invalidArgument("account JSON exceeds ${AccountEnvelopeLimits.MAXIMUM_ACCOUNT_JSON_BYTES} bytes")
        }
        return compact
    }

    /** Maps storage and cipher failures to one fixed message so their text never reaches the host. */
    private inline fun <T> storeFailure(block: () -> T): T = try {
        block()
    } catch (e: MailException) {
        throw e
    } catch (_: Exception) {
        throw MailException(MailErrorCode.INTERNAL, UNAVAILABLE_MESSAGE, retryable = false)
    }

    private companion object {
        const val UNAVAILABLE_MESSAGE = "the saved-account store is unavailable"

        fun notFound(alias: String): MailException =
            MailException(MailErrorCode.ACCOUNT_NOT_FOUND, "no saved account named '$alias'", retryable = false)

        fun damaged(alias: String): MailException =
            MailException(MailErrorCode.INTERNAL, "the saved account '$alias' cannot be read; save it again", retryable = false)
    }
}

/** Strict UTF-8 conversion of secrets between `CharArray` and `ByteArray`, wiping every scratch buffer. */
internal object SecretText {
    private const val MAXIMUM_UTF8_BYTES_PER_CHAR = 3

    fun encode(secret: CharArray): ByteArray {
        if (secret.isEmpty()) throw MailException.invalidArgument("the secret must not be empty")
        if (secret.size > AccountEnvelopeLimits.MAXIMUM_SECRET_BYTES) throw tooLarge()
        val scratch = ByteArray(Math.multiplyExact(secret.size, MAXIMUM_UTF8_BYTES_PER_CHAR))
        try {
            val encoder = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val output = ByteBuffer.wrap(scratch)
            val encoded = encoder.encode(CharBuffer.wrap(secret), output, true)
            if (encoded.isError) encoded.throwException()
            val flushed = encoder.flush(output)
            if (flushed.isError) flushed.throwException()
            val size = output.position()
            if (size > AccountEnvelopeLimits.MAXIMUM_SECRET_BYTES) throw tooLarge()
            return scratch.copyOf(size)
        } catch (_: CharacterCodingException) {
            throw MailException.invalidArgument("the secret is not valid text")
        } finally {
            scratch.fill(0)
        }
    }

    fun decode(plaintext: ByteArray): CharArray {
        if (plaintext.isEmpty() || plaintext.size > AccountEnvelopeLimits.MAXIMUM_SECRET_BYTES) {
            throw MailException.invalidArgument("the secret size is invalid")
        }
        val scratch = CharArray(plaintext.size)
        try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val output = CharBuffer.wrap(scratch)
            val decoded = decoder.decode(ByteBuffer.wrap(plaintext), output, true)
            if (decoded.isError) decoded.throwException()
            val flushed = decoder.flush(output)
            if (flushed.isError) flushed.throwException()
            return scratch.copyOf(output.position())
        } catch (_: CharacterCodingException) {
            throw MailException.invalidArgument("the secret is not valid text")
        } finally {
            scratch.fill('\u0000')
        }
    }

    private fun tooLarge(): MailException =
        MailException.invalidArgument("the secret exceeds ${AccountEnvelopeLimits.MAXIMUM_SECRET_BYTES} bytes")
}
