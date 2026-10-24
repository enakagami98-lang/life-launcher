#!/bin/bash
# 今日のタスク・スマホ使用時間・ロック解除時の「今何してる？」の確認（簡易モード）
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top)"; }
declare -A K=( [q]="60 1720" [w]="164 1720" [e]="272 1720" [r]="380 1720" [t]="488 1720" [y]="596 1720" [u]="704 1720" [i]="812 1720" [o]="920 1720" [p]="1028 1720"
  [a]="112 1868" [s]="220 1868" [d]="328 1868" [f]="432 1868" [g]="540 1868" [h]="648 1868" [j]="756 1868" [k]="864 1868" [l]="968 1868"
  [z]="220 2024" [x]="328 2024" [c]="432 2024" [v]="540 2024" [b]="648 2024" [n]="756 2024" [m]="860 2024" )
softword() { w=$1; for ((i=0;i<${#w};i++)); do adb shell input tap ${K[${w:$i:1}]}; sleep 1; done; }
key() { adb shell input keyevent "$@"; sleep 2; }
next_page() { adb shell input swipe 950 1200 150 1200 400; sleep 2; }
prev_page() { adb shell input swipe 150 1200 950 1200 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_app() { sleep 1; t=$(top); if echo "$t" | grep -q "$2"; then ok "$1 ($t)"; else ng "$1 ($t)"; fi; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-150)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
unlock() { key KEYCODE_POWER; sleep 3; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 4; }
to_tasks() { for i in 1 2 3 4 5 6; do $UI has "今日のタスク" >/dev/null && $UI has "追加" >/dev/null && return; next_page; done; }
to_tasks_en() { for i in 1 2 3 4 5 6; do $UI has "Today's tasks" >/dev/null && $UI has "Add" >/dev/null && return; next_page; done; }
add_task() { lang en-US; to_tasks_en; $UI tap "Add" >/dev/null; sleep 2; $UI tapclass android.widget.EditText >/dev/null; sleep 4; softword $1; sleep 1; $UI tap "Save" >/dev/null; sleep 2; lang ja-JP; to_tasks; }

# アプリの言語を切り替える。日本語のままだと文字入力のキーボードが日本語入力になり、英字を打てないので、打つ間だけ英語にする
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 3; }
adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd locale set-app-locales com.enakagami.lifelauncher --locales ja-JP >/dev/null 2>&1  # 仮想スマホは英語なので、このアプリだけ日本語に
sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 2; python3 .github/e2e/ui.py tap "スキップ" >/dev/null; sleep 2  # 入れた直後の使い方を閉じる
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3

log "== 1. 今日のタスク（習慣の右隣）"
to_tasks; snap tasks_empty
expect_text "今日のタスクの画面" "今日のタスク"
expect_text "使い方の説明" "終わらなかったタスクは次の日に持ち越されます"
add_task abc; snap task_added
expect_text "タスクが追加される" "abc"
expect_text "進み具合" "0 / 1 完了"
$UI tap "abcを完了にする" >/dev/null; sleep 1; snap task_done
expect_text "チェックで完了" "全部おわり！"
add_task xyz
expect_text "2つ目を追加" "1 / 2 完了"
key KEYCODE_HOME; to_tasks
expect_text "ホームに戻っても残る" "xyz"
expect_text "完了の状態も残る" "abcを未完了に戻す"
$UI tap "abcを未完了に戻す" >/dev/null; sleep 1
expect_text "チェックを外せる" "0 / 2 完了"
key KEYCODE_HOME

log "== 2. 今日の調子の画面にスマホ使用時間"
prev_page; snap screen_time
expect_text "使用時間の見出し" "今日のスマホ使用時間"
expect_text "ロック解除の回数" "ロック解除"
expect_text "先月との比較の欄" "先月の記録がたまると"
expect_no_text "前の日との比較はやめた" "前の日より"

log "== 3. 「今何してる？」をオン"
$UI tap "今何してるを聞く" >/dev/null; sleep 1; snap ask_on
key KEYCODE_HOME
unlock; snap ask_shown
expect_app "解除後はLifeLauncher" $PKG
expect_text "質問が出る" "今何してる？"
expect_text "今日の使用時間も出る" "回目のロック解除"

log "== 4. 答えずに他のアプリを開いても戻される"
adb shell monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 8; snap bounced
expect_app "LifeLauncherに戻される" $PKG
expect_text "質問のまま" "今何してる？"
key KEYCODE_HOME
expect_text "ホームボタンでも質問のまま" "今何してる？"

log "== 5. 2タップで答える"
$UI tap "勉強" >/dev/null; sleep 1; $UI tap "SNS" >/dev/null; sleep 2; snap answered
expect_no_text "答えると質問が消える" "今何してる？"
expect_text "寿命画面に戻る" "まずは誕生日と寿命を設定"
adb shell monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 8
expect_app "答えた後は他のアプリを使える" com.android.chrome
key KEYCODE_HOME

log "== 6. 記録が今日の調子に出る"
prev_page; $UI tapp "勉強 → SNS" >/dev/null; sleep 1; snap log_shown
expect_text "時刻つきの記録" "勉強 → SNS"
expect_text "多かった理由" "多かった理由：SNS 1回"

log "== 7. 5分以内はもう一度聞かない"
key KEYCODE_HOME; unlock; snap no_ask
expect_no_text "5分以内は聞かない" "今何してる？"

log "== 8. オフにすると聞かない"
prev_page; $UI tap "今何してるを聞く" >/dev/null; sleep 1
key KEYCODE_HOME
log "   (5分待ってから解除)"; sleep 300
unlock; snap off_no_ask
expect_no_text "オフなら聞かない" "今何してる？"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
