package io.github.supermonster003.autojs6.plugin.angus.mail.store

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.UnrecoverableKeyException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The process-wide [AccountStore] of the plugin: Keystore AES-256-GCM over `noBackupFilesDir` (AGENTS.md section 14). */
object AccountStores {

    @Volatile
    private var instance: AccountStore? = null

    fun of(context: Context): AccountStore = instance ?: synchronized(this) {
        instance ?: EncryptedAccountStore(
            storage = FileAccountRecordStorage(context.applicationContext.noBackupFilesDir),
            cipher = AndroidKeystoreAccountCipher(),
        ).also { instance = it }
    }
}

/** AES-256-GCM whose non-exportable master key lives in Android Keystore (roadmap D8). */
class AndroidKeystoreAccountCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : AccountCipher {

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): EncryptedSecret {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        cipher.updateAAD(associatedData)
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedSecret(cipher.iv, ciphertext)
    }

    override fun decrypt(initializationVector: ByteArray, ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey(), GCMParameterSpec(AUTHENTICATION_TAG_BITS, initializationVector))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(ciphertext)
    }

    /** Removes the master key; every record becomes unreadable (used by the device tests). */
    fun deleteKey() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias)
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = loadKeyStore()
        existingSecretKey(keyStore)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private fun decryptionKey(): SecretKey =
        existingSecretKey(loadKeyStore()) ?: throw UnrecoverableKeyException("saved-account master key is unavailable")

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private fun existingSecretKey(keyStore: KeyStore): SecretKey? {
        if (!keyStore.containsAlias(keyAlias)) return null
        return keyStore.getKey(keyAlias, null) as? SecretKey
            ?: throw UnrecoverableKeyException("saved-account master key has an invalid type")
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "io.github.supermonster003.autojs6.plugin.angus.mail.accounts.v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val AUTHENTICATION_TAG_BITS = AccountEnvelopeLimits.AUTHENTICATION_TAG_BYTES * 8
    }
}

/**
 * App-private record files shared coherently by the settings UI and the Binder service. Every
 * operation is serialized by a JVM mutex and an exclusive OS file lock; publications use fsync +
 * atomic rename + directory fsync so readers observe either the old or the new record.
 *
 * Layout under `<privateRoot>/mail-accounts/`: `account-<sha256(alias)>.bin` per record,
 * `default.alias` naming the default account, `.records.lock` for the file lock.
 */
class FileAccountRecordStorage(
    privateRoot: File,
) : AccountRecordStorage {

    private val privateRoot = privateRoot.absoluteFile
    private val directory = File(this.privateRoot, DIRECTORY_NAME)
    private val lockFile = File(directory, LOCK_FILE_NAME)
    private val defaultFile = File(directory, DEFAULT_FILE_NAME)
    private val processLock = PROCESS_LOCKS.computeIfAbsent(lockFile.absolutePath) { Any() }

    override fun <T> withExclusiveAccess(action: (AccountRecordAccess) -> T): T = synchronized(processLock) {
        ensureDirectory()
        requireSafeDirectChild(lockFile)
        RandomAccessFile(lockFile, "rw").use { lockHandle ->
            requireSafeRegularFile(lockFile)
            val fileLock = lockHandle.channel.lock()
            try {
                val access = LockedRecordAccess()
                try {
                    action(access)
                } finally {
                    access.invalidate()
                }
            } finally {
                fileLock.release()
            }
        }
    }

    private inner class LockedRecordAccess : AccountRecordAccess {
        private var valid = true

        override fun read(alias: String): ByteArray? {
            check(valid) { "record access has ended" }
            return readRecordFile(recordFile(alias))
        }

        override fun readAll(): List<ByteArray> {
            check(valid) { "record access has ended" }
            val files = directory.listFiles { file -> file.isFile && file.name.startsWith(RECORD_PREFIX) && file.name.endsWith(RECORD_SUFFIX) }
                ?: return emptyList()
            return files.sortedBy { it.name }.mapNotNull { file -> runCatching { readRecordFile(file) }.getOrNull() }
        }

        override fun write(alias: String, encodedRecord: ByteArray) {
            check(valid) { "record access has ended" }
            require(encodedRecord.size in 1..AccountRecordCodec.MAXIMUM_ENCODED_BYTES) { "record size is invalid" }
            publish(recordFile(alias), encodedRecord)
        }

        override fun delete(alias: String): Boolean {
            check(valid) { "record access has ended" }
            return deleteFile(recordFile(alias))
        }

        override fun readDefaultAlias(): String? {
            check(valid) { "record access has ended" }
            if (!defaultFile.exists()) return null
            requireSafeRegularFile(defaultFile)
            require(defaultFile.length() in 1L..AccountEnvelopeLimits.MAXIMUM_ALIAS_BYTES.toLong()) { "default marker size is invalid" }
            return String(defaultFile.readBytes(), StandardCharsets.UTF_8).takeIf { it.isNotEmpty() }
        }

        override fun writeDefaultAlias(alias: String?) {
            check(valid) { "record access has ended" }
            if (alias == null) {
                deleteFile(defaultFile)
                return
            }
            val bytes = alias.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size in 1..AccountEnvelopeLimits.MAXIMUM_ALIAS_BYTES) { "default marker size is invalid" }
            publish(defaultFile, bytes)
        }

        fun invalidate() {
            valid = false
        }
    }

    private fun readRecordFile(target: File): ByteArray? {
        if (!target.exists()) return null
        requireSafeRegularFile(target)
        require(target.length() in 1L..AccountRecordCodec.MAXIMUM_ENCODED_BYTES.toLong()) { "record size is invalid" }
        return target.readBytes().also { bytes ->
            require(bytes.size in 1..AccountRecordCodec.MAXIMUM_ENCODED_BYTES) { "record size changed while reading" }
        }
    }

    private fun publish(target: File, bytes: ByteArray) {
        requireSafeDirectChild(target)
        val temporary = File(directory, ".publish-${UUID.randomUUID()}.tmp")
        requireSafeDirectChild(temporary)
        require(!temporary.exists()) { "transaction file already exists" }
        val publicationFailure = try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            Os.rename(temporary.absolutePath, target.absolutePath)
            syncDirectory()
            null
        } catch (error: Throwable) {
            error
        } finally {
            if (temporary.exists()) temporary.delete()
        }
        if (publicationFailure == null) return

        val current = runCatching { if (target.exists()) target.readBytes() else null }.getOrNull()
        val reachedTarget = try {
            current?.contentEquals(bytes) == true
        } finally {
            current?.fill(0)
        }
        if (!reachedTarget) throw publicationFailure
        try {
            syncDirectory()
        } catch (retryFailure: Throwable) {
            retryFailure.addSuppressed(publicationFailure)
            throw retryFailure
        }
    }

    private fun deleteFile(target: File): Boolean {
        if (!target.exists()) return false
        requireSafeRegularFile(target)
        require(target.delete() || !target.exists()) { "record cannot be removed" }
        try {
            syncDirectory()
        } catch (firstFailure: Throwable) {
            if (target.exists()) throw firstFailure
            try {
                syncDirectory()
            } catch (retryFailure: Throwable) {
                retryFailure.addSuppressed(firstFailure)
                throw retryFailure
            }
        }
        return true
    }

    private fun recordFile(alias: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(alias.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        return File(directory, "$RECORD_PREFIX$digest$RECORD_SUFFIX").also(::requireSafeDirectChild)
    }

    private fun ensureDirectory() {
        require(privateRoot.isDirectory || privateRoot.mkdirs()) { "private storage root is unavailable" }
        require(directory.isDirectory || directory.mkdir()) { "private account storage is unavailable" }
        PrivatePathGuard.requireDirectChild(child = directory, trustedParent = privateRoot, label = "account storage")
    }

    private fun requireSafeRegularFile(file: File) {
        require(file.isFile) { "account storage path is not a regular file" }
        requireSafeDirectChild(file)
    }

    private fun requireSafeDirectChild(file: File) {
        PrivatePathGuard.requireDirectChild(child = file, trustedParent = directory, label = "account storage path")
    }

    private fun syncDirectory() {
        val descriptor = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    companion object {
        const val DIRECTORY_NAME = "mail-accounts"
        private const val LOCK_FILE_NAME = ".records.lock"
        private const val DEFAULT_FILE_NAME = "default.alias"
        private const val RECORD_PREFIX = "account-"
        private const val RECORD_SUFFIX = ".bin"
        private val PROCESS_LOCKS = ConcurrentHashMap<String, Any>()
    }
}

/**
 * Validates an app-owned direct child without rejecting the trusted aliases of the Android data
 * root: some releases expose `filesDir` below `/data/user/0` while canonical paths resolve that
 * alias to `/data/data`, so only the child itself must be link-free and canonical containment is
 * checked against the equally canonicalized parent.
 */
internal object PrivatePathGuard {
    fun requireDirectChild(child: File, trustedParent: File, label: String) {
        val absoluteChild = child.absoluteFile
        val absoluteParent = trustedParent.absoluteFile
        require(absoluteChild.parentFile?.path == absoluteParent.path) { "$label escaped its parent" }
        require(!isSymbolicLink(child)) { "$label must not use a link" }
        require(child.canonicalFile.parentFile?.path == trustedParent.canonicalFile.path) { "$label escaped its parent through a link" }
    }

    private fun isSymbolicLink(file: File): Boolean = try {
        OsConstants.S_ISLNK(Os.lstat(file.absolutePath).st_mode)
    } catch (error: ErrnoException) {
        if (error.errno == OsConstants.ENOENT) false else throw error
    }
}
