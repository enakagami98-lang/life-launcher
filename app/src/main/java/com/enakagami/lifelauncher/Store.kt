package com.enakagami.lifelauncher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 目標・習慣の1行 */
data class Item(val id: Long, val text: String)

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
}
