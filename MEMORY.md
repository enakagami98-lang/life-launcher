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
