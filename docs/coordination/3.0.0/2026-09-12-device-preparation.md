# T12端末検証の環境準備

- 状態: 提案
- 記録担当: Codex（Astra / medium）
- 関連タスク: T05、T10、T12
- Codex Run: run_18f7185e91aa
- 進捗: 環境準備のみ。端末テスト未実行、T12未完了。

## システムイメージと専用AVD

既存環境にはAPI36のGoogle APIs / Google Play ARM64画像と他作業のAVDがあった。sdkmanager --listでAPI23/26/35 Google APIs ARM64が配布されていることを確認し、空き261GiBを確認後、3画像を追加した。sdkmanager exit0、各package.xmlの存在を確認。実行ログは /private/tmp/overlay-sdk-image-install.log、一覧は /private/tmp/overlay-sdk-available-images.log。SDK/toolchain本体の更新は行っていない。

avdmanagerでpixel_2 hardware profileの専用AVDを新規作成し、全てexit0とconfig.ini存在を確認した。

| AVD | System image |
| --- | --- |
| OverlayViewManager_3_0_API_23 | system-images;android-23;google_apis;arm64-v8a |
| OverlayViewManager_3_0_API_26 | system-images;android-26;google_apis;arm64-v8a |
| OverlayViewManager_3_0_API_35 | system-images;android-35;google_apis;arm64-v8a |
| OverlayViewManager_3_0_API_36 | system-images;android-36;google_apis;arm64-v8a |

作成ログは /private/tmp/overlay-avd-create-23.log 等。既存AVDを上書きせず、既存実行中端末も操作していない。まだ新AVDを起動しておらず、ホスト上でのboot可否・instrumentation・cross-UID動作の証拠にはならない。sdkmanagerの非推奨通知とdevices.xml読取通知は観測したが、インストール・AVD作成は成功した。

msg_5a7f8451e37dでClaudeへ追加開始、msg_e60771b4f61eで画像追加完了を共有。Gradle検証と画像追加は別対象であり、共有Gradle枠はT04b担当のまま。

## 次工程の公式API確認

T05/T10の準備として2026-09-12に確認した。実装や新しい公開契約への承認ではない。

- API30以降のcurrentWindowMetricsは現在のwindow/task状態を反映し、boundsからinsetsを控除しない。multi-windowで最大画面を現在の利用可能領域と同一視しない。[WindowManager reference](https://developer.android.com/reference/android/view/WindowManager#getCurrentWindowMetrics())
- ACTION_MANAGE_OVERLAY_PERMISSIONのpackage URIによる個別設定画面指定はpre-R向けとして説明されている。承認済みIntent形状は保てるが、Android30+で必ず個別画面へ直行するとは文書に書かない。存在しないActivityへの起動対応はhost側の責務。[Settings reference](https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION)

準備メモ /private/tmp/overlay-t05-platform-preparation.md とmsg_411ef57d6e14でClaudeへ共有。View.getRootWindowInsetsの資料取得はtool internal errorだったため、同資料を検証済みと扱っていない。

## 起動可否の確認完了（18:29 JST）

Luna/medium（launch.effective確認）task_f3e3e9e37422 / ctx_ee636a75e7a4が専用AVD4台をport5580で順次起動し、API23/26/35/36それぞれのSDK値とsys.boot_completed=1を確認。worker_done msg_b43e6993ca64を受領・release。各owned emulatorを終了し、既存emulator-5554のみ残存を確認した。報告は/private/tmp/overlay-emulator-readiness.md、起動ログは/private/tmp/overlay-emulator-API23.log等。

利用制限に伴う縮小指示を送信した時点でprobeは既に完了しており、dispatch_inactiveで拒否された。新dispatchは作らず完了証拠を受領・解放した。起動確認はアプリ動作・instrumentation・cross-UID透過の検証ではなく、T12は未完了。
