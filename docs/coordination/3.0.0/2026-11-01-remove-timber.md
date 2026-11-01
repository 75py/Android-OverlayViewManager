# 2026-11-01 Timber連携の削除

状態: **PR68の対向承認・CI成功を確認し、`8794018` へ統合済み**。`work/3.0.0` も同期済み。文書追記後を含む最新release headのCI・Claude確認は [PR66](https://github.com/75py/Android-OverlayViewManager/pull/66) のchecksとSHA付きコメントで追跡する。

## 経緯

2026-11-01、ユーザーが「Timber対応はもう削除でいいかな、使わないし」と判断を示した。3.0.0は破壊的変更のリリースで、公開前に外せば互換性の負担が残らないため、Claudeはリリース前の削除を提案し、task branchで実施した。

同日、ユーザーは「codexと連携して進めて」と指示した。PRは[PR68](https://github.com/75py/Android-OverlayViewManager/pull/68)。

## 分担

- Claude: Opus 5.5 / high（`AGENTS.md` のFable 5.1 / highとは異なる実態を記録する。Codexと確認し、モデル切替と子エージェント起動は行わない）。修正、PR作成、本記録を担当。
- Codex: 差分レビュー、隔離worktreeでの検証、push、JitPack座標の調査、`release/3.0.0` へのmerge操作を担当。Claude環境の `gh` トークンに `workflow` 権限がなくSSHも使えないため、`ci.yml` を含む `320f671` はCodexがpushした。
- 共有計画: Codexが編集していないため、本PRに限りClaudeへ更新を移管した。過去のcheckpointは保持する。
- 統合後: 統合SHAはPR66のコメントへ記録する。`work/3.0.0` は承認済み `release/3.0.0` へのfast-forwardで同期する。
- 対象外: mainへのmerge、タグ、公開。ユーザーの判断を待つ。

連絡のrequest ID:

| 内容 | request ID |
| --- | --- |
| 分担 | `1e42eea9-1bc7-41c7-b0ca-04e97f35a22b` |
| HTMLの指摘 | `979504a8-701b-4b8d-bc0a-b7a706d8ad1b` |
| JitPack座標 | `b9d136be-0144-4467-8933-c5d552af6987` |
| 検証速報 | `75f4b860-9f29-41e0-a9b2-6fadf202f2a5` |

## Codexレビュー（`320f671`）

SAMPLE番号の繰り上げ、`@RestrictTo` の維持、自前のTimber treeへの移行案内は妥当と判断された。HTML assetに `Log.w(TAG, ...)` を入れたのに `TAG` の宣言がないという指摘を受け、Sample1、Sample2、SampleOverrideScreenBrightnessの表示コードへ宣言を補った。

Codexは `a908cb4` の7ファイルの差分を確認し、指摘の解消と `diff --check` の成功を確認した。

READMEの未公開表記とCHANGELOGの `Unreleased` は、実際に公開前なので今は維持し、日付は公開決定時に確定する。

## 変更内容

- `opt-timber` モジュール（`DebugOverlayTree`、そのテスト、公開artifact `overlayviewmanager-opt-timber`）を削除。
- `settings.gradle`、`jitpack.yml`、CI、`sample/build.gradle` から参照を削除。
- `opt-timber` だけが使っていた `WeakReferenceCache` とそのテストを削除。`OverlayWindowManager` のKDocからopt-timberへの言及を削除。`@RestrictTo` のscope変更はしない。
- sampleのTimberデモ（Activity、layout、asset、ボタン、文字列）を削除し、`SAMPLE4` を `SAMPLE3` に繰り上げ。ログは `android.util.Log` に置換し、表示用assetのコードも揃えた。
- README、CHANGELOG、移行ガイド、AGENTS.md、設計書の注記を更新。過去の記録は書き換えない。

## 検証

- Claude側: Gradleと端末テストは未実行。サンドボックスからGradle daemonへ接続できない（`Could not connect to the Gradle daemon`）。sampleの `androidTest` が削除したActivity、ボタン、文字列を参照していないことはgrepで確認した。

Codex側の結果（実行はすべてCodex。`320f671` と `a908cb4` の差はHTML assetと文書のみ）:

| 検証 | 対象 | 結果 |
| --- | --- | --- |
| Debug/Releaseビルド | `320f671` | 成功 |
| unit tests | `320f671` | 134件成功（core 118、lint 12、sample 4）、skip/error/failure 0。旧183件との差49件はTimber 40件と `WeakReferenceCacheTest` 9件で一致 |
| lint | `320f671` | core エラー0/警告9、sample エラー0/警告55 |
| publish | `320f671` | 成功。AAR、POM、sources、consumer rules、lint.jarを確認。Timberと `WeakReferenceCache` の残留なし |
| core単体のJava/Kotlin consumer | `320f671` | Release、R8、lint成功（45 tasks）。意図した `ALLOW_OUTSIDE_BOUNDS` 警告1件とfixture警告3件、エラー0 |
| API36 sample全suite | `320f671` | 31件成功（freeform2件を含む）、failure/error/skip 0、4分2秒 |
| 起動画面とHTML表示 | `a908cb4` で再構築したAPK | メニューからTimberが消えSAMPLE3になったこと、Sample1とSample3の `TAG` 宣言を実画面で確認 |
| CI | `a908cb4` | run `41286372506` 成功 |

実行コマンド（review worktree）:

```sh
export ANDROID_HOME=/Users/ai-seb/Library/Android/sdk
./gradlew :core:assembleDebug :sample:assembleDebug :core:testDebugUnitTest :lint:test :sample:testDebugUnitTest :core:lintDebug :sample:lintDebug :sample:assembleRelease :sample:assembleDebugAndroidTest :core:publishToMavenLocal -Dmaven.repo.local=/private/tmp/ovm-remove-timber-maven-20261101
./gradlew -p /private/tmp/ovm-remove-timber-consumer-20261101 :app:assembleRelease :app:lintRelease --refresh-dependencies
ANDROID_SERIAL=emulator-5580 ./gradlew :sample:connectedDebugAndroidTest
./gradlew :sample:assembleDebug
```

最後の行は `a908cb4` のUI確認用の再構築。

成果物のSHA256（`320f671`）:

- sample APK: `76077cf20b2f89876b70bcfc3ef41937470afaac4982beb645b0ebb8729692d5`
- test APK: `e0e93fb1aee652533953d4bc9835df7daf4d6c1bc7230a0ac9491a6675fa3576`
- core AAR: `91f0822266fdc43a62f3e306fbbee4fe4d275bf98e760c2293ec2bd33e48a529`

画面の証拠:

- [メニュー](evidence/2026-11-01-remove-timber-menu.png)
- [Sample1](evidence/2026-11-01-remove-timber-sample1.png)
- [Sample3](evidence/2026-11-01-remove-timber-sample3.png)

端末matrixの判断: Codexは、coreの挙動変更が未使用helperの削除だけであるため、API23/26/35は2026-10-31の検証結果を維持し、sampleの実行をAPI36で再確認すると判断した。Claudeも同意した。API23/26/35は今回再実行していない。

専用のAPI36環境のfreeform設定は元のnullへ戻した。`emulator-5554` は操作していない。

観測の限界: Sample1の画像はシンタックスハイライトなしで写っている。起動直後のcaptureで、`run_prettify.js` はDOMContentLoadedと `setTimeout` で非同期に装飾するため、画像だけではハイライトの欠落を確定できない。表示文字と `TAG` 宣言は確認済み。描画タイミングと既存の装飾は今回の削除の受入条件ではなく、追加修正もrelease blockerの扱いもしない。

## JitPack座標

公開artifactが1つになる。旧形式 `com.github.75py.Android-OverlayViewManager:overlayviewmanager` が公開artifact1つのビルドでも使えるかは、文書から確認できなかった。

Codexが[JitPack公式の複数モジュールの説明](https://docs.jitpack.io/building/#multi-module-projects)を確認し、全モジュールを指定する `com.github.User:Repo:Tag` は公開モジュールが複数でも1つでも使える公式形式と判断した。この曖昧さを避けるため、READMEの座標を `com.github.75py:Android-OverlayViewManager:<version>` へ変更し、移行ガイドにも座標の変更を記載した。

根拠の区別:

- 公式仕様: 上記の文書に基づく判断。
- 公開artifact1つの成果物: Codexがlocal Mavenとcore単体のJava/Kotlin R8 consumerで確認済み。
- JitPackサービス上の実ビルドと座標解決: 未確認。従来どおりタグ後に確認する。

## 統合結果

CodexはPR68 head `d773e7e83890aeaa4eae9e95750c4fe2b8c02391` の全差分・記録を確認し、[対向承認](https://github.com/75py/Android-OverlayViewManager/pull/68#issuecomment-6555266404)を記録した。[CI41286948503成功](https://github.com/75py/Android-OverlayViewManager/actions/runs/41286948503)、base、同一head、CLEANをマージ直前に再確認し、`--match-head-commit` を指定した通常mergeで `8794018bcc6d67d338982bfaeaddef78db821540` へ統合した。承認headとtree差分がないことも確認した。

`release/3.0.0` と `work/3.0.0` を同SHAへ同期し、[PR66の本文と統合コメント](https://github.com/75py/Android-OverlayViewManager/pull/66#issuecomment-6555284723)を更新した。最終記録2ファイルはClaudeからCodexへ所有を戻した（request `d23b3f0d-44b7-49b5-a466-6d59c0f1b7b8` とClaudeの同意）。この追記は文書のみで、最終release headのCIとClaude確認はPR66で追跡する。

## 残る公開判断

mainへのmerge、タグ作成、正式公開は未実施。公開決定時にREADMEの未公開表記とCHANGELOGの日付を確定し、タグ後にJitPackサービス上のビルドと座標解決を確認する。
