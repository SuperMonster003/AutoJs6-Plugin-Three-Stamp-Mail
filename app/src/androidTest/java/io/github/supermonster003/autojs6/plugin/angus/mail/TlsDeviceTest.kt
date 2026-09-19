package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.EndpointReport
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger
import java.net.InetAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.security.auth.x500.X500Principal

/**
 * TLS on the device runtime (mail roadmap P6 TLS matrix, "API 24 TLS 1.2 default"): the default
 * client socket of the platform enables TLS 1.2, and the mail core connects to a loopback IMAP
 * server that accepts TLS 1.2 only; a TLS 1.3-only server then tells what the platform offers
 * (TLS 1.3 exists from API 29). The server key pair and its self-signed certificate come from
 * the Android Keystore at run time, so no key material lives in the repository.
 */
@RunWith(AndroidJUnit4::class)
class TlsDeviceTest {

    @Test
    fun theDefaultClientEnablesTls12() {
        val socket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
        try {
            val enabled = socket.enabledProtocols.toList()
            val supported = socket.supportedProtocols.toList()
            Log.i(TAG, "API ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL}): enabled=$enabled supported=$supported default=${SSLContext.getDefault().defaultSSLParameters.protocols.toList()}")
            assertTrue("TLS 1.2 must be enabled by default: $enabled", enabled.contains("TLSv1.2"))
            assertFalse("SSLv3 must not be enabled: $enabled", enabled.contains("SSLv3"))
        } finally {
            socket.close()
        }
    }

    @Test
    fun theMailCoreConnectsToATls12OnlyServerAndTls13TellsTheApiLevel() {
        val context = serverContext()
        ScriptedImapServer(serverSocket(context, arrayOf("TLSv1.2"))).use { server ->
            val report = probe(server.port)
            assertTrue("TLS 1.2 only server on API ${Build.VERSION.SDK_INT}: ${report.error}", report.ok)
            assertEquals("TLSv1.2", server.negotiated.poll(5, TimeUnit.SECONDS))
            Log.i(TAG, "TLS 1.2 only server: ok in ${report.elapsedMs} ms")
        }
        val supports13 = (context.serverSocketFactory.createServerSocket() as SSLServerSocket).use { it.supportedProtocols.contains("TLSv1.3") }
        if (!supports13) {
            Log.i(TAG, "TLS 1.3 only server: the platform (API ${Build.VERSION.SDK_INT}) cannot host TLS 1.3")
            assertTrue("TLS 1.3 arrived with API 29", Build.VERSION.SDK_INT < 29)
            return
        }
        ScriptedImapServer(serverSocket(context, arrayOf("TLSv1.3"))).use { server ->
            val report = probe(server.port)
            Log.i(TAG, "TLS 1.3 only server: ok=${report.ok} error=${report.error?.code} negotiated=${server.negotiated.peek()}")
            if (Build.VERSION.SDK_INT >= 29) {
                assertTrue("TLS 1.3 only server on API ${Build.VERSION.SDK_INT}: ${report.error}", report.ok)
                assertEquals("TLSv1.3", server.negotiated.poll(5, TimeUnit.SECONDS))
            } else {
                assertFalse(report.ok)
                assertEquals(MailErrorCode.TLS_FAILED, report.error!!.code)
            }
        }
    }

    private fun probe(port: Int): EndpointReport {
        val account = MailAccount(
            address = "alice@example.org",
            imap = MailEndpoint("127.0.0.1", port, TlsMode.SSL),
            trustAll = true,
            timeouts = MailTimeouts.uniform(10_000),
        )
        val session = MailSession(account, MailSecret("secret"))
        try {
            return checkNotNull(session.test().imap)
        } finally {
            session.close()
        }
    }

    private fun serverSocket(context: SSLContext, protocols: Array<String>): SSLServerSocket =
        (context.serverSocketFactory.createServerSocket(0, 8, InetAddress.getByName("127.0.0.1")) as SSLServerSocket).apply { enabledProtocols = protocols }

    /** A server context with an EC key pair and self-signed certificate generated in the Android Keystore. */
    private fun serverContext(): SSLContext {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(ALIAS)
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val now = System.currentTimeMillis()
        generator.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                // Conscrypt signs the handshake through NONEwithECDSA on a keystore key, so the raw digest must be allowed too.
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
                .setCertificateSubject(X500Principal("CN=localhost"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(Date(now - DAY_MS))
                .setCertificateNotAfter(Date(now + 30 * DAY_MS))
                .build(),
        )
        generator.generateKeyPair()
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keyStore, null) }
        return SSLContext.getInstance("TLS").apply { init(keyManagers.keyManagers, null, null) }
    }

    private companion object {
        const val TAG = "TlsDeviceTest"
        const val ALIAS = "tls-device-test"
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
