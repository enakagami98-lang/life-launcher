# MEMORY（LifeLauncher）

詳しい説明は Obsidian の「LifeLauncher（寿命ホームアプリ）.md」。ここは作業上の学びだけ。

## 学び
- PCにJavaもAndroid Studioも無い。ビルドは GitHub Actions のみ（gradle wrapper は置かず setup-gradle で Gradle 8.9 を使う）
- 構成: AGP 8.5.2 / Kotlin 2.0.20 / Compose BOM 2024.09.00 / compileSdk 34 / minSdk 26 → 初回でビルド成功
- 署名鍵は Python の cryptography で PKCS12 を作った（keytool 不要）。alias=life、鍵とストアのパスワードは同じ
- versionCode は GitHub の run_number。上書き更新に必要
- 「必ず寿命画面から始める」は onStop と HOME の onNewIntent でページを戻して実現
- 非表示はパッケージ名単位。非表示にするとお気に入りからも外れる

## 動作確認（2026-10-07）
- `.github/workflows/e2e.yml`（手動実行）で GitHub 上の仮想スマホ（Pixel 6 / Android 14）に最新APKを入れて43項目を自動確認 → 全PASS
- 実行: `gh workflow run e2e.yml` → 結果は Artifacts の e2e-out（result.txt と操作ごとのスクショ）
- 学び: 表示中アプリの判定は `dumpsys window` の mCurrentFocus だと標準ランチャーと誤判定する。`dumpsys activity activities` の topResumedActivity を使う
- 学び: `adb shell input text` は物理キーボード扱い → キーボード操作モードになり、ページが勝手に移動して誤判定。画面キーボードのキー座標をタップする方式にした
- 学び: Compose の uiautomator ダンプに BasicTextField のプレースホルダーは出ない。入力欄は class=android.widget.EditText で探す
- 修正したバグ: 入力ダイアログを開いたままホームに戻るとダイアログが残った → ページを key(homeSignal) で作り直して解決
- 修正: 199年生まれ等のありえない生年月日・寿命は保存不可にした

## 2026-10-07 追加分の学び
- 見守りは前景サービス＋UsageStatsのイベントを2秒ごとに確認（画面オン中のみ）。裏からホームを出すため1pxの透明オーバーレイを常時置く（Android 15対策）→ API 35で動作確認済み
- e2e: `gh workflow run e2e.yml -f only=scroll -f tag=v1.0.x` でスクロールだけ・Android 15だけの短いテスト（約8分）。全部だと約30分（5分の時間切れ待ちを含む）
- API 34のエミュレーターは長時間テストで途中で落ちることがある（アプリ側の問題ではない）
- uiautomatorのタップは画面上端140px〜下端2150pxを「見えている」範囲にする（狭すぎると⚙を押せない）
- 時刻入力のTimeInputはadb入力で不安定 → 時・分をボタンで選ぶ方式に変更

## 2026-10-07 簡易モード・長押しパネル
- ユーザーのスマホでは「使用状況へのアクセス」が保護で許可できない → 許可なしの簡易モードを追加（このアプリから開いた間だけ計測。ホームに戻ると今回の利用は終了）
- 簡易モードの使えない時間帯は、電話中・ロック画面の「電話をかける」から10分以外は何を開いてもホームに戻す
- 長押しメニューは ModalBottomSheet（丸アイコン＋項目名＋1行の説明）。説明文は1行に収まる長さにする
- `gh workflow run e2e.yml -f only=simple` で簡易モードとパネルの確認（約10分）→ 18/18 合格

## 2026-10-14 新アイコン・お気に入りドラッグ・今日の調子（v1.0.17）
- アイコン: 元画像は `design/icon_source.png`、変換は `design/make_icon.py`（Pillow）。アダプティブアイコンの前景PNGを各解像度で作る。絵は108dp中76dpに置くと丸型マスクでも左上のスマホの絵が欠けない
- 画面は5枚に（4=今日の調子。寿命から指を左→右へスワイプ）。`PAGES` を変えたら e2e.sh の「一周」テストも直す
- お気に入りの並べ替えは reorderable の longPressDraggableHandle。動かさずに離したら onDragStopped でメニューを出す
- 学び: longPressDraggableHandle と clickable を並べると、動かさずに長押し→離したときに clickable も発火してアプリが開く
- 学び: combinedClickable(onLongClick) で受け止めると、長押し後の指の動きまで吸われてドラッグが効かなくなる → clickable のまま「長押し中・離して400ms以内のタップは無視」で解決
- 「今日の調子」は SharedPreferences の "days"（日付→JSON）。AI用の質問文は `buildAiPrompt()`（直近120日・CSV形式＋目標・習慣）
- Better Than Yesterday の元コードは手元・GitHub・Artifactのどこにも無かった（要件だけで作った）
- `gh workflow run e2e.yml -f only=condition` で並べ替えと今日の調子の確認（約8分）→ 21/21 合格
- e2e の学び: LazyColumn の画面外の項目は uiautomator に出ない → 上の項目を押す前に下向きスワイプで先頭に戻す

## 2026-10-16 使用時間・今何してる？・今日のタスク（v1.0.19）
- 画面は6枚に（4=今日のタスク、5=今日の調子）。PAGES を変えたら e2e.sh と condition.sh の「一周」も直す
- スマホ使用時間は UsageStats を使わず、見守りサービスが「画面オン＋ロック解除中」の2秒ごとの tick を足して数える（簡易モードでも動くため）。Guard.screenMs（日付→ms）、30秒ごと＋画面オフ時に保存
- ロック解除の検知は check() 内の wasActive（画面オフ/ロック中→解除の変化）。USER_PRESENT/SCREEN_ON でも check() を即実行
- 「今何してる？」は Guard.askPending の間、MainActivity が AskScreen をページの代わりに出す。他アプリへ逃げたら openLauncher(null,"ask") で戻す（電話中は除く）。前の答えから5分は聞かない
- 今日のタスクは Store.tasks（doneDate 付き）。前日までに完了したものは pruneTasks で消え、未完了は残る（=持ち越し）
- `gh workflow run e2e.yml -f only=tasks`（約15分、5分待ちを含む）→ 24/24 合格
- e2e の学び: 1回目のキーボード表示は遅い → 入力欄タップ後4秒待つ。誕生日未設定の寿命画面は「人生の進捗」ではなく「まずは誕生日と寿命を設定」
- 全体テスト（all）: Android 15 は 67/68（残り1つは待ち画面の撮影タイミング）。Android 34 は途中でエミュレーターが落ちた（adb: device offline、既知）
