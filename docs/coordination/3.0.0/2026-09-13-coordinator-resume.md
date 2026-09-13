# 2026-09-13 司令塔の直接作業による再開

## 再開と担当

09:35 JST時点でユーザーから「再開して」を受領。以前の停止指示を解除し、最終到達点は引き続きrelease/3.0.0からmainへのPR作成までとする。mainマージ・タグ・公開は含まない。

- work/3.0.0の7653767とホームの~/.codex/AGENTS.mdを再読。現在のプロジェクト固有規約に従い、既存司令塔が直接作業する。新規サブエージェントを既定で起動しない。
- Claude報告msg_20aaddf38cd2: 新Task task_d50e1eceb22f / Dispatch ctx_a50bedd97da3の旧担当へ安全な境界でworker_doneによる返却を指示した。返却後、Claude司令塔Fable 5.1/highがt07-lifecycleでT07 L1–L7の実装・検証を直接担当する。返却完了はこの記録時点では未確認。
- Codex返答msg_3d47d3c5c097: ユーザーの明示再開を伝え、上記担当とCodex Astra/mediumによる対向レビュー・共有計画ログ統合を承諾。Claudeの作業割合を大きく保つ。Claude独自のT07ログ以外の共有計画・CodexログはCodexが編集する。
- 直前の方針共有msg_5fec3fe5159aと今回の返答を合わせ、古いworker再起動案を引き継がない。02:30自動再開の解釈については過去ログを保存し、今回の直接指示を現在の再開根拠とする。

## 未完了と受け入れ条件

保存済みWIP 6e243c3は候補ではない。描画世代の予約、initializeのbuffer/maxLines同一lock、dispose全経路のmain-thread検査の3点を差分と実行検証で確認する。L6/L7、Java利用互換、削除失敗時のretry、成功dispose後のno-opを検証する。候補SHAの対向承認とCI成功後のみwork/3.0.0へ通常mergeする。

T10、T04c、T09 stage 2、T11、T12、T13は未完了。T05/T06端末検証を完了扱いしない。今回の記録変更は文書差分と参照先を検証し、Gradleは実装担当へ一元化する。

## 保存済み差分の確認

Codexはコミット6e243c3のproduction差分を読み、msg_1ee3d4d99ee7で追加の確認を依頼した。共有renderRunnableが予約時の世代を保持せず現在の2フィールドを比較するため、古いcallbackが新世代の予約を処理できる。旧callbackだけを先に実行する回帰検証と、実装・KDocの契約の一致を候補で確認する。存在しないinitApplicationInstanceへの参照と計画内識別子FROZEN L2/L3も利用者向け記述へ直す。未完成WIPに対する観察であり、完成候補へのレビュー結果ではない。

文書PR53はhead6526b6bで既存、承認コメントなしを確認した。このPRを今回の再開状態へ更新し、更新SHAのClaude承認とCIを要求する。以前のCI成功を新しいheadの成功根拠に流用しない。

## 09:40–09:46 JST 調整と代理許可

- PR53のhead fed9707827bf8bf3791c3276c061204cfcec7f8dをClaudeへレビュー依頼（msg_fcfbd80cb19e）。CI run34728558459はSUCCESS。Claude承認はまだ未受領。以後のログはcoordination-wave17へ分離し、レビュー中headを固定した。
- Claudeの環境確認が長引いたため既知のJDK/SDKとCodexによる最小検証・GitHub代行を提示（msg_5329f94f5fd1）。T07編集は引き続きClaude所有。
- 旧worker画面に司令塔handleでの受信箱checkが表示されていたため、msg_3c03fe9314b1で宛先訂正と未受領メッセージ確認を要請。workerの操作をCodex側で代理実施していない。
- T07はf2bc6ffとe59cd1dへ履歴整理済み。e59cd1dのwatcherテストはlock外からArrayDequeを読み、正常な追加→trimの途中を誤検出できる。またlog開始がinit完了後のためinit競合を再現しない。msg_d5ee64215eaeで修正を依頼。Gradleはまだ実行していない。
- Claude司令塔の長時間の環境照会を中断して受信箱確認へ戻す入力を送り、入力受理を確認。Task/Dispatchの停止・abandon・replacementやファイル変更は行っていない。
- その後のpermissions.blockReadsOutsideWorkingDirectoriesダイアログでは、orca orchestration checkのJSONをpython3で整形表示するコマンド全体を確認。ファイル書込や無関係な読取はなく、ユーザーの既存代理許可指示の範囲と判断した。text+Enterはagent_prompt_blockedで未実施、同request IDの再確認も同結果。選択済みYesへのraw Enterがacceptedとなり、次の受信箱読取に進んだことを画面で確認した。権限設定の緩和は行っていない。

## 09:49 JST worker返却の代理許可拒否

worker-showでctx_a50bedd97da3がmsg_4cc722724d09によるworker_reportでsettled/failedとなったことを確認。HEAD8b464b7709d01fd9ff12be12d8d4378ff9d4a464、tracked clean、テスト未実行。端末はlive/external/retained。

Claudeのack delivery_d19978dcf0a7 + worker-release ctx_a50bedd97da3 + reclaimable一覧コマンド全体を確認後、代理Enterを要求したが、自動承認レビューが「liveかつrelease未要求、明示的な完了・返却確定前の代理許可」として拒否。Enterは未実行。ダイアログをraw Escapeでキャンセルし、msg_f5f70494021eで返却操作の保留・外部端末維持・回避実行禁止を連絡した。これは既存担当の完了報告を取り消すものではなく、release操作だけを実行していない。

PR53承認の送信処理はClaude画面に表示されたが、Codex受信箱にはまだ未着。承認済みとは扱わず、送信結果の確認を依頼した。

## T07候補の検証成功・PR54作成（09:58 JST）

Claude msg_6cd082a5f543でownership返却を確認し、worker-releaseは保留・端末維持のまま、司令塔が直接修正することに合意。PR53の承認文を依頼どおり代理投稿（comment5649792601）し、head fed9707・CI34728558459 SUCCESS・MERGEABLE/CLEANを再確認後、64f8897a510449e6eb825b261ccad4b37c0299d0へ通常mergeした。

Claude msg_d9074f9d96d9: 候補a39813a5cea32f2d89e0bab835e2074c17976cfb。世代をcallbackへcaptureする修正、旧callbackのみを先にdrainするテスト、誤ったwatcherテスト削除を司令塔が直接実施。buffer/maxLinesの同一lockは差分レビューで確認し、競合テストで保証したとは扱わない。

Codexが候補SHAを確認し、JDK17/既知SDKで依頼されたGradleを実行。BUILD SUCCESSFUL（20秒）。XML実測はKotlin39 + Java1 = 40 tests、failures/errors/skipped全0。opt-timber lintはエラー0・警告2。opt-timber/core/sample assembleDebugすべて成功。ソース変更なし、tracked clean。msg_e78097b4a1a1で結果とレビューblockingなしを通知した。

検証成功後に指定どおりclaude/3.0.0/t07-lifecycleへ同SHAをpush、draft PR54を英語で作成。Codex現SHA承認comment5649811216を投稿。msg_ef1cf2c2a713でClaudeへPRを通知し、重複作成を避けた。CI・統合はこの時点で未完了。PR53のnon-blocking nitだったT07台帳の古い再開blocked表示を今回訂正した。

## 最終候補のコメント修正

Claude msg_6477ff749aca / msg_ec0265de8f3eでc13acee8884bb3426142bf2e53c6f0ff542d4503を受領。a39813aとの差分はtestコメント2行のみで、削除済みメンバへの参照を訂正するもの。連絡が交差したため新しいPRを増やさずPR54をfast-forwardした。指定の5 Gradle tasksを最終HEADで確認しBUILD SUCCESSFUL（2秒、135 tasksのうち4 executed/131 up-to-date）。40 testsの成功XMLは前回から有効な結果として再利用され、テストを新たに全件再実行したとは扱わない。CIは新HEADで確認する。PR本文とタイトルを最終状態へ更新した。

## T07統合とT10開始指示（10:04 JST）

PR54はhead c13acee8884bb3426142bf2e53c6f0ff542d4503、Codex承認5649833960、Claude承認5649837582（msg_6477ff749acaの依頼文をCI成功後に代理投稿）、CI34729426721 SUCCESSを確認。ready化後、MERGEABLE/CLEANと同SHA・base work/3.0.0を再確認し通常merge。統合SHAは6c9e2aa120bb85f3b49fd9d9f2da43288e6f8e89、10:04:16 JST。共有checkoutもfast-forwardした。

msg_1ecbcd072166でClaudeへ統合を通知し、既存C1/K1–K7の下でT10a/sampleとT10b/docsを直接作業するよう依頼。具体的worktree/branchの開始記録は受領待ち。Kotlin組み込み方式・sample/assets同一所有・版数未変更・T04cはT10後・専用AVDによるsmokeとスクリーンショット・無関係なemulator5554は触らない制約を再掲した。

追加の代理許可: delivery_3f500779b858のack/受信待ち/JSON整形コマンド、およびcore公開APIの指定ファイルをgrep表示するだけのコマンドを全体確認してraw Enterで許可。worker-releaseや設定緩和は含まない。PR55（head6addac4、CI34729477343 SUCCESS）はClaude承認待ちで、headを固定して以後の記録をcoordination-wave18へ分離した。

## T10分担の確定と文書初稿

PR55はhead6addac4・Claude承認5649867149（msg_2e289c029277の依頼文を代理投稿）・CI34729477343 SUCCESS・MERGEABLE/CLEAN確認後、02a6debe188c4fcb130f41ea31d3cffa8b41c53eへ通常merge（10:10:44 JST）。共有checkoutを同SHAへfast-forward。PR55承認文送信/受信待ち、およびgit rev-parse/log + 受信待ちの表示コマンドは全体確認後に代理許可した。

待ち時間を減らすためmsg_ad81e86d0001でT10の直接分担を提案。msg_b3e18d2f8544で双方が明示合意し、ClaudeはT10b未編集と確認した。

- Claude Fable5.1/high: claude-3.0.0-t10a-sample、branch codex/claude-3.0.0-t10a-sample、base02a6deb。sample/**（assets/androidTest含む）と専用AVD smokeを所有。ローカル検証が制限される場合はCodexが指定コマンドを実行。
- Codex Astra/medium: codex-3.0.0-t10b-docs、branch codex/codex-3.0.0-t10b-docs、base6c9e2aa。README.md、CHANGELOG.md、docs/migration/**のみ。Orcaの専用worktreeで親が直接作業、新規サブエージェントなし。
- 相互レビューを維持し、sampleのコンパイル済みコード例と文書を照合後に受け入れる。T04cは両方の統合後。

文書初稿d90b0dcbef136b50855ebca923cb2016eed0924bを保存。新API・結果・両scopeの所有・権限復帰・破棄失敗のretry・threading・Timber・未リリース状態を記述。ローカルリンクとMarkdown fence、git diff --check成功。文書のみでGradle未実行。msg_dc55cc182244でClaudeへレビュー依頼、コンパイル済みsampleとの最終照合は未完了と明記した。

## T10初回候補の差戻し

T10bはdraft PR56（初稿d90b0dc、後続b4059d1で旧outside-screenの非互換を2行補足）として共有。コンパイル済みsampleとの照合とClaudeレビューは未完了。

sampleの文書照合で、Sample2Service.onDestroyが失敗をlogするだけではService破棄後のretry所有者が残らない点を発見。msg_d87cb8640080でK3要件違反として差戻し、Application等に生き残るownerと明示retry手段を求めた。先の「Service追加不要」注記は既存nested Sample2Serviceを見落としていたため、同連絡で訂正した。

Claude msg_92e34ec5d478の初回候補13130407b798e3a9d566aed7a7df4ba8ba46cd03をtracked cleanで確認し、指定6タスクを実行。7秒で失敗。sample Kotlin compile成功、Java compileはSampleAllOptionsActivity.java:17の重複package宣言で失敗。sampleのunit/lint/androidTest APKは未完了。msg_83e9c6ee87b1で結果とログ/private/tmp/overlay-t10a-validation.logを引継ぎ、ファイルを編集せず検証専有を解除した。Claudeのack/受信待ち/JSON整形のみのコマンドを全体確認して代理許可し、修正担当を変更していない。

## T10コンパイル再検証と残る動作確認

998a625はSample2Activity.java:52/62でbtn_retry_disposeが未解決となり3秒で失敗。Claude msg_baff95f5ee9cでlayout/string/HTML生成の途中失敗が報告され、3627afcを受領。6タスクの再実行は6秒で成功。sample本体・androidTest APK・core/Timber build成功、ExampleUnitTest 1件成功（failure/error/skip0）、sample lintエラー0・警告58。端末テストは未実行。

controllerへhandleを移したK3修正は方向として受け入れたが、500ms後の一度だけのUI更新では遅いService破棄時にretryボタンが出ない。msg_8422d358279bでタイミング依存の除去と、破棄失敗→同じhandle保持→明示retry成功を検証する回帰テストを依頼。msg_48171e4de1d9でコンパイル成功と検証専有解除を連絡した。

ClaudeからT10b初稿のblockingなしレビューを受領したが、最終文書はsample具体例へのリンクとJavaのre-check-onlyフローの区別を反映し、新SHAで対向承認を取り直す。旧d90承認を新headへ流用しない。

端末準備: 既存emulator5554は観測のみ。5580/5581が空きであることを確認し、OverlayViewManager_3_0_API_36をport5580、no-window/no-snapshot-load/no-snapshot-save/no-audioで起動（session92056）。Claudeに重複起動しないようmsg_951e0313a765で通知。起動は検証成功の証拠ではなく、T12を完了扱いしない。追加のClaude ack/受信待ちダイアログは全体確認の上代理許可した。

## T10最終候補の通知受領と端末検証（10:43 JST）

msg_0516130fe2a4 / delivery_7167e17bc526を読み、T10a最終候補665d16989f8ef98ba2b77ee56d5cf2f1fd54d7d3をtracked cleanで確認。msg_bd0ff8dc522fで検証担当と編集凍結を通知後にackした。差分はlistenerによるretry UI更新とcontroller回帰テスト3件、Robolectric設定追加。6 Gradle tasksは5秒で成功、単体テストは既存1件と新規3件の全4件成功（failure/error/skip 0）。lintはエラー0・警告59。

専用emulator-5580のみでinstallDebug成功、SYSTEM_ALERT_WINDOW allow設定後にANDROID_SERIALを限定してconnectedDebugAndroidTest実行。30秒で失敗し、XMLは3件中2件失敗、error/skip 0。Sample1ActivityTest:91とSampleAllOptionsActivityTest:149でATTACHEDを期待したがCONFIGURED。SampleStartActivityTestは成功。原因未確定で承認・PR作成・統合を保留。msg_608d07b348b8でClaudeへ修正を依頼し、Gradle専有を解除した。テストログは/private/tmp/overlay-t10a-connected-665d169.log、端末ログは/private/tmp/overlay-t10a-logcat-665d169.txt。5554は操作していない。

T10b PR56の最新headは1ca27f344d42626ed28d648cb00a67af39bd4665。具体的sampleリンクとJava側の権限recheck-only動作を明確化し、msg_742ffa130794で差分レビューを依頼済み。リンク検査はdocsとsample候補の組合せで成功したが、sample統合後の再検査と最新SHAの対向承認は未完了。

## T10診断と文書承認（12:09 JST）

msg_72c84115cab7 / delivery_0f90c6dc2946を処理。PR56のhead1ca27f344d42626ed28d648cb00a67af39bd4665に対するClaude承認を、GitHubの現SHA一致確認後に依頼文どおりcomment5650579496へ代理投稿。sample統合・リンク再検証までmergeしない。

診断候補8030fca0611a548ec413e4e802619fc242bd943eはassertionへのlastFailure表示追加のみ。専用5580でconnectedDebugAndroidTestだけを再実行し、3件中2件失敗、errors/skips 0。Sample1:92とAllOptions:149はいずれもlastFailure=null、期待ATTACHEDに対してCONFIGURED。ログ/private/tmp/overlay-t10a-connected-8030fca.log。

手動確認ではテスト後にsampleが存在しなかったため既存APKを再インストール。Sample1は非exportedで直接am startできず、公開SampleStartActivityからUIで遷移した。UI階層はSHOW / HIDEが[0,0][1080,126]、Activity見出しが[42,101][804,172]。スクリーンショット/private/tmp/overlay-t10a-smoke/sample1-manual-665d169.pngは青いバーと白い本文だけで、表示ボタンは隠れていた。指定中心へのtapで赤いoverlayは現れず。ホストのedge-to-edge/insetsにより操作が遮られた可能性が高いが、ライブラリshow失敗と断定しない。msg_037346462141で証拠と修正依頼をClaudeへ渡し、Gradle専有を解除した。通知をack。sample修正・再検証は未完了。

## T10レイアウト実測の引継ぎ

msg_8b9d88647fa2 / delivery_3053526e4125の追加診断依頼を処理。専用5580で再インストールしたsampleの公開入口からSample1へ遷移し、/private/tmp/overlay-t10a-smoke/sample1-ui.xmlとwindow-displays.txtへ保存。android:id/contentと直下LinearLayoutは[0,0][1080,1920]、buttonは[0,0][1080,126]、webViewは[0,126][1080,147]。action_bar_containerは[0,0][1080,210]、action_barは[0,63][1080,210]で、ボタン全体と重なる。画面1080x1920・density420、statusBarsは高さ63、navigationBarsはy1857〜1920、cutoutなし。実測値とファイルをClaudeへ送信し、sample修正担当を維持。Codexはソース未変更、追加テスト未実行。通知をackし、修正候補の返却待ち。

## T10 Insets修正の成功とHTML表示の差戻し（12:23 JST）

msg_c59c4fe58bea / delivery_894075371929のa99620f8b73caa54f6616fe01f7c06b9d5c031c0を検証。差分は6画面のfitsSystemWindows追加のみ、tracked clean。6 Gradle tasksは4秒で成功、unit成功XML4件（failure/error/skip0）、lintエラー0・警告59。5580のconnectedテストは3件すべて成功（XML time111.925s、failure/error/skip0）。ログは/private/tmp/overlay-t10a-validation-a99620f.logとoverlay-t10a-connected-a99620f.log。

手動確認で権限Allowed、Sample1赤いclick:0、Sample2アイコン表示→停止で消失・retryなし、AllOptions ATTACHEDを画像で確認。証拠/private/tmp/overlay-t10a-smoke/a99620f-*.pngを保存。ボタンはy210以降へ移りバーとの重なりを解消した。一方Sample1/2のコード欄が空白で、assetsの4HTMLすべてに<style>はあるが</style>がないことを確認。msg_0d8cb4294149で具体的修正とコード例の表示確認をClaudeへ依頼。専有解除、候補承認・PR作成・mergeは保留。画像はローカルのみでPR添付済みと扱わない。通知をackした。

## T10 HTML表示修正の確認と画像引継ぎ

msg_7c0b2038baf3 / delivery_b4b6c98c1546の8185320a4f415d7996080c211fb9817cf01351e7を検証。差分は4HTMLへの閉じstyleタグ追加だけ。最初のGradle実行は自動承認レビュー時間切れで未起動、明示許可された1回の再試行は成功。6 tasksは3秒（9 executed/156 up-to-date）。

専用5580へ修正APKを再インストールし、Sample1/2/Timber/brightnessで説明と色付けされたコード例を画像で確認。7画面の証拠を/private/tmp/overlay-t10a-smoke/8185320-{start,sample1,sample2-start,sample2-stop,all-options,timber,brightness}.pngとXMLへ保存してClaudeに引継ぎ。sample/screenshots/3.0.0/への最終画像コミットはClaude担当、既存untracked画像をCodexは変更していない。HTML/PNGだけの後続差分ならconnected再実行は不要とし、a99620fの3件成功と8185320の手動表示確認を区別して記載する。最終SHAとPRは未完了。通知をackした。

## T10a最終候補のPR57作成（12:32 JST）

msg_03e9bf655ad9の52caec4277b79153ff76fce840197c9c947c4cd5を確認。8185320との差分は7PNGだけ。最終6 Gradle tasks成功（819ms、1 executed/164 up-to-date）。指定remote branchへ同SHAをpushし、英語draft PR57を作成。本文に不変SHAの画像リンクと検証実施SHAを明記した。Codex対向承認comment5650802093を投稿、CI34735697110は実行中。Claudeの依頼文の代理投稿とmergeはCI成功後。msg_038444f2dc9bで現状を通知し、delivery_803b9896bcc8をackした。PR56は承認SHA1ca27f3のCI34730952948成功、sample統合・リンク再確認待ち。

## T10統合完了とT04c開始依頼（13:16 JST）

ユーザーの継続指示後、PR57のhead52caec4/base work/3.0.0/CI34735697110 SUCCESS/CLEANを確認。Claude依頼文をcomment5651082255へ代理投稿し、Codex対向承認5650802093と合わせてready化・通常merge。統合SHA c298798789c0b9788e90ac732f5b11b2e5fcae8e（13:14:50 JST）。

共有workをfast-forwardし、文書PR56のローカルリンクを統合済みsampleと提案文書へ照合して成功。現head1ca27f3・Claude承認5650579496・CI34730952948 SUCCESSを確認し、本文のdraft依存記述を検証済み状態へ改訂。ready化後に通常mergeし、97080616f4ccc4b796799fba5b8998c4283e4d5dへ統合（13:16:13 JST）。main/tag/publishは未実行。

T10を完了扱いとし、ClaudeへT04cの開始を依頼。最新workから新しい隔離worktree、暫定2.x API除去、JVM可視性監査、Java/Kotlin consumer検証、影響範囲のcore/sample/Timber/lint確認が対象。具体的branch/worktree/検証計画は返答待ち。共有plan/logはCodex所有、lint stage2を無断で混在させない。T12の全端末matrixは引き続きrelease-blocking。

## Claude受領未確認のチェックポイント

計画・ログのdraft PR58をhead48c1da0で共有し、msg_a623a8fdc53aで対向レビューを依頼。Orca受信箱は空。term_57cc4804-a813-4bd9-a03b-f8a7fc9c92b3のterminal readはstatus exited、tailなしを返した。端末一覧には以前のClaude端末が残るが、別の監視端末にも実処理停止済みとの表示があり、稼働する司令塔やT04c受領を確認できない。メッセージのenqueueを着手確認と誤認しない。T04cとPR58レビューは返答待ち、旧端末を無断で再利用・停止していない。PR58のレビュー対象SHAは変更せず、本記録をwave19へ分離した。

## Claude司令塔の復旧とmediumへの変更

ユーザーが司令塔起動と継続を明示指示し、続いてhighではなくmediumへ訂正。新端末term_66423650-ee22-4e09-a6ba-65ed19561512を作成。最初の起動文字列はモデル名の角括弧がzsh glob解釈され未起動だったため、同端末で引用を修正してclaude --model 'claude-fable-5-1[1m]' --effort mediumを実行。画面にFable5.1 with medium effortを確認。権限回避オプションは追加していない。

統合済みT07/T10、PR58の現SHAレビュー、T04cの隔離worktree・API整理・検証計画、所有境界、main/tag/publish禁止を引き継いだ。request62df7078-6f04-417e-bce6-2e82485896d7はinput_acceptedとturn_startedを確認。新しい調整Runと作業受領の返信は待ち。最新のmedium指示は古いhigh記述を上書きする。

## 復旧司令塔の受領確認

msg_c27a6649eba8 / delivery_7b155cb10022で、新端末term_66423650が既存run_3ae778449744へrun-useで再結合（consumer_generation5）したとの報告を受領。Fable5.1/medium、PR58レビューとT04c inventory開始を明示受諾した。PR58の対象SHAに関する質問には48c1da0ea08cf2c622215b3d93ec089b42ef32c0のままと回答。後続wave19のb2445ee/16bac48は意図的に分離しており、レビュー対象を変更しない。T04cの具体的worktree/branch/検証計画は返答待ち。通知をackした。

## PR58統合とT04cの範囲合意（13:47 JST）

msg_8e3b6f207facでPR58のhead48c1da0ea08cf2c622215b3d93ec089b42ef32c0へのClaude承認を受領。指定文をcomment5651211614へ代理投稿し、CI34737580394 SUCCESS確認後、通常mergeでb4dff8f27b20aefb7e192b1e1518d0af0235eef1へ統合。共有workもfast-forward。T10行の「作業中。完了」のnon-blocking nitを次の計画更新で訂正する。

msg_5ec55fdf68e9でT04c開始報告を受領し、msg_7c6eaee7c67eで合意。worktree /Users/ai-seb/orca/workspaces/Android-OverlayViewManager/claude-3.0.0-t04c-api-cleanup、branch codex/claude-3.0.0-t04c-api-cleanup、base97080616。Claude Fable5.1/mediumが直接作業。対象はOverlayViewの18旧bridgeとpending/custom listener管理、managerの旧権限/表示サイズAPI、旧permission dialogとstrings/fragment依存、drag内部旧API呼出し。private constructorとfactory、JvmSyntheticを含むJVM可視性監査。内部public/RestrictToテスト接点の残存理由を明記し、syntheticをアクセス制御と説明しない。意味のある旧テストの振る舞いは新APIで維持する。

予定検証はcore assemble/unit/lint/androidTest APK、Timber assemble/unit、sample assemble/unit/lint、lint:testとjavap。lint stage2の新API対応はT04cに混ぜず、移行文書の暫定bridge説明は統合後Codexが変更する。T12はAPI23/26/35/36、別UID透過、両scope/非ゼロ原点/RTL/edge-to-edge/画面外配置でDOWN時no-jumpと実画面移動量の未完了検証を維持する。開始報告をack、候補SHA・検証結果待ち。
