# P6 charset matrix (subject, display name, body, file names)

Roadmap P6 "字符集矩阵", run on 2026-09-19 against plugin build 39 (`70b3989`). The enforcing
tests are `CharsetMatrixTest` (4 cases, `mail-core`, fixtures built in memory from the sample
texts) and `CharsetDeviceTest` (1 case, app `androidTest`, run on API 24 / 28 / 33).

## Samples

| Charset | Text (the full-width punctuation of the samples is written as code points here) | Why this text |
| --- | --- | --- |
| UTF-8 | `你好 こんにちは 안녕 café` | four scripts in one string |
| GB18030 | `你好` U+FF0C `𠮷野家的报表` U+3002 | `𠮷` (U+20BB7) needs a four-byte GB 18030 sequence |
| GBK | `你好` U+FF0C `這是一封測試郵件` U+3002 | traditional characters outside GB 2312 |
| GB2312 | `你好` U+FF0C `这是一封测试邮件` U+3002 | the common simplified case |
| Big5 | `測試郵件` U+FF0C `你好` U+3002 | traditional Chinese |
| ISO-2022-JP | `こんにちは` U+3001 `テストメールです` U+3002 | 7-bit with `ESC $ B` / `ESC ( B` switches |
| EUC-KR | `안녕하세요, 테스트 메일입니다.` | Korean |

Each sample appears in a message as the subject, the `From` display name, a `text/plain` body
(8bit), an RFC 2231 `filename*=charset''...` and an RFC 2047 word in `Content-Type; name=`,
in three forms: declared (encoded words and charset parameters), undeclared (the raw bytes on
the wire, no encoded word, no charset parameter) and mis-declared (a charset that cannot hold
the bytes).

## Results (`CharsetMatrixTest`, JDK 21; identical on the devices)

| Charset | Declared: subject / name / body / RFC 2231 / RFC 2047 name | Undeclared: subject / name / body / raw file name |
| --- | --- | --- |
| UTF-8 | all correct | all recovered |
| GB18030 | all correct | all recovered |
| GBK | all correct | all recovered |
| GB2312 | all correct | all recovered |
| Big5 | all correct | mojibake (never an error): Big5 pairs are valid GB 18030 pairs, so the chain cannot tell them apart |
| ISO-2022-JP | all correct | subject and body recovered (new escape detection); the display name and the quoted file name are lost to the parsers (`ESC ( B` reads as an RFC 822 comment in an address, the ESC control character ends a quoted parameter, the file name derives to `part-2`) |
| EUC-KR | all correct | mojibake (never an error), same reason as Big5 |

Mis-declared:

| Declaration | Bytes | Result |
| --- | --- | --- |
| `gb2312` | GBK-only characters (the 163 / QQ habit) | correct: the strict GB 2312 decoder rejects them, the chain reads GB 18030 |
| `us-ascii` | UTF-8 | correct after the fix below |
| `x-mail-cn` (unknown) | GB 18030 | correct through the chain |
| `gb2312` | Big5 | mojibake, never an error |

## What changed (build 40)

1. Jakarta maps a declared `us-ascii` to `ISO-8859-1`, which decodes every byte without an
   error, so UTF-8 (or GB 18030) bytes under an ASCII or Latin-1 declaration came out as Latin-1
   mojibake. `TextRecovery.decode` now sends an ISO-8859-1 or US-ASCII declaration straight to
   the guessing chain: ASCII bytes read the same there, and 8-bit bytes under such a declaration
   are UTF-8 or GB 18030 far more often than Latin-1 (Latin-1 remains the last resort).
2. `TextRecovery.decodeGuessing` and `repairHeader` detect the ISO-2022-JP escape sequences
   (`ESC $ B`, `ESC $ @`, `ESC ( J`) and decode such bytes as ISO-2022-JP before trying UTF-8;
   raw ISO-2022-JP subjects and bodies (7-bit, so they were "valid UTF-8" with the escapes left
   in the text) now read correctly.

The chain is therefore: ISO-2022-JP when its escapes are present, UTF-8 when the bytes are
valid UTF-8, else GB 18030, else ISO-8859-1. Big5 and EUC-KR need their declaration; all real
providers in the matrix declare them.

## Device runtime (`CharsetDeviceTest`)

All seven charsets exist on the device runtime (ICU), the round trip through `TextRecovery`
(declared, undeclared, RFC 2047 word, raw header) matches the JVM on every device:

| Device | API | Result |
| --- | --- | --- |
| AVD_API_24 (x86) | 24 | UTF-8, GB18030, GBK, GB2312, ISO-2022-JP declared + undeclared; Big5, EUC-KR declared |
| Sony G8441 | 28 | same |
| Redmi 22120RN86C | 33 | same |

## Reproducing

```
./gradlew.bat :mail-core:test --tests "*CharsetMatrixTest" --tests "*MessageMapperFixturesTest"
adb -s <serial> uninstall io.github.supermonster003.autojs6.plugin.angus.mail
ANDROID_SERIAL=<serial> ./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.supermonster003.autojs6.plugin.angus.mail.CharsetDeviceTest
adb -s <serial> logcat -d -s CharsetDeviceTest
```
