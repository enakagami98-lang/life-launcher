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

## 2026-10-19 トップ画面の作り変え・支払額（v1.0.22）
- 理由：寿命画面のデザインが参考アプリの丸写しで公開できないため。残り日数だけ残し、週数・月数・進捗バー・命日は削除。左上の見出しは「寿命」→今日の日付
- 残り日数の下は TopWidgets.kt の部品（motto/screen/tasks/spend）。並びと表示は Store.widgets（"widgets" に カンマ区切り）。「表示する項目を編集」で変更
- 今月の支払額は Spend.kt：NotificationListenerService（PaymentListener）で通知文から金額を読む。支払いの語＋金額があり、受け取り/チャージ/返金/広告の語が無いものだけ。LINE等のチャットアプリは除外。10分以内の同額は1件（アプリ通知とメールの二重計上防止）
- 通知へのアクセスも「制限付き設定」の対象 → 許可できないときは アプリ情報→︙→制限付き設定を許可
- 現在地の表示はユーザーが「後で考える」とした（未実装）
- e2e: `gh workflow run e2e.yml -f only=top`（約10分）→ 20/20。通知は `adb shell cmd notification post -t タイトル tag 本文`、許可は `cmd notification allow_listener PKG/PKG.PaymentListener`
- 全体テスト Android 15 は 66/68。2件は撮影タイミング（「残り5分」のお知らせは撮影画像には出ていた）。Android 34 はまたエミュレーターが途中で落ちた

## 2026-10-20 使用時間の比較を先月比に（v1.0.24）
- 今日の調子の使用時間カード：「前の日より◯分」→「先月より使用時間が平均◯%減っています／増えています」（ConditionPage.kt の MonthCompare）
- 平均は「記録のある日（1分以上）」だけで出す。今日は途中なので、今月のほかの日に記録があれば今日は除く。先月の記録が無いときは案内＋今月の1日平均
- 前月データを仮想スマホに入れる手段が無い（release版は run-as 不可）ので、%表示そのものは e2e で確認できていない。案内文の表示だけ確認
- e2e の学び: インストール直後の最初のスワイプが空振りすることがある → tasks.sh は「今日のタスク」の画面に着くまでスワイプ（to_tasks）。修正後 26/26

## 2026-10-21 選択肢の追加（v1.0.27）
- 今していることに「ジム」「読書」を追加（全13個）。①は4行でまだスクロールなしで見える。`-f only=top` で 22/22

## 2026-10-21〜22 言語対応・選択肢の編集・新しい部品（v1.0.31）
- 画面の文字は全部 res/values*/strings.xml。**元は i18n/base.py（日本語・英語）と i18n/tr_xx.py（各言語）**。直したら `python i18n/build.py` で作り直す（書式 %1$s の数がずれると英語にされて警告が出る）
- 対応：日本語・英語（それ以外の言語の既定）・中国語簡体/繁体・韓国語・スペイン語・フランス語・ドイツ語・ポルトガル語・インドネシア語（フォルダ名は values-in）
- 文字リソースは連続した空白が1つにまとめられる（「今月の支払額  ¥」は「今月の支払額 ¥」になる）
- AI用の質問文は日本語と英語だけ。ほかの言語では英語の質問文の最後に「Please answer in ○○」を足す
- 「今何してる？」の選択肢は Guard.doingOptions / purposeOptions（"@キー" は最初からある選択肢で表示時に訳す）。編集画面は OptionsEditDialog
- ホームの最初の表示は モットー・今日のスマホ・支払額。部品は ALL_WIDGETS（motto/screen/spend/tasks/countdown/weather/habits/steps/yesterday）
- 天気は Open-Meteo（無料・キー不要、位置は小数2桁に丸める）。歩数は TYPE_STEP_COUNTER を GuardService の中で聞く（Steps.kt）
- e2e の学び：仮想スマホは英語なので `cmd locale set-app-locales PKG --locales ja-JP` でアプリだけ日本語にする。すると **キーボード（Gboard）が日本語入力の初期設定画面になり英字を打てない** → 英字を打つ間だけ en-US に切り替える（lang 関数）。また他アプリの名前も日本語（設定・連絡帳）で返る
- e2e の学び：ui.py は y>2150 を画面外とみなすので、画面の下端すれすれのボタン（ダイアログの「閉じる」など）は押せない → 戻るボタンで閉じる

## 2026-10-22 20言語に（v1.0.35）
- 追加：イタリア語・タイ語・アラビア語・ヒンディー語・ベンガル語・ロシア語・ウルドゥー語・トルコ語・ベトナム語・マラーティー語・テルグ語・タミル語（世界の話者数上位20をカバー。広東語は中国語繁体、ナイジェリア・ピジンは英語）
- 新しい言語を足す手順：i18n/tr_xx.py を作る → build.py の LANGS と LOCALE_TAG に足す → `python i18n/build.py`
- 1言語の訳は約20KB（APK 7MB の約0.3%）。直接配るAPKには全言語が入る
- アラビア語・ウルドゥー語は supportsRtl で画面が左右反転（ページのスワイプ方向も逆になる）→ 仮想スマホの撮影で確認済み
- 訳は Claude が作ったもの。公開前に、できればその言語を話す人に見てもらう（特にマラーティー語・テルグ語・タミル語・ベンガル語・ウルドゥー語）
- e2e：`-f only=lang` で9言語に切り替えて撮影（約10分）→ 9/9。全体テスト Android 15 は 67/68（残り1つは撮影タイミング）、Android 14 は仮想スマホが固まり60分で打ち切り（いつもの）
- e2e の学び：`while read ... done <<EOF` の中で adb を呼ぶと、adb が一覧を読み込んでしまいループが1回で終わる → `read <&3` と `done 3<<EOF` にする

## 2026-10-22 名前を Loosen に・使用時間の数え方の修正・朝の「今日の目標」（v1.0.42）
- 表示名は Loosen（app_name と notif_title）。パッケージ名・リポジトリ名・APK のファイル名（LifeLauncher.apk）は変えていない（上書き更新とダウンロードリンクを壊さないため）
- 不具合の原因：制限アプリの使用時間は「待ち画面を通して開いた間」しか数えていなかった → 通知・履歴から開いた Instagram/X は0のまま
- 直し方：Guard.usedMs = max(自前の記録, スマホ本体の使用記録)。本体の記録は Guard.systemUsage()（今日0時からの ACTIVITY_RESUMED〜PAUSED/STOPPED を足す、15秒キャッシュ）。「使用状況へのアクセス」が無いと本体の記録は読めない → 設定に赤字の注意
- 確認用ログ：`adb logcat -s Loosen`（usage events=… limited=pkg:秒）
- 今日の目標：Guard.needGoal()＝ 目標オン・夜のおやすみ時間オン・今は使えない時間ではない・今日の夜の時間が終わった（日付をまたぐなら昨日の曜日）・今日の目標が空。true の間 MainActivity は GoalScreen、GuardService は他アプリから戻す。onUnlock では「今何してる？」より優先
- トップの部品に "goal"（今日の目標）。設定の「使えない時間帯」にオン/オフ（最初はオン）
- e2e：`-f only=count`（5/5）、`-f only=goal`（14/14）。e2e の学び：日本語表示だと時計アプリの名前は「時計」、通知の中身は `dumpsys notification --noredact` でないと見えない

## 2026-10-24 使い方（チュートリアル）（v1.0.44）
- TutorialScreen.kt：7ステップ（ようこそ→寿命→ホームアプリ→お気に入り→使いすぎ防止→トップの情報→使い方）。説明を読みながらその場で設定し、「次へ」で保存
- 自動で出るのは Store.tutorialDone が false かつ誕生日が未設定のとき（＝入れたばかり）。設定の「使い方をもう一度見る」からも開ける
- ホームアプリは RoleManager.createRequestRoleIntent(ROLE_HOME) で直接選ばせる（Android 10+）。isDefaultHome() で設定済みか判定
- 文字は i18n/tutorial.py（KEYS と言語ごとのリスト）。build.py が base.py と合わせて読む
- e2e：`-f only=tutorial`。新しく入れると使い方が出るので、ほかのテストは日本語にした直後に「スキップ」を押す
- e2e の学び：画面の下端（y>2150）にある「次へ」などは ui.py の tap では押せない → `ui.py tapraw` を使う
- 公開に向けて：次は課金（Stripe 希望。Google Play の課金ルールの確認が必要）、その次にセキュリティの相談

## 2026-10-24 課金（Google Play 定期購入）（v1.0.47）
- 版を2つに分けた（productFlavors）：`github`＝com.enakagami.lifelauncher（課金なし、LifeLauncher.apk）、`play`＝com.enakagami.loosen（課金あり、Loosen-play.aab/.apk）。BuildConfig.PAYWALL で切り替え
- Billing.kt：定期購入 `loosen_yearly`（基本プラン yearly 20ドル、特典 free-trial 3日は Play Console 側で設定）。Billing Library 8.0.0。購入後は acknowledge 必須（3日以内にしないと自動返金）
- 未購入（3日の無料期間終了後）は PaywallScreen：購入カード＋普通のアプリ一覧。Loosen の機能（使いすぎ防止・夜のロック・質問・目標）は止まるが、スマホ自体は使える（スマホを使えなくするのは Play のポリシー違反）
- 通信できないときは最後に「購入中」を確認してから7日間は使える
- compileSdk/targetSdk 36（2026-08-31以降の新規アプリは API 36 が必須）。AGP 8.9.1、Gradle 8.11.1
- Play Console の手順は Obsidian「Loosen_GooglePlay公開と課金の手順.md」
- e2e：`-f only=paywall`（10/10）。全体テストは使い方のスキップを set-home-activity の後にしないと止まる。全体（Android 15）67/68 合格

## 2026-10-24 セキュリティ対策（v1.0.53）
- R8 を有効に（isMinifyEnabled + isShrinkResources）。APK 8.5MB → 2.5MB。proguard-rules.pro で Log.d/v/i を消す（count.sh の logcat 表示は出なくなるが合否には使っていない）
- Integrity.kt：署名の指紋（SHA-256）を確認し、違えば Billing.locked。指紋は build.gradle.kts の certSha256()（release.p12 から自動）＋ GitHub 変数 `PLAY_CERT_SHA256`（Play Console「アプリの署名」の値）
- **PLAY_CERT_SHA256 が空なら確認しない**（Play から入れたアプリは Google の鍵で署名されるので、空のまま確認すると全員止まる）
- build.gradle.kts で java.security.* を直接書くと `java` 拡張とぶつかってエラー → 先頭で import する
- リポジトリは Public だった（フォーク0）。非公開化はユーザーが行う。非公開だと Actions は月2000分まで
- e2e：paywall 10/10、top 39/39、全体（Android 15）67/68（残りは既知のタイミング）。1回目は入力抜けで 64/68
- 詳細は Obsidian「Loosen_セキュリティ対策.md」
