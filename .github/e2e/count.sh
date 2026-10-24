#!/bin/bash
# 使用時間を制限するアプリの「今日の使用時間」が、ホーム画面を通さずに開いた分も数えられるかの確認
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f]"; }
key() { adb shell input keyevent "$@"; sleep 2; }
next_page() { adb shell input swipe 950 1200 150 1200 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; key KEYCODE_HOME; sleep 2; }
add_limit() { key KEYCODE_HOME; next_page; $UI long "$1" >/dev/null; sleep 2; $UI tap "使いすぎ防止に追加" >/dev/null; sleep 2; $UI tap "60" >/dev/null; sleep 2; key KEYCODE_HOME; }
open_settings_reminder() { key KEYCODE_HOME; $UI tap "設定" >/dev/null; sleep 2; $UI tapp "使用時間を制限するアプリ" >/dev/null; sleep 1; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3
lang ja-JP
for i in 1 2 3 4 5; do $UI has "スキップ" >/dev/null || break; $UI tapraw "スキップ" >/dev/null; sleep 3; done  # 入れた直後の使い方を閉じる

log "== 1. 名前が Loosen"
adb shell dumpsys notification --noredact | grep -q "Loosen" && ok "通知の名前が Loosen" || ng "通知に Loosen が無い"

log "== 2. 使用状況へのアクセスが無いとき：注意を出す"
add_limit Chrome
open_settings_reminder; snap no_access
expect_text "小さな見出し" "使用時間を制限するアプリ"
expect_text "数えられない注意" "通知や履歴から開いた分は0のまま"

log "== 3. 許可したら、ホーム画面を通さずに開いた分も数える"
adb shell appops set $PKG GET_USAGE_STATS allow
key KEYCODE_HOME
# 時計アプリを（このホーム画面を通さずに）直接2分ちょっと開いておく
adb shell monkey -p com.google.android.deskclock -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 135
key KEYCODE_HOME
add_limit 時計
open_settings_reminder; snap with_access
expect_no_text "注意が消える" "通知や履歴から開いた分は0のまま"
if $UI hasp "今日 2分 使用" >/dev/null || $UI hasp "今日 3分 使用" >/dev/null; then ok "直接開いた時計の使用時間が数えられている"; else ng "時計の使用時間が数えられていない / $($UI texts | paste -sd' ' | cut -c1-250)"; fi

log "== アプリ内の記録（使用時間の読み取り）"
adb logcat -d -s Loosen | tail -5 | tee -a $R
log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
