# T04c/T09/T11/T12: PR61の採用と統合

- 状態: ユーザー指示により統合済み。2026-10-25追記: 別UID透過をAPI36で確認（PR64）、配布はJitPack継続。T13は未完了
- 関連タスク: T04c, T09 stage 2, T11, T12
- 記録担当: Claude司令塔（Opus 5.5 / Claude Code、サブエージェントなし）
- 関連PR / head SHA: PR61 `99c918e720c93f20663eef379e4d2bcb63bc6888` → merge `2c256a83d5e41d9deaa6541c350d55bce1f7b108`、PR62 `96bf3fed06cdaf66c760d9eb32070a5fccf70bb6` → merge `208588f4fba70d16d69f154f95615785fd08cfba`、PR60 `9432a900dc625838705080e71e4350280629047f` → merge `9776926d28ad5577279d59a77f9f03a1aa231414`
- Orcaメッセージ / Task / Dispatch: なし。サンドボックスの許可ソケットが古いソケットのままで、Orca runtimeへ接続できなかった（EPERM）。Codex司令塔とは今回やり取りしていない。

## 背景・選択肢

2026-09-15 02:54 JSTの中断時点では、T04cはClaude worktreeでcommit2 `7d5af7b` まで保存し、commit3の7ファイルWIPが未コミットだった。2026-09-20にユーザー依頼のDevinが、`213a7d0` から `work/3.0.0-devin` を作り、PR61でT04cの再実装、T09 stage 2、T11、T12の端末テスト追加を行った。その後ユーザーがCIからAndroid SDK setup手順を外す2コミットを追加。PR61はレビューなし・未マージのまま、CIは成功していた。

選択肢は、(a) PR61を採用する、(b) 旧Claude worktreeのcommit3を完成させる、の2つ。Claudeは(a)を推奨した。理由はT04c〜T12をまとめてカバーしてCIも成功している一方、(b)はcommit3が未完成で全module検証も未実施だったため。

## やり取り（発言要旨）

| 日時・タイムゾーン | 発言者 | 提案・返答・異論 | 根拠・元メッセージ |
| --- | --- | --- | --- |
| 2026-10-10 JST | ユーザー | 進捗の確認 | 対話 |
| 2026-10-10 JST | Claude | 状態を報告し、PR61の採用を推奨 | git refs、`gh pr view 61` |
| 2026-10-10 JST | ユーザー | 「それで進めて」 | 対話 |
| 2026-10-10 13:43 JST | Claude | 全差分をレビューし、承認コメントを投稿。マージは自動許可の判定が拒否 | issuecomment-6093875504 |
| 2026-10-18 15:38 JST | Claude | レビュー指摘1・2をPR62で修正し、draftで作成。CI run 39104750420が成功 | PR62 |
| 2026-10-18 16:34 JST | ユーザー | マージを許可。ClaudeがPR61をマージ。PR62はClaudeのready化が拒否されたため、ユーザーが自分でready化・マージ | 対話 |
| 2026-10-18 JST | ユーザー | PR60は推奨どおり統合、端末確認を許可、配布はJitPackをやめMaven Centralを検討 | 対話 |
| 2026-10-18 JST | ユーザー | 需要を検討した結果、JitPack継続・mainへ実績として公開・その後は最小限の保守（案B）を選択。Maven Central移行は見送り | 対話 |
| 2026-10-25 JST | ユーザー | API36エミュレータで端末テストを実行。初回1件失敗 | b2a927e、`build/t12-connected.log` |
| 2026-10-25 JST | ユーザー | `SAME_UID_ONLY`の列挙名は変えず、説明を直す案を選択 | 対話 |
| 2026-10-25 JST | ユーザー | 修正版を再実行し、sample 9/9成功 | b2c484e、PR64 |

## 判断結果・理由

PR61のレビューで、差分そのものにマージを止める欠陥はなかった。確認した内容は次のとおり。
- 削除したAPIの参照が残っていない。
- `update(OverlaySpec)`だけの変更経路で、DRAGGABLE→DRAGGABLEのときdrag listenerを再利用する。
- lint detectorが宣言クラス・引数数・定数評価を確認している。
- version/jitpack設定がpublicationと整合している。
- 追加された端末テストが空振りにならない（再生成と透過の反証つき）。

リリース前のフォローアップとして4点を指摘した。

1. `OverlayView`の`internal constructor`がJVMでpublicで、`disposeForActivityDestruction$core`も同様。旧ビルドの`javap`で実証した。→ PR62で、private constructor、`@JvmSynthetic`のinternal companion `create()`、`@JvmSynthetic disposeForActivityDestruction`、`OverlayViewJvmVisibilityTest`を追加。
2. CHANGELOGと移行ガイドに、互換APIが残る旨の古い記述があり、lint IDの変更も未記載。→ PR62で修正。
3. lintがKotlinのconstructor/`copy`の名前付き引数を検出しない。→ 移行ガイドに既知の制限として記載した。detectorの拡張は未対応。
4. 別UIDアプリへのタッチ透過は未確認で、release-blocking。JitPackビルドは、ユーザーがJitPack廃止を検討しているため不要になる見込み。

PR62はClaude側のPRで、規約上はCodexの対向承認が必要だった。Codex司令塔が稼働していなかったため承認はなく、ユーザーの明示許可とCI成功を根拠にユーザー自身がマージした。PR60はCodex作成の記録PRで、Claudeがhead `9432a90`へ承認コメントを付けて統合した。記録内容の時点（T04c途中）はPR61で置き換えられたが、経緯の記録として残す。

## 検証

- PR61 CI run 35492821736 (`99c918e`)、PR62 CI run 39104750420 (`96bf3fe`): assemble、unit test 4 module、lintすべて成功。
- サンドボックス内ではGradleが起動しない（ローカルbind不可でFileLockContentionHandlerがEPERM）。adb serverも同じ理由で起動しない。PR61の161 tests、publishToMavenLocal、R8 consumer、API23/26/35/36の端末テストはDevin申告のままで、未再現。

## 未解決事項・次の担当

- T12: 別UIDアプリへのPASS_THROUGH透過と不透明度上限の拒否を、API35/36のエミュレータで確認する。ユーザーは実施を許可済み。サンドボックスの`allowLocalBinding`を許可してもらう必要がある。担当はClaude。
- T11: 配布をMaven Centralへ移す。必要な作業は次のとおり。
  - groupIdとnamespaceを決める。
  - 署名・POM情報・sources/javadocのpublish設定を入れる。
  - `jitpack.yml`を削除し、READMEと移行ガイドの導入手順を書き換える。
  - 実公開は計画の範囲外のまま。
- T09: Kotlinの名前付き引数をlintで検出するかは任意の改善で、未決定。
- 旧worktree `claude-3.0.0-t04c-api-cleanup`（7d5af7bと未コミットWIP）は不要になったが、削除はユーザー確認後。
- T13はT12完了後。

## 計画・実装・検証への影響

T04、T04c、T09は統合済みに更新した。T11は一部統合（配布方法の変更待ち）、T12は作業中（端末確認待ち）とした。T04c/T06のPR40フォローアップ3件のうち、可視性と暫定listenerの2件を完了扱いにした。DISPOSED診断KDocの件は未確認のまま残す。

## 2026-10-25追記: T12の別UID透過確認と配布方針

### 配布

ユーザーはライブラリの需要を検討し、案Bを選んだ。3.0.0は実績としてmainへ出し、配布は設定済みのJitPackを続け、その後は最小限の保守とする。上記「未解決事項」のT11（Maven Central移行）と指摘4の「JitPackビルドは不要になる見込み」は、この判断で取り消す。JitPackビルドはタグ作成後に確認する。

### 別UIDアプリへのタッチ透過

PR64（`claude/3.0.0/t12-cross-uid`）で`sample`に`CrossUidPassThroughTest`（API31+）を追加した。画面全体のアプリオーバーレイの下に設定アプリ（UID 1000）を置き、検索バーをタップして、検索画面のEditTextが出るかで到達を判定する。

ユーザーがemulator-5554（API36、Foldlytics_Pixel_9_Pro_Fold_API_36）で`:core:connectedDebugAndroidTest :sample:connectedDebugAndroidTest`を実行した。b2a927eではcore 2/2成功、sample 8/9成功。失敗は`opaqueSameUidOnly_isBlockedByThePlatform`だった。Claudeがadbで単独再実行し、logcatで次を確認した。

```
App com.nagopy.android.overlayviewmanager.sample has a system alert window (type = 2038) with FLAG_NOT_TOUCHABLE and LayoutParams.alpha = 1.00 > 0.80, setting alpha to 0.80 to let touches pass through
```

WindowManagerは、上限を超える非タッチのアプリオーバーレイのalphaを上限まで下げ、タッチを通す。`SAME_UID_ONLY`はライブラリの不透明度チェックを省くだけで、別UIDへのタッチを止めない。設計書もこのモードでの別UID到達については何も約束していないので、実装は仕様どおり。ただし列挙名から「別アプリには届かない」と誤解されうる。

ユーザーは案1（名前は変えず、説明を直す）を選んだ。改名して公開APIを変える案2は採らなかった。b2c484eの変更は次のとおり。

- 3件目のテストを`opaqueSameUidOnly_isDimmedByThePlatformAndPassesTheTap`（到達を確認）に置き換えた。
- `CrossUidPassThrough`にKDocを追加した。
- READMEと移行ガイドに、`SAME_UID_ONLY`でも別UIDにタッチが届き、alphaが0.8へ下がることを追記した。

ユーザーの再実行でsample 9/9成功、skipなし。

### 残り

- T12: API23/26/35の端末テストはDevin申告のみで未再現。API36以外での再実行はユーザーの判断に委ねる。
- PR64はClaude側のPRで、Codexの対向承認が規約上必要。Codex司令塔は稼働していないため、ユーザーの許可で統合する。
- T13: `release/3.0.0`の作成とmain向けPR。
