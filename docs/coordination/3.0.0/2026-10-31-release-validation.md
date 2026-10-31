# 3.0.0 最終検証・リリースPR

## 目的・担当の合意

2026-10-31 JST、ユーザーはCodexとClaude Codeへ、相談して妥当な案を選び、品質を落とさず過度に複雑にせずmain向けPR作成まで進めるよう指示した。mainへのマージ・タグ・正式配布は実行しない。配布先は既定のJitPackを維持する。

ClaudeはPR63およびplan/logの所有権をCodexへ移管することに画面上で明示合意。Codexは計画・ログ・PR63更新、Gradle/SDK/端末/consumer検証、相互承認後の統合とrelease PRを担当する。ClaudeはPR64、追加端末テスト、公開API・成果物・利用文書の対向レビューを担当する。新規サブエージェントは起動しない。

Orca runtime `52f3b92b-a534-4d1e-a1db-d0e22a749584`。Codex `term_cf3261ec-1c55-4dd7-b66a-bcd6fe532734`、Claude `term_de4fd3ba-d1a1-458a-8184-a64c4403a379`。Codex Run `run_18f7185e91aa` を現在端末へ再結合（generation 4）。分担提案 `msg_dd0ffaa4dd2f`、端末送信receipt `5b60386f-80bc-4065-bab6-641be797c60f`、受け入れ条件追加 `d13dc305-a9e4-4a22-8fdc-1c1d095d6d11`。Claudeのsandboxから現Orca socketへ接続できないため、受諾は端末の実際の返信で確認し、enqueueだけを合意としない。

## PR64・PR63の整合

Claudeが既存Codex記録 `49622cc` と `3e05597` をPR64へ取り込み、head `9974c488e3afa4462eeb675ba04f16ed27052c0f` をpush。Codexはtreeが `3e05597` と同一で、CI `41189303564` が成功したことを確認した。製品・端末テストは前回検証候補から変更されていない。Claudeの記録承認[6541798467](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6541798467)とCodexのコード承認[6541804856](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6541804856)、現head CI成功・base・CLEANを確認し、2026-10-31 18:40:54 JSTに通常mergeで `5c8219b500291230eb423e3ba961e52663cc9df4` へ統合した。PR63の古いSettings検証と「次はT13」の断定を訂正し、10月18日の採用経緯は保持する。

## 現在の検証結果

- `3e05597`（PR64 headとtree同一）でcore/opt-timber/sampleのDebug・Release assemble、4 module unit tests、3 module lint、core/sample androidTest APK compileを一括実行し成功。
- unit tests: core 107、opt-timber 40、lint 12、sample 4、計163件。failure/error/skip 0。
- lint: エラー0。警告 core 9、opt-timber 4、sample 61。警告内訳と最終差分は後続チェックで照合する。
- API35専用AVDでcore 2/2・sample 9/9成功（skipなし、PR64を含む全既存suite）。`/private/tmp/ovm-release-api35-connected.log` と `ovm-release-evidence-20261031/api35-pr64` に結果を保存。
- buildログ: `/private/tmp/ovm-release-validation-build.log`。この一時パスだけを永続証跡とは扱わず、検証SHAと結果を本記録へ残す。

## 残る受け入れ条件

| 項目 | 状態 |
| --- | --- |
| PR63/64の現head相互承認・CI・統合 | PR64統合済み、PR63レビュー待ち |
| API23/26/35/36 core/sampleの最終端末試験 | 進行中 |
| 両scope、RTL、非ゼロ原点、edge-to-edge、画面外配置の実画面ドラッグとDOWN時no-jump | Claudeが追加試験を実装、Codexが実行 |
| 権限なしActivity操作、拒否→許可→再表示、回転・マルチウィンドウ | 既存証拠の照合と不足分の端末検証 |
| Release AAR/POM/sources/lint.jar/consumer rulesとJava/Kotlin/R8 consumer | Codexが最終検証 |
| 公開API・移行・配布・状態遷移の相互レビュー | 未完了 |
| release/3.0.0 → main PR | 上記完了後 |

JitPackサービス上のビルドは、10月18日のユーザー判断に従いタグ作成後の確認として分離する。今回のrelease PR作成を、mainマージ・タグ・配布の実行とは扱わない。
