# 3.0.0 最終検証・リリースPR

## 目的・担当の合意

2026-10-31 JST、ユーザーはCodexとClaude Codeへ、相談して妥当な案を選び、品質を落とさず過度に複雑にせずmain向けPR作成まで進めるよう指示した。mainへのマージ・タグ・正式配布は実行しない。配布先は既定のJitPackを維持する。

ClaudeはPR63およびplan/logの所有権をCodexへ移管することに画面上で明示合意。Codexは計画・ログ・PR63更新、Gradle/SDK/端末/consumer検証、相互承認後の統合とrelease PRを担当する。ClaudeはPR64、追加端末テスト、公開API・成果物・利用文書の対向レビューを担当する。新規サブエージェントは起動しない。

Orca runtime `52f3b92b-a534-4d1e-a1db-d0e22a749584`。Codex `term_cf3261ec-1c55-4dd7-b66a-bcd6fe532734`、Claude `term_de4fd3ba-d1a1-458a-8184-a64c4403a379`。Codex Run `run_18f7185e91aa` を現在端末へ再結合（generation 4）。分担提案 `msg_dd0ffaa4dd2f`、端末送信receipt `5b60386f-80bc-4065-bab6-641be797c60f`、受け入れ条件追加 `d13dc305-a9e4-4a22-8fdc-1c1d095d6d11`。Claudeのsandboxから現Orca socketへ接続できないため、受諾は端末の実際の返信で確認し、enqueueだけを合意としない。

## PR64・PR63の整合

Claudeが既存Codex記録 `49622cc` と `3e05597` をPR64へ取り込み、head `9974c488e3afa4462eeb675ba04f16ed27052c0f` をpush。Codexはtreeが `3e05597` と同一で、CI `41189303564` が成功したことを確認した。製品・端末テストは前回検証候補から変更されていない。Claudeの記録承認[6541798467](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6541798467)とCodexのコード承認[6541804856](https://github.com/75py/Android-OverlayViewManager/pull/64#issuecomment-6541804856)、現head CI成功・base・CLEANを確認し、2026-10-31 18:40:54 JSTに通常mergeで `5c8219b500291230eb423e3ba961e52663cc9df4` へ統合した。PR63の古いSettings検証と「次はT13」の断定を訂正し、10月18日の採用経緯は保持する。

## 実行設定

既存のClaude Codeは本人報告で `claude-opus-5-5`（Opus 5.5）。本人が観測できるeffort内部値は40、端末画面はhigh表示。旧計画のFable 5.1指定とは区別する。今回ユーザーが既存Claude Codeへ同じ連携依頼を出しているため、そのセッションを維持しモデル設定を変更していない。Codexも既存セッションを継続し、追加workerは起動していない。

## 現在の検証結果

- `3e05597`（PR64 headとtree同一）でcore/opt-timber/sampleのDebug・Release assemble、4 module unit tests、3 module lint、core/sample androidTest APK compileを一括実行し成功。
- unit tests: core 107、opt-timber 40、lint 12、sample 4、計163件。failure/error/skip 0。
- lint: エラー0。警告 core 9、opt-timber 4、sample 61。警告内訳と最終差分は後続チェックで照合する。
- API35専用AVDでcore 2/2・sample 9/9成功（skipなし、PR64を含む全既存suite）。`/private/tmp/ovm-release-api35-connected.log` と `ovm-release-evidence-20261031/api35-pr64` に結果を保存。
- API23専用AVDでsample 6/6成功。core旧権限試験はSDK条件により対象0件（成功件数として加算しない）。追加DragGeometryTestでAPI23の拒否/許可とActivity権限不要を別途検証する。
- Release成果物を `-Dmaven.repo.local=/private/tmp/ovm-release-m2-20261031` 付きpublishToMavenLocalで一時repositoryへ生成。core/opt-timberとも3.0.0、sourcesあり、AARにconsumer rulesあり、coreにlint.jarあり、POMにApache License Version 2.0を確認。旧fragment依存なし。Timberは意図どおりhostの明示依存、opt-timberからcoreはruntime依存なので利用例では両artifactを宣言する。
- 別Gradleプロジェクト `/private/tmp/ovm-release-consumer-20261031` に上記2成果物とTimberを宣言し、Java/Kotlin双方で公開APIを使用。`:app:assembleRelease :app:lintRelease` が成功しR8のminifyReleaseWithR8を通過。core/opt-timberのソースprojectへの依存はない。同梱lintはJavaのtrue引数1箇所だけを検出しKotlinのfalse引数は検出せず。manifestのActivityから呼び出し、R8が利用箇所を丸ごと捨てるだけのfixtureにはしていない。
- lint警告は対応環境固定に伴う新SDK/依存更新案、既存サンプルのローカライズ/RTL/アイコン、明示した画面外配置例等。coreのClickableViewAccessibility 2件は汎用Viewへのlistener設定/除去箇所で、クリック/drag分離は回帰試験対象。sampleのJavaScriptはローカルassetsのコード表示用途（既存T10レビュー済み）。新しい全面抑制やbaselineは追加しない。
- buildログ: `/private/tmp/ovm-release-validation-build.log`。この一時パスだけを永続証跡とは扱わず、検証SHAと結果を本記録へ残す。

## 中間チェックポイント時点の受け入れ条件（最終状態は末尾）

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

## ドラッグ端末試験で見つかった不具合と修正合意

Claudeが追加した初期候補 `46cc42f` のAPI35試験は14件中7失敗/1skip。画面外配置では `getWindowVisibleDisplayFrame` が約±100000のplaceholderを返し、ドラッグで大きく飛ぶ。RTLでは未attachのViewのlayoutDirectionが未解決のため、初回STARTを左に解決していた。別の小さな移動量超過は、診断候補 `e934535` の注入/受信ログでAndroidのresamplingによる外挿を確認（18ms間隔で注入raw x=252、受信234.01562）。animation scaleを0にしても再現し、単純な描画待ちの問題ではなかった。

Claudeは全経路をspec差分方式へ置換する案Aを提案。Codexは初期クランプ後の新gestureに不感帯が生まれる回帰を指摘し、boundedの実位置rebasing維持を求めた。両者は案D（bounded既存維持、allowOutsideBoundsだけgravity/marginsを保持する差分方式、RIGHT/BOTTOMでは差分反転）に収束した。試験入力は40ms間隔へ変更し、製品側のraw座標処理はresampling対策で変更しない。

API35のsplit操作は利用できなかったが、Codexが専用AVDで `enable_freeform_support=1` と `am start --windowingMode 5` により、非ゼロ原点のfreeform（bounds 215,420–865,1500）を確認した。ClaudeがActivity scopeのbounded/画面外2試験を追加。freeformにならなければskip、なったのに原点ゼロならfailとする。設定はCodexが検証終了時に元のnullへ復元する。animation scaleの元値も一時JSONに保存して復元する。

候補 `39d4fb07ab511dc603298e791f74e7d2fb8f604d` はテスト変更 `f9deb0c` と製品修正を分離。core unit/lint・lint unit・API35の追加22件を検証中。最終承認・PR・統合は未完了。

API26の既存suiteはcore 2/2、sample 6/6成功（PR64系はSDK条件対象外）。fingerprintは `google/sdk_gphone_arm64/generic_arm64:8.0.0/OSR1.180418.028.A3/10734686:userdebug/dev-keys`。一時証拠 `ovm-release-evidence-20261031/api26-pr64` に保存した。

### PR65の差分レビューと補正

- `39d4fb0`: core115/lint12 unit成功、core lintエラー0。API35追加22件は21成功/1失敗/skip0。bounded freeformの失敗は、テストがwindow幅の中心とscreen座標を比較してframe外へ動かしたため。Claudeがscreen上のframe中心へ判定を修正した。
- CodexがRTL未対応hostとXML明示LTRの上書きを指摘。Claudeは `FLAG_SUPPORTS_RTL` を尊重し、初回layoutで解決した方向を1回補正する方式を追加。show前measure案はhostのonMeasureを先に呼ぶ副作用、補正省略案はXML方向の欠落があるため不採用。
- 初期補正案はlistener残留と失敗握り潰しがあったためCodexが差し戻し。`4fface2925606b5997a76c569cdfaa02b33464d6` は既存hide/external detach/releaseの解除経路へlistener解除を統合し、補正失敗をlastFailureへ記録する。hide/dispose/再show/外部detach/拒否/not-attachedの回帰を追加。全build/unit/lint/API35全suiteで検証中。
- 続けてCodexはshow直後update前後のgravity比較と、同じlayoutイベント中にhostがdisposeする再入条件を指摘。最後に適用したgravityとの比較と状態guardを補う次候補をClaudeが担当する。
- ActivityTestRuleの再生成後getActivity懸念は、実使用のrules 1.7.0 AARのLifecycleCallbackをjavapで確認し解消。Stage.RESUMED時にWeakReferenceを新Activityへ更新するため、既存の再生成試験は古いinstanceを検証していない。

### 最終製品候補 `1bfde2f47128f3ed79eac7c119359b19dd94828b`

`4fface2` のAPI35全suiteはcore 2/2・sample 31/31成功、skipなし。追加22条件はすべて成功し、残りは既存sample試験。全Debug/Release buildとlintも成功。unitはcore124/Timber40/lint12/sample4（計180）成功。このAPI35実行は診断時のanimation scale=0を維持していたため、元設定（window/transition=1.0、animator=null）へ戻した上で次候補を再検証する。

`1bfde2f` は最後に成功したbackend gravityとの比較とlayout中のhide/dispose guard、3回帰試験を追加した。Codexは差分を再レビューし、指摘箇所の修正を確認。現候補のunitはcore127/Timber40/lint12/sample4（計183）成功、failure/error/skipなし。lintはエラー0、警告core9/Timber4/sample61。CI [41191255934](https://github.com/75py/Android-OverlayViewManager/actions/runs/41191255934)成功。

同一ビルドAPKをAPI別の専用AVDへ配り、端末テストを実行する。APKのSHA256:

- core androidTest: `8ef39fd0dcabd5b31efc1abcf0e672255b7e1b1f68c892475d106c77742e36e7`
- sample debug: `cc2382bcc542999e7521f34255adb14cb25688e3ad27d9a97bbc353b91004e6f`
- sample androidTest: `a8b892c1724b02af2252bf0f2572cbcb5f037c1e5d3d10440a8a4a954dd549ac`

API36のGradle全suiteと、独立したAPI35/26端末の直接instrumentationは同じAPKを使う。API35は元のanimation設定、freeform有効。API36のfreeform元値はnullで、検証後に戻す。直接実行は `adb -s SERIAL install -r -t APK`、`adb -s SERIAL shell am instrument -w -r [ -e class CLASS ] PACKAGE/androidx.test.runner.AndroidJUnitRunner`。成功は終了コードだけでなく、instrumentationのテスト完了件数、failure/error/skip、`OK`を確認する。

Claudeの最終レビューはAPI、移行ガイド、状態遷移、配布文書を対象にblockingなし。Timber dispose後の古い参照によるregisterは呼び出し誤りとして例外になり、今回の公開契約変更は行わない。テスト後の文書更新と現head承認は未完了。

最終候補のAPI35 DragGeometry22/22が元のanimation設定で成功（skip0）。freeform設定は元のnullへ復元し専用AVDを終了した。[12秒の操作録画](evidence/2026-10-31-drag-api35.mp4)はこの実行の抜粋で、全条件の代替証拠ではない。動画のデコードとフレーム内容を確認済み。

最終候補のAPI36全suiteはcore 2/2・sample 31/31成功、skip0。API26はcore 2/2・sample 26/26成功。API23/26ではfreeform2件（minSdk28）と別UID3件（minSdk31）がSDK条件で対象外で、26件は実行された試験数。API36のfreeform設定も元のnullへ戻し専用AVDを終了した。

最新AARの再生成後、独立consumerをclean・refresh-dependenciesしてJava/KotlinのRelease・R8・lintを再実行し成功（46 tasks、32秒）。lintは意図したJavaのtrue引数だけ `ALLOW_OUTSIDE_BOUNDS` を1件検出し、falseを指定したKotlin側は検出しない。その他3件はfixtureのicon/文字列/resource shrinkingに関する警告でエラーなし。

最終Release成果物のSHA256:

| 成果物 | SHA256 |
| --- | --- |
| core AAR | `799c8d08d0dbeb4c3b26801e4a09ac082a0256ba6ea5aa3906f404cf8a2b15ef` |
| core POM | `dc5b9b513c68afd7e9fec497fc62c96b0d8c6f50b2d4125404cae1da5d06077f` |
| core sources | `8718a51ae2ce00abe592b031dd6b1b4c3156dc0bacf8e1fc470934595779dac9` |
| Timber AAR | `6698e7f985f552663e9c2fa4a935d8308977ae481d6938b414fcc4003f5fec6e` |
| Timber POM | `185a709cfbcc48465a285bd753c41853d1f86389374b5a7c251992ddd1e6097e` |
| Timber sources | `e371b0405f873710a09ef0b93b59d3c4e7fd5da0104ce7151d5e19ab162bfb76` |

AAR内のclasses.jar/consumer rules、coreのlint.jar、sourcesとPOMを再確認。実行はrepositoryのGradle wrapperへ `-p /private/tmp/ovm-release-consumer-20261031 :app:clean :app:assembleRelease :app:lintRelease --refresh-dependencies` を指定した。一時consumer自身にwrapperはないため、最初の `./gradlew` は実行前に失敗し、正しいwrapper指定で実施した。

### API23/31で残った試験条件の問題

`1bfde2f`のAPI23最終結果はsample6成功/20失敗、API31はcore2成功・sample29成功/2失敗。途中のAPI23進捗を成功と誤読したCodexの端末連絡は撤回し、Claudeの文書完了/pushを保留した。API26/35/36の最終成功集計には影響しない。

API23は全dragで2〜9pxの移動方向への超過があり、40ms間隔だけでは不十分だった。製品frameは正常で、入力補間の影響を受信ログで確認する。CodexはMOVE後に同一座標のstationary MOVEを送って速度をゼロにしてから位置をassertする案を提案し、Claudeが調査する。期待位置/toleranceは緩めない。

API31はfreeform2件のみorigin=[0,0]で失敗。`dumpsys activity`ではmode=freeformなのにtask boundsが空で全画面になっていた。Codexが `am task resize 47 215 420 865 1500` を試し、指定された非ゼロboundsへ移動できることを確認。Claudeはテスト自身のtask IDとdisplay範囲からboundsを指定し、再生成後のRESUMED hostを取得する方式を検討する。手動検証sampleはforce-stopして次の試験から分離した。


### テスト入力・freeform設定の再検証

候補 `2ac46f2d49d941d33822d528d392c0344df97be3` は製品コードを変えず、stationary MOVEとfreeform taskの明示resize/RESUMED Activity再取得を追加。API23のDrag20/20が成功（failure/error/skip 0）。受信ログで、注入252,441に対して初回受信258.43478,447.43478、stationary後252,441へ収束することを確認した。位置の期待値と許容差は変更していない。API35は通常・RTL画面外・freeform2件の代表4/4成功、skip0。

API31はDrag20成功/2失敗。freeformの非ゼロ原点216,384は成立したが、DOWN/MOVEを一切受信していなかった。試験中の`dumpsys input`ではhost/overlayともvisible=false、focus request NOT_VISIBLE。一方WindowManagerではSurface shown=true/HAS_DRAWN/isVisible=true、ActivityもRESUMED/visible=trueだった。座標計算の失敗と断定せず、専用API31のfreeform環境を再起動して切り分ける。生ログは`/private/tmp/ovm-api31-freeform-diagnostic`と`ovm-api31-freeform-window`に保存。

Claudeの上記診断ファイルのgrep/sed/cut/head/tailによる読取要求を全文確認し、代理許可した（receipt `2544eb41-1f97-41ed-88aa-5e683daf2b9a`）。初回Enterは自動承認レビューに現在入力不明として拒否されたため送信されず、同じ許可画面を直前に再確認して具体的なコマンドと読取限定の効果を示した再審査で許可された。恒久設定は変更していない。


### 最終端末集計と判定

`c6bbe69ea5f293fb909f6d7aec93a1da4cda34cd` はfreeform hostが入力システムで表示されることを待ち、前提不成立ならその2件をassumption skipにする。overlayを作る前のhost条件を検査するため、overlayの移動失敗をskipへ変更するものではない。compile成功。`4b36c7db5928b5fd49696cc22a5ab872cac43c27` はコメントだけの補正で、検証対象の実行コードと同一。

| API | 実行結果・検証ソース | SDK対象外・未検証 |
| --- | --- | --- |
| 23 | 既存sample6件成功（PR64製品）、Drag20件成功（2ac46f2）。最終製品のDrag以外6件は1bfde2fでも成功 | core旧権限試験0件、freeform2件と別UID3件はSDK条件対象外 |
| 26 | core2・sample26件成功（1bfde2f）。最終入力方式の通常/RTL画面外2件再確認成功（c6bbe69） | freeform2件と別UID3件はSDK条件対象外 |
| 31 | core2・sampleの非freeform29件成功（1bfde2f）。最終Drag20成功/2skip（c6bbe69）。別UID3件も1bfde2fとPR64の初期/更新imageで成功 | freeform2件はhostが入力上非表示のため未検証。成功数に含めない |
| 35 | core2・sample31件成功（4fface2）。製品の最後のguard修正後Drag22件成功（1bfde2f、animation元値）。最終入力方式/前提条件の代表4件成功（c6bbe69、freeform2件含む） | skipなし |
| 36 | core2・sample31件成功（1bfde2f）。最終入力方式/前提条件の代表4件成功（c6bbe69、freeform2件含む） | skipなし |

API31の再起動後に旧2ac46f2を再実行するとfreeform1成功/1失敗となり、不安定な環境前提と判断。最新c6bbe69では20成功/2skip/失敗0で確定した。合意済み必須matrixはAPI23/26/35/36であり、API31はAndroid12の別UID境界の追加確認。マルチウィンドウの受け入れ証拠はAPI35/36の非ゼロ原点freeformで、API31のskipで代用しない。両司令塔がこの区別に合意した（端末送信request `84640881-eb20-4cb5-81ea-0f8f95bb5360` とClaudeの返信）。

API35のc6bbe69は最初のフィルタ指定でRTLメソッド名を誤り3件だけ実行されたため、正しい`applicationScope_allowOutsideBounds_rtl`を単独実行して1件成功を補った。API26も同様に通常1件+RTL1件として集計した。存在しないメソッドが暗黙除外されるrunner挙動を、成功件数確認により検出した。重複を加算していない。

最終test APK SHA256は `42887ca59037029678dff346b1d634b8517861725622c6c385548c11b4ed0d92`、同ビルドsample APKは `78cd38d946e1bf2cc19fccb08ac01819014ba597f06ad149e8d7ae0c8d435906`。2ac46f2以降はtest/doc変更のみで製品コードは1bfde2fと同一。追加リリースビルド・全suite再実行は不要と両者で判断し、影響する入力/前提条件だけを再検証した。

API31/35/36のfreeform設定を元のnullへ復元し、専用API23/26/31/35/36エミュレータを終了した。無関係なemulator5554は操作していない。

既知の制約はREADME/移行ガイドへ記載: XMLで指定した方向が構成と異なると初回layoutまで推定側に表示され得ること、allowOutsideBoundsのfill gravityが移動でなくサイズを変えること、lintがBuilder呼出しのみを検査すること、Android側のopacity補正と別UIDタッチポリシー、JitPackタグ後のサービス上ビルド確認。これらを隠さず、未解決の必須指摘なしで最終承認へ進む。

使用した最終matrixのOS fingerprint:

| API | fingerprint |
| --- | --- |
| 23 | `Android/sdk_google_phone_arm64/generic_arm64:6.0/MASTER/8690019:userdebug/test-keys` |
| 26 | `google/sdk_gphone_arm64/generic_arm64:8.0.0/OSR1.180418.028.A3/10734686:userdebug/dev-keys` |
| 31 | `Android/sdk_phone64_arm64/emulator64_arm64:12/SE1A.220621.001/8752307:userdebug/test-keys` |
| 35 | `google/sdk_gphone64_arm64/emu64a:15/AE3A.240806.043/12960925:userdebug/dev-keys` |
| 36 | `google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys` |

### PR65統合・releaseへの最終ゲート

PR65 head `ca3f7e0809a966e842d19ba7ea7d102a8ef48b86` の最終文書差分と検証説明を確認。API23行の単一SHA表記は実際の6+20件のソースへ訂正するよう依頼し、Codex承認にも正確な組合せを明記した。[対向承認6542296295](https://github.com/75py/Android-OverlayViewManager/pull/65#issuecomment-6542296295)、[CI41193177729成功](https://github.com/75py/Android-OverlayViewManager/actions/runs/41193177729)、base work/3.0.0、同一head、CLEANを再確認。`--match-head-commit`付き通常mergeで19:35:55 JSTに `497a5e2a6348ffbadd9455813d9bcf388012ebcc` へ統合した。

残る作業は本台帳PR63の現head対向承認/CI/統合と、合意済み統合SHAからのrelease/3.0.0作成・main向けPR。mainマージ、タグ、JitPack正式公開は行わない。最終PRには本記録・移行ガイド・既知制約・検証matrixを参照し、Claudeへ確認を依頼する。

### リリースPR作成完了

PR63 head `a47d95cb64f75fc87aa98c158b0bfc92af6b531a` に対する[Claude APPROVE 6542332736](https://github.com/75py/Android-OverlayViewManager/pull/63#issuecomment-6542332736)と[CI41193528060成功](https://github.com/75py/Android-OverlayViewManager/actions/runs/41193528060)、base・head・CLEANを確認。19:41:14 JSTに `395f2ff10109068451611a7f3573eb75b2fa923c` へ通常merge。承認済みa47d95cとtree差分なしを確認した。

同SHAからOrca管理worktree `release-3.0.0` / branch `release/3.0.0` を作成してpushし、[Release 3.0.0 PR #66](https://github.com/75py/Android-OverlayViewManager/pull/66)をmain向けに作成した。PR本文には移行ガイド、検証、既知制約、API31のfreeform未検証、JitPackのタグ後確認、承認/CI参照を記載。Claudeへ本文・base/head・統合SHAの最終確認を依頼した（request `d001c30b-5c8c-4822-b034-72d7f3905057`）。mainへはマージせず、タグと正式公開も行っていない。

この完了記録だけを `fix/3.0.0/release-checkpoint` からrelease向けPRでレビューし、現head承認・CI成功後に統合する。work/3.0.0への反映は同じ承認済みreleaseへのfast-forwardとし、未レビューの差分を直接追加しない。最終的なrelease head・CI・確認コメントはPR66で追跡できる。
