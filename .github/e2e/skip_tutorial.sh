#!/bin/bash
# 入れた直後の使い方（チュートリアル）を閉じる。画面に出てくるまで待ってから、消えるまで「スキップ」を押す
UI="python3 .github/e2e/ui.py"
seen=0
for i in $(seq 1 15); do
  if $UI has "スキップ" >/dev/null; then seen=1; $UI tapraw "スキップ" >/dev/null; sleep 3
  elif $UI has "Skip" >/dev/null; then seen=1; $UI tapraw "Skip" >/dev/null; sleep 3
  elif [ $seen = 1 ] || [ $i -gt 6 ]; then break
  else sleep 2
  fi
done
