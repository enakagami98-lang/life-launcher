#!/bin/bash
# 仮想スマホで LifeLauncher の動きを確かめる
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
log() { echo "$*" | tee -a $R; }
shot() { adb exec-out screencap -p > $OUT/$1.png; }
focus() { adb shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r'; }
key() { adb shell input keyevent "$@"; sleep 2; }
next_page() { adb shell input swipe 950 1200 120 1200 250; sleep 2; }
prev_page() { adb shell input swipe 120 1200 950 1200 250; sleep 2; }
expect_focus() { # $1=説明 $2=期待するパッケージ
  sleep 2; f=$(focus)
  if echo "$f" | grep -q "$2"; then log "PASS | $1 | $f"; else log "FAIL | $1 | $f"; fi
}
expect_text() { # $1=説明 $2=画面にあるはずの文字
  if $UI has "$2" >/dev/null; then log "PASS | $1 | 「$2」が表示"; else log "FAIL | $1 | 「$2」が無い / 画面: $($UI texts | cut -c1-200)"; fi
}
expect_no_text() {
  if $UI has "$2" >/dev/null; then log "FAIL | $1 | 「$2」が表示されている"; else log "PASS | $1 | 「$2」は出ていない"; fi
}

adb install -r LifeLauncher.apk | tee -a $R
adb shell cmd package set-home-activity $PKG/.MainActivity | tee -a $R
log "HOMEとして登録されたアプリ: $(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"

log "== 1. ホームボタンで LifeLauncher が出るか"
key KEYCODE_HOME; expect_focus "ホームボタン" $PKG; expect_text "寿命画面" "寿命"; shot 01_home

log "== 2. 寿命の設定（1999/4/29生まれ・寿命30歳）"
$UI tap "設定"; sleep 2
$UI tap "年"; adb shell input text 1999
$UI tap "月"; adb shell input text 4
$UI tap "日"; adb shell input text 29
$UI tap "寿命（歳）" ; adb shell input keyevent KEYCODE_MOVE_END; adb shell input keyevent KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL; adb shell input text 30
shot 02a_settings; key KEYCODE_BACK  # キーボードを閉じる
$UI tap "保存"; sleep 2
expect_text "設定後の寿命画面" "人生の進捗"
EXP=$(python3 -c "import datetime;print('{:,}'.format((datetime.date(2029,4,29)-datetime.date.today()).days))")
expect_text "残り日数（期待値 $EXP）" "$EXP"
expect_text "命日" "2029年4月29日"
shot 02b_life

log "== 3. アプリ検索 → Enterで開く"
next_page; expect_text "アプリ一覧" "アプリ名で検索"; shot 03_apps
$UI tap "アプリ名で検索"; sleep 1; adb shell input text Sett; sleep 1; shot 04_search
expect_text "検索結果" "Settings"
key KEYCODE_ENTER; expect_focus "検索からSettingsを起動" com.android.settings; shot 05_opened

log "== 4. 他アプリからホームボタンで戻る"
key KEYCODE_HOME; expect_focus "ホームで戻る" $PKG; expect_text "寿命画面に戻る(アプリ一覧ではない)" "人生の進捗"; expect_no_text "検索欄は空" "Sett"; shot 06_back_home

log "== 5. 他アプリから戻るボタンで戻る"
adb shell am start -a android.settings.SETTINGS >/dev/null; sleep 3
key KEYCODE_BACK; key KEYCODE_BACK; expect_focus "戻るボタンで戻る" $PKG; expect_text "寿命画面" "人生の進捗"; shot 07_back_key

log "== 6. 画面ロック→解除で寿命画面に戻るか（目標画面にいた状態から）"
next_page; next_page; expect_text "目標画面" "目標"
key KEYCODE_POWER; sleep 3; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 3
expect_focus "ロック解除後" $PKG; expect_text "ロック解除後は寿命画面" "人生の進捗"; shot 08_unlock

log "== 7. 4枚で一周するか"
next_page; expect_text "→アプリ" "アプリ名で検索"
next_page; expect_text "→目標" "目標"
next_page; expect_text "→習慣" "習慣"
next_page; expect_text "→寿命に戻る" "人生の進捗"
prev_page; expect_text "左へ戻ると習慣" "習慣"; shot 09_loop
key KEYCODE_HOME

log "== 8. お気に入り追加"
next_page; $UI long "Chrome"; sleep 1; $UI tap "お気に入りに追加"; sleep 1
key KEYCODE_HOME; adb shell input swipe 540 1900 540 300 300; sleep 2
expect_text "寿命画面の下にお気に入り" "Chrome"; shot 10_favorite
$UI tap "Chrome"; expect_focus "お気に入りから起動" com.android.chrome
key KEYCODE_HOME

log "== 9. 非表示"
next_page; $UI long "Contacts"; sleep 1; $UI tap "非表示にする"; sleep 1
expect_no_text "一覧から消える" "Contacts"
$UI tap "アプリ名で検索"; adb shell input text Cont; sleep 1
expect_no_text "検索でも出ない" "Contacts"; shot 11_hidden
key KEYCODE_HOME

log "== 10. 目標の追加"
next_page; next_page; $UI tap "追加"; sleep 1; adb shell input text "TOEIC%s900"; $UI tap "保存"; sleep 1
expect_text "目標が追加される" "TOEIC 900"; shot 12_goal
key KEYCODE_HOME

log "== 11. 再起動（電源を入れ直す）"
adb reboot; adb wait-for-device
for i in $(seq 1 60); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 3; done
sleep 15; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 5
expect_focus "再起動後" $PKG; expect_text "再起動後も寿命画面・設定が残る" "人生の進捗"; expect_text "目標が残っている(寿命画面からは見えないので後で)" "人生の進捗"; shot 13_reboot
next_page; next_page; expect_text "再起動後も目標が残る" "TOEIC 900"

log "== 終了"
grep -c '^PASS' $R | xargs -I{} echo "PASS数: {}" | tee -a $R
grep -c '^FAIL' $R | xargs -I{} echo "FAIL数: {}" | tee -a $R
