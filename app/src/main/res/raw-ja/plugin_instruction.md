Angus Mail は AutoJs6 スクリプトにグローバルオブジェクト `mail` を提供し, メールの送信, メールボックスの一覧と検索, 本文の読み取り, 添付ファイルのダウンロード, フラグとフォルダーの管理, フォルダーの新着監視を可能にします. Jakarta Mail の参照実装である [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5 を基盤とし, TLS 上で IMAP, POP3, SMTP を扱います.

バージョン 1.1.0 はバックグラウンド監視 (ロードマップ P8) を加えます: 監視ページ, フォアグラウンドサービス, AutoJs6 の "メール到着時" タスク. フェーズ P0 から P7 の全項目は 1.0.0 と 1.0.1 で出荷済みで, 証拠は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) にあります. AutoJs6 6.8.0 (ビルド 5282) 以降が必要で, "メール到着時" タスクにはメール契約バージョン 2 を持つホストビルドが必要です. スクリプト API の完全なリファレンスは [AutoJs6 ドキュメント](https://docs.autojs6.com/#/mail) にあります.

### 使い方

1. AutoJs6 ビルド 5282 (6.8.0) 以降がインストールされたデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `Angus Mail` が認識されていることを確認して有効にします.
3. アカウントを準備します: メールプロバイダーの Web 設定で IMAP または POP3 と SMTP を有効にし, 認証コード (QQ, 163, 126, Sina), アプリパスワード (Gmail, iCloud, Yahoo), または OAuth 2.0 アクセストークン (Outlook.com) を取得します; ログインパスワードそのものは通常受け付けられません.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページ (プラグインのランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) にアカウントを保存してエイリアスで接続します.
5. スクリプトを常駐させずに新着メールで実行するには: プラグインの監視ページ (設定 > 監視: アカウントのエイリアス, フォルダー, モード, フィルター) で監視を追加し, 求められたら通知を許可し, AutoJs6 でタスクを作成 (スクリプトを長押し > 定時タスク > ブロードキャストで実行 > メール到着時) して監視を選びます. このタスクにはメール契約バージョン 2 を持つ AutoJs6 ビルドが必要です.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) を参照してください.
