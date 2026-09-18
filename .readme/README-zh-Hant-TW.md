<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>讓 AutoJs6 指令碼透過 IMAP, POP3 和 SMTP 收發, 搜尋和監聽郵件</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### 簡介

******

Angus Mail 為 AutoJs6 指令碼提供全域物件 `mail`, 用於傳送郵件, 列出和搜尋信箱, 讀取內文, 下載附件, 管理標記與資料夾, 以及監聽資料夾中的新郵件. 它基於 Jakarta Mail 的參考實作 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 透過 TLS 使用 IMAP, POP3 和 SMTP 協定.

全部郵件流量都在外掛程式的程序內完成. AutoJs6 透過 Binder 服務發現外掛程式, 交出指令碼提供的帳號 (或外掛程式設定頁中儲存的別名), 並接收 JSON 結果與附件串流; 宿主本身不含任何郵件程式碼. 除非你選擇在外掛程式中儲存帳號, 憑證只在工作階段生命週期內駐留記憶體.

******

### 目前狀態

******

版本 1.0.0 處於開發階段: 儲存庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 外掛程式中心識別的外掛程式身分已經就緒; Binder 契約, 指令碼 API 與設定頁按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 的階段推進. 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本.

******

### 功能

******

外掛程式提供以下能力:

- 寄信: 純文字或 HTML, 多個收件者, 附件與內嵌圖片, 自訂信頭與優先順序; 服務商不自動儲存寄件備份時由外掛程式寫入伺服器.
- 收信: 按頁列出資料夾, 在伺服器端搜尋 (服務商拒絕非 ASCII 搜尋時回退到用戶端過濾), 讀取文字與 HTML 內文, 並把附件直接下載到指令碼工作目錄.
- 整理: 標記已讀或星號, 移動, 複製, 刪除, 清除, 以及建立, 重新命名或刪除資料夾; POP3 帳號獲得唯讀子集.
- 監聽: 在指令碼執行期間透過 IMAP IDLE 接收新郵件事件, 伺服器或 POP3 帳號不支援時回退為輪詢.
- 服務商: 內建 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 預設, 自動填入主機, 連接埠與加密方式; 任何欄位都可為其他伺服器覆寫.
- 驗證: 密碼與服務商授權碼, 或由指令碼提供並附帶重新整理回呼的 XOAUTH2 存取權杖.

******

### 使用方式

******

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳號: 在郵件服務商的設定中開啟 IMAP 或 POP3, 並取得授權碼或應用程式專用密碼 (QQ, 163, 126, Gmail, iCloud), 或 OAuth 2.0 存取權杖 (Outlook.com).
4. 在指令碼中呼叫 `mail.connect(...)`, 或在外掛程式設定頁儲存帳號後以別名連線.

******

### 快速入門

******

一個寄送報表, 讀取帶附件的未讀郵件並等待驗證碼的指令碼:

```js
let client = mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' });

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
```

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 進入點受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外掛程式不匯出其他元件.
- INTERNET 權限只用於指令碼指定伺服器的 IMAP, POP3 和 SMTP 連線; 外掛程式不發起其他請求, 也不收集任何資料.
- 密碼與權杖從指令碼到外掛程式經 Binder 的專用欄位傳遞, 不會出現在記錄檔, JSON 文件, 錯誤訊息或當機報告中, 且只在工作階段生命週期內駐留記憶體. 設定頁儲存的帳號由 Android Keystore 金鑰加密, 並排除在備份之外.
- 連線預設使用 TLS (按服務商要求選擇 SSL 或 STARTTLS); 明文連線與自簽憑證必須為每個帳號明確宣告.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼探索外掛並協商相容性:

```text
application id: io.github.supermonster003.autojs6.plugin.angus.mail
plugin id: angus-mail
engine: mail
variant: default
service action: org.autojs.plugin.MAIL
service category: mail
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mail.api.IMailPlugin
minimum host build: 5282 (6.8.0)
```

`AngusMailPluginService` 實作宿主 mail-api 契約 `org.autojs.plugin.mail.api.IMailPlugin`, 回應 `org.autojs.plugin.MAIL` (category `mail`). `AngusMailPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主啟動外掛程式.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 依階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/18_

- `提示` P0 開發預覽: 儲存庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 外掛程式中心識別的外掛程式身分. Binder 契約, 指令碼 API 與設定頁按 ROADMAP.md 的階段推進.
- `新增` 外掛程式標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答外掛程式資訊, 能力, 服務商與已儲存帳戶列表以及工作階段信封 (具體操作隨 P2 落地)
- `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 驗證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
- `新增` 郵件核心帳戶層 (路線圖 P2.1): 帳戶選項支援 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 與 Aliyun 的服務商預設, 按協定區分的逾時, `tls.trustAll`, IMAP `ID` 命令與脫敏的 `debug` 協定摘要; 工作階段延遲連線, 閒置時中斷並在斷線後重連; `session.test` 經 Binder 回傳各端點的能力與往返耗時
- `新增` 寄信 (路線圖 P2.2): `mail.send` 支援 to / cc / bcc / replyTo, 純文字與 HTML 內文 (`multipart/alternative`), 經宿主傳入的檔案描述符讀取的附件與內嵌圖片 (`multipart/mixed` / `multipart/related`), 自訂標頭, 優先順序, `inReplyTo` / `references` 與日期; 收件者, 附件與標頭的上限, 拒絕標頭注入; `saveToSent` 僅在服務商不自動儲存時經 IMAP 附加副本; `messages.append` 寫入草稿並回傳 UID; 已在實機上對 QQ 郵箱與 Gmail 驗證
- `新增` 收信 (路線圖 P2.3): `folders.list` (樹狀, special-use 角色取自 LIST / XLIST 屬性或約定名稱, 可選計數), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 為游標 (`before` / `after`), 支援 `order` 與 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查詢 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 旗標 / 大小 / `header` / `messageId` / `uid` 與 `and` / `or` / `not`) 編譯為 IMAP SEARCH, 伺服器拒絕時在用戶端過濾 (`fallback`); `messages.get` 回傳純文字與 HTML 內文 (只有 HTML 的郵件衍生文字), 全部標頭, 帶 `partId` 的附件清單, 內嵌預算 (`bodyTruncated` / `bodyParts`) 與 `includeRaw`; `attachments.download` 與 `messages.raw` 串流寫入宿主描述符並回報進度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字元集復原涵蓋 GBK / GB 18030 / ISO-2022-JP, 未宣告與未知字元集, 原始 8 位元標頭與 RFC 2231 / 2047 檔名, 檔名淨化; IMAP `ID` 命令在每條連線上發送 (163 / 126 拒絕未識別的連線), 伺服器不能解析 `UID EXPUNGE` 時改用整個資料夾的 `EXPUNGE`; 已在實機上對 QQ 郵箱, 163 郵箱與 Gmail 驗證
- `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
- `相依性` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 與 Jakarta Activation API 2.1.4
- `相依性` 附加 GreenMail 2.1.13 用於 JVM 郵件核心測試 (僅測試範圍)
- `相依性` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
- `相依性` 附加 `mail-api.aar` (AutoJs6 模組 `plugin-api/mail-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為郵件 Binder 契約 (六個 AIDL 介面, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 並在 `locks/host-api-aars.lock` 中鎖定雜湊

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置與驗證

******

本節面向希望從原始碼建置外掛的開發者; 一般使用者直接安裝 Releases 頁面的預建 APK 即可.

建置 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

執行 JVM 單元測試並建置 instrumentation 測試 APK:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

建置 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集發行產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與產生的產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更新版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 在地化與文件產生

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 產生的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部產生的產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
