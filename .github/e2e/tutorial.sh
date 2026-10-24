#!/bin/bash
# 入れた直後の使い方（チュートリアル）を、実際に設定しながら最後まで進める確認
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f]"; }
# 数字キーボード（Gboard）のキーの位置
declare -A K=( [1]="140 1724" [2]="404 1724" [3]="672 1724" [4]="140 1880" [5]="404 1880" [6]="672 1880" [7]="140 2036" [8]="404 2036" [9]="672 2036" [0]="404 2192" [BS]="944 2036" )
soft() { for ch in "$@"; do adb shell input tap ${K[$ch]}; sleep 1; done; }
softword() { w=$1; for ((i=0;i<${#w};i++)); do soft "${w:$i:1}"; done; }
key() { adb shell input keyevent "$@"; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
next() { $UI tap "$1" >/dev/null; sleep 2; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell cmd locale set-app-locales $PKG --locales ja-JP >/dev/null 2>&1
sleep 3; key KEYCODE_HOME; sleep 3

log "== 1. ようこそ"
snap welcome
expect_text "入れた直後に使い方が出る" "Loosen へようこそ"
expect_text "スキップできる" "スキップ"
next "はじめる"

log "== 2. 寿命"
expect_text "寿命のページ" "あなたの寿命を設定しよう"
$UI tap "年" >/dev/null; sleep 2; softword 1999
$UI tap "月" >/dev/null; sleep 1; softword 4
$UI tap "日" >/dev/null; sleep 1; softword 29
$UI tap "寿命（歳）" >/dev/null; sleep 1; soft BS BS BS; softword 30
key KEYCODE_BACK; snap life
expect_text "残りの日数のメッセージ" "これがあなたに残された日数です"
EXP=$(python3 -c "import datetime;print('{:,}'.format((datetime.date(2029,4,29)-datetime.date.today()).days))")
expect_text "残りの日数（正解 $EXP）" "$EXP"
next "次へ"

log "== 3. ホームアプリ"
expect_text "ホームアプリのページ" "いつでもこの画面が出るようにしよう"
expect_text "ホームアプリになっていることがわかる" "ホームアプリに設定できました"
snap home
next "次へ"

log "== 4. お気に入り"
expect_text "お気に入りのページ" "よく使うアプリを選ぼう"
$UI tap "Chrome" >/dev/null; sleep 1
expect_text "選んだ数" "1個 選択中"
snap favorites
next "次へ"

log "== 5. 使いすぎ防止"
expect_text "使いすぎ防止のページ" "使いすぎてしまうアプリを選ぼう"
$UI tap "Chrome" >/dev/null; sleep 1
expect_text "許可の案内が出る" "次の2つの許可が必要です"
snap limits
next "次へ"

log "== 6. トップに出す情報"
expect_text "情報のページ" "トップ画面に出す情報を選ぼう"
$UI tap "今日の目標を表示" >/dev/null; sleep 1
snap widgets
next "次へ"

log "== 7. 使い方"
expect_text "最後のページ" "準備ができました"
snap done
next "使いはじめる"

log "== 8. 設定した内容がトップ画面に反映されている"
snap top
expect_no_text "使い方が閉じる" "準備ができました"
expect_text "残りの日数" "残りの日数"
expect_text "選んだ部品（今日の目標）" "今日の目標"
adb shell input swipe 540 1900 540 500 400; sleep 2
expect_text "お気に入りに Chrome" "Chrome"
key KEYCODE_HOME

log "== 9. もう一度開いても出ない／設定から見直せる"
key KEYCODE_HOME
expect_no_text "2回目は出ない" "Loosen へようこそ"
$UI tap "設定" >/dev/null; sleep 2
$UI tapp "使い方をもう一度見る" >/dev/null; sleep 2; snap again
expect_text "設定から使い方を開ける" "Loosen へようこそ"
next "スキップ"
expect_no_text "スキップで閉じる" "Loosen へようこそ"
$UI tap "設定" >/dev/null; sleep 2
$UI tapp "使用時間を制限するアプリ" >/dev/null; sleep 1
expect_text "使いすぎ防止に Chrome（1日60分）" "1日 60分"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
