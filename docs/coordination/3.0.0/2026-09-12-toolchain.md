# T03: ビルド構成の再評価と独立検証

- 状態: 24fc1f2はsample設定で失敗、core/TimberのJava/Kotlin相互コンパイルは成功、次修正待ち
- 記録担当: Codex（Astra / medium）
- 関連タスク: T03、T04a
- Codex Run: run_18f7185e91aa
- Claude Run: run_3ae778449744

## 構成の相談と合意

CodexはT03の候補commitを読み、android.newDsl=false / android.builtInKotlin=falseが追加されていることを確認。msg_f61c1953e946（14:38:52 JST）で、旧Kotlin pluginを残すためだけの回避ではなく、公式に対応する構成と具体的な必要性を検討するようClaudeへ依頼した。

Codexは86eb4f89c3f4df9d1dfe0d2b260acbf15dafa898が5eca725を含み、tracked source差分がないコミット済み候補と確認し、既に合意済みの独立検証を開始（msg_39894fb55f59、14:42:48 JST）。このSHAの検証開始は構成への承認とはしない。特にT04aがbuildファイルを編集せずcoreのKotlinコードを追加できることを受け入れ条件として伝達した。

Claudeはmsg_1a1846f89349（14:44:01 JST）で変更理由を報告。実装担当は旧org.jetbrains.kotlin.androidが新DSL非互換で、ローカルGradleを実行できず新方式の検証が困難なため回避フラグを入れたという説明だった。両司令塔は、coreにKotlinを導入する後続との整合を優先して回避フラグを除去することで合意した。

修正方針:
- android.newDsl / android.builtInKotlinの無効化を外し、明示的kotlin.android plugin適用を除去する。
- 組み込みKotlinの版が決まる仕組みを公式情報で確認する。
- 新DSLが拒否する記法をminSdk/targetSdk、buildFeatures.dataBinding、enableUnitTestCoverage、代入記法、compilerOptionsへ移行する。
- lintの対応版はAGP9.2.1のGoogle Maven POMで確認し、バージョン番号の算術だけを根拠にしない。
- 文書化された障害があれば報告し、独断で回避フラグを復活させない。
- wrapper jarは現候補では未再生成。この点は検証とレビューで扱う。

Codexはmsg_614a1f917536（14:45:07 JST）で検証担当へ合意を共有。86eb4f8は具体的な初期エラーを得る基準検証として継続し、既に再検討済みの回避フラグの是非を繰り返し議論しない。修正後のSHAは別途検証する。

## 検証担当・範囲

- Model/effort: gpt-5.6-terra / high（launch.effective確認）
- Task: task_5fa509223d3f
- Dispatch: ctx_d750aaf55990
- Terminal: term_c44d62aa-5699-4d2d-896f-09d5e78f8c92
- Worktree: codex-3.0.0-t03-review
- 対象SHA: 86eb4f89c3f4df9d1dfe0d2b260acbf15dafa898
- 報告先: /private/tmp/overlay-t03-review.md
- 必須ビルド・全ローカルテスト・core/sample lint、依存の実在と互換性、core Kotlin readinessを確認。既存の期待テスト数はT07を含む98件だが、実測結果と区別する。
- tracked source/buildファイルはread-only。必要な一時検証fixtureは専用checkoutか/private/tmp内に限定し、完了前に除去する。
- ローカルGradleはこの担当の専有。T03実装担当は別worktreeのソース修正を継続可能。

この記録時点ではビルド成功・依存解決成功・正式な採用版を主張しない。

## 基準検証の結果と司令塔による評価・訂正

Terra/highのmsg_9fd0731f332f（14:47:29 JST）を有効worker_doneとして受信し、ctx_d750aaf55990をreleaseした。86eb4f8の必須Gradleコマンドは設定フェーズで失敗:
```text
Could not set unknown property 'sourceCompatibility' for project ':lint' of type org.gradle.api.Project.
```
lint/build.gradleのProject直下sourceCompatibility/targetCompatibilityをJava extensionへ移す必要がある。task graph構築前に停止し、JUnit/lint XMLは生成されなかった。テスト数0件という成功結果ではなく、検証未実行と記録する。

子はこれをP0としたが、Codex司令塔は開発候補のビルド阻害としてP1へ評価を修正し、必須修正としてClaudeへ返した（msg_d89be44275b7、14:49:11 JST）。wrapper9.4.1のダウンロード・展開・checksum検証は成功し、旧wrapper jarが動作不能という事実は認めなかった。選定した直接依存の公式POMはHTTP200で存在を確認したが、全推移依存や全ビルド互換性の成功とは扱わない。

CodexはcoreのKotlin pluginが欠ける可能性を挙げていたが、core/build.gradle18–20の実コードで既にorg.jetbrains.kotlin.androidを適用していることを確認し訂正した。子のJVM target不整合の指摘も、この時点では公式ドキュメントに基づく懸念であり、実コンパイルで再現された不具合ではない。修正版でJava/Kotlin17を明示し、実際のKotlin fixtureをコンパイルして確認する。組み込みKotlin/新DSLを採用する合意は、誤った「plugin不在」という前提に依存させず、回避設定を減らす保守性とサポートされる構成への移行として維持した。

## 修正担当への引き渡し

Claudeのmsg_0efbd3db7da6（14:50:29 JST）はlint DSL失敗をP1必須指摘として認め、Java extensionへの移動と同種のGroovy記法の確認を子へ指示。core pluginに関する訂正も反映した。

同時点でc914d43782ef284bc4739c277166f9425eaf1e0eまで新DSL移行を実装済みと報告。両回避フラグとkotlin.android適用を除去し、minSdk/targetSdk、buildFeatures、coverage、代入記法を移行。AGP9.2.1のPOMからKotlin2.2.10とlint32.2.1を確認したという報告であり、修正版のビルド成功を意味しない。新候補SHAを受信してから別途検証する。

T03再試行担当の自己申告はclaude-sonnet-5/high。起動引数と整合するが、Orca launch.effectiveが空であることとは区別して記録する。

PR28はhead ea40be298506b64e0b055475e958f97b851440e5への[Claude承認](https://github.com/75py/Android-OverlayViewManager/pull/28#issuecomment-5643970020)とCI run34675990831成功を確認し1af6dfb6b23202eca18a3e4f42f8a084061440acへ統合。非ブロッキングのT04a/b台帳行補足は次更新へ反映した（msg_02445d2fdf0e / msg_0eeefcb5897a）。

## 修正版24fc1f2の検証開始

Claudeのmsg_be74df233e72（14:52:18 JST）で24fc1f2faacedf088eb7dbefd6d2bf961562af6bを候補として受信。86eb4f8から履歴書き換えなしで、Kotlin plugin/回避フラグの撤去、新DSL移行、lint Java extension修正を追加した。基底はT07を含む5eca725。

[組み込みKotlinの公式移行文書](https://developer.android.com/build/migrate-to-built-in-kotlin)はjvmTargetの既定がandroid.compileOptions.targetCompatibilityになると説明しており、候補はcompileOptions=17による既定を使用する。不要な明示ブロックを必須化せず、実コンパイルで整合を検証することをCodexも受け入れた。旧レポートの一般的なKotlin既定値1.8という記述を、このAndroid組み込みKotlin構成の実証結果として適用しない。

Codexが依頼どおりpushし、msg_15ff4dbb7933（14:55:17 JST）で次の独立担当を共有:
- Model/effort: gpt-5.6-terra / high（launch.effective確認）
- Task: task_74810ea07694
- Dispatch: ctx_d88e20a3160a
- Terminal: term_38982116-4b83-45dc-8adb-909f51d2aafb
- Worktree: codex-3.0.0-t03-review（clean確認後に24fc1f2へfast-forward）
- 新報告先: /private/tmp/overlay-t03-review-revised.md（旧報告を保持）

必須ビルド・テスト・lintと、既存設定を変更しない一時Java/Kotlin fixtureのコンパイル実証を依頼。Gradle資源は独占。ClaudeがPR/remote CIの作成を担当する。検証結果は未受信。

## 24fc1f2の実測結果

PR29が作成され（msg_fc9a90fd6c59）、CI run34676815383はsample/build.gradle36のgetDefaultProguardFile('proguard-android.txt')が非対応となったため設定フェーズで失敗した。CodexはCIログを取得し、msg_4c4ceee43442（15:00:21 JST）でproguard-android-optimize.txtへの修正と同種設定の点検をClaudeへ依頼。根拠なくdontoptimizeを追加してエラーを回避しない方針とした。

Terraの有効worker_done msg_30d961482558（15:00:34 JST）でも同じP1失敗を再現。旧lint Java DSLの失敗は解消されたが、新たなsample設定のため98テストとlintは依然未実行。全体成功を主張しない。レビュー担当ctx_d88e20a3160aは完了後releaseした。

別途、sampleの設定問題に依存しないcore/opt-timberを--configure-on-demandで個別assembleし、一時的なJava/Kotlinクラスを両方向に呼び出すfixtureをコンパイル。両モジュールのJava/Kotlin計4クラスはjavapでclass major61（Java17）と確認できた。これはcore/TimberのKotlin readinessの実証であり、全体のGradleコマンドが成功したという意味ではない。fixtureのソース・classを除去し、tracked sourceはclean。一般的なJVM target既定値から推測した旧懸念は、この構成では実証により解消された。

Codexはmsg_5b1b01e48d25（15:03:03 JST）で結果を共有し、次がProGuard設定のみの修正なら全体検証と限定差分レビューを行い、構成が変わっていないKotlin compiler証明は無目的に繰り返さないと伝えた。

Claudeはmsg_216ab455da4dでCIログのAzure転送先へ到達できないことを報告したが、Codexが取得したエラー要旨と独立ローカル再現を利用できる。msg_8e7f857cf73f（15:02:56 JST）で同P1を実装担当へ返し、同種の古いAPI点検も依頼した。設定修正SHAの受信を待つ。
