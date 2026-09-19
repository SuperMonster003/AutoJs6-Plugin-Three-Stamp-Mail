******

### 发行历史

******

# v1.0.0

###### 2026/09/18

* `提示` P0 开发预览: 仓库骨架, 带本地服务器测试的邮件核心, 以及供 AutoJs6 插件中心识别的插件身份. Binder 契约, 脚本 API 与设置页按 ROADMAP.md 的阶段推进.
* `新增` 插件标识 `angus-mail` (engine `mail`), 含 INFO 服务, Wake Activity 以及 `org.autojs.plugin.MAIL` 服务; 其 `IMailPlugin` Binder 应答插件信息, 能力, 服务商与已保存账户列表以及会话信封 (具体操作随 P2 落地)
* `新增` 基于 Eclipse Angus Mail 的邮件核心: IMAP / POP3 / SMTP 的会话属性 (SSL 或 STARTTLS), 密码与 XOAUTH2 认证, SMTP 发信与 IMAP 收件箱列表, 已在本地 GreenMail 服务器上验证
* `新增` 邮件核心账户层 (路线图 P2.1): 账户选项支持 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 与 Aliyun 的服务商预设, 按协议区分的超时, `tls.trustAll`, IMAP `ID` 命令与脱敏的 `debug` 协议摘要; 会话懒连接, 空闲时断开并在断线后重连; `session.test` 经 Binder 返回各端点的能力与往返耗时
* `新增` 发信 (路线图 P2.2): `mail.send` 支持 to / cc / bcc / replyTo, 纯文本与 HTML 正文 (`multipart/alternative`), 经宿主传入的文件描述符读取的附件与内联图片 (`multipart/mixed` / `multipart/related`), 自定义头, 优先级, `inReplyTo` / `references` 与日期; 收件人, 附件与头的上限, 拒绝头注入; `saveToSent` 仅在服务商不自动保存时经 IMAP 追加副本; `messages.append` 写入草稿并返回 UID; 已在真机上对 QQ 邮箱与 Gmail 验证
* `新增` 收信 (路线图 P2.3): `folders.list` (树形, special-use 角色取自 LIST / XLIST 属性或约定名称, 可选计数), `folders.status` / `create` / `delete` / `rename`; `messages.list` 以 UID 为游标 (`before` / `after`), 支持 `order` 与 `unseenOnly`, 只取信封 (`hasAttachments` 由 BODYSTRUCTURE 判定); `messages.search` 把查询 JSON (`from` / `to` / `subject` / `body` / `text` / 日期 / 标记 / 大小 / `header` / `messageId` / `uid` 与 `and` / `or` / `not`) 编译为 IMAP SEARCH, 服务器拒绝时在客户端过滤 (`fallback`); `messages.get` 返回纯文本与 HTML 正文 (只有 HTML 的邮件派生文本), 全部头, 带 `partId` 的附件列表, 内联预算 (`bodyTruncated` / `bodyParts`) 与 `includeRaw`; `attachments.download` 与 `messages.raw` 流式写入宿主描述符并报告进度; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 字符集恢复覆盖 GBK / GB 18030 / ISO-2022-JP, 未声明与未知字符集, 原始 8 位头与 RFC 2231 / 2047 文件名, 文件名净化; IMAP `ID` 命令在每条连接上发送 (163 / 126 拒绝未识别的连接), 服务器不能解析 `UID EXPUNGE` 时改用整夹 `EXPUNGE`; 已在真机上对 QQ 邮箱, 163 邮箱与 Gmail 验证
* `新增` POP3 (路线图 P2.4): `receive: "pop3"` 的账户通过同一组操作读取唯一的 `INBOX`, `uid` 为 UIDL 字符串: `folders.list` 只返回 `INBOX` (可带计数), `messages.list` 按 UIDL 游标分页且只取信头 (`TOP`), `messages.search` 在客户端按信头由新到旧过滤, 凑够 `limit` 条即停 (最多 200 个候选, 每个一次 `TOP` 往返), `messages.get` / `messages.raw` / `attachments.download` 下载整封邮件, `messages.delete` 发 `DELE` 并在邮箱关闭时生效; 标记, 移动, 复制, expunge, 追加, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` 与正文搜索在连接之前即返回 `UNSUPPORTED_OPERATION`; 已在真机上对 QQ 邮箱验证
* `新增` Binder 会话控制 (路线图 P2.5): 只有已安装且与本插件同签名的 AutoJs6 宿主能打开会话或列出已保存账户 (否则 `SecurityException`, 规则与 MCP Server 插件一致); 请求与响应信封不超过 `MAX_ENVELOPE_BYTES`, 错误消息不超过 `MAX_ERROR_MESSAGE_BYTES`; 每个会话顺序执行调用, 执行中的调用之后最多排队 `MAX_QUEUED_CALLS` 个, 再多则 `LIMIT_EXCEEDED`; `cancel` 立即应答排队中的调用, 并通过关闭套接字打断执行中的调用, 服务器无响应时不再等到读超时; `close` 让全部待处理调用应答 `SESSION_CLOSED`; `getStatus` 报告 `queued` 与 `active`; 操作表标明每个操作支持的收信协议, POP3 账户在解析参数前即被拒绝; 能力集宣告 `append` 与 `clientSearchFallback`
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `修复` 服务商预设 (路线图 P3.2): 163 邮箱与 126 邮箱会在服务器端保存每封经 SMTP 发出的邮件, 两者的 `autoSavesSent` 改为 true, 默认 `saveToSent` 不再向 `已发送` 追加第二份副本 (真实 163 账户核实: `saveToSent: false` 发出的邮件数分钟后出现在已发送文件夹)
* `优化` 错误映射: POP3 服务器在登录后因账户未开启 POP 访问而拒绝邮箱 (Gmail 对 STAT 答 `[SYS/PERM] Your account is not enabled for POP access`) 时, 现在得到说明原因的 `UNSUPPORTED_OPERATION`, 而不是可重试的 `IO_FAILED` "I/O failed"
* `优化` 服务商预设: Sina 邮箱补上已发送文件夹名 (`已发送`), 并注明服务器不保存已发邮件副本且拒绝 IMAP CREATE (文件夹只能在网页端创建); 126 邮箱服务器保存已发邮件副本已用真实账户验证
* `依赖` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 与 Jakarta Activation API 2.1.4
* `依赖` 附加 GreenMail 2.1.13 用于 JVM 邮件核心测试 (仅测试范围)
* `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `mail-api.aar` (AutoJs6 模块 `plugin-api/mail-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为邮件 Binder 契约 (六个 AIDL 接口, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 并在 `locks/host-api-aars.lock` 中锁定哈希
