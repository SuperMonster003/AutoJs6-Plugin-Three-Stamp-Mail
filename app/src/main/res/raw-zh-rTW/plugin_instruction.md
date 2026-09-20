Angus Mail 為 AutoJs6 指令碼提供全域物件 `mail`, 用於傳送郵件, 列出和搜尋信箱, 讀取內文, 下載附件, 管理標記與資料夾, 以及監聽資料夾中的新郵件. 它基於 Jakarta Mail 的參考實作 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 透過 TLS 使用 IMAP, POP3 和 SMTP 協定.

版本 1.0.0 是首個正式版本: 路線圖 P0 至 P6 的全部條目 (郵件核心, Binder 契約, 指令碼 API, 設定頁與別名帳號, 新郵件監聽, 以及 TLS, 字元集, 服務商, 生命週期, 敵意輸入, 秘密稽核與效能矩陣) 均已完成並附有證據, 見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本; 指令碼 API 的完整參考見 [AutoJs6 文件](https://docs.autojs6.com/#/mail).

### 使用方式

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳號: 在郵件服務商的網頁端開啟 IMAP 或 POP3 與 SMTP, 並取得授權碼 (QQ, 163, 126, Sina), 應用程式專用密碼 (Gmail, iCloud, Yahoo) 或 OAuth 2.0 存取權杖 (Outlook.com); 登入密碼本身通常不被接受.
4. 在指令碼中呼叫 `mail.connect(...)`, 或在外掛程式設定頁 (外掛程式的啟動器圖示, 或 AutoJs6 開發者選項 > 郵件帳戶設定) 儲存帳號後以別名連線.

連線指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md).
