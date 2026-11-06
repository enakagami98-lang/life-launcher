package com.enakagami.lifelauncher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 目標・習慣の1行 */
data class Item(val id: Long, val text: String)

/** トップ画面に置ける部品（id → 名前） */
val ALL_WIDGETS = linkedMapOf(
    "motto" to R.string.w_motto,
    "goal" to R.string.goal_title,
    "screen" to R.string.w_screen,
    "spend" to R.string.w_spend,
    "tasks" to R.string.w_tasks,
    "countdown" to R.string.w_countdown,
    "weather" to R.string.w_weather,
    "habits" to R.string.w_habits,
    "steps" to R.string.w_steps,
    "yesterday" to R.string.w_yesterday,
    "quote" to R.string.w_quote,
)

/** 振り返り日記の1日分。書きたい日だけ。空の欄は "" */
data class JournalEntry(
    val story: String = "", val good: String = "", val goals: String = "", val noticed: String = "",
    val music: String = "", val waste: String = "", val happy: String = "", val challenge: String = "",
) {
    fun fields() = listOf(story, good, goals, noticed, music, waste, happy, challenge)
    fun isEmpty() = fields().all { it.isBlank() }
    fun toJson(): JSONObject = JSONObject().apply { JOURNAL_KEYS.zip(fields()).forEach { (k, v) -> if (v.isNotBlank()) put(k, v) } }
    companion object {
        val JOURNAL_KEYS = listOf("story", "good", "goals", "noticed", "music", "waste", "happy", "challenge")
        fun fromJson(o: JSONObject) = JOURNAL_KEYS.map { o.optString(it) }.let {
            JournalEntry(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7])
        }
        fun of(list: List<String>) = JournalEntry(list[0], list[1], list[2], list[3], list[4], list[5], list[6], list[7])
    }
}

/** 心に残った言葉。by＝誰の言葉・誰から聞いたか、source＝本・番組など、note＝前後の文脈やメモ */
data class Quote(val id: Long, val text: String, val by: String = "", val source: String = "", val note: String = "")

/** カウントダウン（例：入社まであと◯日） */
data class Countdown(val id: Long, val title: String, val date: LocalDate)

/** 今日のタスク。doneDate は完了した日（未完了は null） */
/** タスク1件。due は「何日の何時までに」（ミリ秒）。期限なしは null */
data class Task(val id: Long, val text: String, val doneDate: String? = null, val due: Long? = null)

/** お気に入りアプリ（label はユーザーが変えた表示名） */
data class Fav(val key: String, val label: String)

/** スマホ内（SharedPreferences）にすべてのデータを保存する。外部送信はしない。 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("life", Context.MODE_PRIVATE)

    var birth by mutableStateOf(
        prefs.getString("birth", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    )
        private set
    var lifespan by mutableIntStateOf(prefs.getInt("lifespan", 80))
        private set

    var bgColor by mutableIntStateOf(prefs.getInt("bg", 0xFFFFF7DA.toInt()))
        private set
    var accentColor by mutableIntStateOf(prefs.getInt("accent", 0xFF1C2E5C.toInt()))
        private set

    fun saveColors(bg: Int, accent: Int) {
        bgColor = bg
        accentColor = accent
        prefs.edit().putInt("bg", bg).putInt("accent", accent).apply()
    }

    val goals = mutableStateListOf<Item>().apply { addAll(loadItems("goals")) }
    val habits = mutableStateListOf<Item>().apply { addAll(loadItems("habits")) }
    val favorites = mutableStateListOf<Fav>().apply { addAll(loadFavs()) }
    /** 非表示にしたアプリのパッケージ名 */
    val hidden = mutableStateListOf<String>().apply {
        addAll(prefs.getStringSet("hidden", emptySet()).orEmpty().sorted())
    }

    /** 使い方（チュートリアル）を最後まで見たか、スキップしたか */
    var tutorialDone by mutableStateOf(prefs.getBoolean("tutorialDone", false))
        private set

    fun finishTutorial() {
        tutorialDone = true
        prefs.edit().putBoolean("tutorialDone", true).apply()
    }

    fun saveProfile(birth: LocalDate, lifespan: Int) {
        this.birth = birth
        this.lifespan = lifespan
        prefs.edit().putString("birth", birth.toString()).putInt("lifespan", lifespan).apply()
    }

    // ---- 目標・習慣 ----
    fun listFor(kind: String) = if (kind == "goals") goals else habits

    fun addItem(kind: String, text: String) {
        listFor(kind).add(Item(System.currentTimeMillis(), text))
        saveItems(kind)
    }

    fun updateItem(kind: String, id: Long, text: String) {
        val list = listFor(kind)
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) list[i] = list[i].copy(text = text)
        saveItems(kind)
    }

    fun deleteItem(kind: String, id: Long) {
        listFor(kind).removeAll { it.id == id }
        saveItems(kind)
    }

    fun moveItem(kind: String, from: Int, to: Int) {
        val list = listFor(kind)
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        saveItems(kind)
    }

    private fun loadItems(kind: String): List<Item> {
        val arr = JSONArray(prefs.getString(kind, "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Item(o.getLong("id"), o.getString("text"))
        }
    }

    private fun saveItems(kind: String) {
        val arr = JSONArray()
        listFor(kind).forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text)) }
        prefs.edit().putString(kind, arr.toString()).apply()
    }

    // ---- お気に入り ----
    fun isFavorite(key: String) = favorites.any { it.key == key }

    fun addFavorite(key: String, label: String) {
        if (!isFavorite(key)) favorites.add(Fav(key, label))
        saveFavs()
    }

    fun removeFavorite(key: String) {
        favorites.removeAll { it.key == key }
        saveFavs()
    }

    fun renameFavorite(key: String, label: String) {
        val i = favorites.indexOfFirst { it.key == key }
        if (i >= 0) favorites[i] = favorites[i].copy(label = label)
        saveFavs()
    }

    fun moveFavorite(from: Int, to: Int) {
        if (from !in favorites.indices || to !in favorites.indices) return
        favorites.add(to, favorites.removeAt(from))
        saveFavs()
    }

    private fun loadFavs(): List<Fav> {
        val arr = JSONArray(prefs.getString("favorites", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Fav(o.getString("key"), o.getString("label"))
        }
    }

    private fun saveFavs() {
        val arr = JSONArray()
        favorites.forEach { arr.put(JSONObject().put("key", it.key).put("label", it.label)) }
        prefs.edit().putString("favorites", arr.toString()).apply()
    }

    // ---- 非表示 ----
    fun hide(pkg: String) {
        if (pkg !in hidden) hidden.add(pkg)
        // 非表示にしたアプリはお気に入りからも外す
        favorites.removeAll { it.key.substringBefore('/') == pkg }
        saveFavs()
        saveHidden()
    }

    fun unhide(pkg: String) {
        hidden.remove(pkg)
        saveHidden()
    }

    private fun saveHidden() {
        prefs.edit().putStringSet("hidden", hidden.toSet()).apply()
    }

    // ---- トップ画面の部品（残り日数の下に並べるもの） ----
    /** 表示する部品の id を上から順に（ALL_WIDGETS のキー）。最初は モットー・今日のスマホ・支払額 */
    val widgets = mutableStateListOf<String>().apply {
        addAll((prefs.getString("widgets", null) ?: "motto,quote,screen,spend").split(',').filter { it in ALL_WIDGETS })
        // 今日の言葉（2026-11 追加）は、前から使っている人にも1回だけ出す。振り返り日記は専用のページに移した
        if (!prefs.getBoolean("addedQuoteJournal", false)) {
            if ("quote" !in this) add(indexOf("motto") + 1, "quote")
            prefs.edit().putBoolean("addedQuoteJournal", true).putString("widgets", joinToString(",")).apply()
        }
    }

    fun saveWidgets(list: List<String>) {
        widgets.clear(); widgets.addAll(list)
        prefs.edit().putString("widgets", list.joinToString(",")).apply()
    }

    var motto by mutableStateOf(prefs.getString("motto", "") ?: "")
        private set

    fun saveMotto(text: String) {
        motto = text
        prefs.edit().putString("motto", text).apply()
    }

    // ---- カウントダウン ----
    val countdowns = mutableStateListOf<Countdown>().apply {
        val a = JSONArray(prefs.getString("countdowns", "[]"))
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            runCatching { add(Countdown(o.getLong("id"), o.getString("title"), LocalDate.parse(o.getString("date")))) }
        }
    }

    fun addCountdown(title: String, date: LocalDate) {
        countdowns.add(Countdown(System.currentTimeMillis(), title, date)); saveCountdowns()
    }

    fun removeCountdown(id: Long) { countdowns.removeAll { it.id == id }; saveCountdowns() }

    private fun saveCountdowns() {
        val a = JSONArray()
        countdowns.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("date", it.date.toString())) }
        prefs.edit().putString("countdowns", a.toString()).apply()
    }

    // ---- 習慣のチェック（習慣の id → できた日） ----
    val habitDone = mutableStateMapOf<Long, Set<String>>().apply {
        val o = JSONObject(prefs.getString("habitDone", "{}") ?: "{}")
        o.keys().forEach { k ->
            val a = o.getJSONArray(k)
            runCatching { put(k.toLong(), (0 until a.length()).map { a.getString(it) }.toSet()) }
        }
    }

    fun isHabitDone(id: Long, date: LocalDate) = date.toString() in (habitDone[id] ?: emptySet())

    fun toggleHabit(id: Long, date: LocalDate) {
        val d = date.toString()
        val set = habitDone[id] ?: emptySet()
        // 古い記録は400日分だけ残す
        val keep = date.minusDays(400).toString()
        habitDone[id] = (if (d in set) set - d else set + d).filter { it >= keep }.toSet()
        val o = JSONObject()
        habitDone.forEach { (k, v) -> if (habits.any { it.id == k }) o.put(k.toString(), JSONArray(v.sorted())) }
        prefs.edit().putString("habitDone", o.toString()).apply()
    }

    /** 何日連続でできているか（今日まだなら昨日から数える） */
    fun habitStreak(id: Long, today: LocalDate): Int {
        val set = habitDone[id] ?: return 0
        var d = if (today.toString() in set) today else today.minusDays(1)
        var n = 0
        while (d.toString() in set) { n++; d = d.minusDays(1) }
        return n
    }

    // ---- 今日のタスク ----
    // 終わったタスクは次の日に消え、終わらなかったタスクは次の日に持ち越す
    val tasks = mutableStateListOf<Task>().apply {
        val arr = JSONArray(prefs.getString("tasks", "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(Task(o.getLong("id"), o.getString("text"), if (o.isNull("done")) null else o.optString("done"), o.optLong("due", -1).takeIf { it > 0 }))
        }
    }

    /** 前の日までに終わったタスクを片づける */
    fun pruneTasks(today: LocalDate) {
        val t = today.toString()
        if (tasks.removeAll { it.doneDate != null && it.doneDate < t }) saveTasks()
    }

    fun addTask(text: String, due: Long? = null) {
        tasks.add(Task(System.currentTimeMillis(), text, due = due))
        saveTasks()
        Guard.tearToday()
    }

    fun toggleTask(id: Long, today: LocalDate) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        tasks[i] = tasks[i].copy(doneDate = if (tasks[i].doneDate == null) today.toString() else null)
        saveTasks()
    }

    fun updateTask(id: Long, text: String, due: Long?) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i >= 0) tasks[i] = tasks[i].copy(text = text, due = due)
        saveTasks()
    }

    fun deleteTask(id: Long) {
        tasks.removeAll { it.id == id }
        saveTasks()
    }

    fun moveTask(from: Int, to: Int) {
        if (from !in tasks.indices || to !in tasks.indices) return
        tasks.add(to, tasks.removeAt(from))
        saveTasks()
    }

    /** 並び順：終わっていないものが先。その中は期限が近い順（期限なしは後ろ、登録した順） */
    fun sortedTasks(): List<Task> = tasks.sortedWith(compareBy<Task>({ it.doneDate != null }, { it.due ?: Long.MAX_VALUE }, { it.id }))

    private fun saveTasks() {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().put("id", t.id).put("text", t.text).put("done", t.doneDate ?: JSONObject.NULL).apply { t.due?.let { put("due", it) } })
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    // ---- 今日の調子（日付ごとの記録） ----
    val days = mutableStateMapOf<LocalDate, DayLog>().apply {
        val o = JSONObject(prefs.getString("days", "{}") ?: "{}")
        o.keys().forEach { k ->
            runCatching { put(LocalDate.parse(k), DayLog.fromJson(o.getJSONObject(k))) }
        }
    }

    // ---- 振り返り日記（書きたい日だけ。日付 → 1日分） ----
    val journal = mutableStateMapOf<LocalDate, JournalEntry>().apply {
        val o = JSONObject(prefs.getString("journal", "{}") ?: "{}")
        o.keys().forEach { k -> runCatching { put(LocalDate.parse(k), JournalEntry.fromJson(o.getJSONObject(k))) } }
    }

    /** 書いている途中も1文字ごとに保存する（画面が消えても書いた分が残るように） */
    fun saveJournal(date: LocalDate, entry: JournalEntry) {
        if (entry.isEmpty()) journal.remove(date) else journal[date] = entry
        val o = JSONObject()
        journal.forEach { (d, e) -> o.put(d.toString(), e.toJson()) }
        prefs.edit().putString("journal", o.toString()).apply()
    }

    // ---- 今日の言葉（登録した言葉から毎日1つ） ----
    val quotes = mutableStateListOf<Quote>().apply {
        val a = JSONArray(prefs.getString("quotes", "[]"))
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            add(Quote(o.getLong("id"), o.getString("text"), o.optString("by"), o.optString("source"), o.optString("note")))
        }
    }

    /** 今日の1つ。日付で決まるので、1日の間は何度開いても同じ言葉 */
    fun quoteOf(date: LocalDate): Quote? {
        if (quotes.isEmpty()) return null
        val list = quotes.sortedBy { it.id }
        return list[kotlin.random.Random(date.toEpochDay()).nextInt(list.size)]
    }

    fun saveQuote(q: Quote) {
        val i = quotes.indexOfFirst { it.id == q.id }
        if (i >= 0) quotes[i] = q else quotes.add(q)
        saveQuotes()
    }

    fun deleteQuote(id: Long) { quotes.removeAll { it.id == id }; saveQuotes() }

    private fun saveQuotes() {
        val a = JSONArray()
        quotes.forEach { a.put(JSONObject().put("id", it.id).put("text", it.text).put("by", it.by).put("source", it.source).put("note", it.note)) }
        prefs.edit().putString("quotes", a.toString()).apply()
    }

    fun day(date: LocalDate) = days[date] ?: DayLog()

    fun saveDay(date: LocalDate, log: DayLog) {
        if (log.isEmpty()) days.remove(date) else days[date] = log
        val o = JSONObject()
        days.forEach { (d, l) -> o.put(d.toString(), l.toJson()) }
        prefs.edit().putString("days", o.toString()).apply()
    }
}
