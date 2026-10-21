package com.enakagami.lifelauncher

import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.util.Locale
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LifePage(store: Store, today: LocalDate, apps: List<AppInfo>, openSettings: () -> Unit, openCondition: () -> Unit) {
    // スクロール位置は画面ごとに持つ（1つを使い回すと、見えていない寿命画面にスクロールが行ってしまう）
    val listState = rememberLazyListState()
    var renaming by remember { mutableStateOf<Fav?>(null) }
    var sheetFav by remember { mutableStateOf<Int?>(null) }
    val openApp = LocalOpenApp.current
    val haptic = LocalHapticFeedback.current
    // 長押し中に1度でも動かしたか（動かさずに離したら、いつものメニューを出す）
    var moved by remember { mutableStateOf(false) }
    // 長押し中、または離した直後か（長押しのあと指を離したとき、タップ扱いでアプリが開かないようにする）
    var holding by remember { mutableStateOf(false) }
    var releasedAt by remember { mutableStateOf(0L) }

    // アンインストール済みのアプリはお気に入りに出さない（一覧の読み込み前は全部出す）
    val installed = remember(apps) { apps.map { it.key }.toSet() }
    val favs = store.favorites.filter { apps.isEmpty() || it.key in installed }

    // お気に入りの長押しドラッグ並べ替え（寿命の欄や見出しには入らない）
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val a = store.favorites.indexOfFirst { it.key == from.key }
        val b = store.favorites.indexOfFirst { it.key == to.key }
        if (a >= 0 && b >= 0) {
            store.moveFavorite(a, b)
            moved = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val screenHeight = maxHeight
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        // 1画面目：寿命（画面いっぱい。小さい画面では少しはみ出してスクロールできる）
        item {
            Column(
                Modifier.heightIn(min = screenHeight).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 左上は今日の日付（「寿命」の見出しはやめた）
                PageHeader(dateWithWeekday(today)) {
                    RoundButton(Icons.Filled.Settings, stringResource(R.string.settings), CardBg, Ink) { openSettings() }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LifeCards(store.birth, store.lifespan, today) { openSettings() }
                    TopWidgets(store, today, openCondition)
                }
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.favorites), fontSize = 12.sp, color = SubInk)
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = SubInk)
                }
            }
        }
        // 下にスクロール：お気に入りアプリ
        item {
            Text(
                stringResource(R.string.favorites),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        if (favs.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.fav_empty),
                    color = SubInk,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        itemsIndexed(favs, key = { _, f -> f.key }) { _, fav ->
            ReorderableItem(reorder, key = fav.key) { dragging ->
                Text(
                    fav.label,
                    fontSize = 26.sp,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (dragging) Modifier.padding(horizontal = 8.dp).card() else Modifier)
                        .longPressDraggableHandle(
                            onDragStarted = {
                                moved = false
                                holding = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                holding = false
                                releasedAt = System.currentTimeMillis()
                                if (!moved) sheetFav = store.favorites.indexOfFirst { it.key == fav.key }
                            },
                        )
                        .clickable {
                            if (!holding && System.currentTimeMillis() - releasedAt > 400) openApp(fav.key)
                        }
                        .padding(horizontal = if (dragging) 16.dp else 24.dp, vertical = 16.dp),
                )
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
    }

    sheetFav?.let { index ->
        store.favorites.getOrNull(index)?.let { fav ->
            FavoriteActionSheet(
                fav = fav,
                onRename = { renaming = fav },
                onRemove = { store.removeFavorite(fav.key) },
                onDismiss = { sheetFav = null },
            )
        }
    }
    renaming?.let { fav ->
        TextInputDialog(
            title = stringResource(R.string.rename_title),
            initial = fav.label,
            onSave = { store.renameFavorite(fav.key, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun LifeCards(birth: LocalDate?, lifespan: Int, today: LocalDate, onSetup: () -> Unit) {
    val pad = Modifier.padding(horizontal = 20.dp)
    if (birth == null) {
        Column(
            pad.fillMaxWidth().card().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.setup_first), fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSetup) { Text(stringResource(R.string.setup), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        return
    }

    val death = birth.plusYears(lifespan.toLong())
    val days = ChronoUnit.DAYS.between(today, death).coerceAtLeast(0)
    val rest = if (days > 0) Period.between(today, death) else Period.ZERO
    val age = Period.between(birth, today).years

    // 残りの日数（このアプリの顔。ほかの欄は TopWidgets で自由に入れ替える）
    Column(
        pad.fillMaxWidth().card().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.days_left), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "%,d".format(days),
                fontSize = if (days >= 10000) 72.sp else 92.sp,
                fontWeight = FontWeight.Black,
                color = Blue,
                lineHeight = 96.sp,
            )
            Text(stringResource(R.string.unit_days), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(bottom = 16.dp, start = 4.dp))
        }
        Text(stringResource(R.string.ymd, rest.years, rest.months, rest.days), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.age_now, age), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubInk)
            Text(stringResource(R.string.lifespan_is, lifespan), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubInk)
        }
    }
}

@Composable
private fun FavoriteActionSheet(
    fav: Fav,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pkg = fav.key.substringBefore('/')
    var choosing by remember { mutableStateOf(false) }
    ActionSheet(fav.label, Guard.limits[pkg]?.let { stringResource(R.string.limit_badge, it) }, onDismiss) { close ->
        if (choosing) {
            LimitChooser(pkg, onBack = { choosing = false }, onDone = { close {} })
        } else {
            SheetItem(Icons.Filled.Edit, stringResource(R.string.rename), stringResource(R.string.rename_sub)) { close(onRename) }
            ReminderItems(pkg, onChoose = { choosing = true }, close = close)
            SheetItem(Icons.Filled.Close, stringResource(R.string.fav_remove), danger = true) { close(onRemove) }
        }
    }
}
