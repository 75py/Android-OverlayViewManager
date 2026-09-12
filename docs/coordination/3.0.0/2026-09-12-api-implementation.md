# T04段階移行の事前整理

- 状態: 双方合意
- 関連タスク: T04a、T04b、T04c、T05、T06、T07、T08、T10
- 実装開始条件: T03・T04a統合済み。T04bを実装中。

## 独立した影響範囲調査

- Model/effort: gpt-5.6-luna / medium（launch.effective確認）
- Task / Dispatch: task_e5adce159111 / ctx_c2912737a6af
- Terminal: term_a750c908-ffd3-4bc6-bfc1-f1017854519e
- Worktree: codex-3.0.0-t03-final-check（T03でcore本番が未変更のため既存実装の読取に使用）
- Report: /private/tmp/overlay-t04b-migration-map.md
- worker_done: msg_75439ae19755（16:03:52 JST）。読取のみ、Gradle/実装なし。受領後releaseしtranscriptを保存した。

子のファイル・consumer一覧は準備資料として採用する。OverlayViewの可変params、内部OverlayWindowManagerのpost/catch、ScreenMonitor、旧drag listener、sample/Timberおよびテストの依存点を確認した。

## 司令塔が修正した提案

子の「結果を返す公開API・factoryをT04cまで遅らせる」案は採用しない。T07/T10が新APIへ移ってからT04cで一時互換APIを除去する合意済み依存と逆転し、循環が生じるためである。T04bで新しい同期・結果APIを提供し、旧setterや読み取り互換部分だけを一時的に保持する。未適用setter値と最後に成功したeffective specを分離する。

Codexは実ソースのSample2Activityでfactory...show()の戻り値をoverlayViewへ代入する1箇所を確認。この箇所はT04bで代入とshowを分ける最小修正が必要で、sample全体のKotlin化や表示説明の移行はT10に残す。単に返却値を無視する既存呼び出しは同じ理由で修正対象にはならない。

子の責務提案にあったinitの同一/異なるApplication契約はT04bのmanager契約に含め、opacity budgetはT06へ合わせる。T08はActivity寿命・強制破棄と参照解放を担当する。これらの訂正と最小consumer修正をmsg_c82bc658d500（16:05:23 JST）でClaudeへ相談。Claudeはmsg_bf7ddf364470（16:21:29 JST）で訂正・最小consumer修正・責務分担に合意した。調査報告の未採用案を公開契約として扱わない。

## T04a実装開始

T03統合を条件として、base00b45cb9fbc78d4911e164fc8c5109b936eafc16からTerra/high（launch.effective確認）を起動した。Task task_6ca5c3ca3e03 / Dispatch ctx_31dbaecdd7b2 / Terminal term_5969d6d8-366e-4c7d-b9ee-30d154b247f0 / Worktree codex-3.0.0-t04a-models。新規Kotlin型とそのテストのみ、既存coreクラスやbuild・plan/logを編集しない。Java Builder/gettersとKotlin named args/copyを実コンパイルし、範囲検証・不変性・結果を確認する。ローカルGradleを専有し、終了チェックポイントで共有する。msg_0e69e2bb1da0（16:10:30 JST）でClaudeへ通知。

## T04a完了と復旧時の並行操作

- T04a worker_done msg_b5c88cd44fe3（16:16:46 JST）を受領しrelease、transcriptを保存。候補3224165e015c4c95db54c3e4d6eeda517cc276c5、実装報告 /private/tmp/overlay-t04a-implementation-report.md。新規型・consumerテストのみで既存表示処理は変更しない。
- PR33はClaude側独立Sonnet5/highレビュー、exact-head APPROVE 5644524714とCI34680272503成功後、復旧対応端末 term_07348d72-aa3d-4f16-8b39-16701c3a947b がmerge997a958d0e13b35f1a7b1d5eeba267e4a0708a07へ統合した。当司令塔自身のmerge操作とは記録しない。
- 同端末はT07 f86c40fをpush済みだった。当司令塔の同SHA pushはreference already existsで失敗したが、remoteが期待SHAと一致することを確認し、再push/forceは行わなかった。msg_04b6e9bc9617で限定引き継ぎと以後のmutation停止を依頼し、msg_9117c4605dd6でClaudeへ一本化を通知した。17:00頃の同端末screenは通知送信の承認待ちで、引き継ぎ返答は未受信。
- PR33の非ブロッキング指摘はJava異常系・既定値・Builder(source) snapshotテスト補足、不要な12引数constructorのJvmOverloads削減。別小変更で検証・相互レビューする。Kotlin生成nullability annotationを基本としAndroidX明示はconsumer/lint根拠がある箇所に限定する案をmsg_44fa28c940e9で相談した。Claudeはmsg_0d126c52c1e6（17:02:50 JST）でannotation方針にも合意した。

## T07段階2の検証・統合

Claude担当のhead f86c40fec07845a91dead130cee175407a7a84f9はthresholdをvolatileにし、別スレッドから設定した閾値でログを絞るテストを追加。PR34、CI34681799560成功はmsg_73efff7d535eで共有された。

Codexは独立Terra/high（launch.effective確認）task_0f937a90d7eb / ctx_e954a5fd2d52をcodex-3.0.0-t07-threshold-reviewへ配置した。worker_done msg_9005d4fcec08（16:57:16 JST）、報告 /private/tmp/overlay-t07-stage2-review.md。blockingなし、実JUnit XMLは25 tests・失敗/エラー/skipなし、opt-timber lintエラー0。既定compileSdk36への更新通知は既存合意どおり非ブロッキング。受領後releaseしtranscriptを保存した。

新テストはlatch/joinとlogger開始でhappens-beforeを作るため、volatileを削除すれば失敗する回帰テストとは主張しない。同期のない本番setter/log間に必要な可視性は静的JMM検討で確認した。overlayViewの説明がgetInstanceからの参照まで網羅していない点は非ブロッキングで、Kotlin段階で正確にするようmsg_989bc344136fで依頼した。

Codex司令塔はexact-head APPROVE 5644608014を記録し、CI成功とSHA再確認後、PR34をd2a1dd6dd78fc1dd30ae0caa7f185cb6b30cb66aへmerge。main checkoutをfast-forwardした。mainへのmergeや公開は行っていない。

## T04b開始と並行境界

- Task / Dispatch: task_8dae019402ea / ctx_026ea69247c9
- Terminal: term_eddd1c86-e6a6-4bb2-8349-c606089c9bd7
- Model / effort: gpt-5.6-terra / high（launch.effective確認、turn_started確認）
- Worktree: codex-3.0.0-t04b-state、base d2a1dd6
- 所有: 既存core manager/view/adapter、新internal Kotlinとcore tests。旧listener/monitorの最小適応、Sample2Activityのshow代入分割のみ許可。T04a型・opt-timber・build・plan/logは編集しない。
- 受け入れ: 同期main-thread guard、add/update/remove失敗と再試行、no-opと診断保持、immutable effective spec、dispose参照解放、Java/Kotlin利用と段階consumerのコンパイル。T05/T06/T08に残す部分を報告で明示する。
- msg_44fa28c940e9でClaudeへT07 Kotlin変換を引き渡し。Gradle枠はClaude先行、T04bは編集可能だが実行前に枠を問い合わせる。競合するGradle/cacheとソース同時編集を避ける。

## T07 KotlinとT04a補足の起動確認（17:07 JST）

Claudeのmsg_f5a51449cf12: T07 stage 3 task_b34964bd9497 / ctx_ee0098f1485c / term_72e33a29-7093-4735-afad-186cce32af99、worktree claude-3.0.0-t07-timber-kotlin、base d2a1dd6。Sonnet5/highは起動引数で指定、手動作成端末のためlaunch.effectiveは空。候補後のpush・Gradle・独立検証はCodex側が担当する。msg_4ac10f75c9f0で受領した。

T04a補足: Luna/high（launch.effective・turn_started確認）task_2961dfd70174 / ctx_691d3023796f / term_ff1b0d7d-bffc-4e50-a866-ffffac0d65cb、worktree codex-3.0.0-t04a-followup、base d2a1dd6。編集はOverlaySpec.ktとOverlaySpecJavaConsumerTest.javaのみ。constructor overload削減とJava検証補足を行い、Gradle実行前に専有枠を問い合わせる。
