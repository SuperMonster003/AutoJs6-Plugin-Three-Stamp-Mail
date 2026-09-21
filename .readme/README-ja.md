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

バージョン 1.2.0 は 1.1.0 のバックグラウンド監視 (ロードマップ P8) に加えて Google と Microsoft アカウントのブラウザーログイン (ロードマップ P9) を追加します. フェーズ P0 から P8 の全項目は 1.0.0 から 1.1.0 で出荷済みで, 証拠は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) にあります. AutoJs6 6.8.0 (ビルド 5282) 以降が必要です. "メール到着時" タスクにはメール契約バージョン 2 を持つホストビルドが必要です. スクリプト API の完全なリファレンスは [AutoJs6 ドキュメント](https://docs.autojs6.com/#/mail) にあります.

******

### 機能

******

プラグインは以下の機能を提供します:

- 送信: プレーンテキストまたは HTML, 複数の宛先, 添付ファイルとインライン画像, カスタムヘッダーと優先度に対応し, プロバイダーが送信済みコピーを保存しない場合はプラグインがサーバーに保存します.
- 受信: フォルダーをページ単位で一覧し, サーバー側で検索し (非 ASCII 検索を拒否するプロバイダーではクライアント側フィルターにフォールバック), テキストと HTML の本文を読み取り, 添付ファイルをスクリプトの作業ディレクトリへ直接ダウンロードします.
- 整理: 既読やフラグの付与, 移動, コピー, 削除, 完全削除, フォルダーの作成, 名前変更, 削除に対応します. POP3 アカウントは読み取り専用のサブセットになります.
- 監視: スクリプトの実行中に新着メールのイベントを受け取り, サーバーが実際にプッシュする場合は IMAP IDLE, そうでない場合はポーリング (既定 60 秒, 調整可能) を使います: QQ と Sina は IDLE を受け付けても通知せず, 163 と 126 には IDLE がなく, POP3 アカウントは常にポーリングです; ネットワーク断やプラグインプロセスの再起動後も監視は自動的に復帰します.
- バックグラウンド監視: 設定の監視ページは, スクリプトが動いていない間もフォアグラウンドサービスで保存済みアカウントへの IMAP IDLE またはポーリング接続を保ち, 新着メールを記録し, 選んだ監視について AutoJs6 の "メール到着時" タスクを起動します (送信者と件名で絞り込み可). メールは `engines.myEngine().execArgv.mail` でスクリプトに渡されます.
- プロバイダー: Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun のプリセットがホスト, ポート, 暗号化を補完し, 他のサーバー向けに任意の項目を上書きできます.
- 認証: パスワードとプロバイダーの認証コード, またはスクリプトが更新コールバックとともに渡す XOAUTH2 アクセストークンに対応します.
- ブラウザーログイン: Gmail, Outlook.com, Microsoft 365 のアカウントは, プラグイン設定からシステムブラウザーで Google または Microsoft アカウントにログインして追加できます (PKCE 付き OAuth 2.0 認可コードフロー). プラグインはリフレッシュトークンを端末内に暗号化して保存し, セッションのたびにアクセストークンを更新し, アカウントページにログイン状態と "再ログイン" / "ログインを取り消す" を表示します. スクリプトは引き続きエイリアスで接続し, トークンに触れることはありません.

******

### 使い方

******

1. AutoJs6 ビルド 5282 (6.8.0) 以降がインストールされたデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `Angus Mail` が認識されていることを確認して有効にします.
3. アカウントを準備します: プロバイダーのウェブ設定で IMAP または POP3 と SMTP を有効にし, 認証コード (QQ, 163, 126, Sina) またはアプリパスワード (Gmail, iCloud, Yahoo) を取得します. ログインパスワードそのものは通常受け付けられません. Gmail, Outlook.com, Microsoft 365 のアカウントは, 代わりにプラグイン設定からブラウザーで Google または Microsoft アカウントにログインするか (認証方式で "Google / Microsoft アカウントでログイン (ブラウザー)" を選択), 別途取得した OAuth 2.0 アクセストークンを渡すこともできます.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページ (プラグインのランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) にアカウントを保存してエイリアスで接続します.
5. スクリプトを常駐させずに新着メールで実行するには: プラグインの監視ページ (設定 > 監視: アカウントのエイリアス, フォルダー, モード, フィルター) で監視を追加し, 求められたら通知を許可し, AutoJs6 でタスクを作成 (スクリプトを長押し > 定時タスク > ブロードキャストで実行 > メール到着時) して監視を選びます. このタスクにはメール契約バージョン 2 を持つ AutoJs6 ビルドが必要です.

******

### プロバイダーの準備

******

どのプロバイダーもまず Web 設定で IMAP (または POP3) と SMTP を有効にし, ログインパスワードの代わりに認証コード, アプリパスワード, またはアクセストークンを使います; 各プリセット (`provider` の値) の要点:

- QQ メール (`qq`): Web 版のアカウント設定で IMAP/SMTP サービスを有効にして認証コードを生成し, それを `password` として使います.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net は `163` プリセットにホストを上書きして使います): Web 設定の POP3/SMTP/IMAP ページでサービスを有効にして認証コードを生成します; POP3 は別に有効化が必要で, そうしないと IMAP と SMTP が受け付けるコードを POP3 が拒否します. サーバーはすべての IMAP 接続に先に `ID` コマンドを送るよう求めます (さもないと `Unsafe Login` と応答); プラグインが自動的に行います.
- Sina メール (`sina`): Web 版のクライアント設定で IMAP/SMTP サービスを有効にし, 認証コードを使います. サーバーは送信済みメールのコピーを保存せず (プラグインが送信済みフォルダーに追加), IMAP でのフォルダー作成を拒否し, テキスト検索はプラグインがクライアント側で行います.
- 最も簡単なのはプラグイン設定ページのブラウザーログイン ("Google アカウントでログイン (ブラウザー)") で, アプリパスワードが不要でトークンは自動更新されます. それ以外の方法: Gmail (`gmail`): 2 段階認証を有効にしたうえで Google アカウントでアプリパスワードを生成して `password` に使うか, `https://mail.google.com/` スコープ付きの OAuth 2.0 アクセストークン (`accessToken` と `tokenProvider`) を渡します; フォルダーは `[Gmail]` 名前空間にあり, 新着メールは IDLE でプッシュされます. プロジェクトはトークンで実アカウントを検証しました.
- 最も簡単なのはプラグイン設定ページのブラウザーログイン ("Microsoft アカウントでログイン (ブラウザー)") で, トークンの取得と更新はプラグインが行います. それ以外の方法: Outlook.com / Hotmail (`outlook`) と Microsoft 365 (`office365`): Microsoft は個人アカウントの基本認証を無効化したため, アプリパスワードは IMAP, POP3, SMTP のいずれでも拒否され, `outlook` プリセットは OAuth 2.0 アクセストークン (`accessToken` と `tokenProvider`) のみを受け付けます; 職場または学校のアカウント (`office365`) はパスワードでもトークンでも使えますが, テナントポリシーが IMAP, POP3, SMTP AUTH を無効にしている場合があります. トークンには `https://outlook.office.com/` の委任スコープ `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All`, `SMTP.Send` が必要です (プロジェクトはそのトークンで個人アカウントを検証済み: IMAP, POP3, SMTP と IDLE プッシュ); 比較的新しい個人メールボックスでは Microsoft が SMTP AUTH を無効にしており (`535 5.7.139`), ユーザー設定に切り替えはありません.
- iCloud (`icloud`): Apple アカウントで App 用パスワードを生成します; POP3 サービスはありません.
- Yahoo (`yahoo`) と Aliyun 個人メール (`aliyun`): アプリパスワードまたは認証コードを生成します; この 2 つのプリセットは公開ドキュメントに基づいており, プロジェクトにテストアカウントがないため未検証です.

その他のサーバーでは `provider` を省略し, `imap` (または `pop3`) と `smtp` の `host`, `port`, `tls` (`ssl`, `starttls`, `none`) を指定します; プリセットの任意のフィールドも上書きできます. すべてのオプションは [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType) を, 組み込みプリセットは `mail.providers.list()` を参照してください.

******

### クイックスタート

******

エイリアスで接続し, レポートを送信し, 添付ファイル付きの未読メールを読み, 確認コードを監視し, 非同期に検索するスクリプト:

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

### 保存アカウントとエイリアス

******

エイリアスはプラグイン内に保存されたアカウントです. プラグインの設定ページ (ランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) でアカウントを入力し, 接続をテストして保存すると, パスワードやトークンは Android Keystore の鍵で暗号化されてプラグインのプライベートディレクトリに保存され, バックアップから除外されます; スクリプトはその後 `mail.connect('エイリアス')` で接続し, 資格情報はスクリプトにも Binder にも渡りません.

設定ページでは 1 つのアカウントを既定にできます; `mail.accounts.list()` が返す項目にはそのアカウントに `default: true` が付き, `mail.accounts.has(alias)` はエイリアスの有無を確認します. 複数のアカウントを同時に使うときはエイリアスごとにクライアントを作成します; `mail.setDefault(client)` の後は `mail.fetch(...)` などの転送メソッドが既定のクライアントに作用します.

******

### 互換性

******

以下の結論はロードマップ P6 の実アカウントマトリクス (2026-09-19 と 09-20: 各プロバイダーが中国語の件名, 本文, 表示名, ファイル名を持つメールを自分宛てに送り, 各操作を検証) と, TLS, 文字セット, ライフサイクル, 性能の各マトリクスに基づきます:

- QQ メール: 送信, 一覧, 本文, 添付, フラグ, 移動 (`MOVE`), POP3 はすべて合格; 中国語検索にサーバーはエラーではなくヒット 0 件で応答するため `fallback: 'always'` が必要; 送信メールの Message-ID はサーバーに書き換えられる; フォルダー作成は拒否される; 監視はポーリング (IDLE はプッシュしない) で, 新着メールは配信の 15-40 秒後にサーバー上で見えるようになります.
- 163 / 126 / yeah.net: すべて合格; サーバーが送信済みコピーを保存; IDLE がないため監視はポーリング; 163 は最近のメールに対するテキスト検索にヒット 0 件で応答 (126 と yeah.net は正常); 送信者表示名のスペースはアンダースコアで返る; yeah.net の POP3 は Web 設定で別途有効化が必要.
- Sina メール: 合格; サーバーは ALL, SINCE, フラグ条件のみ受け付けるため, テキスト検索は自動的にクライアント側フィルタリングにフォールバック; IDLE なし; フォルダー作成は拒否; 送信済みコピーはプラグインが追加.
- Gmail: OAuth 2.0 トークンで全行合格, 新着メールは IDLE でプッシュ (約 30 秒, Gmail 自身の通知間隔); 中国語を含むサーバー検索はすべてヒット (プラグインは `UTF8=ACCEPT` を有効にしない); カスタム IMAP キーワードが保存される (6 社中唯一); POP3 ビューにはアカウントが自分宛てに送ったメールは含まれない.
- Outlook.com / Hotmail: OAuth 2.0 アクセストークンで 1 つの個人アカウントの全行が通過 (2026-09-21): フォルダーの役割は慣用名から, サーバーが送信済みコピーを保存し Message-ID を書き換え, `MOVE` とフォルダー作成は正常, POP3 は 2 行形式の `AUTH XOAUTH2` でログインし, IDLE は約 10 秒以内にプッシュ (7 社中最速); 中国語の件名のサーバー検索はヒットするが数分かかることがある; アプリパスワードは IMAP, POP3, SMTP のいずれも引き続き拒否され (`AUTH_MECHANISM_UNSUPPORTED`), 比較的新しい個人メールボックスでは Microsoft が SMTP AUTH を無効にしている (`535 5.7.139`); iCloud, Yahoo, Aliyun はテストアカウントがなく, プリセットは未検証.
- TLS と文字セット: 暗黙 SSL, STARTTLS, 平文, 自己署名証明書 (`tls.trustAll` あり / なし), ホスト名不一致, ポートモード不一致を IMAP, POP3, SMTP で個別にテストし, 失敗は `TLS_FAILED` や `TIMEOUT` など判別可能なコードに対応; GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR, UTF-8 の件名, 表示名, 本文, ファイル名を宣言あり, 宣言なし, 誤宣言の 3 ケースで検証.
- デバイスとライフサイクル: Android 7.0 (API 24) エミュレーターと Sony (Android 9), Redmi (Android 13) の実機; スクリプトの正常終了, `exit()`, `engines.stopAll()`, ホストまたはプラグインの強制停止, その場でのアップグレード, プラグインの無効化とアンインストールの 8 通りの終了方法で接続, バインド, スレッドが解放される; 画面消灯 (Doze) で監視は接続を失い, デバイスの復帰後に回復; 途切れない監視が必要なら設定ページからバッテリー最適化の除外を申請できます.
- 性能ベースライン: ローカルの 10000 通の受信トレイと 50 MiB の添付ファイルに対する一覧, 検索, ダウンロード, 送信, 1 時間の IDLE 待機を JVM, Redmi, Sony で測定し, `docs/dev/p6-performance-baseline.md` に記録; メールドキュメントには上限があり (アドレス, ヘッダー, MIME ツリー, インライン本文), 敵対的な入力でセッションが破綻することはありません.

******

### よくある質問

******

- **`AUTH_FAILED` はどう調べればよいですか?** ログインパスワードではなく認証コードまたはアプリパスワードを使っているか, Web 設定で該当プロトコルが有効か (IMAP と POP3 は別々に有効化) を確認し, `client.test()` で受信エンドポイントと SMTP の結果とエラーコードを個別に確認します. トークンアカウントの `AUTH_FAILED` は通常トークンの期限切れで, `tokenProvider` があればプラグインが更新して 1 回再試行します. エラーの `code`, `details`, `retryable` が再試行の価値を示します.
- **163 / 126 が `Unsafe Login` と応答する?** NetEase の IMAP サーバーは `ID` コマンドを送っていない接続を拒否します; プラグインはすべての IMAP 接続でログイン直後に `ID` を送るため, 通常は発生しません. それでも発生する場合は Web 設定で IMAP サービスを再度有効にし, 認証コードを生成し直してください.
- **中国語の検索がヒットしない?** 非 ASCII 検索の扱いはサーバーごとに異なります: Sina は拒否し (プラグインが自動でクライアント側フィルタリングにフォールバック), QQ と 163 はエラーなしでヒット 0 件を返します (既定の `fallback: 'client'` は発動しません). これらのアカウントでは `fallback: 'always'` を使い, `since` や `limit` で範囲を絞ってください; クライアント側の本文フィルタリングは候補を 1 通ずつ取得するため, 大きなメールボックスでは遅くなります.
- **`mail.connect` は成功したのに最初の `fetch` で失敗する?** `connect` はプラグインセッションを開くだけでメールサーバーには接続しません; 最初のネットワークメソッドでログインします (SMTP は最初の送信時). 事前に確認するには `client.test()` を呼び出してください.
- **画面が消えると監視が止まる?** Android の Doze はバックグラウンドアプリのネットワークを凍結するため, 監視は接続を失い, 新着メールはデバイス復帰後の数分以内に報告されます (Doze 終了時にプラグインは直ちに再接続). 途切れない監視が必要なら, 設定ページのガイドボタンでプラグインのバッテリー最適化除外を申請してください; 監視はスクリプト実行中のみ有効で, スクリプト終了時に閉じられます.
- **Outlook.com / Hotmail にはどう接続しますか?** Microsoft は個人アカウントの基本認証を無効にしたため, プリセットはパスワードを受け付けません. プラグイン設定ページでアカウントを保存し, 認証方式に "Microsoft アカウントでログイン (ブラウザー)" を選びます: ブラウザーで Microsoft のログインページが開き, プラグインに戻るのは認可コードだけで, アクセストークンはプラグインが自動更新します. その後スクリプトはエイリアスで接続します. スクリプトは別途取得したアクセストークンを `accessToken` として渡し `tokenProvider` で更新することもできます. アカウントページに "再ログインが必要" と出たら更新が拒否されています (ログインの取り消しか期限切れ): アカウントのメニューから "再ログイン" を選んでください.
- **POP3 アカウントでできることは?** `INBOX` のみで, `uid` は UIDL 文字列です; 一覧, 読み取り, ダウンロード, 削除, ポーリング監視が使え, フラグ, 移動, コピー, 追加, 消去, フォルダー管理は `UNSUPPORTED_OPERATION` を返します; 検索はクライアント側で行われ, エンベロープ条件のみ使えます.

******

### 権限とセキュリティ

******

プラグインは明確な境界に従います:

- Binder のエントリポイントは署名パーミッション `org.autojs.permission.PLUGIN` で保護され, AutoJs6 だけが到達できます. プラグインは他のコンポーネントをエクスポートしません.
- INTERNET 権限は, スクリプトが指定したサーバーへの IMAP, POP3, SMTP 接続と, ブラウザーでログインしたアカウントについては Google (`oauth2.googleapis.com`) と Microsoft (`login.microsoftonline.com`) のトークンエンドポイントへの HTTPS リクエストのみに使われます: 認可コードの交換, 期限が近いアクセストークンの更新, 要求に応じた Google トークンの取り消しです. プラグインは他のリクエストを行わず, データを収集しません.
- パスワードとトークンはスクリプトからプラグインへ Binder の専用フィールドで渡され, ログ, JSON ドキュメント, エラーメッセージ, クラッシュレポートには現れず, セッションの存続期間中だけメモリに置かれます. 設定ページに保存したアカウントは Android Keystore の鍵で暗号化され, バックアップから除外されます. ブラウザーログインのトークンも同じ方法で保存されます: リフレッシュトークンは端末から出ず, メールセッションに渡るのはアクセストークンだけで, AutoJs6 もスクリプトも読み取れません. "ログインを取り消す" は直ちにそれらを破棄します.
- 接続は既定で TLS を使います (プロバイダーの要件に応じて SSL または STARTTLS). 平文接続と自己署名証明書はアカウントごとに明示的に指定する必要があります.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 権限は設定画面の案内ボタンのためだけにあります: ボタンはシステムがバックグラウンドでプラグインを一時停止しうるかを表示し, 求めに応じてシステムのダイアログを開きます; プラグインが自ら要求することはなく, この除外に依存する機能もありません. P5 の監視マトリクスはこの除外の意味を実測しました: 画面を消してしばらくすると (Doze) Android はバックグラウンドアプリのネットワークを凍結し, 監視は切断され, 再接続はタイムアウトし, 新着メールは端末が起きてから数分後に報告されます (Android 9 で約 4 分; Doze の終了時にプラグインは即座に再接続します); 除外すると監視は接続を保ちます.
- 4 つの権限は 1.1.0 のバックグラウンド監視のためだけにあります. FOREGROUND_SERVICE と FOREGROUND_SERVICE_SPECIAL_USE は監視サービス (種別 `specialUse`, サブタイプ `mail_background_watch`: メール監視は上限のあるデータ同期ではなく, サーバーのプッシュを待つ開いた接続だからです) を低優先度の通知 1 件とともに動かします. POST_NOTIFICATIONS は Android 13 以降でその通知を表示できるよう, 監視ページで監視を有効にするときだけ要求されます. RECEIVE_BOOT_COMPLETED は同ページの起動時自動開始スイッチを支え, 既定はオフで, オンにしたときだけレシーバーを有効にします. サービスは監視ページかホストの購読からのみ始まり, 保存済みアカウントにのみ接続し, 秘密はプラグインプロセス内に留まります. AutoJs6 への起動ブロードキャストはメールのエンベロープだけを運び (本文は決して含まず), PLUGIN 署名権限の背後のレシーバーにしか届きません.

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

#### v1.2.0

_2026/09/22_

- `ヒント` ブラウザーログインは, そのプロバイダーの OAuth 2.0 クライアント id を含むビルドだけが提供します (メンテナーの登録情報はビルド時に Git 無視の `oauth-clients.properties` から読まれます). それを持たないビルドはトークンとアプリパスワードの経路を維持し, 認証方式のダイアログでその旨を示します. Google クライアントは Google Cloud プロジェクトの機密 scope 審査を通過するまで任意のアカウントでは動作せず, それまではプロジェクトのテストユーザーに限られます.
- `機能` Google と Microsoft アカウントのブラウザーログイン (メールロードマップ P9): アカウントエディターは Gmail プリセットに "Google アカウントでログイン (ブラウザー)", Outlook.com と Microsoft 365 プリセットに "Microsoft アカウントでログイン (ブラウザー)" を提供します. ログインは Custom Tab (フォールバックは任意のブラウザー) でプロバイダーのページを開き, PKCE (`S256`) とランダムな `state` を伴う OAuth 2.0 認可コード要求を送ります. リダイレクト (`<applicationId>://oauth2/microsoft`, または Google の反転クライアント id スキーム) は `OAuthRedirectActivity` に着地し, 待機中のログイン画面へ渡されます. 画面は `state` が一致しないリダイレクトを拒否し, HTTPS でトークンエンドポイントにてコードを交換し (`HttpsFormPoster`, プラグイン唯一の HTTP クライアント), id トークンからアドレスを事前入力します
- `機能` トークンの保存と更新: ブラウザーログインのトークンはアカウントストアの新種別 `OAUTH2` の暗号化レコード 1 件です (Binder フィールドにも `mail.accounts.list()` にも決して現れません). アカウント文書は `oauth` オブジェクト (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`) を持ち, `mail.accounts.list()` がそれを報告します. そのエイリアスのすべてのセッション (スクリプト, 接続テスト, バックグラウンド監視) は `AccountSecrets` からアクセストークンを受け取り, 残り 5 分未満ならリフレッシュトークンで更新します (アカウントごとに直列化). 更新が拒否されると (`invalid_grant`) レコードに `needsReauth` が付き, セッションは `AUTH_FAILED` ("sign in again") で失敗し, アカウントページはそのアカウントの横に "再ログインが必要" と表示します
- `機能` アカウントページの操作 "再ログイン" (新しいブラウザーログインを同じレコードに保存) と "ログインを取り消す" (レコードのトークンを直ちに取り消し済みマーカーに置き換え, Google にリフレッシュトークンの取り消しを要求し, 再ログインまでアカウントは動作しません). `gmail`, `outlook`, `office365` プリセットの `authHint` はブラウザーログインを最初に挙げます (`providers.json` バージョン 4). 11 言語に 35 の新しい文字列. JVM テスト: PKCE, 認可要求とリダイレクトの解析, スクリプト化した通信に対するトークンクライアント, トークン文書, プロバイダー表, ビルドのクライアントとリダイレクト URI, `AccountSecrets` (更新, 拒否のマーク, 取り消し済みレコード, 消去), アカウントオプション / フォーム / アカウント文書の `oauth` オブジェクト

#### v1.1.0

_2026/09/21_

- `ヒント` 1.1.0 でバックグラウンド監視が加わりました (メールロードマップ P8): 設定の監視ページは, スクリプトが動いていない間もフォアグラウンドサービスで保存済みアカウントの監視を保ち, AutoJs6 の "メール到着時" タスクを起動します. このタスクと監視の選択にはメール契約バージョン 2 を持つホストビルド (AutoJs6 6.8.0 のビルド 5282 より後) が必要です. 古いホストではページに AutoJs6 を起動できない旨が表示され, 新着メールは監視の記録リストにのみ残ります. この機能のために 4 つの権限が追加され, それぞれの理由は README のセキュリティ節にあります: FOREGROUND_SERVICE と FOREGROUND_SERVICE_SPECIAL_USE (監視サービス), POST_NOTIFICATIONS (常駐通知, 監視を有効にするときだけ要求), RECEIVE_BOOT_COMPLETED (監視ページの起動時自動開始スイッチ, 既定はオフ).
- `機能` バックグラウンド監視 (メールロードマップ P8): 設定に監視ページが加わり, 保存済みアカウントに最大 16 個の監視 (`MAX_TRIGGERS`) を設定できます. 各監視は名前, アカウントのエイリアス, フォルダー, モード (自動, IDLE, または間隔付きのポーリング), 任意の送信者 / 件名フィルターを持ち, no-backup ディレクトリの `mail-triggers/triggers.json` に保存されます. `specialUse` フォアグラウンドサービス `MailWatchService` はスクリプトが動いていない間も有効な監視を P5 のウォッチャーで動かし (サーバーがプッシュするなら IDLE, そうでなければポーリング, バックオフ付き再接続, ネットワーク変化時は即時再接続), 低優先度の通知を 1 件表示します. 新着メールは監視の記録リスト (直近 100 件のエンベロープ要約, `MAX_TRIGGER_RECORDS`, 本文は含まない) に加わり, ページには接続状態, 最後のエラー, 記録, 再接続操作が表示されます. 起動時自動開始スイッチ (既定はオフ) は再起動後にサービスを立ち上げる `BOOT_COMPLETED` レシーバーを有効にします
- `機能` メール契約バージョン 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, 機能フラグ `backgroundWatch`): ホストは generation と任意のフィルターで設定済みの監視を購読し, 直ちに現在の状態を受け取り, その後 `onStatus` (stopped, connecting, connected, failed と理由および最後のエラー) と, 一致するメールごとの `onMail(generation, seq, event)` を受け取ります. `mail` イベントは監視 id, エイリアス, アドレス, フォルダー, メールのエンベロープ, `receivedAt` を運びます. 監視あたりの購読者は最大 4 (`MAX_TRIGGER_SUBSCRIBERS`), 無効または存在しない監視と使えないオプションは理由 `refused` の `stopped` 状態で拒否され, `update` は購読者のフィルターを置き換え, `stop` は購読のみを終えます. 生きた購読者がないときは各イベントが明示的ブロードキャスト `org.autojs.autojs6.action.MAIL_TRIGGER` として `PLUGIN` 署名権限の背後の AutoJs6 に送られ, スクリプトが動いていなくてもホストの "メール到着時" タスクを起動します (API 37 AVD での `MailTriggerBinderTest`; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `機能` メールコアに, ページ, サービス, Binder が共有するトリガー文書と規則 (`TriggerConfig`, 送信者と件名の部分文字列を大文字小文字を区別せず照合する `TriggerFilter`, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) と上限 `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS`, `MIN_TRIGGER_INTERVAL_MS` (3 秒, ホストのタスクごとのスロットル) が加わり, ウォッチャーは `onConnected` を報告してバックグラウンド監視がフォルダーを開いた時点で `connected` を表示できるようになりました

#### v1.0.1

_2026/09/21_

- `修正` OAuth 2.0 トークンによる Outlook.com の POP3 (メールロードマップ P6 プロバイダーマトリクス, 2026-09-21): サーバーは Angus Mail が既定で送る 1 行形式の `AUTH XOAUTH2 <base64>` に `-ERR Protocol error. Connection is closed.` と答えて接続を切るため, `outlook` プリセットの POP3 アカウントは `AUTH_FAILED` で失敗していました; プロバイダープリセットに `pop3Xoauth2TwoLine` (カタログバージョン 3, `outlook` と `office365` で true) を追加し, メールコアはこれらのプリセットとプリセットなしで入力された Microsoft の POP3 ホストに対して, コマンドだけを送りサーバーの `+` 継続の後に base64 応答を送るようになりました (`Pop3OAuthScriptedTest` 5 件; 実アカウントで JVM と API 33 実機の両方で検証済み)
- `改善` プロバイダーマトリクスの Outlook.com 列 (メールロードマップ P6) と P5 の実機行を, メンテナーの Entra パブリッククライアント登録によるトークンで 1 つの個人アカウントについて実施: セッションテスト, フォルダー (役割は慣用名から, SPECIAL-USE なし), 送信 (`sentCopy = server`, Message-ID はサーバーが書き換え), 一覧 (約 7 秒で可視), サーバー検索 (すべてのキーがヒット; 中国語の件名も正しくヒットするが 2300 通の受信トレイではサーバーが数分かかる), 本文, 添付, フラグ (カスタムキーワードは保存されない), フォルダー作成, `MOVE`, 監視 (IDLE は送信後約 10 秒以内にプッシュ, 7 社中最速), POP3 (UIDL 5 文字, 送信直後のメールも表示) とクリーンアップ (送信済みコピーは UID で操作可能) がすべて通過; Redmi (API 33) の baseline / プラグイン強制終了 / Wi-Fi オフの 3 シナリオは 9 から 14 秒で到達し, イベント順序は Gmail と同じ; プリセットの notes, README, 証拠ファイルに差異を記録 (Microsoft が新しめの個人メールボックスに返す `535 5.7.139 SmtpClientAuthentication is disabled for the Mailbox` を含む)

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
- メールモジュールのドキュメント: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
