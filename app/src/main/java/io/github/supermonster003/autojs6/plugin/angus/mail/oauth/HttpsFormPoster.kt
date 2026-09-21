package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.FormAnswer
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.FormPoster
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The plugin's only HTTP client (roadmap P9): one `application/x-www-form-urlencoded` POST to an
 * OAuth 2.0 token or revocation endpoint over HTTPS with the platform's trust store, no redirects
 * followed, bounded timeouts and a bounded answer. Nothing else in the plugin speaks HTTP; the
 * manifest comment and `AGENTS.md` name this class as the single exception.
 *
 * [allowPlainLoopback] exists for the JVM tests, which answer from a loopback server without TLS.
 */
class HttpsFormPoster(
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
    private val allowPlainLoopback: Boolean = false,
) : FormPoster {

    override fun post(url: String, form: List<Pair<String, String>>): FormAnswer {
        val target = URL(url)
        val loopback = allowPlainLoopback && target.protocol == "http" && (target.host == "127.0.0.1" || target.host == "localhost")
        require(target.protocol == "https" || loopback) { "token endpoints must be https" }
        val body = form.joinToString("&") { (key, value) -> encode(key) + "=" + encode(value) }.toByteArray(Charsets.UTF_8)
        val connection = target.openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.requestMethod = "POST"
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            val stream = (if (status >= HttpURLConnection.HTTP_BAD_REQUEST) connection.errorStream else connection.inputStream) ?: return FormAnswer(status, "")
            val bytes = ByteArrayOutputStream()
            stream.use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (bytes.size() + read > MAX_BODY_BYTES) throw IOException("the token endpoint's answer exceeds $MAX_BODY_BYTES bytes")
                    bytes.write(buffer, 0, read)
                }
            }
            return FormAnswer(status, bytes.toString(Charsets.UTF_8.name()))
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")

    companion object {
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_BODY_BYTES = 64 * 1024
        const val USER_AGENT = "AutoJs6-Plugin-Angus-Mail"
    }
}
