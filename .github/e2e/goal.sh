#!/bin/bash
# 夜のおやすみ時間が終わったあと最初に「今日の目標」を決めないと使えないこと、トップ画面の「今日の目標」の確認
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top)"; }
declare -A K=( [a]="112 1868" [b]="648 2024" [c]="432 2024" )
softword() { w=$1; for ((i=0;i<${#w};i++)); do adb shell input tap ${K[${w:$i:1}]}; sleep 1; done; }
key() { adb shell input keyevent "$@"; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_app() { sleep 1; t=$(top); if echo "$t" | grep -q "$2"; then ok "$1 ($t)"; else ng "$1 ($t)"; fi; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; key KEYCODE_HOME; sleep 2; }
unlock() { key KEYCODE_POWER; sleep 3; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 4; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3
lang ja-JP
$UI tap "スキップ" >/dev/null; sleep 2  # 入れた直後の使い方を閉じる

log "== 1. 夜のおやすみ時間を「さっき終わった」時間にする"
H=$(adb shell date +%H | sed "s/[^0-9]//g"); S=$(( (10#$H + 22) % 24 ))
log "   端末の時刻 $(adb shell date +%H:%M | sed "s/[^0-9:]//g") → 夜を ${S}:00〜${H}:00 にする"
expect_no_text "設定前は目標の画面は出ない" "夜のおやすみ時間が終わりました"
$UI tap "設定" >/dev/null; sleep 2
$UI tap "23:00" >/dev/null; sleep 2; $UI tap "${S}時" >/dev/null; $UI tap "00分" >/dev/null; sleep 1; $UI tap "決定" >/dev/null; sleep 1
$UI tap "06:00" >/dev/null; sleep 2; $UI tap "$((10#$H))時" >/dev/null; $UI tap "00分" >/dev/null; sleep 1; $UI tap "決定" >/dev/null; sleep 1
expect_text "目標のスイッチがある" "最初に今日の目標を決める"
$UI tap "夜をオンにする" >/dev/null; sleep 3; snap goal_screen
expect_text "目標の画面が出る" "夜のおやすみ時間が終わりました"
expect_text "決めるボタン" "決めて1日を始める"

log "== 2. 決めずに他のアプリを開いても戻される"
adb shell monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 8; snap bounced
expect_app "Loosen に戻される" $PKG
expect_text "目標の画面のまま" "夜のおやすみ時間が終わりました"
key KEYCODE_HOME
expect_text "ホームボタンでも目標の画面のまま" "夜のおやすみ時間が終わりました"

log "== 3. 目標を決める（英字を打つ間だけ英語表示）"
lang en-US; snap goal_en
expect_text "英語の目標画面" "Night rest time is over"
$UI tapclass android.widget.EditText >/dev/null; sleep 4; softword abc; sleep 1
$UI tap "Set it and start the day" >/dev/null; sleep 2
lang ja-JP; snap after_goal
expect_no_text "決めると目標の画面が消える" "夜のおやすみ時間が終わりました"
expect_text "トップ画面に戻る" "まずは誕生日と寿命を設定"
adb shell monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 8
expect_app "決めた後は他のアプリを使える" com.android.chrome
key KEYCODE_HOME

log "== 4. トップ画面の「今日の目標」"
$UI tap "表示する項目を編集" >/dev/null; sleep 2
expect_text "部品の候補に今日の目標" "今日の目標"
$UI tap "今日の目標を表示" >/dev/null; sleep 1
for i in 1 2 3; do $UI tap "今日の目標を上へ" >/dev/null; sleep 1; done
$UI tap "保存" >/dev/null; sleep 2; snap goal_widget
expect_text "決めた目標がトップに出る" "abc"

log "== 5. もう一度ロック解除しても、もう聞かれない"
unlock; snap unlock_again
expect_no_text "2回目は出ない" "夜のおやすみ時間が終わりました"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
