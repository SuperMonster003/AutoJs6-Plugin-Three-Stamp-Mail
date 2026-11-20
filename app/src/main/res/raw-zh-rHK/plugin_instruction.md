3-Stamp Mail 為 AutoJs6 腳本提供全局物件 `mail`, 透過 IMAP, POP3 與 SMTP 發送郵件, 搜尋和讀取郵件, 下載附件, 管理標記與資料夾, 以及監聽新郵件. 帳戶可以儲存在插件內, 腳本按別名連線.

### 開始使用

1. 在服務商的網頁設定中開啟 IMAP 或 POP3 與 SMTP, 並取得授權碼 (QQ, 163, 126, 新浪) 或應用程式專用密碼 (Gmail, iCloud, Yahoo); 登入密碼本身通常不被接受.
2. 從啟動器開啟 3-Stamp Mail, 在 "已儲存帳戶" 頁點按 "新增帳戶", 選擇 "服務商", 填寫郵箱地址, "別名" 與授權碼, 然後儲存.
3. 點按帳戶右側的選單按鈕, 選擇 "測試連線", 確認伺服器可以連通.
4. 在 AutoJs6 腳本中用 `mail.connect('別名')` 連線帳戶, 用法見下文.

### 主要功能

- **收發與管理**: 發送帶附件的郵件, 按主題, 日期等條件搜尋郵件, 讀取正文, 下載附件, 標記已讀, 管理資料夾.
- **瀏覽器登入**: Gmail, Outlook.com 與 Microsoft 365 帳戶可以把 "認證方式" 設為 "使用 Google 帳號登入 (瀏覽器)" 或 "使用 Microsoft 帳號登入 (瀏覽器)", 在瀏覽器中登入後儲存, 無需授權碼.
- **背景守望**: 在 "設定" > "背景守望" 中點按 "新增守望" 並選擇帳戶與資料夾, 插件會在沒有腳本運行時保持連線; 再在 AutoJs6 中長按腳本, 選擇 "定時任務", 在 "廣播觸發任務" 中選擇 "郵件到達時 (郵件插件後台守望)", 之後每封新郵件到達時都會運行該腳本.
- **帳戶安全**: 密碼與權杖加密儲存在插件內, 腳本只按別名連線, 讀不到它們.

### 在腳本中使用

在 AutoJs6 腳本中直接使用全局物件 `mail`, 無需匯入. 下例以別名連線已儲存的帳戶, 發送郵件, 讀取未讀郵件並監聽收件匣:

```js
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    console.log(m.subject);
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => console.log(m.subject, m.text));
```

- 也可以不儲存帳戶, 直接傳入帳戶資料, 例如 `mail.connect({ provider: 'qq', address: 'me@qq.com', password: '授權碼' })`. 每個連網方法都有返回 Promise 的 Async 版本.
- Node.js 腳本用 `require('autojs6:mail')` 載入, Bun 腳本透過 `host.mail` 使用並需另行授權 `mail.control`, 呼叫均返回 Promise.
- 由背景守望運行的腳本透過 `engines.myEngine().execArgv.mail` 讀取到達的郵件.

### 運行要求

- Android 7.0 或更高版本, AutoJs6 6.8.0 或更高版本, 且本插件在 "插件中心" 中處於啟用狀態.
- 郵件到達時運行腳本需要支援背景守望的 AutoJs6 版本; 不支援時 "背景守望" 頁會給出提示.
- 網絡權限只用於連線你設定的郵件伺服器, 以及瀏覽器登入時向 Google 或 Microsoft 換取和續期權杖.
- 獨立簽署的打包應用程式只能使用腳本自行傳入的帳戶資料; 已儲存帳戶與背景守望只對 AutoJs6 開放.

### 常見問題

**連線時提示認證失敗**

多數服務商不接受登入密碼. 確認已在服務商的網頁設定中開啟 IMAP 或 POP3 與 SMTP, 改用授權碼或應用程式專用密碼, 再用 "測試連線" 檢查.

**熄屏後監聽或守望中斷**

Android 會在熄屏一段時間後凍結背景應用程式的網絡. 在 "設定" > "電池優化" 中為插件申請排除即可保持連線; 這一步是可選的.

**腳本提示缺少插件時的處理**

確認本插件已安裝, 並在 AutoJs6 的 "插件中心" 中處於啟用狀態, 然後重新運行腳本, 無需重新啟動 AutoJs6.

全部方法與選項見 [AutoJs6 文檔 > 郵件](https://docs.autojs6.com/#/mail), 各服務商的設定方法與其他資訊見 [項目主頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail).
