<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>让 AutoJs6 脚本通过 IMAP, POP3 和 SMTP 收发, 搜索和监听邮件</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### 简介

******

Angus Mail 为 AutoJs6 脚本提供全局对象 `mail`, 用于发送邮件, 列出和搜索邮箱, 读取正文, 下载附件, 管理标记与文件夹, 以及监听文件夹中的新邮件. 它基于 Jakarta Mail 的参考实现 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 通过 TLS 使用 IMAP, POP3 和 SMTP 协议.

全部邮件流量都在插件进程内完成. AutoJs6 通过 Binder 服务发现插件, 交出脚本提供的账户 (或插件设置页中保存的别名), 并接收 JSON 结果与附件流; 宿主本身不含任何邮件代码. 除非你选择在插件中保存账户, 凭据只在会话生命周期内驻留内存.

******

### 当前状态

******

版本 1.2.0 在 1.1.0 的后台守望 (路线图 P8) 之上新增 Google 与 Microsoft 账号的浏览器登录 (路线图 P9); P0 至 P8 各阶段的全部条目已随 1.0.0 至 1.1.0 发布, 证据见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (build 5282) 或更高版本; "邮件到达时" 任务需要携带邮件契约版本 2 的宿主构建; 完整的脚本 API 参考见 [AutoJs6 文档](https://docs.autojs6.com/#/mail).

******

### 功能

******

插件提供以下能力:

- 发信: 纯文本或 HTML, 多个收件人, 附件与内联图片, 自定义信头与优先级; 服务商不自动保存已发送邮件时由插件写入服务器.
- 收信: 按页列出文件夹, 在服务器端搜索 (服务商拒绝非 ASCII 搜索时回退到客户端过滤), 读取文本与 HTML 正文, 并把附件直接下载到脚本工作目录.
- 整理: 标记已读或星标, 移动, 复制, 删除, 清除, 以及创建, 重命名或删除文件夹; POP3 账户获得只读子集.
- 监听: 在脚本运行期间接收新邮件事件, 服务器真正推送时用 IMAP IDLE, 否则轮询 (默认 60 s, 可调): QQ 与 Sina 接受 IDLE 但不推送, 163 与 126 没有 IDLE, POP3 账户一律轮询; 断网与插件进程重启后监听自动恢复.
- 后台守望: 设置页的守望页面在没有脚本运行时以前台服务保持到已保存账户的 IMAP IDLE 或轮询连接, 记录每封新邮件, 并为所选守望唤醒 AutoJs6 的 "邮件到达时" 任务 (可按发件人与主题过滤), 邮件经 `engines.myEngine().execArgv.mail` 传入脚本.
- 服务商: 内置 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 预设, 自动填充主机, 端口与加密方式; 任何字段都可为其他服务器覆盖.
- 认证: 密码与服务商授权码, 或由脚本提供并附带刷新回调的 XOAUTH2 访问令牌.
- 浏览器登录: Gmail, Outlook.com 或 Microsoft 365 账户可以在插件设置页经系统浏览器以 Google 或 Microsoft 账号登录后添加 (OAuth 2.0 授权码流程 + PKCE); 插件把刷新令牌加密保存在本机, 每次会话前续期访问令牌, 并在账户页显示登录状态与 "重新登录" / "撤销登录" 操作. 脚本仍按别名连接, 不接触任何令牌.

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5282 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `Angus Mail` 已被识别并启用它.
3. 准备账户: 在服务商的网页设置中开启 IMAP 或 POP3 与 SMTP, 并取得授权码 (QQ, 163, 126, Sina) 或应用专用密码 (Gmail, iCloud, Yahoo); 登录密码本身通常不被接受. Gmail, Outlook.com 与 Microsoft 365 账户也可以在插件设置页经浏览器以 Google 或 Microsoft 账号登录 (认证方式选择 "使用 Google / Microsoft 账号登录 (浏览器)"), 或提供在别处取得的 OAuth 2.0 访问令牌.
4. 在脚本中调用 `mail.connect(...)`, 或在插件设置页 (插件的启动器图标, 或 AutoJs6 开发者选项 > 邮件账户设置) 保存账户后以别名连接.
5. 要在新邮件到达时运行脚本而无需常驻脚本: 在插件的守望页面 (设置 > 守望: 账户别名, 文件夹, 模式, 过滤) 添加守望, 按提示允许通知, 然后在 AutoJs6 中创建任务 (长按脚本 > 定时任务 > 广播触发 > 邮件到达时) 并选择守望; 该任务需要携带邮件契约版本 2 的 AutoJs6 构建.

******

### 服务商准备

******

每家服务商都要先在网页端开启 IMAP (或 POP3) 与 SMTP, 并以授权码, 应用专用密码或访问令牌代替登录密码; 各预设 (`provider` 的取值) 的要点:

- QQ 邮箱 (`qq`): 在网页版的账户设置中开启 IMAP/SMTP 服务并生成授权码, 以授权码作为 `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net 使用 `163` 预设并覆盖主机): 在网页版设置的 POP3/SMTP/IMAP 页开启服务并生成授权码; POP3 需要单独开启, 否则 IMAP 与 SMTP 接受的授权码会被 POP3 拒绝. 服务器要求每个 IMAP 连接先发送 `ID` 命令 (否则回答 `Unsafe Login`), 插件自动完成.
- 新浪邮箱 (`sina`): 在网页版的客户端设置中开启 IMAP/SMTP 服务并使用授权码. 服务器不保存已发邮件副本 (插件追加到 `已发送`), 不允许经 IMAP 新建文件夹, 文本搜索由插件在客户端完成.
- 最简单的路径是插件设置页的浏览器登录 ("使用 Google 账号登录 (浏览器)"), 无需应用专用密码且令牌自动续期. 其他方式: Gmail (`gmail`): 开启两步验证后在 Google 账号中生成应用专用密码作为 `password`, 或提供带 `https://mail.google.com/` 范围的 OAuth 2.0 访问令牌 (`accessToken` 与 `tokenProvider`); 文件夹位于 `[Gmail]` 命名空间, 新邮件由 IDLE 推送. 项目以令牌完成了真实账户核实.
- 最简单的路径是插件设置页的浏览器登录 ("使用 Microsoft 账号登录 (浏览器)"), 由插件自行取得并续期令牌. 其他方式: Outlook.com / Hotmail (`outlook`) 与 Microsoft 365 (`office365`): 微软已关闭个人账户的基本认证, 应用密码在 IMAP, POP3 与 SMTP 上都会被拒绝, `outlook` 预设因此只接受 OAuth 2.0 访问令牌 (`accessToken` 与 `tokenProvider`); 工作或学校账户 (`office365`) 可用密码或令牌, 但租户策略可能禁用 IMAP, POP3 或 SMTP AUTH. 令牌需要 `https://outlook.office.com/` 的委托权限 `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` 与 `SMTP.Send` (项目已用这样的令牌核实一个个人账户: IMAP, POP3, SMTP 与 IDLE 推送); 部分较新的个人邮箱被微软禁用了 SMTP AUTH (`535 5.7.139`), 用户设置中没有开关.
- iCloud (`icloud`): 在 Apple 账户中生成 App 专用密码; 没有 POP3 服务.
- Yahoo (`yahoo`) 与阿里云个人邮箱 (`aliyun`): 生成应用密码或授权码; 这两个预设按公开文档编写, 项目没有可用的测试账户, 未经核实.

其他服务器不填 `provider`, 而是给出 `imap` (或 `pop3`) 与 `smtp` 的 `host`, `port` 与 `tls` (`ssl`, `starttls` 或 `none`); 预设的任何字段也都可以覆盖. 全部选项见 [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType), 内置预设可用 `mail.providers.list()` 查看.

******

### 快速开始

******

一个以别名连接, 发送报表, 读取带附件的未读邮件, 监听验证码并异步搜索的脚本:

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

### 别名账户

******

别名是保存在插件内的账户. 在插件设置页 (启动器图标, 或 AutoJs6 开发者选项 > 邮件账户设置) 填写账户, 测试连接并保存后, 密码或令牌由 Android Keystore 密钥加密存放在插件的私有目录中且不参与备份; 脚本随后以 `mail.connect('别名')` 连接, 凭据不经过脚本, 也不经 Binder 传递.

设置页可把一个账户标记为默认, `mail.accounts.list()` 返回的条目带有 `default: true`, `mail.accounts.has(alias)` 检查别名是否存在. 需要同时使用多个账户时为每个别名各建一个客户端; `mail.setDefault(client)` 之后, `mail.fetch(...)` 这类转发方法直接作用于默认客户端.

******

### 兼容性

******

以下结论来自路线图 P6 的真实账户矩阵 (2026-09-19 与 09-20, 每家服务商向自己发送一封带中文主题, 正文, 显示名与附件名的邮件, 再逐项验证), 以及 TLS, 字符集, 生命周期与性能矩阵:

- QQ 邮箱: 发信, 列表, 正文, 附件, 标记, 移动 (`MOVE`) 与 POP3 全部通过; 中文搜索服务器回答 0 命中而不是错误, 需要 `fallback: 'always'`; 发出邮件的 Message-ID 被服务器改写; 不允许新建文件夹; 监听以轮询进行 (IDLE 不推送), 新邮件送达 15-40 s 后才在服务器上可见.
- 163 / 126 / yeah.net: 全部通过; 服务器保存已发副本; 没有 IDLE, 监听轮询; 163 对近期邮件的文本搜索回答 0 命中 (126 与 yeah.net 正常); 发件人显示名中的空格回读为下划线; yeah.net 的 POP3 需在网页端单独开启.
- 新浪邮箱: 通过; 服务器只接受 ALL, SINCE 与标记类搜索条件, 文本搜索自动回退到客户端过滤; 没有 IDLE; 不允许新建文件夹; 已发副本由插件追加.
- Gmail: 以 OAuth 2.0 令牌通过全部行, 新邮件经 IDLE 推送 (约 30 s, 为 Gmail 自身的通知节奏); 含中文的服务器搜索全部命中 (插件不启用 `UTF8=ACCEPT`); 自定义 IMAP 关键字会被保存 (六家中唯一); POP3 视图不含账户自己发出的邮件.
- Outlook.com / Hotmail: 以 OAuth 2.0 令牌在一个个人账户上通过全部行 (2026-09-21): 文件夹角色来自常规名称, 服务器保存已发送副本并改写 Message-ID, `MOVE` 与建夹正常, POP3 以两行式 `AUTH XOAUTH2` 登录, IDLE 约 10 s 内推送 (七家中最快); 中文主题的服务器搜索能命中但可能耗时数分钟; 应用密码在 IMAP, POP3 与 SMTP 上仍被拒绝 (`AUTH_MECHANISM_UNSUPPORTED`), 部分较新的个人邮箱被微软禁用了 SMTP AUTH (`535 5.7.139`); iCloud, Yahoo 与 Aliyun 没有可用的测试账户, 预设未经核实.
- TLS 与字符集: 隐式 SSL, STARTTLS, 明文, 自签证书 (带与不带 `tls.trustAll`), 主机名不匹配与端口模式错配在 IMAP, POP3 与 SMTP 上逐一测试, 失败映射为 `TLS_FAILED`, `TIMEOUT` 等可判断的错误码; GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR 与 UTF-8 的主题, 显示名, 正文与文件名在声明, 未声明与误声明三种情形下逐一断言.
- 设备与生命周期: Android 7.0 (API 24) 模拟器, Sony (Android 9) 与 Redmi (Android 13) 实机; 脚本正常退出, `exit()`, `engines.stopAll()`, 强停宿主或插件, 原地升级, 禁用与卸载插件八种结束方式下连接, 绑定与线程均被回收; 息屏进入 Doze 后监听断开并在设备唤醒后恢复, 需要持续监听时可在设置页申请电池优化豁免.
- 性能基线: 本地 10000 封收件箱与 50 MiB 附件在 JVM, Redmi 与 Sony 上的列表, 搜索, 下载, 发送与一小时 IDLE 待机数据见 `docs/dev/p6-performance-baseline.md`; 邮件文档有界 (地址, 信头, MIME 树与内联正文均有上限), 敌意输入不会撑爆会话.

******

### 常见问题

******

- **`AUTH_FAILED` 怎么排查?** 先确认使用的是授权码或应用专用密码而不是登录密码, 且已在网页端开启对应协议 (IMAP 与 POP3 是分别开启的); 调用 `client.test()` 分别查看收信端点与 SMTP 的结果与错误码. 令牌账户的 `AUTH_FAILED` 通常是令牌过期, 提供 `tokenProvider` 后插件会刷新并重试一次. 错误对象的 `code`, `details` 与 `retryable` 说明是否值得重试.
- **163 / 126 报 `Unsafe Login`?** 网易的 IMAP 服务器拒绝未发送 `ID` 命令的连接, 插件对每个 IMAP 连接在登录后立即发送 `ID`, 正常情况下不会遇到. 若仍出现, 请在网页端重新开启 IMAP 服务并重新生成授权码.
- **中文搜索没有结果?** 各服务器对非 ASCII 搜索的处理不同: 新浪拒绝 (插件自动回退到客户端过滤), QQ 与 163 回答 0 命中而不报错 (默认的 `fallback: 'client'` 不会触发). 对这些账户使用 `fallback: 'always'`, 并用 `since` 或 `limit` 缩小范围; 客户端的正文过滤要逐封抓取, 在大邮箱上可能很慢.
- **`mail.connect` 成功了, 第一次 `fetch` 才报错?** `connect` 只打开插件会话, 不连接邮件服务器; 首个网络方法才登录 (SMTP 在首次发信时). 想提前验证账户请调用 `client.test()`.
- **监听在息屏后停了?** Android 的 Doze 会冻结后台应用的网络, 监听断开, 新邮件在设备唤醒后几分钟内补报 (Doze 结束时插件立即重连). 需要持续监听时, 在设置页用引导按钮为插件申请电池优化豁免; 监听只在脚本运行期间有效, 脚本退出即关闭.
- **如何连接 Outlook.com / Hotmail?** Microsoft 已为个人账户关闭基本认证, 预设不接受密码. 在插件设置页保存账户并把认证方式选为 "使用 Microsoft 账号登录 (浏览器)": 浏览器打开 Microsoft 登录页, 只有授权码返回插件, 访问令牌由插件自动续期; 之后脚本按别名连接. 脚本也仍可把在别处取得的访问令牌作为 `accessToken` 传入并经 `tokenProvider` 刷新. 若账户页显示 "需要重新登录", 说明续期被拒绝 (登录已被撤销或过期): 打开该账户的菜单并选择 "重新登录".
- **POP3 账户能做什么?** 只有 `INBOX`, `uid` 为 UIDL 字符串; 列表, 读取, 下载, 删除与轮询监听可用; 标记, 移动, 复制, 追加, 清除与文件夹管理返回 `UNSUPPORTED_OPERATION`; 搜索在客户端进行且只有信封条件可用.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 插件不导出其他组件.
- INTERNET 权限用于与脚本指定的服务器建立 IMAP, POP3 与 SMTP 连接; 对经浏览器登录的账户, 另外只向 Google (`oauth2.googleapis.com`) 与 Microsoft (`login.microsoftonline.com`) 的令牌端点发起 HTTPS 请求: 交换授权码, 续期即将过期的访问令牌, 以及应用户要求撤销 Google 令牌. 插件不发起其他任何请求, 也不收集数据.
- 密码与令牌从脚本到插件经 Binder 的专用字段传递, 不会出现在日志, JSON 文档, 错误消息或崩溃报告中, 且只在会话生命周期内驻留内存. 设置页保存的账户由 Android Keystore 密钥加密, 并排除在备份之外. 浏览器登录的令牌以同样方式保存: 刷新令牌不离开本机, 只有访问令牌交给邮件会话, AutoJs6 与脚本都无法读取; "撤销登录" 会立即丢弃它们.
- 连接默认使用 TLS (按服务商要求选择 SSL 或 STARTTLS); 明文连接与自签名证书必须为每个账户显式声明.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限只服务于设置页的引导按钮: 按钮显示系统是否可能在后台暂停插件, 并在用户要求时打开系统对话框; 插件从不自行请求, 也没有任何功能依赖该排除. P5 监听矩阵实测了该排除的用途: 屏幕关闭一段时间后 (Doze) Android 会冻结后台应用的网络, 监听断开, 重连超时, 新邮件要等设备唤醒几分钟后才报告 (Android 9 上约四分钟; Doze 结束时插件立即重连); 排除后监听保持连接.
- 四项权限只服务于 1.1.0 的后台守望. FOREGROUND_SERVICE 与 FOREGROUND_SERVICE_SPECIAL_USE 运行守望服务 (类型 `specialUse`, 子类型 `mail_background_watch`: 邮件守望是等待服务器推送的长连接, 而不是有界的数据同步), 只显示一条低优先级通知; POST_NOTIFICATIONS 仅在守望页面启用守望时申请, 以便 Android 13 及以上显示该通知; RECEIVE_BOOT_COMPLETED 支撑该页面的开机自启开关, 默认关闭, 打开时才启用接收器. 服务只从守望页面或宿主订阅启动, 只连接已保存账户, 秘密留在插件进程内; 发往 AutoJs6 的唤醒广播只携带邮件信封 (不含正文), 且只送达受 PLUGIN 签名权限保护的接收器.

请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者; 宿主使用这些标识发现插件并协商兼容性:

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

`AngusMailPluginService` 实现宿主 mail-api 契约 `org.autojs.plugin.mail.api.IMailPlugin`, 响应 `org.autojs.plugin.MAIL` (category `mail`). `AngusMailPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.2.0

_2026/09/22_

- `提示` 只有携带对应服务商 OAuth 2.0 客户端 id 的构建才提供浏览器登录 (维护者的注册信息在构建时从 Git 忽略的 `oauth-clients.properties` 读取); 没有它们的构建保留令牌与应用专用密码路径, 并在认证方式对话框中说明. Google 客户端需先通过 Google Cloud 项目的敏感 scope 审核, 之后任意账户才能登录; 在此之前 Google 只允许项目的测试用户.
- `新增` Google 与 Microsoft 账号的浏览器登录 (邮件路线图 P9): 账户编辑器为 Gmail 预设提供 "使用 Google 账号登录 (浏览器)", 为 Outlook.com 与 Microsoft 365 预设提供 "使用 Microsoft 账号登录 (浏览器)"; 登录在 Custom Tab (回退为任意浏览器) 中打开服务商页面, 携带 PKCE (`S256`) 与随机 `state` 的 OAuth 2.0 授权码请求, 重定向 (`<applicationId>://oauth2/microsoft`, 或 Google 反转客户端 id 的 scheme) 落在 `OAuthRedirectActivity`, 由它交给等待中的登录页面; 页面拒绝任何 `state` 不匹配的重定向, 经 HTTPS 在令牌端点交换授权码 (`HttpsFormPoster`, 插件唯一的 HTTP 客户端), 并从 id 令牌预填地址
- `新增` 令牌存储与续期: 浏览器登录的令牌是账户存储中新种类 `OAUTH2` 的一条加密记录 (从不作为 Binder 字段, 从不出现在 `mail.accounts.list()`), 账户文档带有 `oauth` 对象 (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`), `mail.accounts.list()` 会报告它; 该别名的每个会话 (脚本, 连接测试, 后台守望) 都从 `AccountSecrets` 取访问令牌, 剩余不足五分钟时经刷新令牌续期, 按账户串行; 续期被拒 (`invalid_grant`) 时记录标记 `needsReauth`, 会话以 `AUTH_FAILED` ("sign in again") 失败, 账户页在该账户旁显示 "需要重新登录"
- `新增` 账户页操作 "重新登录" (新的浏览器登录存入同一记录) 与 "撤销登录" (记录的令牌立即换成已撤销标记, 请求 Google 撤销刷新令牌, 账户在重新登录前停止工作); `gmail`, `outlook` 与 `office365` 预设的 `authHint` 首先提及浏览器登录 (`providers.json` 版本 4); 11 语言各 35 条新字符串; JVM 测试覆盖 PKCE, 授权请求与重定向解析, 基于脚本化传输的令牌客户端, 令牌文档, 服务商表, 构建的客户端与重定向 URI, `AccountSecrets` (续期, 拒绝标记, 已撤销记录, 擦除) 以及账户选项, 表单与账户文档中的 `oauth` 对象

#### v1.1.0

_2026/09/21_

- `提示` 1.1.0 新增后台守望 (邮件路线图 P8): 设置页的守望页面在没有脚本运行时以前台服务保持已保存账户的守望, 并唤醒 AutoJs6 的 "邮件到达时" 任务. 该任务及其守望选择器需要携带邮件契约版本 2 的宿主构建 (AutoJs6 6.8.0 构建 5282 之后); 旧版宿主上页面会提示无法唤醒 AutoJs6, 新邮件只进入守望的记录列表. 此功能新增四项权限, 理由见 README 安全章节: FOREGROUND_SERVICE 与 FOREGROUND_SERVICE_SPECIAL_USE (守望服务), POST_NOTIFICATIONS (其常驻通知, 仅在启用守望时申请) 与 RECEIVE_BOOT_COMPLETED (守望页面的开机自启开关, 默认关闭).
- `新增` 后台守望 (邮件路线图 P8): 设置页新增守望页面, 可为已保存账户配置至多 16 个守望 (`MAX_TRIGGERS`), 每个含名称, 账户别名, 文件夹, 模式 (自动, IDLE 或带间隔的轮询) 与可选的发件人 / 主题过滤, 存于 no-backup 目录下的 `mail-triggers/triggers.json`; `specialUse` 前台服务 `MailWatchService` 在没有脚本运行时以 P5 的监听器运行已启用的守望 (服务器推送时用 IDLE, 否则轮询, 断线按退避重连, 网络变化时立即重连) 并显示一条低优先级通知; 每封新邮件进入守望的记录列表 (最近 100 条信封摘要, `MAX_TRIGGER_RECORDS`, 不含正文), 页面显示连接状态, 最近错误, 记录与重连操作; 开机自启开关 (默认关闭) 启用 `BOOT_COMPLETED` 接收器, 重启后重新拉起服务
- `新增` 邮件契约版本 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, 能力特性 `backgroundWatch`): 宿主以 generation 与可选过滤订阅已配置的守望, 立即收到当前状态, 之后收到 `onStatus` (stopped, connecting, connected 或 failed, 附原因与最近错误) 与每封匹配邮件的 `onMail(generation, seq, event)`, `mail` 事件携带守望 id, 别名, 地址, 文件夹, 邮件信封与 `receivedAt`; 每个守望至多 4 个订阅者 (`MAX_TRIGGER_SUBSCRIBERS`), 已禁用或不存在的守望与不可用的选项以 reason 为 `refused` 的 `stopped` 状态拒绝, `update` 替换订阅者的过滤, `stop` 只结束订阅; 没有活动订阅者时每个事件以显式广播 `org.autojs.autojs6.action.MAIL_TRIGGER` 发往 AutoJs6 (受其 `PLUGIN` 签名权限保护), 即使没有脚本运行也能启动宿主的 "邮件到达时" 任务 (`MailTriggerBinderTest` 于 API 37 AVD; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `新增` 邮件核心新增页面, 服务与 Binder 共用的触发文档与规则 (`TriggerConfig`, 发件人与主题子串不区分大小写的 `TriggerFilter`, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) 与上限 `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS`, `MIN_TRIGGER_INTERVAL_MS` (3 s, 宿主每任务的节流间隔), 监听器新增 `onConnected` 回调, 后台守望在文件夹打开后即显示 `connected`

#### v1.0.1

_2026/09/21_

- `修复` Outlook.com 的 POP3 以 OAuth 2.0 令牌登录 (邮件路线图 P6 服务商矩阵, 2026-09-21): 服务器对 Angus Mail 默认发送的单行 `AUTH XOAUTH2 <base64>` 答 `-ERR Protocol error. Connection is closed.` 并断开连接, `outlook` 预设的 POP3 账户因此以 `AUTH_FAILED` 失败; 服务商预设新增 `pop3Xoauth2TwoLine` (目录版本 3, `outlook` 与 `office365` 为 true), 邮件核心对这两个预设以及未用预设而填写的微软 POP3 主机改为先发裸命令, 收到服务器的 `+` 续行后再发 base64 响应 (`Pop3OAuthScriptedTest` 5 例; 已在真实账户上经 JVM 与 API 33 真机核实)
- `优化` 服务商矩阵的 Outlook.com 列 (邮件路线图 P6) 与 P5 设备行, 以维护者 Entra 公共客户端的令牌在一个个人账户上跑通: 会话测试, 文件夹 (角色来自常规名称, 无 SPECIAL-USE), 发信 (`sentCopy = server`, Message-ID 被服务器改写), 列表 (约 7 s 可见), 服务器搜索 (各键均命中; 中文主题能正确命中但服务器在 2300 封的收件箱上要花数分钟), 正文, 附件, 标记 (自定义关键字不存储), 建夹, `MOVE`, 监听 (IDLE 在提交后约 10 s 内推送, 七家中最快), POP3 (UIDL 5 字符, 刚发的邮件在视图中) 与清理 (已发送副本可按 UID 寻址) 全部通过; Redmi (API 33) 上 baseline / 杀插件 / 关 Wi-Fi 三个监听场景 9 到 14 s 到达, 事件顺序与 Gmail 一致; 预设 notes, README 与证据文件记录了差异, 包括微软对较新个人邮箱给出的 `535 5.7.139 SmtpClientAuthentication is disabled for the Mailbox` 拒绝

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建与验证

******

本节面向希望从源码构建插件的开发者; 普通用户直接安装 Releases 页面的预构建 APK 即可.

构建 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 JVM 单元测试并构建 instrumentation 测试 APK:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

构建 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集发布产物并在文件名后追加版本与 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校验多语言文档源与生成产物是否同步 (CI 同样执行此检查):

```powershell
py .python\generate_markdown.py --check
```

构建需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 与插件版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 统一管理.

******

### 本地化与文档生成

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

`.readme/` 与 `.changelog/` 下的语言 JSON 文件是 README, 插件中心说明与更新日志的唯一文案源. 请始终修改这些 JSON 源文件并重新运行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 与更新日志产物不得手工编辑. 运行 `py .python/generate_markdown.py --check` 可校验全部生成产物.

******

### 许可证

******

项目代码基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE) 授权. 第三方组件及其许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- 邮件模块文档: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
