******

### 發行歷史

******

# v1.0.0

###### 2026/09/18

* `提示` P0 開發預覽: 儲存庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 外掛程式中心識別的外掛程式身分. Binder 契約, 指令碼 API 與設定頁按 ROADMAP.md 的階段推進.
* `新增` 外掛程式標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答外掛程式資訊, 能力, 服務商與已儲存帳戶列表以及工作階段信封 (具體操作隨 P2 落地)
* `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 驗證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
* `新增` 郵件核心帳戶層 (路線圖 P2.1): 帳戶選項支援 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 與 Aliyun 的服務商預設, 按協定區分的逾時, `tls.trustAll`, IMAP `ID` 命令與脫敏的 `debug` 協定摘要; 工作階段延遲連線, 閒置時中斷並在斷線後重連; `session.test` 經 Binder 回傳各端點的能力與往返耗時
* `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
* `相依性` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 與 Jakarta Activation API 2.1.4
* `相依性` 附加 GreenMail 2.1.13 用於 JVM 郵件核心測試 (僅測試範圍)
* `相依性` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為共用外掛程式契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `相依性` 附加 `mail-api.aar` (AutoJs6 模組 `plugin-api/mail-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為郵件 Binder 契約 (六個 AIDL 介面, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 並在 `locks/host-api-aars.lock` 中鎖定雜湊
