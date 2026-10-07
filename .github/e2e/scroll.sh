#!/bin/bash
# 寿命画面から下にスクロールしてお気に入りが見えるかを、いろいろな操作の後で20回確かめる
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
log() { echo "$*" | tee -a $R; }
key() { adb shell input keyevent "$@"; sleep 1.5; }
next_page() { adb shell input swipe 950 1200 150 1200 300; sleep 1.2; }
prev_page() { adb shell input swipe 150 1200 950 1200 300; sleep 1.2; }
adb install -r LifeLauncher.apk >/dev/null && log "インストール: $(adb shell dumpsys package $PKG | grep -m1 versionName | tr -d ' ')"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
key KEYCODE_HOME
# お気に入りにChromeを登録
next_page; $UI long "Chrome" >/dev/null; sleep 1; $UI tap "お気に入りに追加" >/dev/null; sleep 1; key KEYCODE_HOME
# いろいろな操作パターン
P=( "home" "n n n n" "p" "n n n n n n n n" "p p p p" "n home" "n n home" "n n n home" "p home" "p p home"
    "n n n n home" "p p p p home" "n p" "n n p p" "n n n n n" "p p p p p" "app" "app home" "lock" "n lock" )
OK=0; NG=0
for i in "${!P[@]}"; do
  for a in ${P[$i]}; do
    case $a in
      n) next_page;; p) prev_page;; home) key KEYCODE_HOME;;
      app) adb shell am start -a android.settings.SETTINGS >/dev/null; sleep 2; key KEYCODE_BACK;;
      lock) key KEYCODE_POWER; sleep 2; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 2;;
    esac
  done
  # 寿命画面に来ていなければホームで戻す
  $UI has "残りの日数" >/dev/null || $UI has "まずは誕生日と寿命を設定しましょう" >/dev/null || key KEYCODE_HOME
  adb shell input swipe 540 1900 540 500 300; sleep 1.5
  if $UI has "Chrome" >/dev/null; then OK=$((OK+1)); log "PASS | パターン$((i+1)) [${P[$i]}] 下にスクロールできた"
  else NG=$((NG+1)); adb exec-out screencap -p > $OUT/ng_$((i+1)).png; log "FAIL | パターン$((i+1)) [${P[$i]}] 下にスクロールできない"; fi
  adb shell input swipe 540 600 540 2000 300; sleep 1
done
log "== 結果: PASS $OK / FAIL $NG"
