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
- 監視: スクリプトの実行中に新着メールのイベントを受け取り, サーバーが実際にプッシュする場合は IMAP IDLE, そうでない場合はポーリング (既定 60 秒, 調整可能) を使います: QQ と Sina は IDLE を受け付けても通知せず, 163 と 126 には IDLE がなく, POP3 アカウントは常にポーリングです; ネットワーク断やプラグインプロセスの再起動後も監視は自動的に復帰します.
- プロバイダー: Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun のプリセットがホスト, ポート, 暗号化を補完し, 他のサーバー向けに任意の項目を上書きできます.
- 認証: パスワードとプロバイダーの認証コード, またはスクリプトが更新コールバックとともに渡す XOAUTH2 アクセストークンに対応します.

******

### 使い方

******

1. AutoJs6 ビルド 5282 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `Angus Mail` が認識されていることを確認して有効化します.
3. アカウントを準備します: メールプロバイダーの設定で IMAP または POP3 を有効にし, 認証コードやアプリパスワード (QQ, 163, 126, Gmail, iCloud), または OAuth 2.0 アクセストークン (Outlook.com) を取得します.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページ (プラグインのランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) にアカウントを保存してエイリアスで接続します.

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
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 権限は設定画面の案内ボタンのためだけにあります: ボタンはシステムがバックグラウンドでプラグインを一時停止しうるかを表示し, 求めに応じてシステムのダイアログを開きます; プラグインが自ら要求することはなく, この除外に依存する機能もありません. P5 の監視マトリクスはこの除外の意味を実測しました: 画面を消してしばらくすると (Doze) Android はバックグラウンドアプリのネットワークを凍結し, 監視は切断され, 再接続はタイムアウトし, 新着メールは端末が起きてから数分後に報告されます (Android 9 で約 4 分; Doze の終了時にプラグインは即座に再接続します); 除外すると監視は接続を保ちます.

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
- `機能` 設定画面 (ロードマップ P4.2): ランチャーアイコンからアカウント一覧を開き, 保存済みアカウントごとにアドレス, プロバイダー, 受信プロトコル, 認証方式を表示し, 編集, 接続テスト, 既定の設定と解除, 削除を提供; アカウント編集画面はプロバイダーのプリセットを自動入力するか, IMAP / POP3 / SMTP サーバーを暗号化方式とポートまで手動で受け付け, パスワードまたはアクセストークンを入力欄から使用後に消去される `CharArray` へ直接読み取り, 編集時に空欄なら保存済みの秘密を維持し, 保存前に入力したサーバーへ `session.test` を実行してプロトコルごとの結果と所要時間をディスクに書かずに表示; 画面は AutoJs6 ホストのテーマ, ナイトモード, 言語に従い, 再生成された編集画面は秘密以外のすべての項目を復元
- `機能` 設定エントリ (ロードマップ P4.3): AutoJs6 ホストはエクスポートされた `org.autojs.plugin.MAIL_SETTINGS` アクティビティ経由でアカウント一覧を開く; このアクティビティはプラグイン権限を要求し, パラメータのないリクエストだけを受け付けて直ちに終了する; 機能セットは `mailSettingsVersion` 1 を宣言; ランチャーのエントリ自体はこの権限を持たない
- `機能` リリース履歴 (ロードマップ P4.5): 設定画面と情報画面からリリース履歴ページを開ける; 内容はプラグインに同梱された現在の言語の変更履歴 (翻訳がない場合は英語) から描画され, バージョンごとに日付とタグ付きの項目を持つカードを表示; プラグイン自身は更新確認を行わず, 更新は AutoJs6 のプラグインセンターに従う
- `機能` 電池の最適化の案内 (ロードマップ P4.6): 設定画面はシステムがバックグラウンドでこのプラグインを一時停止しうるかを表示し (`PowerManager.isIgnoringBatteryOptimizations`), 変更点を説明したうえで `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` によりシステムのダイアログを開く; そのためマニフェストは `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` を宣言; 起動時には何も要求せず, この除外に依存する機能もない
- `機能` 新着メール監視, IMAP IDLE (ロードマップ P5): メールコアは専用の接続で `IMAPFolder.idle` によりフォルダーを監視し, IDLE を 24 分ごとに更新し, 届いたメールを UID で (エンベロープ, または要求時は本文も) 一度だけ取得し, 切断後は指数バックオフ (1 s から 5 min まで, ジッター付き) で再接続し, フォルダーの UIDVALIDITY が変わったときは `resync` を報告; IDLE のないサーバーや 3 回連続で失敗した IDLE は `mode` イベントとともに監視をポーリングへ切り替え; セッションが保持する監視は最大 `MAX_WATCHES_PER_SESSION` 件で, セッションとともに閉じられる
- `機能` 新着メール監視, ポーリング (ロードマップ P5): `mode: "poll"` の監視は `pollIntervalMs` ごと (既定 60 s, 下限 `MIN_POLL_INTERVAL_MS`, 上限 1 時間) にポーリング間で維持する接続でフォルダーを UID で差分; POP3 アカウントは常にポーリングされ, ポーリングごとに 1 回ログインして UIDL で差分するためメールドロップは間に解放され, 追加のみを報告し削除は報告しない; 最初のポーリングはスナップショットを取るだけで未処理分を報告しない
- `機能` Binder 経由の新着メール監視 (ロードマップ P5): `IMailSession.watch` はメールコアの監視の上に監視を開いて即座に応答 (セッションが閉じている, `MAX_WATCHES_PER_SESSION` に達した, オプションが使えない, ホストのコールバックがすでに死んでいる場合は null を返し, 理由はセッション状態に記録); イベントは配信スレッドからホストの `generation` と 1 から数える `seq` を伴ってホストの `oneway` コールバックへ届き, ホストが消費を止めると `MAX_WATCH_QUEUE` 件のキューは 1 件の `resync` にまとめられ, `MAX_ENVELOPE_BYTES` を超えるイベントは本文なしか `resync` として送られ, `stop`, セッションの終了, ホストの死亡はいずれも 1 件の `closed` イベントで監視を終え, 既定ネットワークの変更や喪失は実行中の監視を直ちに再接続させ, 機能一覧は `idle` を宣言するようになった
- `修正` プロバイダープリセット (ロードマップ P3.2): 163 Mail と 126 Mail は SMTP で送信したすべてのメールをサーバー側で保存するため, 両者の `autoSavesSent` を true にし, 既定の `saveToSent` が `已发送` に 2 通目のコピーを追加しないようにしました (実際の 163 アカウントで確認: `saveToSent: false` で送信したメールが数分後に送信済みフォルダーに現れました)
- `修正` 共有ビルドプラグイン 1.8.3 により, AGP 9.1 での SDK XML v4 解析警告と, JVM 単体テストの組み立て時に APK ネイティブライブラリのアラインメント検証が誤って実行される問題
- `修正` アカウントエディター (ロードマップ P4.7): フォーム全体を Android の自動入力フレームワークから除外し, パスワードマネージャーが認証コードの保存を求めなくなりました. これまで HyperOS (API 35) では保存後にエディターを閉じると "アカウントとパスワードを保存" のシートが表示されていました.
- `修正` QQ, Sina, 163, 126 での新着メール監視 (メールロードマップ P5 デバイスマトリクス): プロバイダープリセットに `idlePush` が加わり (カタログ版 2), この 4 社では `mode: auto` が最初からポーリングし IDLE には入らない. QQ と Sina は IMAP IDLE を受け付けてもクライアントのアイドル中に一度もプッシュせず (実アカウント, 2026-09-19: 10 分間タグなし応答なし; Sina は 60 秒後に接続を切る), 163 と 126 には IDLE 自体がないため; 明示的な `mode: 'idle'` は従来どおり IDLE に入る. マトリクス本体 (API 24 エミュレーターと Sony 2 台で QQ, Redmi で 163: プラグイン強制終了, ネットワーク断, Wi-Fi からモバイル回線へ, 強制 Doze) は `docs/dev/p5-watch-evidence.md` に記録され, スモークスクリプト `docs/smoke/watch.js` とドライバー `.python/run_watch_matrix.py` が付属する; 同じマトリクスで Doze がバックグラウンドアプリのネットワークを凍結することも判明した (監視の再接続がタイムアウトし, Android 9 では新着メールが起床後約 4 分で報告された) ため, プラグインは端末が Doze を抜けた瞬間に監視を再接続し, 設定画面の電池最適化の案内文はこの除外の意味を説明する
- `修正` TLS マトリクス (メールロードマップ P6): 暗黙の SSL, STARTTLS (GreenMail の前段に置いた STARTTLS プロキシ経由), 平文, `tls.trustAll` あり / なしの自己署名証明書, ホスト名の一致しない信頼済み証明書, ポートとモードの不一致, アップグレードを提供しないポートを IMAP / POP3 / SMTP について検証した (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`). 検証で Angus の POP3 ストアが STLS アップグレードの欠如とハンドシェイク前のタイムアウトを認証失敗として報告することが分かり, エラーマッパーはこれらを `AUTH_FAILED` ではなく `TLS_FAILED` と `TIMEOUT` で報告するようになった. `TlsDeviceTest` は API 24 / 28 / 33 で, メールコアがプラットフォーム既定のまま TLS 1.2 専用サーバーに接続でき, API 29 以降は TLS 1.3 をネゴシエートすることを確認する
- `修正` 文字セットマトリクス (メールロードマップ P6): GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR, UTF-8 を件名, 表示名, 本文, 2 種類のファイル名形式について, 宣言あり / 宣言なし / 誤った宣言の 3 形態で検証し (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`), API 24 / 28 / 33 でも確認した (`CharsetDeviceTest`). 検証で 2 つのデコード欠陥を修正: `us-ascii` / ISO-8859-1 と宣言されながら UTF-8 や GB 18030 のバイトを含む本文やヘッダーは Latin-1 の文字化けになっていた (Jakarta は `us-ascii` を決して失敗しない ISO-8859-1 に対応付ける) が推測チェーンを通るようになり, エンコード語のない生の ISO-2022-JP の件名と本文はエスケープシーケンスで認識されるようになった. Big5 と EUC-KR は引き続き宣言が必要 (そのバイト対は GB 18030 としても有効)
- `修正` プロバイダーマトリクス (メールロードマップ P6): QQ, 163, 126, yeah.net, Sina を実アカウントで送信, 一覧, サーバー側とクライアント側の中国語検索, 本文, 添付バイト, フラグ, フォルダー作成, 移動, ウォッチ, POP3 まで通し (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`), 差異をプリセットの notes に記録した: QQ は中国語 SEARCH に OK かつヒットなしで応答し (`fallback: 'always'` を使う), Message-ID と ENVELOPE の To 表示名を書き換え, CREATE を拒否し, 自ら保存した送信済みコピーはしばらく UID で指定できない. 163 は最近のメールに対する文字列検索にヒットを返さない. Sina は ALL, SINCE とフラグの検索キーしか受け付けない. 5 社ともカスタムキーワードを保存しない. 併せてクライアントの欠陥を修正: SASL continuation で拒否された POP3 XOAUTH2 ログイン (Gmail) は Angus Mail が拒否を無視するため再試行可能な `IO_FAILED` として報告されていたが, メールコアがログインを検証して `AUTH_FAILED` を返すようになった (`Pop3OAuthScriptedTest`). Gmail (トークン失効), Outlook.com と iCloud (アカウントなし) は未検証のまま
- `修正` ライフサイクルマトリクス (メールロードマップ P6): セッションと watch を保持するスクリプトを Redmi (API 33) 上で 8 通りの方法で終了: 通常終了, watch とクライアントを開いたままの `exit()`, `engines.stopAll()`, ホストの強制停止, プラグインの強制停止, プラグインのその場更新, プラグインの無効化, プラグインのアンインストール; いずれの場合もプラグインの接続, バインド, ファイル記述子は 30 s 以内にスクリプト前の状態に戻り, ホストがサーバーへ直接接続することはなかった (`docs/dev/p6-lifecycle-matrix.md`, `.python/run_lifecycle_matrix.py`, `docs/smoke/lifecycle.js`). マトリクスはリークを 1 件発見し修正済み: Angus Mail は書き込みタイムアウト用にソケットごとのスレッドプールを作り, TLS ソケットがそのラッパー経由で閉じられたときにしか停止しないが, 下層の平文ソケットが cancel や watch 停止で先に閉じられると Android はその手順を飛ばすため, そうした接続ごとにプロセスの生存期間中スレッドが 1 本残っていた; メールコアは共有のデーモンタイマーを Angus に渡すようになった (`WriteTimeouts`, `WriteTimeoutExecutorTest`)
- `修正` プロバイダーマトリクスの Gmail と Outlook.com の列 (メールロードマップ P6, メンテナーが Gmail トークンを更新し Outlook.com / Hotmail の 3 アカウントを提供した後に補完): Gmail はトークンでセッションテスト, フォルダー, 送信, 一覧, サーバー検索, 本文, 添付, フラグとキーワード, フォルダー作成, 移動, watch, POP3 を通過し, その IDLE は実際にプッシュする (新着は配信後およそ 30 s で報告され, これが Gmail の通知周期), P5 のデバイスマトリクス (基準, プラグイン強制終了, Wi-Fi オフ) を Redmi (API 33) で IDLE 経路にて再実行 (`docs/dev/p6-provider-matrix.md`, `docs/dev/p5-watch-evidence.md`). 2 つの発見がメールコアを変えた: Gmail は `UTF8=ACCEPT` を広告し, Angus Mail が有効化すると Gmail は非 ASCII 文字を含むあらゆる検索を拒否する (`BAD Could not parse command`) ため, 日本語や中国語の件名検索はクライアントへフォールバックしていた; この拡張はもう有効化せず, 検索は他のサーバーと同様に `CHARSET UTF-8` を伴う (`Utf8SearchTest`). クライアント側フィルターは 25 候補ごとにデバッグトレースへ進捗を報告するようになった. `fallback: 'always'` の本文検索は一致が `limit` に満たないと 2000 候補の窓を全部走査し, 候補ごとに 2, 3 往復, このネットワークから Gmail へは約 43 分かかるため. Outlook.com: 3 アカウントともアプリパスワードに対し IMAP と POP3 で `Basic authentication is disabled`, SMTP で `535` を返すので, プリセットの XOAUTH2 限定ルールは妥当であり, 操作行はトークン待ち (ロードマップ P9); プリセットを迂回した場合, サーバー自身の拒否 (`LOGINDISABLED`, `AUTH=XOAUTH2` のみ) は `SERVER_ERROR` ではなく `AUTH_MECHANISM_UNSUPPORTED` として報告される (`LoginDisabledTest`)
- `改善` エラーマッピング: アカウントの POP アクセスが無効なためログイン後にメールボックスを拒否する POP3 サーバー (Gmail は STAT に `[SYS/PERM] Your account is not enabled for POP access` と応答) に対して, 再試行可能な `IO_FAILED` "I/O failed" ではなく原因を示すメッセージ付きの `UNSUPPORTED_OPERATION` を返すようにしました
- `改善` プロバイダープリセット: Sina Mail に送信済みフォルダー名 (`已发送`) を追加し, サーバーが送信済みメールのコピーを保持せず IMAP CREATE を拒否する (フォルダーはウェブ UI でのみ作成可能) ことを注記しました. 126 Mail のサーバー側送信済みコピーは実アカウントで検証済みです
- `改善` プロバイダープリセット: Yahoo Mail と Aliyun Mail の注記に, これらのプリセットが実アカウントで未検証であること (プロジェクトにテストアカウントがない) を明記し, 送信済みコピーの挙動は公開ドキュメントに従います.
- `改善` 秘密情報の監査 (メールロードマップ P6): メールコアとアプリからログ出力, コンソール出力, Jakarta のデバッグスイッチ, および秘密情報が実体化されるすべての箇所を検索した. その結果 (`docs/dev/p6-secret-audit.md`) は `SecretAuditTest` によって強制される: ログやデバッグ文があればビルドが失敗し, `reveal()` は Jakarta の 3 つの認証呼び出しに固定され, Jakarta セッションが決してデバッグしないこと, アカウント JSON 内の秘密情報がエコーされずに拒否されること, 値オブジェクト, 例外マッパー, プロトコルトレースが秘密情報をあらゆる形式でマスクすることを検査する
- `改善` 敵対的入力 (メールロードマップ P6): メールが何を含んでいてもメッセージ文書は有界になった (4 つのアドレスリストは合計 500 件, アドレスと表示名は 320 文字, 件名 / 識別子 / ヘッダー値は 4096 文字, `headers` マップは 64 KiB, MIME ツリーは深さ 32 と 256 パート, サイズ不明の本文はインライン予算までしか読まない) ため, 1 通の敵対的メールが `LIMIT_EXCEEDED` で一覧ページ全体を塞ぐことはなくなった. 壊れた base64, 未知の転送エンコーディング, boundary のない / 空の multipart は接続断と誤認されず寛容にデコードされる. 深すぎる / 解析不能な multipart は 1 つのリーフとしてダウンロード可能なまま. `HostileInputTest` と `HostileInputGreenMailTest` (17 ケース, `docs/dev/p6-hostile-input.md`) が深い / 広い MIME, 20000 件の宛先, ヘッダー爆弾, Content-Type の欠落, 不正な base64, 再帰的な `message/rfc822`, 敵対的なファイル名, サイズ不一致, 不正な UTF-8 を網羅する
- `改善` パフォーマンスベースライン (メールロードマップ P6): シード済みローカルサーバー (10000 通の受信箱, 50 MiB の添付) に対して一覧, 検索, ダウンロード, 送信, 1 時間の IDLE 待機を JVM, Redmi (API 33), Sony (API 28) で計測し, 結果を `docs/dev/p6-performance-baseline.md` に記録. これにより 2 点を変更: IMAP のフェッチサイズを 64 KiB から 1 MiB に引き上げ, 50 MiB の添付の往復が約 1100 回から 69 回に (ループバックで 10 MiB/s, ダウンロード中のメモリは引き続き約 2 MiB); クライアント側検索は `limit` 件ヒットした時点で停止し, 候補全件を走査しない. 待機ではプラグインが約 32 MiB PSS, 1 時間あたり CPU 3.5 s と通信 40 KB で, 長時間の監視にはホストのフォアグラウンドサービスが必要 (なければ Android 9 は 31 分でホストとプラグインをキャッシュ空プロセスとして終了)
- `改善` リリースサイズ (メールロードマップ P6): R8 ルールは `jakarta.mail`, `jakarta.activation`, Angus Mail の名前空間全体を名前付きで保持せず, ライブラリが名前で読み込む 20 クラス (IMAP / POP3 / SMTP のストアとトランスポート, その Provider, ストリームプロバイダー, activation レジストリ, 5 つのデータコンテンツハンドラー) のみ保持し, 各ルールの根拠となるリソースを `app/proguard-rules.pro` と `docs/dev/p6-size.md` に記録. universal リリース APK は 2,254,035 バイトから 2,153,957 バイトへ (ダウンロードサイズ 1,507,931 から 1,407,295), DEX メソッド 2,319 個とライブラリクラス 162 個が削減 (`MailHandler` ログ部品, GraalVM 機能, 画像ハンドラー, SASL クライアント); 計装テストスイートは実機で縮小後ビルドに対して合格 (27 件)
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
