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

版本 1.0.0 处于开发阶段: 仓库骨架, 带本地服务器测试的邮件核心, 以及供 AutoJs6 插件中心识别的插件身份已经就绪; Binder 契约, 脚本 API 与设置页按 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) 的阶段推进. 需要 AutoJs6 6.8.0 (构建 5282) 或更高版本.

******

### 功能

******

插件提供以下能力:

- 发信: 纯文本或 HTML, 多个收件人, 附件与内联图片, 自定义信头与优先级; 服务商不自动保存已发送邮件时由插件写入服务器.
- 收信: 按页列出文件夹, 在服务器端搜索 (服务商拒绝非 ASCII 搜索时回退到客户端过滤), 读取文本与 HTML 正文, 并把附件直接下载到脚本工作目录.
- 整理: 标记已读或星标, 移动, 复制, 删除, 清除, 以及创建, 重命名或删除文件夹; POP3 账户获得只读子集.
- 监听: 在脚本运行期间通过 IMAP IDLE 接收新邮件事件, 服务器或 POP3 账户不支持时回退为轮询.
- 服务商: 内置 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 和 Aliyun 预设, 自动填充主机, 端口与加密方式; 任何字段都可为其他服务器覆盖.
- 认证: 密码与服务商授权码, 或由脚本提供并附带刷新回调的 XOAUTH2 访问令牌.

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5282 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `Angus Mail` 已被识别并启用它.
3. 准备账户: 在邮件服务商的设置中开启 IMAP 或 POP3, 并获取授权码或应用专用密码 (QQ, 163, 126, Gmail, iCloud), 或 OAuth 2.0 访问令牌 (Outlook.com).
4. 在脚本中调用 `mail.connect(...)`, 或在插件设置页保存账户后以别名连接.

******

### 快速开始

******

一个发送报表, 读取带附件的未读邮件并等待验证码的脚本:

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

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 插件不导出其他组件.
- INTERNET 权限只用于脚本指定服务器的 IMAP, POP3 和 SMTP 连接; 插件不发起其他请求, 也不收集任何数据.
- 密码与令牌从脚本到插件经 Binder 的专用字段传递, 不会出现在日志, JSON 文档, 错误消息或崩溃报告中, 且只在会话生命周期内驻留内存. 设置页保存的账户由 Android Keystore 密钥加密, 并排除在备份之外.
- 连接默认使用 TLS (按服务商要求选择 SSL 或 STARTTLS); 明文连接与自签名证书必须为每个账户显式声明.

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

_2026/09/18_

- `提示` P0 开发预览: 仓库骨架, 带本地服务器测试的邮件核心, 以及供 AutoJs6 插件中心识别的插件身份. Binder 契约, 脚本 API 与设置页按 ROADMAP.md 的阶段推进.
- `新增` 插件标识 `angus-mail` (engine `mail`), 含 INFO 服务, Wake Activity 以及 `org.autojs.plugin.MAIL` 服务; 其 `IMailPlugin` Binder 应答插件信息, 能力, 服务商与已保存账户列表以及会话信封 (具体操作随 P2 落地)
- `新增` 基于 Eclipse Angus Mail 的邮件核心: IMAP / POP3 / SMTP 的会话属性 (SSL 或 STARTTLS), 密码与 XOAUTH2 认证, SMTP 发信与 IMAP 收件箱列表, 已在本地 GreenMail 服务器上验证
- `新增` 邮件核心账户层 (路线图 P2.1): 账户选项支持 Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina 与 Aliyun 的服务商预设, 按协议区分的超时, `tls.trustAll`, IMAP `ID` 命令与脱敏的 `debug` 协议摘要; 会话懒连接, 空闲时断开并在断线后重连; `session.test` 经 Binder 返回各端点的能力与往返耗时
- `新增` 发信 (路线图 P2.2): `mail.send` 支持 to / cc / bcc / replyTo, 纯文本与 HTML 正文 (`multipart/alternative`), 经宿主传入的文件描述符读取的附件与内联图片 (`multipart/mixed` / `multipart/related`), 自定义头, 优先级, `inReplyTo` / `references` 与日期; 收件人, 附件与头的上限, 拒绝头注入; `saveToSent` 仅在服务商不自动保存时经 IMAP 追加副本; `messages.append` 写入草稿并返回 UID; 已在真机上对 QQ 邮箱与 Gmail 验证
- `新增` 10 种语言的 README, 插件中心说明与更新日志
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
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
