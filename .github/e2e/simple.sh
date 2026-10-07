#!/bin/bash
# 使用状況データの許可なし（簡易モード）＋長押しパネルの確認
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top)"; }
key() { adb shell input keyevent "$@"; sleep 2; }
next_page() { adb shell input swipe 950 1200 150 1200 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_app() { sleep 1; t=$(top); if echo "$t" | grep -q "$2"; then ok "$1 ($t)"; else ng "$1 ($t)"; fi; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-150)"; fi; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
log "使用状況へのアクセス: $(adb shell appops get $PKG GET_USAGE_STATS | tr -d '\r')（許可しない）"
key KEYCODE_HOME; sleep 3

log "== 1. 長押しパネル"
next_page; sleep 2
$UI long "Chrome" >/dev/null; sleep 2; snap sheet_app
expect_text "パネルに使いすぎ防止" "使いすぎ防止に追加"
expect_text "パネルにお気に入り" "お気に入りに追加"
expect_text "パネルに非表示" "非表示にする"
$UI tap "使いすぎ防止に追加" >/dev/null; sleep 2; snap sheet_limit
expect_text "上限の選択肢" "1日に使ってよい時間"
$UI tap "5" >/dev/null; sleep 2
$UI long "Chrome" >/dev/null; sleep 2; snap sheet_limited
expect_text "登録済みの表示" "使いすぎ防止：1日 5分"
expect_text "外す項目" "使いすぎ防止から外す"
key KEYCODE_BACK
$UI long "Clock" >/dev/null; sleep 2; $UI tap "お気に入りに追加" >/dev/null; sleep 2
key KEYCODE_HOME; adb shell input swipe 540 1900 540 500 400; sleep 2
expect_text "お気に入りに追加された" "Clock"
$UI long "Clock" >/dev/null; sleep 2; snap sheet_favorite
expect_text "お気に入りのパネル" "名前を変更"
key KEYCODE_BACK; key KEYCODE_HOME

log "== 2. 簡易モード：ホームから開くと待ち画面"
next_page; sleep 1; $UI tap "Chrome" >/dev/null; sleep 3; snap wait_dialog
expect_text "待ち画面" "Chromeを本当に開きますか？"
sleep 9; $UI tap "5" >/dev/null; T0=$(date +%s)
PILL=0; BAR=0
for i in $(seq 1 10); do W=$(adb shell dumpsys window windows); echo "$W" | grep -q LifeLauncherPill && PILL=1; echo "$W" | grep -q LifeLauncherBar && BAR=1; [ $i = 3 ] && snap chrome_open; sleep 1; done
expect_app "Chromeが開く" com.android.chrome
[ $PILL = 1 ] && ok "「残り5分」のお知らせ" || ng "「残り5分」のお知らせが出ない"
[ $BAR = 1 ] && ok "進捗バー" || ng "進捗バーが出ない"

log "== 3. 簡易モード：時間切れでホームに戻される"
while [ $(( $(date +%s) - T0 )) -lt 215 ]; do sleep 5; done
PILL=0; while [ $(( $(date +%s) - T0 )) -lt 300 ]; do adb shell dumpsys window windows | grep -q LifeLauncherPill && { PILL=1; snap one_minute; break; }; sleep 1; done
[ $PILL = 1 ] && ok "「残り1分」のお知らせ（開始から$(( $(date +%s) - T0 ))秒）" || ng "「残り1分」のお知らせが出ない"
while [ $(( $(date +%s) - T0 )) -lt 320 ]; do sleep 5; done; snap time_over
expect_app "時間切れでLifeLauncherに戻される" $PKG
expect_text "使い切り" "使い切りました"
$UI tap "閉じる" >/dev/null; sleep 1

log "== 4. 簡易モード：使えない時間帯"
H=$(adb shell date +%H | sed "s/[^0-9]//g"); E=$(printf "%02d" $(( (10#$H + 2) % 24 )))
key KEYCODE_HOME; $UI tap "設定" >/dev/null; sleep 2
$UI tap "23:00" >/dev/null; sleep 2; $UI tap "$((10#$H))時" >/dev/null; $UI tap "00分" >/dev/null; $UI tap "決定" >/dev/null; sleep 1
$UI tap "06:00" >/dev/null; sleep 2; $UI tap "$((10#$E))時" >/dev/null; $UI tap "00分" >/dev/null; $UI tap "決定" >/dev/null; sleep 1
snap settings_simple
$UI tap "夜をオンにする" >/dev/null; sleep 3
expect_text "ロック画面" "夜のおやすみ時間"
adb shell am start -a android.settings.SETTINGS >/dev/null; sleep 6; snap blocked
expect_app "他のアプリは閉じられる" $PKG
$UI tap "電話をかける" >/dev/null; sleep 8; snap dialer
t=$(top); if echo "$t" | grep -qv "$PKG"; then ok "電話アプリは使える ($t)"; else ng "電話アプリも閉じられた ($t)"; fi

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
