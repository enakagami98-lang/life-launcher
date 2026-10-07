#!/bin/bash
# 仮想スマホで LifeLauncher の動きを確かめる（操作ごとに撮影）
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
page() { for t in 生年月日と寿命 目標を追加 寿命 アプリ 目標 習慣; do $UI has "$t" >/dev/null && { echo "$t"; return; }; done; echo "?"; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top) 画面=$(page)"; }
# 画面キーボード（Gboard）のキーを指でタップして入力する
declare -A K=( [1]="140 1724" [2]="404 1724" [3]="672 1724" [4]="140 1880" [5]="404 1880" [6]="672 1880" [7]="140 2036" [8]="404 2036" [9]="672 2036" [0]="404 2192" [BS]="944 2036"
  [q]="60 1720" [w]="164 1720" [e]="272 1720" [r]="380 1720" [t]="488 1720" [y]="596 1720" [u]="704 1720" [i]="812 1720" [o]="920 1720" [p]="1028 1720"
  [a]="112 1868" [s]="220 1868" [d]="328 1868" [f]="432 1868" [g]="540 1868" [h]="648 1868" [j]="756 1868" [k]="864 1868" [l]="968 1868"
  [z]="220 2024" [x]="328 2024" [c]="432 2024" [v]="540 2024" [b]="648 2024" [n]="756 2024" [m]="860 2024" [ENTER]="992 2180" [DEL]="1000 2024" )
soft() { for ch in "$@"; do adb shell input tap ${K[$ch]}; sleep 1; done; }
softword() { w=$1; for ((i=0;i<${#w};i++)); do soft "${w:$i:1}"; done; }
key() { adb shell input keyevent "$@"; sleep 2; }
next_page() { adb shell input swipe 950 1200 150 1200 400; sleep 2; }
prev_page() { adb shell input swipe 150 1200 950 1200 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_app() { sleep 1; t=$(top); if echo "$t" | grep -q "$2"; then ok "$1 ($t)"; else ng "$1 ($t)"; fi; }
expect_text() { if $UI has "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | tr '\n' ' ' | cut -c1-150)"; fi; }
expect_no_text() { if $UI has "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
log "ホームアプリに登録: $(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')"

log "== 1. ホームボタン"
key KEYCODE_HOME; expect_app "ホームボタンでLifeLauncher" $PKG; expect_text "寿命画面" "寿命"; snap home

log "== 2. 寿命の設定（1999/4/29生まれ・寿命30歳）"
$UI tap "設定" >/dev/null; sleep 2; snap settings_open
$UI tap "年" >/dev/null; sleep 1; snap keyboard_shown; softword 1999
$UI tap "月" >/dev/null; sleep 1; softword 4
$UI tap "日" >/dev/null; sleep 1; softword 29
$UI tap "寿命（歳）" >/dev/null; sleep 1; soft BS BS BS; softword 30
sleep 1; snap settings_typed
expect_text "年の入力" "1999"; expect_text "寿命の入力" "30"; expect_no_text "エラー表示なし" "生年月日を正しく入力してください（例：1999年4月29日）"
$UI tap "保存" >/dev/null; sleep 2; snap after_save
expect_text "保存後は寿命画面のまま" "人生の進捗"
EXP=$(python3 -c "import datetime;print('{:,}'.format((datetime.date(2029,4,29)-datetime.date.today()).days))")
expect_text "残り日数（計算上の正解 $EXP）" "$EXP"
expect_text "命日" "2029年4月29日"
expect_text "現在の年齢" "現在 27歳"

log "== 2b. ありえない誕生日（199年）は保存できない"
$UI tap "設定" >/dev/null; sleep 2; $UI tap "1999" >/dev/null; sleep 1; soft BS; sleep 1; snap invalid_year
expect_text "エラーが表示される" "生年月日を正しく入力してください（例：1999年4月29日）"
$UI tap "保存" >/dev/null; sleep 2; expect_text "保存されず設定画面のまま" "生年月日と寿命"
$UI tap "閉じる" >/dev/null; sleep 2; expect_text "閉じると元の設定（936日）のまま" "$EXP"

log "== 3. アプリ検索"
next_page; snap swipe1; expect_text "1回スワイプでアプリ一覧" "アプリ"
$UI tapclass android.widget.EditText >/dev/null; sleep 1; snap search_keyboard; softword sett; sleep 1; snap search_typed
expect_text "検索結果にSettings" "Settings"
expect_no_text "関係ないアプリは消える" "Chrome"
soft ENTER; sleep 3; snap after_enter
expect_app "キーボードの決定キーで先頭の検索結果(Settings)が開く" com.android.settings

log "== 4. 他アプリ→ホームボタン"
key KEYCODE_HOME; snap home_from_settings
expect_app "LifeLauncherに戻る" $PKG; expect_text "寿命画面に戻る" "人生の進捗"

log "== 5. 他アプリ→戻るボタン"
next_page; $UI tap "Chrome" >/dev/null; sleep 4; snap chrome_opened
expect_app "一覧タップでChromeが開く" com.android.chrome
for i in 1 2 3 4; do [ "$(top | cut -d/ -f1)" = "$PKG" ] && break; key KEYCODE_BACK; done
snap back_from_chrome
expect_app "戻るボタンでLifeLauncherに戻る" $PKG; expect_text "アプリ一覧ではなく寿命画面" "人生の進捗"

log "== 6. 目標画面にいる状態でロック→解除"
next_page; next_page; snap before_lock; expect_text "目標画面にいる" "目標"
key KEYCODE_POWER; sleep 3; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 3; snap after_unlock
expect_app "解除後はLifeLauncher" $PKG; expect_text "解除後は寿命画面" "人生の進捗"

log "== 7. 目標の入力画面を開いたままホームボタン"
next_page; next_page; $UI tap "追加" >/dev/null; sleep 1; snap goal_dialog_open
key KEYCODE_HOME; snap home_while_dialog
expect_no_text "入力画面が閉じる" "目標を追加"; expect_text "寿命画面" "人生の進捗"

log "== 8. 4枚で一周"
next_page; expect_text "1回目→アプリ" "アプリ"
next_page; expect_text "2回目→目標" "目標"
next_page; expect_text "3回目→習慣" "習慣"
next_page; expect_text "4回目→寿命に戻る" "人生の進捗"; snap loop_back
prev_page; expect_text "逆方向→習慣" "習慣"
key KEYCODE_HOME

log "== 9. お気に入り"
next_page; $UI long "Chrome" >/dev/null; sleep 1; snap long_press_menu; $UI tap "お気に入りに追加" >/dev/null; sleep 1
key KEYCODE_HOME; adb shell input swipe 540 2000 540 400 400; sleep 2; snap favorites
expect_text "寿命画面の下にChrome" "Chrome"
$UI tap "Chrome" >/dev/null; sleep 3; expect_app "お気に入りからChromeが開く" com.android.chrome
key KEYCODE_HOME

log "== 10. 非表示"
next_page; expect_text "一覧にContactsがある" "Contacts"
$UI long "Contacts" >/dev/null; sleep 1; $UI tap "非表示にする" >/dev/null; sleep 1; snap hidden
expect_text "アプリ一覧にいる" "アプリ"; expect_no_text "一覧から消える" "Contacts"
$UI tapclass android.widget.EditText >/dev/null; sleep 1; softword cont; sleep 1; snap hidden_search
expect_text "検索中もアプリ一覧のまま" "アプリ"
expect_no_text "検索でも出ない" "Contacts"
key KEYCODE_HOME

log "== 11. 目標の追加"
next_page; next_page; $UI tap "追加" >/dev/null; sleep 1; $UI tapclass android.widget.EditText >/dev/null; sleep 1; softword toeic; sleep 1; $UI tap "保存" >/dev/null; sleep 2; snap goal_added
expect_text "目標が追加される" "toeic"; expect_text "目標画面のまま" "目標"
key KEYCODE_HOME

log "== 13. 色の変更"
key KEYCODE_HOME
$UI tap "設定" >/dev/null; sleep 2
$UI tap "背景の色" >/dev/null; sleep 2; snap color_dialog
$UI tap "黒" >/dev/null; sleep 1; expect_text "見本の黒を選ぶとカラーコードが#000000" "#000000"
$UI tap "決定" >/dev/null; sleep 1
$UI tap "アクセントの色（数字・ボタン）" >/dev/null; sleep 2
$UI tap "色の四角" >/dev/null; sleep 1; snap color_square_tapped
if $UI has "#000000" >/dev/null || $UI has "#1043E5" >/dev/null; then ng "四角をタップすると色が変わる"; else ok "四角をタップすると色が変わる ($($UI texts | grep -o '#[0-9A-F]\{6\}' | head -1))"; fi
$UI tap "オレンジ" >/dev/null; sleep 1; expect_text "見本のオレンジ" "#F28C28"
$UI tap "決定" >/dev/null; sleep 1
$UI tap "閉じる" >/dev/null; sleep 2; snap dark_theme
adb exec-out screencap -p > $OUT/px.png
PX=$(python3 -c "from PIL import Image; im=Image.open('$OUT/px.png').convert('RGB'); print(im.getpixel((20,1000)))")
if [ "$PX" = "(0, 0, 0)" ]; then ok "背景が黒になった $PX"; else ng "背景が黒になった $PX"; fi
next_page; snap dark_apps; prev_page

log "== 14. アプリリマインダーの設定（Chrome・1日5分・待ち5秒）"
adb shell appops set $PKG GET_USAGE_STATS allow
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME
$UI tap "設定" >/dev/null; sleep 2
$UI tap "＋ アプリを追加" >/dev/null; sleep 2; snap app_picker
$UI tap "Chrome" >/dev/null; sleep 2
$UI tap "1日 30分 ▾" >/dev/null; sleep 1; $UI tap "5分" >/dev/null; sleep 1
expect_text "Chromeが1日5分で登録" "1日 5分 ▾"
$UI tap "10秒" >/dev/null; sleep 1; snap reminder_settings
$UI tap "閉じる" >/dev/null; sleep 2

log "== 15. 履歴・通知のつもりで直接Chromeを開く → 待ち画面に戻される"
adb shell am start -n com.android.chrome/com.google.android.apps.chrome.Main >/dev/null; sleep 5; snap bypass
expect_app "直接開いてもLifeLauncherに戻される" $PKG
expect_text "待ち画面" "Chromeを本当に開きますか？"
sleep 10; snap choose_minutes; expect_text "待つと時間の選択肢が出る" "Chromeにどのくらいの時間を使いますか？"
$UI tap "5" >/dev/null; T0=$(date +%s)
PILL=0; BAR=0
for i in $(seq 1 8); do W=$(adb shell dumpsys window windows); echo "$W" | grep -q LifeLauncherPill && PILL=1; echo "$W" | grep -q LifeLauncherBar && BAR=1; [ $i = 3 ] && snap chrome_with_pill; sleep 1; done
expect_app "5分を選ぶとChromeが開く" com.android.chrome
[ $PILL = 1 ] && ok "画面下に「残り5分」のお知らせが出た" || ng "「残り5分」のお知らせが出ない"
[ $BAR = 1 ] && ok "画面最下部に進捗バーが出た" || ng "進捗バーが出ない"

log "== 16. 1日の残り1分のお知らせ → 時間切れ"
# 使用開始から約4分で「残り1分」のお知らせが出るはず
while [ $(( $(date +%s) - T0 )) -lt 215 ]; do sleep 5; done
PILL=0; while [ $(( $(date +%s) - T0 )) -lt 300 ]; do adb shell dumpsys window windows | grep -q LifeLauncherPill && { PILL=1; snap one_minute_pill; break; }; sleep 1; done
[ $PILL = 1 ] && ok "「残り1分」のお知らせが出た（開始から$(( $(date +%s) - T0 ))秒）" || ng "「残り1分」のお知らせが出ない"
while [ $(( $(date +%s) - T0 )) -lt 330 ]; do sleep 5; done; snap time_over
expect_app "時間切れでLifeLauncherに戻される" $PKG
if $UI hasp "使い切りました" >/dev/null; then ok "「使い切りました」が表示"; else ng "「使い切りました」が無い / $($UI texts | paste -sd" " | cut -c1-150)"; fi
$UI tap "閉じる" >/dev/null; sleep 1
adb shell am start -n com.android.chrome/com.google.android.apps.chrome.Main >/dev/null; sleep 6
expect_app "使い切った後に直接開いても戻される" $PKG; snap reopen_after_limit
$UI tap "閉じる" >/dev/null

log "== 17. アプリを開いたまま画面を消す → つけると寿命画面"
adb shell am start -a android.settings.SETTINGS >/dev/null; sleep 3
key KEYCODE_POWER; sleep 3; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 3; snap screen_off_from_app
expect_app "設定アプリを開いたまま消しても寿命画面から" $PKG

log "== 18. 使えない時間帯（今の時刻を含む夜の時間を設定）"
H=$(adb shell date +%H | sed "s/[^0-9]//g"); E=$(printf "%02d" $(( (10#$H + 2) % 24 )))
log "   端末の時刻 $(adb shell date +%H:%M | sed "s/[^0-9:]//g") → 夜を ${H}:00〜${E}:00 にする"
key KEYCODE_HOME
$UI tap "設定" >/dev/null; sleep 2
$UI tap "23:00" >/dev/null; sleep 2; snap time_dialog
$UI tap "$((10#$H))時" >/dev/null; $UI tap "00分" >/dev/null; sleep 1; snap time_picked
$UI tap "決定" >/dev/null; sleep 1
$UI tap "06:00" >/dev/null; sleep 2
$UI tap "$((10#$E))時" >/dev/null; $UI tap "00分" >/dev/null; sleep 1
$UI tap "決定" >/dev/null; sleep 1; snap night_times
expect_text "開始時刻" "${H}:00"; expect_text "終了時刻" "${E}:00"
$UI tap "夜をオンにする" >/dev/null; sleep 3; snap locked
expect_text "ロック画面が出る" "夜のおやすみ時間"
expect_text "解除時刻" "${E}:00 まではスマホを使えません"
next_page; expect_text "スワイプしてもロック画面のまま" "夜のおやすみ時間"
adb shell am start -a android.settings.SETTINGS >/dev/null; sleep 5
expect_app "設定アプリは使える" com.android.settings
adb shell am start -n com.google.android.deskclock/com.android.deskclock.DeskClock >/dev/null 2>&1 || adb shell monkey -p com.google.android.deskclock 1 >/dev/null 2>&1; sleep 6; snap clock_blocked
expect_app "時計アプリは閉じられる" $PKG

log "== 19. 再起動しても設定が残る（使えない時間帯のまま）"
adb reboot; adb wait-for-device
for i in $(seq 1 60); do [ "$(adb shell getprop sys.boot_completed | tr -d '[:space:]')" = "1" ] && break; sleep 3; done
sleep 20; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 5; snap after_reboot
expect_app "再起動後はLifeLauncher" $PKG; expect_text "再起動後もロック画面" "夜のおやすみ時間"
adb exec-out screencap -p > $OUT/px2.png
PX=$(python3 -c "from PIL import Image; im=Image.open('$OUT/px2.png').convert('RGB'); print(im.getpixel((20,1000)))")
[ "$PX" = "(0, 0, 0)" ] && ok "再起動後も背景は黒 $PX" || ng "再起動後も背景は黒 $PX"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
