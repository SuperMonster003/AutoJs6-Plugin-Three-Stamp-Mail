package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.TextRecovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * The charsets of the mail roadmap P6 matrix exist on the device runtime and the recovery chain
 * of the mail core behaves as on the JVM: declared bytes decode, undeclared UTF-8 / GB 18030 /
 * GBK / GB 2312 / ISO-2022-JP bytes are recovered, and an RFC 2047 word in each charset decodes
 * through `TextRecovery.repairHeader`.
 */
@RunWith(AndroidJUnit4::class)
class CharsetDeviceTest {

    private val samples = listOf(
        Triple("UTF-8", "你好 こんにちは 안녕 café", true),
        Triple("GB18030", "你好，𠮷野家的报表。", true),
        Triple("GBK", "你好，這是一封測試郵件。", true),
        Triple("GB2312", "你好，这是一封测试邮件。", true),
        Triple("Big5", "測試郵件，你好。", false),
        Triple("ISO-2022-JP", "こんにちは、テストメールです。", true),
        Triple("EUC-KR", "안녕하세요, 테스트 메일입니다.", false),
    )

    @Test
    fun theMatrixCharsetsExistAndDecodeOnTheDevice() {
        val report = StringBuilder("API ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL}):")
        samples.forEach { (name, text, recoverable) ->
            assertTrue("$name is not supported on API ${Build.VERSION.SDK_INT}", Charset.isSupported(name))
            val charset = Charset.forName(name)
            val bytes = text.toByteArray(charset)
            assertEquals("$name round trip", text, String(bytes, charset))
            assertEquals("$name declared", text, TextRecovery.decode(bytes, name))
            val guessed = TextRecovery.decodeGuessing(bytes)
            if (recoverable) assertEquals("$name undeclared", text, guessed) else assertTrue("$name undeclared must not be empty", guessed.isNotEmpty())
            val word = "=?$name?B?${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}?="
            assertEquals("$name RFC 2047", text, TextRecovery.repairHeader(word))
            val raw = String(bytes, StandardCharsets.ISO_8859_1)
            if (recoverable) assertEquals("$name raw header", text, TextRecovery.repairHeader(raw))
            report.append(" $name=").append(if (recoverable) "declared+undeclared" else "declared")
        }
        Log.i(TAG, report.toString())
    }

    private companion object {
        const val TAG = "CharsetDeviceTest"
    }
}
