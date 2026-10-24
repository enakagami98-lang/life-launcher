# 「身体の調子」・睡眠の時刻・お酒・アプリ別の円グラフ・今日の目標の達成・AIへの質問文の文字（2026-10-24）。
# tutorial.py と同じく、言語ごとに全部のキーを持つ。base.py・tr_xx.py に同じキーがあれば、こちらが優先される
KEYS = ["this_month_so_far", "c_bed", "c_wake", "c_time_clear", "c_bedphone", "c_bedphone_none", "c_bedphone_unknown",
        "c_alcohol", "alc_no", "alc_yes", "tr_alcohol", "tr_bedphone", "color_cream", "color_indigo",
        "pie_title", "pie_other", "pie_need", "pie_none", "pie_note",
        "goal_done", "goal_done_btn", "goal_undo", "goal_edit_btn", "congrats_title", "congrats_body",
        "page_condition", "c_overall", "c_overall_hint", "st_note", "tr_wait", "tr_good", "tr_note",
        "ai_unlock_section", "ai_prompt"]

D = {}

D["ja"] = {
    "this_month_so_far": "今月（昨日まで）",
    "c_bed": "昨夜寝た時刻", "c_wake": "今朝起きた時刻", "c_time_clear": "未入力に戻す",
    "c_bedphone": "寝る前30分のスマホ：%1$s 使っていました",
    "c_bedphone_none": "寝る前30分のスマホ：使っていません",
    "c_bedphone_unknown": "寝る前30分のスマホ：分かりません（「使用状況へのアクセス」の許可が必要です。スマホ本体の記録は1週間ほどで消えます）",
    "c_alcohol": "お酒", "alc_no": "なし", "alc_yes": "あり",
    "tr_alcohol": "お酒あり", "tr_bedphone": "寝る前スマホあり",
    "color_cream": "薄黄色", "color_indigo": "紺色",
    "pie_title": "今日のアプリ別の使用時間", "pie_other": "その他",
    "pie_need": "アプリごとの時間を見るには、「使用状況へのアクセス」の許可が必要です。",
    "pie_none": "今日はまだ1分以上使ったアプリがありません。",
    "pie_note": "スマホ本体の使用記録（Digital Wellbeing と同じ元データ）から数えています。このホーム画面の時間は含みません。",
    "goal_done": "達成", "goal_done_btn": "達成した！", "goal_undo": "達成を取り消す", "goal_edit_btn": "目標を書き直す",
    "congrats_title": "Congratulations!", "congrats_body": "今日の目標を達成しました。よくがんばりました！",
    "page_condition": "身体の調子", "c_overall": "身体の調子", "c_overall_hint": "体の元気さ・疲れにくさ",
    "st_note": "画面をつけてロックを解除していた時間です。「使用状況へのアクセス」を許可していれば、スマホ本体の記録（直近1週間）でも補います。",
    "tr_wait": "「身体の調子・集中度・気分」を5日分つけると、調子がいい日とそうでない日の違いがここに出ます（いま %1$d日）。",
    "tr_good": "平均 7以上\n(%1$d日)",
    "tr_note": "※「身体の調子・集中度・気分」の平均で、調子がいい日を分けています。食事の中身まで含めた分析は、下の「AIに分析してもらう」がおすすめ。",
    "ai_unlock_section": "# スマホを開いたときの記録（直近14日。ロック解除のたびに答えたもの）\n日付,時刻,そのとき何をしていたか,スマホを開いた理由\n%1$s",
    "ai_prompt": """あなたは生活習慣とコンディション管理にくわしいコーチです。
以下は、私が毎日スマホで記録しているデータです（%1$d日分、%2$s 〜 %3$s）。私が「どういう時に調子がいいのか」を分析してください。

# 記録の項目
- 身体の調子・集中度・気分・睡眠の質：0〜10の自己評価（10が最高）。調子がいい日かどうかは「身体の調子・集中度・気分」の平均で判断してください
- 睡眠時間：前の晩に眠った時間（時間）。就寝・起床：前の晩に寝た時刻と、その日の朝に起きた時刻
- 寝る前スマホ(分)：寝る前の30分間にスマホを使っていた時間
- スマホ(分)：その日にスマホの画面をつけて使っていた時間。解除回数：ロックを解除した回数
- お酒：1＝飲んだ、0＝飲んでいない
- 今日の目標・達成：その日の目標と、達成できたか（1＝達成）
- 朝・昼・夜・メモ：食べたもの、運動・出来事など
- 空欄は「記録していない」という意味です（0点ではありません）

# 参考：私の目標と習慣
- 目標：%4$s
- 習慣：%5$s

# データ（CSV）
日付,曜日,身体の調子,集中度,気分,睡眠の質,睡眠時間,就寝,起床,寝る前スマホ(分),スマホ(分),解除回数,お酒,今日の目標,達成,朝,昼,夜,メモ
%6$s

# お願いしたいこと
1. 調子がいい日と悪い日で、何が違うか（睡眠・寝る時刻・寝る前のスマホ・お酒・食事・スマホの使用時間・曜日など）。できるだけ具体的な数字で示してください
2. 「こういう時に調子がいい」というパターンを3つまで
3. 調子を下げていそうな習慣や組み合わせ（スマホを開いた記録があれば、無駄になっていそうな時間帯・理由も）
4. 明日から試せる、小さく始められる行動を3つ
5. データが少ない・偏っているなど注意すべき点と、今後追加で記録するとよい項目

# 答え方
- 最初に結論を3行でまとめ、そのあと詳しく説明してください
- %7$sで、専門用語を使わずにわかりやすく
- データから言えることと推測は分けて書いてください
- 医学的な診断はしないでください""",
}

D["en"] = {
    "this_month_so_far": "This month (through yesterday)",
    "c_bed": "Went to bed (last night)", "c_wake": "Woke up (this morning)", "c_time_clear": "Clear",
    "c_bedphone": "Phone in the 30 min before bed: %1$s",
    "c_bedphone_none": "Phone in the 30 min before bed: not used",
    "c_bedphone_unknown": "Phone in the 30 min before bed: unknown (needs “Usage access”; the phone keeps its records for about a week)",
    "c_alcohol": "Alcohol", "alc_no": "No", "alc_yes": "Yes",
    "tr_alcohol": "Drank alcohol", "tr_bedphone": "Phone before bed",
    "color_cream": "Light yellow", "color_indigo": "Indigo",
    "pie_title": "Today's time by app", "pie_other": "Other",
    "pie_need": "To see time per app, allow “Usage access”.",
    "pie_none": "No app has been used for 1 minute or more today yet.",
    "pie_note": "Counted from your phone's usage records (the same data as Digital Wellbeing). Time on this home screen isn't included.",
    "goal_done": "Done", "goal_done_btn": "I did it!", "goal_undo": "Mark as not done", "goal_edit_btn": "Edit goal",
    "congrats_title": "Congratulations!", "congrats_body": "You achieved today's goal. Great job!",
    "page_condition": "Condition", "c_overall": "Body", "c_overall_hint": "How energetic you felt",
    "st_note": "Time with the screen on and unlocked. With “Usage access” allowed, it's also filled in from your phone's records (last 7 days).",
    "tr_wait": "Log body, focus and mood for 5 days to see what's different on your good days (%1$d so far).",
    "tr_good": "Avg 7 or higher\n(%1$d days)",
    "tr_note": "* Good days are split by the average of body, focus and mood. For a deeper look including meals, try “Ask AI” below.",
    "ai_unlock_section": "# Why I opened my phone (last 14 days, answered at each unlock)\ndate,time,what I was doing,why I opened my phone\n%1$s",
    "ai_prompt": """You are a coach who knows a lot about daily habits and well-being.
Below is the data I record on my phone every day (%1$d days, %2$s to %3$s). Please analyze when I tend to feel my best.

# Fields
- body, focus, mood, sleep_quality: self-rated 0–10 (10 is best). Judge good days by the average of body, focus and mood
- sleep_h: hours slept the night before. bedtime, wake_up: when I went to bed the night before and woke up that morning
- phone_before_bed: minutes I used my phone in the 30 minutes before going to bed
- phone_min: minutes the screen was on and in use that day. unlocks: number of unlocks
- alcohol: 1 = drank alcohol, 0 = none
- goal, achieved: that day's goal and whether I achieved it (1 = achieved)
- breakfast, lunch, dinner, notes: what I ate, exercise, events, etc.
- Empty means "not recorded" (not zero)

# For reference: my goals and habits
- Goals: %4$s
- Habits: %5$s

# Data (CSV)
date,weekday,body,focus,mood,sleep_quality,sleep_h,bedtime,wake_up,phone_before_bed,phone_min,unlocks,alcohol,goal,achieved,breakfast,lunch,dinner,notes
%6$s

# What I'd like
1. What differs between good and bad days (sleep, bedtime, phone use before bed, alcohol, meals, screen time, weekday, etc.), with concrete numbers where possible
2. Up to 3 patterns of "I feel good when..."
3. Habits or combinations that seem to drag me down (if there are phone-unlock records, also times and reasons that look wasteful)
4. 3 small actions I can try starting tomorrow
5. Caveats (little or skewed data, etc.) and items worth recording in the future

# How to answer
- Start with a 3-line summary, then explain in detail
- Answer in %7$s, in plain language without jargon
- Separate what the data shows from guesses
- Do not give a medical diagnosis""",
}

D["zh_CN"] = {
    "this_month_so_far": "本月（截至昨天）",
    "c_bed": "昨晚入睡时间", "c_wake": "今早起床时间", "c_time_clear": "清除",
    "c_bedphone": "睡前30分钟用手机：%1$s",
    "c_bedphone_none": "睡前30分钟用手机：没有使用",
    "c_bedphone_unknown": "睡前30分钟用手机：无法得知（需要“使用情况访问权限”；手机的记录大约保留一周）",
    "c_alcohol": "饮酒", "alc_no": "没有", "alc_yes": "有",
    "tr_alcohol": "有饮酒", "tr_bedphone": "睡前用手机",
    "color_cream": "淡黄色", "color_indigo": "藏青色",
    "pie_title": "今天各应用的使用时间", "pie_other": "其他",
    "pie_need": "要查看各应用的时间，需要允许“使用情况访问权限”。",
    "pie_none": "今天还没有使用超过 1 分钟的应用。",
    "pie_note": "根据手机本身的使用记录（与“数字健康”相同的数据）统计。不包括本主屏幕的时间。",
    "goal_done": "已达成", "goal_done_btn": "我做到了！", "goal_undo": "取消达成", "goal_edit_btn": "修改目标",
    "congrats_title": "恭喜你！", "congrats_body": "你完成了今天的目标。做得很好！",
    "page_condition": "身体状态", "c_overall": "身体状态", "c_overall_hint": "身体有多有精神、是否容易累",
    "st_note": "屏幕开启且已解锁的时间。若允许“使用情况访问权限”，还会用手机本身的记录（最近一周）补全。",
    "tr_wait": "记录 5 天的身体状态、专注度和心情后，这里会显示状态好的日子有什么不同（目前 %1$d 天）。",
    "tr_good": "平均 7 以上\n(%1$d 天)",
    "tr_note": "※ 按身体状态、专注度和心情的平均分区分状态好的日子。想连饮食内容一起分析，推荐使用下方的“请 AI 分析”。",
    "ai_unlock_section": "# 打开手机时的记录（最近 14 天，每次解锁时的回答）\n日期,时间,当时在做什么,打开手机的原因\n%1$s",
    "ai_prompt": """你是一位精通生活习惯和身心状态管理的教练。
以下是我每天在手机上记录的数据（%1$d 天，%2$s 至 %3$s）。请分析我在什么时候状态最好。

# 记录项目
- 身体状态、专注度、心情、睡眠质量：0～10 的自我评分（10 为最好）。请用身体状态、专注度和心情的平均分判断状态好的日子
- 睡眠时长：前一晚睡了几个小时。入睡、起床：前一晚入睡的时间和当天早上起床的时间
- 睡前手机(分钟)：睡前 30 分钟内使用手机的时间
- 手机(分钟)：当天屏幕开启并使用的时间。解锁次数：解锁的次数
- 饮酒：1＝喝了，0＝没喝
- 当天目标、达成：当天的目标以及是否达成（1＝达成）
- 早餐、午餐、晚餐、备注：吃了什么、运动、发生的事等
- 空白表示"没有记录"（不是 0 分）

# 参考：我的目标和习惯
- 目标：%4$s
- 习惯：%5$s

# 数据（CSV）
日期,星期,身体状态,专注度,心情,睡眠质量,睡眠时长,入睡,起床,睡前手机(分钟),手机(分钟),解锁次数,饮酒,当天目标,达成,早餐,午餐,晚餐,备注
%6$s

# 希望你做的事
1. 状态好的日子和不好的日子有什么不同（睡眠、入睡时间、睡前用手机、饮酒、饮食、手机使用时间、星期等），尽量用具体数字说明
2. 最多 3 个"我在这种时候状态好"的规律
3. 似乎让状态变差的习惯或组合（如有打开手机的记录，也请指出看起来浪费的时间段和原因）
4. 从明天起就能尝试的 3 个小行动
5. 需要注意的地方（数据少或有偏差等），以及今后值得增加记录的项目

# 回答方式
- 先用 3 行总结结论，再详细说明
- 请用%7$s回答，通俗易懂，不用专业术语
- 把数据能说明的事和推测分开写
- 请不要做医学诊断""",
}

D["zh_TW"] = {
    "this_month_so_far": "本月（截至昨天）",
    "c_bed": "昨晚就寢時間", "c_wake": "今早起床時間", "c_time_clear": "清除",
    "c_bedphone": "睡前 30 分鐘用手機：%1$s",
    "c_bedphone_none": "睡前 30 分鐘用手機：沒有使用",
    "c_bedphone_unknown": "睡前 30 分鐘用手機：無法得知（需要「使用情形存取權」；手機的紀錄大約保留一週）",
    "c_alcohol": "飲酒", "alc_no": "沒有", "alc_yes": "有",
    "tr_alcohol": "有飲酒", "tr_bedphone": "睡前用手機",
    "color_cream": "淡黃色", "color_indigo": "深藍色",
    "pie_title": "今天各應用程式的使用時間", "pie_other": "其他",
    "pie_need": "要查看各應用程式的時間，需要允許「使用情形存取權」。",
    "pie_none": "今天還沒有使用超過 1 分鐘的應用程式。",
    "pie_note": "根據手機本身的使用紀錄（與「數位健康」相同的資料）計算。不包含本主畫面的時間。",
    "goal_done": "已達成", "goal_done_btn": "我做到了！", "goal_undo": "取消達成", "goal_edit_btn": "修改目標",
    "congrats_title": "恭喜你！", "congrats_body": "你完成了今天的目標。做得很好！",
    "page_condition": "身體狀態", "c_overall": "身體狀態", "c_overall_hint": "身體有多有精神、是否容易累",
    "st_note": "螢幕開啟且已解鎖的時間。若允許「使用情形存取權」，也會用手機本身的紀錄（最近一週）補足。",
    "tr_wait": "記錄 5 天的身體狀態、專注度和心情後，這裡會顯示狀態好的日子有什麼不同（目前 %1$d 天）。",
    "tr_good": "平均 7 以上\n(%1$d 天)",
    "tr_note": "※ 依身體狀態、專注度和心情的平均分區分狀態好的日子。想連飲食內容一起分析，推薦使用下方的「請 AI 分析」。",
    "ai_unlock_section": "# 打開手機時的紀錄（最近 14 天，每次解鎖時的回答）\n日期,時間,當時在做什麼,打開手機的原因\n%1$s",
    "ai_prompt": """你是一位精通生活習慣和身心狀態管理的教練。
以下是我每天在手機上記錄的資料（%1$d 天，%2$s 至 %3$s）。請分析我在什麼時候狀態最好。

# 記錄項目
- 身體狀態、專注度、心情、睡眠品質：0～10 的自我評分（10 為最好）。請用身體狀態、專注度和心情的平均分判斷狀態好的日子
- 睡眠時數：前一晚睡了幾個小時。就寢、起床：前一晚就寢的時間和當天早上起床的時間
- 睡前手機(分鐘)：睡前 30 分鐘內使用手機的時間
- 手機(分鐘)：當天螢幕開啟並使用的時間。解鎖次數：解鎖的次數
- 飲酒：1＝喝了，0＝沒喝
- 當天目標、達成：當天的目標以及是否達成（1＝達成）
- 早餐、午餐、晚餐、備註：吃了什麼、運動、發生的事等
- 空白表示「沒有記錄」（不是 0 分）

# 參考：我的目標和習慣
- 目標：%4$s
- 習慣：%5$s

# 資料（CSV）
日期,星期,身體狀態,專注度,心情,睡眠品質,睡眠時數,就寢,起床,睡前手機(分鐘),手機(分鐘),解鎖次數,飲酒,當天目標,達成,早餐,午餐,晚餐,備註
%6$s

# 希望你做的事
1. 狀態好的日子和不好的日子有什麼不同（睡眠、就寢時間、睡前用手機、飲酒、飲食、手機使用時間、星期等），盡量用具體數字說明
2. 最多 3 個「我在這種時候狀態好」的規律
3. 似乎讓狀態變差的習慣或組合（如有打開手機的紀錄，也請指出看起來浪費的時段和原因）
4. 從明天起就能嘗試的 3 個小行動
5. 需要注意的地方（資料少或有偏差等），以及今後值得增加記錄的項目

# 回答方式
- 先用 3 行總結結論，再詳細說明
- 請用%7$s回答，淺顯易懂，不用專業術語
- 把資料能說明的事和推測分開寫
- 請不要做醫學診斷""",
}

D["ko"] = {
    "this_month_so_far": "이번 달 (어제까지)",
    "c_bed": "어젯밤 잠든 시각", "c_wake": "오늘 아침 일어난 시각", "c_time_clear": "지우기",
    "c_bedphone": "자기 전 30분 휴대폰: %1$s 사용",
    "c_bedphone_none": "자기 전 30분 휴대폰: 사용하지 않음",
    "c_bedphone_unknown": "자기 전 30분 휴대폰: 알 수 없음 ('사용 정보 접근' 허용이 필요해요. 휴대폰 기록은 1주일 정도 지나면 사라져요)",
    "c_alcohol": "음주", "alc_no": "없음", "alc_yes": "있음",
    "tr_alcohol": "음주 있음", "tr_bedphone": "자기 전 휴대폰",
    "color_cream": "연노랑", "color_indigo": "남색",
    "pie_title": "오늘 앱별 사용 시간", "pie_other": "기타",
    "pie_need": "앱별 시간을 보려면 '사용 정보 접근'을 허용해야 해요.",
    "pie_none": "오늘 1분 이상 사용한 앱이 아직 없어요.",
    "pie_note": "휴대폰의 사용 기록(디지털 웰빙과 같은 데이터)으로 계산해요. 이 홈 화면에 있던 시간은 포함하지 않아요.",
    "goal_done": "달성", "goal_done_btn": "해냈어요!", "goal_undo": "달성 취소", "goal_edit_btn": "목표 고치기",
    "congrats_title": "축하해요!", "congrats_body": "오늘의 목표를 달성했어요. 정말 잘했어요!",
    "page_condition": "몸 컨디션", "c_overall": "몸 컨디션", "c_overall_hint": "몸이 얼마나 활기찼는지",
    "st_note": "화면을 켜고 잠금을 해제한 시간이에요. '사용 정보 접근'을 허용하면 휴대폰 자체 기록(최근 1주일)으로도 채워요.",
    "tr_wait": "몸 컨디션·집중도·기분을 5일 기록하면, 컨디션이 좋은 날과 그렇지 않은 날의 차이가 여기에 나와요 (현재 %1$d일).",
    "tr_good": "평균 7 이상\n(%1$d일)",
    "tr_note": "※ 몸 컨디션·집중도·기분의 평균으로 컨디션이 좋은 날을 나눠요. 식사 내용까지 포함한 분석은 아래 'AI에게 분석 맡기기'를 추천해요.",
    "ai_unlock_section": "# 휴대폰을 열었을 때의 기록 (최근 14일, 잠금 해제할 때마다 답한 것)\n날짜,시각,그때 하고 있던 일,휴대폰을 연 이유\n%1$s",
    "ai_prompt": """당신은 생활 습관과 컨디션 관리에 밝은 코치입니다.
아래는 제가 매일 휴대폰에 기록하는 데이터입니다 (%1$d일분, %2$s ~ %3$s). 제가 어떤 때에 컨디션이 좋은지 분석해 주세요.

# 기록 항목
- 몸 컨디션·집중도·기분·수면의 질: 0~10 자기 평가 (10이 최고). 컨디션이 좋은 날은 몸 컨디션·집중도·기분의 평균으로 판단해 주세요
- 수면 시간: 전날 밤에 잔 시간(시간). 취침·기상: 전날 밤 잠든 시각과 그날 아침 일어난 시각
- 자기 전 휴대폰(분): 자기 전 30분 동안 휴대폰을 사용한 시간
- 휴대폰(분): 그날 화면을 켜고 사용한 시간. 잠금 해제 횟수: 잠금을 해제한 횟수
- 음주: 1 = 마셨음, 0 = 안 마셨음
- 오늘의 목표·달성: 그날의 목표와 달성 여부 (1 = 달성)
- 아침·점심·저녁·메모: 먹은 것, 운동, 있었던 일 등
- 빈칸은 "기록하지 않음"이라는 뜻입니다 (0점이 아닙니다)

# 참고: 제 목표와 습관
- 목표: %4$s
- 습관: %5$s

# 데이터 (CSV)
날짜,요일,몸 컨디션,집중도,기분,수면의 질,수면 시간,취침,기상,자기 전 휴대폰(분),휴대폰(분),잠금 해제 횟수,음주,오늘의 목표,달성,아침,점심,저녁,메모
%6$s

# 부탁하고 싶은 것
1. 컨디션이 좋은 날과 나쁜 날의 차이 (수면, 취침 시각, 자기 전 휴대폰, 음주, 식사, 휴대폰 사용 시간, 요일 등). 가능한 한 구체적인 숫자로 보여 주세요
2. "이럴 때 컨디션이 좋다"는 패턴 최대 3가지
3. 컨디션을 떨어뜨리는 것 같은 습관이나 조합 (휴대폰을 연 기록이 있다면, 낭비로 보이는 시간대와 이유도)
4. 내일부터 작게 시작할 수 있는 행동 3가지
5. 데이터가 적거나 치우쳐 있는 등 주의할 점과, 앞으로 추가로 기록하면 좋을 항목

# 답변 방식
- 먼저 결론을 3줄로 정리한 뒤 자세히 설명해 주세요
- %7$s로, 전문 용어 없이 알기 쉽게
- 데이터로 말할 수 있는 것과 추측은 나눠서 써 주세요
- 의학적 진단은 하지 말아 주세요""",
}

D["es"] = {
    "this_month_so_far": "Este mes (hasta ayer)",
    "c_bed": "Hora de acostarse (anoche)", "c_wake": "Hora de levantarse (hoy)", "c_time_clear": "Borrar",
    "c_bedphone": "Móvil en los 30 min antes de dormir: %1$s",
    "c_bedphone_none": "Móvil en los 30 min antes de dormir: no se usó",
    "c_bedphone_unknown": "Móvil en los 30 min antes de dormir: no se sabe (requiere “Acceso a datos de uso”; el teléfono guarda sus registros una semana aprox.)",
    "c_alcohol": "Alcohol", "alc_no": "No", "alc_yes": "Sí",
    "tr_alcohol": "Bebió alcohol", "tr_bedphone": "Móvil antes de dormir",
    "color_cream": "Amarillo claro", "color_indigo": "Añil",
    "pie_title": "Tiempo de hoy por app", "pie_other": "Otras",
    "pie_need": "Para ver el tiempo por app, permite el “Acceso a datos de uso”.",
    "pie_none": "Hoy todavía no has usado ninguna app durante 1 minuto o más.",
    "pie_note": "Calculado con los registros de uso del teléfono (los mismos datos que Bienestar digital). No incluye el tiempo en esta pantalla de inicio.",
    "goal_done": "Logrado", "goal_done_btn": "¡Lo logré!", "goal_undo": "Marcar como no logrado", "goal_edit_btn": "Editar objetivo",
    "congrats_title": "¡Felicidades!", "congrats_body": "Has cumplido el objetivo de hoy. ¡Buen trabajo!",
    "page_condition": "Condición", "c_overall": "Cuerpo", "c_overall_hint": "Cuánta energía tenía tu cuerpo",
    "st_note": "Tiempo con la pantalla encendida y desbloqueada. Si permites el “Acceso a datos de uso”, también se completa con los registros del teléfono (últimos 7 días).",
    "tr_wait": "Registra cuerpo, concentración y ánimo durante 5 días para ver qué cambia en tus días buenos (%1$d hasta ahora).",
    "tr_good": "Media 7 o más\n(%1$d días)",
    "tr_note": "* Los días buenos se separan por la media de cuerpo, concentración y ánimo. Para un análisis que incluya las comidas, prueba “Pedir a la IA” abajo.",
    "ai_unlock_section": "# Por qué abrí el móvil (últimos 14 días, respondido en cada desbloqueo)\nfecha,hora,qué estaba haciendo,por qué abrí el móvil\n%1$s",
    "ai_prompt": """Eres un coach que sabe mucho de hábitos diarios y bienestar.
Abajo están los datos que registro cada día en mi móvil (%1$d días, del %2$s al %3$s). Analiza cuándo suelo sentirme mejor.

# Campos
- cuerpo, concentración, ánimo, calidad_sueño: autoevaluación de 0 a 10 (10 es lo mejor). Juzga los días buenos por la media de cuerpo, concentración y ánimo
- horas_sueño: horas que dormí la noche anterior. acostarse, levantarse: a qué hora me acosté la noche anterior y a qué hora me levanté esa mañana
- móvil_antes_dormir: minutos que usé el móvil en los 30 minutos antes de acostarme
- móvil_min: minutos con la pantalla encendida y en uso ese día. desbloqueos: número de desbloqueos
- alcohol: 1 = bebí alcohol, 0 = no
- objetivo, logrado: el objetivo de ese día y si lo cumplí (1 = cumplido)
- desayuno, comida, cena, notas: lo que comí, ejercicio, sucesos, etc.
- Vacío significa "sin registrar" (no es cero)

# Como referencia: mis objetivos y hábitos
- Objetivos: %4$s
- Hábitos: %5$s

# Datos (CSV)
fecha,día,cuerpo,concentración,ánimo,calidad_sueño,horas_sueño,acostarse,levantarse,móvil_antes_dormir,móvil_min,desbloqueos,alcohol,objetivo,logrado,desayuno,comida,cena,notas
%6$s

# Lo que me gustaría
1. Qué cambia entre los días buenos y los malos (sueño, hora de acostarme, móvil antes de dormir, alcohol, comidas, tiempo de pantalla, día de la semana, etc.), con cifras concretas cuando sea posible
2. Hasta 3 patrones del tipo "me siento bien cuando..."
3. Hábitos o combinaciones que parecen bajarme el ánimo (si hay registros de desbloqueo, también horas y motivos que parecen tiempo perdido)
4. 3 acciones pequeñas que pueda probar desde mañana
5. Advertencias (pocos datos o sesgados, etc.) y datos que valdría la pena registrar en el futuro

# Cómo responder
- Empieza con un resumen de 3 líneas y luego explica en detalle
- Responde en %7$s, con lenguaje sencillo y sin tecnicismos
- Separa lo que muestran los datos de las suposiciones
- No hagas diagnósticos médicos""",
}

D["fr"] = {
    "this_month_so_far": "Ce mois-ci (jusqu'à hier)",
    "c_bed": "Coucher (hier soir)", "c_wake": "Lever (ce matin)", "c_time_clear": "Effacer",
    "c_bedphone": "Téléphone dans les 30 min avant le coucher : %1$s",
    "c_bedphone_none": "Téléphone dans les 30 min avant le coucher : non utilisé",
    "c_bedphone_unknown": "Téléphone dans les 30 min avant le coucher : inconnu (nécessite « Accès aux données d'utilisation » ; le téléphone garde ses relevés environ une semaine)",
    "c_alcohol": "Alcool", "alc_no": "Non", "alc_yes": "Oui",
    "tr_alcohol": "Alcool bu", "tr_bedphone": "Téléphone avant le coucher",
    "color_cream": "Jaune pâle", "color_indigo": "Indigo",
    "pie_title": "Temps par appli aujourd'hui", "pie_other": "Autres",
    "pie_need": "Pour voir le temps par appli, autorisez « Accès aux données d'utilisation ».",
    "pie_none": "Aucune appli n'a encore été utilisée 1 minute ou plus aujourd'hui.",
    "pie_note": "Calculé à partir des relevés d'utilisation du téléphone (les mêmes données que Bien-être numérique). Le temps passé sur cet écran d'accueil n'est pas inclus.",
    "goal_done": "Réussi", "goal_done_btn": "J'ai réussi !", "goal_undo": "Marquer comme non réussi", "goal_edit_btn": "Modifier l'objectif",
    "congrats_title": "Félicitations !", "congrats_body": "Vous avez atteint l'objectif du jour. Bravo !",
    "page_condition": "Forme", "c_overall": "Corps", "c_overall_hint": "Votre niveau d'énergie physique",
    "st_note": "Temps avec l'écran allumé et déverrouillé. Si « Accès aux données d'utilisation » est autorisé, il est aussi complété par les relevés du téléphone (7 derniers jours).",
    "tr_wait": "Notez corps, concentration et humeur pendant 5 jours pour voir ce qui change vos bons jours (%1$d pour l'instant).",
    "tr_good": "Moyenne 7 ou plus\n(%1$d jours)",
    "tr_note": "* Les bons jours sont séparés selon la moyenne corps, concentration et humeur. Pour une analyse incluant les repas, essayez « Demander à l'IA » ci-dessous.",
    "ai_unlock_section": "# Pourquoi j'ai ouvert mon téléphone (14 derniers jours, répondu à chaque déverrouillage)\ndate,heure,ce que je faisais,pourquoi j'ai ouvert mon téléphone\n%1$s",
    "ai_prompt": """Tu es un coach qui connaît très bien les habitudes de vie et le bien-être.
Voici les données que je note chaque jour sur mon téléphone (%1$d jours, du %2$s au %3$s). Analyse à quels moments je me sens le mieux.

# Champs
- corps, concentration, humeur, qualité_sommeil : auto-évaluation de 0 à 10 (10 = le mieux). Juge les bons jours d'après la moyenne de corps, concentration et humeur
- heures_sommeil : heures dormies la nuit précédente. coucher, lever : l'heure du coucher la veille et du lever ce matin-là
- téléphone_avant_coucher : minutes d'utilisation du téléphone dans les 30 minutes avant le coucher
- téléphone_min : minutes avec l'écran allumé et utilisé ce jour-là. déverrouillages : nombre de déverrouillages
- alcool : 1 = bu de l'alcool, 0 = non
- objectif, réussi : l'objectif du jour et si je l'ai atteint (1 = atteint)
- petit-déjeuner, déjeuner, dîner, notes : ce que j'ai mangé, sport, événements, etc.
- Vide signifie "non noté" (pas zéro)

# Pour info : mes objectifs et habitudes
- Objectifs : %4$s
- Habitudes : %5$s

# Données (CSV)
date,jour,corps,concentration,humeur,qualité_sommeil,heures_sommeil,coucher,lever,téléphone_avant_coucher,téléphone_min,déverrouillages,alcool,objectif,réussi,petit-déjeuner,déjeuner,dîner,notes
%6$s

# Ce que j'aimerais
1. Ce qui diffère entre les bons et les mauvais jours (sommeil, heure du coucher, téléphone avant de dormir, alcool, repas, temps d'écran, jour de la semaine, etc.), avec des chiffres concrets si possible
2. Jusqu'à 3 schémas du type "je me sens bien quand..."
3. Les habitudes ou combinaisons qui semblent me tirer vers le bas (s'il y a des relevés de déverrouillage, aussi les moments et raisons qui semblent du temps perdu)
4. 3 petites actions à essayer dès demain
5. Les précautions (peu de données, données biaisées, etc.) et ce qu'il serait utile de noter à l'avenir

# Comment répondre
- Commence par un résumé en 3 lignes, puis explique en détail
- Réponds en %7$s, avec des mots simples et sans jargon
- Sépare ce que montrent les données de ce qui relève de la supposition
- Ne pose pas de diagnostic médical""",
}

D["de"] = {
    "this_month_so_far": "Diesen Monat (bis gestern)",
    "c_bed": "Ins Bett gegangen (gestern Abend)", "c_wake": "Aufgestanden (heute Morgen)", "c_time_clear": "Löschen",
    "c_bedphone": "Handy in den 30 Min. vor dem Schlafen: %1$s",
    "c_bedphone_none": "Handy in den 30 Min. vor dem Schlafen: nicht benutzt",
    "c_bedphone_unknown": "Handy in den 30 Min. vor dem Schlafen: unbekannt (benötigt „Zugriff auf Nutzungsdaten“; das Handy speichert seine Daten etwa eine Woche)",
    "c_alcohol": "Alkohol", "alc_no": "Nein", "alc_yes": "Ja",
    "tr_alcohol": "Alkohol getrunken", "tr_bedphone": "Handy vor dem Schlafen",
    "color_cream": "Hellgelb", "color_indigo": "Indigo",
    "pie_title": "Heutige Zeit nach App", "pie_other": "Sonstige",
    "pie_need": "Um die Zeit pro App zu sehen, erlaube „Zugriff auf Nutzungsdaten“.",
    "pie_none": "Heute wurde noch keine App 1 Minute oder länger benutzt.",
    "pie_note": "Berechnet aus den Nutzungsdaten des Handys (dieselben Daten wie „Digital Wellbeing“). Die Zeit auf diesem Startbildschirm ist nicht enthalten.",
    "goal_done": "Geschafft", "goal_done_btn": "Geschafft!", "goal_undo": "Als nicht geschafft markieren", "goal_edit_btn": "Ziel bearbeiten",
    "congrats_title": "Glückwunsch!", "congrats_body": "Du hast dein heutiges Ziel erreicht. Super gemacht!",
    "page_condition": "Verfassung", "c_overall": "Körper", "c_overall_hint": "Wie fit sich dein Körper anfühlte",
    "st_note": "Zeit mit eingeschaltetem und entsperrtem Bildschirm. Mit „Zugriff auf Nutzungsdaten“ wird sie auch aus den Daten des Handys ergänzt (letzte 7 Tage).",
    "tr_wait": "Trage 5 Tage lang Körper, Konzentration und Stimmung ein, um zu sehen, was deine guten Tage ausmacht (bisher %1$d).",
    "tr_good": "Schnitt 7 oder mehr\n(%1$d Tage)",
    "tr_note": "* Gute Tage werden nach dem Schnitt aus Körper, Konzentration und Stimmung getrennt. Für eine Analyse inkl. Mahlzeiten probiere unten „KI fragen“.",
    "ai_unlock_section": "# Warum ich mein Handy geöffnet habe (letzte 14 Tage, bei jedem Entsperren beantwortet)\nDatum,Uhrzeit,was ich gerade tat,warum ich das Handy geöffnet habe\n%1$s",
    "ai_prompt": """Du bist ein Coach, der sich mit Alltagsgewohnheiten und Wohlbefinden sehr gut auskennt.
Unten stehen die Daten, die ich jeden Tag auf meinem Handy festhalte (%1$d Tage, %2$s bis %3$s). Bitte analysiere, wann ich mich am besten fühle.

# Felder
- Körper, Konzentration, Stimmung, Schlafqualität: Selbsteinschätzung 0–10 (10 ist am besten). Beurteile gute Tage nach dem Schnitt aus Körper, Konzentration und Stimmung
- Schlaf_h: Stunden Schlaf in der Nacht davor. Zubettgehen, Aufstehen: wann ich am Abend davor ins Bett ging und an dem Morgen aufstand
- Handy_vor_Schlaf: Minuten Handynutzung in den 30 Minuten vor dem Schlafengehen
- Handy_min: Minuten mit eingeschaltetem, benutztem Bildschirm an dem Tag. Entsperrungen: Anzahl der Entsperrungen
- Alkohol: 1 = Alkohol getrunken, 0 = keiner
- Ziel, erreicht: das Tagesziel und ob ich es erreicht habe (1 = erreicht)
- Frühstück, Mittagessen, Abendessen, Notizen: was ich gegessen habe, Sport, Ereignisse usw.
- Leer bedeutet "nicht erfasst" (nicht null)

# Zur Info: meine Ziele und Gewohnheiten
- Ziele: %4$s
- Gewohnheiten: %5$s

# Daten (CSV)
Datum,Wochentag,Körper,Konzentration,Stimmung,Schlafqualität,Schlaf_h,Zubettgehen,Aufstehen,Handy_vor_Schlaf,Handy_min,Entsperrungen,Alkohol,Ziel,erreicht,Frühstück,Mittagessen,Abendessen,Notizen
%6$s

# Was ich mir wünsche
1. Was unterscheidet gute von schlechten Tagen (Schlaf, Zubettgehzeit, Handy vor dem Schlafen, Alkohol, Mahlzeiten, Bildschirmzeit, Wochentag usw.), möglichst mit konkreten Zahlen
2. Bis zu 3 Muster nach dem Motto "Mir geht es gut, wenn..."
3. Gewohnheiten oder Kombinationen, die mich herunterzuziehen scheinen (falls es Entsperr-Daten gibt, auch Zeiten und Gründe, die nach Zeitverschwendung aussehen)
4. 3 kleine Schritte, die ich ab morgen ausprobieren kann
5. Einschränkungen (wenige oder einseitige Daten usw.) und Dinge, die es sich künftig zu erfassen lohnt

# Wie du antworten sollst
- Beginne mit einer Zusammenfassung in 3 Zeilen und erkläre dann ausführlich
- Antworte auf %7$s, in einfacher Sprache ohne Fachbegriffe
- Trenne, was die Daten zeigen, von Vermutungen
- Stelle keine medizinische Diagnose""",
}

D["pt"] = {
    "this_month_so_far": "Este mês (até ontem)",
    "c_bed": "Hora de dormir (ontem à noite)", "c_wake": "Hora de acordar (hoje)", "c_time_clear": "Limpar",
    "c_bedphone": "Celular nos 30 min antes de dormir: %1$s",
    "c_bedphone_none": "Celular nos 30 min antes de dormir: não usou",
    "c_bedphone_unknown": "Celular nos 30 min antes de dormir: desconhecido (requer “Acesso ao uso”; o telefone guarda os registros por cerca de uma semana)",
    "c_alcohol": "Álcool", "alc_no": "Não", "alc_yes": "Sim",
    "tr_alcohol": "Bebeu álcool", "tr_bedphone": "Celular antes de dormir",
    "color_cream": "Amarelo-claro", "color_indigo": "Anil",
    "pie_title": "Tempo de hoje por app", "pie_other": "Outros",
    "pie_need": "Para ver o tempo por app, permita o “Acesso ao uso”.",
    "pie_none": "Nenhum app foi usado por 1 minuto ou mais hoje ainda.",
    "pie_note": "Calculado a partir dos registros de uso do telefone (os mesmos dados do Bem-estar digital). Não inclui o tempo nesta tela inicial.",
    "goal_done": "Concluído", "goal_done_btn": "Consegui!", "goal_undo": "Marcar como não concluído", "goal_edit_btn": "Editar meta",
    "congrats_title": "Parabéns!", "congrats_body": "Você cumpriu a meta de hoje. Muito bem!",
    "page_condition": "Condição", "c_overall": "Corpo", "c_overall_hint": "Quanta energia seu corpo tinha",
    "st_note": "Tempo com a tela ligada e desbloqueada. Com o “Acesso ao uso” permitido, também é completado com os registros do telefone (últimos 7 dias).",
    "tr_wait": "Registre corpo, foco e humor por 5 dias para ver o que muda nos seus dias bons (%1$d até agora).",
    "tr_good": "Média 7 ou mais\n(%1$d dias)",
    "tr_note": "* Os dias bons são separados pela média de corpo, foco e humor. Para uma análise que inclua as refeições, experimente “Pedir à IA” abaixo.",
    "ai_unlock_section": "# Por que abri o celular (últimos 14 dias, respondido a cada desbloqueio)\ndata,hora,o que eu estava fazendo,por que abri o celular\n%1$s",
    "ai_prompt": """Você é um coach que entende muito de hábitos diários e bem-estar.
Abaixo estão os dados que registro todos os dias no celular (%1$d dias, de %2$s a %3$s). Analise quando eu costumo me sentir melhor.

# Campos
- corpo, foco, humor, qualidade_sono: autoavaliação de 0 a 10 (10 é o melhor). Avalie os dias bons pela média de corpo, foco e humor
- horas_sono: horas que dormi na noite anterior. dormir, acordar: a que horas fui dormir na noite anterior e acordei naquela manhã
- celular_antes_dormir: minutos de uso do celular nos 30 minutos antes de dormir
- celular_min: minutos com a tela ligada e em uso naquele dia. desbloqueios: número de desbloqueios
- álcool: 1 = bebi álcool, 0 = não
- meta, concluída: a meta do dia e se eu a cumpri (1 = cumprida)
- café da manhã, almoço, jantar, notas: o que comi, exercícios, acontecimentos etc.
- Vazio significa "não registrado" (não é zero)

# Para referência: minhas metas e hábitos
- Metas: %4$s
- Hábitos: %5$s

# Dados (CSV)
data,dia,corpo,foco,humor,qualidade_sono,horas_sono,dormir,acordar,celular_antes_dormir,celular_min,desbloqueios,álcool,meta,concluída,café da manhã,almoço,jantar,notas
%6$s

# O que eu gostaria
1. O que muda entre dias bons e ruins (sono, hora de dormir, celular antes de dormir, álcool, refeições, tempo de tela, dia da semana etc.), com números concretos quando possível
2. Até 3 padrões do tipo "eu me sinto bem quando..."
3. Hábitos ou combinações que parecem me derrubar (se houver registros de desbloqueio, também horários e motivos que parecem desperdício)
4. 3 pequenas ações que posso testar a partir de amanhã
5. Ressalvas (poucos dados, dados enviesados etc.) e itens que valeria a pena registrar no futuro

# Como responder
- Comece com um resumo de 3 linhas e depois explique em detalhes
- Responda em %7$s, com linguagem simples e sem jargões
- Separe o que os dados mostram das suposições
- Não faça diagnósticos médicos""",
}

D["in"] = {
    "this_month_so_far": "Bulan ini (sampai kemarin)",
    "c_bed": "Jam tidur (tadi malam)", "c_wake": "Jam bangun (pagi ini)", "c_time_clear": "Hapus",
    "c_bedphone": "HP dalam 30 menit sebelum tidur: %1$s",
    "c_bedphone_none": "HP dalam 30 menit sebelum tidur: tidak dipakai",
    "c_bedphone_unknown": "HP dalam 30 menit sebelum tidur: tidak diketahui (perlu izin “Akses penggunaan”; catatan HP hanya tersimpan sekitar seminggu)",
    "c_alcohol": "Alkohol", "alc_no": "Tidak", "alc_yes": "Ya",
    "tr_alcohol": "Minum alkohol", "tr_bedphone": "HP sebelum tidur",
    "color_cream": "Kuning muda", "color_indigo": "Biru tua",
    "pie_title": "Waktu hari ini per aplikasi", "pie_other": "Lainnya",
    "pie_need": "Untuk melihat waktu per aplikasi, izinkan “Akses penggunaan”.",
    "pie_none": "Hari ini belum ada aplikasi yang dipakai 1 menit atau lebih.",
    "pie_note": "Dihitung dari catatan penggunaan HP (data yang sama dengan Digital Wellbeing). Waktu di layar utama ini tidak termasuk.",
    "goal_done": "Tercapai", "goal_done_btn": "Berhasil!", "goal_undo": "Tandai belum tercapai", "goal_edit_btn": "Ubah target",
    "congrats_title": "Selamat!", "congrats_body": "Kamu mencapai target hari ini. Kerja bagus!",
    "page_condition": "Kondisi badan", "c_overall": "Badan", "c_overall_hint": "Seberapa bugar badanmu",
    "st_note": "Waktu saat layar menyala dan tidak terkunci. Jika “Akses penggunaan” diizinkan, juga dilengkapi dari catatan HP (7 hari terakhir).",
    "tr_wait": "Catat badan, fokus, dan suasana hati selama 5 hari untuk melihat apa yang berbeda di hari-hari baikmu (sejauh ini %1$d).",
    "tr_good": "Rata-rata 7 ke atas\n(%1$d hari)",
    "tr_note": "* Hari baik dibedakan dari rata-rata badan, fokus, dan suasana hati. Untuk analisis yang juga mencakup makanan, coba “Tanya AI” di bawah.",
    "ai_unlock_section": "# Alasan saya membuka HP (14 hari terakhir, dijawab setiap kali membuka kunci)\ntanggal,jam,apa yang sedang saya lakukan,alasan membuka HP\n%1$s",
    "ai_prompt": """Kamu adalah pelatih yang paham betul soal kebiasaan sehari-hari dan kesehatan.
Di bawah ini data yang saya catat setiap hari di HP (%1$d hari, %2$s sampai %3$s). Tolong analisis kapan saya biasanya merasa paling baik.

# Kolom
- badan, fokus, suasana_hati, kualitas_tidur: penilaian diri 0–10 (10 terbaik). Nilai hari baik dari rata-rata badan, fokus, dan suasana hati
- jam_tidur: berapa jam saya tidur malam sebelumnya. tidur, bangun: jam saya tidur malam sebelumnya dan bangun pagi itu
- hp_sebelum_tidur: menit saya memakai HP dalam 30 menit sebelum tidur
- hp_menit: menit layar menyala dan dipakai hari itu. buka_kunci: berapa kali membuka kunci
- alkohol: 1 = minum alkohol, 0 = tidak
- target, tercapai: target hari itu dan apakah tercapai (1 = tercapai)
- sarapan, makan_siang, makan_malam, catatan: apa yang saya makan, olahraga, kejadian, dll.
- Kosong berarti "tidak dicatat" (bukan nol)

# Sebagai referensi: target dan kebiasaan saya
- Target: %4$s
- Kebiasaan: %5$s

# Data (CSV)
tanggal,hari,badan,fokus,suasana_hati,kualitas_tidur,jam_tidur,tidur,bangun,hp_sebelum_tidur,hp_menit,buka_kunci,alkohol,target,tercapai,sarapan,makan_siang,makan_malam,catatan
%6$s

# Yang saya minta
1. Apa yang berbeda antara hari baik dan hari buruk (tidur, jam tidur, HP sebelum tidur, alkohol, makanan, waktu layar, hari dalam seminggu, dll.), dengan angka konkret bila memungkinkan
2. Paling banyak 3 pola "saya merasa baik ketika..."
3. Kebiasaan atau kombinasi yang tampaknya membuat kondisi saya turun (jika ada catatan membuka HP, juga waktu dan alasan yang tampak sia-sia)
4. 3 tindakan kecil yang bisa saya coba mulai besok
5. Hal yang perlu diperhatikan (data sedikit atau berat sebelah, dll.) dan hal yang sebaiknya dicatat ke depannya

# Cara menjawab
- Mulailah dengan ringkasan 3 baris, lalu jelaskan secara rinci
- Jawab dalam %7$s, dengan bahasa sederhana tanpa istilah teknis
- Pisahkan apa yang ditunjukkan data dari dugaan
- Jangan memberikan diagnosis medis""",
}

D["it"] = {
    "this_month_so_far": "Questo mese (fino a ieri)",
    "c_bed": "Andato a letto (ieri sera)", "c_wake": "Sveglia (stamattina)", "c_time_clear": "Cancella",
    "c_bedphone": "Telefono nei 30 min prima di dormire: %1$s",
    "c_bedphone_none": "Telefono nei 30 min prima di dormire: non usato",
    "c_bedphone_unknown": "Telefono nei 30 min prima di dormire: non noto (serve “Accesso ai dati di utilizzo”; il telefono conserva i dati per circa una settimana)",
    "c_alcohol": "Alcol", "alc_no": "No", "alc_yes": "Sì",
    "tr_alcohol": "Bevuto alcol", "tr_bedphone": "Telefono prima di dormire",
    "color_cream": "Giallo chiaro", "color_indigo": "Indaco",
    "pie_title": "Tempo di oggi per app", "pie_other": "Altre",
    "pie_need": "Per vedere il tempo per app, consenti “Accesso ai dati di utilizzo”.",
    "pie_none": "Oggi nessuna app è stata ancora usata per 1 minuto o più.",
    "pie_note": "Calcolato dai dati di utilizzo del telefono (gli stessi di Benessere digitale). Il tempo su questa schermata Home non è incluso.",
    "goal_done": "Fatto", "goal_done_btn": "Ce l'ho fatta!", "goal_undo": "Segna come non fatto", "goal_edit_btn": "Modifica obiettivo",
    "congrats_title": "Congratulazioni!", "congrats_body": "Hai raggiunto l'obiettivo di oggi. Ottimo lavoro!",
    "page_condition": "Forma fisica", "c_overall": "Corpo", "c_overall_hint": "Quanta energia aveva il tuo corpo",
    "st_note": "Tempo con lo schermo acceso e sbloccato. Se consenti “Accesso ai dati di utilizzo”, viene completato anche con i dati del telefono (ultimi 7 giorni).",
    "tr_wait": "Registra corpo, concentrazione e umore per 5 giorni per vedere cosa cambia nei giorni buoni (finora %1$d).",
    "tr_good": "Media 7 o più\n(%1$d giorni)",
    "tr_note": "* I giorni buoni sono separati in base alla media di corpo, concentrazione e umore. Per un'analisi che includa i pasti, prova “Chiedi all'IA” qui sotto.",
    "ai_unlock_section": "# Perché ho aperto il telefono (ultimi 14 giorni, risposta a ogni sblocco)\ndata,ora,cosa stavo facendo,perché ho aperto il telefono\n%1$s",
    "ai_prompt": """Sei un coach che conosce molto bene le abitudini quotidiane e il benessere.
Qui sotto ci sono i dati che registro ogni giorno sul telefono (%1$d giorni, dal %2$s al %3$s). Analizza quando tendo a sentirmi meglio.

# Campi
- corpo, concentrazione, umore, qualità_sonno: autovalutazione da 0 a 10 (10 è il massimo). Valuta i giorni buoni in base alla media di corpo, concentrazione e umore
- ore_sonno: ore dormite la notte prima. a_letto, sveglia: quando sono andato a letto la sera prima e quando mi sono svegliato quella mattina
- telefono_prima_di_dormire: minuti di uso del telefono nei 30 minuti prima di andare a letto
- telefono_min: minuti con lo schermo acceso e in uso quel giorno. sblocchi: numero di sblocchi
- alcol: 1 = bevuto alcol, 0 = no
- obiettivo, raggiunto: l'obiettivo del giorno e se l'ho raggiunto (1 = raggiunto)
- colazione, pranzo, cena, note: cosa ho mangiato, attività fisica, eventi ecc.
- Vuoto significa "non registrato" (non zero)

# Per riferimento: i miei obiettivi e abitudini
- Obiettivi: %4$s
- Abitudini: %5$s

# Dati (CSV)
data,giorno,corpo,concentrazione,umore,qualità_sonno,ore_sonno,a_letto,sveglia,telefono_prima_di_dormire,telefono_min,sblocchi,alcol,obiettivo,raggiunto,colazione,pranzo,cena,note
%6$s

# Cosa vorrei
1. Cosa cambia tra giorni buoni e cattivi (sonno, ora di andare a letto, telefono prima di dormire, alcol, pasti, tempo davanti allo schermo, giorno della settimana ecc.), con numeri concreti se possibile
2. Fino a 3 schemi del tipo "sto bene quando..."
3. Abitudini o combinazioni che sembrano abbattermi (se ci sono dati sugli sblocchi, anche orari e motivi che sembrano tempo sprecato)
4. 3 piccole azioni da provare a partire da domani
5. Avvertenze (pochi dati o dati sbilanciati ecc.) e cose che varrebbe la pena registrare in futuro

# Come rispondere
- Inizia con un riassunto di 3 righe, poi spiega nel dettaglio
- Rispondi in %7$s, con parole semplici e senza termini tecnici
- Separa ciò che mostrano i dati dalle supposizioni
- Non fare diagnosi mediche""",
}

D["ru"] = {
    "this_month_so_far": "Этот месяц (по вчерашний день)",
    "c_bed": "Лёг спать (вчера вечером)", "c_wake": "Проснулся (сегодня утром)", "c_time_clear": "Очистить",
    "c_bedphone": "Телефон за 30 мин до сна: %1$s",
    "c_bedphone_none": "Телефон за 30 мин до сна: не использовался",
    "c_bedphone_unknown": "Телефон за 30 мин до сна: неизвестно (нужен «Доступ к истории использования»; телефон хранит записи около недели)",
    "c_alcohol": "Алкоголь", "alc_no": "Нет", "alc_yes": "Да",
    "tr_alcohol": "Был алкоголь", "tr_bedphone": "Телефон перед сном",
    "color_cream": "Светло-жёлтый", "color_indigo": "Тёмно-синий",
    "pie_title": "Время по приложениям сегодня", "pie_other": "Другие",
    "pie_need": "Чтобы видеть время по приложениям, разрешите «Доступ к истории использования».",
    "pie_none": "Сегодня ещё нет приложений, которыми пользовались 1 минуту или дольше.",
    "pie_note": "Подсчитано по записям использования телефона (те же данные, что в «Цифровом благополучии»). Время на этом главном экране не учитывается.",
    "goal_done": "Выполнено", "goal_done_btn": "Получилось!", "goal_undo": "Отменить выполнение", "goal_edit_btn": "Изменить цель",
    "congrats_title": "Поздравляем!", "congrats_body": "Вы выполнили цель на сегодня. Отличная работа!",
    "page_condition": "Самочувствие", "c_overall": "Тело", "c_overall_hint": "Сколько сил было у тела",
    "st_note": "Время, когда экран был включён и разблокирован. Если разрешён «Доступ к истории использования», данные дополняются записями телефона (последние 7 дней).",
    "tr_wait": "Отмечайте тело, концентрацию и настроение 5 дней, чтобы увидеть, чем отличаются хорошие дни (пока %1$d).",
    "tr_good": "В среднем 7+\n(%1$d дн.)",
    "tr_note": "* Хорошие дни определяются по среднему тела, концентрации и настроения. Для анализа с учётом еды попробуйте «Спросить ИИ» ниже.",
    "ai_unlock_section": "# Почему я открывал телефон (последние 14 дней, ответы при каждой разблокировке)\nдата,время,чем я занимался,почему открыл телефон\n%1$s",
    "ai_prompt": """Ты коуч, который хорошо разбирается в повседневных привычках и самочувствии.
Ниже данные, которые я каждый день записываю в телефоне (%1$d дн., с %2$s по %3$s). Проанализируй, когда я чувствую себя лучше всего.

# Поля
- тело, концентрация, настроение, качество_сна: самооценка от 0 до 10 (10 — лучше всего). Хорошие дни оценивай по среднему тела, концентрации и настроения
- сон_ч: сколько часов я спал прошлой ночью. отбой, подъём: когда я лёг спать накануне вечером и когда проснулся в то утро
- телефон_перед_сном: минуты использования телефона за 30 минут до сна
- телефон_мин: минуты с включённым и используемым экраном за день. разблокировки: число разблокировок
- алкоголь: 1 = пил алкоголь, 0 = нет
- цель, выполнено: цель на день и выполнил ли я её (1 = выполнено)
- завтрак, обед, ужин, заметки: что я ел, спорт, события и т. д.
- Пусто означает "не записано" (не ноль)

# Для справки: мои цели и привычки
- Цели: %4$s
- Привычки: %5$s

# Данные (CSV)
дата,день,тело,концентрация,настроение,качество_сна,сон_ч,отбой,подъём,телефон_перед_сном,телефон_мин,разблокировки,алкоголь,цель,выполнено,завтрак,обед,ужин,заметки
%6$s

# Что я хотел бы получить
1. Чем отличаются хорошие и плохие дни (сон, время отбоя, телефон перед сном, алкоголь, еда, экранное время, день недели и т. д.), по возможности с конкретными цифрами
2. До 3 закономерностей вида "мне хорошо, когда..."
3. Привычки или сочетания, которые, похоже, ухудшают самочувствие (если есть записи разблокировок, то и время и причины, похожие на потерю времени)
4. 3 небольших действия, которые можно попробовать с завтрашнего дня
5. Оговорки (мало данных, перекос и т. д.) и что стоит записывать в будущем

# Как отвечать
- Начни с краткого вывода в 3 строки, затем объясни подробно
- Отвечай на языке: %7$s, простыми словами без терминов
- Отделяй то, что видно из данных, от догадок
- Не ставь медицинских диагнозов""",
}

D["th"] = {
    "this_month_so_far": "เดือนนี้ (ถึงเมื่อวาน)",
    "c_bed": "เวลาเข้านอน (เมื่อคืน)", "c_wake": "เวลาตื่น (เช้านี้)", "c_time_clear": "ล้าง",
    "c_bedphone": "ใช้มือถือใน 30 นาทีก่อนนอน: %1$s",
    "c_bedphone_none": "ใช้มือถือใน 30 นาทีก่อนนอน: ไม่ได้ใช้",
    "c_bedphone_unknown": "ใช้มือถือใน 30 นาทีก่อนนอน: ไม่ทราบ (ต้องอนุญาต “การเข้าถึงข้อมูลการใช้งาน” และมือถือเก็บบันทึกไว้ประมาณหนึ่งสัปดาห์)",
    "c_alcohol": "แอลกอฮอล์", "alc_no": "ไม่มี", "alc_yes": "มี",
    "tr_alcohol": "ดื่มแอลกอฮอล์", "tr_bedphone": "ใช้มือถือก่อนนอน",
    "color_cream": "เหลืองอ่อน", "color_indigo": "กรมท่า",
    "pie_title": "เวลาใช้งานแต่ละแอปวันนี้", "pie_other": "อื่นๆ",
    "pie_need": "หากต้องการดูเวลาของแต่ละแอป ต้องอนุญาต “การเข้าถึงข้อมูลการใช้งาน”",
    "pie_none": "วันนี้ยังไม่มีแอปที่ใช้นาน 1 นาทีขึ้นไป",
    "pie_note": "นับจากบันทึกการใช้งานของมือถือ (ข้อมูลเดียวกับ Digital Wellbeing) ไม่รวมเวลาบนหน้าจอหลักนี้",
    "goal_done": "สำเร็จ", "goal_done_btn": "ทำได้แล้ว!", "goal_undo": "ยกเลิกการสำเร็จ", "goal_edit_btn": "แก้ไขเป้าหมาย",
    "congrats_title": "ยินดีด้วย!", "congrats_body": "คุณทำเป้าหมายของวันนี้สำเร็จแล้ว เก่งมาก!",
    "page_condition": "สภาพร่างกาย", "c_overall": "ร่างกาย", "c_overall_hint": "ร่างกายกระปรี้กระเปร่าแค่ไหน",
    "st_note": "เวลาที่หน้าจอเปิดและปลดล็อกอยู่ หากอนุญาต “การเข้าถึงข้อมูลการใช้งาน” จะเติมจากบันทึกของมือถือด้วย (7 วันล่าสุด)",
    "tr_wait": "บันทึกร่างกาย สมาธิ และอารมณ์ 5 วัน เพื่อดูว่าวันที่รู้สึกดีต่างจากวันอื่นอย่างไร (ตอนนี้ %1$d วัน)",
    "tr_good": "เฉลี่ย 7 ขึ้นไป\n(%1$d วัน)",
    "tr_note": "* แบ่งวันที่รู้สึกดีจากค่าเฉลี่ยของร่างกาย สมาธิ และอารมณ์ หากต้องการวิเคราะห์รวมถึงอาหาร ลองใช้ “ให้ AI วิเคราะห์” ด้านล่าง",
    "ai_unlock_section": "# เหตุผลที่เปิดมือถือ (14 วันล่าสุด ตอบทุกครั้งที่ปลดล็อก)\nวันที่,เวลา,ตอนนั้นกำลังทำอะไร,เหตุผลที่เปิดมือถือ\n%1$s",
    "ai_prompt": """คุณเป็นโค้ชที่เชี่ยวชาญเรื่องนิสัยในชีวิตประจำวันและการดูแลสุขภาพ
ด้านล่างคือข้อมูลที่ฉันบันทึกในมือถือทุกวัน (%1$d วัน ตั้งแต่ %2$s ถึง %3$s) ช่วยวิเคราะห์ว่าฉันมักรู้สึกดีที่สุดเมื่อไร

# ช่องข้อมูล
- ร่างกาย สมาธิ อารมณ์ คุณภาพการนอน: ให้คะแนนตัวเอง 0–10 (10 ดีที่สุด) ให้ตัดสินวันที่ดีจากค่าเฉลี่ยของร่างกาย สมาธิ และอารมณ์
- ชั่วโมงนอน: จำนวนชั่วโมงที่นอนคืนก่อนหน้า เข้านอน ตื่น: เวลาที่เข้านอนคืนก่อนหน้าและเวลาที่ตื่นเช้าวันนั้น
- มือถือก่อนนอน: จำนวนนาทีที่ใช้มือถือใน 30 นาทีก่อนเข้านอน
- มือถือ_นาที: จำนวนนาทีที่หน้าจอเปิดและใช้งานในวันนั้น ปลดล็อก: จำนวนครั้งที่ปลดล็อก
- แอลกอฮอล์: 1 = ดื่ม, 0 = ไม่ดื่ม
- เป้าหมาย สำเร็จ: เป้าหมายของวันนั้นและทำได้หรือไม่ (1 = สำเร็จ)
- อาหารเช้า อาหารกลางวัน อาหารเย็น บันทึก: สิ่งที่กิน การออกกำลังกาย เหตุการณ์ ฯลฯ
- ช่องว่างหมายถึง "ไม่ได้บันทึก" (ไม่ใช่ศูนย์)

# ข้อมูลอ้างอิง: เป้าหมายและนิสัยของฉัน
- เป้าหมาย: %4$s
- นิสัย: %5$s

# ข้อมูล (CSV)
วันที่,วัน,ร่างกาย,สมาธิ,อารมณ์,คุณภาพการนอน,ชั่วโมงนอน,เข้านอน,ตื่น,มือถือก่อนนอน,มือถือ_นาที,ปลดล็อก,แอลกอฮอล์,เป้าหมาย,สำเร็จ,อาหารเช้า,อาหารกลางวัน,อาหารเย็น,บันทึก
%6$s

# สิ่งที่อยากให้ช่วย
1. วันที่ดีกับวันที่แย่ต่างกันอย่างไร (การนอน เวลาเข้านอน มือถือก่อนนอน แอลกอฮอล์ อาหาร เวลาหน้าจอ วันในสัปดาห์ ฯลฯ) ใส่ตัวเลขที่ชัดเจนเท่าที่ทำได้
2. รูปแบบ "ฉันรู้สึกดีเมื่อ..." ไม่เกิน 3 ข้อ
3. นิสัยหรือการผสมกันที่ดูเหมือนทำให้ฉันแย่ลง (ถ้ามีบันทึกการเปิดมือถือ ให้บอกช่วงเวลาและเหตุผลที่ดูเสียเวลาด้วย)
4. 3 การกระทำเล็กๆ ที่ลองได้ตั้งแต่พรุ่งนี้
5. ข้อควรระวัง (ข้อมูลน้อยหรือเอนเอียง ฯลฯ) และสิ่งที่ควรบันทึกเพิ่มในอนาคต

# วิธีตอบ
- เริ่มด้วยสรุป 3 บรรทัด แล้วจึงอธิบายโดยละเอียด
- ตอบเป็น%7$s ด้วยภาษาง่ายๆ ไม่ใช้ศัพท์เฉพาะ
- แยกสิ่งที่ข้อมูลบอกออกจากการคาดเดา
- อย่าวินิจฉัยทางการแพทย์""",
}

D["vi"] = {
    "this_month_so_far": "Tháng này (đến hôm qua)",
    "c_bed": "Giờ đi ngủ (tối qua)", "c_wake": "Giờ thức dậy (sáng nay)", "c_time_clear": "Xóa",
    "c_bedphone": "Điện thoại trong 30 phút trước khi ngủ: %1$s",
    "c_bedphone_none": "Điện thoại trong 30 phút trước khi ngủ: không dùng",
    "c_bedphone_unknown": "Điện thoại trong 30 phút trước khi ngủ: không rõ (cần cho phép “Quyền truy cập dữ liệu sử dụng”; điện thoại chỉ lưu khoảng một tuần)",
    "c_alcohol": "Rượu bia", "alc_no": "Không", "alc_yes": "Có",
    "tr_alcohol": "Có uống rượu bia", "tr_bedphone": "Điện thoại trước khi ngủ",
    "color_cream": "Vàng nhạt", "color_indigo": "Xanh chàm",
    "pie_title": "Thời gian hôm nay theo ứng dụng", "pie_other": "Khác",
    "pie_need": "Để xem thời gian theo từng ứng dụng, hãy cho phép “Quyền truy cập dữ liệu sử dụng”.",
    "pie_none": "Hôm nay chưa có ứng dụng nào được dùng từ 1 phút trở lên.",
    "pie_note": "Tính từ dữ liệu sử dụng của điện thoại (cùng dữ liệu với Digital Wellbeing). Không tính thời gian trên màn hình chính này.",
    "goal_done": "Đã đạt", "goal_done_btn": "Mình làm được rồi!", "goal_undo": "Bỏ đánh dấu đã đạt", "goal_edit_btn": "Sửa mục tiêu",
    "congrats_title": "Chúc mừng!", "congrats_body": "Bạn đã đạt mục tiêu hôm nay. Làm tốt lắm!",
    "page_condition": "Thể trạng", "c_overall": "Cơ thể", "c_overall_hint": "Cơ thể khỏe khoắn đến mức nào",
    "st_note": "Thời gian màn hình bật và đã mở khóa. Nếu cho phép “Quyền truy cập dữ liệu sử dụng”, sẽ bổ sung bằng dữ liệu của điện thoại (7 ngày gần nhất).",
    "tr_wait": "Ghi cơ thể, tập trung và tâm trạng trong 5 ngày để xem ngày tốt khác gì (hiện có %1$d ngày).",
    "tr_good": "TB từ 7 trở lên\n(%1$d ngày)",
    "tr_note": "* Ngày tốt được chia theo điểm trung bình của cơ thể, tập trung và tâm trạng. Để phân tích cả bữa ăn, hãy thử “Hỏi AI” bên dưới.",
    "ai_unlock_section": "# Lý do tôi mở điện thoại (14 ngày gần nhất, trả lời mỗi lần mở khóa)\nngày,giờ,lúc đó đang làm gì,lý do mở điện thoại\n%1$s",
    "ai_prompt": """Bạn là một huấn luyện viên am hiểu về thói quen hằng ngày và sức khỏe.
Dưới đây là dữ liệu tôi ghi lại mỗi ngày trên điện thoại (%1$d ngày, từ %2$s đến %3$s). Hãy phân tích khi nào tôi thường cảm thấy tốt nhất.

# Các cột
- cơ_thể, tập_trung, tâm_trạng, chất_lượng_ngủ: tự đánh giá từ 0–10 (10 là tốt nhất). Hãy đánh giá ngày tốt bằng trung bình của cơ thể, tập trung và tâm trạng
- giờ_ngủ: số giờ ngủ đêm trước. đi_ngủ, thức_dậy: giờ tôi đi ngủ tối hôm trước và thức dậy sáng hôm đó
- đt_trước_khi_ngủ: số phút dùng điện thoại trong 30 phút trước khi đi ngủ
- đt_phút: số phút màn hình bật và được dùng trong ngày. mở_khóa: số lần mở khóa
- rượu_bia: 1 = có uống, 0 = không
- mục_tiêu, đạt: mục tiêu của ngày đó và tôi có đạt không (1 = đạt)
- sáng, trưa, tối, ghi_chú: những gì tôi ăn, tập thể dục, sự kiện, v.v.
- Ô trống nghĩa là "không ghi" (không phải 0)

# Tham khảo: mục tiêu và thói quen của tôi
- Mục tiêu: %4$s
- Thói quen: %5$s

# Dữ liệu (CSV)
ngày,thứ,cơ_thể,tập_trung,tâm_trạng,chất_lượng_ngủ,giờ_ngủ,đi_ngủ,thức_dậy,đt_trước_khi_ngủ,đt_phút,mở_khóa,rượu_bia,mục_tiêu,đạt,sáng,trưa,tối,ghi_chú
%6$s

# Điều tôi muốn
1. Ngày tốt và ngày xấu khác nhau thế nào (giấc ngủ, giờ đi ngủ, điện thoại trước khi ngủ, rượu bia, bữa ăn, thời gian dùng màn hình, thứ trong tuần, v.v.), dùng số liệu cụ thể nếu có thể
2. Tối đa 3 quy luật kiểu "tôi thấy khỏe khi..."
3. Thói quen hoặc kết hợp có vẻ làm tôi đi xuống (nếu có dữ liệu mở khóa, cả khung giờ và lý do có vẻ lãng phí)
4. 3 hành động nhỏ tôi có thể thử từ ngày mai
5. Lưu ý (dữ liệu ít hoặc lệch, v.v.) và những mục nên ghi thêm trong tương lai

# Cách trả lời
- Bắt đầu bằng phần tóm tắt 3 dòng, sau đó giải thích chi tiết
- Trả lời bằng %7$s, ngôn ngữ dễ hiểu, không dùng thuật ngữ
- Tách riêng điều dữ liệu cho thấy và điều chỉ là phỏng đoán
- Không đưa ra chẩn đoán y khoa""",
}

D["hi"] = {
    "this_month_so_far": "इस महीने (कल तक)",
    "c_bed": "सोने का समय (कल रात)", "c_wake": "जागने का समय (आज सुबह)", "c_time_clear": "हटाएँ",
    "c_bedphone": "सोने से पहले 30 मिनट में फ़ोन: %1$s",
    "c_bedphone_none": "सोने से पहले 30 मिनट में फ़ोन: इस्तेमाल नहीं किया",
    "c_bedphone_unknown": "सोने से पहले 30 मिनट में फ़ोन: पता नहीं (“उपयोग का ऐक्सेस” चाहिए; फ़ोन अपना रिकॉर्ड लगभग एक हफ़्ता रखता है)",
    "c_alcohol": "शराब", "alc_no": "नहीं", "alc_yes": "हाँ",
    "tr_alcohol": "शराब पी", "tr_bedphone": "सोने से पहले फ़ोन",
    "color_cream": "हल्का पीला", "color_indigo": "गहरा नीला",
    "pie_title": "आज ऐप के हिसाब से समय", "pie_other": "अन्य",
    "pie_need": "हर ऐप का समय देखने के लिए “उपयोग का ऐक्सेस” की अनुमति दें।",
    "pie_none": "आज अभी तक किसी ऐप का इस्तेमाल 1 मिनट या ज़्यादा नहीं हुआ है।",
    "pie_note": "फ़ोन के उपयोग रिकॉर्ड से गिना गया (Digital Wellbeing वाला ही डेटा)। इस होम स्क्रीन पर बिताया समय शामिल नहीं है।",
    "goal_done": "पूरा हुआ", "goal_done_btn": "मैंने कर दिखाया!", "goal_undo": "पूरा न हुआ मानें", "goal_edit_btn": "लक्ष्य बदलें",
    "congrats_title": "बधाई हो!", "congrats_body": "आपने आज का लक्ष्य पूरा कर लिया। बहुत बढ़िया!",
    "page_condition": "शरीर की हालत", "c_overall": "शरीर", "c_overall_hint": "शरीर में कितनी ताज़गी थी",
    "st_note": "स्क्रीन चालू और अनलॉक रहने का समय। “उपयोग का ऐक्सेस” की अनुमति हो, तो फ़ोन के रिकॉर्ड (पिछले 7 दिन) से भी पूरा किया जाता है।",
    "tr_wait": "5 दिन तक शरीर, ध्यान और मूड दर्ज करें, ताकि अच्छे दिनों में क्या अलग है यह दिखे (अभी %1$d दिन)।",
    "tr_good": "औसत 7 या ज़्यादा\n(%1$d दिन)",
    "tr_note": "* अच्छे दिन शरीर, ध्यान और मूड के औसत से अलग किए जाते हैं। खाने समेत गहराई से जानने के लिए नीचे “AI से पूछें” आज़माएँ।",
    "ai_unlock_section": "# मैंने फ़ोन क्यों खोला (पिछले 14 दिन, हर अनलॉक पर दिया गया जवाब)\nतारीख,समय,उस समय क्या कर रहा था,फ़ोन क्यों खोला\n%1$s",
    "ai_prompt": """आप एक कोच हैं जो रोज़ की आदतों और सेहत के बारे में अच्छी जानकारी रखते हैं।
नीचे वह डेटा है जो मैं हर दिन अपने फ़ोन में दर्ज करता हूँ (%1$d दिन, %2$s से %3$s तक)। कृपया विश्लेषण करें कि मैं कब सबसे अच्छा महसूस करता हूँ।

# कॉलम
- शरीर, ध्यान, मूड, नींद_की_गुणवत्ता: 0–10 तक ख़ुद की रेटिंग (10 सबसे अच्छा)। अच्छे दिन शरीर, ध्यान और मूड के औसत से तय करें
- नींद_घंटे: पिछली रात कितने घंटे सोया। सोना, जागना: पिछली रात कब सोया और उस सुबह कब जागा
- सोने_से_पहले_फ़ोन: सोने से पहले के 30 मिनट में फ़ोन इस्तेमाल के मिनट
- फ़ोन_मिनट: उस दिन स्क्रीन चालू रहकर इस्तेमाल होने के मिनट। अनलॉक: कितनी बार अनलॉक किया
- शराब: 1 = पी, 0 = नहीं
- लक्ष्य, पूरा: उस दिन का लक्ष्य और क्या मैंने उसे पूरा किया (1 = पूरा)
- नाश्ता, दोपहर_का_खाना, रात_का_खाना, नोट: क्या खाया, व्यायाम, घटनाएँ आदि
- ख़ाली का मतलब "दर्ज नहीं किया" (शून्य नहीं)

# संदर्भ के लिए: मेरे लक्ष्य और आदतें
- लक्ष्य: %4$s
- आदतें: %5$s

# डेटा (CSV)
तारीख,दिन,शरीर,ध्यान,मूड,नींद_की_गुणवत्ता,नींद_घंटे,सोना,जागना,सोने_से_पहले_फ़ोन,फ़ोन_मिनट,अनलॉक,शराब,लक्ष्य,पूरा,नाश्ता,दोपहर_का_खाना,रात_का_खाना,नोट
%6$s

# मैं क्या चाहता हूँ
1. अच्छे और बुरे दिनों में क्या फ़र्क है (नींद, सोने का समय, सोने से पहले फ़ोन, शराब, खाना, स्क्रीन टाइम, हफ़्ते का दिन आदि), जहाँ हो सके ठोस आँकड़ों के साथ
2. "मुझे तब अच्छा लगता है जब..." जैसे ज़्यादा से ज़्यादा 3 पैटर्न
3. ऐसी आदतें या मेल जो मुझे नीचे खींचते लगते हैं (अगर फ़ोन खोलने का रिकॉर्ड है, तो बेकार लगने वाले समय और कारण भी)
4. 3 छोटे कदम जो मैं कल से आज़मा सकूँ
5. सावधानियाँ (कम या एकतरफ़ा डेटा आदि) और आगे दर्ज करने लायक चीज़ें

# जवाब कैसे दें
- पहले 3 लाइन में सार दें, फिर विस्तार से समझाएँ
- %7$s में, आसान शब्दों में और बिना तकनीकी शब्दों के जवाब दें
- जो डेटा से दिखता है और जो अनुमान है, उन्हें अलग लिखें
- कोई मेडिकल निदान न करें""",
}

D["ar"] = {
    "this_month_so_far": "هذا الشهر (حتى أمس)",
    "c_bed": "وقت النوم (ليلة أمس)", "c_wake": "وقت الاستيقاظ (هذا الصباح)", "c_time_clear": "مسح",
    "c_bedphone": "الهاتف في آخر 30 دقيقة قبل النوم: %1$s",
    "c_bedphone_none": "الهاتف في آخر 30 دقيقة قبل النوم: لم يُستخدم",
    "c_bedphone_unknown": "الهاتف في آخر 30 دقيقة قبل النوم: غير معروف (يلزم إذن «الوصول إلى بيانات الاستخدام»، ويحتفظ الهاتف بسجلاته نحو أسبوع)",
    "c_alcohol": "الكحول", "alc_no": "لا", "alc_yes": "نعم",
    "tr_alcohol": "شرب الكحول", "tr_bedphone": "الهاتف قبل النوم",
    "color_cream": "أصفر فاتح", "color_indigo": "نيلي",
    "pie_title": "وقت اليوم حسب التطبيق", "pie_other": "أخرى",
    "pie_need": "لرؤية الوقت لكل تطبيق، اسمح بـ«الوصول إلى بيانات الاستخدام».",
    "pie_none": "لم يُستخدم أي تطبيق لمدة دقيقة أو أكثر اليوم بعد.",
    "pie_note": "محسوب من سجلات استخدام الهاتف (نفس بيانات «الصحة الرقمية»). لا يشمل الوقت على هذه الشاشة الرئيسية.",
    "goal_done": "تم", "goal_done_btn": "لقد فعلتها!", "goal_undo": "إلغاء الإنجاز", "goal_edit_btn": "تعديل الهدف",
    "congrats_title": "تهانينا!", "congrats_body": "لقد حققت هدف اليوم. أحسنت!",
    "page_condition": "حالة الجسم", "c_overall": "الجسم", "c_overall_hint": "مدى نشاط جسمك",
    "st_note": "الوقت الذي كانت فيه الشاشة مضاءة وغير مقفلة. إذا سمحت بـ«الوصول إلى بيانات الاستخدام»، يُستكمل أيضًا من سجلات الهاتف (آخر 7 أيام).",
    "tr_wait": "سجّل الجسم والتركيز والمزاج لمدة 5 أيام لترى ما الذي يميز أيامك الجيدة (حتى الآن %1$d).",
    "tr_good": "متوسط 7 فأكثر\n(%1$d أيام)",
    "tr_note": "* تُحدَّد الأيام الجيدة بمتوسط الجسم والتركيز والمزاج. لتحليل أعمق يشمل الوجبات، جرّب «اسأل الذكاء الاصطناعي» بالأسفل.",
    "ai_unlock_section": "# لماذا فتحت هاتفي (آخر 14 يومًا، أُجيب عند كل فتح قفل)\nالتاريخ,الوقت,ماذا كنت أفعل,لماذا فتحت الهاتف\n%1$s",
    "ai_prompt": """أنت مدرب لديك خبرة كبيرة في العادات اليومية والصحة.
فيما يلي البيانات التي أسجلها كل يوم على هاتفي (%1$d يومًا، من %2$s إلى %3$s). من فضلك حلّل متى أشعر عادةً بأفضل حال.

# الحقول
- الجسم، التركيز، المزاج، جودة_النوم: تقييم ذاتي من 0 إلى 10 (10 هو الأفضل). احكم على الأيام الجيدة بمتوسط الجسم والتركيز والمزاج
- ساعات_النوم: عدد ساعات النوم في الليلة السابقة. النوم، الاستيقاظ: متى نمت في الليلة السابقة ومتى استيقظت ذلك الصباح
- الهاتف_قبل_النوم: دقائق استخدام الهاتف في الثلاثين دقيقة قبل النوم
- الهاتف_دقائق: دقائق كانت فيها الشاشة مضاءة ومستخدمة في ذلك اليوم. فتح_القفل: عدد مرات فتح القفل
- الكحول: 1 = شربت، 0 = لا
- الهدف، تحقق: هدف ذلك اليوم وهل حققته (1 = تحقق)
- الإفطار، الغداء، العشاء، ملاحظات: ما أكلته، الرياضة، الأحداث وغيرها
- الخانة الفارغة تعني "لم يُسجَّل" (وليست صفرًا)

# للعلم: أهدافي وعاداتي
- الأهداف: %4$s
- العادات: %5$s

# البيانات (CSV)
التاريخ,اليوم,الجسم,التركيز,المزاج,جودة_النوم,ساعات_النوم,النوم,الاستيقاظ,الهاتف_قبل_النوم,الهاتف_دقائق,فتح_القفل,الكحول,الهدف,تحقق,الإفطار,الغداء,العشاء,ملاحظات
%6$s

# ما أريده
1. ما الذي يختلف بين الأيام الجيدة والسيئة (النوم، وقت النوم، الهاتف قبل النوم، الكحول، الوجبات، وقت الشاشة، يوم الأسبوع وغيرها)، بأرقام محددة قدر الإمكان
2. حتى 3 أنماط من نوع "أشعر بحال جيدة عندما..."
3. العادات أو التركيبات التي يبدو أنها تُضعف حالتي (وإن وُجدت سجلات فتح الهاتف، فالأوقات والأسباب التي تبدو مضيعة للوقت أيضًا)
4. 3 خطوات صغيرة يمكنني تجربتها بدءًا من الغد
5. ملاحظات للحذر (قلة البيانات أو انحيازها وغير ذلك) وأمور يستحق تسجيلها مستقبلًا

# طريقة الإجابة
- ابدأ بملخص من 3 أسطر، ثم اشرح بالتفصيل
- أجب باللغة %7$s، بكلمات بسيطة ودون مصطلحات متخصصة
- افصل بين ما تُظهره البيانات وما هو تخمين
- لا تقدّم تشخيصًا طبيًا""",
}

D["ur"] = {
    "this_month_so_far": "اس مہینے (کل تک)",
    "c_bed": "سونے کا وقت (کل رات)", "c_wake": "جاگنے کا وقت (آج صبح)", "c_time_clear": "صاف کریں",
    "c_bedphone": "سونے سے پہلے 30 منٹ میں فون: %1$s",
    "c_bedphone_none": "سونے سے پہلے 30 منٹ میں فون: استعمال نہیں کیا",
    "c_bedphone_unknown": "سونے سے پہلے 30 منٹ میں فون: معلوم نہیں (”استعمال تک رسائی“ کی اجازت درکار ہے؛ فون اپنا ریکارڈ تقریباً ایک ہفتہ رکھتا ہے)",
    "c_alcohol": "شراب", "alc_no": "نہیں", "alc_yes": "ہاں",
    "tr_alcohol": "شراب پی", "tr_bedphone": "سونے سے پہلے فون",
    "color_cream": "ہلکا پیلا", "color_indigo": "گہرا نیلا",
    "pie_title": "آج ایپ کے لحاظ سے وقت", "pie_other": "دیگر",
    "pie_need": "ہر ایپ کا وقت دیکھنے کے لیے ”استعمال تک رسائی“ کی اجازت دیں۔",
    "pie_none": "آج ابھی تک کوئی ایپ 1 منٹ یا زیادہ استعمال نہیں ہوئی۔",
    "pie_note": "فون کے استعمال کے ریکارڈ سے گنا گیا (Digital Wellbeing والا ہی ڈیٹا)۔ اس ہوم اسکرین پر گزارا وقت شامل نہیں۔",
    "goal_done": "مکمل", "goal_done_btn": "میں نے کر دکھایا!", "goal_undo": "مکمل نہ سمجھیں", "goal_edit_btn": "ہدف بدلیں",
    "congrats_title": "مبارک ہو!", "congrats_body": "آپ نے آج کا ہدف پورا کر لیا۔ بہت خوب!",
    "page_condition": "جسم کی حالت", "c_overall": "جسم", "c_overall_hint": "جسم میں کتنی تازگی تھی",
    "st_note": "اسکرین آن اور ان لاک رہنے کا وقت۔ اگر ”استعمال تک رسائی“ کی اجازت ہو تو فون کے ریکارڈ (پچھلے 7 دن) سے بھی پورا کیا جاتا ہے۔",
    "tr_wait": "5 دن تک جسم، توجہ اور موڈ درج کریں تاکہ اچھے دنوں کا فرق نظر آئے (ابھی %1$d دن)۔",
    "tr_good": "اوسط 7 یا زیادہ\n(%1$d دن)",
    "tr_note": "* اچھے دن جسم، توجہ اور موڈ کی اوسط سے الگ کیے جاتے ہیں۔ کھانے سمیت گہرے تجزیے کے لیے نیچے ”AI سے پوچھیں“ آزمائیں۔",
    "ai_unlock_section": "# میں نے فون کیوں کھولا (پچھلے 14 دن، ہر ان لاک پر دیا گیا جواب)\nتاریخ,وقت,اس وقت کیا کر رہا تھا,فون کیوں کھولا\n%1$s",
    "ai_prompt": """آپ ایک کوچ ہیں جو روزمرہ کی عادات اور صحت کے بارے میں اچھی معلومات رکھتے ہیں۔
نیچے وہ ڈیٹا ہے جو میں ہر روز اپنے فون میں درج کرتا ہوں (%1$d دن، %2$s سے %3$s تک)۔ براہ کرم تجزیہ کریں کہ میں کب سب سے اچھا محسوس کرتا ہوں۔

# کالم
- جسم، توجہ، موڈ، نیند_کا_معیار: 0 سے 10 تک خود کی درجہ بندی (10 سب سے اچھا)۔ اچھے دن جسم، توجہ اور موڈ کی اوسط سے طے کریں
- نیند_گھنٹے: پچھلی رات کتنے گھنٹے سویا۔ سونا، جاگنا: پچھلی رات کب سویا اور اس صبح کب جاگا
- سونے_سے_پہلے_فون: سونے سے پہلے 30 منٹ میں فون استعمال کے منٹ
- فون_منٹ: اس دن اسکرین آن رہ کر استعمال ہونے کے منٹ۔ ان_لاک: کتنی بار ان لاک کیا
- شراب: 1 = پی، 0 = نہیں
- ہدف، پورا: اس دن کا ہدف اور کیا میں نے اسے پورا کیا (1 = پورا)
- ناشتہ، دوپہر_کا_کھانا، رات_کا_کھانا، نوٹ: کیا کھایا، ورزش، واقعات وغیرہ
- خالی کا مطلب "درج نہیں کیا" ہے (صفر نہیں)

# حوالے کے لیے: میرے اہداف اور عادات
- اہداف: %4$s
- عادات: %5$s

# ڈیٹا (CSV)
تاریخ,دن,جسم,توجہ,موڈ,نیند_کا_معیار,نیند_گھنٹے,سونا,جاگنا,سونے_سے_پہلے_فون,فون_منٹ,ان_لاک,شراب,ہدف,پورا,ناشتہ,دوپہر_کا_کھانا,رات_کا_کھانا,نوٹ
%6$s

# میں کیا چاہتا ہوں
1. اچھے اور برے دنوں میں کیا فرق ہے (نیند، سونے کا وقت، سونے سے پہلے فون، شراب، کھانا، اسکرین ٹائم، ہفتے کا دن وغیرہ)، جہاں ممکن ہو ٹھوس اعداد کے ساتھ
2. "مجھے تب اچھا لگتا ہے جب..." جیسے زیادہ سے زیادہ 3 پیٹرن
3. ایسی عادات یا امتزاج جو میری حالت گراتے لگتے ہیں (اگر فون کھولنے کا ریکارڈ ہے تو بیکار لگنے والے اوقات اور وجوہات بھی)
4. 3 چھوٹے قدم جو میں کل سے آزما سکوں
5. احتیاطیں (کم یا یک طرفہ ڈیٹا وغیرہ) اور آئندہ درج کرنے کے قابل چیزیں

# جواب کیسے دیں
- پہلے 3 سطروں میں خلاصہ دیں، پھر تفصیل سے سمجھائیں
- %7$s میں، آسان الفاظ میں اور تکنیکی اصطلاحات کے بغیر جواب دیں
- جو ڈیٹا سے نظر آتا ہے اور جو اندازہ ہے، انہیں الگ لکھیں
- کوئی طبی تشخیص نہ کریں""",
}

D["tr"] = {
    "this_month_so_far": "Bu ay (düne kadar)",
    "c_bed": "Yatış saati (dün gece)", "c_wake": "Kalkış saati (bu sabah)", "c_time_clear": "Temizle",
    "c_bedphone": "Yatmadan önceki 30 dk telefon: %1$s",
    "c_bedphone_none": "Yatmadan önceki 30 dk telefon: kullanılmadı",
    "c_bedphone_unknown": "Yatmadan önceki 30 dk telefon: bilinmiyor (“Kullanım erişimi” izni gerekir; telefon kayıtlarını yaklaşık bir hafta tutar)",
    "c_alcohol": "Alkol", "alc_no": "Yok", "alc_yes": "Var",
    "tr_alcohol": "Alkol içildi", "tr_bedphone": "Yatmadan önce telefon",
    "color_cream": "Açık sarı", "color_indigo": "Lacivert",
    "pie_title": "Bugün uygulamaya göre süre", "pie_other": "Diğer",
    "pie_need": "Uygulama başına süreyi görmek için “Kullanım erişimi” iznini verin.",
    "pie_none": "Bugün henüz 1 dakika veya daha uzun kullanılan bir uygulama yok.",
    "pie_note": "Telefonun kullanım kayıtlarından hesaplanır (Dijital Denge ile aynı veri). Bu ana ekranda geçen süre dahil değildir.",
    "goal_done": "Başarıldı", "goal_done_btn": "Başardım!", "goal_undo": "Başarılmadı olarak işaretle", "goal_edit_btn": "Hedefi düzenle",
    "congrats_title": "Tebrikler!", "congrats_body": "Bugünkü hedefine ulaştın. Harika iş!",
    "page_condition": "Vücut durumu", "c_overall": "Vücut", "c_overall_hint": "Vücudun ne kadar enerjikti",
    "st_note": "Ekranın açık ve kilidinin açık olduğu süre. “Kullanım erişimi” izni varsa telefonun kayıtlarıyla da tamamlanır (son 7 gün).",
    "tr_wait": "İyi günlerinde neyin farklı olduğunu görmek için 5 gün boyunca vücut, odak ve ruh hâlini kaydet (şu ana kadar %1$d).",
    "tr_good": "Ortalama 7 ve üzeri\n(%1$d gün)",
    "tr_note": "* İyi günler vücut, odak ve ruh hâli ortalamasına göre ayrılır. Yemekleri de içeren daha derin bir analiz için aşağıdaki “Yapay zekâya sor”u dene.",
    "ai_unlock_section": "# Telefonumu neden açtım (son 14 gün, her kilit açışta verilen yanıt)\ntarih,saat,ne yapıyordum,telefonu neden açtım\n%1$s",
    "ai_prompt": """Sen günlük alışkanlıklar ve iyi oluş konusunda çok bilgili bir koçsun.
Aşağıda her gün telefonuma kaydettiğim veriler var (%1$d gün, %2$s ile %3$s arası). Kendimi en çok ne zaman iyi hissettiğimi analiz et.

# Alanlar
- vücut, odak, ruh_hali, uyku_kalitesi: 0–10 arası öz değerlendirme (10 en iyisi). İyi günleri vücut, odak ve ruh hâli ortalamasına göre değerlendir
- uyku_saat: önceki gece kaç saat uyuduğum. yatış, kalkış: önceki gece ne zaman yattığım ve o sabah ne zaman kalktığım
- yatmadan_önce_telefon: yatmadan önceki 30 dakikada telefon kullandığım dakika
- telefon_dk: o gün ekranın açık ve kullanımda olduğu dakika. kilit_açma: kilidi açma sayısı
- alkol: 1 = alkol içtim, 0 = içmedim
- hedef, başarıldı: o günün hedefi ve ona ulaşıp ulaşmadığım (1 = ulaştım)
- kahvaltı, öğle, akşam, notlar: ne yediğim, egzersiz, olaylar vb.
- Boş, "kaydedilmedi" demektir (sıfır değil)

# Bilgi için: hedeflerim ve alışkanlıklarım
- Hedefler: %4$s
- Alışkanlıklar: %5$s

# Veriler (CSV)
tarih,gün,vücut,odak,ruh_hali,uyku_kalitesi,uyku_saat,yatış,kalkış,yatmadan_önce_telefon,telefon_dk,kilit_açma,alkol,hedef,başarıldı,kahvaltı,öğle,akşam,notlar
%6$s

# İstediklerim
1. İyi ve kötü günler arasında ne fark var (uyku, yatış saati, yatmadan önce telefon, alkol, yemekler, ekran süresi, haftanın günü vb.), mümkünse somut sayılarla
2. "Şu durumda kendimi iyi hissediyorum" türünden en fazla 3 örüntü
3. Beni aşağı çekiyor gibi görünen alışkanlıklar veya kombinasyonlar (kilit açma kayıtları varsa, boşa geçmiş gibi görünen saatler ve nedenler de)
4. Yarından itibaren deneyebileceğim 3 küçük adım
5. Dikkat edilmesi gerekenler (az veya dengesiz veri vb.) ve ileride kaydetmeye değer şeyler

# Nasıl yanıtlamalısın
- 3 satırlık bir özetle başla, sonra ayrıntılı açıkla
- %7$s yanıt ver; sade bir dille, teknik terim kullanmadan
- Verilerin gösterdiğini tahminlerden ayır
- Tıbbi teşhis koyma""",
}

D["bn"] = {
    "this_month_so_far": "এই মাস (গতকাল পর্যন্ত)",
    "c_bed": "ঘুমাতে যাওয়ার সময় (গত রাত)", "c_wake": "ঘুম থেকে ওঠার সময় (আজ সকাল)", "c_time_clear": "মুছুন",
    "c_bedphone": "ঘুমের আগের 30 মিনিটে ফোন: %1$s",
    "c_bedphone_none": "ঘুমের আগের 30 মিনিটে ফোন: ব্যবহার হয়নি",
    "c_bedphone_unknown": "ঘুমের আগের 30 মিনিটে ফোন: জানা নেই (“ব্যবহারের অ্যাক্সেস” অনুমতি লাগবে; ফোন প্রায় এক সপ্তাহ রেকর্ড রাখে)",
    "c_alcohol": "মদ", "alc_no": "না", "alc_yes": "হ্যাঁ",
    "tr_alcohol": "মদ পান করেছি", "tr_bedphone": "ঘুমের আগে ফোন",
    "color_cream": "হালকা হলুদ", "color_indigo": "গাঢ় নীল",
    "pie_title": "আজ অ্যাপ অনুযায়ী সময়", "pie_other": "অন্যান্য",
    "pie_need": "প্রতিটি অ্যাপের সময় দেখতে “ব্যবহারের অ্যাক্সেস” অনুমতি দিন।",
    "pie_none": "আজ এখনও কোনো অ্যাপ 1 মিনিট বা তার বেশি ব্যবহার হয়নি।",
    "pie_note": "ফোনের ব্যবহারের রেকর্ড থেকে গোনা (Digital Wellbeing-এর একই ডেটা)। এই হোম স্ক্রিনের সময় ধরা হয়নি।",
    "goal_done": "সম্পন্ন", "goal_done_btn": "আমি পেরেছি!", "goal_undo": "সম্পন্ন নয় বলে চিহ্নিত করুন", "goal_edit_btn": "লক্ষ্য বদলান",
    "congrats_title": "অভিনন্দন!", "congrats_body": "আপনি আজকের লক্ষ্য পূরণ করেছেন। দারুণ!",
    "page_condition": "শরীরের অবস্থা", "c_overall": "শরীর", "c_overall_hint": "শরীর কতটা চনমনে ছিল",
    "st_note": "স্ক্রিন চালু ও আনলক থাকার সময়। “ব্যবহারের অ্যাক্সেস” অনুমতি থাকলে ফোনের রেকর্ড (গত 7 দিন) থেকেও পূরণ করা হয়।",
    "tr_wait": "5 দিন শরীর, মনোযোগ ও মেজাজ লিখুন, তাহলে ভালো দিনগুলো কোথায় আলাদা তা এখানে দেখা যাবে (এখন পর্যন্ত %1$d দিন)।",
    "tr_good": "গড় 7 বা বেশি\n(%1$d দিন)",
    "tr_note": "* শরীর, মনোযোগ ও মেজাজের গড় দিয়ে ভালো দিন আলাদা করা হয়। খাবারসহ গভীর বিশ্লেষণের জন্য নিচের “AI-কে জিজ্ঞেস করুন” চেষ্টা করুন।",
    "ai_unlock_section": "# আমি কেন ফোন খুলেছিলাম (গত 14 দিন, প্রতিবার আনলকের সময় দেওয়া উত্তর)\nতারিখ,সময়,তখন কী করছিলাম,কেন ফোন খুলেছিলাম\n%1$s",
    "ai_prompt": """আপনি একজন কোচ, যিনি দৈনন্দিন অভ্যাস ও সুস্থতা নিয়ে ভালো জানেন।
নিচে সেই ডেটা আছে যা আমি প্রতিদিন ফোনে লিখে রাখি (%1$d দিন, %2$s থেকে %3$s)। আমি কখন সবচেয়ে ভালো বোধ করি তা বিশ্লেষণ করুন।

# কলাম
- শরীর, মনোযোগ, মেজাজ, ঘুমের_মান: 0–10 নিজের দেওয়া নম্বর (10 সবচেয়ে ভালো)। ভালো দিন শরীর, মনোযোগ ও মেজাজের গড় দিয়ে বিচার করুন
- ঘুম_ঘণ্টা: আগের রাতে কত ঘণ্টা ঘুমিয়েছি। ঘুমানো, ওঠা: আগের রাতে কখন ঘুমাতে গেছি আর সেদিন সকালে কখন উঠেছি
- ঘুমের_আগে_ফোন: ঘুমাতে যাওয়ার আগের 30 মিনিটে ফোন ব্যবহারের মিনিট
- ফোন_মিনিট: সেদিন স্ক্রিন চালু রেখে ব্যবহারের মিনিট। আনলক: কতবার আনলক করেছি
- মদ: 1 = পান করেছি, 0 = না
- লক্ষ্য, পূরণ: সেদিনের লক্ষ্য আর তা পূরণ হয়েছে কি না (1 = পূরণ)
- সকালের_খাবার, দুপুরের_খাবার, রাতের_খাবার, নোট: কী খেয়েছি, ব্যায়াম, ঘটনা ইত্যাদি
- ফাঁকা মানে "লেখা হয়নি" (শূন্য নয়)

# তথ্যের জন্য: আমার লক্ষ্য ও অভ্যাস
- লক্ষ্য: %4$s
- অভ্যাস: %5$s

# ডেটা (CSV)
তারিখ,বার,শরীর,মনোযোগ,মেজাজ,ঘুমের_মান,ঘুম_ঘণ্টা,ঘুমানো,ওঠা,ঘুমের_আগে_ফোন,ফোন_মিনিট,আনলক,মদ,লক্ষ্য,পূরণ,সকালের_খাবার,দুপুরের_খাবার,রাতের_খাবার,নোট
%6$s

# আমি যা চাই
1. ভালো আর খারাপ দিনের মধ্যে কী পার্থক্য (ঘুম, ঘুমানোর সময়, ঘুমের আগে ফোন, মদ, খাবার, স্ক্রিন টাইম, সপ্তাহের দিন ইত্যাদি), যতটা সম্ভব নির্দিষ্ট সংখ্যা দিয়ে
2. "আমি তখন ভালো থাকি যখন..." ধরনের সর্বোচ্চ 3টি প্যাটার্ন
3. যেসব অভ্যাস বা মিলন আমাকে খারাপ করে বলে মনে হয় (ফোন খোলার রেকর্ড থাকলে, অপচয় মনে হওয়া সময় ও কারণও)
4. কাল থেকে চেষ্টা করা যায় এমন 3টি ছোট কাজ
5. সতর্কতা (কম বা একপেশে ডেটা ইত্যাদি) এবং ভবিষ্যতে লিখে রাখার মতো বিষয়

# কীভাবে উত্তর দেবেন
- প্রথমে 3 লাইনে সারাংশ দিন, তারপর বিস্তারিত বোঝান
- %7$s ভাষায়, সহজ কথায়, পারিভাষিক শব্দ ছাড়া উত্তর দিন
- ডেটা যা দেখায় আর যা অনুমান, তা আলাদা করে লিখুন
- কোনো চিকিৎসা নির্ণয় করবেন না""",
}

D["mr"] = {
    "this_month_so_far": "या महिन्यात (कालपर्यंत)",
    "c_bed": "झोपण्याची वेळ (काल रात्री)", "c_wake": "उठण्याची वेळ (आज सकाळी)", "c_time_clear": "काढून टाका",
    "c_bedphone": "झोपण्यापूर्वी 30 मिनिटांत फोन: %1$s",
    "c_bedphone_none": "झोपण्यापूर्वी 30 मिनिटांत फोन: वापरला नाही",
    "c_bedphone_unknown": "झोपण्यापूर्वी 30 मिनिटांत फोन: माहीत नाही (“वापर ऍक्सेस” परवानगी हवी; फोन आपली नोंद साधारण एक आठवडा ठेवतो)",
    "c_alcohol": "दारू", "alc_no": "नाही", "alc_yes": "हो",
    "tr_alcohol": "दारू घेतली", "tr_bedphone": "झोपण्यापूर्वी फोन",
    "color_cream": "फिकट पिवळा", "color_indigo": "गडद निळा",
    "pie_title": "आज ॲपनुसार वेळ", "pie_other": "इतर",
    "pie_need": "प्रत्येक ॲपची वेळ पाहण्यासाठी “वापर ऍक्सेस” ची परवानगी द्या.",
    "pie_none": "आज अजून कोणतेही ॲप 1 मिनिट किंवा जास्त वापरले गेले नाही.",
    "pie_note": "फोनच्या वापराच्या नोंदींवरून मोजले (Digital Wellbeing सारखाच डेटा). या होम स्क्रीनवरचा वेळ धरलेला नाही.",
    "goal_done": "पूर्ण", "goal_done_btn": "मी करून दाखवलं!", "goal_undo": "पूर्ण नाही म्हणून खूण करा", "goal_edit_btn": "ध्येय बदला",
    "congrats_title": "अभिनंदन!", "congrats_body": "तुम्ही आजचे ध्येय पूर्ण केले. खूप छान!",
    "page_condition": "शरीराची स्थिती", "c_overall": "शरीर", "c_overall_hint": "शरीरात किती उत्साह होता",
    "st_note": "स्क्रीन चालू आणि अनलॉक असलेला वेळ. “वापर ऍक्सेस” ची परवानगी असल्यास फोनच्या नोंदींतूनही (गेले 7 दिवस) भरला जातो.",
    "tr_wait": "5 दिवस शरीर, लक्ष आणि मूड नोंदवा, म्हणजे चांगल्या दिवसांत काय वेगळे आहे ते इथे दिसेल (आतापर्यंत %1$d दिवस).",
    "tr_good": "सरासरी 7 किंवा जास्त\n(%1$d दिवस)",
    "tr_note": "* चांगले दिवस शरीर, लक्ष आणि मूडच्या सरासरीवरून वेगळे केले जातात. जेवणासह सखोल विश्लेषणासाठी खालील “AI ला विचारा” वापरून पाहा.",
    "ai_unlock_section": "# मी फोन का उघडला (गेले 14 दिवस, प्रत्येक अनलॉकला दिलेले उत्तर)\nतारीख,वेळ,तेव्हा काय करत होतो,फोन का उघडला\n%1$s",
    "ai_prompt": """तुम्ही रोजच्या सवयी आणि आरोग्य याबद्दल चांगली माहिती असलेले कोच आहात.
खाली मी रोज फोनवर नोंदवत असलेला डेटा आहे (%1$d दिवस, %2$s ते %3$s). मला सर्वात चांगले केव्हा वाटते याचे विश्लेषण करा.

# स्तंभ
- शरीर, लक्ष, मूड, झोपेची_गुणवत्ता: 0–10 स्वतःचे गुण (10 सर्वोत्तम). चांगले दिवस शरीर, लक्ष आणि मूडच्या सरासरीवरून ठरवा
- झोप_तास: आदल्या रात्री किती तास झोपलो. झोपणे, उठणे: आदल्या रात्री केव्हा झोपलो आणि त्या सकाळी केव्हा उठलो
- झोपण्यापूर्वी_फोन: झोपण्यापूर्वीच्या 30 मिनिटांत फोन वापरलेली मिनिटे
- फोन_मिनिटे: त्या दिवशी स्क्रीन चालू ठेवून वापरलेली मिनिटे. अनलॉक: किती वेळा अनलॉक केले
- दारू: 1 = घेतली, 0 = नाही
- ध्येय, पूर्ण: त्या दिवसाचे ध्येय आणि ते पूर्ण झाले का (1 = पूर्ण)
- नाश्ता, दुपारचे_जेवण, रात्रीचे_जेवण, नोंद: काय खाल्ले, व्यायाम, घडलेल्या गोष्टी इ.
- रिकामे म्हणजे "नोंद केली नाही" (शून्य नाही)

# संदर्भासाठी: माझी ध्येये आणि सवयी
- ध्येये: %4$s
- सवयी: %5$s

# डेटा (CSV)
तारीख,वार,शरीर,लक्ष,मूड,झोपेची_गुणवत्ता,झोप_तास,झोपणे,उठणे,झोपण्यापूर्वी_फोन,फोन_मिनिटे,अनलॉक,दारू,ध्येय,पूर्ण,नाश्ता,दुपारचे_जेवण,रात्रीचे_जेवण,नोंद
%6$s

# मला काय हवे आहे
1. चांगल्या आणि वाईट दिवसांत काय फरक आहे (झोप, झोपण्याची वेळ, झोपण्यापूर्वी फोन, दारू, जेवण, स्क्रीन वेळ, आठवड्याचा वार इ.), शक्य तिथे ठोस आकड्यांसह
2. "मला तेव्हा छान वाटते जेव्हा..." असे जास्तीत जास्त 3 नमुने
3. मला खाली खेचणाऱ्या वाटणाऱ्या सवयी किंवा जोड्या (फोन उघडल्याच्या नोंदी असल्यास, वाया गेल्यासारख्या वाटणाऱ्या वेळा आणि कारणेही)
4. उद्यापासून करून पाहता येतील अशा 3 छोट्या कृती
5. लक्षात ठेवण्याच्या गोष्टी (कमी किंवा एकांगी डेटा इ.) आणि पुढे नोंदवण्यासारख्या गोष्टी

# उत्तर कसे द्यावे
- आधी 3 ओळींत सारांश द्या, मग सविस्तर समजावा
- %7$s मध्ये, सोप्या शब्दांत आणि तांत्रिक शब्दांशिवाय उत्तर द्या
- डेटावरून दिसणारे आणि अंदाज वेगळे लिहा
- कोणतेही वैद्यकीय निदान करू नका""",
}

D["te"] = {
    "this_month_so_far": "ఈ నెల (నిన్నటి వరకు)",
    "c_bed": "పడుకున్న సమయం (నిన్న రాత్రి)", "c_wake": "లేచిన సమయం (ఈ ఉదయం)", "c_time_clear": "తొలగించు",
    "c_bedphone": "పడుకునే ముందు 30 నిమిషాల్లో ఫోన్: %1$s",
    "c_bedphone_none": "పడుకునే ముందు 30 నిమిషాల్లో ఫోన్: వాడలేదు",
    "c_bedphone_unknown": "పడుకునే ముందు 30 నిమిషాల్లో ఫోన్: తెలియదు (“వినియోగ యాక్సెస్” అనుమతి కావాలి; ఫోన్ తన రికార్డులను సుమారు ఒక వారం ఉంచుతుంది)",
    "c_alcohol": "మద్యం", "alc_no": "లేదు", "alc_yes": "ఉంది",
    "tr_alcohol": "మద్యం తాగారు", "tr_bedphone": "పడుకునే ముందు ఫోన్",
    "color_cream": "లేత పసుపు", "color_indigo": "ముదురు నీలం",
    "pie_title": "ఈ రోజు యాప్‌ల వారీ సమయం", "pie_other": "ఇతరాలు",
    "pie_need": "ప్రతి యాప్ సమయం చూడాలంటే “వినియోగ యాక్సెస్” అనుమతి ఇవ్వండి.",
    "pie_none": "ఈ రోజు ఇంకా ఏ యాప్‌నూ 1 నిమిషం లేదా ఎక్కువ వాడలేదు.",
    "pie_note": "ఫోన్ వినియోగ రికార్డుల నుంచి లెక్కించాం (Digital Wellbeing లాంటి అదే డేటా). ఈ హోమ్ స్క్రీన్‌పై గడిపిన సమయం చేర్చలేదు.",
    "goal_done": "పూర్తి", "goal_done_btn": "నేను సాధించాను!", "goal_undo": "పూర్తి కాలేదని గుర్తించు", "goal_edit_btn": "లక్ష్యాన్ని మార్చు",
    "congrats_title": "అభినందనలు!", "congrats_body": "మీరు ఈ రోజు లక్ష్యాన్ని సాధించారు. చాలా బాగుంది!",
    "page_condition": "శరీర స్థితి", "c_overall": "శరీరం", "c_overall_hint": "శరీరం ఎంత ఉత్సాహంగా ఉంది",
    "st_note": "స్క్రీన్ ఆన్‌లో, అన్‌లాక్‌లో ఉన్న సమయం. “వినియోగ యాక్సెస్” అనుమతి ఉంటే, ఫోన్ రికార్డుల (గత 7 రోజులు) నుంచి కూడా నింపుతాం.",
    "tr_wait": "5 రోజులు శరీరం, ఏకాగ్రత, మూడ్ నమోదు చేస్తే, మంచి రోజుల్లో తేడా ఇక్కడ కనిపిస్తుంది (ఇప్పటివరకు %1$d రోజులు).",
    "tr_good": "సగటు 7 లేదా ఎక్కువ\n(%1$d రోజులు)",
    "tr_note": "* శరీరం, ఏకాగ్రత, మూడ్ సగటుతో మంచి రోజులను వేరు చేస్తాం. భోజనంతో సహా లోతైన విశ్లేషణ కోసం కింద “AIని అడగండి” ప్రయత్నించండి.",
    "ai_unlock_section": "# నేను ఫోన్ ఎందుకు తెరిచాను (గత 14 రోజులు, ప్రతి అన్‌లాక్‌లో ఇచ్చిన సమాధానం)\nతేదీ,సమయం,అప్పుడు ఏం చేస్తున్నాను,ఫోన్ ఎందుకు తెరిచాను\n%1$s",
    "ai_prompt": """మీరు రోజువారీ అలవాట్లు, ఆరోగ్యం గురించి బాగా తెలిసిన కోచ్.
క్రింద నేను ప్రతి రోజు ఫోన్‌లో నమోదు చేసే డేటా ఉంది (%1$d రోజులు, %2$s నుంచి %3$s వరకు). నేను ఎప్పుడు అత్యుత్తమంగా అనిపిస్తానో విశ్లేషించండి.

# కాలమ్‌లు
- శరీరం, ఏకాగ్రత, మూడ్, నిద్ర_నాణ్యత: 0–10 స్వీయ రేటింగ్ (10 అత్యుత్తమం). మంచి రోజులను శరీరం, ఏకాగ్రత, మూడ్ సగటుతో నిర్ణయించండి
- నిద్ర_గంటలు: ముందు రాత్రి ఎన్ని గంటలు నిద్రపోయాను. పడుకోవడం, లేవడం: ముందు రాత్రి ఎప్పుడు పడుకున్నాను, ఆ ఉదయం ఎప్పుడు లేచాను
- పడుకునే_ముందు_ఫోన్: పడుకునే ముందు 30 నిమిషాల్లో ఫోన్ వాడిన నిమిషాలు
- ఫోన్_నిమిషాలు: ఆ రోజు స్క్రీన్ ఆన్‌లో ఉండి వాడిన నిమిషాలు. అన్‌లాక్: ఎన్నిసార్లు అన్‌లాక్ చేశాను
- మద్యం: 1 = తాగాను, 0 = లేదు
- లక్ష్యం, సాధించాను: ఆ రోజు లక్ష్యం, దాన్ని సాధించానా (1 = సాధించాను)
- అల్పాహారం, మధ్యాహ్న_భోజనం, రాత్రి_భోజనం, గమనికలు: ఏం తిన్నాను, వ్యాయామం, జరిగిన విషయాలు మొదలైనవి
- ఖాళీ అంటే "నమోదు చేయలేదు" (సున్నా కాదు)

# సూచన కోసం: నా లక్ష్యాలు, అలవాట్లు
- లక్ష్యాలు: %4$s
- అలవాట్లు: %5$s

# డేటా (CSV)
తేదీ,వారం,శరీరం,ఏకాగ్రత,మూడ్,నిద్ర_నాణ్యత,నిద్ర_గంటలు,పడుకోవడం,లేవడం,పడుకునే_ముందు_ఫోన్,ఫోన్_నిమిషాలు,అన్‌లాక్,మద్యం,లక్ష్యం,సాధించాను,అల్పాహారం,మధ్యాహ్న_భోజనం,రాత్రి_భోజనం,గమనికలు
%6$s

# నాకు కావలసింది
1. మంచి, చెడు రోజుల మధ్య తేడా ఏమిటి (నిద్ర, పడుకునే సమయం, పడుకునే ముందు ఫోన్, మద్యం, భోజనం, స్క్రీన్ సమయం, వారంలో రోజు మొదలైనవి), వీలైనంతవరకు కచ్చితమైన సంఖ్యలతో
2. "నేను ఇలా ఉన్నప్పుడు బాగుంటాను..." లాంటి గరిష్ఠంగా 3 నమూనాలు
3. నన్ను కిందకు లాగుతున్నట్టు అనిపించే అలవాట్లు లేదా కలయికలు (ఫోన్ తెరిచిన రికార్డులు ఉంటే, వృథా అనిపించే సమయాలు, కారణాలు కూడా)
4. రేపటి నుంచి ప్రయత్నించగల 3 చిన్న పనులు
5. జాగ్రత్తలు (తక్కువ లేదా ఒకవైపు డేటా మొదలైనవి), భవిష్యత్తులో నమోదు చేయదగిన అంశాలు

# ఎలా సమాధానం ఇవ్వాలి
- ముందు 3 లైన్లలో సారాంశం ఇచ్చి, తర్వాత వివరంగా చెప్పండి
- %7$sలో, సులభమైన మాటల్లో, సాంకేతిక పదాలు లేకుండా సమాధానం ఇవ్వండి
- డేటా చూపేదాన్ని, ఊహను వేర్వేరుగా రాయండి
- ఎలాంటి వైద్య నిర్ధారణ చేయవద్దు""",
}

D["ta"] = {
    "this_month_so_far": "இந்த மாதம் (நேற்று வரை)",
    "c_bed": "தூங்கிய நேரம் (நேற்றிரவு)", "c_wake": "எழுந்த நேரம் (இன்று காலை)", "c_time_clear": "அழி",
    "c_bedphone": "தூங்கும் முன் 30 நிமிடத்தில் போன்: %1$s",
    "c_bedphone_none": "தூங்கும் முன் 30 நிமிடத்தில் போன்: பயன்படுத்தவில்லை",
    "c_bedphone_unknown": "தூங்கும் முன் 30 நிமிடத்தில் போன்: தெரியவில்லை (“பயன்பாட்டு அணுகல்” அனுமதி தேவை; போன் தன் பதிவுகளைச் சுமார் ஒரு வாரம் வைத்திருக்கும்)",
    "c_alcohol": "மது", "alc_no": "இல்லை", "alc_yes": "ஆம்",
    "tr_alcohol": "மது அருந்தினேன்", "tr_bedphone": "தூங்கும் முன் போன்",
    "color_cream": "வெளிர் மஞ்சள்", "color_indigo": "அடர் நீலம்",
    "pie_title": "இன்று ஆப் வாரியான நேரம்", "pie_other": "மற்றவை",
    "pie_need": "ஒவ்வொரு ஆப்பின் நேரத்தைப் பார்க்க “பயன்பாட்டு அணுகல்” அனுமதி கொடுங்கள்.",
    "pie_none": "இன்று இன்னும் எந்த ஆப்பும் 1 நிமிடம் அல்லது அதற்கு மேல் பயன்படுத்தப்படவில்லை.",
    "pie_note": "போனின் பயன்பாட்டுப் பதிவுகளிலிருந்து கணக்கிடப்பட்டது (Digital Wellbeing-இன் அதே தரவு). இந்த முகப்புத் திரையில் இருந்த நேரம் சேர்க்கப்படவில்லை.",
    "goal_done": "முடிந்தது", "goal_done_btn": "நான் செய்துவிட்டேன்!", "goal_undo": "முடியவில்லை எனக் குறி", "goal_edit_btn": "இலக்கை மாற்று",
    "congrats_title": "வாழ்த்துகள்!", "congrats_body": "இன்றைய இலக்கை அடைந்துவிட்டீர்கள். அருமை!",
    "page_condition": "உடல் நிலை", "c_overall": "உடல்", "c_overall_hint": "உடல் எவ்வளவு சுறுசுறுப்பாக இருந்தது",
    "st_note": "திரை ஆன் ஆகி, திறக்கப்பட்டிருந்த நேரம். “பயன்பாட்டு அணுகல்” அனுமதித்திருந்தால், போனின் பதிவுகளிலிருந்தும் (கடந்த 7 நாட்கள்) நிரப்பப்படும்.",
    "tr_wait": "5 நாட்கள் உடல், கவனம், மனநிலையைப் பதிவுசெய்தால், நல்ல நாட்களில் என்ன வேறுபாடு என்பது இங்கே தெரியும் (இதுவரை %1$d நாட்கள்).",
    "tr_good": "சராசரி 7 அல்லது மேல்\n(%1$d நாட்கள்)",
    "tr_note": "* உடல், கவனம், மனநிலையின் சராசரியால் நல்ல நாட்கள் பிரிக்கப்படுகின்றன. உணவையும் சேர்த்து ஆழமாக அறிய, கீழே உள்ள “AI-யிடம் கேள்” முயலுங்கள்.",
    "ai_unlock_section": "# நான் ஏன் போனைத் திறந்தேன் (கடந்த 14 நாட்கள், ஒவ்வொரு முறை திறக்கும்போதும் அளித்த பதில்)\nதேதி,நேரம்,அப்போது என்ன செய்துகொண்டிருந்தேன்,போனைத் திறந்த காரணம்\n%1$s",
    "ai_prompt": """நீங்கள் அன்றாடப் பழக்கங்கள் மற்றும் நலவாழ்வு பற்றி நன்கு அறிந்த பயிற்சியாளர்.
கீழே நான் தினமும் போனில் பதிவுசெய்யும் தரவு உள்ளது (%1$d நாட்கள், %2$s முதல் %3$s வரை). நான் எப்போது சிறப்பாக உணர்கிறேன் என்பதை ஆராயுங்கள்.

# நெடுவரிசைகள்
- உடல், கவனம், மனநிலை, தூக்கத்தின்_தரம்: 0–10 சுய மதிப்பீடு (10 சிறந்தது). நல்ல நாட்களை உடல், கவனம், மனநிலையின் சராசரியால் தீர்மானியுங்கள்
- தூக்க_மணி: முந்தைய இரவு எத்தனை மணி நேரம் தூங்கினேன். தூங்குதல், எழுதல்: முந்தைய இரவு எப்போது தூங்கினேன், அன்று காலை எப்போது எழுந்தேன்
- தூங்கும்_முன்_போன்: தூங்குவதற்கு முந்தைய 30 நிமிடத்தில் போன் பயன்படுத்திய நிமிடங்கள்
- போன்_நிமிடம்: அன்று திரை ஆன் ஆகிப் பயன்பாட்டில் இருந்த நிமிடங்கள். திறப்புகள்: எத்தனை முறை திறந்தேன்
- மது: 1 = அருந்தினேன், 0 = இல்லை
- இலக்கு, அடைந்தது: அன்றைய இலக்கு, அதை அடைந்தேனா (1 = அடைந்தேன்)
- காலை_உணவு, மதிய_உணவு, இரவு_உணவு, குறிப்புகள்: என்ன சாப்பிட்டேன், உடற்பயிற்சி, நிகழ்வுகள் போன்றவை
- காலியாக இருந்தால் "பதிவுசெய்யவில்லை" என்று பொருள் (பூஜ்ஜியம் அல்ல)

# குறிப்புக்காக: என் இலக்குகளும் பழக்கங்களும்
- இலக்குகள்: %4$s
- பழக்கங்கள்: %5$s

# தரவு (CSV)
தேதி,கிழமை,உடல்,கவனம்,மனநிலை,தூக்கத்தின்_தரம்,தூக்க_மணி,தூங்குதல்,எழுதல்,தூங்கும்_முன்_போன்,போன்_நிமிடம்,திறப்புகள்,மது,இலக்கு,அடைந்தது,காலை_உணவு,மதிய_உணவு,இரவு_உணவு,குறிப்புகள்
%6$s

# நான் விரும்புவது
1. நல்ல நாட்களுக்கும் மோசமான நாட்களுக்கும் என்ன வேறுபாடு (தூக்கம், தூங்கும் நேரம், தூங்கும் முன் போன், மது, உணவு, திரை நேரம், வாரத்தின் நாள் போன்றவை), முடிந்தவரை உறுதியான எண்களுடன்
2. "நான் இப்படி இருக்கும்போது நன்றாக உணர்கிறேன்..." போன்ற அதிகபட்சம் 3 முறைகள்
3. என்னைக் கீழே இழுப்பதாகத் தோன்றும் பழக்கங்கள் அல்லது சேர்க்கைகள் (போன் திறந்த பதிவுகள் இருந்தால், வீணானதாகத் தோன்றும் நேரங்களும் காரணங்களும்)
4. நாளை முதல் முயலக்கூடிய 3 சிறிய செயல்கள்
5. கவனிக்க வேண்டியவை (குறைவான அல்லது ஒருபக்கச் சாய்வான தரவு போன்றவை) மற்றும் இனி பதிவுசெய்யத் தகுந்தவை

# எப்படிப் பதில் சொல்ல வேண்டும்
- முதலில் 3 வரிகளில் சுருக்கம் கொடுத்து, பின் விரிவாக விளக்குங்கள்
- %7$s மொழியில், எளிய சொற்களில், தொழில்நுட்பச் சொற்கள் இல்லாமல் பதிலளியுங்கள்
- தரவு காட்டுவதையும் ஊகத்தையும் தனித்தனியாக எழுதுங்கள்
- எந்த மருத்துவ நோயறிதலும் செய்ய வேண்டாம்""",
}

L = {code: [d[k] for k in KEYS] for code, d in D.items()}
