#!/bin/bash
# 今日の言葉・振り返り日記（キーボードで入力欄が隠れないか）・日めくりの確認（2026-11-04）
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
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 3; }
to_tasks_en() { for i in 1 2 3 4 5 6 7; do $UI has "Tasks" >/dev/null && $UI has "Add" >/dev/null && return; next_page; done; }
to_journal() { key KEYCODE_HOME; for i in 1 2 3 4; do next_page; done; }
# 入力中の欄の書き始め（上端から1行分）がキーボードより上に見えているか。
# キーボードの上端は Pixel 6 の英字キーボード（候補の帯つき）で約1515（撮影画像で確認、2026-11-04）
KB_TOP=1515
check_field() {
  sleep 4; t=$($UI focused); snap "$2"
  if [ "$t" -gt 0 ] && [ $((t + 120)) -le $KB_TOP ]; then ok "$1 入力欄の上端 $t（キーボードは $KB_TOP から）"; else ng "$1 入力欄の上端 $t / キーボード $KB_TOP"; fi
}

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd locale set-app-locales $PKG --locales ja-JP >/dev/null 2>&1
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
sleep 3; adb shell input keyevent KEYCODE_HOME; sleep 3; bash .github/e2e/skip_tutorial.sh
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3

log "== 1. トップに今日の言葉、日記は習慣とタスクの間のページ"
snap top_ja
expect_text "今日の言葉の部品" "今日の言葉"
$UI see "振り返り日記" >/dev/null
expect_no_text "トップに日記の部品は出ない" "振り返り日記"
key KEYCODE_HOME
for i in 1 2 3; do next_page; done; expect_text "3回進むと習慣" "習慣"
next_page; snap journal_page; expect_text "その次が日記" "振り返り日記"
next_page; expect_text "その次がタスク" "タスク"
lang en-US

log "== 2. 今日の言葉を登録する"
$UI tap "Today's words" >/dev/null; sleep 2; snap quote_empty
expect_text "空のときの説明" "one shows here each day"
$UI tap "＋ Add new" >/dev/null; sleep 2
$UI tapbelow "Words" android.widget.EditText >/dev/null; check_field "言葉の欄" quote_text_kb; softword abc
$UI tapbelow "Source (book, show, etc.)" android.widget.EditText >/dev/null; check_field "出どころの欄" quote_source_kb; softword bac
$UI tapbelow "Context / notes" android.widget.EditText >/dev/null; check_field "メモの欄（一番下）" quote_note_kb; softword cab
$UI tap "Save" >/dev/null; sleep 2; snap quote_saved
expect_text "今日の言葉に出る" "“abc”"
expect_text "出どころが見える" "bac"
expect_text "前後の文脈が見える" "cab"
expect_text "登録した言葉の一覧" "Saved words (1)"
key KEYCODE_BACK; snap quote_top
expect_text "トップに今日の言葉" "“abc”"
expect_text "トップに出どころ" "— bac"

log "== 3. 振り返り日記を書く（キーボードと入力欄が重ならないか）"
to_journal; snap journal_empty
expect_text "義務ではない説明" "No need to write every day"
expect_text "まだ無い" "No entries yet"
$UI tap "Write today's journal" >/dev/null; sleep 2; snap journal_editor
$UI tapbelow "What happened and what you thought" android.widget.EditText >/dev/null; check_field "最初の欄" journal_first_kb; softword abc
$UI tapbelow "Music I've been listening to" android.widget.EditText >/dev/null; check_field "途中の欄" journal_mid_kb; softword cab
$UI tapbelow "What I challenged myself to do" android.widget.EditText >/dev/null; check_field "一番下の欄" journal_last_kb; softword xyz
snap journal_typed
$UI tap "Done" >/dev/null; sleep 2; snap journal_view
expect_text "書いた内容が見られる" "xyz"
expect_text "欄の見出し" "What I challenged myself to do"
expect_no_text "空の欄は出さない" "What made me happy"
key KEYCODE_BACK; snap journal_list
expect_text "これまでの日記に出る" "abc"
expect_text "日数" "1 entries"
expect_text "ページは「書きました」" "You wrote today's journal"

log "== 4. 書いている途中でホームに戻っても、書いた分は残る"
$UI tap "Open today's journal" >/dev/null; sleep 2
$UI tapbelow "Things I noticed" android.widget.EditText >/dev/null; sleep 4; softword ab
to_journal
$UI tap "abc" >/dev/null; sleep 2; snap journal_kept
expect_text "途中まで書いた欄が残る" "Things I noticed"
key KEYCODE_BACK; key KEYCODE_BACK

log "== 5. 日めくり（その日に初めてタスクを登録したとき）"
MONTH=$(adb shell date +%B | tr -dc 'A-Za-z')
to_tasks_en; $UI tap "Add" >/dev/null; sleep 2; $UI tapclass android.widget.EditText >/dev/null; sleep 4; softword abc
# 日めくりは約2秒で消えるので、撮影より先に文字を調べる
$UI tap "Save" >/dev/null; expect_text "日めくりが出る（誕生日が未設定なので月の名前）" "$MONTH"; snap tear
sleep 5; snap tear_end
expect_no_text "少しすると消える" "$MONTH"
$UI tap "Add" >/dev/null; sleep 2; $UI tapclass android.widget.EditText >/dev/null; sleep 4; softword xyz
$UI tap "Save" >/dev/null; sleep 0.5; snap tear_second
expect_no_text "2つ目のタスクでは出ない（1日1回）" "$MONTH"

log "== 6. 日本語の表示"
lang ja-JP; for i in 1 2 3 4; do next_page; done; snap journal_ja
expect_text "日本語で「書きました」" "今日の日記を書きました"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
