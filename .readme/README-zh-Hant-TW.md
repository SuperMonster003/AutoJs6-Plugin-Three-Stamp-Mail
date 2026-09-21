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

版本 1.0.1 是首個正式版本: 路線圖 P0 至 P6 的全部條目 (郵件核心, Binder 契約, 指令碼 API, 設定頁與別名帳號, 新郵件監聽, 以及 TLS, 字元集, 服務商, 生命週期, 敵意輸入, 秘密稽核與效能矩陣) 均已完成並附有證據, 見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本; 指令碼 API 的完整參考見 [AutoJs6 文件](https://docs.autojs6.com/#/mail).

******

### 功能

******

外掛程式提供以下能力:

- 寄信: 純文字或 HTML, 多個收件者, 附件與內嵌圖片, 自訂信頭與優先順序; 服務商不自動儲存寄件備份時由外掛程式寫入伺服器.
- 收信: 按頁列出資料夾, 在伺服器端搜尋 (服務商拒絕非 ASCII 搜尋時回退到用戶端過濾), 讀取文字與 HTML 內文, 並把附件直接下載到指令碼工作目錄.
- 整理: 標記已讀或星號, 移動, 複製, 刪除, 清除, 以及建立, 重新命名或刪除資料夾; POP3 帳號獲得唯讀子集.
- 監聽: 在指令碼執行期間接收新郵件事件, 伺服器真正推送時用 IMAP IDLE, 否則輪詢 (預設 60 s, 可調): QQ 與 Sina 接受 IDLE 但不推送, 163 與 126 沒有 IDLE, POP3 帳號一律輪詢; 斷網與外掛程序重啟後監聽自動恢復.
- 服務商: 內建 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 預設, 自動填入主機, 連接埠與加密方式; 任何欄位都可為其他伺服器覆寫.
- 驗證: 密碼與服務商授權碼, 或由指令碼提供並附帶重新整理回呼的 XOAUTH2 存取權杖.

******

### 使用方式

******

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝外掛程式 APK.
2. 開啟 AutoJs6 外掛程式中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳號: 在郵件服務商的網頁端開啟 IMAP 或 POP3 與 SMTP, 並取得授權碼 (QQ, 163, 126, Sina), 應用程式專用密碼 (Gmail, iCloud, Yahoo) 或 OAuth 2.0 存取權杖 (Outlook.com); 登入密碼本身通常不被接受.
4. 在指令碼中呼叫 `mail.connect(...)`, 或在外掛程式設定頁 (外掛程式的啟動器圖示, 或 AutoJs6 開發者選項 > 郵件帳戶設定) 儲存帳號後以別名連線.

******

### 服務商準備

******

每家服務商都要先在網頁端開啟 IMAP (或 POP3) 與 SMTP, 並以授權碼, 應用程式專用密碼或存取權杖代替登入密碼; 各預設 (`provider` 的取值) 的要點:

- QQ 信箱 (`qq`): 在網頁版的帳號設定中開啟 IMAP/SMTP 服務並產生授權碼, 以授權碼作為 `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net 使用 `163` 預設並覆寫主機): 在網頁版設定的 POP3/SMTP/IMAP 頁開啟服務並產生授權碼; POP3 需要單獨開啟, 否則 IMAP 與 SMTP 接受的授權碼會被 POP3 拒絕. 伺服器要求每個 IMAP 連線先傳送 `ID` 命令 (否則回答 `Unsafe Login`), 外掛程式自動完成.
- 新浪信箱 (`sina`): 在網頁版的用戶端設定中開啟 IMAP/SMTP 服務並使用授權碼. 伺服器不儲存已寄郵件副本 (外掛程式附加到 `已发送`), 不允許經 IMAP 新建資料夾, 文字搜尋由外掛程式在用戶端完成.
- Gmail (`gmail`): 開啟兩步驟驗證後在 Google 帳戶中產生應用程式專用密碼作為 `password`, 或提供帶 `https://mail.google.com/` 範圍的 OAuth 2.0 存取權杖 (`accessToken` 與 `tokenProvider`); 資料夾位於 `[Gmail]` 命名空間, 新郵件由 IDLE 推送. 專案以權杖完成了真實帳號核實.
- Outlook.com / Hotmail (`outlook`) 與 Microsoft 365 (`office365`): 微軟已關閉個人帳戶的基本驗證, 應用程式密碼在 IMAP, POP3 與 SMTP 上都會被拒絕, `outlook` 預設因此只接受 OAuth 2.0 存取權杖 (`accessToken` 與 `tokenProvider`); 工作或學校帳戶 (`office365`) 可用密碼或權杖, 但租用戶原則可能停用 IMAP, POP3 或 SMTP AUTH. 權杖需要 `https://outlook.office.com/` 的委派權限 `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` 與 `SMTP.Send` (專案已用這樣的權杖核實一個個人帳號: IMAP, POP3, SMTP 與 IDLE 推送); 部分較新的個人信箱被微軟停用了 SMTP AUTH (`535 5.7.139`), 使用者設定中沒有開關.
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
- **Outlook.com / Hotmail 怎麼接入?** 微軟已關閉個人帳戶的基本驗證, 需要透過 OAuth 2.0 授權流程 (需要一個已註冊的應用程式) 取得帶 IMAP, POP 與 SMTP 權限範圍的存取權杖, 以 `accessToken` 傳入並用 `tokenProvider` 重新整理; 預設不接受密碼.
- **POP3 帳號能做什麼?** 只有 `INBOX`, `uid` 為 UIDL 字串; 列表, 讀取, 下載, 刪除與輪詢監聽可用; 標記, 移動, 複製, 附加, 清除與資料夾管理回傳 `UNSUPPORTED_OPERATION`; 搜尋在用戶端進行且只有信封條件可用.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 進入點受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠存取; 外掛程式不匯出其他元件.
- INTERNET 權限只用於指令碼指定伺服器的 IMAP, POP3 和 SMTP 連線; 外掛程式不發起其他請求, 也不收集任何資料.
- 密碼與權杖從指令碼到外掛程式經 Binder 的專用欄位傳遞, 不會出現在記錄檔, JSON 文件, 錯誤訊息或當機報告中, 且只在工作階段生命週期內駐留記憶體. 設定頁儲存的帳號由 Android Keystore 金鑰加密, 並排除在備份之外.
- 連線預設使用 TLS (按服務商要求選擇 SSL 或 STARTTLS); 明文連線與自簽憑證必須為每個帳號明確宣告.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 權限只服務於設定頁的引導按鈕: 按鈕顯示系統是否可能在背景暫停外掛程式, 並在使用者要求時開啟系統對話方塊; 外掛程式從不自行請求, 也沒有任何功能依賴該排除. P5 監聽矩陣實測了該排除的用途: 螢幕關閉一段時間後 (Doze) Android 會凍結背景應用程式的網路, 監聽斷開, 重連逾時, 新郵件要等裝置喚醒幾分鐘後才回報 (Android 9 上約四分鐘; Doze 結束時外掛程式立即重連); 排除後監聽保持連線.

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

#### v1.0.1

_2026/09/21_

- `修復` Outlook.com 的 POP3 以 OAuth 2.0 權杖登入 (郵件路線圖 P6 服務商矩陣, 2026-09-21): 伺服器對 Angus Mail 預設傳送的單行 `AUTH XOAUTH2 <base64>` 答 `-ERR Protocol error. Connection is closed.` 並中斷連線, `outlook` 預設的 POP3 帳號因此以 `AUTH_FAILED` 失敗; 服務商預設新增 `pop3Xoauth2TwoLine` (目錄版本 3, `outlook` 與 `office365` 為 true), 郵件核心對這兩個預設以及未用預設而填寫的微軟 POP3 主機改為先傳送裸命令, 收到伺服器的 `+` 續行後再傳送 base64 回應 (`Pop3OAuthScriptedTest` 5 例; 已在真實帳號上經 JVM 與 API 33 真機核實)
- `優化` 服務商矩陣的 Outlook.com 欄 (郵件路線圖 P6) 與 P5 裝置列, 以維護者 Entra 公用用戶端的權杖在一個個人帳號上跑通: 工作階段測試, 資料夾 (角色來自常規名稱, 無 SPECIAL-USE), 傳送 (`sentCopy = server`, Message-ID 被伺服器改寫), 列表 (約 7 s 可見), 伺服器搜尋 (各鍵均命中; 中文主旨能正確命中但伺服器在 2300 封的收件匣上要花數分鐘), 內文, 附件, 標記 (自訂關鍵字不儲存), 建立資料夾, `MOVE`, 監聽 (IDLE 在提交後約 10 s 內推送, 七家中最快), POP3 (UIDL 5 字元, 剛傳送的郵件在檢視中) 與清理 (已傳送副本可按 UID 定址) 全部通過; Redmi (API 33) 上 baseline / 殺外掛 / 關 Wi-Fi 三個監聽情境 9 到 14 s 到達, 事件順序與 Gmail 一致; 預設 notes, README 與證據檔案記錄了差異, 包括微軟對較新個人信箱給出的 `535 5.7.139 SmtpClientAuthentication is disabled for the Mailbox` 拒絕

#### v1.0.0

_2026/09/19_

- `提示` 首個正式版本: 郵件核心, Binder 契約, 指令碼 API `mail`, 設定頁與別名帳號, 新郵件監聽, 以及 TLS, 字元集, 服務商, 生命週期, 敵意輸入, 秘密稽核與效能矩陣均已完成並附有證據 (ROADMAP.md P0 至 P6). 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本.
- `新增` 外掛程式標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答外掛程式資訊, 能力, 服務商與已儲存帳戶列表以及工作階段信封 (具體操作隨 P2 落地)
- `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 驗證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
- `新增` 郵件核心帳戶層 (路線圖 P2.1): 帳戶選項支援 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 與 Aliyun 的服務商預設, 按協定區分的逾時, `tls.trustAll`, IMAP `ID` 命令與脫敏的 `debug` 協定摘要; 工作階段延遲連線, 閒置時中斷並在斷線後重連; `session.test` 經 Binder 回傳各端點的能力與往返耗時
- `新增` 寄信 (路線圖 P2.2): `mail.send` 支援 to / cc / bcc / replyTo, 純文字與 HTML 內文 (`multipart/alternative`), 經宿主傳入的檔案描述符讀取的附件與內嵌圖片 (`multipart/mixed` / `multipart/related`), 自訂標頭, 優先順序, `inReplyTo` / `references` 與日期; 收件者, 附件與標頭的上限, 拒絕標頭注入; `saveToSent` 僅在服務商不自動儲存時經 IMAP 附加副本; `messages.append` 寫入草稿並回傳 UID; 已在實機上對 QQ 郵箱與 Gmail 驗證
- `新增` 收信 (路線圖 P2.3): `folders.list` (樹狀, special-use 角色取自 LIST / XLIST 屬性或約定名稱, 可選計數), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 為游標 (`before` / `after`), 支援 `order` 與 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查詢 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 旗標 / 大小 / `header` / `messageId` / `uid` 與 `and` / `or` / `not`) 編譯為 IMAP SEARCH, 伺服器拒絕時在用戶端過濾 (`fallback`); `messages.get` 回傳純文字與 HTML 內文 (只有 HTML 的郵件衍生文字), 全部標頭, 帶 `partId` 的附件清單, 內嵌預算 (`bodyTruncated` / `bodyParts`) 與 `includeRaw`; `attachments.download` 與 `messages.raw` 串流寫入宿主描述符並回報進度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字元集復原涵蓋 GBK / GB 18030 / ISO-2022-JP, 未宣告與未知字元集, 原始 8 位元標頭與 RFC 2231 / 2047 檔名, 檔名淨化; IMAP `ID` 命令在每條連線上發送 (163 / 126 拒絕未識別的連線), 伺服器不能解析 `UID EXPUNGE` 時改用整個資料夾的 `EXPUNGE`; 已在實機上對 QQ 郵箱, 163 郵箱與 Gmail 驗證
- `新增` POP3 (路線圖 P2.4): `receive: "pop3"` 的帳戶透過同一組操作讀取唯一的 `INBOX`, `uid` 為 UIDL 字串: `folders.list` 只回傳 `INBOX` (可帶計數), `messages.list` 依 UIDL 游標分頁且只取信頭 (`TOP`), `messages.search` 在用戶端依信頭由新到舊過濾, 湊足 `limit` 筆即停 (最多 200 個候選, 每個一次 `TOP` 往返), `messages.get` / `messages.raw` / `attachments.download` 下載整封郵件, `messages.delete` 發 `DELE` 並在信箱關閉時生效; 旗標, 移動, 複製, expunge, 附加, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 與內文搜尋在連線之前即回傳 `UNSUPPORTED_OPERATION`; 已在實機上對 QQ 郵箱驗證
- `新增` Binder 工作階段控制 (路線圖 P2.5): 只有已安裝且與本外掛程式同簽章的 AutoJs6 宿主能開啟工作階段或列出已儲存帳戶 (否則 `SecurityException`, 規則與 MCP Server 外掛程式一致); 請求與回應信封不超過 `MAX_ENVELOPE_BYTES`, 錯誤訊息不超過 `MAX_ERROR_MESSAGE_BYTES`; 每個工作階段依序執行呼叫, 執行中的呼叫之後最多排隊 `MAX_QUEUED_CALLS` 個, 再多則 `LIMIT_EXCEEDED`; `cancel` 立即回應排隊中的呼叫, 並透過關閉通訊端中斷執行中的呼叫, 伺服器無回應時不再等到讀取逾時; `close` 讓全部待處理呼叫回應 `SESSION_CLOSED`; `getStatus` 回報 `queued` 與 `active`; 操作表標明每個操作支援的收信協定, POP3 帳戶在解析參數前即被拒絕; 能力集宣告 `append` 與 `clientSearchFallback`
- `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌
- `新增` 已儲存帳戶儲存 (路線圖 P4.1): 在外掛程式內儲存的帳戶把非秘密的帳戶文件與經 Android Keystore 主金鑰 AES-256-GCM 加密的密碼或存取權杖存在一起; 驗證資料綁定別名, 秘密類型與帳戶文件, 在磁碟上被改動或移動的記錄不再能解密; 記錄存於 `noBackupFilesDir` (本已排除在備份之外), 在檔案鎖下原子寫入, 秘密只經過用後即清零的 `CharArray` / `ByteArray` 緩衝區; 別名去除首尾空白, 經 NFC 正規化且不區分大小寫
- `新增` 已儲存帳戶工作階段 (路線圖 P4.3): `openSession` 接受別名形態 (`accountAlias`) 並在外掛程式處理程序內解密秘密, `mail.connect('alias')` 因此不經 Binder 傳遞任何憑證; `listSavedAccounts` 回傳每個已儲存帳戶的別名, 地址, 使用者名稱, 服務商, 驗證方式, 收信協定, 端點與預設標記, 不含任何秘密; 能力集合新增 `savedAccounts`
- `新增` 設定頁 (路線圖 P4.2): 啟動器圖示開啟帳戶頁, 列出每個已儲存帳戶的地址, 服務商, 收信協定與驗證方式, 提供編輯, 測試連線, 設為或取消預設以及刪除; 帳戶編輯頁可按服務商預設自動填入, 也可填寫自訂 IMAP / POP3 / SMTP 伺服器及其加密方式與連接埠, 密碼或存取權杖直接從輸入欄讀入用後即清零的 `CharArray`, 編輯時留空則沿用已儲存的秘密, 儲存前可對所填伺服器執行 `session.test` 並按協定顯示結果與耗時而不寫入磁碟; 頁面跟隨 AutoJs6 宿主的主題, 夜間模式與語言, 編輯頁重建後還原除秘密外的全部欄位
- `新增` 設定入口 (路線圖 P4.3): AutoJs6 宿主經匯出的 `org.autojs.plugin.MAIL_SETTINGS` 活動開啟帳戶頁, 該活動要求外掛程式權限, 只接受無參數的請求並立即結束; 能力集合宣告 `mailSettingsVersion` 1; 啟動器入口本身不帶該權限
- `新增` 發行歷史 (路線圖 P4.5): 設定頁與關於頁可開啟發行歷史頁, 內容取自隨外掛程式打包的目前語言更新記錄 (無對應翻譯時回退英語), 每個版本一張卡片, 含日期與帶標籤的條目; 外掛程式不做自身的更新檢查, 更新跟隨 AutoJs6 外掛程式中心
- `新增` 電池最佳化引導 (路線圖 P4.6): 設定頁顯示系統是否可能在背景暫停本外掛程式 (`PowerManager.isIgnoringBatteryOptimizations`), 說明影響後經 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 開啟系統對話方塊; 資訊清單因此宣告 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; 啟動時不發起任何請求, 也沒有功能依賴該排除
- `新增` 新郵件監聽, IMAP IDLE (路線圖 P5): 郵件核心以獨立連線用 `IMAPFolder.idle` 監聽資料夾, 每 24 分鐘續期一次 IDLE, 新郵件按 UID 抓取 (信封, 或按需內文) 且只回報一次, 斷線後按指數退避重連 (1 s 起, 上限 5 min, 帶抖動), 資料夾 UIDVALIDITY 變更時回報 `resync`; 伺服器無 IDLE 或 IDLE 連續失敗 3 次則切換為輪詢並發出 `mode` 事件; 每個工作階段最多 `MAX_WATCHES_PER_SESSION` 個監聽, 工作階段關閉時一併關閉
- `新增` 新郵件監聽, 輪詢 (路線圖 P5): `mode: "poll"` 的監聽每 `pollIntervalMs` (預設 60 s, 下限 `MIN_POLL_INTERVAL_MS`, 上限 1 小時) 按 UID 對資料夾做差分, 連線在輪次之間保持; POP3 帳戶一律輪詢, 按 UIDL 差分且每輪登入一次, 輪次之間不鎖定信箱, 只回報新增, 不回報刪除; 首輪只取快照, 不回報積壓
- `新增` 經 Binder 的新郵件監聽 (路線圖 P5): `IMailSession.watch` 在郵件核心的監聽器上開啟監聽並立即回應 (工作階段已關閉, 達到 `MAX_WATCHES_PER_SESSION`, 參數不可用或宿主回呼已死亡時回傳 null, 原因記入工作階段狀態); 事件由投遞執行緒帶宿主的 `generation` 與自 1 起計數的 `seq` 送達宿主的 `oneway` 回呼, 宿主停止消費時 `MAX_WATCH_QUEUE` 條事件的佇列摺疊為一條 `resync`, 超過 `MAX_ENVELOPE_BYTES` 的事件去掉內文或退化為 `resync` 後送出, `stop`, 工作階段關閉與宿主死亡均以單條 `closed` 事件結束監聽, 預設網路變更或遺失時執行中的監聽立即重連, 能力集現在宣告 `idle`
- `修復` 服務商預設 (路線圖 P3.2): 163 信箱與 126 信箱會在伺服器端保存每封經 SMTP 發出的郵件, 兩者的 `autoSavesSent` 改為 true, 預設 `saveToSent` 不再向 `已发送` 追加第二份副本 (真實 163 帳戶核實: `saveToSent: false` 發出的郵件數分鐘後出現在已發送資料夾)
- `修復` AGP 9.1 建置時的 SDK XML v4 解析警告及 JVM 單元測試組裝工作誤觸發 APK 原生程式庫對齊檢查的問題 (共用建置外掛 1.8.3)
- `修復` 帳戶編輯器 (路線圖 P4.7): 整個表單退出 Android 自動填入框架, 密碼管理器不再索取授權碼; 此前 HyperOS (API 35) 會在儲存後關閉編輯器時彈出 "自動儲存帳號密碼".
- `修復` QQ, Sina, 163 與 126 的新郵件監聽 (郵件路線圖 P5 裝置矩陣): 服務商預設新增 `idlePush` (預設表版本 2), 這四家上 `mode: auto` 從一開始就輪詢而不再進入 IDLE, 因為 QQ 與 Sina 接受 IMAP IDLE 卻在用戶端閒置期間從不推送 (真實帳號, 2026-09-19: 10 分鐘內沒有任何未標記回應; Sina 還會在 60 s 後斷開連線), 而 163 與 126 根本沒有 IDLE; 明確的 `mode: 'idle'` 仍會進入 IDLE. 矩陣本身 (QQ 在 API 24 模擬器與兩台 Sony 手機, 163 在一台 Redmi: 終止外掛程序, 斷網, Wi-Fi 切行動網路, 強制 Doze) 記錄於 `docs/dev/p5-watch-evidence.md`, 搭配冒煙指令碼 `docs/smoke/watch.js` 與驅動 `.python/run_watch_matrix.py`; 同一矩陣還表明 Doze 會凍結背景應用程式的網路 (監聽的重連逾時, Android 9 上新郵件在喚醒後約四分鐘才回報), 因此外掛程式現在在裝置離開 Doze 的瞬間重連其監聽, 設定頁的電池最佳化引導文案說明了該排除的用途
- `修復` TLS 矩陣 (郵件路線圖 P6): 隱式 SSL, STARTTLS (經 GreenMail 前置的 STARTTLS 代理), 明文, 帶 / 不帶 `tls.trustAll` 的自簽憑證, 主機名稱不符的受信憑證, 連接埠模式錯配以及不提供升級的連接埠, 現已對 IMAP / POP3 / SMTP 逐一測試 (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`); 測試發現 Angus 的 POP3 存放區把缺少的 STLS 升級與交握前逾時回報為驗證失敗, 錯誤對應器現改為 `TLS_FAILED` 與 `TIMEOUT` 而非 `AUTH_FAILED`; `TlsDeviceTest` 在 API 24 / 28 / 33 上確認郵件核心以平台預設設定即可連線僅 TLS 1.2 的伺服器, 並自 API 29 起協商 TLS 1.3
- `修復` 字元集矩陣 (郵件路線圖 P6): GB18030 / GBK / GB2312 / Big5 / ISO-2022-JP / EUC-KR / UTF-8 在主旨, 顯示名稱, 內文與兩種檔名形態中按宣告, 未宣告與誤宣告三種情形逐一斷言 (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`), 並在 API 24 / 28 / 33 上核對 (`CharsetDeviceTest`); 本輪修正兩處解碼缺口: 宣告為 `us-ascii` / ISO-8859-1 卻攜帶 UTF-8 或 GB 18030 位元組的內文或標頭此前解出 Latin-1 亂碼 (Jakarta 把 `us-ascii` 對應為永不失敗的 ISO-8859-1), 現改走猜測鏈; 無編碼字的原始 ISO-2022-JP 主旨與內文現按其跳脫序列識別; Big5 與 EUC-KR 仍需宣告 (其位元組對在 GB 18030 中同樣合法)
- `修復` 服務商相容矩陣 (郵件路線圖 P6): QQ, 163, 126, yeah.net 與 Sina 以真實帳戶跑完寄信, 列表, 伺服器與用戶端中文搜尋, 內文, 附件位元組, 標記, 建立資料夾, 移動, 監聽與 POP3 (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`), 差異記入預設 notes: QQ 對中文 SEARCH 答 OK 零命中 (請用 `fallback: 'always'`), 改寫 Message-ID 與 ENVELOPE 的 To 顯示名稱, 拒絕 CREATE, 且其自存的寄件備份一段時間內無法按 UID 定址; 163 對近期郵件的文字搜尋零命中; Sina 只接受 ALL, SINCE 與旗標搜尋鍵; 五家均不儲存自訂關鍵字. 本輪同時修正一處用戶端缺陷: POP3 XOAUTH2 登入被以 SASL continuation 拒絕時 (Gmail) 因 Angus Mail 忽略拒絕而回報為可重試的 `IO_FAILED`, 現由郵件核心核實登入並回傳 `AUTH_FAILED` (`Pop3OAuthScriptedTest`). Gmail (權杖過期), Outlook.com 與 iCloud (無帳戶) 仍為未核實
- `修復` 生命週期矩陣 (郵件路線圖 P6): 在 Redmi (API 33) 上以八種方式結束一個持有工作階段與 watch 的指令碼: 正常結束, 開著 watch 與用戶端 `exit()`, `engines.stopAll()`, 強制停止宿主, 強制停止外掛, 原地升級外掛, 停用外掛, 解除安裝外掛; 每種情況下外掛的連線, 繫結與檔案描述元都在 30 s 內回到指令碼前的狀態, 宿主從不直接連線伺服器 (`docs/dev/p6-lifecycle-matrix.md`, `.python/run_lifecycle_matrix.py`, `docs/smoke/lifecycle.js`). 矩陣發現並修復一處洩漏: Angus Mail 為每個通訊端的寫入逾時自建執行緒池, 只在 TLS 通訊端經該包裝層關閉時才關掉它, 而底層明文通訊端已被 cancel 或 watch 停止關閉後 Android 會跳過這一步, 於是每條這樣的連線在行程存續期間留下一條執行緒; 郵件核心現在把一個共用的守護計時器交給 Angus (`WriteTimeouts`, `WriteTimeoutExecutorTest`)
- `修復` 服務商矩陣的 Gmail 與 Outlook.com 欄 (郵件路線圖 P6, 維護者更新 Gmail 權杖並提供三個 Outlook.com / Hotmail 帳戶後回填): Gmail 以權杖通過工作階段測試, 資料夾, 寄信, 列表, 伺服器搜尋, 內文, 附件, 標記與關鍵字, 建立資料夾, 移動, 監看與 POP3, 其 IDLE 確實推送 (新郵件在投遞後約 30 s 回報, 即 Gmail 的通知節奏), P5 裝置矩陣 (基線, 終止外掛, 關閉 Wi-Fi) 在 Redmi (API 33) 上以 IDLE 路徑重跑 (`docs/dev/p6-provider-matrix.md`, `docs/dev/p5-watch-evidence.md`). 兩個發現改動了郵件核心: Gmail 宣告 `UTF8=ACCEPT`, Angus Mail 啟用後 Gmail 反而拒絕所有含非 ASCII 文字的搜尋 (`BAD Could not parse command`), 中文主旨搜尋因此退回用戶端; 現在不再啟用該擴充, 搜尋如同在其他伺服器上一樣帶著 `CHARSET UTF-8` (`Utf8SearchTest`). 用戶端過濾現在每 25 個候選向偵錯軌跡回報一次進度, 因為 `fallback: 'always'` 的內文搜尋在命中不足 `limit` 時會掃完 2000 個候選的整個視窗, 每個候選兩到三次往返, 在本網路下對 Gmail 約 43 分鐘. Outlook.com: 三個帳戶對應用程式密碼在 IMAP 與 POP3 上都回 `Basic authentication is disabled`, SMTP 回 `535`, 預設的僅 XOAUTH2 規則成立, 操作列等待權杖 (路線圖 P9); 繞過預設後伺服器自身的拒絕 (`LOGINDISABLED`, 僅 `AUTH=XOAUTH2`) 回報為 `AUTH_MECHANISM_UNSUPPORTED` 而非 `SERVER_ERROR` (`LoginDisabledTest`)
- `優化` 錯誤對應: POP3 伺服器在登入後因帳戶未開啟 POP 存取而拒絕信箱 (Gmail 對 STAT 回應 `[SYS/PERM] Your account is not enabled for POP access`) 時, 現在得到說明原因的 `UNSUPPORTED_OPERATION`, 而不是可重試的 `IO_FAILED` "I/O failed"
- `優化` 服務商預設: Sina 信箱補上已傳送資料夾名稱 (`已发送`), 並註明伺服器不保存已傳送郵件副本且拒絕 IMAP CREATE (資料夾只能在網頁端建立); 126 信箱伺服器保存已傳送郵件副本已用真實帳戶驗證
- `優化` 服務商預設: Yahoo Mail 與 Aliyun Mail 的說明註明這兩個預設未經真實帳號核實 (專案無法取得測試帳號), 其已傳送副本行為按公開文件推定.
- `優化` 秘密稽核 (郵件路線圖 P6): 對郵件核心與應用程式搜尋記錄陳述式, 主控台輸出, Jakarta 偵錯開關以及每一處秘密被具體化的位置; 結果 (`docs/dev/p6-secret-audit.md`) 由 `SecretAuditTest` 強制執行: 任何記錄或偵錯陳述式都會使建置失敗, `reveal()` 被固定在三處 Jakarta 驗證呼叫, 並檢查 Jakarta 工作階段永不偵錯, 帳號 JSON 中的秘密被拒絕且不回顯, 值物件, 例外對應器與協定摘要在秘密傳播的每種形式下都將其遮蔽
- `優化` 敵意輸入 (郵件路線圖 P6): 無論郵件攜帶什麼, 郵件文件現在都有界 (四個位址清單合計 500 項, 位址與顯示名稱截至 320 字元, 主旨 / 識別碼 / 標頭值截至 4096 字元, `headers` 對應至多 64 KiB, MIME 樹深度至多 32 且部件至多 256, 未知大小的內文按內嵌預算截讀), 單封敵意郵件不再能以 `LIMIT_EXCEEDED` 封死整頁清單; 損壞的 base64, 未知傳輸編碼, 無 boundary 或空的 multipart 改為寬鬆解碼而不再被誤判為斷線; 過深或無法解析的 multipart 作為一個葉子保持可下載; `HostileInputTest` 與 `HostileInputGreenMailTest` (17 例, `docs/dev/p6-hostile-input.md`) 涵蓋超深 / 超寬 MIME, 20000 收件者, 標頭炸彈, 缺少 Content-Type, 非法 base64, 遞迴 `message/rfc822`, 敵意檔名, 大小不符與非法 UTF-8
- `優化` 效能基準 (郵件路線圖 P6): 以本機種子伺服器 (10000 封收件匣, 一封 50 MiB 附件) 在 JVM, Redmi (API 33) 與 Sony (API 28) 上跑列表, 搜尋, 下載, 傳送與一小時 IDLE 待機, 資料記入 `docs/dev/p6-performance-baseline.md`. 由此變更兩處: IMAP 分段擷取由 64 KiB 提高到 1 MiB, 50 MiB 附件從約 1100 次往返降到 69 次 (回環 10 MiB/s, 下載常駐記憶體仍約 2 MiB); 用戶端搜尋在命中 `limit` 筆後即停止而不再掃遍全部候選. 待機顯示外掛約 32 MiB PSS, 每小時 3.5 s CPU 與 40 KB 流量, 且長時監聽需要宿主的前景服務 (否則 Android 9 在 31 分鐘時把宿主與外掛當作快取空行程終止)
- `優化` 發布體積 (郵件路線圖 P6): R8 規則不再按原名保留整個 `jakarta.mail`, `jakarta.activation` 與 Angus Mail 命名空間, 只保留程式庫按名載入的 20 個類別 (IMAP / POP3 / SMTP 的 store 與 transport, 其 Provider, 串流提供者, activation 登錄表與五個資料內容處理器), 每條規則的依據資源記入 `app/proguard-rules.pro` 與 `docs/dev/p6-size.md`. release universal APK 由 2,254,035 位元組降至 2,153,957 位元組 (下載體積 1,507,931 -> 1,407,295), 去掉 2,319 個 DEX 方法與 162 個程式庫類別 (`MailHandler` 記錄元件, GraalVM 特性, 圖片處理器, SASL 用戶端); 儀器測試套件在實機上對裁剪後的組建通過 (27 例)
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
- 郵件模組文件: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
