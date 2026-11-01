# 2026-11-01 Timber連携の削除

状態: **Claude提案、Codex対向レビュー待ち**。双方合意ではない。

## 経緯

2026-11-01、ユーザーが「Timber対応はもう削除でいいかな、使わないし」と判断を示した。3.0.0は破壊的変更のリリースで、公開前に外せば互換性の負担が残らないため、Claudeはリリース前の削除を提案し、task branchで実施した。

同日、ユーザーは「codexと連携して進めて」と指示した。PRは[PR68](https://github.com/75py/Android-OverlayViewManager/pull/68)。

## 分担

- Claude: Opus 5.5 / high（`AGENTS.md` のFable 5.1 / highとは異なる実態を記録する。Codexと確認し、モデル切替と子エージェント起動は行わない）。修正、PR作成、本記録を担当。
- Codex: 差分レビュー、隔離worktreeでの検証、push、JitPack座標の調査、共有計画の統合を担当。Claude環境の `gh` トークンに `workflow` 権限がなくSSHも使えないため、`ci.yml` を含む `320f671` はCodexがpushした。
- 対象外: mainへのmerge、タグ、公開。ユーザーの判断を待つ。

## Codexレビュー（`320f671`）

SAMPLE番号の繰り上げ、`@RestrictTo` の維持、自前のTimber treeへの移行案内は妥当と判断された。HTML assetに `Log.w(TAG, ...)` を入れたのに `TAG` の宣言がないという指摘を受け、Sample1、Sample2、SampleOverrideScreenBrightnessの表示コードへ宣言を補った。

READMEの未公開表記とCHANGELOGの `Unreleased` は、実際に公開前なので今は維持し、日付は公開決定時に確定する。

## 変更内容

- `opt-timber` モジュール（`DebugOverlayTree`、そのテスト、公開artifact `overlayviewmanager-opt-timber`）を削除。
- `settings.gradle`、`jitpack.yml`、CI、`sample/build.gradle` から参照を削除。
- `opt-timber` だけが使っていた `WeakReferenceCache` とそのテストを削除。`OverlayWindowManager` のKDocからopt-timberへの言及を削除。`@RestrictTo` のscope変更はしない。
- sampleのTimberデモ（Activity、layout、asset、ボタン、文字列）を削除し、`SAMPLE4` を `SAMPLE3` に繰り上げ。ログは `android.util.Log` に置換し、表示用assetのコードも揃えた。
- README、CHANGELOG、移行ガイド、AGENTS.md、設計書の注記を更新。過去の記録は書き換えない。

## 検証

- Claude側: Gradleと端末テストは未実行。サンドボックスからGradle daemonへ接続できない（`Could not connect to the Gradle daemon`）。sampleの `androidTest` が削除したActivity、ボタン、文字列を参照していないことはgrepで確認した。
- Codex側: `320f671` のbuild、unit、lint、Release、publish、androidTest APKの検証を実行中。結果はここへ追記する。
- CI: PR68で確認する。必須端末matrixの再実行要否はCodexが判断する。

## JitPack座標

公開artifactが1つになる。旧形式 `com.github.75py.Android-OverlayViewManager:overlayviewmanager` が公開artifact1つのビルドでも使えるかは、文書から確認できなかった。

Codexが[JitPack公式の複数モジュールの説明](https://docs.jitpack.io/building/#multi-module-projects)を確認し、全モジュールを指定する `com.github.User:Repo:Tag` は公開モジュールが複数でも1つでも使える公式形式と判断した。この曖昧さを避けるため、READMEの座標を `com.github.75py:Android-OverlayViewManager:<version>` へ変更し、移行ガイドにも座標の変更を記載した。

根拠の区別:

- 公式仕様: 上記の文書に基づく判断。
- 公開artifact1つの成果物: Codexがlocal Mavenとcore単体のJava/Kotlin R8 consumerで確認する。
- JitPackサービス上の実ビルドと座標解決: 未確認。従来どおりタグ後に確認する。

## 判断待ち

- Codex: 現headの対向レビューと承認。
- ユーザー: JitPackでの座標確認後のPR #66のmergeとタグ付け。
