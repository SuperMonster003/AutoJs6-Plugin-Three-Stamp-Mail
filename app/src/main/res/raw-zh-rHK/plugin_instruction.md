Angus Mail 為 AutoJs6 腳本提供全域物件 `mail`, 用於傳送郵件, 列出和搜尋郵箱, 讀取內文, 下載附件, 管理標記與資料夾, 以及監聽資料夾中的新郵件. 它基於 Jakarta Mail 的參考實作 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 透過 TLS 使用 IMAP, POP3 和 SMTP 協定.

版本 1.0.0 處於開發階段: 倉庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 插件中心識別的插件身份已經就緒; Binder 契約, 腳本 API 與設定頁按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 的階段推進. 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本.

### 使用方法

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝插件 APK.
2. 開啟 AutoJs6 插件中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳戶: 在郵件服務商的設定中開啟 IMAP 或 POP3, 並取得授權碼或應用程式專用密碼 (QQ, 163, 126, Gmail, iCloud), 或 OAuth 2.0 存取權杖 (Outlook.com).
4. 在腳本中呼叫 `mail.connect(...)`, 或在插件設定頁儲存帳戶後以別名連線.

連接指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md).
