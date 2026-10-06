# MEMORY（LifeLauncher）

詳しい説明は Obsidian の「LifeLauncher（寿命ホームアプリ）.md」。ここは作業上の学びだけ。

## 学び
- PCにJavaもAndroid Studioも無い。ビルドは GitHub Actions のみ（gradle wrapper は置かず setup-gradle で Gradle 8.9 を使う）
- 構成: AGP 8.5.2 / Kotlin 2.0.20 / Compose BOM 2024.09.00 / compileSdk 34 / minSdk 26 → 初回でビルド成功
- 署名鍵は Python の cryptography で PKCS12 を作った（keytool 不要）。alias=life、鍵とストアのパスワードは同じ
- versionCode は GitHub の run_number。上書き更新に必要
- 「必ず寿命画面から始める」は onStop と HOME の onNewIntent でページを戻して実現
- 非表示はパッケージ名単位。非表示にするとお気に入りからも外れる
