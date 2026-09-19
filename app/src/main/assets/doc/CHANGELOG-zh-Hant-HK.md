******

### 發行歷史

******

# v1.0.0

###### 2026/09/19

* `提示` P0 開發預覽: 倉庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 插件中心識別的插件身份. Binder 契約, 腳本 API 與設定頁按 ROADMAP.md 的階段推進.
* `新增` 插件標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答插件資訊, 能力, 服務商與已儲存帳戶列表以及會話信封 (具體操作隨 P2 落地)
* `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 認證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
* `新增` 郵件核心帳戶層 (路線圖 P2.1): 帳戶選項支援 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 與 Aliyun 的服務商預設, 按協議區分的逾時, `tls.trustAll`, IMAP `ID` 命令與脫敏的 `debug` 協議摘要; 工作階段延遲連線, 閒置時中斷並在斷線後重連; `session.test` 經 Binder 回傳各端點的能力與往返耗時
* `新增` 寄信 (路線圖 P2.2): `mail.send` 支援 to / cc / bcc / replyTo, 純文字與 HTML 內文 (`multipart/alternative`), 經宿主傳入的檔案描述符讀取的附件與內嵌圖片 (`multipart/mixed` / `multipart/related`), 自訂標頭, 優先級, `inReplyTo` / `references` 與日期; 收件人, 附件與標頭的上限, 拒絕標頭注入; `saveToSent` 僅在服務商不自動儲存時經 IMAP 附加副本; `messages.append` 寫入草稿並回傳 UID; 已在真機上對 QQ 郵箱與 Gmail 驗證
* `新增` 收信 (路線圖 P2.3): `folders.list` (樹形, special-use 角色取自 LIST / XLIST 屬性或約定名稱, 可選計數), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 為游標 (`before` / `after`), 支援 `order` 與 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查詢 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 標記 / 大小 / `header` / `messageId` / `uid` 與 `and` / `or` / `not`) 編譯為 IMAP SEARCH, 伺服器拒絕時在客戶端過濾 (`fallback`); `messages.get` 回傳純文字與 HTML 內文 (只有 HTML 的郵件派生文字), 全部標頭, 帶 `partId` 的附件列表, 內嵌預算 (`bodyTruncated` / `bodyParts`) 與 `includeRaw`; `attachments.download` 與 `messages.raw` 串流寫入宿主描述符並回報進度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字元集復原涵蓋 GBK / GB 18030 / ISO-2022-JP, 未聲明與未知字元集, 原始 8 位標頭與 RFC 2231 / 2047 檔名, 檔名淨化; IMAP `ID` 命令在每條連線上發送 (163 / 126 拒絕未識別的連線), 伺服器不能解析 `UID EXPUNGE` 時改用整個資料夾的 `EXPUNGE`; 已在真機上對 QQ 郵箱, 163 郵箱與 Gmail 驗證
* `新增` POP3 (路線圖 P2.4): `receive: "pop3"` 的帳戶透過同一組操作讀取唯一的 `INBOX`, `uid` 為 UIDL 字串: `folders.list` 只回傳 `INBOX` (可帶計數), `messages.list` 依 UIDL 游標分頁且只取信頭 (`TOP`), `messages.search` 在客戶端依信頭由新到舊過濾, 湊夠 `limit` 筆即停 (最多 200 個候選, 每個一次 `TOP` 往返), `messages.get` / `messages.raw` / `attachments.download` 下載整封郵件, `messages.delete` 發 `DELE` 並在郵箱關閉時生效; 標記, 移動, 複製, expunge, 附加, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 與內文搜尋在連線之前即回傳 `UNSUPPORTED_OPERATION`; 已在真機上對 QQ 郵箱驗證
* `新增` Binder 會話控制 (路線圖 P2.5): 只有已安裝且與本插件同簽署的 AutoJs6 宿主能開啟會話或列出已儲存帳戶 (否則 `SecurityException`, 規則與 MCP Server 插件一致); 請求與回應信封不超過 `MAX_ENVELOPE_BYTES`, 錯誤訊息不超過 `MAX_ERROR_MESSAGE_BYTES`; 每個會話依序執行呼叫, 執行中的呼叫之後最多排隊 `MAX_QUEUED_CALLS` 個, 再多則 `LIMIT_EXCEEDED`; `cancel` 立即回應排隊中的呼叫, 並透過關閉插槽中斷執行中的呼叫, 伺服器無回應時不再等到讀取逾時; `close` 讓全部待處理呼叫回應 `SESSION_CLOSED`; `getStatus` 回報 `queued` 與 `active`; 操作表標明每個操作支援的收信協定, POP3 帳戶在解析參數前即被拒絕; 能力集宣告 `append` 與 `clientSearchFallback`
* `新增` 10 種語言的 README, 插件中心說明與更新日誌
* `新增` 已儲存帳戶儲存 (路線圖 P4.1): 在插件內儲存的帳戶把非秘密的帳戶文件與經 Android Keystore 主密鑰 AES-256-GCM 加密的密碼或存取權杖存在一起; 認證資料綁定別名, 秘密類型與帳戶文件, 在磁碟上被改動或移動的記錄不再能解密; 記錄存於 `noBackupFilesDir` (本已排除在備份之外), 在檔案鎖下原子寫入, 秘密只經過用後即清零的 `CharArray` / `ByteArray` 緩衝區; 別名去除首尾空白, 經 NFC 正規化且不區分大小寫
* `新增` 已儲存帳戶會話 (路線圖 P4.3): `openSession` 接受別名形態 (`accountAlias`) 並在插件進程內解密秘密, `mail.connect('alias')` 因此不經 Binder 傳遞任何憑證; `listSavedAccounts` 回傳每個已儲存帳戶的別名, 地址, 使用者名稱, 服務商, 認證方式, 收信協定, 端點與預設標記, 不含任何秘密; 能力集合新增 `savedAccounts`
* `新增` 設定頁 (路線圖 P4.2): 啟動器圖示開啟帳戶頁, 列出每個已儲存帳戶的地址, 服務商, 收信協定與認證方式, 提供編輯, 測試連接, 設為或取消預設以及刪除; 帳戶編輯頁可按服務商預設自動填入, 也可填寫自訂 IMAP / POP3 / SMTP 伺服器及其加密方式與連接埠, 密碼或存取權杖直接從輸入欄讀入用後即清零的 `CharArray`, 編輯時留空則沿用已儲存的秘密, 儲存前可對所填伺服器執行 `session.test` 並按協定顯示結果與耗時而不寫入磁碟; 頁面跟隨 AutoJs6 宿主的主題, 夜間模式與語言, 編輯頁重建後還原除秘密外的全部欄位
* `新增` 設定入口 (路線圖 P4.3): AutoJs6 宿主經匯出的 `org.autojs.plugin.MAIL_SETTINGS` 活動開啟帳戶頁, 該活動要求插件權限, 只接受無參數的請求並立即結束; 能力集合宣告 `mailSettingsVersion` 1; 啟動器入口本身不帶該權限
* `修復` 服務商預設 (路線圖 P3.2): 163 郵箱與 126 郵箱會在伺服器端保存每封經 SMTP 發出的郵件, 兩者的 `autoSavesSent` 改為 true, 預設 `saveToSent` 不再向 `已发送` 追加第二份副本 (真實 163 帳戶核實: `saveToSent: false` 發出的郵件數分鐘後出現在已發送資料夾)
* `修復` AGP 9.1 構建時的 SDK XML v4 解析警告及 JVM 單元測試組裝任務誤觸發 APK 原生程式庫對齊檢查的問題 (共用構建外掛 1.8.3)
* `優化` 錯誤映射: POP3 伺服器在登入後因帳戶未開啟 POP 存取而拒絕郵箱 (Gmail 對 STAT 答 `[SYS/PERM] Your account is not enabled for POP access`) 時, 現在得到說明原因的 `UNSUPPORTED_OPERATION`, 而不是可重試的 `IO_FAILED` "I/O failed"
* `優化` 服務商預設: Sina 郵箱補上已發送資料夾名 (`已发送`), 並註明伺服器不保存已發郵件副本且拒絕 IMAP CREATE (資料夾只能在網頁端建立); 126 郵箱伺服器保存已發郵件副本已用真實帳戶驗證
* `依賴` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 與 Jakarta Activation API 2.1.4
* `依賴` 附加 GreenMail 2.1.13 用於 JVM 郵件核心測試 (僅測試範圍)
* `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為共用插件契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
* `依賴` 附加 `mail-api.aar` (AutoJs6 模組 `plugin-api/mail-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為郵件 Binder 契約 (六個 AIDL 介面, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 並在 `locks/host-api-aars.lock` 中鎖定雜湊
