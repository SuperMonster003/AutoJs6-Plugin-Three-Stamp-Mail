package io.github.supermonster003.autojs6.plugin.angus.mail

import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The decision behind `CallerGuard` (roadmap P2.5), aligned with the MCP Server plugin's verifier. */
class CallerPolicyTest {

    private val signer = setOf("ab" * 32)
    private val host = CallerPolicy.InstalledHost(AngusMailPlugin.HOST_PACKAGE_NAME, uid = 10_123, versionCode = AngusMailPlugin.REQUIRED_HOST_VERSION, signers = signer)
    private val hostCaller = CallerPolicy.Caller(uid = 10_123, packages = setOf(AngusMailPlugin.HOST_PACKAGE_NAME))

    private operator fun String.times(count: Int): String = repeat(count)

    @Test
    fun theInstalledSameSignerHostPasses() {
        assertNull(CallerPolicy.refusal(hostCaller, listOf(host), signer))
        assertNull("a newer host passes", CallerPolicy.refusal(hostCaller, listOf(host.copy(versionCode = host.versionCode + 1)), signer))
        assertNull("other packages sharing the host uid do not matter", CallerPolicy.refusal(hostCaller.copy(packages = setOf("org.example.shared", AngusMailPlugin.HOST_PACKAGE_NAME)), listOf(host), signer))
    }

    @Test
    fun onlyTheHostPackageNamesCount() {
        assertEquals(listOf("org.autojs.autojs6"), CallerPolicy.HOST_PACKAGE_NAMES)
        val stranger = CallerPolicy.InstalledHost("org.example.other", uid = 10_123, versionCode = 9_999, signers = signer)
        val refusal = CallerPolicy.refusal(hostCaller, listOf(stranger), signer)
        assertNotNull(refusal)
        assertTrue(refusal!!, refusal.contains("not installed"))
    }

    @Test
    fun everyRefusalNamesTheHostAndBecomesTheSameSecurityException() {
        val cases = listOf(
            "not installed" to CallerPolicy.refusal(hostCaller, emptyList(), signer),
            "uid" to CallerPolicy.refusal(hostCaller.copy(uid = 10_999), listOf(host), signer),
            "does not run under uid" to CallerPolicy.refusal(hostCaller.copy(packages = setOf("org.example.spoof")), listOf(host), signer),
            "older than the required build" to CallerPolicy.refusal(hostCaller, listOf(host.copy(versionCode = AngusMailPlugin.REQUIRED_HOST_VERSION - 1)), signer),
            "signer set is empty" to CallerPolicy.refusal(hostCaller, listOf(host), emptySet()),
            "not signed by the plugin's signer" to CallerPolicy.refusal(hostCaller, listOf(host.copy(signers = setOf("cd" * 32))), signer),
            "not signed by the plugin's signer" to CallerPolicy.refusal(hostCaller, listOf(host.copy(signers = signer + ("cd" * 32))), signer),
        )
        cases.forEach { (expected, refusal) ->
            assertNotNull(expected, refusal)
            assertTrue("$expected: $refusal", refusal!!.contains(expected))
            assertTrue(refusal, refusal.contains("AutoJs6"))
            val exception = CallerPolicy.exception(refusal)
            assertTrue(exception.message, exception.message!!.startsWith("Caller is not the installed same-signer AutoJs6 host: "))
        }
    }

    @Test
    fun theRequiredBuildIsTheContractHost() {
        assertEquals(5282L, AngusMailPlugin.REQUIRED_HOST_VERSION)
        assertNull(CallerPolicy.refusal(hostCaller, listOf(host), signer, requiredHostVersion = 5282L))
        assertNotNull(CallerPolicy.refusal(hostCaller, listOf(host), signer, requiredHostVersion = 5283L))
    }
}
