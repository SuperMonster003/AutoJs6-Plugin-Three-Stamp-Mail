package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** A PKCE pair (RFC 7636, `S256`): the verifier stays in the plugin, the challenge goes to the browser. */
class PkceChallenge(val verifier: String, val challenge: String) {
    val method: String get() = METHOD

    companion object {
        const val METHOD = "S256"
    }
}

/**
 * Random material of the authorization request: the PKCE verifier (32 random bytes, so 43
 * base64url characters within RFC 7636's 43..128) and the `state` that ties the browser's redirect
 * to the request that opened it (16 random bytes).
 */
object Pkce {

    fun generate(random: SecureRandom = SecureRandom()): PkceChallenge {
        val verifier = encode(randomBytes(random, VERIFIER_BYTES))
        return PkceChallenge(verifier, challengeOf(verifier))
    }

    fun challengeOf(verifier: String): String {
        require(verifier.length in MIN_VERIFIER_LENGTH..MAX_VERIFIER_LENGTH && verifier.all { it.isLetterOrDigit() || it in "-._~" }) { "not a PKCE verifier" }
        return encode(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    }

    fun state(random: SecureRandom = SecureRandom()): String = encode(randomBytes(random, STATE_BYTES))

    /** Constant-time comparison of two states; a null or blank candidate never matches. */
    fun sameState(expected: String, candidate: String?): Boolean {
        if (candidate.isNullOrEmpty() || expected.isEmpty()) return false
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), candidate.toByteArray(Charsets.UTF_8))
    }

    private fun randomBytes(random: SecureRandom, count: Int): ByteArray = ByteArray(count).also(random::nextBytes)

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private const val VERIFIER_BYTES = 32
    private const val STATE_BYTES = 16
    private const val MIN_VERIFIER_LENGTH = 43
    private const val MAX_VERIFIER_LENGTH = 128
}
