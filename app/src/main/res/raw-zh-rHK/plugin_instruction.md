Angus Mail 為 AutoJs6 腳本提供全域物件 `mail`, 用於傳送郵件, 列出和搜尋郵箱, 讀取內文, 下載附件, 管理標記與資料夾, 以及監聽資料夾中的新郵件. 它基於 Jakarta Mail 的參考實作 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 透過 TLS 使用 IMAP, POP3 和 SMTP 協定.

版本 1.2.1 在 1.1.0 的後台守望 (路線圖 P8) 之上新增 Google 與 Microsoft 帳號的瀏覽器登入 (路線圖 P9); P0 至 P8 各階段的全部條目已隨 1.0.0 至 1.1.0 發佈, 證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (build 5282) 或更高版本; "郵件到達時" 任務需要攜帶郵件契約版本 2 的宿主建置; 完整的指令碼 API 參考見 [AutoJs6 文件](https://docs.autojs6.com/#/mail).

### 使用方法

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝插件 APK.
2. 開啟 AutoJs6 插件中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳戶: 在服務商的網頁設定中開啟 IMAP 或 POP3 與 SMTP, 並取得授權碼 (QQ, 163, 126, Sina) 或應用程式專用密碼 (Gmail, iCloud, Yahoo); 登入密碼本身通常不被接受. Gmail, Outlook.com 與 Microsoft 365 帳戶也可以在外掛設定頁經瀏覽器以 Google 或 Microsoft 帳號登入 (驗證方式選擇 "使用 Google / Microsoft 帳號登入 (瀏覽器)"), 或提供在別處取得的 OAuth 2.0 存取權杖.
4. 在腳本中呼叫 `mail.connect(...)`, 或在插件設定頁 (插件的啟動器圖示, 或 AutoJs6 開發者選項 > 電郵帳戶設定) 儲存帳戶後以別名連線.
5. 要在新郵件到達時運行腳本而無需常駐腳本: 在插件的守望頁面 (設定 > 守望: 帳戶別名, 資料夾, 模式, 過濾) 新增守望, 按提示允許通知, 然後在 AutoJs6 中建立任務 (長按腳本 > 定時任務 > 廣播觸發 > 郵件到達時) 並選擇守望; 該任務需要攜帶郵件契約版本 2 的 AutoJs6 構建.

連接指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md).
