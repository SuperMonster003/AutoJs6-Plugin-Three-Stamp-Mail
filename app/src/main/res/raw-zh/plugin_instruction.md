3-Stamp Mail 为 AutoJs6 脚本提供全局对象 `mail`, 通过 IMAP, POP3 与 SMTP 发送邮件, 搜索和读取邮件, 下载附件, 管理标记与文件夹, 以及监听新邮件. 账户可以保存在插件内, 脚本按别名连接.

### 开始使用

1. 在服务商的网页设置中开启 IMAP 或 POP3 与 SMTP, 并取得授权码 (QQ, 163, 126, 新浪) 或应用专用密码 (Gmail, iCloud, Yahoo); 登录密码本身通常不被接受.
2. 从启动器打开 3-Stamp Mail, 在 "已保存账户" 页点按 "添加账户", 选择 "服务商", 填写邮箱地址, "别名" 与授权码, 然后保存.
3. 点按账户右侧的菜单按钮, 选择 "测试连接", 确认服务器可以连通.
4. 在 AutoJs6 脚本中用 `mail.connect('别名')` 连接账户, 用法见下文.

### 主要功能

- **收发与管理**: 发送带附件的邮件, 按主题, 日期等条件搜索邮件, 读取正文, 下载附件, 标记已读, 管理文件夹.
- **浏览器登录**: Gmail, Outlook.com 与 Microsoft 365 账户可以把 "认证方式" 设为 "使用 Google 账号登录 (浏览器)" 或 "使用 Microsoft 账号登录 (浏览器)", 在浏览器中登录后保存, 无需授权码.
- **后台守望**: 在 "设置" > "后台守望" 中点按 "添加守望" 并选择账户与文件夹, 插件会在没有脚本运行时保持连接; 再在 AutoJs6 中长按脚本, 选择 "定时任务", 在 "广播触发任务" 中选择 "邮件到达时 (邮件插件后台守望)", 之后每封新邮件到达时都会运行该脚本.
- **账户安全**: 密码与令牌加密保存在插件内, 脚本只按别名连接, 读不到它们.

### 在脚本中使用

在 AutoJs6 脚本中直接使用全局对象 `mail`, 无需导入. 下例以别名连接已保存的账户, 发送邮件, 读取未读邮件并监听收件箱:

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

- 也可以不保存账户, 直接传入账户信息, 例如 `mail.connect({ provider: 'qq', address: 'me@qq.com', password: '授权码' })`. 每个联网方法都有返回 Promise 的 Async 版本.
- Node.js 脚本用 `require('autojs6:mail')` 加载, Bun 脚本通过 `host.mail` 使用并需单独授权 `mail.control`, 调用均返回 Promise.
- 由后台守望运行的脚本通过 `engines.myEngine().execArgv.mail` 读取到达的邮件.

### 运行要求

- Android 7.0 或更高版本, AutoJs6 6.8.0 或更高版本, 且本插件在 "插件中心" 中处于启用状态.
- 邮件到达时运行脚本需要支持后台守望的 AutoJs6 版本; 不支持时 "后台守望" 页会给出提示.
- 网络权限只用于连接你配置的邮件服务器, 以及浏览器登录时向 Google 或 Microsoft 换取和续期令牌.
- 独立签名的打包应用只能使用脚本自行传入的账户信息; 已保存账户与后台守望只对 AutoJs6 开放.

### 常见问题

**连接时提示认证失败**

多数服务商不接受登录密码. 确认已在服务商的网页设置中开启 IMAP 或 POP3 与 SMTP, 改用授权码或应用专用密码, 再用 "测试连接" 检查.

**息屏后监听或守望中断**

Android 会在息屏一段时间后冻结后台应用的网络. 在 "设置" > "电池优化" 中为插件申请排除即可保持连接; 这一步是可选的.

**脚本提示缺少插件时的处理**

确认本插件已安装, 并在 AutoJs6 的 "插件中心" 中处于启用状态, 然后重新运行脚本, 无需重启 AutoJs6.

全部方法与选项见 [AutoJs6 文档 > 邮件](https://docs.autojs6.com/#/mail), 各服务商的设置方法与其他信息见 [项目主页](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail).
