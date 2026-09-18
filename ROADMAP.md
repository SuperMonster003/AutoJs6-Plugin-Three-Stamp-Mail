# AutoJs6 Angus Mail 插件 Roadmap

本文是 `AutoJs6-Plugin-Angus-Mail` (为脚本提供发信 / 收信 / 搜索 / 附件 / 新邮件监听能力, 脚本侧全局对象 `mail`) 的可执行状态表.
以 2026-09-18 的宿主本地代码快照 (`AutoJs6 master@abf51bd51`, `VERSION_NAME=6.8.0`, `VERSION_BUILD=5281`),
Eclipse Angus Mail `2.0.5` (Jakarta Mail API `2.1.x`), 平台版本插件 `1.8.2` 为起点, 每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.

使用方式:

1. 每次会话开始时, 从 "阶段总览" 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 邮件服务商), 证据等级见附录 E.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(文档)` 文档 / d.ts / Ace / 离线文档四个关联仓库, `(测试)`, `(发布)`.
4. 涉及宿主公开契约或脚本 API 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 语言) 与本仓库 `.changelog`.
5. 附录 D 的 "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到 "固定决策".
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 已于 2026-09-18 (P0.1) 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 该文件的裁剪版即本仓库 `AGENTS.md`; 之后的工程约定以 `AGENTS.md` 为准, 本文件只记录 "改什么" 与证据.

---

## 1. 固定决策

以下决策 D1-D11 已由维护者于 2026-09-18 确认, 后续阶段不再重新讨论; D12-D24 为据此派生的技术决策, 进入对应阶段前可推翻 (推翻点见附录 D), 之后视同固定; D25-D32 是维护者于 2026-09-18 (第二次会话) 对附录 D 的 Q1-Q8 与附录 F 的拍板结果, 与 D1-D11 同级. D33-D34 为 P2.2 落地时派生的技术决策, 与 D12-D24 同级.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名 | 脚本全局对象 `mail` (别名 `$mail`, 与其它模块一致); 仓库 `AutoJs6-Plugin-Angus-Mail`; `rootProject.name=autojs6-plugin-angus-mail`; `applicationId=io.github.supermonster003.autojs6.plugin.angus.mail`; 插件 ID `angus-mail`, engine `mail`, variant `default`. engine 取 `mail` 而非 `angus-mail`, 是为了让宿主 `selectOrThrow(engine = "mail")` 在将来允许其它实现 (如 `android-mail` 分支或原生实现) 以不同 ID 接入同一 engine 家族, 形态与 `ocr` 家族一致. |
| D2 | 底层库选 Eclipse Angus Mail 2.0.x | `org.eclipse.angus:jakarta.mail:2.0.5` (API + 实现合一的 bundle) + `org.eclipse.angus:angus-activation:2.0.3` + `jakarta.activation:jakarta.activation-api:2.1.4`. 官方声明支持 Android (API 19+), 内建 XOAUTH2 (不依赖 Android 缺失的 SASL), 覆盖 IMAP (含 IDLE / SEARCH / UID / 部分抓取) / SMTP / POP3 与完整 MIME. P0 做 `minSdk 24` + R8 + `jakarta.activation` mailcap 处理器可行性 spike; 硬性失败时退回 `com.sun.mail:android-mail:1.6.8` (退路见附录 E.2). MailCore2 因 Android 构建链陈旧 (OpenSSL 1.0.2 / NDK 17) 且带来多 ABI 与 16 KB 对齐负担, 不采用. |
| D3 | 1.0.0 协议范围 IMAP + SMTP + POP3 | 收信 / 搜索 / 正文 / 附件 / 文件夹 / 标记 / 移动 / 删除走 IMAP, 发信走 SMTP. POP3 作为只读退化路径: 无文件夹 (只有 `INBOX`), 无服务器端搜索 (客户端按信头过滤且有上限), 无标记 / 移动, 无 IDLE (只能轮询, 以 UIDL 差分识别新邮件); 不支持的操作返回 `UNSUPPORTED_OPERATION`. |
| D4 | 认证: 密码 / 授权码 + 脚本自带 XOAUTH2 令牌 | 支持 `LOGIN` / `PLAIN` (普通密码与各家 "应用专用密码 / 授权码", 如 QQ / 163 授权码, Gmail App Password) 以及 `XOAUTH2` (脚本传入 `accessToken`, 或提供 `tokenProvider()` 回调; 令牌过期导致 `AUTH_FAILED` 时宿主调用回调取新令牌, 重开会话并重试该操作一次). 1.0.0 不内置 Google / Microsoft 的浏览器授权流程 (需注册并审核 OAuth 客户端); 该流程已排期为 P9 (D31), 落地前脚本自行提供令牌. |
| D5 | API 形态: 实例为主 + 全局便捷 | `mail.connect(options | alias)` 返回 `MailClient` 实例 (多账户并存, 持有连接, `close()`); 所有邮件操作都在实例上. 全局 `mail.send` / `mail.fetch` / `mail.search` / `mail.get` / `mail.watch` 等是面向 "默认账户" 的便捷层, 默认账户由 `mail.setDefault(...)` 指定, 或取插件设置页中标记为默认的已保存账户; 没有默认账户时抛 `NO_DEFAULT_ACCOUNT`. |
| D6 | 调用风格: 同步为主 + `*Async` 返回 Promise | 每个网络方法都有同步版 (阻塞调用线程, 宿主以 `runBlocking` 桥接, 与 `opencc` / `ocr` / `yolo` 一致) 与 `xxxAsync` 版 (返回 Promise, 经 `ScriptPromiseAdapter` 回到脚本线程). 监听只有事件形式 (EventEmitter), 无同步等待版. |
| D7 | 监听: 脚本生命周期内 IDLE + 轮询退化 | `client.watch(folder, options)` 返回 `MailWatch` (EventEmitter: `message` / `error` / `close` / `mode`). 服务器通告 IDLE 能力时用 IMAP IDLE 推送, 否则按 `pollInterval` 轮询; POP3 只能轮询. 脚本引擎退出即停止, 插件不启动前台服务, 也不做开机自启. 无脚本运行时的 "后台守望 + 触发脚本" 已排期为 P8 (D30, 附录 F.1). |
| D8 | 凭据: 脚本传入 + 插件可选保存账户 | 主路径: 脚本在 `mail.connect({...})` 中传入主机 / 用户 / 密码或令牌, 宿主与插件都不落盘, 凭据只在会话生命周期内驻留内存. 辅路径: 插件设置页 "账户" 以 Android Keystore AES-256-GCM 加密保存 (仿 Three-Stone-AI `AiCredentialStore`), 脚本用 `mail.connect('alias')` 引用, 密码不进脚本文件. 设置页为独立阶段 (P4). |
| D9 | 附件与大正文经 `ParcelFileDescriptor` | 下载: 宿主创建管道, 把写端交给插件, 插件流式写入; 宿主按 `ArchiveEntryMaterializer` 模式 (预检大小与空间, 临时文件 + 原子重命名, 中断时清理) 写到脚本给出的路径 (默认为脚本工作目录下 `mail-attachments/`). 上传: 宿主以只读 PFD 把附件文件交给插件. 正文超过内联阈值时同样走 PFD. 插件不申请任何存储权限. |
| D10 | 内置服务商预设表 + 自定义覆盖 | 插件内置 Gmail / Outlook.com / Microsoft 365 / QQ / 163 / 126 / iCloud / Yahoo / Sina / Aliyun 等预设 (IMAP / POP3 / SMTP 主机, 端口, SSL / STARTTLS, 认证提示); 脚本只传 `provider: 'qq'` + 账号密码即可; 任何字段可显式覆盖; 不做域名探测. 预设表以数据文件维护, 有快照测试, 见附录 C. |
| D11 | 正文形态: text + html 分开, 附件元数据懒加载 | `message.text` (纯文本, 无 `text/plain` 时从 HTML 降级提取), `message.html` (原始 HTML, 可能为 `null`), `message.attachments[]` 只含文件名 / MIME / 大小 / `contentId` / `inline`, 调用 `download()` 才传输. 列表类方法 (`fetch` / `search`) 默认只拉 envelope + flags + size, 正文经 `get` / `fetchBody` 按需拉取. |
| D12 | 路线图与仓库 | 本文件位于 `D:/idea-projects/AutoJs6-Plugin-Angus-Mail/ROADMAP.md`. 本次会话只落盘路线图; 骨架, `git init` 与首批提交在 P0.1. |
| D13 | 邮件 I/O 全部在插件进程 | Jakarta Mail 只装入插件 APK (宿主在 size roadmap P1 已移除 JavaMail jar, 不回退). 宿主只保留契约模块, Binder 客户端, 脚本 API 与文件落盘; 插件未安装 / 禁用时宿主不具备任何发收邮件能力, `app.sendEmail` (Intent 交给外部邮件应用) 保持原样并在文档中互相引用. |
| D14 | 契约信封: `Bundle` + 字符串常量 + JSON 文档 | 请求 / 响应 / 事件为 JSON 字符串放在 `Bundle` 固定 key 下 (与 MCP 家族 D10 同形), 因为邮件对象天然是 JSON 且脚本侧直接消费; 秘密字段 (`password` / `accessToken`) 不进 JSON, 放独立 `Bundle` key, 保证任何 JSON 日志都不含凭据; 大负载走 PFD. 契约版本 `MailContract.CONTRACT_VERSION` + `MIN / MAX` 区间协商, 不做异常嗅探. (Q1, 已拍板) |
| D15 | 会话模型 | `IMailPlugin.openSession(account, callback) -> IMailSession`; 一个会话 = 一个账户, 内部按需建立 IMAP / POP3 `Store` 与 SMTP `Transport`; 会话内操作在插件侧单线程串行 (Jakarta `Store` / `Folder` 不是线程安全的); `watch` 使用独立的第二条 `Store` 连接, 使 IDLE 不阻塞同会话的其它操作. 宿主每个 `MailClient` 持有一条专用绑定租约 (`AidlPluginHost.callWithDedicatedBindingLease`), 脚本引擎退出时关闭全部会话. 插件侧会话空闲超过 `SESSION_IDLE_TIMEOUT` (默认 10 分钟) 自动断开底层连接但保留会话对象, 下次调用透明重连; 插件进程死亡时宿主收到 death 通知, `MailClient` 用内存中的凭据懒重开会话, 正在进行的操作以 `SESSION_CLOSED` 失败. |
| D16 | 操作分派: 单一异步 `call` + op 表 | `IMailSession.call(request, descriptors, callback)` 返回 `requestId`, 结果经 `oneway` 回调返回, 宿主以 `suspendCancellableCoroutine` 等待并在取消 / 超时时调用 `cancel(requestId)`; 不占用 Binder 线程等待网络. 操作以 `op` 字符串区分 (附录 B.2), 新增 op 只追加表项并经 capability 协商, AIDL 方法保持稳定. |
| D17 | 监听事件协议 | `IMailWatchCallback.onEvent(generation, seq, event)` 为 `oneway`; `generation` 由宿主在 `watch` 时分配, 插件回传, 用于丢弃陈旧回调; `seq` 单调递增. 事件类型 `message` (envelope JSON, 可选携带正文) / `error` / `closed` / `mode` (`idle` <-> `poll` 切换) / `resync` (插件侧待发队列超过上限或重连后无法确定增量, 提示脚本自行 `fetch`). IDLE 每 24 分钟主动重发 (RFC 2177 建议 29 分钟内), 连接断开按 1 s 起指数退避至 5 分钟重连, 重连后按上次已知最大 UID 补拉. |
| D18 | 错误模型 | 契约固定错误码词汇 (附录 B.4), 插件把 Jakarta 异常映射为错误码 + 已脱敏的 `message` + `details` (服务器响应文本去掉凭据); 宿主把 Binder / 发现 / 版本失败映射为 `PLUGIN_UNAVAILABLE` 家族; 脚本侧抛 `MailError` (可 `instanceof`, 有 `code` / `message` / `details` / `retryable`). |
| D19 | 插件中心默认启用 | 遵循 `PluginDefaultEnabledPolicy` 现状 (除 YOLO 外默认启用), 不为本插件新增例外; 授权态仍由 `PluginTrustManager` 把关. (Q2, 已拍板) |
| D20 | HTML 转纯文本自研最小实现 | 块级元素换行, `<br>` / `<p>` / `<li>` / `<tr>` 处理, 去 `<script>` / `<style>`, HTML entity 解码; 不引入 jsoup. |
| D21 | 字符集与非标准 MIME 的宽容策略 | Session 属性 `mail.mime.decodefilename=true`, `mail.mime.encodefilename=true`, `mail.mime.decodetext.strict=false`, `mail.mime.parameters.strict=false`, `mail.mime.charset=UTF-8`, `mail.mime.address.strict=false`; 未声明 charset 的正文对国内服务商默认按 GB18030 尝试 (可配置); 附件文件名去掉路径分隔符与控制字符后再落盘. |
| D22 | 测试基础设施 | 插件仓库含纯 JVM 模块 `:mail-core` (不依赖 Android, 装载 Jakarta Mail 逻辑), JVM 测试用 GreenMail (`com.icegreen:greenmail:2.1.x`, 与 Jakarta Mail 2.1 对齐) 起本地 IMAP / SMTP / POP3; instrumentation 覆盖 Binder; 真实服务商矩阵用维护者提供的测试账户 (本地 `mail-test-accounts.properties`, git 忽略). |
| D23 | 上限常量 | 见附录 B.5, 写入 `MailContract`. |
| D24 | 插件自身权限集 | 1.0.0 骨架: `INTERNET`, `ACCESS_NETWORK_STATE`, `org.autojs.permission.PLUGIN`; P4 设置页落地时追加 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (D27, 只在用户点击引导按钮时发起系统请求); 不申请存储权限; 前台服务 / 通知 / 开机自启权限只在 P8 后台守望立项时追加. `ManifestContractTest` 断言权限集合精确, 追加时同步修改. |
| D25 | `tls.trustAll` 纳入 1.0.0 | 默认 `false`, 只对单个账户生效: `mail.<protocol>.ssl.trust=*` 且关闭主机名校验 (`ssl.checkserveridentity=false`); `test()` 结果与 `client.account` 标记 `insecure: true` (明文 `tls: 'none'` 同样标记); 不提供全局开关; 文档明确风险. 已在 `:mail-core` 落地 (`MailAccount.trustAll` / `MailAccount.insecure`, `MailSessionPropertiesTest.trustAllOnlyRelaxesEncryptedEndpoints`). (Q3) |
| D26 | 日期表示为 JS `Date` | 脚本返回值统一为 JS `Date` (与 `files` / `device` 等模块一致); 参数接受 `Date` / 毫秒时间戳 / ISO 字符串; 契约 JSON 内部统一用 UTC 毫秒时间戳 (`Long`), `Date` 的构造与解析只在宿主 `MailObjects` 完成, 插件不处理时区. (Q4) |
| D27 | 设置页提供 "忽略电池优化" 引导按钮 | Manifest 追加 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; 按钮显示 `PowerManager.isIgnoringBatteryOptimizations` 的当前状态, 点击后解释用途并以 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 转到系统对话框; 不在启动时弹窗, 不作为 `watch` (P5) 或后台守望 (P8) 的前置条件; 侧载插件不受应用商店政策限制. 设置页因此提前为 P4. (Q5) |
| D28 | 脱敏协议日志 `options.debug` | `mail.connect({debug: true})` 时插件把脱敏后的协议摘要 (命令名 + 响应码 + 前 200 字符, 去掉 LOGIN / AUTH / XOAUTH2 行与 Base64 载荷) 经 `IMailCallCallback.onProgress` 回传, 宿主逐行写到 `console.verbose`; Jakarta `mail.debug` / `Session.setDebug` 永不对脚本开放, 实现方式在 P2.1 决定. (Q7) |
| D29 | 1.0.0 只做发行历史页 | 设置页提供 "发行历史" 入口 (按 locale 读取 `assets/doc/CHANGELOG-<lang>.md`, 缺失回退英语); 不做插件内更新检查, 更新跟随宿主插件中心. (Q6) |
| D30 | 后台守望与新邮件触发脚本纳入排期 | 作为 P8 展开 (原附录 F.1), 目标版本 1.1.0, 前置 P7; 契约以 `CONTRACT_VERSION = 2` 追加方法, 旧版任一方缺失时功能不可见. (Q8 / F.1) |
| D31 | 内置 OAuth2 浏览器授权流程纳入排期 | 作为 P9 展开 (原附录 F.2), 目标版本 1.2.0, 前置 P4 设置页与 P7; Custom Tabs + PKCE, 刷新令牌经 `AccountStore` 加密保存, 会话自动续期, 不需要 AIDL 变化; 维护者需注册 Google / Microsoft OAuth 客户端. (F.2) |
| D32 | 不做非标准协议 | Microsoft Graph / EWS / Gmail REST / ActiveSync 不排期; 附录 F.3 只保留 "另一个 engine `mail` 实现" 的接入点说明. (F.3) |
| D33 | `saveToSent: true` 不对自动保存的服务商追加副本 | 预设 `autoSavesSent = true` 时无论 `saveToSent` 取何值都不 APPEND (QQ 以 `NO Mail has saved by smtp!` 拒绝, Gmail 会出现重复); `SendResult.sentCopy` 以 `server` / `appended` / `failed` / `none` 说明副本去向, 追加失败只进入 `saveError` 而不改变发送成功. (P2.2 派生, 2026-09-18 真机核实) |
| D34 | 头部一律 RFC 2047 编码 | 会话属性去掉 `mail.mime.allowutf8`: 显示名, 主题, 文件名与自定义头始终编码, 不依赖每一跳的 SMTPUTF8 / UTF8=ACCEPT; 非 ASCII 邮箱地址 (EAI) 不在 1.0.0 范围. (P2.2 派生) |

阶段顺序调整 (2026-09-18 第二次会话): 设置页与账户存储提前为 P4 (D27 的引导按钮与 P9 都依赖设置页, 且 Q5 已拍板, 无需再等 Doze 矩阵), 新邮件监听顺延为 P5; P8 / P9 由附录 F 展开; 各阶段证据文件名随之为 `p4-settings-evidence.md` / `p5-watch-evidence.md`.

由 D13 / D14 派生的硬约束:

- 插件不复制宿主 `PluginInfo` 或 AIDL 伪实现; `mail-api` 与 `common-plugin-api` 的 AAR 复制到本仓库 `libs/` 并以 SHA-256 锁定 (`locks/host-api-aars.lock`, 格式同 MCP Server 插件).
- 已发布 AIDL 演进只在末尾追加方法并通过 `CONTRACT_VERSION` 协商; op 表追加不改 AIDL.
- 凭据永不进入日志, JSON 文档, 异常消息, `toString()`, 崩溃报告或 changelog; 插件与宿主的测试夹具只用假凭据.

---

## 2. 范围与非目标

范围内:

- 本仓库: 插件 APK (Binder 服务, `:mail-core` JVM 模块, 服务商预设, 会话 / 监听实现, 账户存储与设置页, 10 语言资源, README / changelog 生成, JVM 与 instrumentation 测试, CI).
- 宿主 `D:/idea-projects/AutoJs6`: `plugin-api/mail-api` 契约模块; `core/plugin/mail/` 宿主客户端, 附件落盘, 监听桥; `runtime/api/augment/mail/` 脚本 API `mail`; 插件中心注册; `docs/dev/mail-plugin-protocol-v1.md`; changelog.
- 关联仓库: `AutoJs6-Documentation` (`api/mail.md` 与类型页), `AutoJs6-TypeScript-Declarations` (`aj6-int-mail.d.ts`), `AutoJs6-Plugin-Ace-Editor` (内置声明再生成), `AutoJs6-Plugin-Offline-Docs` (离线文档同步).

范围内但在 1.0.0 之后交付 (2026-09-18 拍板):

- 无脚本运行时的后台守望与 "新邮件触发脚本" (P8, D30, 目标 1.1.0).
- 内置 OAuth2 浏览器授权流程与令牌刷新存储 (P9, D31, 目标 1.2.0).

非目标 (本 Roadmap 不处理):

- 邮件服务器 (SMTP / IMAP 服务端), 邮件列表 / 群发营销, 退信解析, DKIM / S/MIME / PGP 签名与加密.
- Exchange ActiveSync / EWS / Microsoft Graph / Gmail REST API 等非标准协议 (D32, 不排期; 附录 F.3 只记录接入点).
- 通讯录, 日历 (CalDAV / iCalendar 附件解析).
- 宿主内置邮件客户端 UI (收件箱界面); 宿主只提供脚本 API 与插件中心入口.
- 对 `app/src/main/java/com/stardust/**` 兼容包的任何改动.

---

## 3. 现状诊断

以下是 2026-09-18 探查得到的事实, 是各阶段条目的直接依据. 行号以宿主快照 `abf51bd51` 为准.

### 3.1 可直接复用的宿主能力

| 事实 | 锚点 |
| --- | --- |
| 单能力插件的完整链路模板 (`opencc`): 契约模块 `plugin-api/opencc-api` (`IOpenccPlugin.aidl` 首方法 `PluginInfo getInfo()`, `OpenccPluginActions` / `OpenccPluginIds` / `OpenccPluginContract` + `OpenccPluginCapabilityKeys.CONTRACT_VERSION`), `build.gradle.kts` 的 `buildFeatures { aidl = true }` + `api(project(":plugin-api:common-plugin-api"))` | `plugin-api/opencc-api/src/main/aidl/.../IOpenccPlugin.aidl:1-15`, `OpenccPluginContract.kt:4-15`, `plugin-api/opencc-api/build.gradle.kts:22-29` |
| 宿主客户端模板 (94 行): `object OpenccPluginHost` 持有 `AidlPluginHost(action, tag, moduleLabel, proxyFactory, infoReader)`, 透出 `discover / probe / queryServiceCount`, 每次调用 `host.selectOrThrow(context, engine = ...)` + `host.call(context, target) { proxy -> ... }`, 契约版本从 `capabilities.getInt(CONTRACT_VERSION)` 读取 | `core/plugin/text/OpenccPluginHost.kt:13-33, 87-93` |
| 脚本全局对象的原生定义方式: `object OpenCC : Augmentable(), Invokable`, `keys = listOf(AugmentableKey("opencc"))`, `selfAssignmentFunctions`, `@RhinoSingletonFunctionInterface` / `@RhinoRuntimeFunctionInterface` + `@RhinoFunctionBody` 双层; 同步桥接为 `runBlocking { ... }`; 注册于 `ScriptRuntime.kt` (`OpenCC.augmentWithRuntime(target, this)`); `assets/modules/` 没有插件全局对象的 JS 包装层 | `runtime/api/augment/opencc/OpenCC.kt:18-22, 58-80, 344-354`, `runtime/ScriptRuntime.kt:1031-1039` |
| 事件对象: `AsEmitter` 标记接口 + `Events.__asEmitter__` 混入; 插件回调到脚本事件的成熟范例 `AiStream : EventEmitter`, 全部回调经 `ScriptAsyncDispatcher.dispatchValues` 单跳回到脚本线程 (含溢出保护) | `augment/AsEmitter.kt:3`, `Augmentable.kt:402-404`, `augment/ai/AiStream.kt:16-123`, `augment/ScriptAsyncDispatcher.kt:23-241, 436` |
| Promise 互操作: `ScriptPromiseAdapter.toJsPromise(scriptRuntime)` | `augment/PromiseInterop.kt:7-16` |
| 长连接与死亡处理: `AidlPluginHost.callWithDedicatedBindingLease` (`LeasedCall(value, bindingLease)`), `ServiceBindingLease` 只解绑一次, `DeathRecipient` / `onBindingDied` / `onNullBinding` 分别失败, 60 s 绑定超时; 会话保持租约的调用方范例 `McpServerPluginHost` / `McpServerSessionController` | `core/plugin/AidlPluginHost.kt:135-156, 446-601`, `core/plugin/ServiceBindingLease.kt:15-40`, `core/plugin/mcp/McpServerSessionController.kt:153, 188` |
| `oneway` 回调 AIDL 先例 (20 个 `I*Callback.aidl`); 最贴近监听需求的是 `IMcpServerCallback { onStatus(Bundle); onEvent(Bundle) }` + `IMcpServerSession { getStatus; updateConfig; stop; close }`; 带序号防陈旧的 `IJavaExecutionCallback(generation, ...)` | `plugin-api/mcp-server-api/src/main/aidl/.../IMcpServerCallback.aidl:9-16`, `IMcpServerSession.aidl:9-25`, `AutoJs6-Plugin-Java-Runtime/.../IJavaExecutionCallback.aidl` |
| 大负载: 插件输出流式落盘的模板 `ArchiveEntryMaterializer` (大小 / 空间预检, 临时文件, 中断检查, 声明大小校验); 宿主向插件提供只读 PFD (`MediainfoPluginHost`); 内存到 PFD 管道 (`BarcodePluginHost.createPipe`); PFD 数组所有权纪律 (`AndroidAiProviderSessionDescriptors`) | `core/plugin/explorer/archive/ArchiveEntryMaterializer.kt:40-85`, `core/plugin/mediainfo/MediainfoPluginHost.kt:157-159`, `core/plugin/barcode/BarcodePluginHost.kt:393-412`, `core/plugin/ai/AndroidAiProviderSessionDescriptors.kt:14-262` |
| 插件中心注册四处: `InstalledPluginRepository.queryDeclaredPluginServices` 的 `specs` **与** `discoverPackage` 分支, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE`, `PluginCenterFragment.probeAidlPluginService`; 默认启用策略 `PluginDefaultEnabledPolicy` (仅 YOLO 例外) | `core/plugin/center/InstalledPluginRepository.kt:153-223`, `PluginCenterViewModel.kt:1033-1056`, `PluginCenterFragment.kt:985-1015`, `PluginDefaultEnabledPolicy.kt` |
| 缺失 / 未授权 / 未启用 / 版本过低的用户可见错误已由 `AidlPluginHost.buildSelectionFailure` 统一产生 (`IllegalStateException` + 5 个既有字符串), `moduleLabel` 决定文案中的模块名; UI 分类 `PluginErrorMapper` | `core/plugin/AidlPluginHost.kt:270-337, 360-372`, `res/values/strings.xml:421, 432, 460, 461, 572`, `center/PluginErrorMapper.kt:8-57` |
| Gradle 与 Manifest 接线: `settings.gradle.kts` 的 `pluginApi` 列表与目录映射; `app/build.gradle.kts` 的 `implementation(project(":plugin-api:xxx-api"))` 块; `<queries>` 中按 action 声明 (非必需, 宿主已有 `QUERY_ALL_PACKAGES`, 但新能力均补充) | `settings.gradle.kts:118-183`, `app/build.gradle.kts:655-734`, `AndroidManifest.xml:37-116, 199-201` |
| 新插件家族接入的提交模板: `8e25c22e5` (屏幕取色器), `b07a09890` (Bun 运行时), MCP 的 `b63cca493` / `423678165` | `git show <hash> --stat` |
| 宿主构建事实: `minSdk 24`, `compileSdk / targetSdk 37`, core library desugaring 开启, `core/plugin` 全面使用协程 (`suspend` API + Rhino 层 `runBlocking`) | `version.properties`, `app/build.gradle.kts:839, 1091` |
| 官方插件只读设置快照 (主题色 / 夜间模式 / 语言), 供插件设置页跟随宿主外观; 参考实现 OpenCC `AutoJs6HostSettings.kt` | `docs/dev/official-plugin-settings-contract-v1.md:36-78` |
| 凭据加密存储的成熟范例: Three-Stone-AI `AiCredentialStore` (接口) + `AndroidKeystoreCredentialCipher` (AES-256-GCM, 不可导出主密钥, AAD 绑定) + `FileCredentialRecordStorage` (进程间文件锁) + `[REDACTED]` 的 `toString`; 设置页以 `CharArray` 读取输入并交出所有权 | `AutoJs6-Plugin-Three-Stone-AI/.../credential/AiCredentialStore.kt:17-63`, `AndroidCredentialInfrastructure.kt:22`, `OnlineAiSettingsActivity.kt:692-779` |
| 插件仓库基础骨架与 AAR 锁: OpenCC (build-logic 约定插件, `resValue` 六键, `appendDigestToReleasedFiles`, Manifest 组件集, Compose UI 套件), MCP Server (`locks/host-api-aars.lock` 与配置期 SHA-256 校验, `THIRD_PARTY_NOTICES.md`, `docs/dev/p<阶段>-<主题>.md` 命名, ROADMAP 会话记录) | `AutoJs6-Plugin-OpenCC/app/build.gradle.kts:41-46, 105-106, 122`, `AutoJs6-Plugin-MCP-Server/app/build.gradle.kts:63-102` |

### 3.2 缺口 (需要新建或修改)

| 缺口 | 处理阶段 |
| --- | --- |
| 宿主没有任何 SMTP / IMAP / POP3 能力; `app.sendEmail` 只是 `ACTION_SEND` Intent 交给外部邮件应用 (`App.kt:430-461`), `IntentUtils.sendMailTo` 只是 `mailto:`; JavaMail jar 已在 `155137c98` 移除 | P2 (插件) / D13 |
| 没有 `plugin-api/mail-api` 契约, 没有 `core/plugin/mail/`, 没有 `augment/mail/` | P1 / P3 |
| 全局名 `mail` / `$mail` 在宿主 augment, d.ts (`aj6-int-init.d.ts`) 与文档 `api/` 中均未被占用 | P3 / P7 |
| 现有回调 AIDL 均为 "一次请求多次回调后终止" (AI 流, 编译), 没有 "长期订阅 + 序号 + 重同步" 的监听契约 | P1 (契约) / P5 (实现) |
| 现有插件没有 "宿主创建管道写端交给插件写入" 的下载形态 (Archive Manager 是插件返回读端); 需要新写 `MailAttachmentSink` | P1.2 / P3 |
| 没有纯 JVM 的邮件核心模块与 GreenMail 测试先例 | P0.2 / P2 |
| 插件生态没有 "账户 / 密码 / 别名" 的设置页先例 (Three-Stone-AI 的 profile 最接近) | P4 |
| 没有 `docs/dev/mail-*.md`, 文档 / d.ts / Ace / 离线文档均无 `mail` | P1.4 / P7 |

### 3.3 外部事实

| 事实 | 依据 |
| --- | --- |
| Angus Mail 是 JavaMail / Jakarta Mail 的官方后继, 2.0.0 起包名 `org.eclipse.angus.mail.*`; 最新稳定版 2.0.5 (2025-09), 2.1.0-M1 为里程碑; 推荐依赖 `jakarta.mail-api 2.1.x` + `angus-mail 2.0.x`, 或合一 bundle `org.eclipse.angus:jakarta.mail` | `eclipse-ee4j.github.io/angus-mail`, Maven Central 目录 |
| Angus Mail 官方 Android 页: 标准发行版可在 Android 运行 (API 19+); Android 不支持 SASL, `mail.<protocol>.sasl.*` 无效; 内建 XOAUTH2 不需要 SASL; Gradle 需 `packagingOptions { pickFirst 'META-INF/LICENSE.md'; pickFirst 'META-INF/NOTICE.md' }` | `eclipse-ee4j.github.io/angus-mail/Android` |
| XOAUTH2 用法: `mail.imap.auth.mechanisms=XOAUTH2` (SMTP 同名替换), 令牌作为 `connect()` 的密码传入, 不要自行 base64; Gmail 需 `mail.imap.ssl.enable=true`; Outlook.com POP3 需 `mail.pop3.auth.xoauth2.two.line.authentication.format=true` | `eclipse-ee4j.github.io/angus-mail/OAuth2` |
| Gmail 已关闭 "不够安全的应用" 的普通密码登录: 只能 App Password (需两步验证) 或 OAuth 2.0 (`https://mail.google.com/` scope, 公开应用需审核) | Google Workspace Admin Help 14114704, developers.google.com/workspace/gmail/imap |
| Outlook.com / Hotmail 个人账户: IMAP 的基本认证已禁用, SMTP 仍部分可用, 第三方客户端必须 OAuth (XOAUTH2); Microsoft 365 的 SMTP AUTH 基本认证 2026-12 起默认禁用, 新租户不可用 | Microsoft Support / Q&A, Exchange Team Blog 2026-01-27 更新 |
| 国内服务商 (QQ / 163 / 126 / Sina / Aliyun) 普遍要求 "授权码" 而非登录密码, 开启 IMAP / POP3 需在网页设置中手动打开; QQ 与 163 对非 ASCII 的 IMAP SEARCH 支持不稳定; 163 要求客户端发送 `ID` 命令 (`IMAPStore.id(...)`) 否则返回 `Unsafe Login` | 各服务商帮助中心 (预设表落地时逐项核实并记录到附录 C) |
| MailCore2 上游 Android 构建链停留在 OpenSSL 1.0.2u / NDK 17 / 预 AndroidX 示例, 无版本化制品; 活跃分支 (Readdle / Canary / Edison) 面向 iOS / macOS | `github.com/MailCore/mailcore2/build-android`, JitPack 列表 |
| GreenMail 2.1.13 (2026-08) 支持 SMTP / IMAP / POP3 本地服务器, 2.1.x 对齐 Jakarta Mail 2.1 / Angus 2.0.5; P0.2 实测: 通告并推送 IDLE (`GreenMailRoundTripTest.imapIdleDeliversNewMessageEvents`), 支持 XOAUTH2, 明文端口不提供 STARTTLS (只能验证 "拒绝降级", 正向 STARTTLS 留给真实服务商); standalone 版可作为设备端测试的本机服务器 (见 `MailCoreDeviceTest` KDoc) | `github.com/greenmail-mail-test/greenmail/releases`, `mail-core` 测试 |
| 平台版本插件 `1.8.2` 已发布到 plugins.gradle.org (2026-09-18 以 `curl` 读取 maven-metadata 核实), 本仓库 `settings.gradle.kts` 使用 1.8.2 并完成构建 (Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, JDK 21); OpenCC 用 1.8.1, MCP 用 1.8.0 | `AutoJs6-Gradle-Platform-Versions/version.properties`, plugins.gradle.org maven-metadata |

---

## 4. 目标架构

### 4.1 数据流

```
脚本 (Rhino)
    mail.connect({...}) / mail.connect('alias')  ->  MailClient  ->  client.fetch(...) / client.send(...) / client.watch(...)
    |  同步: runBlocking            异步: ScriptPromiseAdapter            事件: MailWatch (EventEmitter) <- ScriptAsyncDispatcher
    v
宿主进程 (AutoJs6)  org.autojs.autojs.core.plugin.mail
    MailPluginHost (AidlPluginHost: 发现 / 授权 / 版本 / 专用绑定租约)
    MailSessionClient (JSON 编解码, requestId, 超时, cancel, 死亡后懒重开, XOAUTH2 令牌回调重试)
    MailAttachmentSink (PFD 管道写端 -> 临时文件 -> 原子重命名)   MailAttachmentSource (文件 -> 只读 PFD)
    MailWatchBridge (IMailWatchCallback -> generation / seq 校验 -> EventEmitter)
    |  AIDL  IMailPlugin.openSession(account, callback) -> IMailSession.call(request, descriptors, callback) / watch(options, callback)
    v
插件进程 (io.github.supermonster003.autojs6.plugin.angus.mail)
    AngusMailPluginService (Binder, CallerGuard: 宿主包名 / 签名 / UID)
    RequestRouter (op 表, Limits, 错误映射)  ->  :mail-core
        SessionFactory (jakarta.mail.Session 属性, 预设, TLS, 认证机制)
        ImapMailbox / Pop3Mailbox / SmtpSender / MessageMapper / BodyExtractor / AttachmentStreamer / SearchQueryCompiler
        IdleWatcher / PollWatcher (独立连接, 退避重连, UID 补拉)
    AccountStore (Keystore AES-GCM, 别名 -> 账户)   ProviderPresets (数据文件)
    |  IMAP / SMTP / POP3 over TLS
    v
邮件服务器
```

### 4.2 目标包结构

插件 (`io.github.supermonster003.autojs6.plugin.angus.mail`):

```
:mail-core (纯 JVM, 无 Android 依赖)
    account/    MailAccount, MailAccountOptions, AuthMethod, TlsMode, ProviderPreset, ProviderPresets (JSON 数据 + 解析)
    session/    MailSessionFactory, MailSessionProperties, ImapMailbox, Pop3Mailbox, SmtpSender, ConnectionGuard (超时 / 重连)
    message/    MessageMapper (MimeMessage -> JSON envelope / body), BodyExtractor, HtmlToText, AttachmentStreamer, FilenameSanitizer, CharsetFallback
    query/      SearchQuery (JSON) -> SearchTerm 编译器, FlagMapper, UidRange
    watch/      Watcher 接口, IdleWatcher, PollWatcher (IMAP UID / POP3 UIDL 差分), Backoff
    error/      MailErrorCode, MailException, ExceptionMapper (Jakarta 异常 -> 错误码, 脱敏)
    json/       kotlinx-serialization 模型与编解码
:app
    service/    AngusMailPluginService (IMailPlugin.Stub), AngusMailPluginInfoService (INFO), WakeActivity
    binder/     MailSessionBinder (IMailSession.Stub), MailWatchBinder, RequestRouter (op 表), Limits, CallerGuard, DescriptorIo (PFD 读写)
    store/      AccountStore (Keystore AES-GCM), AccountRecordCodec, AccountAlias
    ui/         AppSettingsActivity, AccountsActivity (列表 / 新增 / 编辑 / 测试连接 / 设为默认), AboutActivity, ReleaseHistoryActivity, AutoJs6HostSettings, ui/* Compose 套件 (复制自 OpenCC)
```

宿主新增 (`org.autojs.autojs.core.plugin.mail` 与 `org.autojs.autojs.runtime.api.augment.mail`):

```
core/plugin/mail/
    MailPluginHost               (AidlPluginHost 封装; discover / probe / queryServiceCount; openSession 返回带租约的 MailSessionClient)
    MailSessionClient            (call 封装, requestId, 超时与取消, 死亡后懒重开, 令牌刷新重试; 纯逻辑部分可 JVM 测试)
    MailWatchBridge              (回调校验 generation / seq, 事件投递到 MailWatch)
    MailAttachmentSink           (PFD 管道写端交给插件, 宿主端流式落盘, 大小 / 空间预检, 原子重命名)
    MailAttachmentSource         (脚本给出的附件路径 -> 只读 PFD, 带大小与数量上限)
    MailJson                     (请求 / 响应 / 事件 JSON 的宿主侧模型, 纯 Kotlin)
    MailErrorMapper              (错误码 -> MailError 参数; 发现 / 绑定失败 -> PLUGIN_UNAVAILABLE 家族)
runtime/api/augment/mail/
    Mail                         (Augmentable, keys = ["mail"], 全局便捷层, providers / accounts / MailError / setDefault)
    MailClient                   (脚本可见实例; 同步 + Async 方法对)
    MailWatch                    (EventEmitter; stop / mode / isActive)
    MailObjects                  (MailMessage / MailAttachment / MailAddress / MailFolder 的 JS 对象构造与 download() 绑定)
```

宿主契约 (`plugin-api/mail-api`, 包 `org.autojs.plugin.mail.api`):

```
IMailPlugin.aidl            PluginInfo getInfo(); Bundle getCapabilities(); IMailSession openSession(in Bundle account, IMailSessionCallback callback); Bundle listProviders(); Bundle listSavedAccounts();
IMailSession.aidl           Bundle getStatus(); String call(in Bundle request, in ParcelFileDescriptor[] descriptors, IMailCallCallback callback); void cancel(String requestId); IMailWatch watch(in Bundle options, IMailWatchCallback callback); void close();
IMailCallCallback.aidl      oneway: void onProgress(in Bundle progress); void onResult(in Bundle response);
IMailWatch.aidl             Bundle getStatus(); void stop();
IMailWatchCallback.aidl     oneway: void onEvent(long generation, long seq, in Bundle event);
IMailSessionCallback.aidl   oneway: void onStatus(in Bundle status);
MailContract.kt             CONTRACT_VERSION / MIN / MAX, KEY_* 常量, OP_* 常量, 上限常量 (附录 B.5)
MailActions.kt              SERVICE_ACTION = "org.autojs.plugin.MAIL", CATEGORY = "mail"
MailIds.kt                  PLUGIN_ID = "angus-mail", ENGINE = "mail", VARIANT = "default", DEFAULT_PACKAGE_NAME, REQUIRED_HOST_VERSION_CODE
MailCapabilityKeys.kt       REQUIRES_HOST_VERSION, CONTRACT_VERSION, PROTOCOLS, AUTH_MECHANISMS, FEATURES (idle / saved-accounts / append), PROVIDERS_VERSION
MailErrorCodes.kt           附录 B.4 的错误码字符串常量
```

设计原则:

1. 单一事实来源: op 表 (名称 / 参数 / 上限 / 需要的描述符方向 / 支持的协议) 只存在于契约模块的 `MailContract` 与插件的 `RequestRouter` 注册表; 文档的方法表与快照测试从它派生.
2. 纯 JVM 可测: `:mail-core` 全部逻辑, 宿主的 `MailJson` / `MailErrorMapper` / `MailSessionClient` 状态机都不依赖 Android / Binder, 用 JUnit4 + GreenMail 直接测试.
3. 失败闭合: 任何上限超出返回明确错误码; 凭据只在会话内存; 插件死亡时正在进行的操作显式失败而不是挂起; `watch` 无法保证增量时发 `resync` 而不是静默丢事件.
4. 宿主改动最小且可退化: 插件未安装 / 未启用 / 不兼容 / 调用失败四态由 `AidlPluginHost` 既有文案区分; 插件禁用或卸载后宿主不保留任何邮件能力.

---

## 5. 阶段总览

| 阶段 | 目标 | 主要落点 | 前置 |
| --- | --- | --- | --- |
| P0 | 仓库骨架 + Angus Mail 可行性 spike + 决策点 | 插件 | 无 |
| P1 | 宿主契约模块, 宿主客户端, 插件中心注册, 协议文档 | 宿主 | P0 骨架 (可并行) |
| P2 | 插件核心: 账户 / 预设 / 会话 / SMTP 发信 / IMAP 收信搜索正文附件标记文件夹 / POP3 退化 / Binder 路由 | 插件 | P0 决策点; P1 契约 |
| P3 | 脚本 API `mail` (连接 / 发信 / 收信 / 搜索 / 正文 / 附件 / 标记 / 移动 / 文件夹, 同步 + Async, MailError) | 宿主 | P1, P2 |
| P4 | 插件设置页与账户存储, 别名连接, 电池优化引导 (D27), 发行历史 (D29), 外观跟随 | 插件 (+ 宿主小) | P2 |
| P5 | 新邮件监听: IDLE / 轮询 / POP3 UIDL, 重连与补拉, 事件桥, Doze 矩阵 | 插件 + 宿主 | P2, P3 |
| P6 | 健壮性, 安全, 兼容矩阵, 性能, 体积 | 全部 | P3, P4, P5 |
| P7 | 文档, d.ts, Ace, 离线文档, README, changelog, 1.0.0 发布 gate | 文档 + 发布 | P6 |
| P8 | 后台守望与新邮件触发脚本 (D30, 目标 1.1.0) | 插件 + 宿主 | P7 |
| P9 | 内置 OAuth2 浏览器授权流程 (D31, 目标 1.2.0) | 插件 (+ 宿主小) | P4, P7 |

建议会话切分: P0 一次会话 (实际两次, 见会话记录); P1 一到两次 (契约 + 客户端为一次, 注册 + 文档为一次); P2 两到三次 (会话与发信; 收信搜索正文附件; 标记文件夹与 POP3 与路由); P3 两次 (连接 / 发信 / 收信; 附件 / 标记 / 文件夹 / Async); P4 一次; P5 一到两次; P6 一到两次; P7 一次; P8 两次 (插件前台服务; 宿主触发器与 UI); P9 一到两次 (授权流程与令牌存储; 服务商核实).

---

## P0: 仓库骨架与可行性 spike

目标: 让 `AutoJs6-Plugin-Angus-Mail` 成为一个可构建, 可安装, 能在真机上用 Angus Mail 经 SMTP 发出一封邮件并经 IMAP 列出收件箱的最小 APK, 并在阶段末决定 D2 是否成立.

### P0.1 仓库骨架

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 第 2 节确定标识并全仓库一致: `{PROJECT_NAME}=AutoJs6-Plugin-Angus-Mail`, `{ROOT_PROJECT_NAME}=autojs6-plugin-angus-mail`, `{APP_NAME}=Angus Mail`, `{APPLICATION_ID}=io.github.supermonster003.autojs6.plugin.angus.mail`, `{PLUGIN_ID}=angus-mail`, `{PLUGIN_ENGINE}=mail`, `{PLUGIN_VARIANT}=default`, `{PLUGIN_SERVICE}=AngusMailPluginService`, `{CAPABILITY_API}=mail-api`, `{SERVICE_ACTION}=org.autojs.plugin.MAIL`, `{SERVICE_CATEGORY}=mail`, `{REQUIRES_HOST_VERSION}=5282` (P1.4 回填: 交付 `mail-api` 契约模块的宿主构建, 宿主提交 34d2c8fcd), `{PLATFORM_VERSIONS_PLUGIN_VERSION}=1.8.2` (先确认 plugins.gradle.org 可解析, 否则暂用已验证的 1.8.1 并记录). (SOURCE: `AngusMailPlugin.kt`, `.readme/common.json`, `app/build.gradle.kts` 的 `resValue` 五键; JVM: `AngusMailPluginRuntimeInfoTest` 3 用例交叉比对; `{REQUIRES_HOST_VERSION}` 暂取 5281 (骨架开发所对的宿主构建 6.8.0, 为临时值), P1.4 回填; 平台插件 1.8.2 已核实可解析并用于构建; 2026-09-18)
- [x] (插件) 以 OpenCC (骨架 / Compose UI 套件 / README 样式) 与 MCP Server (`locks/host-api-aars.lock` 与配置期 SHA-256 校验, `THIRD_PARTY_NOTICES.md`) 为模板生成: `settings.gradle.kts` (平台插件位于 `includeBuild("build-logic")` 之前, 无 `mavenLocal()`, `include(":app", ":mail-core")`), 根 `build.gradle.kts` (`apply false` 版本声明, 含 `com.android.library` 仅当需要), `build-logic/` (`org.autojs.build.{utils,versions,signs,jvm-convention}`), `mail-core/build.gradle.kts` (`java-library` + Kotlin JVM, 依赖 Angus Mail 与 kotlinx-serialization-json, 测试依赖 GreenMail), `app/build.gradle.kts` (`resValue` 六键, `buildFeatures { aidl = true; resValues = true }`, `packaging { resources.pickFirsts += listOf("META-INF/LICENSE.md", "META-INF/NOTICE.md") }`, `appendDigestToReleasedFiles`, 不启用 ABI 拆分并在 `getInfo()` 显式 `supportedAbis = emptyArray()`), `version.properties` (`VERSION_NAME=1.0.0`, `COMPILE/TARGET_SDK=37`, `MIN_SDK=24`, `OVERRIDDEN_*=NONE`), `gradle.properties`, wrapper. (ANDROID_BUILD 2026-09-18: Gradle 9.5.0 / AGP 9.3.2 / Kotlin 2.3.20 / JDK 21, `:app:assembleDebug` `:app:assembleRelease` `:app:lintDebug` `:app:assembleDebugAndroidTest` 通过; `build-logic` 13 个约定源码与 MCP 一致; `pickFirsts` 以 `META-INF/LICENSE.*` / `NOTICE.*` 通配覆盖 `.md` 变体; 未启用 ABI 拆分且 `nativeAlignment.expectNoNativeLibraries` 生效; Compose UI 套件推迟到 P4 设置页时复制)
- [x] (插件) 从宿主复制 `.gitignore`, `sign.properties`, `app/sm003.jks` 到相同相对路径, `.gitignore` 追加 `/mail-test-accounts.properties`, `git check-ignore` 确认三者不入库. (SOURCE 2026-09-18: `.gitignore` 源自宿主并追加 `/mail-test-accounts.properties`, `mail-core/build/`, `*.keystore`; `git check-ignore -v` 确认 `sign.properties`, `app/sm003.jks` (`*.jks`), `mail-test-accounts.properties` 与 `local.properties` 均被忽略)
- [x] (插件) Manifest 骨架: `INTERNET`, `ACCESS_NETWORK_STATE`, `org.autojs.permission.PLUGIN`, `<queries><package android:name="org.autojs.autojs6"/></queries>`, `org.autojs.plugin.WAKE_ACTIVITY` + `WakeActivity`, `org.autojs.plugin.info.AUTHOR`, `INFO` 服务 (`org.autojs.plugin.INFO` + category `mail` + `requiresHostVersion` meta-data), `AngusMailPluginService` (action `org.autojs.plugin.MAIL`, category `mail`, PLUGIN 权限), `allowBackup=false`, `usesCleartextTraffic` 保持默认 false (明文 SMTP / IMAP 只在脚本显式 `tls: 'none'` 时由 socket 层决定, 与 Manifest 无关, 在注释说明), `localeConfig`, `supportsRtl`; 移除 `androidx.startup` 与 `profileinstaller` 组件 (与 OpenCC 一致, 零 provider / receiver). (JVM: `ManifestContractTest` 3 用例; BINDER 2026-09-18: `AngusMailPluginContractTest` 3 用例在 AVD API 24 x86 与 Xiaomi 23046RP50C / API 35 通过; 无 provider / receiver, 未引入 `androidx.startup` / `profileinstaller`)
- [x] (插件) 资源骨架: 10 语言 `strings.xml` (`plugin_description` 各语言, 句尾无点号, 不写 "适用于 AutoJs6", 例如 `通过 IMAP, POP3 和 SMTP 收发, 搜索和监听邮件`), `strings_donottranslate.xml` (`app_name=Angus Mail`), 按 `name` 升序, ASCII 标点; `mipmap/ic_launcher.png` (体现邮件语义的专属图标, 含夜间变体或同步简化 README 模板). (JVM: `StringResourceParityTest` 4 用例 (键集合 / 排序 / 无句尾标点 / `locales_config` 与目录一致), `ApplicationTextPunctuationTest`; 图标由 `.python/generate_launcher_icons.py` 确定性生成: 信封图形, `#1D4ED8` / 夜间 `#1E3A8A` 背景, `mipmap` + `mipmap-night` + adaptive 前景 / 单色图层; 2026-09-18)
- [x] (插件) `.readme/` + `.changelog/` (10 个 `lang_*.json` + 模板) + `.python/generate_markdown.py` (写入与 `--check`) + `.bat` 入口; 根 `README.md` 标明简体中文并与 `README-zh-Hans.md` 同源; `LICENSE` (与宿主插件生态一致); `THIRD_PARTY_NOTICES.md` (common-plugin-api, mail-api, Kotlin stdlib, Angus Mail EPL-2.0 / GPL-2.0-with-classpath-exception / EDL-1.0 三选一声明按上游 `LICENSE.md` 原文, Jakarta Activation EDL-1.0, kotlinx-serialization Apache-2.0, GreenMail Apache-2.0 仅测试). (DOCS 2026-09-18: `py .python/generate_markdown.py --check` = `MARKDOWN_OK languages=10 artifacts=36` (README + 10 语言 README + 11 份 `raw*/plugin_instruction.md` + 11 份 CHANGELOG + 3 汇总); `THIRD_PARTY_NOTICES.md` 含 common-plugin-api SHA-256, Angus Mail 三选一许可, Angus / Jakarta Activation EDL-1.0, kotlinx-serialization, desugar_jdk_libs, GreenMail / SLF4J 仅测试; `mail-api` 条目待 P1.1)
- [x] (插件) 复制 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 为本仓库 `AGENTS.md` 并裁剪 (无原生库, 无 ABI 拆分, 有独立设置页与发行历史, 有联网, 有凭据存储, 有 `:mail-core` JVM 模块与 GreenMail 测试约定, 测试账户文件约定). (SOURCE: `AGENTS.md` 20 节, 增补 5.5 R8 与 Jakarta Mail, 9 邮件核心与网络约束, 14 凭据存储 / 设置页 / 发行历史, 15.1 GreenMail 约定与 15.3 无账户时的本机 GreenMail 设备往返; 2026-09-18)
- [x] (插件) `git init`; 按 "身份与构建骨架 / 契约 / 资源与文档 / 测试与 CI" 拆分初始提交, 每次提交前把 `VERSION_BUILD` 写为 "当前提交数 + 1", 最后校验 `VERSION_BUILD == git rev-list --count HEAD` 且工作树干净. (SOURCE 2026-09-18: `git init -b master`; 6 次提交 (build: Gradle 骨架与 AAR 锁 / docs: 资源, 图标与文案源 / feat: 插件身份与激活契约 / feat(core): Angus Mail spike / test: JVM, instrumentation 与 CI / docs: 路线图与证据), 每次提交前 `VERSION_BUILD` 写为提交数 + 1, 末尾校验 `VERSION_BUILD == git rev-list --count HEAD == 6` 且 `git status --short` 为空)
- [x] (测试) `PluginInfo` 纯数据映射 JVM 测试; Manifest 契约与服务发现 instrumentation 测试 (Wake Activity, INFO 服务, `AngusMailPluginService` 的 exported / permission / action / category 唯一命中); 资源 ASCII 标点守卫测试. (JVM: `AngusMailPluginRuntimeInfoTest` / `ManifestContractTest` / `ApplicationTextPunctuationTest` / `StringResourceParityTest` 共 11 用例, `:app:testDebugUnitTest` 通过; BINDER: `AngusMailPluginContractTest` 3 用例 (Wake Activity 契约, INFO 服务 `getInfo()` 往返含显式空 `supportedAbis`, `MAIL` 服务 descriptor) 在 AVD API 24 与 Xiaomi API 35 通过; 2026-09-18)
- [x] (插件) CI: `.github/workflows/build.yml` (JVM 测试含 GreenMail, debug / androidTest / release APK, lint, API 24 x86 + API 35 x86_64 模拟器契约测试), `markdown.yml` (Windows 运行 `.python/check_markdown.bat`). (SOURCE: `build.yml` (`:mail-core:test`, debug / androidTest APK 与 lint, `verifyNativePageAlignment` 顺带装配 release, API 24 x86 + API 35 x86_64 模拟器契约测试), `markdown.yml` (Windows `check_markdown.bat`); 仓库尚未推送到 GitHub, 工作流未实际运行; 2026-09-18)

### P0.2 Angus Mail 可行性 spike

- [x] (插件) 在 `:mail-core` 引入 `org.eclipse.angus:jakarta.mail:2.0.5` + `angus-activation:2.0.3` + `jakarta.activation-api:2.1.4`; 写一个最小 `SmtpSender.send(text)` 与 `ImapMailbox.listInbox(limit)`; JVM 测试用 GreenMail (`com.icegreen:greenmail:2.1.13`) 起 SMTP + IMAP + POP3, 断言发一封收一封; 顺带实测 GreenMail 是否支持 IDLE (支持则 P5 的 IDLE 测试可在 JVM 完成, 否则 IDLE 只在真实服务商验证并记录). (JVM 2026-09-18: `GreenMailRoundTripTest` 8 用例 (SMTP 发 multipart -> IMAP 列表与 MIME 结构 / 附件字节断言, POP3 列表, 空邮箱, 隐式 TLS + trustAll 三协议, STARTTLS 拒绝降级, 错误密码不回退且消息不含密码, XOAUTH2 三协议 + 无效令牌, IMAP IDLE 推送), `MailSessionPropertiesTest` 7, `MailcapRegistryTest` 2, 共 17 用例 `:mail-core:test` 全绿; GreenMail 2.1.13 通告并推送 IDLE, P5 的 IDLE 测试可在 JVM 完成)
- [x] (插件) Android 装配: 在 `app` 的临时 debug 入口 (仅 spike, 不留 UI) 用同一段 `:mail-core` 代码对一个真实测试账户 (QQ 或 163 授权码, 由维护者提供) 执行发一封 + 列收件箱前 5 封; 记录 `minSdk 24` (AVD API 24) 与一台 API 33+ 实体机的运行结果. (部分完成 2026-09-18: 不留临时 UI, 改为 `MailCoreDeviceTest.realAccountSendsAndListsWhenProvided` 经 instrumentation 参数接收账户; 以本机 GreenMail standalone 2.1.13 代替真实账户完成 DEVICE 往返: AVD API 24 x86 发信 745 ms / 列出收件箱 164 ms, Xiaomi 23046RP50C / API 35 (`adb reverse`) 发信 696 ms / 列表 285 ms, 两者 IDLE 通告 true, MIME 组装 / 解析往返 1005 B; 真实服务商 (QQ / 163 授权码) 未验证, 待维护者提供账户后用同一测试的 `mailAddress` / `mailSecret` / `mailImapHost` / `mailSmtpHost` 参数执行) (2026-09-18 第五次会话完成并关闭: 真实服务商往返经 `mail-test-accounts.properties` + `.python/run_real_account.py` 执行, QQ 邮箱 (授权码) 在 Redmi 22120RN86C / API 33: `session.test` imap 971 ms / smtp 775 ms, 发信 908 ms, 列出收件箱 5 封 756 ms; Gmail (OAuth Playground 访问令牌, XOAUTH2) 在 Sony XQ-AT72 / API 31: imap 3113 ms / smtp 3550 ms, 发信 3512 ms, 列表 2560 ms; 两家的 IMAP / SMTP capability 与 P2.2 的带附件往返见 `docs/dev/p2-core-evidence.md`)
- [x] (插件) release 构建的 R8 处理: `jakarta.activation` 的 `META-INF/mailcap` / `META-INF/services` 与 `org.eclipse.angus.mail.handlers.*` 处理器保留规则 (否则出现 `no object DCH for MIME type multipart/*`), 必要时在代码中显式 `MailcapCommandMap.addMailcap(...)` 注册; `javax.naming` / `java.beans` / `java.lang.management` 等缺失包的 `-dontwarn`; 记录 release APK 体积增量 (预期 < 1.5 MiB), 首次连接延迟, 空闲 PSS; 16 KB 页无关 (无原生库) 记为不适用. (ANDROID_BUILD 2026-09-18: `:app:assembleRelease` 首次失败于 R8 缺失类 `java.awt.Image` / `java.awt.Toolkit` (`image_gif` / `image_jpeg` 处理器) 与 `javax.security.auth.callback.NameCallback` (`OAuth2SaslClient`, Android 无 SASL 路径), 追加 `-dontwarn java.awt.**` / `javax.security.auth.callback.**` 后通过; `MailcapRegistry.ensureRegistered()` 在代码中显式注册五个处理器作为兜底; release APK 403,225 B (下载体积 372,874 B, `classes.dex` 608,456 B + `classes2.dex` 4,392 B), `META-INF/mailcap` / `javamail.default.providers` 保留, `org.eclipse.angus.mail.handlers.*` 与 `IMAPStore` / `SMTPTransport` 保留 (apkanalyzer); debug APK 2,597,081 B; 体积增量按整个 APK 计 0.39 MiB, 远低于 1.5 MiB 预期; `-PandroidTestRelease` 在 AVD API 24 以 `-PandroidTestRelease` 在 AVD API 24 通过全部 6 个 instrumentation 用例 (处理器解析, MIME 往返, 本机 GreenMail 发信 117 ms / 列表 404 ms; 测试专用规则 `proguard-test-rules.pro` / `proguard-android-test-release.pro` 不进入正式 release); 首次连接延迟见上条 (本机 GreenMail), 空闲 PSS 与真实服务商延迟留待真实账户运行; 16 KB 页对齐不适用 (`verifyReleaseNativePageAlignment` 通过, 无原生库))
- [x] (插件) XOAUTH2 冒烟: 用维护者临时取得的 Gmail 或 Outlook.com 访问令牌 (由 `oauth2.py` 或浏览器开发者流程手工获得, 不入库) 以 `mail.imap.auth.mechanisms=XOAUTH2` 连接一次并列出收件箱; 若无法取得令牌, 至少用 GreenMail 的 XOAUTH2 (2.1.13 已修复其解析) 完成一次 JVM 往返并明确记录未做真实服务商验证. (JVM 2026-09-18: `GreenMailRoundTripTest.xoauth2AuthenticatesImapPop3AndSmtp` 以 `mail.<protocol>.auth.mechanisms=XOAUTH2` 完成 SMTP / IMAP / POP3 往返, 无效令牌 -> `AuthenticationFailedException` 且异常消息不含令牌; 未取得 Gmail / Outlook.com 真实令牌, 真实服务商 XOAUTH2 验证明确推迟到 P6 兼容矩阵)
- [x] (文档) 决策点: 以上全部通过则 D2 成立并关闭本节; 任一硬性失败 (无法在 API 24 运行, 或 R8 后不可用, 或体积增量 > 4 MiB 且无法裁剪, 或 IMAP IDLE 在 Android 上不可用) 则启用附录 E.2 退路 (`com.sun.mail:android-mail:1.6.8`) 并回填 D2. 证据写入 `docs/dev/p0-spike-evidence.md`. (D2 成立, 2026-09-18: API 24 可运行 (AVD 往返), R8 后可用 (release 构建 + 处理器解析 + release 构建的设备往返), 体积 0.39 MiB 远低于 4 MiB 上限, IMAP IDLE 在 Android 上由 Angus 正常协商 (GreenMail 通告), 真实服务商的 IDLE 推送留待 P5 设备矩阵; 退路 E.2 未启用; 证据 `docs/dev/p0-spike-evidence.md`)

验收条件: `:app:assembleDebug` / `:mail-core:test` / `:app:testDebugUnitTest` / `:app:assembleDebugAndroidTest` 通过; GreenMail 往返 (JVM) 与真实服务商往返 (DEVICE) 各一次; 证据 (设备, API, 服务商, APK 体积) 写入本节与 `docs/dev/p0-spike-evidence.md`.

2026-09-18 结论: 除 "真实服务商往返" 外全部达成 (DEVICE 往返以本机 GreenMail 完成); P0.2 第 2 条保持打开, 维护者提供测试账户后补跑并关闭, 不阻塞 P1. 2026-09-18 (第五次会话) 补跑真实服务商往返 (QQ 与 Gmail, 真机), P0.2 第 2 条关闭, 本节完成.

---

## P1: 宿主契约与宿主客户端

目标: 宿主拥有 `mail-api` 契约模块, 能发现 / 探测 / 绑定插件并完成一次 `openSession -> call(session.test) -> onResult` 往返; 插件中心正确展示本插件.

### P1.1 契约模块 `plugin-api/mail-api`

- [x] (宿主) 新建 `plugin-api/mail-api` (包 `org.autojs.plugin.mail.api`, `build.gradle.kts` 复制自 `opencc-api`): 六个 AIDL 文件按 4.2 节声明, `PluginInfo` 由 `common-plugin-api` 提供; `MailContract` (`CONTRACT_VERSION = 1`, `MIN_SUPPORTED = 1`, `MAX_SUPPORTED = 1`, 全部 `KEY_*` / `OP_*` / 上限常量), `MailActions`, `MailIds` (`REQUIRED_HOST_VERSION_CODE` 在 P1.4 回填), `MailCapabilityKeys`, `MailErrorCodes`. AIDL 方法顺序即事务号, 冻结. (2026-09-18 落地, 宿主提交 34d2c8fcd: `MailIds.REQUIRED_HOST_VERSION_CODE = 5282`; `MailContract` 另含 `KEY_CONTRACT_VERSION` / `KEY_WATCH_OPTIONS_JSON` / `KEY_PROVIDERS_JSON` / `KEY_ACCOUNTS_JSON`, `FIELD_*` 信封字段, `OPS_WITH_SINK` / `OPS_WITH_SOURCES` / `OPS_METADATA`, 状态 / 事件 / 模式 / 协议 / 认证 / TLS / 特性闭集与宿主侧 `callTimeoutMs()`; `MailActions.OPEN_SETTINGS = org.autojs.plugin.MAIL_SETTINGS`)
- [x] (宿主) `settings.gradle.kts` 的 `pluginApi` 列表加入 `mail-api`; `app/build.gradle.kts` 加 `implementation(project(":plugin-api:mail-api"))`; `AndroidManifest.xml` `<queries>` 增加 `org.autojs.plugin.MAIL` intent. (同一提交 34d2c8fcd)
- [x] (测试) JVM: 常量唯一性与命名 (action / category / id / engine / variant / 所有 `KEY_*` 与 `OP_*` 无重复), 错误码集合与附录 B.4 一致的快照测试, 上限常量为正且满足层级关系 (内联正文阈值 < JSON 信封上限 < 附件上限). (`MailContractTest` 9 用例 + `MailAidlOrderTest` 1 用例, `:plugin-api:mail-api:testDebugUnitTest` 全绿; 信封字段唯一性对嵌套 `error` 对象单独计算, 因 `message` 同时是事件字段)

### P1.2 宿主客户端与桥接组件

- [x] (宿主) `core/plugin/mail/MailPluginHost` (`AidlPluginHost(action = MailActions.SERVICE_ACTION, tag = "MailPluginHost", moduleLabel = "Mail plugin", proxyFactory = IMailPlugin.Stub::asInterface, infoReader = getInfo)`; `discover / probe / queryServiceCount`; `openSession(context, account, secrets)` 用 `callWithDedicatedBindingLease` 得到 `IMailSession` + 租约, 校验 `capabilities` 的契约版本区间与 `requiresHostVersion`; `listProviders` / `listSavedAccounts` 走池化短调用). (按 engine = mail / variant = default 选择且不钉死包名 (F.3 预留); `validatePluginInfo` 校验 engine / variant / 契约版本区间 / `requiresHostVersion >= 5282`; `openSession` 返回 `MailSessionPort` (`BinderMailSessionPort`: 专用租约 + death recipient, 在 `MailBinders.kt`), 另有 `sessionOpener` / `newClient`)
- [x] (宿主) `MailSessionClient`: 请求 JSON 组装 (`id` / `op` / `args`), `call` 封装为 `suspend` (`suspendCancellableCoroutine` + `withTimeout(op 级超时)`; 取消时 `cancel(requestId)`), `onProgress` 转发, 响应解析与 `MailErrorMapper`; 会话死亡 (`DeadObjectException` / death 通知 / `onStatus(closed)`) 后标记 `needsReopen`, 下次调用用内存凭据重开; XOAUTH2 `AUTH_FAILED` 且有 `tokenProvider` 时刷新令牌重开并重试一次 (D4). (纯 Kotlin 状态机, 通过 `MailSessionPort` / `MailSessionOpener` 接口与 Binder 解耦; 死亡通知 / `onStatus(closed)` / 传输异常均标记重开, 进行中调用以 `SESSION_CLOSED` 失败; 凭据以 `MailAccountSpec` 留在内存, `toString()` 不含秘密)
- [x] (宿主) `MailAttachmentSink`: `ParcelFileDescriptor.createReliablePipe()`, 写端随请求交给插件并在请求提交后立刻本端关闭, 读端在后台线程流式写入临时文件 (`<target>.part`), 预检 `expectedSize` 与可用空间, 超过 `MAX_ATTACHMENT_BYTES` 或超出声明大小即中止并删除临时文件, 完成后原子重命名到目标路径; 目标路径规范化与目录创建; 文件名净化 (去分隔符 / 控制字符 / 前导点, 过长截断, 重名时追加序号, 除非 `overwrite: true`). (`createReliablePipe`; 纯逻辑 `spool` / `resolveTarget` / `precheck` (目标路径以字符串判断尾部分隔符, `java.io.File` 会吞掉它); 文件名净化在 `MailFileNames` (`sanitize` / `unique`), 不用 `java.nio.file` 以兼容 API 24 / 25)
- [x] (宿主) `MailAttachmentSource`: 脚本给出的路径 -> `ParcelFileDescriptor.open(MODE_READ_ONLY)`, 校验存在 / 可读 / 大小与数量上限; 调用结束或失败时统一关闭. (`validate` 纯逻辑 + `open`, 关闭统一在 `close()`)
- [x] (宿主) `MailWatchBridge`: 持有 `generation`, 校验回调的 `generation` 与 `seq` 单调, 乱序或陈旧丢弃并计数, 事件交给 P3 的 `MailWatch` (本阶段先以接口 + 假 sink 落地). (`MailWatchSink` 接口 (P3 `MailWatch` 实现), `MailWatchCallbackBinder` 适配 Binder; delivered / stale / reordered / malformed 计数, `closed` 事件后关闭)
- [x] (宿主) 插件中心四处注册 (`InstalledPluginRepository` 的 `specs` 与 `discoverPackage`, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE["mail"]`, `PluginCenterFragment.probeAidlPluginService` 分支); 无需新增插件不可用文案 (`moduleLabel` 复用 5 个既有字符串), 若插件中心需要新的用户可见文案则 11 语言同步. (无新增用户可见文案)
- [x] (测试) JVM: `MailJson` 编解码往返 (含 Unicode 补充平面, 空数组, 缺省字段), `MailErrorMapper` 全码覆盖, `MailSessionClient` 状态机 (假 `IMailSession`: 正常 / 超时 / 取消 / 死亡后重开 / 令牌刷新重试一次后仍失败), `MailAttachmentSink` (声明大小不符 / 超限 / 中断 / 重名 / 原子性), 文件名净化表驱动测试, `MailWatchBridge` 的 generation / seq 过滤. (63 用例 8 类 (`MailJsonTest` 12, `MailErrorMapperTest` 7, `MailSessionClientTest` 17, `MailAttachmentSinkTest` 11, `MailFileNamesTest` 3, `MailAttachmentSourceTest` 3, `MailWatchBridgeTest` 6, `MailAccountSpecTest` 4) 全绿; `MailJson` 基于 Gson, 因宿主 JVM 单测没有 `org.json` 桩)

### P1.3 P1 验收的 Binder 往返

- [ ] (测试) 用 P0 的插件 (此时 `RequestRouter` 只需实现 `session.test` 与 `folders.list`) 在 AVD API 24 与一台实体机完成: 宿主 `MailPluginHost.openSession` -> `call(session.test)` -> `onResult(ok)`; 断开插件进程后再次调用得到 `SESSION_CLOSED` 且下一次调用自动重开; 记录为 BINDER 证据. 若插件尚未就绪, 本条保持打开并在 P2 末尾关闭. (2026-09-18 部分完成: 插件侧 `IMailPlugin.Stub` (`binder/MailPluginBinder`) 与 `PlaceholderMailSession` 落地 (getInfo / getCapabilities 含 `MailCapabilityKeys` 全部键, listProviders / listSavedAccounts 返回 `[]`, openSession 校验账户 Bundle 并在拒绝时 `onStatus(closed)`, call 解析信封并在执行器线程回调, `session.close` 生效, 其余 op 返回 `UNSUPPORTED_OPERATION`, 收到的描述符一律关闭, watch 返回 null); 插件 instrumentation `mailServiceAnswersTheContractBinder` 覆盖同进程 Binder 往返 (结果见会话记录). 宿主 <-> 插件跨进程往返 (安装宿主 APK, `session.test` 真实实现, 杀进程后 `SESSION_CLOSED` 与自动重开) 留到 P2 末尾关闭)

### P1.4 协议文档与 changelog

- [x] (文档) 宿主 `docs/dev/mail-plugin-protocol-v1.md` (英文, 形态同 `mcp-server-protocol-v1.md`): 决策, 发现与绑定, 六个 AIDL 的方法语义 / nullability / 线程 / 所有权, `Bundle` key 表, op 表与参数 JSON 形态, 事件协议, 错误码, 上限, 版本协商, 安全边界 (凭据不进 JSON, 插件侧调用方校验). (宿主提交 34d2c8fcd; 含 op 参数 / 结果 JSON 形态, 描述符所有权, 事件 / 状态文档, 错误码与上限表, 安全边界与宿主客户端行为)
- [x] (宿主) 宿主 `versionCode` 递增并回填 `MailIds.REQUIRED_HOST_VERSION_CODE` 与本文件 P0.1 的 `{REQUIRES_HOST_VERSION}`; 宿主 `.changelog` 10 语言 `feature` 条目 (脚本 API 在 P3 落地时再补一条; 本条只记契约与插件中心支持). (宿主 `VERSION_BUILD` 5281 -> 5282 (构建号自增默认关闭, 手动递增), `MailIds.REQUIRED_HOST_VERSION_CODE = 5282`; 插件 `AngusMailPlugin.REQUIRED_HOST_VERSION = 5282` 与 Manifest meta-data / `.readme/common.json` / changelog / `AGENTS.md` / 本文件同步; 宿主 `.changelog` 10 语言 feature 条目已加并重新生成 markdown)
- [x] (宿主) `assembleAppDebug` + 全量 `testAppDebugUnitTest` 通过 (记录用例数). (`:app:assembleAppDebug` 通过 (universal debug APK 40,165,139 B); 全量 `testAppDebugUnitTest` 由并行会话 autojs6-a9 在含本会话文件的工作树上跑过一次, 除本会话两个测试自身缺陷外全绿, 修复后定向重跑 mail 63 用例 + 契约模块 10 用例全绿)

验收条件: P1.1 / P1.2 的 JVM 测试通过; 插件中心能列出并探测本插件 (DEVICE); P1.3 的往返完成或明确挂起到 P2.

---

## P2: 插件核心

目标: `:mail-core` 具备完整的发信 / 收信 / 搜索 / 正文 / 附件 / 标记 / 移动 / 文件夹 / POP3 退化能力并被 GreenMail 测试覆盖; `app` 的 `RequestRouter` 把全部 op 暴露到 Binder.

### P2.1 账户, 预设与会话

- [x] (插件) `MailAccountOptions` 模型 (附录 A.2 的字段) 与规范化: `provider` 预设合并 (显式字段覆盖预设), `address` 推导 `user` (可分别指定), `receiveProtocol` (`imap` | `pop3`, 默认 `imap`), 每协议的 `host / port / tls (ssl | starttls | none)`, `auth` (`password` | `xoauth2`), 超时 (`connectTimeoutMs` / `readTimeoutMs` / `writeTimeoutMs`), `tls.trustAll` (D25), `name` (显示名), `clientId` (IMAP ID 命令的键值, 163 需要); 非法组合 (如 `xoauth2` 无令牌, POP3 + `watch` 的 IDLE 请求) 给出 `INVALID_ARGUMENT` 明细. (2026-09-18 完成: `account/MailAccountOptions.kt`, 字段名集中在 `MailAccountOptions.Fields`, JSON 中出现 `password` / `accessToken` 等密钥字段直接 `INVALID_ARGUMENT`, 未知字段同样拒绝; 预设合并规则: 显式 host / tls 覆盖后未给 port 时取该 tls 的惯例端口; `MailAccountOptionsTest` 12 用例. POP3 + IDLE 的组合校验属于 watch 参数解析, 留给 P5.)
- [x] (插件) `ProviderPresets`: 数据文件 `mail-core/src/main/resources/providers.json` (附录 C 初表), 加载与 `resolve(providerId)`, `PROVIDERS_VERSION` 上报到 capabilities; 快照测试保证每个预设的主机 / 端口 / TLS 组合合法且 `authHint` 非空. (2026-09-18 完成: `mail-core/src/main/resources/providers.json` version 1, 10 家; `ProviderPresetsTest` 5 用例 (端口与 TLS 组合, `authHint` / `docsUrl` 非空, 只含公开事实); 逐项核实见附录 C 注.)
- [x] (插件) `MailSessionFactory`: 由账户生成 `jakarta.mail.Session` 属性 (`mail.imap(s).*` / `mail.pop3(s).*` / `mail.smtp(s).*` 的 host / port / ssl / starttls / auth / timeouts / `auth.mechanisms`, D21 的 MIME 宽容属性, `mail.imap.partialfetch` 与 `fetchsize` 调优, `mail.smtp.localhost` 避免反向解析阻塞); `tls.trustAll` 时使用 `mail.<protocol>.ssl.trust=*` 并在返回值中标记 `insecure: true`. (2026-09-18 完成: 连接 / 读 / 写超时分开 (`MailTimeouts`), `mail.smtp(s).localhost=localhost`, `tls.trustAll` 与明文端点在 `AccountDocument.insecure` 标记, IMAP `ID` 在登录后由 `ImapMailbox.identify` 发送 (仅当服务器通告 `ID`; 163 / 126 预设默认携带插件名 / 版本 / vendor).)
- [x] (插件) `options.debug` (D28): 会话级开关, 打开时收集脱敏的协议摘要 (命令名 + 响应码 + 前 200 字符, 去掉 LOGIN / AUTH / XOAUTH2 行与 Base64 载荷) 并经 `IMailCallCallback.onProgress` 以 `{id, debug: [...]}` 回传; 实现候选: 每会话 `Session.setDebugOut` 接脱敏过滤流 + `mail.debug.auth=false`, 或在 `ImapMailbox` / `SmtpSender` 层手工记录命令摘要; 脚本永远拿不到原始协议日志, `MailSessionPropertiesTest` 继续断言默认 `session.debug == false`. (2026-09-18 完成, 选定手工摘要方案: `ProtocolTrace` 为每条命令记录一行 `+<ms> <protocol> <command> ok|failed <Exception> <ms>`, 经 `Redactor` 脱敏 (密钥的明文 / Base64 / SASL PLAIN / XOAUTH2 形态与 32 字符以上的 Base64 串一律 `***`) 并截到 200 字符, 每会话最多 200 行; `MailSessionBinder` 在 `onResult` 前以 `{id, debug: [...]}` 经 `onProgress` 回传; 未使用 `Session.setDebugOut`, `session.debug` 保持 false, `MailSessionPropertiesTest` 继续断言.)
- [x] (插件) `ImapMailbox` / `Pop3Mailbox` / `SmtpSender` 的连接管理 (`ConnectionGuard`): 懒连接, 空闲超时断开, 断开后透明重连, 每会话单线程执行器 (D15), `session.test` 返回各协议连接结果与服务器能力 (`IDLE` / `UIDPLUS` / `MOVE` / `CONDSTORE` / `ID` 等 IMAP capability) 与往返耗时. (2026-09-18 完成: `ConnectionGuard` 懒连接 / 空闲过期 / 死连接替换 / 非超时丢失重试一次 (SMTP 默认不重试, 避免重复发送); `MailSession` 持有三个守卫, 所有异常经 `ExceptionMapper` 映射为契约错误码; `session.test` 只探测 `receive` 协议与 SMTP, 逐端点报告 `ok` / `capabilities` / `elapsedMs` / `error`; `MailSessionBinder` 每 60 s 在会话线程调用 `expireIdle`.)
- [x] (测试) GreenMail: 预设合并优先级, 三协议 SSL / STARTTLS / 明文各一次连接, 错误密码 -> `AUTH_FAILED`, 错误端口 -> `CONNECT_FAILED`, 超时 -> `TIMEOUT`, 空闲断开后重连. (2026-09-18 完成: `MailSessionGreenMailTest` 10 用例 (明文 / SSL+trustAll 探测, POP3 receive, 错误密码 -> `AUTH_FAILED`, 空闲端口 -> `CONNECT_FAILED`, 只接受不应答的本地 ServerSocket -> `TIMEOUT`, 假时钟空闲过期 + 死连接 + `disconnect()` 后重连, 脱敏 trace, 仅 SMTP 账户, 关闭后拒绝并清空密钥) + `ConnectionGuardTest` 8 + `ExceptionMapperTest` 9 + `MailAccountOptionsTest` 12 + `ProviderPresetsTest` 5; `:mail-core:test` 合计 61 用例. STARTTLS 的成功路径仍只能在真实服务商验证 (GreenMail 不提供 STARTTLS), 拒绝降级由 P0 用例覆盖.)

### P2.2 发信 (SMTP)

- [x] (插件) `SmtpSender.send(request, attachments)`: 收件人 (`to` / `cc` / `bcc`, 各为字符串或 `{name, address}` 或其数组, RFC 2047 编码显示名), `replyTo`, `subject`, `text` / `html` (两者皆有则 `multipart/alternative`), 附件 (`multipart/mixed`, 文件名 B 编码, 显式 `mimeType` 或按扩展名推断, `contentId` 时归入 `multipart/related` 作为内联), 自定义 `headers` (禁止覆盖 `From` / `To` / `Date` / `Message-ID` / `Content-*`), `priority` (`high` | `normal` | `low` -> `X-Priority` / `Importance`), `inReplyTo` / `references`, `date`; 返回 `{messageId, accepted[], rejected[]}` (部分拒收 -> `SEND_REJECTED` 且带明细). (2026-09-18 完成: `:mail-core` `message/` 新增 `OutgoingMessage` (`MailAddressSpec`, `Priority`, `OutgoingAttachment`), `AttachmentSource` (可重复打开: SMTP 与 IMAP `APPEND` 各序列化一次; `FileAttachmentSource` / `BytesAttachmentSource`), `OutgoingMessageParser` (字段与形状校验, `MAX_RECIPIENTS` / `MAX_ATTACHMENTS_PER_MESSAGE` / `MAX_ATTACHMENT_BYTES` / 32 个头 / 998 字符, `FORBIDDEN_HEADERS` 与 `Content-*` 拒绝, 头值禁止换行, `descriptorIndex` 越界与重复拒绝, 文件名去掉路径分隔符与控制字符), `MessageComposer` (mixed / related / alternative 树, 附件 base64, 文件名与显示名 RFC 2047; 去掉 `mail.mime.allowutf8` 使头部一律编码, D34), `MimeTypes`; `SmtpSender.send` 返回 `SentMessage {mime, messageId, accepted}`, `sendpartial` 保持关闭 (全有或全无, 任一拒收 -> `SEND_REJECTED` 带明细); app 侧 `DescriptorSource` (宿主描述符 -> `AttachmentSource`: `dup` + `lseek(0)`, 仅接受可 seek 文件, `statSize` 为大小), `RequestRouter` 注册 `mail.send` / `messages.append` 并在路由之后才解析描述符, `MailSessionBinder` 在会话线程校验描述符 (数量 <= `MAX_DESCRIPTORS` -> 否则 `LIMIT_EXCEEDED`, 非 `OPS_WITH_SOURCES` / `OPS_WITH_SINK` 的 op 携带描述符 -> `INVALID_ARGUMENT`) 并在 `onResult` 前关闭全部副本)
- [x] (插件) `saveToSent` 选项: 发送成功后若账户为 IMAP 且服务器未自动保存 (预设表的 `autoSavesSent` 字段), 用 IMAP `APPEND` 写入 `Sent` 文件夹 (预设表给出各服务商的已发送文件夹名, 缺省尝试 `\Sent` special-use 属性). (2026-09-18 完成: `MailSession.send(message, saveToSent?)` 三态: `null` 按预设 `autoSavesSent` (false 时追加), `true` 仅在服务商不自动保存时追加, `false` 不追加; 已发送文件夹解析 `resolveSentFolder`: 预设 `sentFolder` (存在时) -> `\Sent` special-use -> 候选 `Sent` / `Sent Messages` / `Sent Items` / `INBOX.Sent` / `INBOX/Sent` / `已发送`, 从不创建; 副本带 `\Seen` 且 Message-ID 与发出的一致; 结果 `SendResult {messageId, accepted, rejected, savedToSent, sentCopy: server | appended | failed | none, sentFolder?, saveError?, elapsedMs}`, 追加失败不影响发送结果 (`saveError`); 真机核实: QQ 对刚经 SMTP 发出的邮件 APPEND `Sent Messages` 回 `A5 NO Mail has saved by smtp!`, 证实预设 `autoSavesSent = true`, 由此 D33: 显式 `true` 也不对自动保存的服务商追加 (Gmail 会出现重复); `messages.append(folder, message, flags)` 经 `FlagMapper` 设标志, UIDPLUS 时返回 UID (真机: QQ `Drafts` UID 2 / 2019 ms, Gmail `[Gmail]/Drafts` UID 327 / 5613 ms), 缺失文件夹 -> `FOLDER_NOT_FOUND`)
- [x] (测试) GreenMail: 纯文本 / HTML / 混合 / 多附件 / 内联图片 / 中文主题与文件名 / 500 收件人上限 / 附件数量与大小上限 / 非法地址 -> `INVALID_ARGUMENT`; 接收端解析回的 MIME 结构断言. (2026-09-18 完成: `OutgoingMessageParserTest` 9 (完整 / 最小解析, send / append 参数含 `$Label1` 关键字, 收件人校验与 500 上限, 形状错误命名字段, 描述符绑定, 附件数量与大小上限, 头校验, 文件名清洗), `MessageComposerTest` 7 (纯文本单部件, text+html -> alternative, 仅 html, 内联图片 + 附件 -> related in mixed 且头部全部编码, 显式 from, 无正文附件得到空文本部件, 数据源每次序列化重新打开), `SmtpSendGreenMailTest` 9 (to / cc / bcc 投递且 Bcc 不外泄 + X-Priority, 内联 png + 中文 csv 结构断言, `saveToSent` 追加 `\Seen` 副本 Message-ID 一致, 预设默认与显式 true / false 及自动保存服务商不重复, 缺失文件夹不创建, 仅 SMTP 账户跳过副本, 草稿 APPEND 带标志与 UID, 缺失文件夹 -> `FOLDER_NOT_FOUND`, 非法地址 -> `INVALID_ARGUMENT` 且未发送); `:mail-core:test` 85 用例; app `RequestRouterTest` 6 (快照含 `mail.send` / `messages.append`, 描述符只属传输 op, 参数错误先于联网); `AngusMailPluginContractTest` 新增描述符所有权用例 (非传输 op 携带描述符, 越界 `descriptorIndex`, 管道 -> 非可 seek, 65 个 -> `LIMIT_EXCEEDED`, 副本均在 `onResult` 前关闭) 在 AVD API 24 debug 与 release (R8) 6/6 通过; 真机: QQ_A -> QQ_B 带附件往返 (对端 1535 ms 收到, 2335 B) 与 Gmail XOAUTH2 带附件发送, 两家各追加一封草稿; 证据 `docs/dev/p2-core-evidence.md`, 运行脚本 `.python/run_real_account.py`)

### P2.3 收信, 搜索, 正文与附件 (IMAP)

- [ ] (插件) `folders.list` (树形 + `specialUse` 属性 + 消息数 / 未读数可选), `folders.status`, `folders.create / delete / rename`.
- [ ] (插件) `messages.list`: 参数 `folder` (默认 `INBOX`), `limit` (默认 50, 上限 `MAX_PAGE_SIZE`), `before` / `after` (以 UID 作游标, 分页无需偏移量), `order` (`desc` 默认), `unseenOnly`; 用 `FetchProfile` 只取 ENVELOPE / FLAGS / SIZE / BODYSTRUCTURE 摘要 (`hasAttachments` 由 BODYSTRUCTURE 判定, 不下载正文); 返回附录 A.4 的 envelope 形态.
- [ ] (插件) `messages.search`: 附录 A.5 的查询 JSON -> `SearchTerm` 编译 (`from / to / cc / subject / body / text / since / before / sentSince / sentBefore / seen / flagged / answered / draft / larger / smaller / header / messageId / uid` 与 `and / or / not` 组合); 服务器返回 `BAD` (常见于非 ASCII) 时按 `fallback: 'client'` 选项对最近 `MAX_CLIENT_FILTER` 封做客户端过滤并在结果中标记 `fallback: true`; 结果按 UID 降序分页.
- [ ] (插件) `messages.get`: 按 UID 取正文, `MessageMapper` 递归遍历 MIME 树: 首选 `text/plain` 与 `text/html` 各一, `BodyExtractor` 无纯文本时 `HtmlToText` 降级, 附件与内联部件枚举 (`partId` 为 IMAP section 路径, `fileName` 解码, `mimeType`, `size`, `contentId`, `inline`), 全部 `headers`; 正文超过 `MAX_INLINE_BODY_BYTES` 时不内联而给出 `bodyDescriptor` 提示走 `messages.raw` / `attachments.download`; `peek: true` 默认不置 `\Seen`.
- [ ] (插件) `attachments.download`: 按 `uid + partId` 定位部件, 解码后流式写入宿主给出的 PFD 写端 (64 KiB 缓冲, 每 1 MiB 或 5% 发一次 `onProgress`), 超过 `MAX_ATTACHMENT_BYTES` 或宿主关闭读端时中止; `messages.raw` 同样流式输出 RFC 822 原文.
- [ ] (插件) `messages.setFlags` (`add | remove | set`, 系统标记 `seen / flagged / answered / draft / deleted` 与用户关键字), `messages.move` (有 `MOVE` 能力用之, 否则 COPY + `\Deleted` + EXPUNGE), `messages.copy`, `messages.delete` (`expunge: true` 立即清除, 否则仅标记), `messages.expunge`, `messages.append` (脚本构造的邮件存入文件夹, 如草稿).
- [ ] (测试) GreenMail: 每个 op 的 happy path, 空文件夹, 不存在的文件夹 / UID -> `FOLDER_NOT_FOUND` / `MESSAGE_NOT_FOUND`, 分页游标连续性, 附件 10 MiB 流式与中途关闭读端, GB18030 / GBK / ISO-2022-JP / 未声明 charset 的夹具 (`mail-core/src/test/resources/mime/*.eml`), 非标准编码文件名, 嵌套 `message/rfc822`, 只有 HTML 的邮件降级文本, 路径穿越文件名净化.

### P2.4 POP3 退化

- [ ] (插件) `Pop3Mailbox`: `folders.list` 只返回 `INBOX`; `messages.list` 以 UIDL 为 `uid` (字符串), 分页按序号; `messages.get` 完整下载后映射; `messages.delete` 为 `DELE` (关闭时生效, `expunge` 语义映射); `messages.search` 只做客户端过滤 (信头 `TOP` 抓取, 上限 `MAX_CLIENT_FILTER`); `setFlags / move / copy / folders.*` -> `UNSUPPORTED_OPERATION`; `messages.raw` 与 `attachments.download` 走完整下载.
- [ ] (测试) GreenMail POP3: 列表 / 正文 / 删除 / 客户端过滤 / 不支持操作的错误码.

### P2.5 Binder 路由, 上限与调用方校验

- [ ] (插件) `AngusMailPluginService` 实现 `IMailPlugin`: `getInfo` (仿 OpenCC `PluginRuntimeInfo`), `getCapabilities` (契约版本, 协议集, 认证机制集, 特性集 `idle / savedAccounts / append / clientSearchFallback`, `PROVIDERS_VERSION`), `openSession` (账户 JSON + 秘密 key 解析, 别名解析预留给 P4, 每会话一个 `MailSessionBinder`), `listProviders`, `listSavedAccounts` (P4 前返回空).
- [ ] (插件) `MailSessionBinder`: `call` 立即返回 `requestId` 并投递到会话执行器, 结果经 `IMailCallCallback.onResult`; `cancel` 中断执行器中的任务 (`Thread.interrupt` + `Folder.close(false)` 打断阻塞读); `close` 关闭连接与全部 watch; `getStatus` 返回连接态与最近错误.
- [ ] (插件) `Limits`: JSON 信封 <= `MAX_ENVELOPE_BYTES`, 描述符数量 <= `MAX_DESCRIPTORS`, 字符串字段长度, 数组长度, 枚举值, 未知 `op` -> `INVALID_ARGUMENT`; 每会话并发 `call` 上限 (超出排队, 队列满 -> `LIMIT_EXCEEDED`).
- [ ] (插件) `CallerGuard`: 只接受宿主包名 (`org.autojs.autojs6` 及其调试变体) 且签名匹配官方证书或与本插件同签名的调用方 (`Binder.getCallingUid` + `PackageManager`), 否则 `SecurityException`; 与 MCP Server 插件的实现对齐.
- [ ] (插件) `RequestRouter` 注册表: op -> 处理器 + 参数 schema 校验 + 支持协议集; 快照测试保证与 `MailContract.OP_*` 集合完全一致.
- [ ] (测试) JVM: `RequestRouter` 的每个 op 用假 mailbox 覆盖参数校验与错误映射; instrumentation: `openSession` -> `call(folders.list)` 经真实 Binder, 超限信封 -> `LIMIT_EXCEEDED`, 非宿主调用方被拒, 进程重建后 `SESSION_CLOSED`; 关闭 P1.3.

验收条件: `:mail-core:test` 全绿 (GreenMail 覆盖三协议); instrumentation 在 AVD API 24 与一台实体机通过; 用真实服务商 (至少 QQ 或 163 一家 + Gmail App Password) 手工跑一遍发信 / 列表 / 正文 / 附件下载, 证据写入 `docs/dev/p2-core-evidence.md`.

---

## P3: 脚本 API `mail`

目标: 脚本可以用附录 A 的 API 完成连接, 发信, 收信, 搜索, 正文, 附件, 标记, 移动与文件夹操作, 同步与 Async 双形态, 错误为 `MailError`.

### P3.1 连接与全局对象

- [ ] (宿主) `augment/mail/Mail` (`Augmentable`, `keys = ["mail"]`): `connect(options | alias)` / `connectAsync`, `setDefault(client | options | alias)`, `default` 属性, `providers.list() / get(id) / resolve(address)`, `accounts.list() / has(alias)` (元数据, 无秘密), `MailError` 构造器暴露 (`instanceof` 可用), `close()` (关闭默认客户端); 全局便捷方法 `send / fetch / search / get / download / markRead / markUnread / move / delete / folders / watch` 及其 `Async` 版转发到默认客户端, 无默认账户抛 `NO_DEFAULT_ACCOUNT`; 注册到 `ScriptRuntime` (`Mail(this).augment(target)` 与 `Mediainfo` 相邻) 并在引擎退出钩子中关闭全部客户端.
- [ ] (宿主) `MailClient` 脚本对象: `account` (脱敏快照), `isConnected`, `test()` / `testAsync()`, `close()`; 同步方法经 `runBlocking(scriptRuntime.coroutineContext)` 桥接, Async 方法经 `ScriptPromiseAdapter`; 每个方法的参数规范化在纯 Kotlin 函数中完成 (可 JVM 测试), 只在 Rhino 边界做类型转换; `debug: true` (D28) 时把 `onProgress` 携带的脱敏协议摘要逐行输出到 `console.verbose`.
- [ ] (宿主) `MailError`: JS 可见异常类 (`name = 'MailError'`, `code`, `message`, `details`, `retryable`), 同步方法直接抛, Async 方法以之 reject; 发现 / 绑定 / 版本失败的 `IllegalStateException` 文案保留原文并包成 `PLUGIN_UNAVAILABLE`.
- [ ] (测试) JVM: 参数规范化 (地址字符串 / 对象 / 数组, 预设 + 覆盖, 非法组合), `MailError` 映射; DEVICE: `mail.connect({provider: 'qq', ...})` 与 `mail.connect('alias')` (P4 前用内存假别名) 冒烟脚本.

### P3.2 发信与收信

- [ ] (宿主) `client.send(message, options?)` / `sendAsync`: 附件路径 -> `MailAttachmentSource`; 返回 `{messageId, accepted, rejected}`; `saveToSent` 透传.
- [ ] (宿主) `client.folders(options?)`, `client.folder(name).status()`, `createFolder / deleteFolder / renameFolder`.
- [ ] (宿主) `client.fetch(options?)` / `fetchAsync`: 返回 `MailMessage[]` (envelope, `bodyLoaded = false`); `client.search(query, options?)`; `client.get(uid | message, options?)` / `fetchBody` 别名: 返回带 `text` / `html` / `attachments` / `headers` 的完整 `MailMessage`; 所有返回对象为普通 JS 对象 + 少量绑定方法 (`message.load()`, `attachment.download(target?)`), `date` 为 JS `Date` (D26).
- [ ] (宿主) `client.download(attachment | {uid, partId}, target?)` / `downloadAsync`: 目标为目录时用净化后的文件名, 为文件路径时按给定名; 返回落盘路径; 进度经可选 `onProgress` 回调 (Async 版) ; `client.raw(uid, target)` 导出 `.eml`.
- [ ] (宿主) `client.setFlags(uids, flags, mode?)`, 语法糖 `markRead / markUnread / flag / unflag`, `move(uids, folder)`, `copy`, `delete(uids, {expunge})`, `expunge(folder)`, `append(folder, message, flags?)`.
- [ ] (测试) DEVICE 冒烟脚本 (放入本仓库 `docs/smoke/*.js`, 用 `mail-test-accounts.properties` 注入账户): 对 QQ / 163 / Gmail 各跑一遍 发信 -> 搜索刚发的主题 -> 取正文 -> 下载附件到工作目录 -> 标记已读 -> 移动到自建文件夹 -> 删除; Async 版同样跑一遍并确认不阻塞 UI 线程 (在 `ui` 模式脚本中调用).

### P3.3 文档与 changelog 同步 (P3 末)

- [ ] (文档) 宿主 `docs/dev/mail-plugin-protocol-v1.md` 补脚本 API 到 op 的映射表; 宿主 `.changelog` 10 语言 `feature` 条目 "脚本 API mail".
- [ ] (宿主) 全量 `testAppDebugUnitTest` 与 `assembleAppDebug` 通过.

验收条件: P3.2 的冒烟脚本在两台以上设备 (含 API 24 AVD) 与三家服务商通过; JVM 测试通过; 证据写入 `docs/dev/p3-script-api-evidence.md`.

---

## P4: 插件设置页与账户存储

目标: 用户可在插件内保存账户 (密码或令牌加密存储), 脚本用别名连接; 设置页跟随宿主外观; 有发行历史 (D29) 与电池优化引导 (D27).

- [ ] (插件) `AccountStore`: 仿 Three-Stone-AI 的 `EncryptedAiCredentialStore` (Android Keystore AES-256-GCM 主密钥, AAD 绑定别名, 文件记录 + 文件锁), 记录含账户非秘密字段 JSON 与密文秘密; `put / get / list / remove / setDefault`; 秘密只以 `CharArray` / `ByteArray` 经手并即时清零; `toString` 为 `[REDACTED]`.
- [ ] (插件) `AccountsActivity` (Compose, 复制 OpenCC UI 套件): 账户列表 (别名, 地址, 服务商, 默认标记), 新增 / 编辑 (选择预设自动填充, 显式高级字段, 密码或令牌输入以 `CharArray` 读取), "测试连接" (调用 `:mail-core` 的 `session.test`, 展示各协议结果与耗时, 不落盘), 删除确认, 设为默认; 与 `AppSettingsActivity` / `AboutActivity` / `ReleaseHistoryActivity` 以 `parentActivityName` 串联; 跟随宿主主题 / 夜间 / 语言 (`AutoJs6HostSettings`); 无障碍标签, RTL, 大字体, 进程恢复.
- [ ] (插件) `openSession` 支持 `alias` 形态 (秘密在插件进程内解密, 不经 Binder 回传宿主); `listSavedAccounts` 返回别名元数据; capabilities `savedAccounts = true`; 设置页可从宿主插件中心跳转 (`org.autojs.plugin.MAIL_SETTINGS` action, 形态同 `AI_PROVIDER_SETTINGS`).
- [ ] (宿主) `mail.connect('alias')` / `mail.accounts.list()` 走别名形态; 插件中心的本插件条目提供 "设置" 入口 (若通用机制已支持 settings action 则仅注册, 否则最小接入).
- [ ] (插件) 设置页 "发行历史" 复用 `assets/doc/CHANGELOG-<lang>.md` (按 locale 选择, 缺失回退英语); 不做插件内更新检查 (D29).
- [ ] (插件) "忽略电池优化" 引导 (D27): Manifest 追加 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (同步 `ManifestContractTest` 的权限集合与 README 安全章节 / changelog 的理由说明), 设置页按钮读取 `PowerManager.isIgnoringBatteryOptimizations` 显示当前状态, 点击后说明用途并以 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 打开系统对话框; 不在启动时自动请求, 不作为任何功能的前置条件; API 24 / 28 / 33+ 三档设备各验证一次对话框与状态回读.
- [ ] (测试) JVM: `AccountRecordCodec` 往返与篡改检测 (AAD 不匹配 -> 失败), 别名规范化; instrumentation: Keystore 密钥生成 / 加解密 / 删除后不可解密, `openSession(alias)` 往返; DEVICE: 设置页在 API 24 / 28 / 33+ 三档设备上新增 QQ 账户 -> 测试连接 -> 脚本以别名收信.

验收条件: instrumentation 与 DEVICE 通过; 凭据审计 (日志 / JSON / 异常 / 崩溃报告 grep 无秘密) 写入 `docs/dev/p4-settings-evidence.md`.

---

## P5: 新邮件监听

目标: `client.watch()` 在 IMAP IDLE 与轮询两种模式下稳定推送新邮件事件, 断网 / 切网 / 进程重建后能恢复, POP3 以 UIDL 轮询. `IdleWatcher` / `PollWatcher` 设计为可脱离脚本会话复用, 供 P8 后台守望直接使用.

- [ ] (插件) `IdleWatcher`: 独立 `Store` 连接 (D15), `IMAPFolder.idle()` 循环, 每 24 分钟由看门狗线程发 `NOOP` 打断并重进 IDLE, `MessageCountListener` 收到新消息后按 UID 抓 envelope (可选正文) 并发 `message` 事件; `FolderClosedException` / `IOException` -> 指数退避重连 (1 s 起, 上限 5 min, 抖动), 重连后按 `lastUid` 用 `UID SEARCH <lastUid+1>:*` 补拉并去重; 服务器无 IDLE 能力或 IDLE 连续失败 3 次 -> 切到 `PollWatcher` 并发 `mode` 事件.
- [ ] (插件) `PollWatcher`: 每 `pollIntervalMs` (默认 60 s, 下限 `MIN_POLL_INTERVAL_MS`) 执行 UID (IMAP) 或 UIDL (POP3) 差分; POP3 只报新增, 不报删除.
- [ ] (插件) `MailWatchBinder`: `watch(options, callback)` 返回 `IMailWatch`, 事件按 D17 携带 `generation` / `seq`; 待发队列超过 `MAX_WATCH_QUEUE` 时清空并发 `resync`; `stop` / 会话 `close` / 宿主死亡 (`callback.asBinder().linkToDeath`) 均停止线程与连接; 每会话 watch 数上限 `MAX_WATCHES_PER_SESSION`.
- [ ] (宿主) `MailWatch` (`EventEmitter`): 事件 `message(message, watch)` / `error(err)` / `close(reason)` / `mode(mode)`; `stop()`, `isActive`, `mode`, `folder`; 事件经 `ScriptAsyncDispatcher` 投递; 脚本退出自动 `stop`; 插件死亡时发 `error(PLUGIN_UNAVAILABLE)` 并按 `reconnect: true` (默认) 用重开的会话重新 `watch` (新 generation), 否则 `close`.
- [ ] (宿主) 全局 `mail.watch(folder?, options?)` 转发到默认客户端; `options.fetchBody` / `options.pollIntervalMs` / `options.mode` (`auto` | `idle` | `poll`) / `options.reconnect`.
- [ ] (测试) JVM: `IdleWatcher` / `PollWatcher` 用 GreenMail (IDLE 可用时) 或假 `Folder` 覆盖新邮件 / 断线重连 / 补拉去重 / 队列溢出 `resync` / 模式切换; 宿主 `MailWatchBridge` + `MailWatch` 的事件顺序.
- [ ] (测试) DEVICE 矩阵 (证据 `docs/dev/p5-watch-evidence.md`): QQ 与 Gmail 各一, 脚本 `watch('INBOX')` 后从另一账户发信, 记录到达延迟; 关 Wi-Fi 30 s 再开 / Wi-Fi 切蜂窝 / 手机灭屏 15 分钟 (Doze) / 手动杀插件进程 四种扰动下事件是否恢复; 记录 Doze 下网络被冻结导致的延迟, 结论写入 README 兼容性节与 P4 设置页电池优化引导 (D27) 的说明文案.

验收条件: JVM 测试通过; 两家服务商在两台设备上完成扰动矩阵; 到达延迟与恢复行为写入证据文件.

---

## P6: 健壮性, 安全, 兼容矩阵, 性能与体积

- [ ] (测试) 敌意输入 (`docs/dev/p6-hostile-input.md`): 超长 / 嵌套过深的 MIME, 20000 个收件人的信头, 无 `Content-Type` 的部件, 非法 base64, 递归 `message/rfc822`, 文件名含 `../` / NUL / 2000 字符, `Content-Disposition` 与 `Content-Type` 文件名冲突, 声明大小与实际不符的附件, 超过上限的 JSON 信封, 非法 UTF-8 主题; 全部返回明确错误码且不崩溃, 宿主不留半写入文件.
- [ ] (测试) 服务商兼容矩阵 (`docs/dev/p6-provider-matrix.md`): QQ / 163 / 126 / Gmail (App Password) / Outlook.com (XOAUTH2 令牌, 可用时) / iCloud (App Password) / Aliyun 各跑 发信 / 列表 / 中文搜索 (服务器与客户端回退) / 正文 / 附件 / 标记 / 移动 / IDLE; 记录每家的特殊行为 (163 `ID` 命令, QQ 的已发送文件夹名, Gmail 的 `[Gmail]/` 前缀与标签语义, iCloud 的文件夹层级分隔符) 并回填附录 C.
- [ ] (测试) TLS 矩阵: SSL 993 / 995 / 465, STARTTLS 143 / 110 / 587, 明文 (仅 GreenMail), 自签证书 (GreenMail 自签 + `tls.trustAll`), 证书主机名不匹配 -> `TLS_FAILED`; API 24 上 TLS 1.2 默认启用确认.
- [ ] (测试) 字符集矩阵: GB18030 / GBK / GB2312 / Big5 / ISO-2022-JP / EUC-KR / UTF-8 / 未声明 的正文与主题与文件名夹具, 断言解码结果.
- [ ] (插件) 秘密审计: 全仓库 `grep` 日志语句与异常构造, 确认无 `password` / `accessToken` / 授权码进入 `Log` / JSON / `toString` / 异常消息; Jakarta `mail.debug` 强制关闭且不可由脚本打开 (D28 允许的调试输出仅限脱敏后的协议摘要).
- [ ] (测试) 性能基线 (`docs/dev/p6-performance-baseline.md`): 10000 封收件箱的 `fetch(limit 50)` 与 `search` 耗时, 50 MiB 附件下载吞吐与内存峰值, 发送 10 MiB 附件, IDLE 待机 1 小时的插件 PSS 与电量 (`dumpsys batterystats`); 无回归阈值 (仅记录).
- [ ] (测试) 生命周期矩阵 (`docs/dev/p6-lifecycle-matrix.md`): 脚本正常退出 / 强停 / 宿主被杀 / 插件被杀 / 插件被禁用 / 插件被卸载 / 插件升级 六种情况下 会话与 watch 的清理, 无泄漏 (`dumpsys activity services` 与 `lsof` 检查 FD).
- [ ] (发布) 体积: release universal APK 体积记录; Angus Mail 未用的 provider (如 `gimap` 若不用 Gmail 扩展, `dsn`, `pop3` 保留) 与 `MailHandler` 日志组件经 R8 裁剪; 记录每个裁剪的依据.

验收条件: 以上证据文件齐备; 已知不通过项在附录 D 立项或在 README 兼容性节明示.

---

## P7: 文档, changelog 与发布 gate

- [ ] (文档) `AutoJs6-Documentation`: `api/mail.md` (模块页, 插件依赖段落按 `api/barcode.md:5` 的写法, `## [@] mail` / `## [m] connect` / ... 全部方法含 `Async` 标签与重载, 示例脚本); 类型页 `api/mailClientType.md`, `api/mailMessageType.md`, `api/mailAccountOptionsType.md`, `api/mailSearchQueryType.md` (按 `*Type.md` 惯例, `dataTypes.md` 加转发段); 注册 `api/sidebar.md`, `api/toc.md`, `api/progress.md`, `api/changelog.md`; `project.json` 版本; `normalize-markdown.py --check` + `auto-generate.py --check` 通过.
- [ ] (文档) `AutoJs6-TypeScript-Declarations`: `declarations/autojs6/aj6-int-mail.d.ts` (`Internal.Mail` / `Internal.MailClient` / `Mail.*` 类型命名空间, 同步 + Async 重载, `MailWatch extends EventEmitter` 的事件重载), `index.d.ts` 三斜线引用, `aj6-int-init.d.ts` 的 `declare let mail: Internal.Mail;` / `$mail`; `docs/smoke/mail-smoke.ts` 通过 `tsc --noEmit --strictNullChecks --skipLibCheck --target ES2020 --lib ES2020`; `package.json` 两处版本 minor 递增, `docs/CHANGELOG.md` 与 README 版本历史 (只保留最近 3 版).
- [ ] (文档) `AutoJs6-Plugin-Ace-Editor`: 拷贝 `aj6-int-mail.d.ts` 到 `assets/editor/.../types/autojs6/`, 用宿主 `tools/ace-completion/generate-dts.mjs` 再生成聚合声明与 LSP 分组; `AutoJs6-Plugin-Offline-Docs`: 运行文档仓库 `auto-generate-for-autojs6.bat --sync-offline`, `SOURCE_PROVENANCE.md` 更新; 两仓库按各自 `AGENTS.md` 处理版本与提交.
- [ ] (插件) README (10 语言, 单一文案源): 能做什么, 安装, 在插件中心启用, 各服务商准备工作 (开启 IMAP / 取授权码 / Gmail App Password / Outlook 需令牌), 快速示例 (发信 / 收信 / 监听), 别名账户, 兼容性 (P6 矩阵摘要), 常见问题 (`AUTH_FAILED` 排查, 163 `Unsafe Login`, 中文搜索回退), 发行历史, 许可证与第三方声明; `.changelog` 10 语言 `v1.0.0` 完整.
- [ ] (宿主) 宿主 `.changelog` 10 语言复核 (契约 / 脚本 API / 插件中心三条), `docs/dev/mail-plugin-protocol-v1.md` 定稿, `README` 插件列表若有则加入本插件.
- [ ] (发布) 发布 gate: `generate_markdown.py --check`, `:mail-core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug :app:assembleDebugAndroidTest`, `:app:lintDebug`, instrumentation 在 API 24 AVD + 一台实体机, `appendDigestToReleasedFiles` 产出单个已签名 universal APK (`autojs6-plugin-angus-mail-v1.0.0-<CRC32>.apk`), 安装 + 插件中心启用 + 冒烟脚本; 官方插件索引 receipt (若索引仓库流程要求); GitHub 仓库创建与推送由维护者明确指示后进行.
- [ ] (文档) 本文件收尾: 全部条目勾选并附证据, 附录 D 全部回填, 会话记录补最后一条.

验收条件: 四个关联仓库各自的检查命令通过; 发布 gate 全绿; 已知限制在 README 与本文件附录 D 明示.

---

## P8: 后台守望与新邮件触发脚本 (1.1.0)

目标: 没有脚本运行时插件也能为指定账户保持 IMAP IDLE (或轮询) 连接, 新邮件到达后由宿主启动指定脚本并把 envelope 作为参数传入; 用户在插件设置页与宿主定时任务中管理. 前置 P7 (1.0.0 已发布), 维护者已于 2026-09-18 拍板 (D30).

- [ ] (宿主) 契约演进: `IMailPlugin.aidl` 末尾追加 `IMailTrigger openTrigger(in Bundle options, IMailTriggerCallback callback)` 与 `Bundle listTriggers()`; 新增 `IMailTrigger { Bundle getStatus(); void update(in Bundle options); void stop(); }` 与 `IMailTriggerCallback { oneway void onMail(long generation, long seq, in Bundle event); oneway void onStatus(in Bundle status); }`; `CONTRACT_VERSION` 升为 2 (`MIN_SUPPORTED` 仍为 1); `MailCapabilityKeys.FEATURES` 加 `backgroundWatch`; 宿主与插件按能力协商, 任一方为旧版时功能不可见, 不抛错.
- [ ] (插件) `MailWatchService`: 前台服务 (`foregroundServiceType="dataSync"`, Android 14+ 需 `FOREGROUND_SERVICE_DATA_SYNC`; 常驻通知说明账户与状态, `POST_NOTIFICATIONS` 运行时申请), 复用 P5 的 `IdleWatcher` / `PollWatcher` 与 P4 的 `AccountStore` (只支持已保存别名, 不接受脚本传入的凭据); `ConnectivityManager` 网络变化回调触发立即重连; Doze 下按 D27 引导但不作为前置; 触发记录 (最近 100 条, 只含 envelope 摘要, 不含正文) 持久化于 `noBackupFilesDir`; 开机自启为设置页开关 (默认关, `RECEIVE_BOOT_COMPLETED`). 权限追加理由写入 README 安全章节与 changelog.
- [ ] (插件) 设置页 "守望": 账户 (别名) 选择, 文件夹, 模式 (IDLE / 轮询 + 间隔), 可选过滤 (发件人 / 主题子串), 启停开关, 状态 (连接态 / 最近事件 / 最近错误), 触发记录列表, 通知与电池优化状态提示.
- [ ] (宿主) 定时任务子系统新触发器 "邮件到达": 选择脚本 + 插件守望 (由 `listTriggers` 列出) + 可选过滤; 宿主收到 `onMail` 后经既有任务调度器启动脚本, envelope JSON 作为 `engines.myEngine().execArgv.mail`; 宿主未运行时由插件以 PLUGIN 权限保护的显式 Intent (新 action `org.autojs.autojs6.action.MAIL_TRIGGER`, 形态同 `WAKE`) 拉起宿主; 去重 (同一账户 + 文件夹 + UID 只触发一次) 与节流 (`MIN_TRIGGER_INTERVAL_MS`).
- [ ] (宿主) 插件中心与定时任务 UI 文案 11 语言; `docs/dev/mail-plugin-protocol-v1.md` 增加 v2 章节 (触发器契约与降级规则).
- [ ] (测试) JVM: 去重 / 节流 / 过滤 / 触发记录; instrumentation: 前台服务启停, 通知权限缺失时的行为, 进程重建后守望恢复, 旧版宿主下 `openTrigger` 不可见; DEVICE 矩阵 (`docs/dev/p8-background-watch-evidence.md`): QQ + Gmail 各一, 灭屏 30 分钟 / 断网 5 分钟 / 杀宿主 / 杀插件 四种扰动, 记录触发延迟与电量 (`dumpsys batterystats`).
- [ ] (文档) 文档 / d.ts (`execArgv.mail` 类型) / README 守望章节 / changelog 10 语言.

验收条件: 两家服务商在两台设备上完成扰动矩阵; 契约 v2 协商在新旧版本组合下正确降级; 证据写入 `docs/dev/p8-background-watch-evidence.md`.

---

## P9: 内置 OAuth2 浏览器授权流程 (1.2.0)

目标: 用户在插件设置页经系统浏览器 (Custom Tabs) 完成 Google / Microsoft 授权, 刷新令牌加密保存, 会话自动续期; 脚本仍以别名连接, 不接触令牌. 前置 P4 设置页与 P7, 维护者已于 2026-09-18 拍板 (D31).

- [ ] (维护者) 注册 OAuth 客户端: Google (`https://mail.google.com/` scope, Android 客户端类型绑定包名与签名指纹, 发布前需通过敏感 scope 审核) 与 Microsoft Entra (公共客户端, 移动重定向 URI, scope `https://outlook.office.com/IMAP.AccessAsUser.All` / `POP.AccessAsUser.All` / `SMTP.Send` / `offline_access`); 客户端 ID 以 `resValue` 注入, 不含密钥.
- [ ] (插件) `OAuthProviders` (授权 / 令牌端点, scope, PKCE 参数, 数据文件) 与 `OAuthFlow`: `androidx.browser` Custom Tabs 打开授权页, 自定义 scheme (或 `https` App Link) 回调 Activity 接收 `code` 并校验 `state`, PKCE (S256) 换取 access / refresh token (HTTPS 只指向令牌端点, 本阶段同步更新 Manifest 注释与 `AGENTS.md` 第 6 节 "不发起 HTTP 请求" 的表述); 令牌与过期时间由 P4 的 `AccountStore` 加密保存 (`AccountRecord` 增加 `oauth` 字段); 会话建立时 access token 剩余不足 5 分钟则先刷新; 刷新失败 -> `AUTH_FAILED` 并在设置页标记 "需要重新授权".
- [ ] (插件) 设置页: 新增账户时提供 "使用 Google / Microsoft 账号登录" 路径, 展示授权状态与过期时间, "重新授权" / "撤销" 操作; 预设表 `gmail` / `outlook` / `office365` 的 `authHint` 更新为 "内置授权, 或 App Password / 脚本令牌".
- [ ] (宿主) 无 AIDL 变化 (别名会话在插件内解析秘密); `mail.accounts.list()` 元数据增加 `auth: 'oauth2'` 标记, 插件中心说明更新.
- [ ] (测试) JVM: PKCE / `state` / 令牌响应解析 / 过期判断 (本地假 HTTP 服务器); instrumentation: 回调 Activity 只接受匹配 `state` 的回调, 令牌加密存储往返, 撤销后不可解密; DEVICE: Gmail 与 Outlook.com 各完成一次 授权 -> 收信 -> 手工撤销 -> 自动刷新失败提示重新授权 (`docs/dev/p9-oauth2-evidence.md`).
- [ ] (文档) README 各语言 "使用 Google / Microsoft 账号" 章节与隐私说明 (令牌只存本机, 不经宿主与任何第三方服务器), changelog 10 语言; 若使用 App Link 则说明 `assetlinks.json` 的托管位置.

验收条件: 两家服务商的授权 / 刷新 / 撤销路径在两台设备上通过; P6 的凭据审计扩展到令牌; 证据写入 `docs/dev/p9-oauth2-evidence.md`.

---

## 附录 A: 脚本 API 草案

### A.1 命名与通用约定

- 全局对象 `mail` (别名 `$mail`); 实例类型 `MailClient`, `MailWatch`; 数据对象 `MailMessage`, `MailAttachment`, `MailAddress`, `MailFolder`, `MailSendResult`; 异常 `MailError`.
- 每个网络方法有同步版 `x(...)` 与 `xAsync(...)` (Promise); 参数与返回完全一致. 纯本地方法 (`providers.*`, `accounts.list`, `setDefault`, `close`, `MailWatch.stop`) 只有同步版.
- 收件人 / 地址参数统一接受: `'a@b.c'`, `'Name <a@b.c>'`, `{name, address}`, 以及以上任意项的数组.
- UID 参数统一接受: 单个 `number | string` (POP3 的 UIDL 为字符串), `MailMessage` 对象, 或它们的数组.
- 文件夹名为字符串, 默认 `'INBOX'`; 层级分隔符按服务器 (`/` 或 `.`) 原样.
- 日期参数接受 `Date` / 时间戳 / ISO 字符串; 返回值统一为 JS `Date` (D26).
- 所有方法在插件缺失 / 未启用 / 不兼容时抛 `MailError{code: 'PLUGIN_UNAVAILABLE'}`, `message` 为宿主既有文案.

### A.2 `mail.connect(options | alias)` 与 `MailAccountOptions`

```js
let client = mail.connect({
    provider: 'qq',                 // 可选, 见附录 C; 显式字段覆盖预设
    address: 'user@qq.com',         // 必填 (或 user + address 分开)
    user: 'user@qq.com',            // 可选, 缺省等于 address
    name: 'My Name',                // 可选, 发信显示名
    password: 'authorization-code', // 与 accessToken 二选一
    accessToken: '...',             // XOAUTH2 令牌
    tokenProvider: () => '...',     // 可选, 令牌过期时被调用一次
    receive: 'imap',                // 'imap' | 'pop3', 默认 'imap'
    imap: { host: 'imap.qq.com', port: 993, tls: 'ssl' },      // tls: 'ssl' | 'starttls' | 'none'
    pop3: { host: 'pop.qq.com', port: 995, tls: 'ssl' },
    smtp: { host: 'smtp.qq.com', port: 465, tls: 'ssl' },
    timeout: { connect: 15000, read: 60000, write: 60000 },
    tls: { trustAll: false },       // D25
    debug: false,                   // D28: 脱敏协议摘要输出到 console.verbose
    clientId: { name: 'AutoJs6', version: '6.8.0' }, // IMAP ID 命令, 163 需要
});
let saved = mail.connect('work');   // 插件设置页保存的别名 (P4)
```

### A.3 `MailClient` 方法表

| 方法 | 参数 | 返回 | op | 协议 |
| --- | --- | --- | --- | --- |
| `test()` | - | `{imap?, pop3?, smtp?: {ok, latencyMs, capabilities?, error?}}` | `session.test` | 全部 |
| `send(message, options?)` | `MailSendMessage`, `{saveToSent?}` | `MailSendResult {messageId, accepted[], rejected[]}` | `mail.send` | SMTP |
| `folders(options?)` | `{subscribedOnly?, status?}` | `MailFolder[] {name, path, delimiter, specialUse?, messages?, unseen?, children?}` | `folders.list` | IMAP |
| `createFolder(path)` / `deleteFolder(path)` / `renameFolder(path, newPath)` | | `MailFolder` / `boolean` / `MailFolder` | `folders.*` | IMAP |
| `fetch(options?)` | `{folder?, limit?, before?, after?, order?, unseenOnly?}` | `MailMessage[]` (envelope) | `messages.list` | IMAP / POP3 |
| `search(query, options?)` | `MailSearchQuery`, `{folder?, limit?, before?, fallback?: 'client' \| 'none'}` | `MailMessage[]` (envelope, 结果附 `fallback` 标记) | `messages.search` | IMAP (POP3 客户端过滤) |
| `get(uid \| message, options?)` | `{folder?, peek?: true, includeRaw?: false}` | `MailMessage` (含正文) | `messages.get` | IMAP / POP3 |
| `download(attachment \| {uid, partId}, target?, options?)` | 目标目录或文件路径, `{folder?, overwrite?, onProgress?}` | 落盘路径 `string` | `attachments.download` | IMAP / POP3 |
| `raw(uid, target, options?)` | | 落盘路径 | `messages.raw` | IMAP / POP3 |
| `setFlags(uids, flags, mode?)` | `flags: string \| string[]`, `mode: 'add' \| 'remove' \| 'set'` | 受影响 UID 数组 | `messages.setFlags` | IMAP |
| `markRead(uids)` / `markUnread(uids)` / `flag(uids)` / `unflag(uids)` | 语法糖 | 同上 | 同上 | IMAP |
| `move(uids, folder, options?)` / `copy(uids, folder, options?)` | `{from?}` | 目标 UID 数组 (有 UIDPLUS 时) 或 `true` | `messages.move` / `copy` | IMAP |
| `delete(uids, options?)` | `{folder?, expunge?: false}` | 受影响 UID 数组 | `messages.delete` | IMAP / POP3 |
| `expunge(folder?)` | | 清除数 | `messages.expunge` | IMAP |
| `append(folder, message, flags?)` | `MailSendMessage` | 新 UID | `messages.append` | IMAP |
| `watch(folder?, options?)` | `{mode?, pollIntervalMs?, fetchBody?, reconnect?}` | `MailWatch` | `watch` | IMAP / POP3 |
| `close()` | | - | `session.close` | 全部 |

`MailSendMessage`: `{to, cc?, bcc?, replyTo?, subject, text?, html?, attachments?: (string | {path, fileName?, mimeType?, contentId?, inline?})[], headers?, priority?, inReplyTo?, references?, date?}`.

### A.4 `MailMessage` / `MailAttachment` / `MailAddress`

```
MailMessage {
    uid: number | string, folder: string, messageId: string | null, inReplyTo: string | null, references: string[],
    subject: string, from: MailAddress | null, sender: MailAddress | null, replyTo: MailAddress[],
    to: MailAddress[], cc: MailAddress[], bcc: MailAddress[],
    date: Date | null, receivedDate: Date | null, size: number,
    flags: string[], seen: boolean, flagged: boolean, answered: boolean, draft: boolean, deleted: boolean,
    hasAttachments: boolean,
    // 以下仅在 get() 之后填充, 否则 bodyLoaded = false
    bodyLoaded: boolean, text: string | null, html: string | null, headers: { [name: string]: string[] },
    attachments: MailAttachment[], bodyTruncated: boolean,
    load(options?): MailMessage,   // 绑定到 client.get
}
MailAttachment { partId: string, fileName: string, mimeType: string, size: number, contentId: string | null, inline: boolean, download(target?, options?): string }
MailAddress { name: string | null, address: string, toString(): string }
```

### A.5 `MailSearchQuery`

```js
client.search({
    from: 'boss@example.com', to: '...', cc: '...', subject: '验证码', body: '...', text: '...',   // 子串, 大小写不敏感
    since: '2026-09-01', before: new Date(), sentSince: ..., sentBefore: ...,
    seen: false, flagged: true, answered: false, draft: false,
    larger: 1024, smaller: 10 * 1024 * 1024,
    header: { 'X-Mailer': 'foo' }, messageId: '<...>', uid: '100:*',
    and: [ {...}, {...} ], or: [ {...}, {...} ], not: {...},
});
```

### A.6 `MailWatch` 事件

| 事件 | 参数 | 说明 |
| --- | --- | --- |
| `message` | `(message: MailMessage, watch)` | 新邮件, `fetchBody: true` 时含正文 |
| `mode` | `('idle' \| 'poll')` | 推送模式切换 |
| `resync` | `(reason: string)` | 插件无法保证增量, 脚本应自行 `fetch` |
| `error` | `(err: MailError)` | 可恢复错误 (会继续重连) 或不可恢复 (随后 `close`) |
| `close` | `(reason: string)` | 已停止, 不再有事件 |

### A.7 示例

```js
// 发信
let client = mail.connect({ provider: '163', address: 'me@163.com', password: '授权码' });
let result = client.send({ to: 'you@example.com', subject: '报表', text: '见附件', attachments: ['/sdcard/report.xlsx'] });
console.log(result.messageId);

// 收未读并下载附件
client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => console.log(a.download(files.join(files.cwd(), 'mail-attachments'))));
    client.markRead(m);
});

// 监听
let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/验证码/.test(m.subject)) console.log(m.text); });
watch.on('error', e => console.warn(e.code, e.message));
setTimeout(() => { watch.stop(); client.close(); }, 10 * 60 * 1000);

// 异步
mail.setDefault(client);
mail.searchAsync({ subject: '发票', since: '2026-09-01' }).then(list => console.log(list.length));
```

---

## 附录 B: 契约草案 (`plugin-api/mail-api`)

### B.1 `Bundle` key

| key | 类型 | 用途 |
| --- | --- | --- |
| `KEY_ACCOUNT_JSON` | String | 账户非秘密字段 (A.2 去掉 `password` / `accessToken` / `tokenProvider`) |
| `KEY_SECRET_PASSWORD` | String | 密码 / 授权码, 只在 `openSession` 出现 |
| `KEY_SECRET_ACCESS_TOKEN` | String | XOAUTH2 令牌 |
| `KEY_ACCOUNT_ALIAS` | String | 别名形态 (P5), 与上面三者互斥 |
| `KEY_REQUEST_JSON` / `KEY_RESPONSE_JSON` / `KEY_EVENT_JSON` / `KEY_STATUS_JSON` / `KEY_PROGRESS_JSON` | String | 文档 |
| `KEY_HOST_VERSION_CODE` | Long | 宿主版本 (插件校验) |
| `KEY_GENERATION` | Long | watch 的 generation (由宿主放入 options) |

### B.2 op 表

| op | args | result | 描述符 | 协议 |
| --- | --- | --- | --- | --- |
| `session.test` | `{}` | 各协议结果 | - | 全部 |
| `folders.list` / `folders.status` / `folders.create` / `folders.delete` / `folders.rename` | 见 A.3 | | - | IMAP |
| `messages.list` / `messages.search` / `messages.get` | 见 A.3 / A.5 | envelope 数组 / 完整消息 | - | IMAP (POP3 子集) |
| `messages.raw` / `attachments.download` | `{folder, uid, partId?}` | `{bytes, fileName, mimeType}` | 1 个写端 (宿主管道) | IMAP / POP3 |
| `messages.setFlags` / `messages.move` / `messages.copy` / `messages.delete` / `messages.expunge` / `messages.append` | 见 A.3 | | `append` 可带 N 个只读附件 | IMAP |
| `mail.send` | `MailSendMessage` (附件以 `descriptorIndex` 引用) | `MailSendResult` | N 个只读附件 | SMTP |
| `session.close` | `{}` | | - | 全部 |

请求 `{id, op, args}`; 响应 `{id, ok: true, result} | {id, ok: false, error: {code, message, details?, retryable}}`; 进度 `{id, transferred, total?}`; 事件 `{type, seq, generation, folder, message?, mode?, reason?, error?}`; 状态 `{state: 'open' | 'closed', reason?, lastError?}`.

### B.3 线程与所有权

- `openSession` 同步返回, 不做网络; 首次网络在第一个 `call`.
- `call` 立即返回 `requestId`; 结果回调在插件 Binder 线程外的执行器线程发出; 每个 `requestId` 恰好一次 `onResult`, `onProgress` 零到多次且都在 `onResult` 之前.
- 描述符: 宿主创建并拥有, 随 `call` 传入后插件 `dup` 使用并在 `onResult` 前关闭自己的副本; 宿主在 `call` 返回后关闭传入的原件 (管道写端) 或在 `onResult` 后关闭 (只读附件).
- `watch` 回调为 `oneway`, 插件不得因宿主慢而阻塞; 队列策略见 D17.
- 会话与 watch 在宿主 death 时由插件自行清理 (`linkToDeath` 到回调 Binder).

### B.4 错误码

`INVALID_ARGUMENT`, `NO_DEFAULT_ACCOUNT`, `ACCOUNT_NOT_FOUND`, `PROVIDER_UNKNOWN`, `AUTH_FAILED`, `AUTH_MECHANISM_UNSUPPORTED`, `CONNECT_FAILED`, `TLS_FAILED`, `TIMEOUT`, `CANCELLED`, `FOLDER_NOT_FOUND`, `MESSAGE_NOT_FOUND`, `ATTACHMENT_NOT_FOUND`, `UNSUPPORTED_OPERATION`, `LIMIT_EXCEEDED`, `IO_FAILED`, `SESSION_CLOSED`, `WATCH_CLOSED`, `SEND_REJECTED`, `SERVER_ERROR`, `PLUGIN_UNAVAILABLE`, `INTERNAL`.

`retryable` 为 `true` 的默认集合: `CONNECT_FAILED`, `TIMEOUT`, `SESSION_CLOSED`, `SERVER_ERROR` (视服务器响应), `PLUGIN_UNAVAILABLE` (仅进程死亡分支).

### B.5 上限常量 (写入 `MailContract`)

| 常量 | 默认值 | 说明 |
| --- | --- | --- |
| `MAX_ENVELOPE_BYTES` | 512 KiB | 单次 Binder 的 JSON 文档 |
| `MAX_INLINE_BODY_BYTES` | 256 KiB | `messages.get` 内联 text + html 合计, 超出走 `messages.raw` |
| `MAX_ATTACHMENT_BYTES` | 200 MiB | 单附件下载 / 上传 |
| `MAX_ATTACHMENTS_PER_MESSAGE` | 64 | |
| `MAX_RECIPIENTS` | 500 | to + cc + bcc |
| `MAX_PAGE_SIZE` / `DEFAULT_PAGE_SIZE` | 1000 / 50 | `messages.list` / `search` |
| `MAX_CLIENT_FILTER` | 2000 | 客户端过滤扫描封数 |
| `MAX_DESCRIPTORS` | 64 | 单次 `call` |
| `MAX_CONCURRENT_CALLS` / `MAX_QUEUED_CALLS` | 4 / 32 | 每会话 |
| `MAX_WATCHES_PER_SESSION` | 4 | |
| `MAX_WATCH_QUEUE` | 256 | 超出发 `resync` |
| `MIN_POLL_INTERVAL_MS` / `DEFAULT_POLL_INTERVAL_MS` | 15000 / 60000 | |
| `IDLE_RENEW_MS` | 24 min | |
| `SESSION_IDLE_TIMEOUT_MS` | 10 min | 底层连接空闲断开 |
| `DEFAULT_CONNECT_TIMEOUT_MS` / `DEFAULT_READ_TIMEOUT_MS` | 15000 / 60000 | |
| `CALL_TIMEOUT_MS` (宿主侧) | 按 op: 元数据 30 s, 正文 120 s, 传输 = 大小 / 64 KiB/s 下限 5 min | |

---

## 附录 C: 服务商预设初表 (P2.1 落地时逐项核实)

| id | IMAP | POP3 | SMTP | 认证提示 | 备注 |
| --- | --- | --- | --- | --- | --- |
| `gmail` | imap.gmail.com:993 ssl | pop.gmail.com:995 ssl | smtp.gmail.com:465 ssl (587 starttls) | App Password 或 XOAUTH2 | 自动保存已发送; 文件夹 `[Gmail]/...` |
| `outlook` | outlook.office365.com:993 ssl | outlook.office365.com:995 ssl | smtp-mail.outlook.com:587 starttls | 仅 XOAUTH2 (IMAP 已禁基本认证) | Outlook.com 个人账户 |
| `office365` | outlook.office365.com:993 ssl | outlook.office365.com:995 ssl | smtp.office365.com:587 starttls | XOAUTH2 (SMTP 基本认证 2026-12 起默认禁用) | 租户策略可能禁用 |
| `qq` | imap.qq.com:993 ssl | pop.qq.com:995 ssl | smtp.qq.com:465 ssl | 授权码 | 已发送文件夹 `Sent Messages`; 非 ASCII SEARCH 不稳定 |
| `163` | imap.163.com:993 ssl | pop.163.com:995 ssl | smtp.163.com:465 ssl | 授权码 | 需 IMAP `ID` 命令; 不自动保存已发送 |
| `126` | imap.126.com:993 ssl | pop.126.com:995 ssl | smtp.126.com:465 ssl | 授权码 | 同 163 |
| `icloud` | imap.mail.me.com:993 ssl | - | smtp.mail.me.com:587 starttls | App-Specific Password | 无 POP3 |
| `yahoo` | imap.mail.yahoo.com:993 ssl | pop.mail.yahoo.com:995 ssl | smtp.mail.yahoo.com:465 ssl | App Password | |
| `sina` | imap.sina.com:993 ssl | pop.sina.com:995 ssl | smtp.sina.com:465 ssl | 授权码 | |
| `aliyun` | imap.aliyun.com:993 ssl | pop3.aliyun.com:995 ssl | smtp.aliyun.com:465 ssl | 密码 | 个人版 |

每个预设记录: `autoSavesSent`, `sentFolder`, `requiresClientId`, `authHint`, `docsUrl` (帮助页).

2026-09-18 核实 (P2.1): 表中 26 个主机全部 DNS 可解析且对应端口 TCP 可连 (`outlook.office365.com` 993 / 995, `smtp-mail.outlook.com` / `smtp.office365.com` / `smtp.mail.me.com` 587, 其余 993 / 995 / 465), 10 个帮助页 HTTP 200 (Apple 页跳转到 `support.apple.com/en-us/102525`); 已落地为 `mail-core/src/main/resources/providers.json` (version 1, 每项含 `domains` / `auth` / `notes`). `autoSavesSent` 的取值: Gmail / Outlook.com / Microsoft 365 / QQ 为 true (已知行为), 163 / 126 / iCloud 为 false, Yahoo 记为 true, Sina / Aliyun 记为 false 且 `sentFolder` 留空 (回退到 special-use); 后三家未经真实账户核实, P2.2 `saveToSent` 落地时复核 (取值错误的后果只是已发送副本缺失或重复).

2026-09-18 核实 (P2.2): QQ 的 `autoSavesSent = true` 经真机确认 (对刚经 SMTP 发出的邮件 APPEND `Sent Messages` 得到 `NO Mail has saved by smtp!`, 服务器按 Message-ID 去重); Gmail 沿用已知行为 (未追加, 未观察到重复); 两家的 `Drafts` / `[Gmail]/Drafts` APPEND 均返回 APPENDUID. Yahoo / Sina / Aliyun 仍未核实.

---

## 附录 D: 待决事项 (已于 2026-09-18 全部拍板, 结果见 D25-D32)

### Q1 (P1 前): 信封是否改用 TaggedWire

- 现状: D14 选 `Bundle` + JSON (与 MCP 家族一致); AI 家族用 `protocol-wire-api` 的 TaggedWire.
- 推荐: 维持 JSON. 邮件对象天然 JSON, 脚本侧零转换, 且 JSON 层可用 GreenMail 夹具直接快照测试.
- 拍板 (2026-09-18): 维持 JSON (D14).

### Q2 (P1 前): 插件是否默认启用

- 现状: `PluginDefaultEnabledPolicy` 除 YOLO 外全部默认启用.
- 推荐: 默认启用 (D19). 脚本本就具备网络与文件能力, 邮件不高于现有风险级别.
- 拍板 (2026-09-18): 默认启用 (D19).

### Q3 (P2 前): `tls.trustAll` 是否纳入 1.0.0

- 场景: 企业内网自签证书的邮件服务器.
- 推荐: 纳入, 默认 `false`, 文档明确风险, `test()` 结果与 `client.account` 标记 `insecure: true`; 不提供全局开关.
- 拍板 (2026-09-18): 按推荐纳入 1.0.0 (D25).

### Q4 (P3 前): 日期表示

- 选项: JS `Date` (推荐, 与 `files` / `device` 等模块一致) vs 毫秒时间戳 vs ISO 字符串.
- 拍板 (2026-09-18): JS `Date` (D26).

### Q5 (P4 后): Doze 下的网络冻结是否提供电池优化豁免引导

- 依据: P4 的灭屏 15 分钟矩阵结果.
- 选项: 不做 (文档说明) / 设置页提供 "忽略电池优化" 引导按钮 (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, 需 Manifest 权限, 商店政策风险不适用于侧载插件) / 交给宿主已有的电池优化引导.
- 拍板 (2026-09-18): 设置页提供 "忽略电池优化" 引导按钮 (D27); 由此设置页提前为 P4, Doze 矩阵 (P5) 只用于校准引导文案, 不再决定是否做按钮.

### Q6 (P5 前): 是否提供插件更新检查对话框

- 参照: Three-Stone-AI 的更新对话框 + 发行历史 Neutral 按钮.
- 推荐: 1.0.0 只做发行历史页, 更新检查沿用宿主插件中心的机制 (若已有).
- 拍板 (2026-09-18): 按推荐, 1.0.0 只做发行历史页 (D29).

### Q7 (P6 前): 是否允许脚本开启脱敏协议日志

- 场景: 用户排查 `AUTH_FAILED` / `SERVER_ERROR`.
- 推荐: 提供 `options.debug: true` 把插件侧脱敏后的协议摘要 (命令名 + 响应码 + 前 200 字符, 去掉 LOGIN / AUTH 行) 经 `onProgress` 回传到脚本 `console.verbose`; 不开放 Jakarta `mail.debug`.
- 拍板 (2026-09-18): 按推荐提供 `options.debug` 脱敏摘要 (D28).

### Q8 (P7 后): 是否排期 P8 后台守望

- 见附录 F.1.
- 拍板 (2026-09-18): 排期为 P8, 目标 1.1.0 (D30); 附录 F.2 同时拍板为 P9 (D31), F.3 不排期 (D32).

---

## 附录 E: 证据等级与退路

### E.1 证据等级

| 标签 | 可以证明 | 不能证明 |
| --- | --- | --- |
| `SOURCE` | 源码存在, 结构符合设计 | 编译或行为正确 |
| `JVM` | Android-free 逻辑的单元测试 (JUnit4, GreenMail 本地服务器) | Binder / 真实服务商 / 真机行为 |
| `ANDROID_BUILD` | `assembleDebug` / `testDebugUnitTest` / `lintDebug` / `assembleRelease` 通过 | 真机行为 |
| `BINDER` | 指定设备上的 instrumentation: 发现, 绑定, 往返, 敌意输入 | 网络与服务商行为 |
| `DEVICE` | 指定设备与 API 级别上, 对指定服务商 (名称) 完成真实操作 | 未列出设备 / API / 服务商 |
| `DOCS` | README (10 语言), changelog, 协议文档, 文档 / d.ts / Ace / 离线文档已同步且版本号已更新 | - |
| `RELEASE` | 签名 APK, CRC32 文件名, GitHub Release, 官方索引 receipt | 未明确覆盖的设备 / 服务商 |

条目勾选时在其后追加证据, 格式示例: `[x] ... (JVM: ImapMailboxTest 12 用例; DEVICE: Xiaomi 23046RP50C / API 35 + QQ, Sony G8441 / API 28 + 163, 2026-09-xx; commit abc1234)`.

当前可用设备池 (以当日 `adb devices -l` 为准): Xiaomi 23046RP50C (API 35), Sony G8441 (API 28), Sony XQ-AT72 (API 31), Redmi 22120RN86C (API 33), AVD API 24 / 33 / 36. 无真实账户时, DEVICE 级往返可用开发机上的 GreenMail standalone 完成 (`MailCoreDeviceTest` KDoc), 证据中注明 "本机 GreenMail" 而不是服务商名.

### E.2 D2 退路: `com.sun.mail:android-mail`

触发条件见 P0.2 决策点. 形态: 依赖换为 `com.sun.mail:android-mail:1.6.8` + `com.sun.mail:android-activation:1.6.8`, 包名回到 `javax.mail.*` (`:mail-core` 以接口隔离, `MailSessionFactory` / `ImapMailbox` 等只改导入), XOAUTH2 仍内建 (1.5.5+), 缺少 Jakarta 2.1 的新 API 但本 Roadmap 未依赖; 仓库名保持 `AutoJs6-Plugin-Angus-Mail` 需在 README 说明或在维护者同意后改名为 `AutoJs6-Plugin-Android-Mail` (改名规则见 `AGENTS.md` 第 2 节, 未发布前不留兼容层).

---

## 附录 F: 预留

### F.1 后台守望与新邮件触发脚本 (P8)

- 2026-09-18 拍板纳入排期 (D30), 条目已展开到 P8; 以下为原始形态说明.
- 插件前台服务 (`foregroundServiceType="dataSync"` 或 `specialUse`, 通知常驻) 持有指定账户的 IDLE 连接, 无脚本运行时也保持; 新邮件到达后经 `IMailSessionCallback.onStatus` 或新的 `IMailTriggerCallback` 通知宿主, 宿主以定时任务子系统的新触发器类型 "邮件到达" 启动指定脚本并把 envelope 作为 `engines.myEngine().execArgv`.
- 需要: 宿主定时任务的新触发器类型与 UI, 插件的账户选择与电量策略, Doze 豁免引导 (Q5), 开机自启决策.
- 契约预留: `MailCapabilityKeys.FEATURES` 中的 `backgroundWatch`, AIDL 末尾追加 `IMailPlugin.openTrigger(...)` 并升 `CONTRACT_VERSION`.

### F.2 内置 OAuth2 授权流程

- 2026-09-18 拍板纳入排期 (D31), 条目已展开到 P9; 以下为原始形态说明.
- 设置页以 Custom Tabs + PKCE 完成 Google / Microsoft 授权, 刷新令牌加密保存, 会话自动续期; 需要维护者注册 OAuth 客户端 (Google `https://mail.google.com/` scope 须通过应用审核, Microsoft 需 Azure 应用注册).
- 契约预留: 别名形态的会话已经在插件内解析秘密, 令牌刷新对宿主透明, 不需要 AIDL 变化.

### F.3 非标准协议

- 2026-09-18 拍板不排期 (D32); 保留接入点说明.
- Microsoft Graph / Gmail REST / Exchange EWS 可作为另一个 engine `mail` 的实现 (不同 `PLUGIN_ID`), 复用同一契约 op 表; `folders` / `uid` 语义映射由该实现负责.

---

## 附录 G: 参考

- `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (仓库约定)
- `D:/idea-projects/AutoJs6-Plugin-MCP-Server/ROADMAP.md` (路线图形态, 会话记录, AAR 锁, 回调 AIDL)
- `D:/idea-projects/AutoJs6-Plugin-OpenCC` (骨架, UI 套件, README / changelog 生成)
- `D:/idea-projects/AutoJs6-Plugin-Three-Stone-AI` (凭据加密存储, 设置页, 发行历史)
- `D:/idea-projects/AutoJs6/docs/dev/mcp-server-protocol-v1.md`, `ai-provider-protocol-v2.md`, `official-plugin-settings-contract-v1.md`
- `D:/idea-projects/AutoJs6/core/plugin/text/OpenccPluginHost.kt`, `runtime/api/augment/opencc/OpenCC.kt`, `augment/ai/AiStream.kt`, `core/plugin/explorer/archive/ArchiveEntryMaterializer.kt`
- Eclipse Angus Mail: `https://eclipse-ee4j.github.io/angus-mail/` (Android 页, OAuth2 页, 各 provider 属性文档), `https://github.com/eclipse-ee4j/angus-mail`
- Jakarta Mail 2.1 规范: `https://jakarta.ee/specifications/mail/2.1/`
- GreenMail: `https://github.com/greenmail-mail-test/greenmail`
- RFC 3501 (IMAP4rev1), RFC 2177 (IDLE), RFC 6851 (MOVE), RFC 4315 (UIDPLUS), RFC 6154 (SPECIAL-USE), RFC 1939 (POP3), RFC 5321 / 5322 (SMTP / 邮件格式), RFC 2045-2049 (MIME), RFC 2047 / 2231 (信头与参数编码), RFC 7628 (OAuth SASL)
- Google: `https://developers.google.com/workspace/gmail/imap/xoauth2-protocol`; Microsoft: Exchange Online 基本认证弃用公告 (2026-01-27 更新)

---

## 会话记录

### 2026-09-18

- 完成: 宿主插件链路 (契约 / 客户端 / augment / 插件中心 / 大负载 / 回调 AIDL) 探查, 文档 / d.ts / Ace / 离线文档接入方式探查, 参照插件 (OpenCC / Three-Stone-AI / MCP Server) 骨架与凭据存储探查; Angus Mail / MailCore2 / GreenMail 与各服务商认证现状核实; 维护者拍板 D1-D11 (命名, 底层库, 协议范围, 认证, API 形态, 调用风格, 监听模型, 凭据, 附件, 预设, 正文形态); 派生 D12-D24; 本 Roadmap 初稿.
- 未做: 仓库骨架与 `git init` (P0.1), Angus Mail spike (P0.2), 任何宿主改动; 平台版本插件 1.8.2 的公共仓库可解析性 (plugins.gradle.org 访问 502) 与 GreenMail IDLE 支持未核实, 均留在 P0.
- 待维护者: 提供至少一个国内服务商 (QQ 或 163) 与一个 Gmail (App Password) 测试账户到本地 `mail-test-accounts.properties` (git 忽略); 附录 D 的 Q1 / Q2 在 P1 前拍板.
- 下次会话建议起点: P0.1 全部条目 + P0.2 spike; spike 通过后同一会话可开始 P1.1 契约模块.

### 2026-09-18 (第二次会话, P0)

- 拍板: 维护者回复附录 D 的 Q1-Q8 与附录 F.1-F.3, 回填为 D25-D32; 阶段顺序调整 (设置页 P4, 监听 P5), P8 (后台守望, 1.1.0) 与 P9 (内置 OAuth2, 1.2.0) 展开.
- 完成: P0.1 全部条目 (Gradle / build-logic / Manifest / 身份常量 / 10 语言资源 / 图标脚本 / README 与 changelog 生成 / `AGENTS.md` / `THIRD_PARTY_NOTICES.md` / JVM 与 instrumentation 测试 / CI 工作流 / `git init` 与 6 次提交); P0.2 的 JVM spike (`:mail-core` 17 用例全绿), R8 release 构建 (APK 403,225 B), DEVICE 往返 (AVD API 24 与 Xiaomi API 35 对本机 GreenMail, debug 与 release 构建), XOAUTH2 JVM 往返, D2 决策成立; 证据 `docs/dev/p0-spike-evidence.md`.
- 未做: 真实服务商往返 (无测试账户, P0.2 第 2 条保持打开), 真实令牌的 XOAUTH2, 空闲 PSS; 宿主任何改动 (宿主仓库同期有其它会话在提交, HEAD 从 74138d9cb 经 c0d5fecdb 移到 cb9502520, `plugin-api/common-plugin-api` 在此期间无改动, 本会话只用 `:plugin-api:common-plugin-api:assembleRelease` 产出 AAR); CI 未在 GitHub 上运行 (仓库未推送); `REQUIRED_HOST_VERSION` 暂为 5281.
- 待维护者: `mail-test-accounts.properties` (QQ 或 163 授权码 + Gmail App Password); GitHub 仓库创建与推送时机; P1.4 回填 `REQUIRED_HOST_VERSION`.
- 下次会话建议起点: P1.1 契约模块 (宿主 `plugin-api/mail-api`) + P1.2 宿主客户端; 若维护者已提供测试账户, 先补跑 P0.2 第 2 条的真实服务商往返并关闭它.

### 2026-09-18 (第三次会话, P1)

- 完成: P1.1 契约模块 `plugin-api/mail-api` (六个 AIDL, `MailContract` / `MailActions` / `MailIds` / `MailCapabilityKeys` / `MailErrorCodes`, 10 个 JVM 用例); P1.2 宿主客户端 `core/plugin/mail` (`MailPluginHost`, `MailSessionClient`, `MailAttachmentSink` / `MailAttachmentSource` / `MailFileNames`, `MailWatchBridge`, `MailJson`, `MailErrorMapper`, `MailAccountSpec`, `MailBinders.kt` 适配, 63 个 JVM 用例) 与插件中心四处注册; P1.4 协议文档 `docs/dev/mail-plugin-protocol-v1.md`, 宿主 `VERSION_BUILD` 5282 回填与 10 语言 changelog; 宿主提交 34d2c8fcd (只暂存本会话路径; 同期 autojs6-a9 会话在同一工作树提交了体积工作 b90ab13d0, 两个 Gradle 同时跑会互相破坏增量状态, 已协调串行). 插件侧: 暂存 `mail-api.aar` (sha256 7b7f537a..., `common-plugin-api.aar` 与 c0d5fecdb 构建逐字节相同, 同一锁文件), `REQUIRED_HOST_VERSION` 5281 -> 5282 全仓库同步, `binder/MailPluginBinder` (`IMailPlugin.Stub`) + `PlaceholderMailSession` 替换占位 Binder, 能力 Bundle 补齐 `MailCapabilityKeys`; instrumentation `AngusMailPluginContractTest` (3 用例, 含 `mailServiceAnswersTheContractBinder`) 在 AVD API 24 debug / release (`-PandroidTestRelease`) 与 Xiaomi API 35 debug 通过 (DEVICE, 同进程 Binder).
- 未做: P1.3 宿主 <-> 插件跨进程往返 (需安装宿主 APK 且插件有真实 `session.test`, 留到 P2 末尾); 宿主全量 `testAppDebugUnitTest` 未由本会话独立跑完 (并行会话跑过一次, 见 P1.4 注); 插件中心在设备上列出 / 探测本插件未人工核对 (宿主 debug APK 已产出但未安装).
- 待维护者: `mail-test-accounts.properties` (同前); GitHub 仓库创建与推送时机; 宿主 34d2c8fcd 的推送.
- 下次会话建议起点: P2.1 (账户 JSON -> `MailAccount`, 预设表, 会话路由替换 `PlaceholderMailSession`), 并在 P2 中顺手关闭 P1.3 的跨进程往返; 若维护者提供了测试账户, 先关闭 P0.2 第 2 条.

### 2026-09-18 (第四次会话, P2.1)

- 完成: P2.1 全部六项. `:mail-core` 新增 `error/` (`MailErrorCode` 镜像, `MailException`, `Redactor`, `ExceptionMapper`), `MailLimits` 镜像, `account/` (`MailAccountOptions` + `SecretKind`, `ProviderPreset` / `ProviderPresets` + `providers.json`), `json/` (`MailJson`, `SessionTestResult` 等文档模型), `session/` (`ConnectionGuard`, `ProtocolTrace`, `MailSession`; `MailSessionFactory` / `MailSessionProperties` / `ImapMailbox` / `Pop3Mailbox` / `SmtpSender` 扩展); `MailAccount` 改为 `MailTimeouts` / `receive` / `displayName` / `clientId` / `debug` / `provider`. `app` 侧: `PlaceholderMailSession` 由 `MailSessionBinder` + `RequestRouter` (op 表, 目前 `session.test` / `session.close`, 其余契约 op 返回 `UNSUPPORTED_OPERATION`) 替换, `openSession` 经 `MailAccountOptions` 解析并在密钥缺失 / 非法组合时以 closed 状态拒绝, `listProviders` 返回预设目录 JSON, `PROVIDERS_VERSION` 绑定到目录版本; 宿主 death 时 `linkToDeath` 关闭会话. JVM: `:mail-core:test` 61 用例, `app` 单元测试 18 用例 (新增 `MailCoreContractParityTest` 4: 错误码 / 上限 / 枚举 id / 能力值与 `mail-api` 契约一致; `RequestRouterTest` 3: op 表快照). 设备结果见下.
- 未做: P2.2 起的全部 op; P1.3 跨进程往返 (宿主 APK 仍未安装到设备; 本会话的 instrumentation 仍是同进程 Binder, 但 `session.test` 已经过真实 `IMailSession.call` + `onProgress` + `onResult` 往返); 真实服务商往返 (无 `mail-test-accounts.properties`); Gmail 需改用 XOAUTH2 令牌或另一账户 (维护者账户无法进入 App passwords 页, 见维护者反馈).
- 待维护者: 测试账户 (QQ 或 163 授权码; Gmail 侧改为 OAuth Playground 的 1 小时访问令牌或开启两步验证后的 App Password); GitHub 远端尚未配置 (`git remote -v` 为空), 配置后随时可推.
- 下次会话建议起点: P2.2 发信 (`OutgoingMessage` 扩展 + `saveToSent`), 之后 P2.3; 每完成一段就把 op 注册到 `RequestRouter.HANDLERS` 并让 `RequestRouterTest` 的快照跟着走.
### 2026-09-18 (第五次会话, P2.2)

- 完成: P0.2 第 2 条 (真实服务商往返: QQ 授权码在 Redmi 22120RN86C / API 33, Gmail OAuth Playground 令牌 XOAUTH2 在 Sony XQ-AT72 / API 31; 维护者已填写 `mail-test-accounts.properties`, 运行脚本 `.python/run_real_account.py` 屏蔽全部密钥); P2.2 全部三项. `:mail-core` 新增 `message/` (`OutgoingMessage`, `AttachmentSource`, `OutgoingMessageParser`, `MessageComposer`, `MimeTypes`), `query/FlagMapper`, `json/SendResult` (`AppendResult`), `MailSession.send` / `append` / `resolveSentFolder`, `ImapMailbox.folderExists` / `findSpecialUse` / `append`, `SmtpSender.send` 返回 `SentMessage`; app 侧 `DescriptorSource`, `RequestRouter` 注册 `mail.send` / `messages.append` (路由后再解析描述符), `MailSessionBinder` 校验并在 `onResult` 前关闭描述符副本. JVM: `:mail-core:test` 85 用例, app 21 用例. 设备: AVD API 24 debug 与 release (R8) 6/6 (1 skip, 含描述符所有权用例); 真机 QQ_A -> QQ_B 带附件往返 (对端 1535 ms 收到) 与 Gmail 带附件发送, 两家各 APPEND 一封草稿 (UID 2 / 327); 证据 `docs/dev/p2-core-evidence.md`. 决策 D33 (自动保存的服务商不追加副本, `sentCopy` 四态) 与 D34 (去掉 `mail.mime.allowutf8`). 远端 `origin` 已由维护者建立, 本会话提交后推送.
- 未做: P2.3 起的 op (`messages.raw` / `attachments.download` 的写端描述符走 `OPS_WITH_SINK`, 路由已按此预留); P1.3 跨进程往返 (仍是同进程 Binder); 宿主协议文档需补 `MailSendResult` 的 `savedToSent` / `sentCopy` / `sentFolder` / `saveError` / `elapsedMs` 与 `messages.append` 结果 `{folder, uid?}` (宿主暂不改动, 等 P2 收尾一并同步); Yahoo / Sina / Aliyun 的 `autoSavesSent` 仍未核实; Gmail 已发送副本的存在性待 P2.3 `folders.list` / `messages.list` 后核对.
- 下次会话建议起点: P2.3 收信, 搜索, 正文与附件 (IMAP): `folders.*`, `messages.list` / `search` / `get` / `raw` / `setFlags` / `move` / `copy` / `delete` / `expunge`, `attachments.download` (写端描述符), 每个 op 落地即注册到 `RequestRouter.HANDLERS`; QQ 的 SEARCH 非 ASCII 不可靠 (预设 notes) 需走客户端过滤.
