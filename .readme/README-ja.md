<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 スクリプトから IMAP, POP3, SMTP でメールを送受信, 検索, 監視する</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語

******

現在の README.md は以下の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### はじめに

******

Angus Mail は AutoJs6 スクリプトにグローバルオブジェクト `mail` を提供し, メールの送信, メールボックスの一覧と検索, 本文の読み取り, 添付ファイルのダウンロード, フラグとフォルダーの管理, フォルダーの新着監視を可能にします. Jakarta Mail の参照実装である [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5 を基盤とし, TLS 上で IMAP, POP3, SMTP を扱います.

すべてのメール通信はプラグインのプロセス内で完結します. AutoJs6 は Binder サービスを通じてプラグインを検出し, スクリプトが渡したアカウント (またはプラグインの設定ページに保存したエイリアス) を引き渡し, JSON の結果と添付ファイルのストリームを受け取ります. ホスト自体にはメール関連のコードは含まれません. プラグインにアカウントを保存しない限り, 資格情報はセッションの存続期間中だけメモリに置かれます.

******

### 現在の状態

******

バージョン 1.0.0 は開発中です: リポジトリの骨組み, ローカルサーバーテスト付きのメールコア, AutoJs6 プラグインセンター向けのプラグイン識別情報が整い, Binder コントラクト, スクリプト API, 設定ページは [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) のフェーズに沿って進めます. AutoJs6 6.8.0 (ビルド 5282) 以降が必要です.

******

### 機能

******

プラグインは以下の機能を提供します:

- 送信: プレーンテキストまたは HTML, 複数の宛先, 添付ファイルとインライン画像, カスタムヘッダーと優先度に対応し, プロバイダーが送信済みコピーを保存しない場合はプラグインがサーバーに保存します.
- 受信: フォルダーをページ単位で一覧し, サーバー側で検索し (非 ASCII 検索を拒否するプロバイダーではクライアント側フィルターにフォールバック), テキストと HTML の本文を読み取り, 添付ファイルをスクリプトの作業ディレクトリへ直接ダウンロードします.
- 整理: 既読やフラグの付与, 移動, コピー, 削除, 完全削除, フォルダーの作成, 名前変更, 削除に対応します. POP3 アカウントは読み取り専用のサブセットになります.
- 監視: スクリプトの実行中, IMAP IDLE で新着メールのイベントを受け取り, 非対応サーバーや POP3 アカウントではポーリングにフォールバックします.
- プロバイダー: Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun のプリセットがホスト, ポート, 暗号化を補完し, 他のサーバー向けに任意の項目を上書きできます.
- 認証: パスワードとプロバイダーの認証コード, またはスクリプトが更新コールバックとともに渡す XOAUTH2 アクセストークンに対応します.

******

### 使い方

******

1. AutoJs6 ビルド 5282 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `Angus Mail` が認識されていることを確認して有効化します.
3. アカウントを準備します: メールプロバイダーの設定で IMAP または POP3 を有効にし, 認証コードやアプリパスワード (QQ, 163, 126, Gmail, iCloud), または OAuth 2.0 アクセストークン (Outlook.com) を取得します.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページにアカウントを保存してエイリアスで接続します.

******

### クイックスタート

******

レポートを送信し, 添付ファイル付きの未読メールを読み, 確認コードを待つスクリプト:

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

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder のエントリポイントは署名パーミッション `org.autojs.permission.PLUGIN` で保護され, AutoJs6 だけが到達できます. プラグインは他のコンポーネントをエクスポートしません.
- INTERNET パーミッションはスクリプトが指定したサーバーへの IMAP, POP3, SMTP 接続にのみ使われ, プラグインは他のリクエストを行わず, データを収集しません.
- パスワードとトークンはスクリプトからプラグインへ Binder の専用フィールドで渡され, ログ, JSON ドキュメント, エラーメッセージ, クラッシュレポートには現れず, セッションの存続期間中だけメモリに置かれます. 設定ページに保存したアカウントは Android Keystore の鍵で暗号化され, バックアップから除外されます.
- 接続は既定で TLS を使います (プロバイダーの要件に応じて SSL または STARTTLS). 平文接続と自己署名証明書はアカウントごとに明示的に指定する必要があります.

プラグインは公式の [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) ページまたは AutoJs6 のプラグインセンターからのみ入手してください. 出所不明のパッケージは, バージョン番号が同じに見えてもホストの検証に失敗したり, リスクを伴う可能性があります.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストおよびプラグインの開発者向けです. ホストはこれらの識別子を使ってプラグインを検出し, 互換性を交渉します:

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

`AngusMailPluginService` はホストの mail-api コントラクト `org.autojs.plugin.mail.api.IMailPlugin` を実装し, `org.autojs.plugin.MAIL` (category `mail`) に応答します. `AngusMailPluginInfoService` は `org.autojs.plugin.INFO` に PluginInfo で応答します. `WakeActivity` によりホストがプラグインを起動できます.

******

### ロードマップ

******

プラグインの計画と進捗は ROADMAP.md にチェック可能なリストとして管理され, 段階ごとに受け入れ基準と証拠レベルが付いています. 未チェックの項目は現在の機能ではなく意図を表します. Issues での議論を歓迎します.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/09/19_

- `ヒント` P0 開発プレビュー: リポジトリの骨組み, ローカルサーバーテスト付きのメールコア, AutoJs6 プラグインセンター向けのプラグイン識別情報. Binder コントラクト, スクリプト API, 設定ページは ROADMAP.md のフェーズに沿って進めます.
- `機能` プラグイン識別子 `angus-mail` (エンジン `mail`), INFO サービス, Wake Activity, および `org.autojs.plugin.MAIL` サービス; その `IMailPlugin` Binder はプラグイン情報, 機能, プロバイダーと保存済みアカウントの一覧, セッションエンベロープに応答 (各操作は P2 で実装)
- `機能` Eclipse Angus Mail 上のメールコア: SSL または STARTTLS を使う IMAP / POP3 / SMTP のセッション設定, パスワードと XOAUTH2 認証, SMTP 送信と IMAP 受信トレイ一覧をローカルの GreenMail サーバーで検証
- `機能` メールコアのアカウント層 (ロードマップ P2.1): Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun のプロバイダープリセット, プロトコルごとのタイムアウト, `tls.trustAll`, IMAP `ID` コマンド, マスク済みの `debug` トレースを備えたアカウントオプション; セッションは遅延接続し, アイドル接続を切断し, 切断後に再接続; `session.test` は Binder 経由でエンドポイントごとの機能と往復時間を返す
- `機能` 送信 (ロードマップ P2.2): `mail.send` は to / cc / bcc / replyTo, テキストと HTML 本文 (`multipart/alternative`), ホストから渡されたディスクリプタから読む添付ファイルとインライン画像 (`multipart/mixed` / `multipart/related`), カスタムヘッダー, 優先度, `inReplyTo` / `references`, 日付に対応; 宛先, 添付, ヘッダーの上限とヘッダーインジェクションの拒否; `saveToSent` はプロバイダーが自動保存しない場合にのみ IMAP でコピーを追加; `messages.append` は下書きを保存して UID を返す; 実機で QQ メールと Gmail に対して検証済み
- `機能` 受信 (ロードマップ P2.3): `folders.list` (ツリー, special-use ロールは LIST / XLIST 属性または慣用名から取得, 件数は任意), `folders.status` / `create` / `delete` / `rename`; `messages.list` は UID をカーソルにし (`before` / `after`), `order` と `unseenOnly` に対応, エンベロープのみ取得 (`hasAttachments` は BODYSTRUCTURE で判定); `messages.search` はクエリ JSON (`from` / `to` / `subject` / `body` / `text` / 日付 / フラグ / サイズ / `header` / `messageId` / `uid` と `and` / `or` / `not`) を IMAP SEARCH にコンパイルし, サーバーが拒否した場合はクライアント側で絞り込む (`fallback`); `messages.get` はテキストと HTML 本文 (HTML のみのメールはテキストを派生), 全ヘッダー, `partId` 付きの添付一覧, インライン上限 (`bodyTruncated` / `bodyParts`) と `includeRaw` を返す; `attachments.download` と `messages.raw` はホストのディスクリプタへ進捗付きでストリーミング; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; 文字セット復元は GBK / GB 18030 / ISO-2022-JP, 未宣言および未知の文字セット, 生の 8 ビットヘッダーと RFC 2231 / 2047 ファイル名に対応, ファイル名を無害化; IMAP `ID` コマンドは接続ごとに送信 (163 / 126 は未識別の接続を拒否), サーバーが `UID EXPUNGE` を解釈できない場合はフォルダー全体の `EXPUNGE` に切り替え; 実機で QQ メール, 163 メール, Gmail に対して検証済み
- `機能` POP3 (ロードマップ P2.4): `receive: "pop3"` のアカウントは同じ操作で唯一の `INBOX` を読み, `uid` は UIDL 文字列: `folders.list` は `INBOX` のみを返し (件数は任意), `messages.list` は UIDL カーソルでページングしヘッダーのみ取得 (`TOP`), `messages.search` はクライアント側でヘッダーを新しい順に絞り込み `limit` 件そろった時点で停止 (候補は最大 200 件, 1 件につき `TOP` 1 往復), `messages.get` / `messages.raw` / `attachments.download` はメール全体をダウンロード, `messages.delete` は `DELE` を発行しメールボックスを閉じるときに確定; フラグ, 移動, コピー, expunge, 追加, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` と本文検索は接続前に `UNSUPPORTED_OPERATION` を返す; 実機で QQ メールに対して検証済み
- `機能` Binder セッション制御 (ロードマップ P2.5): セッションを開けるのと保存済みアカウントを一覧できるのは, インストール済みで本プラグインと同じ鍵で署名された AutoJs6 ホストだけ (それ以外は `SecurityException`, MCP Server プラグインと同じ規則); リクエストとレスポンスのエンベロープは `MAX_ENVELOPE_BYTES` 以内, エラーメッセージは `MAX_ERROR_MESSAGE_BYTES` 以内; 各セッションは呼び出しを順に実行し, 実行中の呼び出しの後ろに最大 `MAX_QUEUED_CALLS` 件をキューに入れ, それ以上は `LIMIT_EXCEEDED` で拒否; `cancel` はキュー内の呼び出しに即座に応答し, 実行中の呼び出しはソケットを閉じて中断するため, 応答しないサーバーで読み取りタイムアウトまで待つことはなくなった; `close` は保留中の全呼び出しに `SESSION_CLOSED` を返す; `getStatus` は `queued` と `active` を報告; 操作表は各操作が対応する受信プロトコルを示し, POP3 アカウントは引数の解析前に拒否される; capabilities は `append` と `clientSearchFallback` を宣言
- `機能` 10 言語の README, プラグインセンターの説明, 更新履歴
- `機能` 保存済みアカウントストア (ロードマップ P4.1): プラグイン内に保存したアカウントは, 秘密を含まないアカウント文書と, Android Keystore のマスターキーで AES-256-GCM 暗号化したパスワードまたはアクセストークンを一緒に保持; 認証付きデータがエイリアス, 秘密の種類, 文書を結び付けるため, ディスク上で書き換えられたり移動されたレコードは復号できない; レコードは `noBackupFilesDir` (元からバックアップ対象外) に置かれ, ファイルロック下でアトミックに書き込まれ, 秘密は使用後に消去される `CharArray` / `ByteArray` バッファだけを通る; エイリアスは前後の空白を除き, NFC 正規化され, 大文字小文字を区別しない
- `機能` 保存済みアカウントのセッション (ロードマップ P4.3): `openSession` はエイリアス形式 (`accountAlias`) を受け付け, プラグインのプロセス内で秘密を復号するため, `mail.connect('alias')` が Binder 経由で資格情報を運ぶことはない; `listSavedAccounts` は保存済みアカウントごとにエイリアス, アドレス, ユーザー名, プロバイダー, 認証方式, 受信プロトコル, エンドポイント, 既定マークを秘密なしで返す; 機能セットに `savedAccounts` を追加
- `修正` プロバイダープリセット (ロードマップ P3.2): 163 Mail と 126 Mail は SMTP で送信したすべてのメールをサーバー側で保存するため, 両者の `autoSavesSent` を true にし, 既定の `saveToSent` が `已发送` に 2 通目のコピーを追加しないようにしました (実際の 163 アカウントで確認: `saveToSent: false` で送信したメールが数分後に送信済みフォルダーに現れました)
- `修正` 共有ビルドプラグイン 1.8.3 により, AGP 9.1 での SDK XML v4 解析警告と, JVM 単体テストの組み立て時に APK ネイティブライブラリのアラインメント検証が誤って実行される問題
- `改善` エラーマッピング: アカウントの POP アクセスが無効なためログイン後にメールボックスを拒否する POP3 サーバー (Gmail は STAT に `[SYS/PERM] Your account is not enabled for POP access` と応答) に対して, 再試行可能な `IO_FAILED` "I/O failed" ではなく原因を示すメッセージ付きの `UNSUPPORTED_OPERATION` を返すようにしました
- `改善` プロバイダープリセット: Sina Mail に送信済みフォルダー名 (`已发送`) を追加し, サーバーが送信済みメールのコピーを保持せず IMAP CREATE を拒否する (フォルダーはウェブ UI でのみ作成可能) ことを注記しました. 126 Mail のサーバー側送信済みコピーは実アカウントで検証済みです
- `依存関係` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) と Angus Activation 2.0.3, Jakarta Activation API 2.1.4
- `依存関係` JVM メールコアテスト用に GreenMail 2.1.13 を追加 (テストスコープのみ)
- `依存関係` 共有プラグインコントラクトとして `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5282, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
- `依存関係` メール Binder コントラクトとして `mail-api.aar` (AutoJs6 モジュール `plugin-api/mail-api`, ホストビルド 6.8.0 / 5282, MPL 2.0; 6 つの AIDL インターフェース, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`) を追加し, `locks/host-api-aars.lock` でハッシュを固定

##### さらに詳しいリリース履歴

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルドと検証

******

このセクションはソースからプラグインをビルドしたい開発者向けです. 通常のユーザーは Releases ページのビルド済み APK をインストールするだけで済みます.

デバッグ APK をビルドする:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM ユニットテストを実行し, インストルメンテーションテスト APK をビルドする:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

リリース APK をビルドする:

```powershell
.\gradlew.bat :app:assembleRelease
```

リリース成果物を収集し, ファイル名にバージョンと CRC32 ダイジェストを追加する:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

多言語ドキュメントのソースと生成物が同期していることを検証する (CI でも実施):

```powershell
py .python\generate_markdown.py --check
```

ビルドには JDK 21 以降と Android SDK 37 が必要です. Gradle とプラグインのバージョンは `version.properties` と `io.github.supermonster003.autojs6-platform-versions` で一元管理されます.

******

### ローカライズとドキュメント生成

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

`.readme/` と `.changelog/` の言語 JSON ファイルが README, プラグインセンターの説明, 変更履歴の唯一のソースです. 常にこれらの JSON ソースを編集して `py .python/generate_markdown.py` を再実行してください. 生成された README, `plugin_instruction.md`, 変更履歴は手で編集しません. `py .python/generate_markdown.py --check` を実行するとすべての生成物を検証できます.

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE) の下で提供されます. サードパーティのコンポーネントとそのライセンスは [サードパーティ通知](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md) に記載しています.

******

### リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
