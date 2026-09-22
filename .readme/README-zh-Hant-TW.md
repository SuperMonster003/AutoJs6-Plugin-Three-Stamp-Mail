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

版本 1.2.1 在 1.1.0 的背景守望 (路線圖 P8) 之上新增 Google 與 Microsoft 帳號的瀏覽器登入 (路線圖 P9); P0 至 P8 各階段的全部條目已隨 1.0.0 至 1.1.0 發布, 證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (build 5282) 或更高版本; "郵件到達時" 任務需要攜帶郵件契約版本 2 的宿主建置; 完整的指令碼 API 參考見 [AutoJs6 文件](https://docs.autojs6.com/#/mail).

******

### 功能

******

外掛程式提供以下能力:

- 寄信: 純文字或 HTML, 多個收件者, 附件與內嵌圖片, 自訂信頭與優先順序; 服務商不自動儲存寄件備份時由外掛程式寫入伺服器.
- 收信: 按頁列出資料夾, 在伺服器端搜尋 (服務商拒絕非 ASCII 搜尋時回退到用戶端過濾), 讀取文字與 HTML 內文, 並把附件直接下載到指令碼工作目錄.
- 整理: 標記已讀或星號, 移動, 複製, 刪除, 清除, 以及建立, 重新命名或刪除資料夾; POP3 帳號獲得唯讀子集.
- 監聽: 在指令碼執行期間接收新郵件事件, 伺服器真正推送時用 IMAP IDLE, 否則輪詢 (預設 60 s, 可調): QQ 與 Sina 接受 IDLE 但不推送, 163 與 126 沒有 IDLE, POP3 帳號一律輪詢; 斷網與外掛程序重啟後監聽自動恢復.
- 背景守望: 設定頁的守望頁面在沒有指令碼執行時以前景服務保持到已儲存帳戶的 IMAP IDLE 或輪詢連線, 記錄每封新郵件, 並為所選守望喚醒 AutoJs6 的 "郵件到達時" 任務 (可依寄件者與主旨過濾), 郵件經 `engines.myEngine().execArgv.mail` 傳入指令碼.
- 服務商: 內建 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 預設, 自動填入主機, 連接埠與加密方式; 任何欄位都可為其他伺服器覆寫.
- 驗證: 密碼與服務商授權碼, 或由指令碼提供並附帶重新整理回呼的 XOAUTH2 存取權杖.
- 瀏覽器登入: Gmail, Outlook.com 或 Microsoft 365 帳戶可以在外掛設定頁經系統瀏覽器以 Google 或 Microsoft 帳號登入後新增 (OAuth 2.0 授權碼流程 + PKCE); 外掛把重新整理權杖加密儲存在本機, 每次工作階段前續期存取權杖, 並在帳戶頁顯示登入狀態與 "重新登入" / "撤銷登入" 操作. 指令碼仍按別名連線, 不接觸任何權杖.

******

### 使用方式

******

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳戶: 在服務商的網頁設定中開啟 IMAP 或 POP3 與 SMTP, 並取得授權碼 (QQ, 163, 126, Sina) 或應用程式專用密碼 (Gmail, iCloud, Yahoo); 登入密碼本身通常不被接受. Gmail, Outlook.com 與 Microsoft 365 帳戶也可以在外掛設定頁經瀏覽器以 Google 或 Microsoft 帳號登入 (驗證方式選擇 "使用 Google / Microsoft 帳號登入 (瀏覽器)"), 或提供在別處取得的 OAuth 2.0 存取權杖.
4. 在指令碼中呼叫 `mail.connect(...)`, 或在外掛程式設定頁 (外掛程式的啟動器圖示, 或 AutoJs6 開發者選項 > 郵件帳戶設定) 儲存帳號後以別名連線.
5. 要在新郵件到達時執行指令碼而無需常駐指令碼: 在外掛的守望頁面 (設定 > 守望: 帳戶別名, 資料夾, 模式, 過濾) 新增守望, 依提示允許通知, 然後在 AutoJs6 中建立任務 (長按指令碼 > 定時任務 > 廣播觸發 > 郵件到達時) 並選擇守望; 該任務需要攜帶郵件契約版本 2 的 AutoJs6 組建.

******

### 服務商準備

******

每家服務商都要先在網頁端開啟 IMAP (或 POP3) 與 SMTP, 並以授權碼, 應用程式專用密碼或存取權杖代替登入密碼; 各預設 (`provider` 的取值) 的要點:

- QQ 信箱 (`qq`): 在網頁版的帳號設定中開啟 IMAP/SMTP 服務並產生授權碼, 以授權碼作為 `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net 使用 `163` 預設並覆寫主機): 在網頁版設定的 POP3/SMTP/IMAP 頁開啟服務並產生授權碼; POP3 需要單獨開啟, 否則 IMAP 與 SMTP 接受的授權碼會被 POP3 拒絕. 伺服器要求每個 IMAP 連線先傳送 `ID` 命令 (否則回答 `Unsafe Login`), 外掛程式自動完成.
- 新浪信箱 (`sina`): 在網頁版的用戶端設定中開啟 IMAP/SMTP 服務並使用授權碼. 伺服器不儲存已寄郵件副本 (外掛程式附加到 `已发送`), 不允許經 IMAP 新建資料夾, 文字搜尋由外掛程式在用戶端完成.
- 最簡單的路徑是外掛設定頁的瀏覽器登入 ("使用 Google 帳號登入 (瀏覽器)"), 無需應用程式專用密碼且權杖自動續期. 其他方式: Gmail (`gmail`): 開啟兩步驟驗證後在 Google 帳戶中產生應用程式專用密碼作為 `password`, 或提供帶 `https://mail.google.com/` 範圍的 OAuth 2.0 存取權杖 (`accessToken` 與 `tokenProvider`); 資料夾位於 `[Gmail]` 命名空間, 新郵件由 IDLE 推送. 專案以權杖完成了真實帳號核實.
- 最簡單的路徑是外掛設定頁的瀏覽器登入 ("使用 Microsoft 帳號登入 (瀏覽器)"), 由外掛自行取得並續期權杖. 其他方式: Outlook.com / Hotmail (`outlook`) 與 Microsoft 365 (`office365`): 微軟已關閉個人帳戶的基本驗證, 應用程式密碼在 IMAP, POP3 與 SMTP 上都會被拒絕, `outlook` 預設因此只接受 OAuth 2.0 存取權杖 (`accessToken` 與 `tokenProvider`); 工作或學校帳戶 (`office365`) 可用密碼或權杖, 但租用戶原則可能停用 IMAP, POP3 或 SMTP AUTH. 權杖需要 `https://outlook.office.com/` 的委派權限 `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` 與 `SMTP.Send` (專案已用這樣的權杖核實一個個人帳號: IMAP, POP3, SMTP 與 IDLE 推送); 部分較新的個人信箱被微軟停用了 SMTP AUTH (`535 5.7.139`), 使用者設定中沒有開關.
- iCloud (`icloud`): 在 Apple 帳戶中產生 App 專用密碼; 沒有 POP3 服務.
- Yahoo (`yahoo`) 與阿里雲個人信箱 (`aliyun`): 產生應用程式密碼或授權碼; 這兩個預設按公開文件編寫, 專案沒有可用的測試帳號, 未經核實.

其他伺服器不填 `provider`, 而是給出 `imap` (或 `pop3`) 與 `smtp` 的 `host`, `port` 與 `tls` (`ssl`, `starttls` 或 `none`); 預設的任何欄位也都可以覆寫. 全部選項見 [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType), 內建預設可用 `mail.providers.list()` 檢視.

******

### 快速入門

******

一個以別名連線, 寄送報表, 讀取帶附件的未讀郵件, 監聽驗證碼並非同步搜尋的指令碼:

```js
// A saved alias keeps the credential inside the plugin; an inline account works as well:
// mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
watch.on('error', e => console.warn(e.code, e.message));

// Every network method also has an Async form; every failure is a MailError with a code.
mail.setDefault(client);
mail.searchAsync({ subject: 'invoice', since: '2026-09-01' }).then(list => console.log(list.length, list.fallback));
```

******

### 別名帳號

******

別名是儲存在外掛程式內的帳號. 在外掛程式設定頁 (啟動器圖示, 或 AutoJs6 開發者選項 > 郵件帳戶設定) 填寫帳號, 測試連線並儲存後, 密碼或權杖由 Android Keystore 金鑰加密存放在外掛程式的私有目錄中且不參與備份; 指令碼隨後以 `mail.connect('別名')` 連線, 憑證不經過指令碼, 也不經 Binder 傳遞.

設定頁可把一個帳號標記為預設, `mail.accounts.list()` 回傳的條目帶有 `default: true`, `mail.accounts.has(alias)` 檢查別名是否存在. 需要同時使用多個帳號時為每個別名各建一個用戶端; `mail.setDefault(client)` 之後, `mail.fetch(...)` 這類轉發方法直接作用於預設用戶端.

******

### 相容性

******

以下結論來自路線圖 P6 的真實帳號矩陣 (2026-09-19 與 09-20, 每家服務商向自己寄送一封帶中文主旨, 內文, 顯示名稱與附件名的郵件, 再逐項驗證), 以及 TLS, 字元集, 生命週期與效能矩陣:

- QQ 信箱: 寄信, 列表, 內文, 附件, 標記, 移動 (`MOVE`) 與 POP3 全部通過; 中文搜尋伺服器回答 0 命中而不是錯誤, 需要 `fallback: 'always'`; 寄出郵件的 Message-ID 被伺服器改寫; 不允許新建資料夾; 監聽以輪詢進行 (IDLE 不推送), 新郵件送達 15-40 s 後才在伺服器上可見.
- 163 / 126 / yeah.net: 全部通過; 伺服器儲存已寄副本; 沒有 IDLE, 監聽輪詢; 163 對近期郵件的文字搜尋回答 0 命中 (126 與 yeah.net 正常); 寄件者顯示名稱中的空格回讀為底線; yeah.net 的 POP3 需在網頁端單獨開啟.
- 新浪信箱: 通過; 伺服器只接受 ALL, SINCE 與標記類搜尋條件, 文字搜尋自動回退到用戶端過濾; 沒有 IDLE; 不允許新建資料夾; 已寄副本由外掛程式附加.
- Gmail: 以 OAuth 2.0 權杖通過全部列, 新郵件經 IDLE 推送 (約 30 s, 為 Gmail 自身的通知節奏); 含中文的伺服器搜尋全部命中 (外掛程式不啟用 `UTF8=ACCEPT`); 自訂 IMAP 關鍵字會被儲存 (六家中唯一); POP3 檢視不含帳號自己寄出的郵件.
- Outlook.com / Hotmail: 以 OAuth 2.0 權杖在一個個人帳號上通過全部列 (2026-09-21): 資料夾角色來自常規名稱, 伺服器保存已傳送副本並改寫 Message-ID, `MOVE` 與建立資料夾正常, POP3 以兩行式 `AUTH XOAUTH2` 登入, IDLE 約 10 s 內推送 (七家中最快); 中文主旨的伺服器搜尋能命中但可能耗時數分鐘; 應用程式密碼在 IMAP, POP3 與 SMTP 上仍被拒絕 (`AUTH_MECHANISM_UNSUPPORTED`), 部分較新的個人信箱被微軟停用了 SMTP AUTH (`535 5.7.139`); iCloud, Yahoo 與 Aliyun 沒有可用的測試帳號, 預設未經核實.
- TLS 與字元集: 隱含式 SSL, STARTTLS, 明文, 自簽憑證 (帶與不帶 `tls.trustAll`), 主機名稱不符與連接埠模式錯配在 IMAP, POP3 與 SMTP 上逐一測試, 失敗對應為 `TLS_FAILED`, `TIMEOUT` 等可判斷的錯誤碼; GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR 與 UTF-8 的主旨, 顯示名稱, 內文與檔名在宣告, 未宣告與誤宣告三種情形下逐一斷言.
- 裝置與生命週期: Android 7.0 (API 24) 模擬器, Sony (Android 9) 與 Redmi (Android 13) 實機; 指令碼正常結束, `exit()`, `engines.stopAll()`, 強制停止宿主或外掛程式, 原地升級, 停用與解除安裝外掛程式八種結束方式下連線, 繫結與執行緒均被回收; 熄屏進入 Doze 後監聽中斷並在裝置喚醒後恢復, 需要持續監聽時可在設定頁申請電池最佳化豁免.
- 效能基線: 本機 10000 封收件匣與 50 MiB 附件在 JVM, Redmi 與 Sony 上的列表, 搜尋, 下載, 寄送與一小時 IDLE 待機資料見 `docs/dev/p6-performance-baseline.md`; 郵件文件有界 (地址, 信頭, MIME 樹與內嵌內文均有上限), 敵意輸入不會撐爆工作階段.

******

### 常見問題

******

- **`AUTH_FAILED` 怎麼排查?** 先確認使用的是授權碼或應用程式專用密碼而不是登入密碼, 且已在網頁端開啟對應協定 (IMAP 與 POP3 是分別開啟的); 呼叫 `client.test()` 分別檢視收信端點與 SMTP 的結果與錯誤碼. 權杖帳號的 `AUTH_FAILED` 通常是權杖過期, 提供 `tokenProvider` 後外掛程式會重新整理並重試一次. 錯誤物件的 `code`, `details` 與 `retryable` 說明是否值得重試.
- **163 / 126 報 `Unsafe Login`?** 網易的 IMAP 伺服器拒絕未傳送 `ID` 命令的連線, 外掛程式對每個 IMAP 連線在登入後立即傳送 `ID`, 正常情況下不會遇到. 若仍出現, 請在網頁端重新開啟 IMAP 服務並重新產生授權碼.
- **中文搜尋沒有結果?** 各伺服器對非 ASCII 搜尋的處理不同: 新浪拒絕 (外掛程式自動回退到用戶端過濾), QQ 與 163 回答 0 命中而不報錯 (預設的 `fallback: 'client'` 不會觸發). 對這些帳號使用 `fallback: 'always'`, 並用 `since` 或 `limit` 縮小範圍; 用戶端的內文過濾要逐封抓取, 在大信箱上可能很慢.
- **`mail.connect` 成功了, 第一次 `fetch` 才報錯?** `connect` 只開啟外掛程式工作階段, 不連線郵件伺服器; 首個網路方法才登入 (SMTP 在首次寄信時). 想提前驗證帳號請呼叫 `client.test()`.
- **監聽在熄屏後停了?** Android 的 Doze 會凍結背景應用程式的網路, 監聽中斷, 新郵件在裝置喚醒後幾分鐘內補報 (Doze 結束時外掛程式立即重連). 需要持續監聽時, 在設定頁用引導按鈕為外掛程式申請電池最佳化豁免; 監聽只在指令碼執行期間有效, 指令碼結束即關閉.
- **如何連線 Outlook.com / Hotmail?** Microsoft 已為個人帳戶關閉基本驗證, 預設不接受密碼. 在外掛設定頁儲存帳戶並把驗證方式選為 "使用 Microsoft 帳號登入 (瀏覽器)": 瀏覽器開啟 Microsoft 登入頁, 只有授權碼返回外掛, 存取權杖由外掛自動續期; 之後指令碼按別名連線. 指令碼也仍可把在別處取得的存取權杖作為 `accessToken` 傳入並經 `tokenProvider` 重新整理. 若帳戶頁顯示 "需要重新登入", 說明續期被拒絕 (登入已被撤銷或過期): 開啟該帳戶的選單並選擇 "重新登入".
- **POP3 帳號能做什麼?** 只有 `INBOX`, `uid` 為 UIDL 字串; 列表, 讀取, 下載, 刪除與輪詢監聽可用; 標記, 移動, 複製, 附加, 清除與資料夾管理回傳 `UNSUPPORTED_OPERATION`; 搜尋在用戶端進行且只有信封條件可用.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 進入點受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外掛程式不匯出其他元件.
- INTERNET 權限用於與指令碼指定的伺服器建立 IMAP, POP3 與 SMTP 連線; 對經瀏覽器登入的帳戶, 另外只向 Google (`oauth2.googleapis.com`) 與 Microsoft (`login.microsoftonline.com`) 的權杖端點發起 HTTPS 請求: 交換授權碼, 續期即將過期的存取權杖, 以及應使用者要求撤銷 Google 權杖. 外掛不發起其他任何請求, 也不收集資料.
- 密碼與權杖從指令碼到外掛程式經 Binder 的專用欄位傳遞, 不會出現在記錄檔, JSON 文件, 錯誤訊息或當機報告中, 且只在工作階段生命週期內駐留記憶體. 設定頁儲存的帳號由 Android Keystore 金鑰加密, 並排除在備份之外. 瀏覽器登入的權杖以同樣方式儲存: 重新整理權杖不離開本機, 只有存取權杖交給郵件工作階段, AutoJs6 與指令碼都無法讀取; "撤銷登入" 會立即捨棄它們.
- 連線預設使用 TLS (按服務商要求選擇 SSL 或 STARTTLS); 明文連線與自簽憑證必須為每個帳號明確宣告.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 權限只服務於設定頁的引導按鈕: 按鈕顯示系統是否可能在背景暫停外掛程式, 並在使用者要求時開啟系統對話方塊; 外掛程式從不自行請求, 也沒有任何功能依賴該排除. P5 監聽矩陣實測了該排除的用途: 螢幕關閉一段時間後 (Doze) Android 會凍結背景應用程式的網路, 監聽斷開, 重連逾時, 新郵件要等裝置喚醒幾分鐘後才回報 (Android 9 上約四分鐘; Doze 結束時外掛程式立即重連); 排除後監聽保持連線.
- 四項權限只服務於 1.1.0 的背景守望. FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_SPECIAL_USE 執行守望服務 (類型 `specialUse`, 子類型 `mail_background_watch`: 郵件守望是等待伺服器推送的長連線, 而不是有界的資料同步), 只顯示一則低優先級通知; POST_NOTIFICATIONS 僅在守望頁面啟用守望時申請, 以便 Android 13 及以上顯示該通知; RECEIVE_BOOT_COMPLETED 支撐該頁面的開機自啟開關, 預設關閉, 開啟時才啟用接收器. 服務只從守望頁面或宿主訂閱啟動, 只連線已儲存帳戶, 秘密留在外掛程序內; 發往 AutoJs6 的喚醒廣播只攜帶郵件信封 (不含正文), 且只送達受 PLUGIN 簽章權限保護的接收器.

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

#### v1.2.1

_2026/09/22_

- `修復` 會話關閉時守望的 `closed` 事件原因固定為 `closed`: 此前會話的工作執行緒可能先以 `session-closed` 停掉部分守望 (API 24 模擬器的 connected 套件曾出現一次).
- `優化` 發布建置也攜帶維護者的 Google OAuth 2.0 用戶端 id (2026-09-22 註冊的 Android 用戶端), Gmail 預設提供 "使用 Google 登入 (瀏覽器)". Google 專案處於測試狀態: 只有專案的測試使用者能登入, 且其更新權杖 7 天後過期 (隨後顯示 "重新登入"); 其他 Google 帳戶會看到 Google 的拒絕存取頁面, 權杖與應用程式專用密碼路徑不變.

#### v1.2.0

_2026/09/22_

- `提示` 只有攜帶對應服務商 OAuth 2.0 用戶端 id 的建置才提供瀏覽器登入 (維護者的註冊資訊在建置時從 Git 忽略的 `oauth-clients.properties` 讀取); 沒有它們的建置保留權杖與應用程式專用密碼路徑, 並在驗證方式對話框中說明. Google 用戶端需先通過 Google Cloud 專案的敏感 scope 審核, 之後任意帳戶才能登入; 在此之前 Google 只允許專案的測試使用者.
- `新增` Google 與 Microsoft 帳號的瀏覽器登入 (郵件路線圖 P9): 帳戶編輯器為 Gmail 預設提供 "使用 Google 帳號登入 (瀏覽器)", 為 Outlook.com 與 Microsoft 365 預設提供 "使用 Microsoft 帳號登入 (瀏覽器)"; 登入在 Custom Tab (回退為任意瀏覽器) 中開啟服務商頁面, 攜帶 PKCE (`S256`) 與隨機 `state` 的 OAuth 2.0 授權碼請求, 重新導向 (`<applicationId>://oauth2/microsoft`, 或 Google 反轉用戶端 id 的 scheme) 落在 `OAuthRedirectActivity`, 由它交給等待中的登入頁面; 頁面拒絕任何 `state` 不匹配的重新導向, 經 HTTPS 在權杖端點交換授權碼 (`HttpsFormPoster`, 外掛唯一的 HTTP 用戶端), 並從 id 權杖預填地址
- `新增` 權杖儲存與續期: 瀏覽器登入的權杖是帳戶儲存中新種類 `OAUTH2` 的一條加密記錄 (從不作為 Binder 欄位, 從不出現在 `mail.accounts.list()`), 帳戶文件帶有 `oauth` 物件 (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`), `mail.accounts.list()` 會報告它; 該別名的每個工作階段 (指令碼, 連線測試, 背景守望) 都從 `AccountSecrets` 取存取權杖, 剩餘不足五分鐘時經重新整理權杖續期, 按帳戶串行; 續期被拒 (`invalid_grant`) 時記錄標記 `needsReauth`, 工作階段以 `AUTH_FAILED` ("sign in again") 失敗, 帳戶頁在該帳戶旁顯示 "需要重新登入"
- `新增` 帳戶頁操作 "重新登入" (新的瀏覽器登入存入同一記錄) 與 "撤銷登入" (記錄的權杖立即換成已撤銷標記, 請求 Google 撤銷重新整理權杖, 帳戶在重新登入前停止運作); `gmail`, `outlook` 與 `office365` 預設的 `authHint` 首先提及瀏覽器登入 (`providers.json` 版本 4); 11 語言各 35 條新字串; JVM 測試覆蓋 PKCE, 授權請求與重新導向解析, 基於指令碼化傳輸的權杖用戶端, 權杖文件, 服務商表, 建置的用戶端與重新導向 URI, `AccountSecrets` (續期, 拒絕標記, 已撤銷記錄, 擦除) 以及帳戶選項, 表單與帳戶文件中的 `oauth` 物件

#### v1.1.0

_2026/09/21_

- `提示` 1.1.0 新增背景守望 (郵件路線圖 P8): 設定頁的守望頁面在沒有指令碼執行時以前景服務保持已儲存帳戶的守望, 並喚醒 AutoJs6 的 "郵件到達時" 任務. 該任務及其守望選擇器需要攜帶郵件契約版本 2 的宿主組建 (AutoJs6 6.8.0 組建 5282 之後); 舊版宿主上頁面會提示無法喚醒 AutoJs6, 新郵件只進入守望的記錄清單. 此功能新增四項權限, 理由見 README 安全章節: FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_SPECIAL_USE (守望服務), POST_NOTIFICATIONS (其常駐通知, 僅在啟用守望時申請) 與 RECEIVE_BOOT_COMPLETED (守望頁面的開機自啟開關, 預設關閉).
- `新增` 背景守望 (郵件路線圖 P8): 設定頁新增守望頁面, 可為已儲存帳戶設定至多 16 個守望 (`MAX_TRIGGERS`), 每個含名稱, 帳戶別名, 資料夾, 模式 (自動, IDLE 或帶間隔的輪詢) 與可選的寄件者 / 主旨過濾, 存於 no-backup 目錄下的 `mail-triggers/triggers.json`; `specialUse` 前景服務 `MailWatchService` 在沒有指令碼執行時以 P5 的監聽器執行已啟用的守望 (伺服器推送時用 IDLE, 否則輪詢, 斷線按退避重連, 網路變化時立即重連) 並顯示一則低優先級通知; 每封新郵件進入守望的記錄清單 (最近 100 筆信封摘要, `MAX_TRIGGER_RECORDS`, 不含正文), 頁面顯示連線狀態, 最近錯誤, 記錄與重連操作; 開機自啟開關 (預設關閉) 啟用 `BOOT_COMPLETED` 接收器, 重新開機後重新拉起服務
- `新增` 郵件契約版本 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, 能力特性 `backgroundWatch`): 宿主以 generation 與可選過濾訂閱已設定的守望, 立即收到目前狀態, 之後收到 `onStatus` (stopped, connecting, connected 或 failed, 附原因與最近錯誤) 與每封符合郵件的 `onMail(generation, seq, event)`, `mail` 事件攜帶守望 id, 別名, 地址, 資料夾, 郵件信封與 `receivedAt`; 每個守望至多 4 個訂閱者 (`MAX_TRIGGER_SUBSCRIBERS`), 已停用或不存在的守望與不可用的選項以 reason 為 `refused` 的 `stopped` 狀態拒絕, `update` 取代訂閱者的過濾, `stop` 只結束訂閱; 沒有活動訂閱者時每個事件以明確廣播 `org.autojs.autojs6.action.MAIL_TRIGGER` 發往 AutoJs6 (受其 `PLUGIN` 簽章權限保護), 即使沒有指令碼執行也能啟動宿主的 "郵件到達時" 任務 (`MailTriggerBinderTest` 於 API 37 AVD; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `新增` 郵件核心新增頁面, 服務與 Binder 共用的觸發文件與規則 (`TriggerConfig`, 寄件者與主旨子字串不區分大小寫的 `TriggerFilter`, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) 與上限 `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS`, `MIN_TRIGGER_INTERVAL_MS` (3 s, 宿主每任務的節流間隔), 監聽器新增 `onConnected` 回呼, 背景守望在資料夾開啟後即顯示 `connected`

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
- 郵件模組文件: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
