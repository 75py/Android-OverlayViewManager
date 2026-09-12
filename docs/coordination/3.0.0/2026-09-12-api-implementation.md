# T04段階移行の事前整理

- 状態: 双方合意
- 記録担当: Codex（Astra / medium）
- 関連PR: #33、#34、#35、#36、#37
- Codex Run: run_18f7185e91aa
- Claude Run: run_3ae778449744
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

## PR35統合とT04a補足の差戻し・検証

PR35 head08c6b4fはClaude独立レビューと承認5644660877、CI34682494798成功後、復旧端末により9691d35b7a35d463800dbe0a026a0d9f6ac7fae8へ統合された。当司令塔は重複mergeせず、msg_4237917de1d7で実施者を区別して共有。非ブロッキングのログ見出し欄を今回補った。

Lunaの初回補足候補6aee461は、sandbox内Orca接続エラーでGradleを実行せずworker_done msg_1e216b05eebcを送った。実装のみ受領・releaseし、検証未完了とCodexの検証責任をmsg_850d6a0cf848で明示した。親の差分確認で、JavaテストがMATCH_PARENT=-1とWRAP_CONTENT=-2を不正値と誤認していることを発見。実装の正常値許可を変えず、テストを差し戻した。

修正・検証はLuna/high（launch.effective確認）task_d60f7c17a9b3 / ctx_3ee505bcf0b4 / term_34f93e8c-f9e8-48c0-80aa-59c14ec7b709、同じcodex-3.0.0-t04a-followup checkoutで担当。初回の既存checkout起動は--setup指定が不適合としてpreflight拒否され、余分な引数を除いた起動のみ成立した。Claudeからmsg_ed15b5e6fb8cでGradle枠返却後、指定JDK/SDKでcore:testDebugUnitTest成功。XML83件、失敗/エラー/skipなし、javapでnoarg/full constructorを確認した。通知中のSDK34表記は古いガイド由来の誤記で、実コマンドは指定SDKパスとchecked-in compileSdk36を使用した。Gradle枠返却msg_040e849e97f5、最終worker_done msg_4f9250c35a98、受領後release。最終head2d2dc4b580b1b8c9b0ba177c7deff6a17ea9d675をPR36にし、msg_17abd2259398でClaudeへ独立レビューを依頼。

## T04b一次確認での修正要求とmonitor境界

T04b担当は4a24cd21c6b4dea449843df2f94621c27c0e9015を未検証で作成し、sandboxのruntime_unavailableを停止と判断してターンを終了した。親は実runtimeへ接続可能であること、worker_done未送信を確認し、同じterminalへ通信復旧と継続を送りturn_startedを確認した。重複editorは起動していない。報告中のruntime停止は確認済み事実ではなく、sandbox接続制限による観測だった。

親はmsg_544b212f28b4で6点を差し戻した: dispose後のbackend/WM参照保持、external detach後の古いlistener残存、application brightnessのupdate/旧setter経由の検証漏れ、main Looper null許容、Throwableの過広な捕捉、DRAGGABLE spec経由のlistener適用漏れ。これは承認前の候補確認であり、実装・独立検証完了ではない。

monitorのrequest/cancelを新state pathから外したことで補助window失敗が結果へ混入しないことは担当msg_5161111ac97aで確認。一方、旧dragのmonitor座標が未測定0になる影響を親が指摘し、msg_5c517443e6fdでClaudeへ相談。Claudeのmsg_16212d73b4f5で、T04bに最小座標橋渡しと最低1件の検証を含め、insets等の限界を明示し、T05で完全boundsとmonitor撤去へ進めることに合意した。msg_221f0cf139ffで担当へ受け入れ条件を伝達。T04bのGradleは修正checkpointまで待機、T07検証担当が先に専有する。

## T07 Kotlin候補の独立検証開始

Claudeがmsg_16212d73b4f5 / msg_f0f25dd8788fでa95f227e2be6776aeaa9f45a97151e4162183995を確定。Codexがclaude/3.0.0/t07-timber-kotlinへpushし、msg_c81372b69b2fで通知した。

独立Terra/high（launch.effective・turn_started確認）task_18c7821b0712 / ctx_c704670bf482 / term_a5620eef-80f8-4648-aaf7-af09e962eac3、codex-3.0.0-t07-kotlin-review、base a95f227。検証はopt-timber tests/lintとsample assemble、Java/JVM公開APIとfield初期化・並行処理を対象とする。package-privateからpublicへ拡大したテスト用要素、overlayView説明のgetInstance参照欠落も審査する。候補をKotlinへ変換しただけで承認済みとはしない。

## PR36統合とPR37差戻し（17:31 JST）

ClaudeはPR36 head2d2dc4bを独立Sonnet5レビュー後に承認5644737864（msg_6309a45cb52b）。子のnoarg constructor消失という疑いは、全引数既定値のKotlin仕様と実javap結果で司令塔が解消した。CI34683075403成功を照合し、Codexが41d74aa45284d3b18423b30ec03da5f85a857cf4へmergeした。これでPR33のJavaテスト・constructor補足を完了とする。

T07独立レビューworker_done msg_8dfd45aaa87aを受領・release。25 tests・lintエラー0・sample assemble成功だが、javapで旧package-private constructor/initialize/postToMainThread/INSTANCEおよびbuffer等のpublic化を確認した。外部から同期を迂回して内部状態を操作できるため、候補a95f227は未承認。PR37コメント5644737303とmsg_84dcded7d6c0で必須修正を依頼し、Claudeはmsg_b6db52e0b966で担当への差戻しに同意した。

公開APIを広げないことを優先し、既存Javaテスト無変更という下位制約を緩和する。msg_89add64cdeffで、protectedも外部subclassに公開されるためprivateを基本とし、test sourceのreflection helper等で検証目的を維持しproductionの新hookを避ける方針を補足した。元からprotectedのlog overrideは維持する。KDocのgetInstance参照漏れとregisterのmain-thread断定も修正対象。

T04b担当はb316ff8でterminal diagnostic保持を補ったが、親の6点差戻しを処理せず再度検証待機でターンを終えた。親は同じterminalへ6点とmonitor橋渡しを再提示し、T07レビュー終了後のGradle枠を明示GRANT、turn_startedを確認した。msg_9c2a31cba059でClaudeへ枠所有を通知。T04bの受け入れ・独立レビューは引き続き未完了。

## 検証報告の訂正とT04b追加差戻し（17:44 JST）

PR38 head3cdd008はClaude承認5644781045とCI34683563498成功後、Codexがc9ef9341fa90a7a54665328a8d19601ba766c43fへ統合した。

T04b worker_done msg_73f432d92882はfbfd42eと46 tests成功を報告したが、親が実TEST XMLを数えると51件・失敗/エラー/skipなしだった。対応表も不足していたため、同じTerra/high terminalをread-only補足task_696a81ba78fb / ctx_132bf59e1f51へ再利用した（新たなmodel設定ではなく起動済み同一processの継続）。既存terminalをmain checkoutで起動しようとした最初の再利用はworktree mismatchで拒否され、正しい既存worktree指定の起動だけが成立した。

補足worker_done msg_21f91d4d4367で /private/tmp/overlay-t04b-implementation-report.md を51件へ訂正し、無関係なsource XMLタグ数を除去。/private/tmp/overlay-t04b-test-migration.md に振る舞いごとの移行対応と不足を整理した。permission bridge、許可されたwindow type/flag、旧bridge/click、CANCEL、実gesture列等に検証不足があると確認。API28へのtest pinはJDK17で無指定API36が走る問題を避けたが、元のAPI23検証を保持した証拠にはならない。

親はさらに、既定DRAGGABLE listenerがupdate成功ごとに再生成され、DOWN内updateの後のMOVE/UPで座標/alpha状態を失う経路（msg_8eaac1189617）と、dispose後の旧custom setterがguard前にpending listenerを再保持する経路（msg_1bc3bca7e286）を指摘した。補足担当も未修正T04b欠陥と確認し、T05/T06へ先送りしない。

補足終了後、同じTerra/high terminalをtask_6a0a8696194e / ctx_3cedaff14c1aへ再利用。2欠陥、実DOWN→MOVE→UP/CANCEL、API23最低境界とAPI26 type、permission preflight/bridge、代表的layout/clickの回帰検証を追加する。msg_252f24550474でClaudeへ共有した。Gradle ask msg_ebe118c3eac1へreply msg_70ad394a3fd5（17:46:13 JST）で専有をGRANTし、msg_ffe6e0379176でClaudeへ通知した。

担当が後から許可待ちと報告したため、msg_47d9888fd4a0で回答ID付き許可を再通知。blocking CLIをexec_commandで開始した際にoutputだけを表示するとsession_idを失い、返答を回収できない可能性をmsg_72bde9a97a93で指摘した。これは確認済み原因との断定ではない。検証担当は引き続き同じdispatchであり、新しいeditorを重ねていない。

## T07とKotlin coreの統合境界

- msg_a2c906fe52eb / Claude msg_41d196cdea94: T07 KotlinのgetView()呼び出しをview propertyへ揃える。旧Java coreのsynthetic propertyと新Kotlin val viewの両方で利用し、生成Java getView signatureは維持する。
- msg_ea2a4070ba9a: 旧Timber fixtureはrunnerなしで毎回mock Applicationを作りcore.initを呼ぶため、T04bの実main Looper必須・同一Application init契約と不整合になる。T07のtest適応へ実main Looperと適切なtest限定初期化を含めるよう相談した。
- msg_e60771b4f61e: opt-timber/build.gradle.ktsにはRobolectricがないため、必要なら既選定4.16のtestImplementation追加だけを許可。coreの契約を弱めてテストを通さない。
- msg_5daa1a48067aでT04b担当へ既存Timber unit testsも一度実行し、既知fixture失敗とcore成否を区別して報告するよう依頼。統合前にcore/Timberを組み合わせたunit testsを実行する。PR37のprivate化・test適応は引き続きClaude所有。

## T04b 59件検証とproduction test seamの除去（18:05 JST）

worker_done msg_1e7319d86f83（task_6a0a8696194e / ctx_3cedaff14c1a）で2ea8c0262f1041437d572f33427cc93d6c6638b9を受領。core 59件は失敗/エラー/skipなし、3 module debug assemble成功。opt-timberは既知のmock Application fixtureで25件すべて失敗しており、統合検証成功とは扱わない。

Claudeのmsg_28a0c95a3eb0はfixture対応としてproduction reset hookを提案した。Codexはmsg_d4f9a400b6e3で、新しい公開APIをtest都合で追加しない方針から却下し、T04bに既に入っていたpublic resetForTestingも除去することを提案。Claudeはmsg_34efa4d396d4で合意し、test sourceのreflection helperでprivate static singletonを初期化する方式へ変更した。coreのprivate field名instanceを維持し、テスト外のcleanup保証は追加しない。

完了済みTerra/high terminalをtask_ec0232c0b942 / ctx_e3fa4f8be2d9へ直ちに再利用した。同一起動済みprocessのため再利用receiptのlaunch設定はnullであり、新しいモデル起動の証拠とはしない。範囲は新しいproduction test seamsの除去、test-only reflection、未検証の限定的state失敗経路。core Gradle枠を担当へ付与し、msg_b3a2c239cf63でClaudeへ共有した。PR37の修正候補と組み合わせた検証・独立レビューは未完了。

Claudeのmsg_e5e61527ed32でPR37修正候補c0d27e12887f2bbeabe46865cb3df33f9fd472deを受領し、Codexが既存branchへ通常push。独立Terra/high（launch.effectiveとturn_started確認）task_2dcf2927b0a5 / ctx_fa0d32833fc2 / term_c4e636a6-e846-49ce-9410-2c4c083b1d8dをcodex-3.0.0-t07-rereviewへ配置した。まず静的レビュー、core検証完了後にGradleを移譲して候補単体とT04b組み合わせを確認する。組み合わせ操作が実装checkoutへ混入しないよう別worktreeとした。msg_6b9a5b8ff5f6でClaudeへ通知。
