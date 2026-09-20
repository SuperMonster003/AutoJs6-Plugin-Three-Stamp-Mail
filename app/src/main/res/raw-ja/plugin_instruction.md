Angus Mail は AutoJs6 スクリプトにグローバルオブジェクト `mail` を提供し, メールの送信, メールボックスの一覧と検索, 本文の読み取り, 添付ファイルのダウンロード, フラグとフォルダーの管理, フォルダーの新着監視を可能にします. Jakarta Mail の参照実装である [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5 を基盤とし, TLS 上で IMAP, POP3, SMTP を扱います.

バージョン 1.0.0 は最初の正式リリースです: ロードマップ P0 から P6 の全項目 (メールコア, Binder コントラクト, スクリプト API, 設定ページと保存アカウント, 新着メールの監視, TLS / 文字セット / プロバイダー / ライフサイクル / 敵対的入力 / 秘密情報監査 / 性能の各マトリクス) が証拠付きで完了しており, [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) を参照してください. AutoJs6 6.8.0 (ビルド 5282) 以降が必要です; スクリプト API の完全なリファレンスは [AutoJs6 ドキュメント](https://docs.autojs6.com/#/mail) にあります.

### 使い方

1. AutoJs6 ビルド 5282 (6.8.0) 以降がインストールされたデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `Angus Mail` が認識されていることを確認して有効にします.
3. アカウントを準備します: メールプロバイダーの Web 設定で IMAP または POP3 と SMTP を有効にし, 認証コード (QQ, 163, 126, Sina), アプリパスワード (Gmail, iCloud, Yahoo), または OAuth 2.0 アクセストークン (Outlook.com) を取得します; ログインパスワードそのものは通常受け付けられません.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページ (プラグインのランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) にアカウントを保存してエイリアスで接続します.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) を参照してください.
