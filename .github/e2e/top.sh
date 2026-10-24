#!/bin/bash
# トップ画面（日付・残り日数・入れ替えできる部品）、支払い通知の読み取り、「今何してる？」の選択肢の編集、言語の切り替えの確認
PKG=com.enakagami.lifelauncher
OUT=e2e-out; mkdir -p $OUT; R=$OUT/result.txt; : > $R
UI="python3 .github/e2e/ui.py"
N=0
log() { echo "$*" | tee -a $R; }
top() { adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -o '[a-z0-9.]*/[A-Za-z0-9.]*' | head -1; }
snap() { N=$((N+1)); f=$(printf "%02d_%s" $N "$1"); adb exec-out screencap -p > $OUT/$f.png; log "   [撮影 $f] 表示中アプリ=$(top)"; }
key() { adb shell input keyevent "$@"; sleep 2; }
next_page() { adb shell input swipe 950 1200 150 1200 400; sleep 2; }
prev_page() { adb shell input swipe 150 1200 950 1200 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-200)"; fi; }
expect_no_text() { if $UI hasp "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
order() { for t in "$@"; do echo "$($UI y "$t") $t"; done | sort -n | awk '{print $2}' | paste -sd'<'; }
notify() { adb shell "cmd notification post -t '$2' $1 '$3'" >/dev/null; sleep 3; }
unlock() { key KEYCODE_POWER; sleep 3; key KEYCODE_WAKEUP; adb shell wm dismiss-keyguard; sleep 4; }
# 仮想スマホは英語なので、このアプリだけ日本語にする（スマホの言語設定に合わせて切り替わることの確認は最後に）
lang() { adb shell cmd locale set-app-locales $PKG --locales $1 >/dev/null 2>&1; sleep 3; key KEYCODE_HOME; sleep 2; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3
lang ja-JP
bash .github/e2e/skip_tutorial.sh  # 入れた直後の使い方を閉じる

log "== 1. トップ画面の見た目（最初は モットー・今日のスマホ・支払額）"
snap top_default
TODAY=$(python3 -c "import datetime;d=datetime.date.today();print(f'{d.month}月{d.day}日')")
expect_text "左上は今日の日付" "$TODAY"
expect_no_text "命日は無い" "命日"
expect_text "モットーの部品" "タップして、人生のモットーを入力"
expect_text "使用時間の部品" "今日のスマホ"
expect_text "支払額の部品" "今月の支払額"
expect_no_text "タスクは最初は出ない" "今日のタスク"
expect_text "許可のお願い" "通知へのアクセス"
o=$(order タップして、人生のモットーを入力 今日のスマホ 今月の支払額)
[ "$o" = "タップして、人生のモットーを入力<今日のスマホ<今月の支払額" ] && ok "最初の並び ($o)" || ng "最初の並び ($o)"

log "== 2. 並べ替えと表示の切り替え"
$UI tap "表示する項目を編集" >/dev/null; sleep 2; snap edit_dialog
expect_text "部品の候補：カウントダウン" "カウントダウン"
expect_text "部品の候補：天気" "天気"
expect_text "部品の候補：習慣チェック" "今日の習慣チェック"
expect_text "部品の候補：歩数" "歩数"
$UI tapp "昨日のふり返り" >/dev/null; sleep 1
expect_text "部品の候補：昨日のふり返り" "昨日のふり返り"
$UI tap "今日のタスクを表示" >/dev/null; sleep 1
for i in 1 2 3; do $UI tap "今日のタスクを上へ" >/dev/null; sleep 1; done
$UI tap "人生のモットーを表示" >/dev/null; sleep 1
$UI tap "保存" >/dev/null; sleep 2; snap edited
expect_no_text "モットーを非表示" "タップして、人生のモットーを入力"
o=$(order 今日のタスク 今日のスマホ 今月の支払額)
[ "$o" = "今日のタスク<今日のスマホ<今月の支払額" ] && ok "タスクが一番上 ($o)" || ng "並び順 (期待 今日のタスク<今日のスマホ<今月の支払額 / 実際 $o)"
key KEYCODE_HOME
o=$(order 今日のタスク 今日のスマホ 今月の支払額)
[ "$o" = "今日のタスク<今日のスマホ<今月の支払額" ] && ok "ホームに戻っても並びが残る" || ng "並びが戻った ($o)"

log "== 3. 新しい部品"
$UI tap "表示する項目を編集" >/dev/null; sleep 2
for w in カウントダウン 天気 今日の習慣チェック 歩数 昨日のふり返り; do $UI tap "${w}を表示" >/dev/null; sleep 1; done
$UI tap "保存" >/dev/null; sleep 3; snap new_widgets
expect_text "カウントダウン" "タップして、目標の日を登録"
expect_text "習慣チェック" "「習慣」の画面で追加すると"
adb shell input swipe 540 1800 540 700 400; sleep 2; snap new_widgets_lower
expect_text "歩数（許可のお願いかセンサー無し）" "歩"
expect_text "昨日のふり返り" "今日の身体の調子を入力"
expect_text "天気（許可のお願い）" "だいたいの位置"
$UI tap "今日の身体の調子を入力 ›" >/dev/null; sleep 2; snap to_condition
expect_text "ふり返りから身体の調子へ" "睡眠の質"
key KEYCODE_HOME

log "== 4. 支払いの通知から金額を読む"
adb shell cmd notification allow_listener $PKG/$PKG.PaymentListener
sleep 3
notify pay1 "PayPay" "セブン-イレブンで¥1,280を支払いました"
key KEYCODE_HOME; snap after_pay1
expect_text "支払額に反映" "¥1,280"
notify pay2 "PayPay" "田中さんから¥500を受け取りました"
notify pay3 "お得情報" "最大20%還元！1,000円以上の支払いで"
key KEYCODE_HOME
expect_text "受け取り・広告は数えない" "¥1,280"
notify pay4 "三井住友カード" "ご利用のお知らせ ご利用金額：3,500円 AMAZON"
key KEYCODE_HOME; snap after_pay4
expect_text "カードの通知も足される" "¥4,780"
$UI tap "今月の支払額" >/dev/null; sleep 2; snap spend_list
expect_text "一覧に出る" "¥3,500"
$UI tap "¥3,500を消す" >/dev/null; sleep 2
expect_text "消すと合計が減る" "今月の支払額 ¥1,280"
$UI tap "閉じる" >/dev/null; sleep 1
key KEYCODE_HOME

log "== 5. 「今何してる？」の選択肢と編集"
prev_page; $UI tap "今何してるを聞く" >/dev/null; sleep 1
key KEYCODE_HOME; unlock; snap ask_options
for t in 買い物 音楽を聴く ジム 読書 支払い; do expect_text "$t" "$t"; done
$UI tap "選択肢を編集" >/dev/null; sleep 2; snap options_edit
$UI tap "ジムを消す" >/dev/null; sleep 1
key KEYCODE_BACK; snap removed_gym
expect_no_text "消した選択肢は出ない" "ジム"
$UI tap "選択肢を編集" >/dev/null; sleep 2
$UI tap "最初の選択肢に戻す" >/dev/null; sleep 1
key KEYCODE_BACK
expect_text "元に戻すと復活" "ジム"
$UI tap "買い物" >/dev/null; sleep 1; $UI tap "支払い" >/dev/null; sleep 2
expect_no_text "答えると閉じる" "今何してる？"

log "== 6. スマホの言語が英語なら英語で出る"
lang en-US; snap english
expect_text "英語の案内" "First, set your birthday"
expect_text "英語の部品名" "Phone today"
expect_no_text "日本語は出ない" "今日のスマホ"
lang ja-JP
expect_text "日本語に戻る" "今日のスマホ"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
