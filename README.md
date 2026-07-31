# 私の栄養記録

Galaxy A53 5GとGarmin Venu 2 Plus向けの、個人用・完全オフライン栄養管理Androidアプリです。アカウントやサーバーは使わず、食事・目標・活動データは端末内のRoomデータベースに保存します。

## インストール

1. GitHubの **Releases** を開き、最新の `eiyoapp-vX.Y.Z.apk` をGalaxy A53へダウンロードします。
2. APKを1回タップし、Androidのパッケージインストーラーで「インストール」を押します。
3. 初回だけ、ブラウザまたはファイルアプリに「不明なアプリのインストール」を許可するよう求められる場合があります。

APK自体がAndroid用インストーラーです。Google Playや別のダウンローダーは不要です。

## 主な機能

- 使用回数順・部分一致検索、前回量を使った3タップ食事記録
- 13栄養素、添加物、目標達成度、当日タイムライン
- 食品ライブラリの登録・編集・削除と10食品の初期データ
- 7日／14日の栄養推移と摂取・消費カロリー比較
- Health Connectから歩数、総／活動カロリー、運動、睡眠、安静時心拍を読み取り
- Health Connect非対応時の手入力とGarmin CSV取込
- Markdown、Excel向けUTF-8 BOM付きCSVの保存・共有・全文コピー
- 全データのJSONバックアップ／復元

画面・挙動の原典は[要件定義書](docs/要件定義書_Codex向け.md)と[UIモック](docs/ui/)です。

## Garmin / Health Connect設定

1. Android 14の設定から「ヘルスコネクト」を有効にします。
2. Garmin Connect → 設定 → 接続済みアプリ → Health Connectで、歩数・カロリー・心拍・睡眠の共有を許可します。
3. 本アプリのホーム → Garminカード → 設定から、読み取り権限を許可します。

本アプリはGarminと直接通信せず、Health Connectを読み取るだけです。Health Connectへ書き込みません。

## 開発と検証

```bash
./gradlew testDebugUnitTest assembleRelease
```

- minSdk 28 / targetSdk 36
- Kotlin + Jetpack Compose
- Room + Coroutines / Flow
- Health Connect 1.1.0
- WorkManagerによる日次バックフィル

Releaseビルドは署名Secretがない環境ではインストール確認用のdebug keyで署名されます。継続更新用にはGitHub Actionsへ次のSecretsを登録します。

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

同じ鍵を維持しないAPKは、既存インストールへ上書き更新できません。

## プライバシー

ネットワーク権限を宣言していません。通常の記録・閲覧・出力は機内モードでも動作します。外部へデータが出るのは、ユーザーが明示的に共有またはファイル保存したときだけです。
