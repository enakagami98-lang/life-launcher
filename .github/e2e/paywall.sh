#!/bin/bash
# Google Play 版：定期購入が無いときは Loosen の機能を止めて購入画面＋アプリ一覧（スマホは使える）
# 仮想スマホには Play ストアが無いので「Google Play に接続できません」の状態を確かめる
PKG=com.enakagami.loosen
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top)"; }
key() { adb shell input keyevent "$@"; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_app() { sleep 1; t=$(top); if echo "$t" | grep -q "$2"; then ok "$1 ($t)"; else ng "$1 ($t)"; fi; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }

adb install -r Loosen-play.apk >/dev/null && log "インストール成功（Play 版 $PKG）"
adb shell cmd package set-home-activity $PKG/com.enakagami.lifelauncher.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell cmd locale set-app-locales $PKG --locales ja-JP >/dev/null 2>&1
sleep 3; key KEYCODE_HOME; sleep 3

log "== 1. 入れた直後は使い方が先"
expect_text "使い方が出る" "Loosen へようこそ"
$UI tapraw "スキップ" >/dev/null; sleep 3; snap paywall

log "== 2. 定期購入が無いので購入画面"
expect_text "購入画面の見出し" "Loosen を続けるには"
expect_text "3日間無料の案内" "最初の3日間は無料"
expect_text "Play に接続できない案内" "Google Play に接続できません"
expect_text "ほかのホームアプリへ切り替えられる" "ほかのホームアプリに切り替える"
expect_no_text "残りの日数は出ない" "まずは誕生日と寿命を設定"

log "== 3. スマホ自体は使える（アプリ一覧から開ける）"
expect_text "アプリ一覧がある" "アプリ"
$UI tap "Chrome" >/dev/null; sleep 4
expect_app "一覧から Chrome が開く" com.android.chrome
key KEYCODE_HOME
expect_app "ホームボタンで戻る" $PKG
expect_text "戻ると購入画面" "Loosen を続けるには"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
