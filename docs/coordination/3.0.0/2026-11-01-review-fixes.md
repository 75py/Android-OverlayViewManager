# 2026-11-01 リリース前レビュー指摘への対応

状態: PR70・PR71を相互承認/CI成功後に統合済み。基点は PR66 の `a79dcb8254d83eaf72784f524f34a12d68668cb7`。今回の依頼は、妥当性確認、再現する問題の修正、回帰テスト、現headの相互レビューとCI成功を経た `release/3.0.0` への反映まで。mainマージ・タグ・正式公開は対象外。

## 所有と合意

- Claude Code: core全体（削除中再入、ドラッグalpha、A1 listener、A2破棄理由、Bの小修正、回帰テスト）と `docs/design/3.0.0-api.md` を単独所有。`fix/3.0.0/core-lifecycle-review`。Codexが対向レビューとGradle検証を担当。
- Codex: CIのRelease/publish検証（A3）、未参照Timber画像の削除・残留調査（A4）、migrationのstdlib説明（A5）、本記録と計画、C1〜C4の推奨案、統合を担当。`fix/3.0.0/review-release-support`。Claudeが対向レビュー。
- 分担はClaude端末で明示合意。Orca記録 `msg_b4335a09de6d`、提案request `9ac84ff7-e1c7-41b2-ba88-480fa053ba19`、詳細共有request `472c24d5-2e4e-4284-bc9f-7fc5ffbf0323`。同じcoreファイルの並行編集を避け、Gradle/SDKはCodexが直列化する。追加の子agentは起動しない。
- 全タスクPRは `release/3.0.0` 向け。公開シグネチャ・enum・README未公開表記・CHANGELOG日付は変更しない。Cは提案のみで実装しない。

## 初期確認

coreでは `hide()` がbackend削除後に無条件でCONFIGUREDを書き、`dispose()` の公開入口は削除中フラグを確認していない。ドラッグは `spec.alpha` を直接変更し、UP/CANCELでbackup値を戻す。非DRAGGABLEの生成とacceptにも `setOnTouchListener(null)` があり、破棄後の分類は一律WINDOW_MANAGER_REJECTED。これらは静的確認であり、回帰テストによる再現結果と区別する。

A3: CIにはDebug・unit・lintのみでpublish処理がなかったため、`:core:assembleRelease :core:publishToMavenLocal` を追加。これはJitPackと同じ公開タスクの事前検証であり、JitPackサービスでの実際のビルド・座標解決の代替とは扱わない。

A4: `sample/screenshots/3.0.0/api36-timber.png` は参照なしで削除。設計書の現行スレッド説明等はClaude担当。移行ガイドの削除案内、CHANGELOG・計画・連携記録・設計書の明確な過去記述は維持する。AGENTSの古いコミット件名例は現行機能の説明ではない。

A5: Javaだけの利用アプリにもkotlin-stdlibが推移的に入ることを移行ガイドに追記。

## C1〜C4: 判断前の推奨案（履歴）

以下は当初の提案。2026-11-01のユーザー決定は後述の「C1〜C4のユーザー決定と追加端末検証」で確定し、この提案より優先する。

### C1: OverlaySpec

公開前に通常のclassへ設計し直し、Builderと明示的な更新APIを安定面にする案を推奨する。現在のdata classは追加プロパティがconstructor/copyのJVMシグネチャを変える。末尾への追加だけなら既存componentNは通常そのままだが、順序変更は分解代入の意味も変え得る。通常classにするだけで自動的に互換になるわけではなく、公開constructor/copyの設計と既存Java/Kotlin利用の移行を併せて確定する必要がある。現状維持なら3.xでは形を固定し、拡張用の別型/APIか次期majorで変更する方針が必要。今回は公開シグネチャ不変の依頼に従い何も変えない。

根拠: [Kotlinの後方互換性ガイド](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html#avoid-using-data-classes-in-your-api)。

### C2: internal adapterとテストsetter

公開前の別PRでKotlin側をinternalへ絞り、Javaからの呼び出し可能面も個別に監査する案を推奨する。internalはKotlinモジュール境界であり、JVMではpublicになるので、それだけでJavaから隠せるわけではない。必要なメソッドのJvmSyntheticやテストfixtureの分離を併せて検討する。実際にsampleのJavaテストがsetApplicationInstanceを利用しているため、可視性だけを機械的に変更しない。今回は変更しない。

根拠: [KotlinのJava相互運用と可視性](https://kotlinlang.org/docs/java-to-kotlin-interop.html#visibility)、`sample/src/test/java/com/nagopy/android/overlayviewmanager/sample/Sample2OverlayControllerTest.java`。

### C3: ABI検査

C1/C2の決定後、3.0公開前にAPI baselineとCI検査を導入する案を推奨する。意図しないシグネチャ変更を検出できる。binary-compatibility-validatorはmaintenance modeだが重大修正と現行Kotlin対応は継続し、新機能はKotlin Gradle plugin側へ移行している。採用方法はAGP9.2.1のbuilt-in Kotlinとの対応を別PRで検証して決める。apiCheckだけでは動作互換やJavaに見えるinternalの全漏出を保証しないため、既存Java/Kotlin consumerも残す。今回はプラグイン追加やapiDumpをしない。

根拠: [公式binary-compatibility-validator README](https://github.com/Kotlin/binary-compatibility-validator)。

### C4: 記録類のmainへの収録

今回はAGENTS、計画、連携記録と証拠をmainにも維持する案を推奨する。レビュー・端末検証の根拠へ既に相互リンクがあり、約2MBのため現段階では分離の負担が利点を上回る。`.serena/` の追跡対象は共有project.ymlとgitignoreだけで、cache/local設定は除外済み。チームがSerenaを使わない方針なら、後日この共有設定だけを外す別PRを提案する。別ブランチへの退避は既存リンクの付け替えも必要で、ファイルを削除しても過去Git履歴の容量は消えない。今回は移動・削除・履歴改変をしない。

## 検証・レビュー・統合

### CI・文書側の基準検証

Codexがsupport worktreeで以下を実行して成功（148 tasks）。core118/lint12/sample4、計134 tests、failure/error/skipはすべて0。lintはcore警告9/sample警告55、エラー0。POMにkotlin-stdlib2.2.10の依存を確認。新しいcore回帰テストはまだ含まない。

```sh
ANDROID_HOME=/Users/ai-seb/Library/Android/sdk ./gradlew :core:assembleDebug :sample:assembleDebug :core:testDebugUnitTest :lint:test :sample:testDebugUnitTest :core:lintDebug :sample:lintDebug :core:assembleRelease :core:publishToMavenLocal -Dmaven.repo.local=/private/tmp/ovm-review-support-maven-20261101
```

公開先を一時Maven repositoryへ隔離しているが、実行するpublishタスクはJitPackと同じ。`git diff --check` 成功。core修正後の検証、相互レビュー、CI、統合は未完了。


### 再現と修正の検証

実装は既存Claude Code司令塔（Opus 5.5 / high）が担当し、Codexが直接レビュー・検証した。追加の子agentは起動していない。ClaudeのsandboxからOrca inboxを読めなかったため、補足と結果はterminal sendでも再共有し、双方の回答を確認した。

最初のテストcommit `a231952` は、旧製品コードに対して70件中18件失敗。仕様整理後のテストだけのcommit `b036288` は72件中21件失敗（Lifecycle10件中2、State32件中9、Drag30件中10）。後者は、spec.alphaを利用者の値として保持し、一時表示はwindow alphaとして検証する仕様に合わせたもの。旧実装の二重remove、DISPOSEDの上書き、host listenerの消失、Activity破棄後の誤分類、mode変更後のalpha残留、明示alphaの上書き、復元失敗後の透明度累積を再現した。追加テストだけでなく、既存のdragテストも実際のwindow alphaを検証するよう更新した。

`381e827` がcore修正、`e344aa5c05420b8c75be6a84b332090812c09ac4` がレビュー補正後の候補。CodexはNOT_ATTACHEDのKDocがdispose時のDISPOSEDを表現できていない点、設計書の「異なるspecだけを適用」が復元失敗後のequal update再適用と矛盾する点、通常CANCELのwindow alpha assertionを指摘し、Claudeが修正した（request `bdbebee7-b24c-4199-8e59-69a1402b5028`）。

| 指摘 | 結論・修正 |
| --- | --- |
| 削除中の再入 | 妥当。removeは一度だけ。再入hideはno-op、再入disposeは参照を解放しDISPOSEDを維持。通常の削除失敗はATTACHEDを保ちretry可能。再入disposeが既に解放した後の削除失敗は外側へ報告するがDISPOSEDを戻さない。この境界も文書化。 |
| drag alpha | 妥当。spec.alphaは利用者の値を保ち、windowだけを一時的に0.6倍。モード変更、UP/CANCEL、hide/disposeで終了し、明示値を古いbackupで上書きしない。復元失敗後はequal updateでも再適用できる。 |
| A1 | 妥当。非DRAGGABLEの生成・show・update・releaseはhost listenerを触らない。library listenerの切替だけsetOnTouchListenerを呼ぶ。DRAGGABLE中はlibraryがlistenerを所有し、hostが直接置換したlistenerも解除時に消える制約はPR71本文で明示した。 |
| A2 | 妥当。Activity破棄経由で処分したhandleのshow/updateはACTIVITY_DESTROYED。通常disposeは従来の分類を維持。 |
| A3/A4/A5/B | 指摘を確認し、CI公開タスク、Timberの現行記述/画像、stdlib説明、到達不能SDK分岐、例外判定コメント、MOVEログ、license headersを修正。 |

`e344aa5` に対するCodexの実行コマンド:

```sh
ANDROID_HOME=/Users/ai-seb/Library/Android/sdk ./gradlew :core:assembleDebug :sample:assembleDebug :core:testDebugUnitTest :lint:test :sample:testDebugUnitTest :core:lintDebug :sample:lintDebug :core:assembleRelease :core:publishToMavenLocal :sample:assembleDebugAndroidTest -Dmaven.repo.local=/private/tmp/ovm-review-core-maven-20261101
ANDROID_HOME=/Users/ai-seb/Library/Android/sdk ./gradlew -p /private/tmp/ovm-review-core-consumer-20261101 :app:assembleRelease :app:lintRelease --refresh-dependencies
ANDROID_HOME=/Users/ai-seb/Library/Android/sdk ANDROID_SERIAL=emulator-5580 ./gradlew :sample:connectedDebugAndroidTest
```

- 最初のbuild/unit/lint/publishは176 tasks成功。unitはcore145/lint12/sample4の計161件、failure/error/skipは0。旧134件から27件増加。lintはcore警告6/sample警告55、エラー0。
- 独立したJava/Kotlin consumerはR8有効のRelease・lint、45 tasks成功。意図的なALLOW_OUTSIDE_BOUNDS警告1件とfixture由来3件、エラー0。
- Releaseのclassファイルをjavapで比較し、syntheticを除いたJavaから呼べるpublic/protected宣言15クラスに変更なし。追加helperはinternalかつJvmSynthetic。enum値も変更なし。API検査プラグインの導入ではなく、一時的な比較のみ。
- API36 sample端末試験は31/31成功（freeform2件を含む）、failure/error/skip0、4分4秒。専用AVDのfreeform設定を元のnullへ復元しemulator5580を終了した。無関係の5554は操作していない。物理実機およびAPI23/26/31/35は今回未検証。今回の再入・モード切替の直接再現はRobolectric/backend callbackによるもので、実機再現とは区別する。

### PR70の統合

[PR70](https://github.com/75py/Android-OverlayViewManager/pull/70) head `e8f39de71899ff6d96bc12f290fc8e65e052d2a8` は、[Claude承認](https://github.com/75py/Android-OverlayViewManager/pull/70#issuecomment-6555757402)と[CI成功](https://github.com/75py/Android-OverlayViewManager/actions/runs/41290341804)、base/head/CLEANを再確認し、`--match-head-commit` の通常mergeで `87c753da35951cd1f4fde50c00b211e4bbd05455` へ統合。PR71の統合結果は次節。


### PR71の統合と最終checkpoint

[PR71](https://github.com/75py/Android-OverlayViewManager/pull/71) head `e344aa5c05420b8c75be6a84b332090812c09ac4` は、[Codex承認](https://github.com/75py/Android-OverlayViewManager/pull/71#issuecomment-6555825058)と[CI成功](https://github.com/75py/Android-OverlayViewManager/actions/runs/41290698222)、base/head/CLEAN/readyを確認し、`--match-head-commit` の通常mergeで `4b9b2a78cbc1ca89e1994c35d00343ae57bd7905` へ統合した。レビュー済みheadと統合SHAのcore/sampleソース/lint/build設定に差分なし。PR70のCI・文書・画像削除も同時に含む。PR71本文の検証コマンドはCodexが実行した一時Maven repository引数とconsumerを含む完全形に揃えた。

今回の記録追記は文書だけで製品/テスト/build変更なし。最終release headとwork/3.0.0の同期、SHA付きClaude確認、CIは[PR66](https://github.com/75py/Android-OverlayViewManager/pull/66)の最新コメント/checksで追跡する。mainへのマージ・タグ・正式公開は実行しない。この時点ではC1〜C4は提案のみでユーザー判断待ちだった。後述の追加依頼で判断が確定した。

再現情報（候補e344aa5）:

- API36 fingerprint: `google/sdk_gphone64_arm64/emu64a:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`
- core Release AAR SHA256: `db6412e94c763f6e323f4c720eefada4c252a48c67b0ace30a0e8bc457e7cde8`
- sample Debug APK SHA256: `a0e55a5ee8697a6ec26ac17468944f53f6276e7452766e9e15ef91208ab91982`
- sample test APK SHA256: `e0e93fb1aee652533953d4bc9835df7daf4d6c1bc7230a0ac9491a6675fa3576`

PR72初稿 `63cf89b` の対向レビューで、Claudeは計画冒頭の旧134 testsが「現行候補」と読める点を指摘した。Codexは「Timber削除時点の候補」と時点を明記して161件の最新結果と区別した。製品コードの追加変更はない。


## C1〜C4のユーザー決定と追加端末検証

2026-11-01、ユーザーは `release/3.0.0` の `c1eafebdd04e2a3f77f84a6154d766c6d9a06980` を基点に、以下を明示決定した。上の推奨案を採用したという意味ではなく、この決定が現行方針となる。

| 項目 | ユーザー決定・今回の対応 |
| --- | --- |
| C1 | `OverlaySpec` はdata classを維持。3.xの新プロパティはデフォルト値付きで末尾に追加する。Kotlinのconstructor/copy呼び出しとJava Builder利用のソース互換を保つ一方、コンパイル済み中間ライブラリのconstructor/copyに関するバイナリ互換はマイナーバージョン間で保証しない。JavaはBuilderを使う。KDocと移行ガイドへ明記する。 |
| C2 | 現状維持。internalパッケージの宣言は `@RestrictTo(LIBRARY_GROUP)` とinternalパッケージ名の慣習で保護し、コードは変更しない。 |
| C3 | 3.0.0ではAPI互換チェックを導入しない。3.0.0タグをbaselineに、3.1着手前の導入タスクを計画に追加。binary-compatibility-validatorとKotlin Gradle plugin組み込みABI validationの比較、およびAGP 9のbuilt-in Kotlinとの組合せ検証を含む。 |
| C4 | 現状維持。AGENTS・計画・連携記録・共有設定をmainにもそのまま含める。移動や削除はしない。 |

分担: Codexが専用worktree `codex-compatibility-device-validation` / `fix/3.0.0/compatibility-device-validation` でKDoc・migration・計画・本記録の編集、Gradle/AVD検証、PR、統合を所有する。既存Claude司令塔（Opus 5.5 / high）は対向レビューを担当し、元のcore KDoc所有権を今回の範囲でCodexへ移管した。端末送信request `008e4871-05f5-4d50-9e27-2e8956d18d94` に対しClaudeが明示合意。追加子agentは起動しない。

受け入れ条件: 公開APIシグネチャ・enum・README未公開表記・CHANGELOG日付は不変。指定Gradle全タスクとPR CI成功、最低API23のsample connected試験を再実行し、可能なAPI26/35も実行。失敗時は修正せず原因・再現手順を報告する。release向けPRは現head対向承認とCI後に統合し、PR66へ統合SHA/CI結果をコメントする。mainマージ・タグ・正式公開はしない。現在検証中で、成功結果は未確定。
