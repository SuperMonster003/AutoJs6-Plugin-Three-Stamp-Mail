# AutoJs6 3-Stamp Mail

3-Stamp Mail は AutoJs6 スクリプトにグローバルオブジェクト `mail` を提供し, メールの送信, メールボックスの一覧と検索, 本文の読み取り, 添付ファイルのダウンロード, フラグとフォルダーの管理, フォルダーの新着監視を可能にします. Jakarta Mail の参照実装である [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5 を基盤とし, TLS 上で IMAP, POP3, SMTP を扱います.

メール操作, アカウント保存, バックグラウンド監視, Google/Microsoft のブラウザログインに対応します. AutoJs6 6.8.0 build 5316 以降が必要です. [mail API ドキュメント](https://docs.autojs6.com/#/mail).

### 使い方

1. AutoJs6 ビルド 5316 (6.8.0) 以降がインストールされたデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/releases) からプラグイン APK をインストールします.
2. AutoJs6 プラグインセンターを開き, `3-Stamp Mail` が認識されていることを確認して有効にします.
3. アカウントを準備します: プロバイダーのウェブ設定で IMAP または POP3 と SMTP を有効にし, 認証コード (QQ, 163, 126, Sina) またはアプリパスワード (Gmail, iCloud, Yahoo) を取得します. ログインパスワードそのものは通常受け付けられません. Gmail, Outlook.com, Microsoft 365 のアカウントは, 代わりにプラグイン設定からブラウザーで Google または Microsoft アカウントにログインするか (認証方式で "Google / Microsoft アカウントでログイン (ブラウザー)" を選択), 別途取得した OAuth 2.0 アクセストークンを渡すこともできます.
4. スクリプトで `mail.connect(...)` を呼び出すか, プラグインの設定ページ (プラグインのランチャーアイコン, または AutoJs6 のデベロッパーオプション > メールアカウント設定) にアカウントを保存してエイリアスで接続します.
5. スクリプトを常駐させずに新着メールで実行するには: プラグインの監視ページ (設定 > 監視: アカウントのエイリアス, フォルダー, モード, フィルター) で監視を追加し, 求められたら通知を許可し, AutoJs6 でタスクを作成 (スクリプトを長押し > 定時タスク > ブロードキャストで実行 > メール到着時) して監視を選びます. このタスクにはメール契約バージョン 2 を持つ AutoJs6 ビルドが必要です.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/ROADMAP.md) を参照してください.


[Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/), Jakarta Mail/Activation, GreenMail と, 実装や開発規約を参考にした AutoJs6, OpenCC, 3-Stone AI, MCP Server, Pinyin4j の開発者に感謝します. [出典とライセンス](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/THIRD_PARTY_NOTICES.md#project-origins-and-acknowledgements). 本プロジェクトは独立しており, 各開発者の公認を意味しません. 名称と権利は各権利者に帰属します. [権利に関する申し立てと対応](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/RIGHTS_AND_TAKEDOWN.md).
