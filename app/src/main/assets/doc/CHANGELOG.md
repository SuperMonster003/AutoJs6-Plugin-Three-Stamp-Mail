******

### 发行历史

******

# v1.0.0

###### 2026/09/18

* `提示` P0 开发预览: 仓库骨架, 带本地服务器测试的邮件核心, 以及供 AutoJs6 插件中心识别的插件身份. Binder 契约, 脚本 API 与设置页按 ROADMAP.md 的阶段推进.
* `新增` 插件标识 `angus-mail` (engine `mail`), 含 INFO 服务, Wake Activity 以及 `org.autojs.plugin.MAIL` 服务; 其 `IMailPlugin` Binder 应答插件信息, 能力, 服务商与已保存账户列表以及会话信封 (具体操作随 P2 落地)
* `新增` 基于 Eclipse Angus Mail 的邮件核心: IMAP / POP3 / SMTP 的会话属性 (SSL 或 STARTTLS), 密码与 XOAUTH2 认证, SMTP 发信与 IMAP 收件箱列表, 已在本地 GreenMail 服务器上验证
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `依赖` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) 及 Angus Activation 2.0.3 与 Jakarta Activation API 2.1.4
* `依赖` 附加 GreenMail 2.1.13 用于 JVM 邮件核心测试 (仅测试范围)
* `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为共享插件契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 附加 `mail-api.aar` (AutoJs6 模块 `plugin-api/mail-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为邮件 Binder 契约 (六个 AIDL 接口, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), 并在 `locks/host-api-aars.lock` 中锁定哈希
