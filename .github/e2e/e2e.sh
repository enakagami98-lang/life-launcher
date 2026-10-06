#!/bin/bash
# 仮想スマホで LifeLauncher の動きを確かめる（操作ごとに撮影）
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
page() { for t in 設定 目標を追加 寿命 アプリ 目標 習慣; do $UI has "$t" >/dev/null && { echo "$t"; return; }; done; echo "?"; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top) 画面=$(page)"; }
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
$UI tap "年" >/dev/null; adb shell input text 1999
$UI tap "月" >/dev/null; adb shell input text 4
$UI tap "日" >/dev/null; adb shell input text 29
$UI tap "寿命（歳）" >/dev/null; adb shell input keyevent KEYCODE_MOVE_END KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL; adb shell input text 30
sleep 1; snap settings_typed
key KEYCODE_BACK; snap after_back_closes_keyboard
$UI tap "保存" >/dev/null; sleep 2; snap after_save
EXP=$(python3 -c "import datetime;print('{:,}'.format((datetime.date(2029,4,29)-datetime.date.today()).days))")
expect_text "残り日数（計算上の正解 $EXP）" "$EXP"
expect_text "命日" "2029年4月29日"
expect_text "現在の年齢" "現在 27歳"

log "== 3. アプリ検索"
next_page; snap swipe1; expect_text "1回スワイプでアプリ一覧" "アプリ名で検索"
$UI tap "アプリ名で検索" >/dev/null; sleep 1; adb shell input text Sett; sleep 1; snap search_typed
expect_text "検索結果にSettings" "Settings"
expect_no_text "関係ないアプリは消える" "Chrome"
key KEYCODE_ENTER; sleep 2; snap after_enter
expect_app "Enterで先頭の検索結果(Settings)が開く" com.android.settings

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
next_page; expect_text "1回目→アプリ" "アプリ名で検索"
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
expect_text "アプリ一覧にいる" "アプリ名で検索"; expect_no_text "一覧から消える" "Contacts"
$UI tap "アプリ名で検索" >/dev/null; adb shell input text Cont; sleep 1; snap hidden_search
expect_no_text "検索でも出ない" "Contacts"
key KEYCODE_HOME

log "== 11. 目標の追加"
next_page; next_page; $UI tap "追加" >/dev/null; sleep 1; adb shell input text "TOEIC%s900"; sleep 1; $UI tap "保存" >/dev/null; sleep 1; snap goal_added
expect_text "目標が追加される" "TOEIC 900"
key KEYCODE_HOME

log "== 12. 再起動（電源を入れ直す）"
adb reboot; adb wait-for-device
for i in $(seq 1 60); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 3; done
sleep 20; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 5; snap after_reboot
expect_app "再起動後はLifeLauncher" $PKG; expect_text "設定が残っている" "人生の進捗"
next_page; next_page; expect_text "目標が残っている" "TOEIC 900"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
