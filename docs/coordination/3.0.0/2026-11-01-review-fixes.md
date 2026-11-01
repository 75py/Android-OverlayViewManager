# 2026-11-01 リリース前レビュー指摘への対応

状態: 修正・検証中。基点は PR66 の `a79dcb8254d83eaf72784f524f34a12d68668cb7`。今回の依頼は、妥当性確認、再現する問題の修正、回帰テスト、現headの相互レビューとCI成功を経た `release/3.0.0` への反映まで。mainマージ・タグ・正式公開は対象外。

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

## C1〜C4: ユーザー判断待ちの推奨案（未実装）

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
