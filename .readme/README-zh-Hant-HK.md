<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>讓 AutoJs6 腳本透過 IMAP, POP3 和 SMTP 收發, 搜尋和監聽郵件</p>

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
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
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

Angus Mail 為 AutoJs6 腳本提供全域物件 `mail`, 用於傳送郵件, 列出和搜尋郵箱, 讀取內文, 下載附件, 管理標記與資料夾, 以及監聽資料夾中的新郵件. 它基於 Jakarta Mail 的參考實作 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 透過 TLS 使用 IMAP, POP3 和 SMTP 協定.

全部郵件流量都在插件程序內完成. AutoJs6 透過 Binder 服務發現插件, 交出腳本提供的帳戶 (或插件設定頁中儲存的別名), 並接收 JSON 結果與附件串流; 宿主本身不含任何郵件程式碼. 除非你選擇在插件中儲存帳戶, 憑證只在工作階段生命週期內駐留記憶體.

******

### 目前狀態

******

版本 1.0.0 處於開發階段: 倉庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 插件中心識別的插件身份已經就緒; Binder 契約, 腳本 API 與設定頁按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 的階段推進. 需要 AutoJs6 6.8.0 (組建 5282) 或更高版本.

******

### 功能

******

外掛程式提供以下能力:

- 寄信: 純文字或 HTML, 多個收件人, 附件與內嵌圖片, 自訂信頭與優先級; 服務商不自動儲存已寄郵件時由插件寫入伺服器.
- 收信: 按頁列出資料夾, 在伺服器端搜尋 (服務商拒絕非 ASCII 搜尋時回退到用戶端過濾), 讀取文字與 HTML 內文, 並把附件直接下載到腳本工作目錄.
- 整理: 標記已讀或星標, 移動, 複製, 刪除, 清除, 以及建立, 重新命名或刪除資料夾; POP3 帳戶獲得唯讀子集.
- 監聽: 在腳本執行期間接收新郵件事件, 伺服器真正推送時用 IMAP IDLE, 否則輪詢 (預設 60 s, 可調): QQ 與 Sina 接受 IDLE 但不推送, 163 與 126 沒有 IDLE, POP3 帳戶一律輪詢; 斷網與插件進程重啟後監聽自動恢復.
- 服務商: 內建 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 預設, 自動填入主機, 連接埠與加密方式; 任何欄位都可為其他伺服器覆蓋.
- 認證: 密碼與服務商授權碼, 或由腳本提供並附帶重新整理回呼的 XOAUTH2 存取權杖.

******

### 使用方法

******

1. 在安裝了 AutoJs6 組建 5282 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安裝插件 APK.
2. 開啟 AutoJs6 插件中心, 確認 `Angus Mail` 已被識別並啟用它.
3. 準備帳戶: 在郵件服務商的設定中開啟 IMAP 或 POP3, 並取得授權碼或應用程式專用密碼 (QQ, 163, 126, Gmail, iCloud), 或 OAuth 2.0 存取權杖 (Outlook.com).
4. 在腳本中呼叫 `mail.connect(...)`, 或在插件設定頁 (插件的啟動器圖示, 或 AutoJs6 開發者選項 > 電郵帳戶設定) 儲存帳戶後以別名連線.

******

### 快速開始

******

一個寄送報表, 讀取帶附件的未讀郵件並等待驗證碼的腳本:

```js
let client = mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' });

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
```

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 能夠存取; 插件不匯出其他元件.
- INTERNET 權限只用於腳本指定伺服器的 IMAP, POP3 和 SMTP 連線; 插件不發起其他請求, 也不收集任何資料.
- 密碼與權杖從腳本到插件經 Binder 的專用欄位傳遞, 不會出現在日誌, JSON 文件, 錯誤訊息或當機報告中, 且只在工作階段生命週期內駐留記憶體. 設定頁儲存的帳戶由 Android Keystore 金鑰加密, 並排除在備份之外.
- 連線預設使用 TLS (按服務商要求選擇 SSL 或 STARTTLS); 明文連線與自簽憑證必須為每個帳戶明確宣告.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 權限只服務於設定頁的引導按鈕: 按鈕顯示系統是否可能在背景暫停插件, 並在用戶要求時打開系統對話框; 插件從不自行請求, 也沒有任何功能依賴該排除. P5 監聽矩陣實測了該排除的用途: 螢幕關閉一段時間後 (Doze) Android 會凍結背景應用程式的網絡, 監聽斷開, 重連超時, 新郵件要等裝置喚醒幾分鐘後才報告 (Android 9 上約四分鐘; Doze 結束時插件立即重連); 排除後監聽保持連接.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼發現外掛並協商相容性:

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

`AngusMailPluginService` 實作宿主 mail-api 契約 `org.autojs.plugin.mail.api.IMailPlugin`, 回應 `org.autojs.plugin.MAIL` (category `mail`). `AngusMailPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主啟動插件.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 按階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/19_

- `提示` P0 開發預覽: 倉庫骨架, 帶本機伺服器測試的郵件核心, 以及供 AutoJs6 插件中心識別的插件身份. Binder 契約, 腳本 API 與設定頁按 ROADMAP.md 的階段推進.
- `新增` 插件標識 `angus-mail` (engine `mail`), 含 INFO 服務, Wake Activity 以及 `org.autojs.plugin.MAIL` 服務; 其 `IMailPlugin` Binder 應答插件資訊, 能力, 服務商與已儲存帳戶列表以及會話信封 (具體操作隨 P2 落地)
- `新增` 基於 Eclipse Angus Mail 的郵件核心: IMAP / POP3 / SMTP 的工作階段屬性 (SSL 或 STARTTLS), 密碼與 XOAUTH2 認證, SMTP 寄信與 IMAP 收件匣列表, 已在本機 GreenMail 伺服器上驗證
- `新增` 郵件核心帳戶層 (路線圖 P2.1): 帳戶選項支援 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 與 Aliyun 的服務商預設, 按協議區分的逾時, `tls.trustAll`, IMAP `ID` 命令與脫敏的 `debug` 協議摘要; 工作階段延遲連線, 閒置時中斷並在斷線後重連; `session.test` 經 Binder 回傳各端點的能力與往返耗時
- `新增` 寄信 (路線圖 P2.2): `mail.send` 支援 to / cc / bcc / replyTo, 純文字與 HTML 內文 (`multipart/alternative`), 經宿主傳入的檔案描述符讀取的附件與內嵌圖片 (`multipart/mixed` / `multipart/related`), 自訂標頭, 優先級, `inReplyTo` / `references` 與日期; 收件人, 附件與標頭的上限, 拒絕標頭注入; `saveToSent` 僅在服務商不自動儲存時經 IMAP 附加副本; `messages.append` 寫入草稿並回傳 UID; 已在真機上對 QQ 郵箱與 Gmail 驗證
- `新增` 收信 (路線圖 P2.3): `folders.list` (樹形, special-use 角色取自 LIST / XLIST 屬性或約定名稱, 可選計數), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 為游標 (`before` / `after`), 支援 `order` 與 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查詢 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 標記 / 大小 / `header` / `messageId` / `uid` 與 `and` / `or` / `not`) 編譯為 IMAP SEARCH, 伺服器拒絕時在客戶端過濾 (`fallback`); `messages.get` 回傳純文字與 HTML 內文 (只有 HTML 的郵件派生文字), 全部標頭, 帶 `partId` 的附件列表, 內嵌預算 (`bodyTruncated` / `bodyParts`) 與 `includeRaw`; `attachments.download` 與 `messages.raw` 串流寫入宿主描述符並回報進度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字元集復原涵蓋 GBK / GB 18030 / ISO-2022-JP, 未聲明與未知字元集, 原始 8 位標頭與 RFC 2231 / 2047 檔名, 檔名淨化; IMAP `ID` 命令在每條連線上發送 (163 / 126 拒絕未識別的連線), 伺服器不能解析 `UID EXPUNGE` 時改用整個資料夾的 `EXPUNGE`; 已在真機上對 QQ 郵箱, 163 郵箱與 Gmail 驗證
- `新增` POP3 (路線圖 P2.4): `receive: "pop3"` 的帳戶透過同一組操作讀取唯一的 `INBOX`, `uid` 為 UIDL 字串: `folders.list` 只回傳 `INBOX` (可帶計數), `messages.list` 依 UIDL 游標分頁且只取信頭 (`TOP`), `messages.search` 在客戶端依信頭由新到舊過濾, 湊夠 `limit` 筆即停 (最多 200 個候選, 每個一次 `TOP` 往返), `messages.get` / `messages.raw` / `attachments.download` 下載整封郵件, `messages.delete` 發 `DELE` 並在郵箱關閉時生效; 標記, 移動, 複製, expunge, 附加, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 與內文搜尋在連線之前即回傳 `UNSUPPORTED_OPERATION`; 已在真機上對 QQ 郵箱驗證
- `新增` Binder 會話控制 (路線圖 P2.5): 只有已安裝且與本插件同簽署的 AutoJs6 宿主能開啟會話或列出已儲存帳戶 (否則 `SecurityException`, 規則與 MCP Server 插件一致); 請求與回應信封不超過 `MAX_ENVELOPE_BYTES`, 錯誤訊息不超過 `MAX_ERROR_MESSAGE_BYTES`; 每個會話依序執行呼叫, 執行中的呼叫之後最多排隊 `MAX_QUEUED_CALLS` 個, 再多則 `LIMIT_EXCEEDED`; `cancel` 立即回應排隊中的呼叫, 並透過關閉插槽中斷執行中的呼叫, 伺服器無回應時不再等到讀取逾時; `close` 讓全部待處理呼叫回應 `SESSION_CLOSED`; `getStatus` 回報 `queued` 與 `active`; 操作表標明每個操作支援的收信協定, POP3 帳戶在解析參數前即被拒絕; 能力集宣告 `append` 與 `clientSearchFallback`
- `新增` 10 種語言的 README, 插件中心說明與更新日誌
- `新增` 已儲存帳戶儲存 (路線圖 P4.1): 在插件內儲存的帳戶把非秘密的帳戶文件與經 Android Keystore 主密鑰 AES-256-GCM 加密的密碼或存取權杖存在一起; 認證資料綁定別名, 秘密類型與帳戶文件, 在磁碟上被改動或移動的記錄不再能解密; 記錄存於 `noBackupFilesDir` (本已排除在備份之外), 在檔案鎖下原子寫入, 秘密只經過用後即清零的 `CharArray` / `ByteArray` 緩衝區; 別名去除首尾空白, 經 NFC 正規化且不區分大小寫
- `新增` 已儲存帳戶會話 (路線圖 P4.3): `openSession` 接受別名形態 (`accountAlias`) 並在插件進程內解密秘密, `mail.connect('alias')` 因此不經 Binder 傳遞任何憑證; `listSavedAccounts` 回傳每個已儲存帳戶的別名, 地址, 使用者名稱, 服務商, 認證方式, 收信協定, 端點與預設標記, 不含任何秘密; 能力集合新增 `savedAccounts`
- `新增` 設定頁 (路線圖 P4.2): 啟動器圖示開啟帳戶頁, 列出每個已儲存帳戶的地址, 服務商, 收信協定與認證方式, 提供編輯, 測試連接, 設為或取消預設以及刪除; 帳戶編輯頁可按服務商預設自動填入, 也可填寫自訂 IMAP / POP3 / SMTP 伺服器及其加密方式與連接埠, 密碼或存取權杖直接從輸入欄讀入用後即清零的 `CharArray`, 編輯時留空則沿用已儲存的秘密, 儲存前可對所填伺服器執行 `session.test` 並按協定顯示結果與耗時而不寫入磁碟; 頁面跟隨 AutoJs6 宿主的主題, 夜間模式與語言, 編輯頁重建後還原除秘密外的全部欄位
- `新增` 設定入口 (路線圖 P4.3): AutoJs6 宿主經匯出的 `org.autojs.plugin.MAIL_SETTINGS` 活動開啟帳戶頁, 該活動要求插件權限, 只接受無參數的請求並立即結束; 能力集合宣告 `mailSettingsVersion` 1; 啟動器入口本身不帶該權限
- `新增` 發行歷史 (路線圖 P4.5): 設定頁與關於頁可開啟發行歷史頁, 內容取自隨插件打包的目前語言更新日誌 (無對應翻譯時回退英語), 每個版本一張卡片, 含日期與帶標籤的條目; 插件不做自身的更新檢查, 更新跟隨 AutoJs6 插件中心
- `新增` 電池優化引導 (路線圖 P4.6): 設定頁顯示系統是否可能在背景暫停本插件 (`PowerManager.isIgnoringBatteryOptimizations`), 說明影響後經 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 打開系統對話框; 清單因此聲明 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; 啟動時不發起任何請求, 也沒有功能依賴該排除
- `新增` 新郵件監聽, IMAP IDLE (路線圖 P5): 郵件核心以獨立連線用 `IMAPFolder.idle` 監聽資料夾, 每 24 分鐘續期一次 IDLE, 新郵件按 UID 抓取 (信封, 或按需內文) 且只回報一次, 斷線後按指數退避重連 (1 s 起, 上限 5 min, 帶抖動), 資料夾 UIDVALIDITY 變更時回報 `resync`; 伺服器無 IDLE 或 IDLE 連續失敗 3 次則切換為輪詢並發出 `mode` 事件; 每個會話最多 `MAX_WATCHES_PER_SESSION` 個監聽, 會話關閉時一併關閉
- `新增` 新郵件監聽, 輪詢 (路線圖 P5): `mode: "poll"` 的監聽每 `pollIntervalMs` (預設 60 s, 下限 `MIN_POLL_INTERVAL_MS`, 上限 1 小時) 按 UID 對資料夾做差分, 連線在輪次之間保持; POP3 帳戶一律輪詢, 按 UIDL 差分且每輪登入一次, 輪次之間不鎖定信箱, 只回報新增, 不回報刪除; 首輪只取快照, 不回報積壓
- `新增` 經 Binder 的新郵件監聽 (路線圖 P5): `IMailSession.watch` 在郵件核心的監聽器上開啟監聽並立即回應 (會話已關閉, 達到 `MAX_WATCHES_PER_SESSION`, 參數不可用或宿主回呼已死亡時回傳 null, 原因記入會話狀態); 事件由投遞執行緒帶宿主的 `generation` 與自 1 起計數的 `seq` 送達宿主的 `oneway` 回呼, 宿主停止消費時 `MAX_WATCH_QUEUE` 條事件的佇列摺疊為一條 `resync`, 超過 `MAX_ENVELOPE_BYTES` 的事件去掉內文或退化為 `resync` 後送出, `stop`, 會話關閉與宿主死亡均以單條 `closed` 事件結束監聽, 預設網絡變更或遺失時執行中的監聽立即重連, 能力集現在宣告 `idle`
- `修復` 服務商預設 (路線圖 P3.2): 163 郵箱與 126 郵箱會在伺服器端保存每封經 SMTP 發出的郵件, 兩者的 `autoSavesSent` 改為 true, 預設 `saveToSent` 不再向 `已发送` 追加第二份副本 (真實 163 帳戶核實: `saveToSent: false` 發出的郵件數分鐘後出現在已發送資料夾)
- `修復` AGP 9.1 構建時的 SDK XML v4 解析警告及 JVM 單元測試組裝任務誤觸發 APK 原生程式庫對齊檢查的問題 (共用構建外掛 1.8.3)
- `修復` 帳戶編輯器 (路線圖 P4.7): 整個表單退出 Android 自動填充框架, 密碼管理器不再索取授權碼; 此前 HyperOS (API 35) 會在儲存後關閉編輯器時彈出 "自動儲存帳號密碼".
- `修復` QQ, Sina, 163 與 126 的新郵件監聽 (郵件路線圖 P5 裝置矩陣): 服務商預設新增 `idlePush` (預設表版本 2), 這四家上 `mode: auto` 從一開始就輪詢而不再進入 IDLE, 因為 QQ 與 Sina 接受 IMAP IDLE 卻在客戶端閒置期間從不推送 (真實帳戶, 2026-09-19: 10 分鐘內沒有任何未標記回應; Sina 還會在 60 s 後斷開連線), 而 163 與 126 根本沒有 IDLE; 明確的 `mode: 'idle'` 仍會進入 IDLE. 矩陣本身 (QQ 在 API 24 模擬器與兩部 Sony 手機, 163 在一部 Redmi: 終止插件進程, 斷網, Wi-Fi 切流動網絡, 強制 Doze) 記錄於 `docs/dev/p5-watch-evidence.md`, 配套冒煙腳本 `docs/smoke/watch.js` 與驅動 `.python/run_watch_matrix.py`; 同一矩陣還表明 Doze 會凍結背景應用程式的網絡 (監聽的重連超時, Android 9 上新郵件在喚醒後約四分鐘才報告), 因此插件現在在裝置離開 Doze 的瞬間重連其監聽, 設定頁的電池優化引導文案說明了該排除的用途
- `修復` TLS 矩陣 (郵件路線圖 P6): 隱式 SSL, STARTTLS (經 GreenMail 前置的 STARTTLS 代理), 明文, 帶 / 不帶 `tls.trustAll` 的自簽證書, 主機名不匹配的受信證書, 端口模式錯配以及不提供升級的端口, 現已對 IMAP / POP3 / SMTP 逐一測試 (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`); 測試發現 Angus 的 POP3 存儲把缺失的 STLS 升級與握手前超時報告為認證失敗, 錯誤映射器現改為 `TLS_FAILED` 與 `TIMEOUT` 而非 `AUTH_FAILED`; `TlsDeviceTest` 在 API 24 / 28 / 33 上確認郵件核心以平台預設設定即可連接僅 TLS 1.2 的伺服器, 並自 API 29 起協商 TLS 1.3
- `修復` 字元集矩陣 (郵件路線圖 P6): GB18030 / GBK / GB2312 / Big5 / ISO-2022-JP / EUC-KR / UTF-8 在主題, 顯示名, 正文與兩種檔案名形態中按聲明, 未聲明與誤聲明三種情形逐一斷言 (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`), 並在 API 24 / 28 / 33 上核對 (`CharsetDeviceTest`); 本輪修復兩處解碼缺口: 聲明為 `us-ascii` / ISO-8859-1 卻攜帶 UTF-8 或 GB 18030 位元組的正文或信頭此前解出 Latin-1 亂碼 (Jakarta 把 `us-ascii` 映射為永不失敗的 ISO-8859-1), 現改走猜測鏈; 無編碼詞的原始 ISO-2022-JP 主題與正文現按其轉義序列識別; Big5 與 EUC-KR 仍需聲明 (其位元組對在 GB 18030 中同樣合法)
- `修復` 服務商兼容矩陣 (郵件路線圖 P6): QQ, 163, 126, yeah.net 與 Sina 以真實帳戶跑完發信, 列表, 伺服器與客戶端中文搜尋, 正文, 附件位元組, 標記, 建立資料夾, 移動, 監聽與 POP3 (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`), 差異記入預設 notes: QQ 對中文 SEARCH 答 OK 零命中 (請用 `fallback: 'always'`), 改寫 Message-ID 與 ENVELOPE 的 To 顯示名, 拒絕 CREATE, 且其自存的已發送副本一段時間內不可按 UID 尋址; 163 對近期郵件的文字搜尋零命中; Sina 只接受 ALL, SINCE 與標誌搜尋鍵; 五家均不儲存自訂關鍵字. 本輪同時修復一處客戶端缺陷: POP3 XOAUTH2 登入被以 SASL continuation 拒絕時 (Gmail) 因 Angus Mail 忽略拒絕而報為可重試的 `IO_FAILED`, 現由郵件核心核實登入並返回 `AUTH_FAILED` (`Pop3OAuthScriptedTest`). Gmail (令牌過期), Outlook.com 與 iCloud (無帳戶) 仍為未核實
- `優化` 錯誤映射: POP3 伺服器在登入後因帳戶未開啟 POP 存取而拒絕郵箱 (Gmail 對 STAT 答 `[SYS/PERM] Your account is not enabled for POP access`) 時, 現在得到說明原因的 `UNSUPPORTED_OPERATION`, 而不是可重試的 `IO_FAILED` "I/O failed"
- `優化` 服務商預設: Sina 郵箱補上已發送資料夾名 (`已发送`), 並註明伺服器不保存已發郵件副本且拒絕 IMAP CREATE (資料夾只能在網頁端建立); 126 郵箱伺服器保存已發郵件副本已用真實帳戶驗證
- `優化` 服務商預設: Yahoo Mail 與 Aliyun Mail 的說明註明這兩個預設未經真實帳戶核實 (項目無法取得測試帳戶), 其已發送副本行為按公開文檔推定.
- `優化` 秘密審計 (郵件路線圖 P6): 對郵件核心與應用程式搜尋日誌語句, 控制台輸出, Jakarta 偵錯開關以及每一處秘密被實體化的位置; 結果 (`docs/dev/p6-secret-audit.md`) 由 `SecretAuditTest` 強制執行: 任何日誌或偵錯語句都會使構建失敗, `reveal()` 被固定在三處 Jakarta 認證呼叫, 並檢查 Jakarta 會話永不偵錯, 帳戶 JSON 中的秘密被拒絕且不回顯, 值物件, 異常映射器與協定摘要在秘密傳播的每種形式下都將其遮蔽
- `優化` 敵意輸入 (郵件路線圖 P6): 無論郵件攜帶什麼, 郵件文檔現在都有界 (四個地址列表合計 500 項, 地址與顯示名截至 320 字元, 主題 / 標識 / 信頭值截至 4096 字元, `headers` 映射至多 64 KiB, MIME 樹深度至多 32 且部件至多 256, 未知大小的正文按內聯預算截讀), 單封敵意郵件不再能以 `LIMIT_EXCEEDED` 封死整頁列表; 損壞的 base64, 未知傳輸編碼, 無 boundary 或空的 multipart 改為寬鬆解碼而不再被誤判為斷線; 過深或無法解析的 multipart 作為一個葉子保持可下載; `HostileInputTest` 與 `HostileInputGreenMailTest` (17 例, `docs/dev/p6-hostile-input.md`) 覆蓋超深 / 超寬 MIME, 20000 收件人, 信頭炸彈, 缺失 Content-Type, 非法 base64, 遞歸 `message/rfc822`, 敵意檔案名, 大小不符與非法 UTF-8
- `依賴` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 與 Jakarta Activation API 2.1.4
- `依賴` 附加 GreenMail 2.1.13 用於 JVM 郵件核心測試 (僅測試範圍)
- `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為共用插件契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
- `依賴` 附加 `mail-api.aar` (AutoJs6 模組 `plugin-api/mail-api`, 宿主組建 6.8.0 / 5282, MPL 2.0) 作為郵件 Binder 契約 (六個 AIDL 介面, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 並在 `locks/host-api-aars.lock` 中鎖定雜湊

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

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

收集發佈產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與生成產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 本地化與文件生成

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

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部生成產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
