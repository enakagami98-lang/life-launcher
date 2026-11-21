#!/bin/bash
# 集中タイム・習慣のチェック・表示するページ・AIに渡す期間・重ねて表示の目印の確認（2026-11-16）
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
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_exact() { if $UI has "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 3; }
clock() { adb shell am start -n com.google.android.deskclock/com.android.deskclock.DeskClock >/dev/null 2>&1 || adb shell monkey -p com.google.android.deskclock 1 >/dev/null 2>&1; sleep 6; }
anchor() { adb shell dumpsys window windows | grep -c "LifeLauncherAnchor"; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd locale set-app-locales $PKG --locales ja-JP >/dev/null 2>&1
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 3; bash .github/e2e/skip_tutorial.sh
adb shell appops set $PKG GET_USAGE_STATS allow
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3

log "== 1. トップに集中タイムの部品"
snap top
expect_exact "集中タイムの部品" "集中タイム"
expect_text "まだ集中していない" "今日はまだ集中していません"

log "== 2. 集中タイムの画面（長さと使えるアプリを選ぶ）"
adb shell cmd notification allow_dnd $PKG >/dev/null 2>&1  # 「おやすみモードへのアクセス」を許可（通知を止めるため）
zen() { adb shell settings get global zen_mode | tr -d ''; }
$UI tap "集中を始める" >/dev/null; sleep 2; snap focus_dialog
expect_text "通知を止める切り替え" "集中中は電話以外の通知を止める"
[ "$(zen)" = "0" ] && ok "始める前はおやすみモードではない" || ng "始める前からおやすみモード（zen_mode=$(zen)）"
expect_text "長さを選ぶ" "何分集中する？"
expect_text "最初は電話だけ" "電話のほかに使えるアプリはありません"
$UI tap "＋ 使えるアプリを選ぶ" >/dev/null; sleep 2; snap picker
$UI tap "Chrome" >/dev/null; sleep 1; $UI tap "決定" >/dev/null; sleep 2; snap picked
expect_exact "Chrome が使えるアプリに入る" "Chrome"
expect_no_text "「電話のほかに無い」が消える" "電話のほかに使えるアプリはありません"
$UI tap "15分" >/dev/null; sleep 1
$UI tap "集中を始める" >/dev/null; sleep 3; snap focus_screen
expect_exact "集中の画面" "集中タイム中"
expect_text "終わる時刻" "に終わります"
expect_exact "電話のボタン" "電話をかける"
expect_exact "使えるアプリのボタン" "Chrome"
z=$(zen); [ "$z" = "1" ] && ok "集中中は電話以外の通知を止める（zen_mode=1）" || ng "集中中なのに通知が止まっていない（zen_mode=$z）"

log "== 3. 使えないアプリは閉じられる"
clock; snap clock_blocked
expect_app "時計アプリは閉じられる" $PKG
expect_exact "集中の画面に戻る" "集中タイム中"

log "== 4. 使えるアプリは開ける・ホームボタンでも集中の画面のまま"
$UI tap "Chrome" >/dev/null; sleep 8; snap chrome_allowed
expect_app "Chrome は使える" com.android.chrome
key KEYCODE_HOME; snap home_during_focus
expect_exact "ホームボタンでも集中の画面" "集中タイム中"
next_page; expect_exact "スワイプしても集中の画面" "集中タイム中"

log "== 5. 長押しでやめる"
$UI tap "長押しで集中をやめる" >/dev/null; sleep 2
expect_exact "短く押してもやめない" "集中タイム中"
$UI hold "長押しで集中をやめる" 4000 >/dev/null; sleep 3; snap stopped
expect_no_text "長押しでやめられる" "集中タイム中"
z=$(zen); [ "$z" = "0" ] && ok "やめると通知が元に戻る（zen_mode=0）" || ng "やめても通知が止まったまま（zen_mode=$z）"
expect_exact "ホーム画面に戻る（集中タイムの部品が見える）" "集中を始める"
clock; snap clock_after
expect_app "やめた後は時計が使える" deskclock
key KEYCODE_HOME

log "== 6. 重ねて表示の目印は、ホームを呼び出すときだけ（「他のアプリの上に重ねて表示」の通知を出し続けない）"
sleep 8
c=$(anchor); [ "$c" = "0" ] && ok "何もしていない間は目印が無い" || ng "目印が残っている（$c 件）"

log "== 7. 習慣のページ（アプリ一覧の右隣）にチェックと続けた日数"
next_page; next_page; snap habits_page
expect_exact "2回進むと習慣" "習慣"
lang en-US; next_page; next_page
$UI tap "Add" >/dev/null; sleep 1; $UI tapclass android.widget.EditText >/dev/null; sleep 5; softword run; sleep 1; $UI tap "Save" >/dev/null; sleep 2; snap habit_added
expect_exact "習慣が追加される" "run"
expect_text "0時に外れる説明" "0:00"
$UI tap "Mark run as done" >/dev/null; sleep 2; snap habit_checked
expect_exact "チェックすると続けた日数" "1-day streak"
$UI tap "Mark run as not done" >/dev/null; sleep 2
expect_no_text "外すと日数が消える" "1-day streak"
$UI tap "Mark run as done" >/dev/null; sleep 1
lang ja-JP

log "== 8. 表示するページで目標を隠す"
key KEYCODE_HOME
$UI tap "設定" >/dev/null; sleep 2
$UI see "表示するページ" >/dev/null; snap pages_setting
expect_exact "表示するページの設定" "表示するページ"
$UI tap "目標" >/dev/null; sleep 2; snap goals_off
key KEYCODE_HOME
next_page; next_page; expect_exact "アプリ一覧の右は習慣" "習慣"
next_page; snap after_habits; expect_exact "目標を飛ばして日記" "振り返り日記"
key KEYCODE_HOME
$UI tap "設定" >/dev/null; sleep 2; $UI see "表示するページ" >/dev/null; $UI tap "目標" >/dev/null; sleep 2
key KEYCODE_HOME
next_page; next_page; next_page; expect_exact "戻すと目標が出る" "目標"
key KEYCODE_HOME

log "== 9. AIに渡す期間"
prev_page; $UI see "AIに渡す期間" >/dev/null; snap ai_range
expect_exact "期間の見出し" "AIに渡す期間"
expect_exact "1週間" "1週間"; expect_exact "1ヶ月" "1ヶ月"; expect_exact "全期間" "全期間"
$UI tap "1週間" >/dev/null; sleep 1; snap ai_week
key KEYCODE_HOME

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
