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

版本 1.0.0 是首个正式版本: 路线图 P0 至 P6 的全部条目 (邮件核心, Binder 契约, 脚本 API, 设置页与别名账户, 新邮件监听, 以及 TLS, 字符集, 服务商, 生命周期, 敌意输入, 秘密审计与性能矩阵) 均已完成并附有证据, 见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (构建 5282) 或更高版本; 脚本 API 的完整参考见 [AutoJs6 文档](https://docs.autojs6.com/#/mail).

******

### 功能

******

插件提供以下能力:

- 发信: 纯文本或 HTML, 多个收件人, 附件与内联图片, 自定义信头与优先级; 服务商不自动保存已发送邮件时由插件写入服务器.
- 收信: 按页列出文件夹, 在服务器端搜索 (服务商拒绝非 ASCII 搜索时回退到客户端过滤), 读取文本与 HTML 正文, 并把附件直接下载到脚本工作目录.
- 整理: 标记已读或星标, 移动, 复制, 删除, 清除, 以及创建, 重命名或删除文件夹; POP3 账户获得只读子集.
- 监听: 在脚本运行期间接收新邮件事件, 服务器真正推送时用 IMAP IDLE, 否则轮询 (默认 60 s, 可调): QQ 与 Sina 接受 IDLE 但不推送, 163 与 126 没有 IDLE, POP3 账户一律轮询; 断网与插件进程重启后监听自动恢复.
- 服务商: 内置 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 预设, 自动填充主机, 端口与加密方式; 任何字段都可为其他服务器覆盖.
- 认证: 密码与服务商授权码, 或由脚本提供并附带刷新回调的 XOAUTH2 访问令牌.

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5282 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `Angus Mail` 已被识别并启用它.
3. 准备账户: 在邮件服务商的网页端开启 IMAP 或 POP3 与 SMTP, 并取得授权码 (QQ, 163, 126, Sina), 应用专用密码 (Gmail, iCloud, Yahoo) 或 OAuth 2.0 访问令牌 (Outlook.com); 登录密码本身通常不被接受.
4. 在脚本中调用 `mail.connect(...)`, 或在插件设置页 (插件的启动器图标, 或 AutoJs6 开发者选项 > 邮件账户设置) 保存账户后以别名连接.

******

### 服务商准备

******

每家服务商都要先在网页端开启 IMAP (或 POP3) 与 SMTP, 并以授权码, 应用专用密码或访问令牌代替登录密码; 各预设 (`provider` 的取值) 的要点:

- QQ 邮箱 (`qq`): 在网页版的账户设置中开启 IMAP/SMTP 服务并生成授权码, 以授权码作为 `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net 使用 `163` 预设并覆盖主机): 在网页版设置的 POP3/SMTP/IMAP 页开启服务并生成授权码; POP3 需要单独开启, 否则 IMAP 与 SMTP 接受的授权码会被 POP3 拒绝. 服务器要求每个 IMAP 连接先发送 `ID` 命令 (否则回答 `Unsafe Login`), 插件自动完成.
- 新浪邮箱 (`sina`): 在网页版的客户端设置中开启 IMAP/SMTP 服务并使用授权码. 服务器不保存已发邮件副本 (插件追加到 `已发送`), 不允许经 IMAP 新建文件夹, 文本搜索由插件在客户端完成.
- Gmail (`gmail`): 开启两步验证后在 Google 账号中生成应用专用密码作为 `password`, 或提供带 `https://mail.google.com/` 范围的 OAuth 2.0 访问令牌 (`accessToken` 与 `tokenProvider`); 文件夹位于 `[Gmail]` 命名空间, 新邮件由 IDLE 推送. 项目以令牌完成了真实账户核实.
- Outlook.com / Hotmail (`outlook`) 与 Microsoft 365 (`office365`): 微软已关闭个人账户的基本认证, 应用密码在 IMAP, POP3 与 SMTP 上都会被拒绝, `outlook` 预设因此只接受 OAuth 2.0 访问令牌 (`accessToken` 与 `tokenProvider`); 工作或学校账户 (`office365`) 可用密码或令牌, 但租户策略可能禁用 IMAP, POP3 或 SMTP AUTH.
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
- Outlook.com / Hotmail: 三个账户的应用密码在 IMAP, POP3 与 SMTP 上均被微软拒绝 (`AUTH_MECHANISM_UNSUPPORTED`), 操作行等待令牌; iCloud, Yahoo 与 Aliyun 没有可用的测试账户, 预设未经核实.
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
- **Outlook.com / Hotmail 怎么接入?** 微软已关闭个人账户的基本认证, 需要通过 OAuth 2.0 授权流程 (需要一个已注册的应用) 取得带 IMAP, POP 与 SMTP 权限范围的访问令牌, 以 `accessToken` 传入并用 `tokenProvider` 刷新; 预设不接受密码.
- **POP3 账户能做什么?** 只有 `INBOX`, `uid` 为 UIDL 字符串; 列表, 读取, 下载, 删除与轮询监听可用; 标记, 移动, 复制, 追加, 清除与文件夹管理返回 `UNSUPPORTED_OPERATION`; 搜索在客户端进行且只有信封条件可用.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 插件不导出其他组件.
- INTERNET 权限只用于脚本指定服务器的 IMAP, POP3 和 SMTP 连接; 插件不发起其他请求, 也不收集任何数据.
- 密码与令牌从脚本到插件经 Binder 的专用字段传递, 不会出现在日志, JSON 文档, 错误消息或崩溃报告中, 且只在会话生命周期内驻留内存. 设置页保存的账户由 Android Keystore 密钥加密, 并排除在备份之外.
- 连接默认使用 TLS (按服务商要求选择 SSL 或 STARTTLS); 明文连接与自签名证书必须为每个账户显式声明.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限只服务于设置页的引导按钮: 按钮显示系统是否可能在后台暂停插件, 并在用户要求时打开系统对话框; 插件从不自行请求, 也没有任何功能依赖该排除. P5 监听矩阵实测了该排除的用途: 屏幕关闭一段时间后 (Doze) Android 会冻结后台应用的网络, 监听断开, 重连超时, 新邮件要等设备唤醒几分钟后才报告 (Android 9 上约四分钟; Doze 结束时插件立即重连); 排除后监听保持连接.

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

#### v1.0.0

_2026/09/19_

- `提示` 首个正式版本: 邮件核心, Binder 契约, 脚本 API `mail`, 设置页与别名账户, 新邮件监听, 以及 TLS, 字符集, 服务商, 生命周期, 敌意输入, 秘密审计与性能矩阵均已完成并附有证据 (ROADMAP.md P0 至 P6). 需要 AutoJs6 6.8.0 (构建 5282) 或更高版本.
- `新增` 插件标识 `angus-mail` (engine `mail`), 含 INFO 服务, Wake Activity 以及 `org.autojs.plugin.MAIL` 服务; 其 `IMailPlugin` Binder 应答插件信息, 能力, 服务商与已保存账户列表以及会话信封 (具体操作随 P2 落地)
- `新增` 基于 Eclipse Angus Mail 的邮件核心: IMAP / POP3 / SMTP 的会话属性 (SSL 或 STARTTLS), 密码与 XOAUTH2 认证, SMTP 发信与 IMAP 收件箱列表, 已在本地 GreenMail 服务器上验证
- `新增` 邮件核心账户层 (路线图 P2.1): 账户选项支持 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 与 Aliyun 的服务商预设, 按协议区分的超时, `tls.trustAll`, IMAP `ID` 命令与脱敏的 `debug` 协议摘要; 会话懒连接, 空闲时断开并在断线后重连; `session.test` 经 Binder 返回各端点的能力与往返耗时
- `新增` 发信 (路线图 P2.2): `mail.send` 支持 to / cc / bcc / replyTo, 纯文本与 HTML 正文 (`multipart/alternative`), 经宿主传入的文件描述符读取的附件与内联图片 (`multipart/mixed` / `multipart/related`), 自定义头, 优先级, `inReplyTo` / `references` 与日期; 收件人, 附件与头的上限, 拒绝头注入; `saveToSent` 仅在服务商不自动保存时经 IMAP 追加副本; `messages.append` 写入草稿并返回 UID; 已在真机上对 QQ 邮箱与 Gmail 验证
- `新增` 收信 (路线图 P2.3): `folders.list` (树形, special-use 角色取自 LIST / XLIST 属性或约定名称, 可选计数), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 为游标 (`before` / `after`), 支持 `order` 与 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查询 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 标记 / 大小 / `header` / `messageId` / `uid` 与 `and` / `or` / `not`) 编译为 IMAP SEARCH, 服务器拒绝时在客户端过滤 (`fallback`); `messages.get` 返回纯文本与 HTML 正文 (只有 HTML 的邮件派生文本), 全部头, 带 `partId` 的附件列表, 内联预算 (`bodyTruncated` / `bodyParts`) 与 `includeRaw`; `attachments.download` 与 `messages.raw` 流式写入宿主描述符并报告进度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字符集恢复覆盖 GBK / GB 18030 / ISO-2022-JP, 未声明与未知字符集, 原始 8 位头与 RFC 2231 / 2047 文件名, 文件名净化; IMAP `ID` 命令在每条连接上发送 (163 / 126 拒绝未识别的连接), 服务器不能解析 `UID EXPUNGE` 时改用整夹 `EXPUNGE`; 已在真机上对 QQ 邮箱, 163 邮箱与 Gmail 验证
- `新增` POP3 (路线图 P2.4): `receive: "pop3"` 的账户通过同一组操作读取唯一的 `INBOX`, `uid` 为 UIDL 字符串: `folders.list` 只返回 `INBOX` (可带计数), `messages.list` 按 UIDL 游标分页且只取信头 (`TOP`), `messages.search` 在客户端按信头由新到旧过滤, 凑够 `limit` 条即停 (最多 200 个候选, 每个一次 `TOP` 往返), `messages.get` / `messages.raw` / `attachments.download` 下载整封邮件, `messages.delete` 发 `DELE` 并在邮箱关闭时生效; 标记, 移动, 复制, expunge, 追加, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 与正文搜索在连接之前即返回 `UNSUPPORTED_OPERATION`; 已在真机上对 QQ 邮箱验证
- `新增` Binder 会话控制 (路线图 P2.5): 只有已安装且与本插件同签名的 AutoJs6 宿主能打开会话或列出已保存账户 (否则 `SecurityException`, 规则与 MCP Server 插件一致); 请求与响应信封不超过 `MAX_ENVELOPE_BYTES`, 错误消息不超过 `MAX_ERROR_MESSAGE_BYTES`; 每个会话顺序执行调用, 执行中的调用之后最多排队 `MAX_QUEUED_CALLS` 个, 再多则 `LIMIT_EXCEEDED`; `cancel` 立即应答排队中的调用, 并通过关闭套接字打断执行中的调用, 服务器无响应时不再等到读超时; `close` 让全部待处理调用应答 `SESSION_CLOSED`; `getStatus` 报告 `queued` 与 `active`; 操作表标明每个操作支持的收信协议, POP3 账户在解析参数前即被拒绝; 能力集宣告 `append` 与 `clientSearchFallback`
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `新增` 已保存账户存储 (路线图 P4.1): 在插件内保存的账户把非秘密的账户文档与经 Android Keystore 主密钥 AES-256-GCM 加密的密码或访问令牌存在一起; 认证数据绑定别名, 秘密类型与账户文档, 在磁盘上被改动或移动的记录不再能解密; 记录存于 `noBackupFilesDir` (本已排除在备份之外), 在文件锁下原子写入, 秘密只经过用后即清零的 `CharArray` / `ByteArray` 缓冲; 别名去除首尾空白, 经 NFC 规范化且不区分大小写
- `新增` 已保存账户会话 (路线图 P4.3): `openSession` 接受别名形态 (`accountAlias`) 并在插件进程内解密秘密, `mail.connect('alias')` 因此不经 Binder 传递任何凭据; `listSavedAccounts` 返回每个已保存账户的别名, 地址, 用户名, 服务商, 认证方式, 收信协议, 端点与默认标记, 不含任何秘密; 能力集合新增 `savedAccounts`
- `新增` 设置页 (路线图 P4.2): 启动器图标打开账户页, 列出每个已保存账户的地址, 服务商, 收信协议与认证方式, 提供编辑, 测试连接, 设为或取消默认以及删除; 账户编辑页可按服务商预设自动填充, 也可填写自定义 IMAP / POP3 / SMTP 服务器及其加密方式与端口, 密码或访问令牌直接从输入框读入用后即清零的 `CharArray`, 编辑时留空则沿用已保存的秘密, 保存前可对所填服务器执行 `session.test` 并按协议展示结果与耗时而不落盘; 页面跟随 AutoJs6 宿主的主题, 夜间模式与语言, 编辑页重建后恢复除秘密外的全部字段
- `新增` 设置入口 (路线图 P4.3): AutoJs6 宿主经导出的 `org.autojs.plugin.MAIL_SETTINGS` 活动打开账户页, 该活动要求插件权限, 只接受无参数的请求并立即结束; 能力集合宣告 `mailSettingsVersion` 1; 启动器入口本身不带该权限
- `新增` 发行历史 (路线图 P4.5): 设置页与关于页可打开发行历史页, 内容取自随插件打包的当前语言更新日志 (无对应翻译时回退英语), 每个版本一张卡片, 含日期与带标签的条目; 插件不做自身的更新检查, 更新跟随 AutoJs6 插件中心
- `新增` 电池优化引导 (路线图 P4.6): 设置页显示系统是否可能在后台暂停本插件 (`PowerManager.isIgnoringBatteryOptimizations`), 说明影响后经 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 打开系统对话框; 清单因此声明 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; 启动时不发起任何请求, 也没有功能依赖该排除
- `新增` 新邮件监听, IMAP IDLE (路线图 P5): 邮件内核以独立连接用 `IMAPFolder.idle` 监听文件夹, 每 24 分钟续期一次 IDLE, 新邮件按 UID 抓取 (信封, 或按需正文) 且只报一次, 断线后按指数退避重连 (1 s 起, 上限 5 min, 带抖动), 文件夹 UIDVALIDITY 变化时报 `resync`; 服务器无 IDLE 或 IDLE 连续失败 3 次则切换为轮询并发 `mode` 事件; 每个会话最多 `MAX_WATCHES_PER_SESSION` 个监听, 会话关闭时一并关闭
- `新增` 新邮件监听, 轮询 (路线图 P5): `mode: "poll"` 的监听每 `pollIntervalMs` (默认 60 s, 下限 `MIN_POLL_INTERVAL_MS`, 上限 1 小时) 按 UID 对文件夹做差分, 连接在轮次之间保持; POP3 账户一律轮询, 按 UIDL 差分且每轮登录一次, 轮次之间不锁定邮箱, 只报新增, 不报删除; 首轮只取快照, 不报积压
- `新增` 经 Binder 的新邮件监听 (路线图 P5): `IMailSession.watch` 在邮件内核的监听器上打开监听并立即应答 (会话已关闭, 达到 `MAX_WATCHES_PER_SESSION`, 参数不可用或宿主回调已死亡时返回 null, 原因记入会话状态); 事件由投递线程带宿主的 `generation` 与自 1 起计数的 `seq` 送达宿主的 `oneway` 回调, 宿主停止消费时 `MAX_WATCH_QUEUE` 条事件的队列折叠为一条 `resync`, 超过 `MAX_ENVELOPE_BYTES` 的事件去掉正文或退化为 `resync` 后送出, `stop`, 会话关闭与宿主死亡均以单条 `closed` 事件结束监听, 默认网络变化或丢失时运行中的监听立即重连, 能力集现在宣告 `idle`
- `修复` 服务商预设 (路线图 P3.2): 163 邮箱与 126 邮箱会在服务器端保存每封经 SMTP 发出的邮件, 两者的 `autoSavesSent` 改为 true, 默认 `saveToSent` 不再向 `已发送` 追加第二份副本 (真实 163 账户核实: `saveToSent: false` 发出的邮件数分钟后出现在已发送文件夹)
- `修复` AGP 9.1 构建时的 SDK XML v4 解析警告及 JVM 单元测试组装任务误触发 APK 原生库对齐检查的问题 (共享构建插件 1.8.3)
- `修复` 账户编辑器 (路线图 P4.7): 整个表单退出 Android 自动填充框架, 密码管理器不再索取授权码; 此前 HyperOS (API 35) 会在保存后关闭编辑器时弹出 "自动保存账号密码".
- `修复` QQ, Sina, 163 与 126 的新邮件监听 (邮件路线图 P5 设备矩阵): 服务商预设新增 `idlePush` (预设表版本 2), 这四家上 `mode: auto` 从一开始就轮询而不再进入 IDLE, 因为 QQ 与 Sina 接受 IMAP IDLE 却在客户端空闲期间从不推送 (真实账户, 2026-09-19: 10 分钟内没有任何未标记响应; Sina 还会在 60 s 后断开连接), 而 163 与 126 根本没有 IDLE; 显式 `mode: 'idle'` 仍会进入 IDLE. 矩阵本身 (QQ 在 API 24 模拟器与两台 Sony 手机, 163 在一台 Redmi: 杀插件进程, 断网, Wi-Fi 切蜂窝, 强制 Doze) 记录于 `docs/dev/p5-watch-evidence.md`, 配套冒烟脚本 `docs/smoke/watch.js` 与驱动 `.python/run_watch_matrix.py`; 同一矩阵还表明 Doze 会冻结后台应用的网络 (监听的重连超时, Android 9 上新邮件在唤醒后约四分钟才报告), 因此插件现在在设备离开 Doze 的瞬间重连其监听, 设置页的电池优化引导文案说明了该排除的用途
- `修复` TLS 矩阵 (邮件路线图 P6): 隐式 SSL, STARTTLS (经 GreenMail 前置的 STARTTLS 代理), 明文, 带 / 不带 `tls.trustAll` 的自签证书, 主机名不匹配的受信证书, 端口模式错配以及不提供升级的端口, 现已对 IMAP / POP3 / SMTP 逐一测试 (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`); 测试发现 Angus 的 POP3 存储把缺失的 STLS 升级与握手前超时报告为认证失败, 错误映射器现改为 `TLS_FAILED` 与 `TIMEOUT` 而非 `AUTH_FAILED`; `TlsDeviceTest` 在 API 24 / 28 / 33 上确认邮件内核以平台默认设置即可连接仅 TLS 1.2 的服务器, 并自 API 29 起协商 TLS 1.3
- `修复` 字符集矩阵 (邮件路线图 P6): GB18030 / GBK / GB2312 / Big5 / ISO-2022-JP / EUC-KR / UTF-8 在主题, 显示名, 正文与两种文件名形态中按声明, 未声明与误声明三种情形逐一断言 (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`), 并在 API 24 / 28 / 33 上核对 (`CharsetDeviceTest`); 本轮修复两处解码缺口: 声明为 `us-ascii` / ISO-8859-1 却携带 UTF-8 或 GB 18030 字节的正文或信头此前解出 Latin-1 乱码 (Jakarta 把 `us-ascii` 映射为永不失败的 ISO-8859-1), 现改走猜测链; 无编码词的原始 ISO-2022-JP 主题与正文现按其转义序列识别; Big5 与 EUC-KR 仍需声明 (其字节对在 GB 18030 中同样合法)
- `修复` 服务商兼容矩阵 (邮件路线图 P6): QQ, 163, 126, yeah.net 与 Sina 以真实账户跑完发信, 列表, 服务器与客户端中文搜索, 正文, 附件字节, 标记, 建夹, 移动, 监听与 POP3 (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`), 差异记入预设 notes: QQ 对中文 SEARCH 答 OK 零命中 (请用 `fallback: 'always'`), 改写 Message-ID 与 ENVELOPE 的 To 显示名, 拒绝 CREATE, 且其自存的已发送副本一段时间内不可按 UID 寻址; 163 对近期邮件的文本搜索零命中; Sina 只接受 ALL, SINCE 与标志搜索键; 五家均不存储自定义关键字. 本轮同时修复一处客户端缺陷: POP3 XOAUTH2 登录被以 SASL continuation 拒绝时 (Gmail) 因 Angus Mail 忽略拒绝而报为可重试的 `IO_FAILED`, 现由邮件核心核实登录并返回 `AUTH_FAILED` (`Pop3OAuthScriptedTest`). Gmail (令牌过期), Outlook.com 与 iCloud (无账户) 仍为未核实
- `修复` 生命周期矩阵 (邮件路线图 P6): 在 Redmi (API 33) 上以八种方式结束一个持有会话与 watch 的脚本: 正常退出, 开着 watch 与客户端 `exit()`, `engines.stopAll()`, 强停宿主, 强停插件, 原地升级插件, 禁用插件, 卸载插件; 每种情况下插件的连接, 绑定与文件描述符都在 30 s 内回到脚本前的状态, 宿主从不直接连接服务器 (`docs/dev/p6-lifecycle-matrix.md`, `.python/run_lifecycle_matrix.py`, `docs/smoke/lifecycle.js`). 矩阵发现并修复一处泄漏: Angus Mail 为每个套接字的写超时自建线程池, 只在 TLS 套接字经该包装层关闭时才关掉它, 而底层明文套接字已被 cancel 或 watch 停止关闭后 Android 会跳过这一步, 于是每条这样的连接在进程存续期间留下一条线程; 邮件核心现在把一个共享的守护定时器交给 Angus (`WriteTimeouts`, `WriteTimeoutExecutorTest`)
- `修复` 服务商矩阵的 Gmail 与 Outlook.com 列 (邮件路线图 P6, 维护者更新 Gmail 令牌并提供三个 Outlook.com / Hotmail 账户后回填): Gmail 以令牌通过会话测试, 文件夹, 发信, 列表, 服务器搜索, 正文, 附件, 标记与关键字, 建夹, 移动, 监听与 POP3, 其 IDLE 确实推送 (新邮件在投递后约 30 s 报告, 即 Gmail 的通知节奏), P5 设备矩阵 (基线, 杀插件, 关 Wi-Fi) 在 Redmi (API 33) 上以 IDLE 路径重跑 (`docs/dev/p6-provider-matrix.md`, `docs/dev/p5-watch-evidence.md`). 两个发现改动了邮件核心: Gmail 广告 `UTF8=ACCEPT`, Angus Mail 启用后 Gmail 反而拒绝所有含非 ASCII 文本的搜索 (`BAD Could not parse command`), 中文主题搜索因此回退到客户端; 现不再启用该扩展, 搜索像在其他服务器上一样携带 `CHARSET UTF-8` (`Utf8SearchTest`). 客户端过滤现在每 25 个候选向调试轨迹报告一次进度, 因为 `fallback: 'always'` 的正文搜索在命中不足 `limit` 时会扫完 2000 个候选的整个窗口, 每个候选两到三次往返, 在本网络下对 Gmail 约 43 分钟. Outlook.com: 三个账户对应用密码在 IMAP 与 POP3 上都答 `Basic authentication is disabled`, SMTP 答 `535`, 预设的仅 XOAUTH2 规则成立, 操作行等待令牌 (路线图 P9); 绕过预设后服务器自身的拒绝 (`LOGINDISABLED`, 仅 `AUTH=XOAUTH2`) 报为 `AUTH_MECHANISM_UNSUPPORTED` 而非 `SERVER_ERROR` (`LoginDisabledTest`)
- `优化` 错误映射: POP3 服务器在登录后因账户未开启 POP 访问而拒绝邮箱 (Gmail 对 STAT 答 `[SYS/PERM] Your account is not enabled for POP access`) 时, 现在得到说明原因的 `UNSUPPORTED_OPERATION`, 而不是可重试的 `IO_FAILED` "I/O failed"
- `优化` 服务商预设: Sina 邮箱补上已发送文件夹名 (`已发送`), 并注明服务器不保存已发邮件副本且拒绝 IMAP CREATE (文件夹只能在网页端创建); 126 邮箱服务器保存已发邮件副本已用真实账户验证
- `优化` 服务商预设: Yahoo Mail 与 Aliyun Mail 的说明注明这两个预设未经真实账户核实 (项目无法获得测试账户), 其已发送副本行为按公开文档推定.
- `优化` 秘密审计 (邮件路线图 P6): 对邮件内核与应用搜索日志语句, 控制台输出, Jakarta 调试开关以及每一处秘密被实体化的位置; 结果 (`docs/dev/p6-secret-audit.md`) 由 `SecretAuditTest` 强制执行: 任何日志或调试语句都会使构建失败, `reveal()` 被固定在三处 Jakarta 认证调用, 并检查 Jakarta 会话永不调试, 账户 JSON 中的秘密被拒绝且不回显, 值对象, 异常映射器与协议摘要在秘密传播的每种形式下都将其掩盖
- `优化` 敌意输入 (邮件路线图 P6): 无论邮件携带什么, 邮件文档现在都有界 (四个地址列表合计 500 项, 地址与显示名截至 320 字符, 主题 / 标识 / 信头值截至 4096 字符, `headers` 映射至多 64 KiB, MIME 树深度至多 32 且部件至多 256, 未知大小的正文按内联预算截读), 单封敌意邮件不再能以 `LIMIT_EXCEEDED` 封死整页列表; 损坏的 base64, 未知传输编码, 无 boundary 或空的 multipart 改为宽松解码而不再被误判为断线; 过深或无法解析的 multipart 作为一个叶子保持可下载; `HostileInputTest` 与 `HostileInputGreenMailTest` (17 例, `docs/dev/p6-hostile-input.md`) 覆盖超深 / 超宽 MIME, 20000 收件人, 信头炸弹, 缺失 Content-Type, 非法 base64, 递归 `message/rfc822`, 敌意文件名, 大小不符与非法 UTF-8
- `优化` 性能基线 (邮件路线图 P6): 以本地种子服务器 (10000 封收件箱, 一封 50 MiB 附件) 在 JVM, Redmi (API 33) 与 Sony (API 28) 上跑列表, 搜索, 下载, 发送与一小时 IDLE 待机, 数据记入 `docs/dev/p6-performance-baseline.md`. 由此改动两处: IMAP 分段抓取由 64 KiB 提高到 1 MiB, 50 MiB 附件从约 1100 次往返降到 69 次 (回环 10 MiB/s, 下载常驻内存仍约 2 MiB); 客户端搜索在命中 `limit` 条后即停止而不再扫遍全部候选. 待机显示插件约 32 MiB PSS, 每小时 3.5 s CPU 与 40 KB 流量, 且长时监听需要宿主的前台服务 (否则 Android 9 在 31 分钟时把宿主与插件当作缓存空进程杀掉)
- `优化` 发布体积 (邮件路线图 P6): R8 规则不再按原名保留整个 `jakarta.mail`, `jakarta.activation` 与 Angus Mail 命名空间, 只保留库按名加载的 20 个类 (IMAP / POP3 / SMTP 的 store 与 transport, 其 Provider, 流提供者, activation 注册表与五个数据内容处理器), 每条规则的依据资源记入 `app/proguard-rules.pro` 与 `docs/dev/p6-size.md`. release universal APK 由 2,254,035 字节降至 2,153,957 字节 (下载体积 1,507,931 -> 1,407,295), 去掉 2,319 个 DEX 方法与 162 个库类 (`MailHandler` 日志组件, GraalVM 特性, 图片处理器, SASL 客户端); 仪器测试套件在真机上对裁剪后的构建通过 (27 例)
- `依赖` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 与 Jakarta Activation API 2.1.4
- `依赖` 附加 GreenMail 2.1.13 用于 JVM 邮件核心测试 (仅测试范围)
- `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 附加 `mail-api.aar` (AutoJs6 模块 `plugin-api/mail-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为邮件 Binder 契约 (六个 AIDL 接口, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 并在 `locks/host-api-aars.lock` 中锁定哈希

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
