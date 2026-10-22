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
)

/** カウントダウン（例：入社まであと◯日） */
data class Countdown(val id: Long, val title: String, val date: LocalDate)

/** 今日のタスク。doneDate は完了した日（未完了は null） */
data class Task(val id: Long, val text: String, val doneDate: String? = null)

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

    var bgColor by mutableIntStateOf(prefs.getInt("bg", 0xFFFFFFFF.toInt()))
        private set
    var accentColor by mutableIntStateOf(prefs.getInt("accent", 0xFF1043E5.toInt()))
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
        addAll((prefs.getString("widgets", null) ?: "motto,screen,spend").split(',').filter { it in ALL_WIDGETS })
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
            add(Task(o.getLong("id"), o.getString("text"), if (o.isNull("done")) null else o.optString("done")))
        }
    }

    /** 前の日までに終わったタスクを片づける */
    fun pruneTasks(today: LocalDate) {
        val t = today.toString()
        if (tasks.removeAll { it.doneDate != null && it.doneDate < t }) saveTasks()
    }

    fun addTask(text: String) {
        tasks.add(Task(System.currentTimeMillis(), text))
        saveTasks()
    }

    fun toggleTask(id: Long, today: LocalDate) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        tasks[i] = tasks[i].copy(doneDate = if (tasks[i].doneDate == null) today.toString() else null)
        saveTasks()
    }

    fun updateTask(id: Long, text: String) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i >= 0) tasks[i] = tasks[i].copy(text = text)
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

    private fun saveTasks() {
        val arr = JSONArray()
        tasks.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.doneDate ?: JSONObject.NULL)) }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    // ---- 今日の調子（日付ごとの記録） ----
    val days = mutableStateMapOf<LocalDate, DayLog>().apply {
        val o = JSONObject(prefs.getString("days", "{}") ?: "{}")
        o.keys().forEach { k ->
            runCatching { put(LocalDate.parse(k), DayLog.fromJson(o.getJSONObject(k))) }
        }
    }

    fun day(date: LocalDate) = days[date] ?: DayLog()

    fun saveDay(date: LocalDate, log: DayLog) {
        if (log.isEmpty()) days.remove(date) else days[date] = log
        val o = JSONObject()
        days.forEach { (d, l) -> o.put(d.toString(), l.toJson()) }
        prefs.edit().putString("days", o.toString()).apply()
    }
}
