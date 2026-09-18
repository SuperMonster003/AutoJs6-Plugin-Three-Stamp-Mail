******

### 發行歷史

******

# v1.0.0

###### 2026/09/18

* `提示` P0 開發預覽: 倉庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 插件中心識別的插件身份. Binder 契約, 腳本 API 與設定頁按 ROADMAP.md 的階段推進.
* `新增` 插件標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答插件資訊, 能力, 服務商與已儲存帳戶列表以及會話信封 (具體操作隨 P2 落地)
* `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 認證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
* `新增` 郵件核心帳戶層 (路線圖 P2.1): 帳戶選項支援 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 與 Aliyun 的服務商預設, 按協議區分的逾時, `tls.trustAll`, IMAP `ID` 命令與脫敏的 `debug` 協議摘要; 工作階段延遲連線, 閒置時中斷並在斷線後重連; `session.test` 經 Binder 回傳各端點的能力與往返耗時
* `新增` 寄信 (路線圖 P2.2): `mail.send` 支援 to / cc / bcc / replyTo, 純文字與 HTML 內文 (`multipart/alternative`), 經宿主傳入的檔案描述符讀取的附件與內嵌圖片 (`multipart/mixed` / `multipart/related`), 自訂標頭, 優先級, `inReplyTo` / `references` 與日期; 收件人, 附件與標頭的上限, 拒絕標頭注入; `saveToSent` 僅在服務商不自動儲存時經 IMAP 附加副本; `messages.append` 寫入草稿並回傳 UID; 已在真機上對 QQ 郵箱與 Gmail 驗證
* `新增` 收信 (路線圖 P2.3): `folders.list` (樹形, special-use 角色取自 LIST / XLIST 屬性或約定名稱, 可選計數), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 為游標 (`before` / `after`), 支援 `order` 與 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查詢 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 標記 / 大小 / `header` / `messageId` / `uid` 與 `and` / `or` / `not`) 編譯為 IMAP SEARCH, 伺服器拒絕時在客戶端過濾 (`fallback`); `messages.get` 回傳純文字與 HTML 內文 (只有 HTML 的郵件派生文字), 全部標頭, 帶 `partId` 的附件列表, 內嵌預算 (`bodyTruncated` / `bodyParts`) 與 `includeRaw`; `attachments.download` 與 `messages.raw` 串流寫入宿主描述符並回報進度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字元集復原涵蓋 GBK / GB 18030 / ISO-2022-JP, 未聲明與未知字元集, 原始 8 位標頭與 RFC 2231 / 2047 檔名, 檔名淨化; IMAP `ID` 命令在每條連線上發送 (163 / 126 拒絕未識別的連線), 伺服器不能解析 `UID EXPUNGE` 時改用整個資料夾的 `EXPUNGE`; 已在真機上對 QQ 郵箱, 163 郵箱與 Gmail 驗證
* `新增` 10 種語言的 README, 插件中心說明與更新日誌
* `依賴` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 與 Jakarta Activation API 2.1.4
* `依賴` 附加 GreenMail 2.1.13 用於 JVM 郵件核心測試 (僅測試範圍)
* `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為共用插件契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `依賴` 附加 `mail-api.aar` (AutoJs6 模組 `plugin-api/mail-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為郵件 Binder 契約 (六個 AIDL 介面, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 並在 `locks/host-api-aars.lock` 中鎖定雜湊
