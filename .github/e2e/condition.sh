#!/bin/bash
# お気に入りの長押しドラッグ並べ替え＋「今日の調子」画面の確認
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
favs() { adb shell input swipe 540 1900 540 500 400; sleep 2; }
ok() { log "PASS | $1"; }
ng() { log "FAIL | $1"; }
expect_text() { if $UI hasp "$2" >/dev/null; then ok "$1 「$2」あり"; else ng "$1 「$2」なし / 画面の文字: $($UI texts | paste -sd' ' | cut -c1-150)"; fi; }
expect_no_text() { if $UI has "$2" >/dev/null; then ng "$1 「$2」が出ている"; else ok "$1 「$2」なし"; fi; }
# 並び順（上から）を「A<B<C」の形で返す
order() { for t in "$@"; do echo "$($UI y "$t") $t"; done | sort -n | awk '{print $2}' | paste -sd'<'; }
expect_order() { o=$(order Clock Chrome Settings); if [ "$o" = "$2" ]; then ok "$1 ($o)"; else ng "$1 (期待 $2 / 実際 $o)"; fi; }

adb install -r LifeLauncher.apk >/dev/null && log "インストール成功"
adb shell cmd locale set-app-locales com.enakagami.lifelauncher --locales ja-JP >/dev/null 2>&1  # 仮想スマホは英語なので、このアプリだけ日本語に
adb shell cmd package set-home-activity $PKG/.MainActivity >/dev/null
key KEYCODE_HOME; sleep 3

log "== 1. お気に入りを3つ登録"
next_page; sleep 2
for a in Clock Chrome Settings; do $UI long "$a" >/dev/null; sleep 2; $UI tap "お気に入りに追加" >/dev/null; sleep 2; done
key KEYCODE_HOME; favs; snap favs_before
expect_order "登録した順に並ぶ" "Clock<Chrome<Settings"

log "== 2. 長押しドラッグで並べ替え"
X=300; Y1=$($UI y Settings); Y2=$($UI y Clock)
adb shell input draganddrop $X $Y1 $X $((Y2 - 20)) 1500; sleep 2; snap dragged_settings_top
expect_order "Settingsを一番上へ" "Settings<Clock<Chrome"
Y1=$($UI y Settings); Y2=$($UI y Chrome)
adb shell input draganddrop $X $Y1 $X $((Y2 + 20)) 1500; sleep 2; snap dragged_settings_bottom
expect_order "Settingsを一番下へ" "Clock<Chrome<Settings"
expect_app() { t=$(top); if echo "$t" | grep -q "$2"; then ok "$1 ($t)"; else ng "$1 ($t)"; fi; }
expect_app "ドラッグしてもアプリは開かない" $PKG
key KEYCODE_HOME; favs
expect_order "ホームに戻っても並びが残る" "Clock<Chrome<Settings"

log "== 3. 動かさずに長押し→いつものメニュー"
$UI long "Chrome" >/dev/null; sleep 2; snap sheet_favorite
expect_text "メニューが出る" "名前を変更"
expect_no_text "上へ移動は無くなった" "上へ移動"
key KEYCODE_BACK
$UI tap "Clock" >/dev/null; sleep 3
t=$(top); if echo "$t" | grep -qv "$PKG"; then ok "タップでアプリが開く ($t)"; else ng "タップしても開かない ($t)"; fi
key KEYCODE_HOME

log "== 4. 寿命から右へスワイプで今日の調子"
prev_page; snap condition
expect_text "今日の調子の画面" "今日の調子"
expect_text "睡眠の質" "睡眠の質"
expect_text "集中度" "集中度"
expect_text "食事" "朝ごはん"
$UI tap "8" >/dev/null; sleep 1
$UI tap "＋" >/dev/null; sleep 1; $UI tap "＋" >/dev/null; sleep 1; snap condition_input
expect_text "睡眠時間の入力" "7.5時間"
key KEYCODE_HOME; prev_page
expect_text "ホームに戻っても記録が残る" "7.5時間"

log "== 5. 傾向とAI用の質問文"
$UI tap "質問文をコピー" >/dev/null; snap copied
expect_text "コピーできた" "コピーしました"
expect_text "傾向の案内" "いま 1日"
for i in 1 2 3; do adb shell input swipe 540 600 540 1900 300; sleep 1; done
$UI tap "前の日" >/dev/null; sleep 1; snap yesterday
expect_text "前の日に切り替わる" "昨日"
expect_text "前の日の睡眠時間は未入力" "未入力"
$UI tap "次の日" >/dev/null; sleep 1
expect_text "今日に戻ると記録がある" "7.5時間"

log "== 6. 6枚で一周"
key KEYCODE_HOME
for i in 1 2 3 4 5; do next_page; done
expect_text "5回進むと今日の調子" "今日の調子"
next_page; expect_text "もう1回で寿命に戻る" "まずは誕生日と寿命を設定"

log "== 結果: PASS $(grep -c '^PASS' $R) / FAIL $(grep -c '^FAIL' $R)"
