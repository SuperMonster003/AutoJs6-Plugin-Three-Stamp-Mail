# AutoJs6-Plugin-Three-Stamp-Mail AGENTS.md

本文件是本仓库的工程约定, 由 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (AutoJs6 新插件仓库参考规范) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么".

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库不包含原生库, 模型, 上游源码快照或 ABI 拆分, 参考规范中对应的 CONDITIONAL 条款不适用 (见第 5.4 节的省略理由).
- 本仓库会发起出站网络连接 (IMAP / POP3 / SMTP), 保存用户凭据 (路线图 P4 的账户存储), 拥有独立设置页与内置发行历史 (P4), 并含纯 JVM 模块 `:mail-core` 与 GreenMail 测试; 对应条款见第 9, 14, 15 节.

## 2. 仓库身份

下列值在 Gradle, Manifest, Kotlin 常量 (`ThreeStampMailPlugin`), 资源, 文档 (`.readme/common.json`), 测试和宿主注册信息中 MUST 完全一致. 修改任一值时同步修改全部位置, 并运行 `ManifestContractTest` 与 `ThreeStampMailPluginRuntimeInfoTest`.

| 项目 | 值 |
|---|---|
| 仓库与目录名 | `AutoJs6-Plugin-Three-Stamp-Mail` |
| `rootProject.name` | `autojs6-plugin-three-stamp-mail` |
| 应用标题 (不可翻译) | `3-Stamp Mail` |
| `applicationId` / namespace | `io.github.supermonster003.autojs6.plugin.three.stamp.mail` |
| 插件 ID / engine / variant | `three-stamp-mail` / `mail` / `default` |
| 脚本全局对象 | `mail` (宿主侧, 路线图 D1 / P3) |
| Binder 服务类 | `ThreeStampMailPluginService` (默认进程) |
| 服务发现 action / category | `org.autojs.plugin.MAIL` / `mail` |
| INFO 服务 | `ThreeStampMailPluginInfoService`, action `org.autojs.plugin.INFO`, category `mail` |
| 专用 API | `mail-api` (宿主 `plugin-api/mail-api`, AIDL 包 `org.autojs.plugin.mail.api`, Binder descriptor `org.autojs.plugin.mail.api.IMailPlugin`; 路线图 P1.1 落地后以 AAR 形式进入 `libs/`) |
| 最低宿主 versionCode | `ThreeStampMailPlugin.REQUIRED_HOST_VERSION` (5316, 含新邮件插件身份与设置入口的 6.8.0 宿主构建); `MailIds.REQUIRED_HOST_VERSION_CODE` 的通用协议下限仍为 5282, 避免使其他邮件实现失效 |
| 邮件库 | Eclipse Angus Mail `org.eclipse.angus:jakarta.mail` 2.0.5 + `angus-activation` 2.0.3 + `jakarta.activation-api` 2.1.4 (路线图 D2) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.8.3 |
| 发布文件名 | `autojs6-plugin-three-stamp-mail-v{VERSION_NAME}-{CRC32}.apk` (单 APK) |

## 3. 工作区与提交

### 3.1 会话开始

- MUST 运行 `git status --short`, 检查当前分支, 最近提交和相关文件差异.
- MUST 将已有未提交内容视为用户工作. 不覆盖, 不回滚, 不擅自整理与当前任务无关的改动.
- 禁止使用 `git reset --hard`, `git checkout -- <path>` 或其他可能丢失用户内容的命令, 除非用户明确授权.
- 先阅读 `ROADMAP.md` 的 "阶段总览" 与最后一条 "会话记录", 从路线图建议的起点开始.

### 3.2 开发过程

- 每个行为改动应同时考虑实现, 测试, 10 语言资源, README, changelog, 宿主入口和公共契约.
- 不提交本地缓存, IDE 状态, 调试输出, 测试账户或无意生成的二进制文件.
- Gradle 自动修改 `BUILD_TIME` 时, 在确认来源后与相关变更一并处理. 若 Gradle 修改 `VERSION_BUILD`, 必须按第 3.4 节的提交计数规则校正; `VERSION_NAME` 只按语义化版本规则调整.
- 修改第三方依赖时同步记录版本, 来源, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`), 并在 changelog 的 `dependency` 分类记录.
- 路线图条目完成后在 `ROADMAP.md` 勾选并写入证据 (设备, API, 服务商, 度量值), 不勾选没有证据的条目.

### 3.3 提交

- 除非用户明确要求本次会话不要提交, 会话结束前 MUST 将本次范围内的全部文件按逻辑提交, 一个路线图子项一个提交.
- 使用 Conventional Commits 风格: `feat:`, `fix:`, `docs:`, `build:`, `test:`, `ci:`, `chore:`, 可加作用域, 例如 `feat(core): ...`, `feat(binder): ...`.
- 一个提交表达一个完整意图; 行为实现, 对应测试和对应 changelog 通常放在同一提交.
- 提交前 MUST 审阅 `git diff --check`, `git diff --cached`, `git status --short`, 确认没有密钥, 密码, 授权码, 令牌, 邮箱测试账户, 本地路径, 临时 APK 或无关改动.
- 会话结束时最终 `git status --short` 无输出; 若发现无法纳入本次提交的用户改动, 停止自动提交并向用户说明.

### 3.4 提交计数

- `VERSION_BUILD` MUST 与当前分支 `HEAD` 可达的 Git 提交数一致.
- 每次准备新提交时, 先用当前提交数加 1 得到即将产生的 build number, 写入 `version.properties`, 再把该文件与本次逻辑改动一并提交. 不要先写成当前提交数再提交.

```bash
next=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
sed -i "s/^VERSION_BUILD=.*/VERSION_BUILD=$next/" version.properties
```

最后一笔提交完成后 MUST 验证 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 无输出. 若发现不一致, 将 `VERSION_BUILD` 设置为 "当前提交数 + 1" 并创建一笔有明确含义的校正提交.

### 3.5 版本名称

- `VERSION_NAME` 从 1.0.0 开始, 按语义化版本管理, 与提交数量不绑定.
- 修改 `VERSION_NAME` 时同步更新全部 changelog JSON 的版本 key, README, 发布文件名断言与测试夹具, 再运行文档生成器.

## 4. 仓库结构

```text
AutoJs6-Plugin-Three-Stamp-Mail/
|-- .changelog/                 lang_*.json x 10 + template_changelog.md (文案源)
|-- .github/workflows/          build.yml, markdown.yml
|-- .python/                    generate_markdown.py (+ .bat), check_markdown.bat, generate_launcher_icons.py
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, template_plugin_instruction.md, README-*.md (生成)
|-- app/                        Android 插件 (Binder 服务, 账户存储, 设置页, 后台守望 trigger/, 资源, JVM 与 instrumentation 测试)
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   `-- src/{main,test,androidTest}
|-- build-logic/                org.autojs.build.{utils,versions,signs,jvm-convention,...} 约定插件
|-- docs/dev/                   阶段证据与开发笔记 (p0-spike-evidence.md 等; oauth-client-registration.md 为维护者的 OAuth 客户端注册流程)
|-- gradle/                     libs.versions.toml, wrapper/
|-- libs/                       宿主 API AAR (哈希锁定, 见 libs/README.md)
|-- locks/                      host-api-aars.lock
|-- mail-core/                  纯 JVM 模块: Jakarta Mail 会话 / 收发 / MIME / 监听逻辑与 GreenMail 测试
|-- AGENTS.md, ROADMAP.md, README.md (生成, 简体中文), LICENSE (MPL-2.0), THIRD_PARTY_NOTICES.md
|-- build.gradle.kts, settings.gradle.kts, gradle.properties, version.properties
|-- mail-test-accounts.properties   维护者本地的真实测试账户, Git 忽略, 永不入库
|-- oauth-clients.properties     维护者的 OAuth 2.0 客户端 id 与租户 (路线图 P9), Git 忽略, 构建时注入
`-- sign.properties             本地签名配置, Git 忽略
```

不要仅为目录整齐创建空模块. 宿主侧的契约模块, 脚本 API 与文档位于各自仓库 (第 10 节), 不放入本仓库.

## 5. Gradle 与版本平台

### 5.1 在线平台版本插件

- MUST 使用在线仓库中的 `io.github.supermonster003.autojs6-platform-versions` (当前 1.8.3). 升级时先确认新版本已能从公共仓库解析, 并与其他官方插件仓库统一升级.
- 禁止使用 `mavenLocal()`, 禁止本地平台版本实现, 禁止提交 `gradle/data` 消费端覆盖.
- 平台插件只在根 `settings.gradle.kts` 应用一次, 且整个 `plugins` 块位于 `includeBuild("build-logic")` 之前; `build-logic/settings.gradle.kts` 不应用它.
- 根 `build.gradle.kts` 用 `System.getProperty("gradle.agp.version")` 与 `gradle.kotlin.version` 声明模块实际使用的插件 (`com.android.application`, `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.serialization`) 并 `apply false`; 模块只应用插件, 不硬编码版本. 版本逃生门只用 `version.properties` 的 `OVERRIDDEN_*`, 常规构建保持 `NONE`.
- `app` 模块从 `version.properties` 和 `org.autojs.build.versions` 读取 compileSdk, minSdk, targetSdk, versionCode, versionName; `:mail-core` 从同一约定插件发布的 `gradle.java.version.select` / `gradle.jvm.target.effective` 取 JDK 工具链与目标.
- 不声明 `org.jetbrains.kotlin.android`; `app` 的 Kotlin 支持由 AGP 内置能力与约定插件提供.

验收命令 (模拟 GitHub Actions 的 Temurin 环境, 日志 MUST 只有一段 `Version information for IDE platform and Gradle plugins`):

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :mail-core:test :app:assembleDebug :app:testDebugUnitTest
```

### 5.2 仓库边界

- Gradle 构建 MUST 自包含. 禁止引用仓库外部的 JAR, AAR, `flatDir` 或兄弟项目路径 (例如 `../AutoJs6/...`).
- 宿主 API AAR MUST 复制到 `libs/` 并在 `locks/host-api-aars.lock` 记录小写 SHA-256; `app/build.gradle.kts` 在配置期校验文件存在, 非 debug 命名, 哈希匹配, 锁文件键集合精确. 更新 AAR 时同步更新锁文件, `THIRD_PARTY_NOTICES.md` 与契约测试. `mail-api.aar` 与 `common-plugin-api.aar` MUST 来自同一宿主提交并一起换锁.
- 宿主与插件需要同步更新时分别修改各仓库 (宿主 `D:/idea-projects/AutoJs6`), 不通过跨仓库相对路径制造隐式耦合.
- `:mail-core` MUST 保持纯 JVM: 不依赖 Android SDK, 不引用 `android.*`, 不使用 `android.util.Log`; Android 侧适配 (Binder, PFD, Keystore, 资源) 全部放在 `app`.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, MUST 保持被 Git 忽略 (`git check-ignore` 验证). 仓库中不得出现密码, token, 私钥或开发者绝对路径.
- 保留 `org.autojs.build.signs`, `signingConfigs` 与 release 签名选择逻辑.
- `appendDigestToReleasedFiles` 任务 MUST 保留该名称, 依赖 `assembleRelease`, 在签名缺失时失败, 校验实际 APK 集合恰为 `autojs6-plugin-three-stamp-mail-v{VERSION_NAME}.apk`, 并追加 CRC32 生成 `autojs6-plugin-three-stamp-mail-v{VERSION_NAME}-{CRC32}.apk` 到 `releases/` (不入库).
- `-PandroidTestRelease` 把 `testBuildType` 切到 release, 用于在真机验证 R8 处理后的 Angus Mail 处理器仍可解析 (路线图 P0.2); 引入或升级运行时依赖后 SHOULD 跑一次. 该属性同时把 `app/proguard-android-test-release.pro` (保留测试直接引用的插件与 `:mail-core` 成员) 加入应用规则, 并以 `testProguardFiles` 为测试 APK 加载 `app/proguard-test-rules.pro`; 两者都不属于正式 release 构建, 正式规则只有 `proguard-rules.pro`.

### 5.4 不启用 ABI 拆分的理由

插件完全由 Kotlin / Java 字节码与普通资源构成 (Angus Mail, Jakarta Activation, kotlinx-serialization 均为纯 JVM 库), 拆分包内容实质相同, 不会带来下载或兼容性收益. 因此:

- 不配置 `splits.abi`, 不配置 `ndk.abiFilters`, 每次发布只有一个 APK; `nativeAlignment { expectNoNativeLibraries }` 在构建期拒绝意外引入的原生库.
- `getInfo()` MUST 显式写有 `supportedAbis = emptyArray()`, 测试断言其为显式空数组.
- 16 KB page size 检查不适用 (`NATIVE_PAGE_ALIGNMENT` meta-data 为 0); 若未来引入含原生库的依赖, 本节作废并需补齐 ABI 与 16 KB 验证.

### 5.5 R8 与 Jakarta Mail

- Angus Mail 按名称实例化协议提供者与 MIME 处理器 (`META-INF/javamail.default.providers`, `META-INF/mailcap`, `ServiceLoader`); `app/proguard-rules.pro` MUST 保留 `jakarta.mail.**`, `jakarta.activation.**`, `org.eclipse.angus.mail.**`, `org.eclipse.angus.activation.**`, 并对 `java.beans`, `javax.naming`, `javax.security.sasl`, `java.lang.management`, `javax.management` 等桌面 JDK 包 `-dontwarn`.
- `MailcapRegistry.ensureRegistered()` 在代码中显式注册 Angus 处理器, 作为 `META-INF/mailcap` 不可用时的兜底; 每个 `Session` 都经 `MailSessionFactory` 创建, 不直接调用 `Session.getDefaultInstance`.
- 引入或升级运行时依赖后 MUST 执行 `:app:assembleRelease` (缺失类只会在这里暴露), 并记录 release APK 体积到 changelog 或 `docs/dev/`.

## 6. Manifest 与激活协议

- Manifest MUST 声明 `org.autojs.permission.PLUGIN`, `<queries>` 宿主包名, `org.autojs.plugin.WAKE_ACTIVITY` 与 `org.autojs.plugin.info.AUTHOR` meta-data, `NATIVE_PAGE_ALIGNMENT=0`.
- `WakeActivity` MUST 为 `exported=true`, `Theme.NoDisplay`, `excludeFromRecents`, `finishOnTaskLaunch`, 受 PLUGIN 权限保护, 响应 `org.autojs.plugin.action.WAKE` + DEFAULT category, 启动后立即结束, 不做任何副作用.
- `ThreeStampMailPluginInfoService` 与 `ThreeStampMailPluginService` MUST `exported=true`, 受 PLUGIN 权限保护, 声明 `requiresHostVersion` meta-data (与 `ThreeStampMailPlugin.REQUIRED_HOST_VERSION` 一致), 运行在默认进程.
- 所有对外组件逐项审查 `android:exported`; 除契约入口外不得导出其他组件. 独立设置页 (P4) 若需被宿主打开, 使用 PLUGIN 权限保护的显式 action. 唯一例外是 P8 的 `trigger.BootReceiver` (`BOOT_COMPLETED` 要求 exported, Manifest 默认 `enabled=false`, 只由守望页面的开机自启开关经 `PackageManager.setComponentEnabledSetting` 打开); `trigger.MailWatchService` 不导出.
- `android:usesCleartextTraffic` 保持默认 (false), Manifest 注释 MUST 保留该说明: 明文 IMAP / POP3 / SMTP 只在脚本显式 `tls: 'none'` 时由 socket 层决定, 与网络安全策略无关; 插件不发起任何 HTTP 请求, 唯一例外是路线图 P9 浏览器登录的令牌端点 (`oauth.HttpsFormPoster`, 只接受 `https`, 只向 `OAuthProviders` 表中的 Google / Microsoft 端点 POST 表单, 不跟随重定向, 响应体 64 KiB 上限).
- P9 的两个 Activity: `oauth.OAuthSignInActivity` 不导出 (`singleTop`, 父页为账户编辑器), `oauth.OAuthRedirectActivity` MUST 导出且无权限 (浏览器投递重定向), `Theme.NoDisplay` + `excludeFromRecents`, 只响应两个 VIEW / DEFAULT / BROWSABLE filter (`${applicationId}://oauth2/microsoft` 与 `${oauthGoogleScheme}:/oauth2redirect`), 只把 URI 转发给登录页 (`CLEAR_TOP | SINGLE_TOP`) 后结束, 不做任何判断; `state` 校验在登录页完成, 不匹配的重定向被拒绝且不回显. 客户端 id 与租户来自根目录 Git 忽略的 `oauth-clients.properties` (`googleClientId` / `microsoftClientId` / `microsoftTenant`), 经 `resValue` 与 manifest 占位符 `oauthGoogleScheme` 注入, MUST NOT 写入源码或资源; 缺失时登录项禁用.
- 权限清单只包含 PLUGIN, INTERNET, ACCESS_NETWORK_STATE, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (路线图 P4.6 的 "忽略电池优化" 引导按钮, D27: 只在用户点击时发起系统请求, 不在启动时弹窗, 不作为任何功能的前置条件) 以及 P8 后台守望的四项: `FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_SPECIAL_USE` (守望前台服务, 类型 `specialUse`, 子类型 `mail_background_watch`, D42), `POST_NOTIFICATIONS` (只在守望页面启用守望时申请), `RECEIVE_BOOT_COMPLETED` (守望页面的开机自启开关, 默认关闭). `ManifestContractTest` 断言权限集合精确; 新增权限必须在 README 安全章节与 changelog 说明理由.
- 在 ColorOS 等会保持新装应用停止状态的设备上 SHOULD 做真实激活验收; 未执行时在路线图如实记录 `未执行真实设备激活验证`.

## 7. PluginInfo 与能力协商

- `ThreeStampMailPluginRuntimeInfo` 是纯数据映射, `ThreeStampMailPluginInfo.kt` 负责 Android 侧读取 (包版本, 本地化描述, `@raw/plugin_instruction`, 构建日期), 二者的分离 MUST 保持, 以便 JVM 测试覆盖映射.
- `name` 与不可翻译的 `app_name` 一致; `description` 来自当前 locale 的 `plugin_description`; `versionName` / `versionCode` 来自 `PackageInfo`; `versionDate` 来自 `plugin_version_date` (`MMM d, yyyy`, `GMT+08:00`); `id` / `engine` / `variant` 与第 2 节一致.
- `capabilities` 包含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION` (Long) 与 `MailCapabilityKeys` 的契约版本 (Int), 协议集合, 认证机制, 特性集合 (String 数组), 预设表版本 (Int) 与邮件库版本 (String), 由 `ThreeStampMailPlugin` 的常量单点定义 (`capabilitiesBundle()`); 宿主先读取能力再调用新方法, 不通过捕获异常猜测协议版本.

## 8. Binder 与公共 API

- 公共常量, op 名, key, capability key, 错误码, ID, action 和 category MUST 集中在宿主 `mail-api` 契约模块 (AAR) 与 `ThreeStampMailPlugin` 中, 禁止散落字符串字面量.
- 请求 / 响应 / 事件为 `Bundle` 固定 key 下的 JSON 文档 (路线图 D14); 所有 Binder 输入 MUST 做边界校验 (长度, 大小, key, 枚举, 索引, 描述符数量), 上限常量集中定义并与路线图附录 B.5 一致.
- 凭据只经 Binder 的专用 key 传递 (`KEY_SECRET_PASSWORD`, `KEY_SECRET_ACCESS_TOKEN`), MUST NOT 放进 JSON 文档, 日志, 异常消息或错误 `details`; `MailAccountOptions` 在账户 JSON 里遇到 `password` / `accessToken` 等字段直接拒绝, 所有离开插件进程的文本经 `Redactor` 脱敏.
- 描述符是插件的副本 (路线图 B.3): 只有 `MailContract.OPS_WITH_SOURCES` / `OPS_WITH_SINK` 的 op 接受描述符, 其余 op 携带描述符 -> `INVALID_ARGUMENT`, 数量超过 `MAX_DESCRIPTORS` -> `LIMIT_EXCEEDED`; 校验在会话线程且在路由之后 (未知 / 未实现 op 的错误优先), 附件描述符 MUST 是可 seek 的文件 (`DescriptorSource` 每次 `open()` 都 `dup` + `lseek(0)`, 因为同一封邮件为 SMTP 与 IMAP `APPEND` 各序列化一次), 全部副本 MUST 在 `onResult` 前关闭, 无论成功与否.
- 下载 op (`OPS_WITH_SINK`: `messages.raw`, `attachments.download`) 只接受恰好一个描述符, 即宿主给出的管道或文件写端 (路线图 P2.3): 参数校验先于打开写端, 插件在 `onResult` 前 flush 并关闭写端, 宿主读端因此在结果之前读到 EOF; 传输进度经 `onProgress` 以 `{id, transferred, total?}` 报告 (`MailContract.FIELD_TRANSFERRED` / `FIELD_TOTAL`, 每 1 MiB 或总量的 5% 一次, 不低于 64 KiB); 写端失败 (宿主关闭读端) 以 `SinkFailedException` 区别于服务器连接故障, 映射为 `IO_FAILED` 且不断开服务器连接, 也不重试. 路由层 `RequestRouter.CallIo` 把描述符与进度通道延迟到处理器真正需要时才打开.
- 调用方校验 (路线图 P2.5, D35): `openSession` / `listSavedAccounts` 与每个 `IMailSession` 方法先过 `CallerGuard` (`binder/CallerPolicy` 是纯决策, `HostCallerGuard` 用 `Binder.getCallingUid` + `PackageManager` 取证): 调用 uid == 已安装宿主 uid 且宿主包在该 uid 下, 宿主 `versionCode` >= `REQUIRED_HOST_VERSION`, 宿主与插件 SHA-256 签名者集合非空且相等, 会话方法只接受打开会话的 uid; 不满足则抛 `SecurityException("Caller is not the installed same-signer AutoJs6 host: <reason>")` (文案与 MCP Server 插件一致); `getInfo` / `getCapabilities` / `listProviders` 对持有插件权限的调用方开放; 同进程 instrumentation 用 `CallerGuard.trusting()`. 规则改动 MUST 同步 `CallerPolicyTest` 与 MCP Server 插件.
- 会话上限与队列 (`binder/Limits`, D35): 请求信封与响应信封按 UTF-8 字节 <= `MAX_ENVELOPE_BYTES` (请求超限在解析 op 前拒绝, 响应超限把结果换成 `LIMIT_EXCEEDED`), 错误消息截到 `MAX_ERROR_MESSAGE_BYTES`; 每会话一条工作线程顺序执行, 执行中的调用之后最多排队 `MAX_QUEUED_CALLS`, 再多 -> `LIMIT_EXCEEDED`; `MAX_CONCURRENT_CALLS` 只约束宿主. 拒绝 (超限, 队列满, 已关闭) 的应答由 `three-stamp-mail-session-responder` 线程送出, 结果 MUST NOT 在 Binder 线程回调.
- 取消与关闭语义 (D35): `cancel(id)` 对排队中的调用立即答 `CANCELLED` 并关闭其描述符副本; 对执行中的调用 `worker.interrupt()` + `MailSession.abort()` (关闭会话全部套接字, `SocketRegistry` 在 `abort` 与 `resume` 之间拒绝新套接字, 于是取消先于建连的竞态也能覆盖), 该调用之后的失败映射为 `CANCELLED`, 已成功完成的调用保留其结果; 未知或已完成的 id 忽略. `close()` 与宿主死亡都走 `shutdown(reason)`: 排队调用各答 `SESSION_CLOSED`, 执行中的调用被中止后答 `SESSION_CLOSED`, 工作线程退出时关闭 `MailSession` 并 `onStatus(closed, reason)`; 每个调用恰好应答一次. `getStatus` 除 `state` / `reason` / `lastError` / `connected` 外带 `queued` 与 `active` (执行中的 id). `RequestRouter.ENTRIES` 记录每个 op 支持的收信协议集, 不支持的协议在解析参数前答 `UNSUPPORTED_OPERATION`.
- 已发布 AIDL 演进时保持旧 transaction 顺序, 末尾追加, 通过契约版本协商; op 表追加不改 AIDL; 破坏性重设计同步升级宿主与插件.
- 不在 Binder 主路径执行无界网络访问或不可取消的长耗时初始化; 会话内操作在插件侧串行, 每个请求有 `requestId`, 超时与取消; 服务被回收, 首次绑定, 重复绑定和并发调用都应保持确定行为.

## 9. 邮件核心与网络约束

- 每个 `jakarta.mail.Session` 只服务一个账户与一个协议, 由 `MailSessionFactory` 创建, `debug=false`; `options.debug` (路线图 D28) 只经 `onProgress` 输出脱敏摘要, 永不打开 Jakarta 的 `mail.debug`.
- TLS 默认开启 (SSL 或 STARTTLS 由预设或脚本指定); `starttls.required=true`, 不做机会式降级; `tls.trustAll` (D25) 只对单个账户生效, 同时关闭主机名校验并把会话标记为 `insecure`, 不提供全局开关.
- 认证机制固定为脚本声明的机制 (`LOGIN PLAIN` 或 `XOAUTH2`), 不回退; 认证失败的异常消息不得含密码或令牌.
- 浏览器登录 (路线图 P9, D44): OAuth 2.0 授权码 + PKCE (`S256`), 公共客户端无 secret; `TokenClient` 的错误消息与 details 不得含 `code`, verifier 或令牌; 刷新只在剩余不足 `OAuthTokens.REFRESH_MARGIN_MS` 时进行且按账户串行; Microsoft 的刷新重复 scope, Google 不带; 撤销只对提供撤销端点的服务商发起 (Google), 本地先作废记录.
- 超时: 连接 / 读 / 写都设置 (`MailAccount.timeoutMillis`), 不允许无限期阻塞; 监听 (P5) 与后台守望 (P8) 的重连使用指数退避.
- 出站连接只指向脚本或预设给出的主机; 插件不做 DNS 之外的任何发现, 不上报遥测.
- 服务商预设 (`providers.json`) 只包含公开的主机 / 端口 / TLS / 认证提示, 不含任何账户.
- 收信侧 (路线图 P2.3, `ImapMailbox` / `MessageMapper`): 每个 op 自行打开并关闭所需文件夹 (关闭不 expunge), 存储不保留选中状态, 脚本可见的游标只有 UID (`before` / `after` 映射为序号区间, 缺失的 UID 仍按位置切分); 列表与搜索只取 ENVELOPE / FLAGS / RFC822.SIZE / BODYSTRUCTURE / UID (`hasAttachments` 由 BODYSTRUCTURE 判定, 不下载正文); `messages.get` 一律以 PEEK 取正文, `peek: false` 之后显式 STORE `\Seen`; 正文内联预算 `MAX_INLINE_BODY_BYTES` (超出的部件进入 `bodyParts`, `bodyTruncated = true`, 只有 HTML 的邮件才派生文本); `includeRaw` 的 `raw` 是 ISO-8859-1 逐字节映射的字符串. 字符集恢复链 (`TextRecovery`): 声明的字符集严格解码 -> UTF-8 严格 -> GB 18030 -> ISO-8859-1, 头部里的原始 8 位字节按同样规则修复; 附件文件名去掉路径分隔符与控制字符, 无名部件用 Content-ID 或 `part-<id>` 加扩展名.
- IMAP `ID` (RFC 2971) 由 `IdentifyingImapStore` 在每条连接认证成功后发送 (覆盖 `newIMAPProtocol` 与全部认证入口), 不只是首条连接: Angus 的连接池会为 LIST / STATUS 另开连接并在文件夹关闭后丢弃多余连接, 163 / 126 / yeah.net 对任何未识别连接的 SELECT / EXAMINE 都答 `Unsafe Login`. `clientId` 为空或服务器未通告 `ID` 时不发送; 服务器拒绝 ID 本身时连接照常使用. 不要为这类服务商引入读写打开文件夹之类的绕过.
- 搜索 (`SearchQueryCompiler`): 查询 JSON 编译为 Jakarta `SearchTerm` (`uid` 只允许顶层, 用 `folder.search(term, candidates)` 限定候选; 嵌套上限 8 层); 服务器拒绝 SEARCH (`MessagingException`) 且 `fallback` 不是 `none` 时, 对最新 `MAX_CLIENT_FILTER` 封在客户端用 `term.match` 过滤并在结果里标 `fallback: "client"`; `fallback: "always"` 跳过服务器 (真实服务商的索引会滞后于投递, 刚到的邮件几秒内搜不到). `messages.move` 有 `MOVE` 能力用之, 否则 COPY + `\Deleted` + UID EXPUNGE (无 UIDPLUS, 或服务器对 UID EXPUNGE 答 BAD 时整夹 expunge, 163 如此, `ImapMailbox.uidExpungeRefused` 记住一次); 下载与变更类 op 不做断线重试.
- POP3 退化路径 (路线图 D3 / P2.4, `Pop3Mailbox`): `uid` 是服务器的 UIDL 字符串 (`MessageArgs` 的每个解析器按账户的收信协议解析 `uid`, `RequestRouter` 传入 `session.receiveProtocol`; `MessageDocument.uid` / `UidsResult.uids` 是 `JsonPrimitive`, IMAP 为数字, POP3 为字符串); 只有 `INBOX`, `folders.list` 带 `status` 时以 `STAT` 计数且 `unseen` 为 null; 每个 op 自行打开并关闭 `INBOX`, 只有 `messages.delete` 以读写打开并在 `close(true)` 时让 `DELE` 生效 (`expunge: false` 同样删除, POP3 没有 "已标记未清除" 状态); 列表与搜索只取信头 (`TOP n 0`), `hasAttachments` 按 Content-Type `multipart/mixed` 判定; Angus 对每封各发 `TOP` 与 `LIST`, 所以搜索必须由新到旧逐封匹配并在凑够 `limit` 时停止, 窗口是 `MAX_POP3_CLIENT_FILTER` (200) 而不是 IMAP 的 `MAX_CLIENT_FILTER`, 结果一律 `fallback: "client"`; `body` / `text` / `uid` 条件, `unseenOnly`, 非 `INBOX` 文件夹, `folders.status` / `create` / `delete` / `rename`, `setFlags` / `move` / `copy` / `expunge` / `append` 必须在连接之前拒绝 (`MailSession.imapOnly` / `Pop3Mailbox.check*` -> `UNSUPPORTED_OPERATION` / `FOLDER_NOT_FOUND`); `messages.get` / `raw` / `attachments.download` 走整封 `RETR`, `peek` 无意义. 不要为 POP3 引入跨调用的信头缓存或长期打开的文件夹 (POP3 会话独占邮箱锁).

## 10. 主项目职责

若改动同时需要修改 `D:/idea-projects/AutoJs6`, MUST 遵循:

- 宿主只保留入口 (契约模块, Binder 客户端, 脚本 API `mail`, 附件落盘), 插件拥有邮件 I/O 的真实实现 (路线图 D13); 插件未安装或被禁用时宿主不得提供重复实现.
- 宿主先区分 `未安装`, `已安装但未激活或禁用`, `版本不兼容`, `调用失败`, `可用`, 各状态有对应提示与引导 (安装来源, 激活按钮, 所需版本).
- 更新包名, action, category, ID 或 API 时同步检查宿主注册表, ProGuard/R8, 安装 URL, 启用状态缓存和测试夹具.
- 涉及公开脚本 API 时再同步 `AutoJs6-Documentation`, `AutoJs6-TypeScript-Declarations`, `AutoJs6-Plugin-Offline-Docs`, `AutoJs6-Plugin-Ace-Editor`.

## 11. 应用标题与字符串资源

- 用户可见字符串 MUST 覆盖 `values`, `values-en`, `values-ar`, `values-es`, `values-fr`, `values-ja`, `values-ko`, `values-ru`, `values-zh`, `values-zh-rHK`, `values-zh-rTW`; `values` 与 `values-en` 共有条目内容一致, 各语言占位符与转义一致 (`StringResourceParityTest`).
- `app_name` 位于 `strings_donottranslate.xml` 且 `translatable="false"`; `plugin_author`, `plugin_id`, `plugin_engine`, `plugin_variant`, `plugin_version_date` 由 Gradle `resValue` 生成.
- 每个 locale MUST 有 `plugin_description`: 简洁说明能力, 句尾不加终止标点, 不写 "邮件插件" 前缀, 不写 "适用于 AutoJs6" 等限定表述.
- `<string>` 按 `name` 升序; plurals 与数组放入各自文件.
- 所有资源与文档字符串使用 ASCII 标点 (`, . : ; ! ? ( ) [ ] / -`), 省略号用 `...` 并加 `tools:ignore="TypographyEllipsis"`; 禁止全角标点, 顿号, 弯引号. `ApplicationTextPunctuationTest` 会扫描 `app/src/main`, `.readme`, `.changelog`, `docs`, `README.md`, `ROADMAP.md`, `AGENTS.md` 与 `THIRD_PARTY_NOTICES.md`.

### 11.1 启动器图标

- `app/src/main/res/mipmap/ic_launcher.png` 与 `mipmap-night/` 变体, adaptive 图层由 `.python/generate_launcher_icons.py` 确定性生成; 修改图标时修改脚本并重新生成, 不手工改 PNG.
- 图标原稿由维护者提供, 保存在 `.python/icons/three-stamp-mail-ic-launcher-light.png` 与 `-dark.png`, 各为 1092 x 1092 RGBA, alpha 一致. light / dark 指使用模式. 原稿字节保留, UI / adaptive 比例按 Optical geometry v1 派生, 光学偏移 0.0. 原稿包含表达信封折线的明暗细节, 只取 alpha 并涂成纯色会丢失这些线条, 因而保留两套原稿及其明暗细节, 生成 RGB 时去掉细微色偏, 统一为 R=G=B 中性灰阶.
- 遵循同目录 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` 的 UI / 系统资源分离, 安全圆与四种启动器模式规则. `ic_launcher` 始终是透明位图, 不设自适应覆盖; application 使用 `ic_launcher_system`, 四个 alias 默认 Auto. 系统背景固定为亮 #FAFAFA / 暗 #212121, 单色层来自原稿 alpha. UI 与桌面选择独立. 运行生成器与 `--check`, 验证最终非零 alpha 像素和包内资源.

### 11.2 3-Stamp Mail 2.0 安装身份

- 维护者于 2026-10-03 明确要求点分包名 `io.github.supermonster003.autojs6.plugin.three.stamp.mail`, 显示名 `3-Stamp Mail`, 版本 2.0.0. 改名按同目录 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` 执行.
- 原 `io.github.supermonster003.autojs6.plugin.angus.mail` 与新包独立安装, 旧包可并存, 账户, 设置与守望不自动迁移. 不自动卸载旧包或清除其数据, 不新增旧身份兼容别名.
- 保留功能契约 `mail` engine, `default` variant, `mail` INFO / MAIL category, `org.autojs.plugin.MAIL`, `MAIL_SETTINGS`, `org.autojs.plugin.mail.api` AIDL 包, descriptor 与 transaction 顺序. Eclipse Angus Mail 及其 `org.eclipse.angus` 包是第三方库, 不能改名. 历史 changelog / 阶段证据保留当时名称.
- 宿主 `MailIds` 与插件内锁定 AAR 同步更新; 脚本 API 仍为 `mail`. 浏览器登录必须使用新包名对应的 Google Android 客户端和 Microsoft 重定向注册, 不复用未经确认的旧注册. 注册步骤见 `docs/dev/oauth-client-registration.md`.


## 12. README 与多语言生成

- README, 插件中心说明 (`raw*/plugin_instruction.md`) 与 changelog MUST 由 `.readme/*.json`, `.changelog/*.json` 与模板通过 `.python/generate_markdown.py` 生成; 生成产物不得手工编辑.
- 修改 JSON 或模板后先运行 `py .python/generate_markdown.py`, 再运行 `py .python/generate_markdown.py --check` (CI `markdown.yml` 也会执行). 生成器校验语言集合, JSON 键与列表形状, 全角符号, 未替换占位符, 版本对齐, 孤儿产物与漂移.
- 根 `README.md` 是简体中文版本, 与 `.readme/README-zh-Hans.md` 同源; 语言导航必须出现 `简体中文`.
- README 先说明用户能完成什么, 再说明安装与账户准备 (授权码 / 应用专用密码 / OAuth 令牌); 不写 Android Studio 或 IntelliJ IDEA 版本信息, 不向普通用户解释 `supportedAbis`, 签名过程等实现细节. 脚本 API 尚不可用时 README 与插件说明 MUST 如实标明当前状态.
- README 链接必须指向本仓库的真实 release, issue, license 与生成 changelog.

## 13. Changelog

- `.changelog/` 只存放 10 个 `lang_*.json` 与模板; 生成的多语言 changelog 位于 `app/src/main/assets/doc/`.
- 涉及 `feature`, `fix`, `improvement`, `dependency` 的提交 MUST 更新当前 `VERSION_NAME` 对应 `vX.Y.Z` 条目的全部语言 JSON, `released_date` 更新为当日 `YYYY/MM/DD`.
- 分类 key 只用 `hint`, `feature`, `fix`, `improvement`, `dependency`; 标签沿用既有固定翻译 (简体中文 `提示`, `新增`, `修复`, `优化`, `依赖`; 英文 `Hint`, `Feature`, `Fix`, `Improvement`, `Dependency`; 其他语言见现有 JSON).
- `feature` 条目不以 `新增` 开头, `fix` 条目不以 `修复` 开头; `dependency` 只记录 Gradle 依赖变化, 使用 `附加`, `升级`, `移除` 等固定动作词.
- 与 AutoJs6 GitHub Issue 有关时按既有格式写明 Issue 引用.

## 14. 凭据存储, 独立界面与发行历史 (CONDITIONAL, 路线图 P4)

- 脚本传入的凭据只在会话生命周期内驻留内存 (路线图 D8); 插件侧保存账户 MUST 使用 Android Keystore 派生的 AES-GCM 密钥加密, 存于 `noBackupFilesDir`, 排除在备份与 `data_extraction_rules.xml` 之外, 不写入 `SharedPreferences` 明文, 资源, BuildConfig 或日志.
- 设置页 (`AccountsActivity` 等) SHOULD 跟随宿主的语言, 夜间模式和主题色, 宿主配置不可用时安全回退; 密码 / 令牌输入以 `CharArray` 读取, 界面最多显示脱敏后的尾 4 位, 不在截图或最近任务缩略图中泄露.
- 设置页 MUST 提供独立的 `发行历史` 入口, 按当前 locale 读取 `doc/CHANGELOG-{LANGUAGE_TAG}.md`, 找不到时回退英语; 1.0.0 不做插件内更新检查, 更新跟随宿主插件中心 (路线图 Q6).
- "忽略电池优化" 引导按钮 (D27) 只解释用途并转到系统对话框, 不自动请求, 不作为监听 (P5) 或后台守望 (P8) 的前置条件.
- 浏览器登录的账户 (P9) 以 `SecretKind.OAUTH2` 保存整份令牌文档 (访问 / 刷新令牌, 过期时刻), 账户文档只带 `oauth` 元数据; 所有别名会话 MUST 经 `oauth.AccountSecrets.withUsableSecret` 取访问令牌 (刷新在插件进程内完成), 令牌从不进 Binder 字段 (`MailBundles.secret` 对 `OAUTH2` 返回 null), `listSavedAccounts` 只报告 `oauth` 元数据; 登录结果在进程内 `PendingGrants` 暂存 10 分钟, 不写入 Bundle / Intent extras.
- 后台守望 (P8) 只运行已保存别名的账户, 秘密在插件进程内解密; 守望配置与触发记录 (至多 `MAX_TRIGGER_RECORDS` 条信封摘要, 不含正文) 存于 `noBackupFilesDir/mail-triggers/`; 发往宿主的 `org.autojs.autojs6.action.MAIL_TRIGGER` 广播是显式 Intent (`setPackage` 宿主), 以 PLUGIN 权限投递, 只携带信封文档; 有活动订阅者时不广播.
- 所有界面覆盖无障碍标签, RTL, 大字体, 夜间模式与进程恢复.

## 15. 测试要求

### 15.1 `:mail-core` JVM 测试 (GreenMail)

- 账户选项 / 预设 / 错误映射 / 连接守卫有纯 JVM 用例 (`MailAccountOptionsTest`, `ProviderPresetsTest`, `ExceptionMapperTest`, `ConnectionGuardTest`); 每个会话 / 收发 / MIME / 查询 / 监听逻辑 MUST 有 GreenMail 覆盖 (`com.icegreen:greenmail`, 与 Angus Mail 2.0.5 对齐); 测试用 `ServerSetupTest` 的非特权端口 (3025 / 3143 / 3110 及 SSL 变体), 每个测试类自行 `start()` / `stop()`, 不共享服务器实例.
- 发信链路 (路线图 P2.2) 的用例: `OutgoingMessageParserTest` (字段校验, 上限, 头注入, 描述符绑定, 文件名清洗), `MessageComposerTest` (MIME 树与头部编码, 在序列化后的字节上断言), `SmtpSendGreenMailTest` (`MailSession.send` / `append` 对 GreenMail: 投递, Bcc 不外泄, `saveToSent` 三态与 `sentCopy`, 草稿 APPEND 与 UID, 错误码).
- 收信链路 (路线图 P2.3) 的用例: `HtmlToTextTest` (块 / 换行 / 列表 / 表格 / 链接目标 / 实体 / `pre` / 空白折叠), `SearchQueryCompilerTest` (每种条件到 `SearchTerm` 的映射, 日期形式, 组合与深度上限, `uid` 只允许顶层, `UidSet` 语法, 编译结果对本地 `MimeMessage` 的 `match`), `MessageArgsTest` (每个 op 的参数形状, 默认值, 上限与错误信息), `TransferTest` (进度步长, 上限, 计数写入, 写端失败 -> `SinkFailedException`), `MessageMapperFixturesTest` (下条的夹具), `ImapOperationsGreenMailTest` (文件夹增删改查与角色, 120 封分页游标双向连续, `unseenOnly`, 服务器与客户端搜索, `fallback: always`, `messages.get` 的正文 / 头 / 附件 / `peek` / `includeRaw`, 超预算正文进入 `bodyParts` 且可下载, 10 MiB 附件流式下载带进度, `messages.raw` 可重新解析, 写端中断 -> `IO_FAILED` 且连接保留, 标记增删替换, `MOVE` 与 COPY + expunge 两条路径, 复制 / 删除 / expunge).
- POP3 退化路径 (路线图 P2.4) 的用例: `Pop3OperationsGreenMailTest` (经 IMAP APPEND 播种同一邮箱, POP3 会话开启 `debug` 以读取 trace: 只有 `INBOX` 且 IMAP-only op 与非 INBOX 文件夹不连接即拒绝, UIDL 游标双向分页与信头列表, `get` / `raw` / `download` 整封下载, 客户端信头过滤与扫描计数 (`limit: 1` 只扫 1 封), DELE 在关闭时生效且 IMAP 视图一致, POP3 账户的 `session.test` 与 `send`); `MessageArgsTest` 覆盖 POP3 的 UIDL 字符串解析与 IMAP-only 解析器的拒绝.
- MIME 夹具放在 `mail-core/src/test/resources/mime/*.eml` (由生成脚本产出, 覆盖未声明 GBK, 声明 gb2312, ISO-2022-JP, 未知字符集, 原始 8 位头与文件名, RFC 2231 / 2047 文件名, 嵌套 `message/rfc822`, 只有 HTML, 路径穿越与控制字符文件名, alternative / related / calendar 结构); 夹具不得含真实邮箱地址, 真实姓名或真实服务器响应.
- GreenMail 通过 `exclude(group = "jakarta.mail", module = "jakarta.mail-api")` 引入, 以免与 Angus bundle 重复; SLF4J 只绑定 `slf4j-nop`.
- 真实服务商往返 (QQ / 163 / Gmail / Outlook.com 等) 不进入 JVM 测试; 见 15.3.

### 15.2 `app` JVM 单元测试 (`app/src/test`)

- `ThreeStampMailPluginRuntimeInfoTest`: PluginInfo 纯数据映射, 身份常量, 与 `.readme/common.json` / `app/build.gradle.kts` 发布值一致.
- `ManifestContractTest`: Manifest 与 `ThreeStampMailPlugin` 常量一致 (权限集合精确, queries, Wake Activity, 两个服务的 action / category / 进程 / requiresHostVersion, 无 receiver / provider); P9 起再断言 `OAuthRedirectActivity` 只响应两个重定向 scheme 且登录页 `singleTop`.
- `SecretAuditTest` (mail-core) 扫描 `mail-core` 与 `app` 的主源码: 禁止 `Log` / `println` / `printStackTrace` / Jakarta debug; 例外只有 `STATE_LOGGERS` 清单 (守望与 OAuth 的状态日志文件), 其中每一行日志的插值不得含 secret / password / token / verifier / code / address / subject / body / email 等词 (`SECRET_WORDS`); 新增日志行 MUST 只插值 id, 状态, 计数与时长.
- `ApplicationTextPunctuationTest`: 打包与生成文本只使用 ASCII 标点.
- `StringResourceParityTest`: 10 语言键集合一致, 按名排序, `plugin_description` 无句尾标点, `locales_config.xml` 与语言集合一致.
- `MailCoreContractParityTest`: `:mail-core` 镜像的错误码 (`MailErrorCode`), 上限 (`MailLimits`), 枚举 id 与能力值与 `mail-api` 契约逐项一致 (`:mail-core` 不能依赖 AAR, 镜像靠这个测试守住).
- `RequestRouterTest`: op 表快照与 `MailContract.OPS` 一致 (P2.3 起 19 个 op 全部有处理器, `PENDING_OPS` 为空), 未知 op -> `INVALID_ARGUMENT`; 描述符只属于 `OPS_WITH_SOURCES` / `OPS_WITH_SINK`, 数量上限与 "非传输 op 携带描述符" 的检查在路由之后, 处理器之前 (`CallIo` 的伪实现证明描述符未被打开); 每个 op 的参数错误先于打开描述符与任何连接; 协议表快照 (`IMAP_ONLY` 九个 op, 其余 `ANY_RECEIVE`, POP3 账户对 IMAP-only op 在解析参数前即 `UNSUPPORTED_OPERATION`).
- `LimitsTest`: UTF-8 字节计数, 信封恰好 `MAX_ENVELOPE_BYTES` 通过而多一字节拒绝 (请求与响应), 队列深度 = `MAX_QUEUED_CALLS`, 错误消息截断不切开码点.
- `CallerPolicyTest`: 同签名宿主通过, 宿主未安装 / uid 不符 / 包不在 uid 下 / 版本过旧 / 插件签名者为空 / 签名者不同的每条拒绝理由都点名 AutoJs6, 异常文案前缀固定.
- 路线图 P4 起补充: 账户存储编解码.

### 15.3 Android instrumentation (`app/src/androidTest`)

- `ThreeStampMailPluginContractTest` MUST 覆盖: Wake Activity 契约, INFO 服务发现与真实 `getInfo()` 往返 (包版本, 本地化描述, ID / engine / variant, 显式空 `supportedAbis`, `REQUIRES_HOST_VERSION`), `ThreeStampMailPluginService` 发现, 显式绑定与 Binder descriptor, `IMailSession.call` 的会话信封与描述符所有权 (非传输 op 携带描述符, 越界 `descriptorIndex`, 管道, 超过 `MAX_DESCRIPTORS`, 副本在 `onResult` 前关闭), 以及下载 op 的写端规则 (恰好一个写端, 写端在 `onResult` 前关闭使读端读到 EOF, 两个或零个描述符 -> `INVALID_ARGUMENT`, 参数错误同样释放写端, 收信 op 的参数错误经 Binder 返回).
- `MailCoreDeviceTest` MUST 覆盖: Angus 处理器在 APK 内可解析, MIME 组装 / 解析在设备运行时往返; 账户往返只在经 instrumentation 参数 (`mailAddress`, `mailSecret`, `mailImapHost`, `mailSmtpHost` 等) 提供账户时执行, 否则 `Assume` 跳过; 账户往返在邮件到达的一侧 (对端账户, 或收件人是自己时的本账户) 执行 P2.3 的读操作 (`folders.list` 带计数, `folders.status`, `messages.list` 轮询投递, 按 Message-ID 搜索 (服务器索引滞后时轮询后改 `fallback: always`), `messages.get` 校验主题 / 正文 / 附件且 `peek` 不置已读, `attachments.download` 逐字节比对, `messages.raw` 可重新解析, `messages.setFlags` 增删, `mailCleanup=true` 时 `messages.delete` + expunge); `mailReceive=pop3` (或对端的 `mailPeerReceive=pop3`) 让到达的一侧走 P2.4 的 POP3 子集 (`INBOX` 单文件夹, 客户端搜索, `unseenOnly` / `setFlags` / `folders.status` 等断言 `UNSUPPORTED_OPERATION`, `messages.delete` 为 DELE). 无真实账户时 SHOULD 用开发机上的 GreenMail standalone (与 `:mail-core` 测试同版本, `adb reverse` 映射 3025 / 3143 端口, `mailTls=none`) 完成一次设备端往返 (命令见该测试的 KDoc); 真实服务商未验证时在路线图记录 "未执行真实服务商验证".
- 真实测试账户放在仓库根 `mail-test-accounts.properties` (Git 忽略) 或命令行参数中, MUST NOT 写入源码, 测试资源, Gradle 脚本, CI 配置或提交信息; 测试输出与日志只打印地址域名与耗时, 不打印密码, 令牌或邮件正文. `.python/run_real_account.py <QQ_A|QQ_B|GMAIL_A|NETEASE_A|NETEASE_B> <serial> [--peer QQ_B] [--save-sent true|false] [--append Drafts] [--cleanup] [--receive pop3] [--debug] [--release]` 读取该文件 (NetEase 档案按地址域名选 `163` / `126` 预设, `yeah.net` 改用其自身主机) 并屏蔽输出中的全部密钥 (`report leak check: clean` 是每次运行的必要条件); 个人地址不得写入文档, 证据只记录服务商与域名.
- `ThreeStampMailPluginContractTest.mailServiceAnswersTheContractBinder` 对已安装的服务只做元数据与拒绝断言: instrumentation 运行在插件自身 uid 下, 不是宿主, 所以 `openSession` / `listSavedAccounts` MUST 抛以 `Caller is not the installed same-signer AutoJs6 host` 开头的 `SecurityException`; 会话信封与描述符用例改在 `mailBinderAnswersTheSessionEnvelope` 用同进程 `MailPluginBinder(context, CallerGuard.trusting())` 跑.
- `MailSessionBinderTest` MUST 覆盖 (回环 `ServerSocket` 只 accept 不应答, 绑定到显式 `127.0.0.1`): 取消执行中的调用在数秒内答 `CANCELLED` (读超时远大于此), 取消后会话仍可用且 `lastError` 为 `CANCELLED`, 未知 / 已完成 id 被忽略; 第 `MAX_QUEUED_CALLS` + 1 个排队调用 `LIMIT_EXCEEDED` 且其描述符副本已关闭, 取消排队调用即时 `CANCELLED`, `close()` 让排队与执行中的调用各答一次 `SESSION_CLOSED` 再 `onStatus(closed, "closed")`, 关闭后的调用 `SESSION_CLOSED`; 超过 `MAX_ENVELOPE_BYTES` 的请求信封在解析前 `LIMIT_EXCEEDED`, 不建连, 描述符副本已关闭; 拒绝的应答不来自测试线程.
- 路线图 P1.3 关闭时补充: 宿主 <-> 插件跨进程往返, 插件进程被杀后 `SESSION_CLOSED` 与自动重开, 绑定 / 解绑不泄漏.
- 路线图 P9 (浏览器登录) 的设备用例: `OAuthDeviceTest` MUST 覆盖登录页在浏览器中打开 (Custom Tab, 只看顶层 Activity 与截图, 不输入任何凭据), 回调 Activity 对外来 `state` 的拒绝与匹配 `state` 的一次性交换 (以伪造的 code 到达真实令牌端点, 服务商拒绝即为通过, 失败文案不得含 code), 以及 `OAUTH2` 记录的 Keystore 往返 / 续期 / 拒绝标记 / 撤销 / 重新授权 (一次性目录与测试密钥别名, 或一次性别名); 登录页 / 回调 / 撤销记录三类用例对每个有客户端 id 的服务商各一个方法 (Microsoft: `opensTheProviderPageInTheBrowser` 等; Google: `opensTheGooglePageInTheBrowser`, `refusesAForeignStateAndExchangesTheMatchingOneAtGoogle`, `aRevokedGoogleRecordRefusesEverySessionUntilANewSignIn`, 后者让撤销器把伪造的令牌真实发到 Google 的撤销端点并等其状态行), 没有客户端 id 时以 assumption 跳过; `RealAccountOAuthDeviceTest` 只经 `am instrument` 运行 (`.python/run_oauth_device.py`), 令牌只以 base64 instrumentation 参数进入设备 (`oauthRefreshTokenB64` 等, 来自 `build/outlook-token.properties`, 与构建的客户端 id 同一注册), 日志只含别名 / 服务商 / 秒数; Google 没有 PC 侧刷新令牌 (Android 客户端不接受回环重定向), 驱动以 `--plugin-only` 只跑插件侧步骤, 或以 `--signed-in-alias <alias>` 接手维护者在设备上真实登录的记录 (无种入, 重新授权步骤跳过, 结束时删除记录除非 `--keep`); 真实记录 (种入或登录) 之后 MUST 先跑 `RealAccountOAuthDeviceTest#opensASessionOverTheSavedAlias` (插件进程内经 `MailPluginBinder` 打开别名, `session.test` 各端点 XOAUTH2 与 `messages.list`, 即宿主 `mail.connect(alias)` 所做的事; 手机以 `--skip-host` 保留宿主状态时这是唯一的会话证据), 再撤销; `--signed-in-alias` 模式下驱动先跑步骤 5a `RealAccountOAuthDeviceTest#renewsAStaleAccessTokenAtTheProvider` (把记录的 access token 置为过期, 以真实 refresh token 到服务商续期, 即刷新路径; 种入记录由 seed 覆盖); `revokesTheSignIn` 等待 `TokenRevoker.revoke` 返回的服务商侧请求 (`Future<Boolean>?`) 的结果, 有撤销端点的服务商 MUST 接受真实令牌 (不等待时被测进程会在后台请求完成前结束, 状态行缺失); Android 9 上被测进程偶尔在 `ADB-JDWP Connection` 线程于启动时崩溃 (无任何 `TestRunner` 行), 驱动对这种无输出的 "Process crashed" 最多启动 4 次; 宿主侧以 `docs/smoke/oauth-status.js` 与 `docs/smoke/saved-account.js` 经 `run_host_script_smoke.py --alias` 验证, 报告从宿主测试的日志行读取 (connected 运行结束时被测宿主被卸载). 证据写入 `docs/dev/p9-oauth2-evidence.md`.
- 有设备或模拟器时执行 `:app:connectedDebugAndroidTest`; 性能度量与正确性测试分开.

## 16. CI 基线

- `build.yml`: push, pull request 与手动触发; `contents: read`; JDK 21 Temurin; 运行 `:mail-core:test` (GreenMail), 组装 debug / androidTest / release APK 与 lint, 上传产物; 在 API 24 (x86) 与 API 35 (x86_64) 模拟器上执行 instrumentation 契约测试.
- `markdown.yml`: Windows 环境运行 `.python\check_markdown.bat`, 阻止生成文档漂移.
- CI 不持有任何邮箱账户; 真实服务商验证只在维护者本地执行.
- CI action 使用固定大版本并定期更新; timeout 与真实构建时长匹配.

## 17. 验证顺序

按变更范围执行最小但充分的验证:

```powershell
py .python/generate_markdown.py --check
.\gradlew.bat :mail-core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease             # 引入或升级运行时依赖后 MUST 执行: R8 缺失类只会在这里以构建失败暴露
.\gradlew.bat :app:connectedDebugAndroidTest   # 有设备或模拟器时
```

Release 前额外执行 `.\gradlew.bat :app:appendDigestToReleasedFiles`, 检查 `releases/` 中只出现预期的已签名单 APK 且 CRC32 与文件内容一致. 任何未执行的验证都在最终说明中明确列出原因; 构建耗时较长时给予足够时间, 不用过短 timeout 误判失败.

## 18. 许可证, 安全, 隐私与第三方内容

- 根目录 `LICENSE` 为 Mozilla Public License 2.0, README 徽章与源码头保持一致.
- `android:allowBackup="false"` 与 `data_extraction_rules.xml` 全量排除保持不变.
- 不记录邮件正文, 附件内容, 密码, 授权码, 令牌, 服务器原始响应到普通日志; 诊断只保留服务商, 协议, 耗时, 大小与错误分类.
- 联网行为限定为脚本或预设指定服务器的 IMAP / POP3 / SMTP 连接; 超时, TLS 策略, 认证失败与证书错误的行为在 README 安全章节与错误码文档中明确.
- 第三方代码与 AAR 必须记录来源, 版本, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`); Angus Mail 按上游源码 LICENSE / NOTICE 的 EPL-2.0 及附条件的 GPL-2.0 + Classpath Exception 声明, 不将父 POM 的汇总 EDL 条目视为整个实现的额外许可. 引入或升级依赖时同一提交更新该文件.

## 19. 完成检查清单

- [ ] 第 2 节身份值在 Gradle, Manifest, Kotlin 常量, 资源, 文档与测试中一致; `ManifestContractTest` 通过.
- [ ] 平台插件只在根 settings 应用一次, 无 `mavenLocal()`, 无外部路径引用, 无 `gradle/data`.
- [ ] `libs/` AAR 与 `locks/host-api-aars.lock` 哈希匹配, `THIRD_PARTY_NOTICES.md` 已更新.
- [ ] `sign.properties`, `app/sm003.jks`, `mail-test-accounts.properties` 被 Git 忽略; `appendDigestToReleasedFiles` 可用.
- [ ] Wake Activity, INFO 服务, `MAIL` 服务契约完整; `getInfo()` 显式 `supportedAbis = emptyArray()`.
- [ ] 10 语言资源与文档完整, ASCII 标点, `plugin_description` 无句尾点号; 图标由脚本生成.
- [ ] JSON 文案源已生成产物且 `--check` 通过; 当前版本全部语言 changelog 已更新.
- [ ] `:mail-core:test`, 单元测试, assemble, lint 通过; 有设备时 instrumentation 通过, 否则明确记录.
- [ ] `ROADMAP.md` 已勾选完成条目并写入证据; `VERSION_BUILD` 与提交数一致; `git status --short` 无输出.

## 20. 参考项目路由

只读取完成当前任务所需的参考, 不复制项目专属内容:

- 单 APK 骨架, 宿主 AAR 哈希锁定, `THIRD_PARTY_NOTICES.md`, Bundle + JSON 桥接与设置页: `D:/idea-projects/AutoJs6-Plugin-MCP-Server`
- 构建平台, Wake 激活, PluginInfo, 多语言生成, README 样式与 Compose UI 套件: `D:/idea-projects/AutoJs6-Plugin-OpenCC`
- 纯 JVM PluginInfo 与真实 Binder 测试: `D:/idea-projects/AutoJs6-Plugin-Pinyin4j`
- 独立设置页, 跟随宿主主题与内置发行历史: `D:/idea-projects/AutoJs6-Plugin-Three-Stone-AI`
- `ParcelFileDescriptor` 流式落盘 (`ArchiveEntryMaterializer` 模式), 宿主入口, 插件发现, 安装和启用引导: `D:/idea-projects/AutoJs6`

参考时以这些仓库的当前代码为准, 不以历史 README 或旧 release 中已经淘汰的写法为准.


## Standalone settings convention (2026-09-29)

- Follow the workspace `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`: language, dark mode, theme color, launcher icon; flat grouped rows, 16/14sp text, 72dp minimum, 24dp padding, a 24dp line icon in a 40dp slot, and text-aligned dividers.
- Appearance dialogs keep draft changes local until OK. Use the shared neutral surfaces and HCT primary/onPrimary roles. Presets and a single HEX/RGB field preview the actual control colors; Cancel writes nothing.
- New launcher installs default to Auto. Keep PackageManager as the sole mode store, normalize mixed upgrade states through the internal MY_PACKAGE_REPLACED receiver, preserve unique explicit choices, and migrate mutable shortcut ownership before disabling an alias.
- Host appearance acquisition and IPC must run on a worker. Cache in process, discard stale generations, refresh only effective followed fields, and defer host-driven recreation after user interaction or while a dialog is open. Never rewrite local choices because the host is unavailable.

## Optical icon standard (2026-10-03)

- Follow `../AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` for every standalone plugin, including the Plugin Center. `.python/icon_geometry.py` v1 is a self-contained copy of the common geometry algorithm; keep its implementation identical across the standalone plugins. Never read sibling checkouts during a build.
- Derive size from the equal-weight combination of visible bounding-box area (alpha >= 16) and alpha-weighted ink area. Target visible size is 0.52 of the canvas, with advisory optical corrections of 0.80-1.20. Values outside this range are allowed when canvas, transparency and safe-circle checks pass; verify shipped size against the saved recipe. The adaptive ratio is always the UI ratio multiplied by 72/108. This supersedes older hardcoded UI/adaptive widths in historical notes. Preserve aspect ratio, optical placement and final nonzero-alpha safety checks.
- Current derived widths: UI 0.5981, adaptive 0.3988 (rounded documentation values, not generation constants). Optical scale=1.00 and zero offsets.
- Generate `mipmap/ic_plugin_center.png` and its night counterpart from the same geometry as the transparent UI/launcher mode. They are transparent neutral artwork for installed and catalog entries, independent of the active launcher alias. Keep them through `raw/keep_plugin_center_icon.xml`. Existing separate brand assets retain their original purpose.
- Black, white and neutral grayscale are allowed for every plugin without per-plugin approval. Pure silhouettes default to #272727 / #D8D8D8; shaded artwork may preserve meaningful tonal details with R=G=B and matching day/night alpha. Stamp Mail is one example, not an exception. Keep light-theme artwork dark enough and dark-theme artwork light enough to remain legible. Do not introduce a filled background into the Plugin Center assets.
- Run the icon generator and its read-only `--check`, `.python/tests/test_icon_geometry.py`, existing icon regressions, and review the full set at 36/48/64 px in both themes and in launcher masks. `.github/workflows/icons.yml` verifies Windows/Linux reproducibility. Synthetic previews do not replace actual launcher verification.


## Icon Studio publication snapshot (2026-10-04)

- `.icons/recipe.json` and its content-addressed original assets own the current icon geometry, tone and backgrounds. Keep the portable renderer, generated resources, keep rules and icon CI in the same change.
- Use `.python/generate_icon_studio.py --check` for read-only reproduction checks. Optical size bands are advisory; retain canvas, transparency and safe-circle checks. Three uses neutral foregrounds and fixed #FAFAFA / #212121 surfaces; other plugins may use colored artwork and custom or transparent surfaces.
