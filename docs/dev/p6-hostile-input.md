# P6 hostile input (bounded documents, lenient decoding, no half-written files)

Roadmap P6 "敌意输入", run on 2026-09-19 against plugin build 37 (`be68c33`) and the host mail
packages of `051ff078f`. The enforcing tests are `HostileInputTest` (12 cases, in memory) and
`HostileInputGreenMailTest` (5 cases, the same messages appended to GreenMail and read back over
IMAP and POP3) in `mail-core`; every case is part of `:mail-core:test`.

## What changed (D39)

Three defects came out of the first run and are fixed in plugin build 38:

1. A part with damaged base64 or an unknown `Content-Transfer-Encoding` made Jakarta throw an
   `IOException` / `MessagingException` while the body was read; the connection guard takes any
   `IOException` for a lost connection, so one broken mail dropped a healthy IMAP connection and
   answered `IO_FAILED`. `MimeLeniency` now installs the Jakarta switches that read such parts
   leniently (invalid base64 / uuencode characters skipped, unknown encodings read as identity,
   malformed parameters skipped, empty or boundary-less multiparts accepted, encoded words
   decoded leniently and inside file names). Four switches are static in the Jakarta classes, so
   they are installed from every mail-core entry point (`MailAccountOptions`,
   `OutgoingMessageParser`, `MimeTree`, `MessageMapper`, `MailSessionFactory`), from the plugin
   service's `onCreate`, and by the mail-core test task on its JVM.
2. A header bomb (20000 recipients, a 1 MiB subject, thousands of headers) produced a message
   document far above `MAX_ENVELOPE_BYTES`; the response check then refused the whole listing
   page with `LIMIT_EXCEEDED`, so one hostile mail blocked `messages.list` for its page. The
   mapper now bounds every document (plugin constants in `MailLimits`):

   | Bound | Value | Applies to |
   | --- | --- | --- |
   | `MAX_RECIPIENTS` (shared) | 500 | `to` + `cc` + `bcc` + `replyTo` of one document, in that order |
   | `MAX_ADDRESS_CHARS` | 320 | each address, display name, content id and header name |
   | `MAX_HEADER_VALUE_CHARS` | 4096 | subject, message id, in-reply-to, declared file name, each header value |
   | `MAX_HEADERS_BYTES` | 64 KiB | the `headers` map of `messages.get`; a header that does not fit is dropped, later ones still fit |
   | `MAX_MIME_DEPTH` | 32 | multipart nesting; a deeper multipart is one downloadable leaf (`multipart/*`) |
   | `MAX_MIME_PARTS` | 256 | leaves per message; later parts are neither listed nor downloadable (`ATTACHMENT_NOT_FOUND`) |
   | `MAX_INLINE_BODY_BYTES` (existing) | 256 KiB | a body or raw source of unknown size is now read no further than the budget |

   With these, one message document always fits the response envelope: a script that hits
   `LIMIT_EXCEEDED` on a page can always page down to `limit: 1`.
3. A multipart nested more deeply than the JVM stack recursed without a cap in `MimeTree`, and a
   multipart whose parts cannot be parsed was silently empty. Both are one leaf now, so the
   content stays reachable through `attachments.download`.

Also: `sanitizeFileName` turns a name made only of dots (`..`) into `attachment` (the host's
`MailFileNames.sanitize` already did, the plugin side now agrees), and a leaf whose
`Content-Type` is not a valid `type/subtype` is `application/octet-stream`.

## Cases

Input built by `Hostile` (raw RFC 822 text, ISO-8859-1 bytes); "in memory" = `HostileInputTest`,
"server" = `HostileInputGreenMailTest` (IMAP BODYSTRUCTURE tree and section downloads, POP3
`TOP` envelopes and `RETR`).

| Roadmap case | Input | Behaviour | Where |
| --- | --- | --- | --- |
| 嵌套过深的 MIME | `multipart/mixed` nested 2000 levels (40 on the server) | one leaf with a 32-component part id, `multipart/mixed`, downloadable raw (contains the innermost text); a tree of depth 10 is walked to its text; `partId + ".1"` answers `ATTACHMENT_NOT_FOUND` | in memory, server |
| 超长 MIME (wide) | 5000 sibling attachments (1000 on the server) | 256 leaves, part 256 downloadable, part 257 `ATTACHMENT_NOT_FOUND`, document fits | in memory, server |
| 20000 个收件人的信头 | `To`, `Cc`, `Bcc`, `Reply-To` with 20000 addresses each (about 1.5 MB of headers) | `to` holds the first 500, the other lists are empty (shared budget), the `To` header value is cut to 4096 characters, the envelope and the full document fit; POP3 `TOP` envelopes are bounded the same way | in memory, server |
| 超长主题 / 信头 | 1 MiB subject, 10000-character `Message-ID` and `In-Reply-To`, 3000 headers of 1000 characters, a 2000-character header name | subject and ids cut to 4096, headers map within 64 KiB with the small header after the bombs still present, header name cut to 320, document fits | in memory |
| 无 `Content-Type` 的部件 | a message and a body part without `Content-Type` | read as `text/plain` | in memory |
| 无 boundary 参数 | `multipart/mixed` without `boundary` | Jakarta takes the first `--` line as the boundary; the text is read | in memory |
| 空 / 不可解析的 multipart | no parts at all; content without any boundary line | empty: no leaves, no attachments; unparsable: one `multipart/mixed` leaf with part id `1` | in memory |
| 非法 base64 | `aGVsbG8g!!!d29ybGQ=` | `hello world` (invalid characters skipped), no exception, the IMAP connection is kept (`connectCount == 1`) | in memory, server |
| 未知传输编码 | `Content-Transfer-Encoding: x-unknown` | the bytes are read as identity through `attachments.download` | in memory, server |
| 递归 `message/rfc822` | `message/rfc822` nested 100 levels | one leaf `2` (`part-2.eml`), never enumerated, download contains the innermost header | in memory |
| 文件名含 `../` | `filename="../../etc/passwd"`, RFC 2231 `..%2F..%2Fx.txt`, `..%5C..%5Cwin.ini` | `.._.._etc_passwd`, `.._.._x.txt`, `.._.._win.ini` | in memory, server |
| 文件名含 NUL | `nul<NUL>byte.txt` | `nul_byte.txt` | in memory, server |
| 文件名 2000 字符 | 2000 `n` plus `.txt` | 255 characters (`MAX_FILE_NAME_LENGTH`) | in memory |
| 文件名 `..` | `filename=".."` | derived name `part-6...`, never `..` | in memory, server |
| `Content-Disposition` 与 `Content-Type` 文件名冲突 | `Content-Type: image/png; name="pic.png"` + `Content-Disposition: attachment; filename="evil.exe"` | the disposition name wins (`evil.exe`) as RFC 2183 says, the MIME type stays `image/png` (never derived from the extension); with only the `Content-Type` name, `only-type.png` | in memory, server |
| 声明大小与实际不符的附件 | 1000 random bytes as base64 | the document carries the encoded size (about 1.37 KB, larger than the payload), the transfer returns the 1000 decoded bytes, a transfer limit of 500 stops it with `LIMIT_EXCEEDED` (not retryable); the host treats the declared size as a hint only (see below) | in memory |
| 超过上限的 JSON 信封 | a request or response of `MAX_ENVELOPE_BYTES + 1` bytes | `LIMIT_EXCEEDED` (not retryable) by `Limits.checkRequest` / `checkResponse` before parsing and before answering (`LimitsTest`, app module; `MailSessionBinder`) | app |
| 非法 UTF-8 主题 | `=?UTF-8?B?/v/A?=`, `=?GB2312?B?gIA=?=`, raw `\xFF\xFE\xFD`, `=?UNKNOWN-CS?Q?abc?=`, an 8-bit display name, `charset="../../x"` and `charset="notacharset"` | replacement characters, the raw bytes as Latin-1, the encoded word kept literally, the name repaired, the body decoded through the charset fallback chain; every document serializes to valid JSON | in memory |
| 超大正文 | a 306 KiB text part, `includeRaw` | `bodyTruncated`, the part in `bodyParts`, `text` null, `rawTruncated`; `BodyExtractor.bytes` stops at the budget without buffering the rest | in memory |

## The host does not leave half-written files

The host's `MailAttachmentSink.spool` (AutoJs6 `core/plugin/mail`) writes every download into
`<target>.part` and renames it onto the target only after the copy succeeded; on any failure the
part file is deleted and the target is never created. `MailAttachmentSinkTest` (host module)
covers a stream shorter than declared (`IO_FAILED`), longer than declared (`IO_FAILED`), above
the ceiling (`LIMIT_EXCEEDED`), cancelled (`CANCELLED`) and an `IOException` from the pipe
(`IO_FAILED`), asserting after each that neither the target nor the part file exists. The
script-facing `download` passes no expected size to the sink (`MailClientNativeObject`): the
encoded size from the document only budgets the call timeout, because the decoded byte count
of a base64 part is always smaller than the declared one.

## Reproducing

```
./gradlew.bat :mail-core:test --tests "*HostileInput*"
./gradlew.bat :app:testDebugUnitTest --tests "*LimitsTest"
cd D:/idea-projects/AutoJs6 && ./gradlew.bat :app:testDebugUnitTest --tests "*MailAttachmentSinkTest"
```
