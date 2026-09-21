Angus Mail 為 AutoJs6 指令碼提供全域物件 `mail`, 用於傳送郵件, 列出和搜尋信箱, 讀取內文, 下載附件, 管理標記與資料夾, 以及監聽資料夾中的新郵件. 它基於 Jakarta Mail 的參考實作 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 透過 TLS 使用 IMAP, POP3 和 SMTP 協定.

版本 1.1.0 新增背景守望 (路線圖 P8): 守望頁面, 前景服務與 AutoJs6 的 "郵件到達時" 任務; P0 至 P7 的全部條目已隨 1.0.0 與 1.0.1 發佈, 證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本; "郵件到達時" 任務需要攜帶郵件契約版本 2 的宿主組建; 指令碼 API 的完整參考見 [AutoJs6 文件](https://docs.autojs6.com/#/mail).

### 使用方式

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳號: 在郵件服務商的網頁端開啟 IMAP 或 POP3 與 SMTP, 並取得授權碼 (QQ, 163, 126, Sina), 應用程式專用密碼 (Gmail, iCloud, Yahoo) 或 OAuth 2.0 存取權杖 (Outlook.com); 登入密碼本身通常不被接受.
4. 在指令碼中呼叫 `mail.connect(...)`, 或在外掛程式設定頁 (外掛程式的啟動器圖示, 或 AutoJs6 開發者選項 > 郵件帳戶設定) 儲存帳號後以別名連線.
5. 要在新郵件到達時執行指令碼而無需常駐指令碼: 在外掛的守望頁面 (設定 > 守望: 帳戶別名, 資料夾, 模式, 過濾) 新增守望, 依提示允許通知, 然後在 AutoJs6 中建立任務 (長按指令碼 > 定時任務 > 廣播觸發 > 郵件到達時) 並選擇守望; 該任務需要攜帶郵件契約版本 2 的 AutoJs6 組建.

連線指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md).
