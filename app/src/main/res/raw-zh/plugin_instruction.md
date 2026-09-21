Angus Mail 为 AutoJs6 脚本提供全局对象 `mail`, 用于发送邮件, 列出和搜索邮箱, 读取正文, 下载附件, 管理标记与文件夹, 以及监听文件夹中的新邮件. 它基于 Jakarta Mail 的参考实现 [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, 通过 TLS 使用 IMAP, POP3 和 SMTP 协议.

版本 1.1.0 新增后台守望 (路线图 P8): 守望页面, 前台服务与 AutoJs6 的 "邮件到达时" 任务; P0 至 P7 的全部条目已随 1.0.0 与 1.0.1 发布, 证据见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). 需要 AutoJs6 6.8.0 (构建 5282) 或更高版本; "邮件到达时" 任务需要携带邮件契约版本 2 的宿主构建; 脚本 API 的完整参考见 [AutoJs6 文档](https://docs.autojs6.com/#/mail).

### 使用方法

1. 在安装了 AutoJs6 构建 5282 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `Angus Mail` 已被识别并启用它.
3. 准备账户: 在邮件服务商的网页端开启 IMAP 或 POP3 与 SMTP, 并取得授权码 (QQ, 163, 126, Sina), 应用专用密码 (Gmail, iCloud, Yahoo) 或 OAuth 2.0 访问令牌 (Outlook.com); 登录密码本身通常不被接受.
4. 在脚本中调用 `mail.connect(...)`, 或在插件设置页 (插件的启动器图标, 或 AutoJs6 开发者选项 > 邮件账户设置) 保存账户后以别名连接.
5. 要在新邮件到达时运行脚本而无需常驻脚本: 在插件的守望页面 (设置 > 守望: 账户别名, 文件夹, 模式, 过滤) 添加守望, 按提示允许通知, 然后在 AutoJs6 中创建任务 (长按脚本 > 定时任务 > 广播触发 > 邮件到达时) 并选择守望; 该任务需要携带邮件契约版本 2 的 AutoJs6 构建.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md).
