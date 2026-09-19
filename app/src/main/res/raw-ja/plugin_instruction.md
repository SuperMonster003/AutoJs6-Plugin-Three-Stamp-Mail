Angus Mail は AutoJs6 スクリプトにグローバルオブジェクト `mail` を提供し, メールの送信, メールボックスの一覧と検索, 本文の読み取り, 添付ファイルのダウンロード, フラグとフォルダーの管理, フォルダーの新着監視を可能にします. Jakarta Mail の参照実装である [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5 を基盤とし, TLS 上で IMAP, POP3, SMTP を扱います.

バージョン 1.0.0 は開発中です: リポジトリの骨組み, ローカルサーバーテスト付きのメールコア, AutoJs6 プラグインセンター向けのプラグイン識別情報が整い, Binder コントラクト, スクリプト API, 設定ページは [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) のフェーズに沿って進めます. AutoJs6 6.8.0 (ビルド 5282) 以降が必要です.

### 使い方

1. AutoJs6 ビルド 5282 (6.8.0) 以降がインストールされた端末に, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `Angus Mail` が認識されていることを確認して有効化します.
3. アカウントを準備します: メールプロバイダーの設定で IMAP または POP3 を有効にし, 認証コードやアプリパスワード (QQ, 163, 126, Gmail, iCloud), または OAuth 2.0 アクセストークン (Outlook.com) を取得します.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページ (プラグインのランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) にアカウントを保存してエイリアスで接続します.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) を参照してください.
