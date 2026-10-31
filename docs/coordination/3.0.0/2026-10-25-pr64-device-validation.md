# T12: PR64の別UIDタップ透過を再検証

- 状態: receiver方式・所有範囲は双方合意。候補の端末検証成功。最終headの対向承認・統合はこの記録時点では未完了。
- 記録担当: Codex。既存Claude司令塔がコードと利用文書を修正、CodexがSDK・端末検証と計画・ログ更新を担当。新規サブエージェントは起動していない。
- PR: [#64](https://github.com/75py/Android-OverlayViewManager/pull/64)。base `9776926d28ad5577279d59a77f9f03a1aa231414`、検証候補 `c9d8334f358a72217419947ddc3bc2bb57c61e9a`。
- 独立検証worktree: `codex-pr64-device-validation` / branch `orca/codex-pr64-device-validation`。実装worktree: `claude-3.0.0-t12-cross-uid` / branch `claude/3.0.0/t12-cross-uid`。

## 背景と判断

ユーザーはPR64レビュー、レビューコメントの投稿、その後のAPI31/36検証を依頼した。最初の指摘は、Android 12初期版には2022年3月のWindowManagerによる不透明度補正がないため、API31以上を一律に「alphaを上限へ補正して通す」と扱えない点だった。

- [当初のレビュー](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6396264825)
- [AOSPの不透明度補正](https://android.googlesource.com/platform/frameworks/base/+/a451d5bae2e2fb1e75152c596a6f476d6f7ac1ec)

再検証で、旧テストのSettings画面が非システムのオーバーレイを隠すことが判明した。API31のwindow dumpは `mForceHideNonSystemOverlayWindow=true`、`mPolicyVisibility=false`、`isReadyForDisplay()=false`、`mLastAlpha=0.0`。Viewのattachmentと座標の包含だけでは、タップ時に表示されている証拠にならない。**Settingsを用いた以前のsample 9/9成功は、別UID透過の証拠として採用しない。**

- [Settingsのoverlay非表示処理](https://android.googlesource.com/platform/packages/apps/Settings/+/a74060c1b7002edcc6d2747addc1dfb61aa98cbb)
- 途中でAPI31の検索欄がEditTextではなくAutoCompleteTextViewである点も修正したが、Settings自体が不適切と判明したため、最終方式ではそのselectorを使わない。

## 調整の要旨（2026-10-25 JST）

1. CodexはAPI31イメージとAPI36の検証を専有し、Claudeはテストと利用文書を所有することで合意。Gradle・SDKの競合を避けた。
2. CodexがSettingsの非表示証拠と、未初期化の同UID input layer（type UNKNOWN / visible=false）を誤採用する問題を報告。Claudeはvisibleなoverlay input windowを待つ方式へ変更した。
3. Codexが「既存androidTest APKに受信Activityを置く」方式を提案。Claudeが同意し、`c9d8334`で実装。新moduleやproduction behaviorの変更は不要と判断した。受信Activityはoverlay非表示やobscured-touch拒否を設定しない。
4. 受信側のpackage UIDとsample UIDが異なること、画面上の実行UIDがtest APKのUIDであること、表示中overlayがタップ点を覆うことをassertする。タップ後の画面上の回数を検証する。
5. 計画・調整ログはCodexだけが編集し、Claudeがそのcommitを確認してPR64へ取り込むことで合意。最終文書は「多くのビルド」等の普及率の推測を避け、補正の有無に依存する説明にするよう依頼した。

Orca: 既存Claude端末 `term_de4fd3ba-d1a1-458a-8184-a64c4403a379`。所有権連絡 `8350f35c-5924-4ca4-9ee2-b37ea628d3df`、selector報告 `0cd904ad-f6f0-42ef-b89b-9f5f47db90d8`、非表示証拠 `f53087c2-0f0f-40b8-81c4-bbf3d5e5208e`、再検証結果送信 `cfb3b052-cd9f-444e-8cba-c98887b8ef63`。受信・応答が確認できた合意と、結果送信のreceiptは区別する。永続の結果報告は[PRコメント](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6396450325)にも保存した。

代理許可: ユーザーが許可した範囲で、Codexが完全なコマンド・対象・効果を確認し、4ファイルのcommit/通常push（`3d7ae4a`）と、test manifest/CrossUidPassThroughTestだけを変えるPython/sedスクリプトを許可した。前者の通常送信はpermission promptに拒否されたため、画面のYesをbare Enterで選択（receipt `ce3a790d-6215-4f90-84b5-3ff305274c2f`）。後者は完全なスクリプトを確認して同様に許可（`570a73da-a480-4770-a42b-f789267fddea`）。merge・release・公開は許可も実行もしていない。

## 検証結果

JDK17、Gradle9.4.1、Android SDK36 / Build Tools36.0.0、emulator37.1.11を使用。各環境で同じ候補 `c9d8334f358a72217419947ddc3bc2bb57c61e9a` を順次実行した。

```sh
ANDROID_SERIAL=<serial> ./gradlew :sample:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nagopy.android.overlayviewmanager.sample.CrossUidPassThroughTest \
  --console=plain
```

実行時には上記コマンドを `rtk proxy env` 経由で呼び出した。

| 環境 | serial | 結果 | alpha=1要求時の表示中input alpha / タップ |
| --- | --- | --- | --- |
| API31 initial arm64 r02 | emulator-5584 | 3/3成功、failure/error/skip 0 | 1.00 / 遮断（InputDispatcherのdropログあり） |
| API31 default arm64 r03（SDK repository revision4） | emulator-5582 | 3/3成功、failure/error/skip 0 | 1.00 / 遮断（InputDispatcherのdropログあり） |
| API36 Google APIs arm64 | emulator-5554 | 3/3成功、failure/error/skip 0 | 0.799805 / 受信側tap countが1 |

3件の内訳は、上限の半分のalphaで別UIDへタップ到達、上限超過のWHEN_SYSTEM_ALLOWSをattach前に拒否、不透明SAME_UID_ONLYがOSの実際のalphaに応じて通過/遮断すること。上限は全環境で0.8。低alphaはAPI31で0.40、API36で0.399902。API31はvisible=true/type2038、API36はNOT_VISIBLEを含まないNOT_TOUCHABLEのinput windowを確認した。各ケースのUID assertionsも成功した。

Build fingerprints:

- API31 r02: `Android/sdk_phone64_arm64/emulator64_arm64:12/SC/7752726:userdebug/test-keys`
- API31 r03: `Android/sdk_phone64_arm64/emulator64_arm64:12/SE1A.220621.001/8752307:userdebug/test-keys`
- API36: `google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`

初期イメージは[Google公式arm64-v8a-31_r02.zip](https://dl.google.com/android/repository/sys-img/android/arm64-v8a-31_r02.zip)（Last-Modified 2021-10-04、636979726 bytes、SHA256 `2124286179ce00b613fa109009eba06985bb6ec7c799d57173489842587e4864`）。専用AVDから別ディレクトリのimageを参照し、通常SDKのAPI31 imageは置換していない。

ローカル証跡: `/private/tmp/ovm-pr64-evidence/c9d8334-api31-r02-results`、`c9d8334-api31-r03-results`、`c9d8334-api36-results` に各XMLとtest logcat、同prefixの `*-gradle.log` にビルド結果を保存。Settings非表示のdumpは `api31-r03-window-observation.txt`。一時ディレクトリの保存は永続保証ではないため、本ログにもfingerprint・SHA・コマンド・観測値を記載した。

候補の[CI run 40184221635](https://github.com/75py/Android-OverlayViewManager/actions/runs/40184221635)も成功。その後の `43b1b3b47fa543595f015983dfa955ca028079d7` はREADME・KDoc・移行ガイドだけを修正し、androidTestは `c9d8334` と同一であることをCodexとClaudeが差分で確認した。[43b1b3bのCI run 40184685971](https://github.com/75py/Android-OverlayViewManager/actions/runs/40184685971)も成功。端末検証は上記3テストのみで、sample全体やrelease matrixの再実行とは区別する。

## 残るゲートと記録の整合

- 最終の説明修正と本記録を含むheadに対して、差分確認・ClaudeによるCodex文書の確認・CodexのPR64承認・CI確認が必要。mergeは実行していない。
- PR63 head `f1627229a60b302923379dc480c622063a2fd42d` の「Settingsで端末確認済み／次はT13」は上記発見より前の記録。今回のreceiver結果で置き換え、PR63/64統合時には新しい履歴を失わず計画を整合する。配布をJitPack継続とするユーザー判断は変更しない。
- 本作業はAPI31/36の別UID透過に限定。合意済みAPI23/26/35を含むT12全体、T05/T06の権限・drag・座標等のrelease-blocking試験や最終consumer/成果物検証を完了扱いにしない。未実施ゲートは両司令塔が別途証跡を照合して閉じる。

## 記録の対向レビュー（2026-10-25 19:28 JST）

Claudeは記録commit `49622cc` を読み取りレビューし、XML・logcatの数値、fingerprint、候補CI、PR63 headの一致を確認した。Orca receiptと初期imageのSHA256はClaude側では未照合（Codexが取得・記録）。指摘は、現head `43b1b3b` とCIの追記、PR63との統合手順の具体化、計画末尾の空行1行削除の3点。本追記と上記CI説明で対応した。

PR63との整合は次の順を提案し、実際のmerge/rebase/pushを行う承認とは区別する。PR64を先に統合する場合、PR63所有者Claudeが最新baseを取り込み、計画と調整ログの旧2026-10-25節を本記録への参照に置換する。状態行の「次はT13」とT12行の旧sample9/9を訂正し、T12全体の未完了ゲートを保持する。2026-10-18の統合履歴を失わず、10月18日→25日の順に並べる。競合解消後はCodexが差分を再確認し、新head承認・CIを確認する。両PRは現在未マージで、この手順は将来の統合時のもの。

記録のPR64 branchへの取り込み/push依頼は自動承認レビューが拒否し、ユーザーの明示回答待ち。Codexは許可不要の読み取りレビューとローカル修正まで完了し、取り込み/push/PR63変更を実施していない。[コードレビューと現head CIの報告](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6396511235)は投稿済み。最終merge承認は記録反映後のheadに対して別途確認する。専用API31エミュレータ5582/5584を停止し、既存5554は起動したまま保持した。
