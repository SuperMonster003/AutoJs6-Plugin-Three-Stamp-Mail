******

### 發行歷史

******

# v1.0.0

###### 2026/09/18

* `提示` P0 開發預覽: 倉庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 插件中心識別的插件身份. Binder 契約, 腳本 API 與設定頁按 ROADMAP.md 的階段推進.
* `新增` 插件標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答插件資訊, 能力, 服務商與已儲存帳戶列表以及會話信封 (具體操作隨 P2 落地)
* `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 認證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
* `新增` 10 種語言的 README, 插件中心說明與更新日誌
* `依賴` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 與 Jakarta Activation API 2.1.4
* `依賴` 附加 GreenMail 2.1.13 用於 JVM 郵件核心測試 (僅測試範圍)
* `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為共用插件契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `依賴` 附加 `mail-api.aar` (AutoJs6 模組 `plugin-api/mail-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為郵件 Binder 契約 (六個 AIDL 介面, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 並在 `locks/host-api-aars.lock` 中鎖定雜湊
