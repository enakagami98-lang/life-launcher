#!/bin/bash
# 新しいトップ画面（日付・残り日数・入れ替えできる部品）と、支払い通知の読み取りの確認
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

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
key KEYCODE_HOME; sleep 3

log "== 1. トップ画面の見た目"
snap top_default
TODAY=$(python3 -c "import datetime;d=datetime.date.today();w='月火水木金土日'[d.weekday()];print(f'{d.month}月{d.day}日（{w}）')")
expect_text "左上は今日の日付" "$TODAY"
expect_no_text "見出しの「寿命」は無い" "人生の進捗"
expect_no_text "命日は無い" "命日"
expect_text "モットーの部品" "タップして、人生のモットーを入力"
expect_text "使用時間の部品" "今日のスマホ"
expect_text "タスクの部品" "今日のタスク"
expect_text "支払額の部品" "今月の支払額"
expect_text "許可のお願い" "通知へのアクセス"

log "== 2. 並べ替えと非表示"
$UI tap "表示する項目を編集" >/dev/null; sleep 2; snap edit_dialog
$UI tap "今日のタスクを上へ" >/dev/null; sleep 1
$UI tap "今日のタスクを上へ" >/dev/null; sleep 1
$UI tap "人生のモットーを表示" >/dev/null; sleep 1
$UI tap "保存" >/dev/null; sleep 2; snap edited
expect_no_text "モットーを非表示" "タップして、人生のモットーを入力"
o=$(order 今日のタスク 今日のスマホ 今月の支払額)
[ "$o" = "今日のタスク<今日のスマホ<今月の支払額" ] && ok "タスクが一番上 ($o)" || ng "並び順 (期待 今日のタスク<今日のスマホ<今月の支払額 / 実際 $o)"
key KEYCODE_HOME
o=$(order 今日のタスク 今日のスマホ 今月の支払額)
[ "$o" = "今日のタスク<今日のスマホ<今月の支払額" ] && ok "ホームに戻っても並びが残る" || ng "並びが戻った ($o)"

log "== 3. 支払いの通知から金額を読む"
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
expect_text "消すと合計が減る" "今月の支払額  ¥1,280"
$UI tap "閉じる" >/dev/null; sleep 1
key KEYCODE_HOME

log "== 4. 「今何してる？」の選択肢"
prev_page; $UI tap "今何してるを聞く" >/dev/null; sleep 1
key KEYCODE_HOME; unlock; snap ask_options
expect_text "買い物" "買い物"
expect_text "音楽を聴く" "音楽を聴く"
expect_text "ジム" "ジム"
expect_text "読書" "読書"
expect_text "支払い" "支払い"
$UI tap "買い物" >/dev/null; sleep 1; $UI tap "支払い" >/dev/null; sleep 2
expect_no_text "答えると閉じる" "今何してる？"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
