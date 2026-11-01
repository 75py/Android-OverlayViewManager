# 2026-11-01 Timber連携の削除

状態: **Claude提案、Codex対向レビュー待ち**。双方合意ではない。

## 経緯

2026-11-01、ユーザーが「Timber対応はもう削除でいいかな、使わないし」と判断を示した。3.0.0は破壊的変更のリリースで、公開前に外せば互換性の負担が残らないため、Claudeはリリース前の削除を提案し、task branchで実施した。

## 変更内容

- `opt-timber` モジュール（`DebugOverlayTree`、そのテスト、公開artifact `overlayviewmanager-opt-timber`）を削除。
- `settings.gradle`、`jitpack.yml`、CI、`sample/build.gradle` から参照を削除。
- `opt-timber` だけが使っていた `WeakReferenceCache` とそのテストを削除。`OverlayWindowManager` のKDocからopt-timberへの言及を削除。`@RestrictTo` のscope変更はしない。
- sampleのTimberデモ（Activity、layout、asset、ボタン、文字列）を削除し、`SAMPLE4` を `SAMPLE3` に繰り上げ。ログは `android.util.Log` に置換し、表示用assetのコードも揃えた。
- README、CHANGELOG、移行ガイド、AGENTS.md、設計書の注記を更新。過去の記録は書き換えない。

## 検証

- ローカル: 未実行。サンドボックスからGradle daemonへ接続できない（`Could not connect to the Gradle daemon`）。
- CI: このPRのbuild、unit test、lintで確認する。
- 端末テスト: 未実行。`core` のロジックは変更しておらず、sampleの `androidTest` は削除したActivity、ボタン、文字列を参照していないことをgrepで確認した。リリース前に再実行するかはCodexの判断に委ねる。

## 未解決: JitPack座標

公開artifactが1つになる。JitPackの文書は、複数モジュールの場合の `com.github.User.Repo:Module:Tag` と、全モジュールをまとめた `com.github.User:Repo:Tag` を説明しているが、公開artifactが1つのビルドで前者の形式が引き続き使えるかは文書から確認できなかった。READMEの座標は変更していない。JitPackはサンドボックスから接続できないため、実際のビルドで確認する必要がある。

## 判断待ち

- Codex: 現headの対向レビューと承認。
- ユーザー: JitPackでの座標確認後のPR #66のmergeとタグ付け。
