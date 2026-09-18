******

### リリース履歴

******

# v1.0.0

###### 2026/09/18

* `ヒント` P0 開発プレビュー: リポジトリの骨組み, ローカルサーバーテスト付きのメールコア, AutoJs6 プラグインセンター向けのプラグイン識別情報. Binder コントラクト, スクリプト API, 設定ページは ROADMAP.md のフェーズに沿って進めます.
* `機能` プラグイン識別子 `angus-mail` (エンジン `mail`), INFO サービス, Wake Activity, および `org.autojs.plugin.MAIL` サービス; その `IMailPlugin` Binder はプラグイン情報, 機能, プロバイダーと保存済みアカウントの一覧, セッションエンベロープに応答 (各操作は P2 で実装)
* `機能` Eclipse Angus Mail 上のメールコア: SSL または STARTTLS を使う IMAP / POP3 / SMTP のセッション設定, パスワードと XOAUTH2 認証, SMTP 送信と IMAP 受信トレイ一覧をローカルの GreenMail サーバーで検証
* `機能` メールコアのアカウント層 (ロードマップ P2.1): Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, Aliyun のプロバイダープリセット, プロトコルごとのタイムアウト, `tls.trustAll`, IMAP `ID` コマンド, マスク済みの `debug` トレースを備えたアカウントオプション; セッションは遅延接続し, アイドル接続を切断し, 切断後に再接続; `session.test` は Binder 経由でエンドポイントごとの機能と往復時間を返す
* `機能` 送信 (ロードマップ P2.2): `mail.send` は to / cc / bcc / replyTo, テキストと HTML 本文 (`multipart/alternative`), ホストから渡されたディスクリプタから読む添付ファイルとインライン画像 (`multipart/mixed` / `multipart/related`), カスタムヘッダー, 優先度, `inReplyTo` / `references`, 日付に対応; 宛先, 添付, ヘッダーの上限とヘッダーインジェクションの拒否; `saveToSent` はプロバイダーが自動保存しない場合にのみ IMAP でコピーを追加; `messages.append` は下書きを保存して UID を返す; 実機で QQ メールと Gmail に対して検証済み
* `機能` 10 言語の README, プラグインセンターの説明, 更新履歴
* `依存関係` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) と Angus Activation 2.0.3, Jakarta Activation API 2.1.4
* `依存関係` JVM メールコアテスト用に GreenMail 2.1.13 を追加 (テストスコープのみ)
* `依存関係` 共有プラグインコントラクトとして `common-plugin-api.aar` (AutoJs6 モジュール `plugin-api/common-plugin-api`, ホストビルド 6.8.0 / 5282, MPL 2.0) を追加し, `locks/host-api-aars.lock` でハッシュを固定
* `依存関係` メール Binder コントラクトとして `mail-api.aar` (AutoJs6 モジュール `plugin-api/mail-api`, ホストビルド 6.8.0 / 5282, MPL 2.0; 6 つの AIDL インターフェース, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`) を追加し, `locks/host-api-aars.lock` でハッシュを固定
