#!/bin/bash
# いろいろな言語で表示が崩れないかの確認（アラビア語・ウルドゥー語は右から左）。言語ごとにトップ・今日の調子・質問画面を撮影
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f]"; }
key() { adb shell input keyevent "$@"; sleep 2; }
prev_page() { adb shell input swipe 150 1200 950 1200 400; sleep 2; }
next_page() { adb shell input swipe 950 1200 150 1200 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; key KEYCODE_HOME; sleep 2; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3

# 言語タグ  画面に出るはずの文字（トップの「まずは誕生日と寿命を設定しましょう」に当たるもの）
# adb が標準入力を読んでしまうので、一覧は 3 番から読む
while read -r tag word <&3; do
  [ -z "$tag" ] && continue
  log "== $tag"
  lang $tag; snap "${tag}_top"
  expect_text "$tag のトップ" "$word"
  prev_page; snap "${tag}_condition"
  key KEYCODE_HOME
done 3<<'EOF'
it-IT Prima imposta
th-TH เริ่มจากตั้งวันเกิด
ar-EG أولًا، اضبط
ur-PK پہلے اپنی تاریخ
hi-IN पहले अपनी जन्मतिथि
ru-RU Сначала укажи
tr-TR Önce doğum
bn-BD প্রথমে জন্মতারিখ
ta-IN முதலில் பிறந்த
EOF

lang ja-JP
log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
