package com.enakagami.lifelauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun LifePage(store: Store, today: LocalDate, apps: List<AppInfo>, listState: LazyListState, openSettings: () -> Unit) {
    var renaming by remember { mutableStateOf<Fav?>(null) }
    val context = LocalContext.current
    val openApp = LocalOpenApp.current

    // アンインストール済みのアプリはお気に入りに出さない（一覧の読み込み前は全部出す）
    val installed = remember(apps) { apps.map { it.key }.toSet() }
    val favs = store.favorites.filter { apps.isEmpty() || it.key in installed }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val screenHeight = maxHeight
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        // 1画面目：寿命（画面いっぱい。小さい画面では少しはみ出してスクロールできる）
        item {
            Column(
                Modifier.heightIn(min = screenHeight).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                PageHeader("寿命") {
                    RoundButton(Icons.Filled.Settings, "設定", CardBg, Ink) { openSettings() }
                }
                LifeCards(store.birth, store.lifespan, today) { openSettings() }
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("お気に入り", fontSize = 12.sp, color = SubInk)
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = SubInk)
                }
            }
        }
        // 下にスクロール：お気に入りアプリ
        item {
            Text(
                "お気に入り",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        if (favs.isEmpty()) {
            item {
                Text(
                    "右にスワイプした「アプリ」の画面で、アプリを長押しすると追加できます。",
                    color = SubInk,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        itemsIndexed(favs, key = { _, f -> f.key }) { index, fav ->
            FavoriteRow(
                fav = fav,
                canUp = index > 0,
                canDown = index < favs.lastIndex,
                onOpen = { openApp(fav.key) },
                onRename = { renaming = fav },
                onMove = { delta ->
                    val from = store.favorites.indexOf(fav)
                    val target = favs.getOrNull(index + delta) ?: return@FavoriteRow
                    store.moveFavorite(from, store.favorites.indexOf(target))
                },
                onRemove = { store.removeFavorite(fav.key) },
            )
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
    }

    renaming?.let { fav ->
        TextInputDialog(
            title = "表示名を変更",
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
            Text("まずは誕生日と寿命を設定しましょう", fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSetup) { Text("設定する", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        return
    }

    val death = birth.plusYears(lifespan.toLong())
    val days = ChronoUnit.DAYS.between(today, death).coerceAtLeast(0)
    val weeks = days / 7
    val months = ChronoUnit.MONTHS.between(today, death).coerceAtLeast(0)
    val rest = if (days > 0) Period.between(today, death) else Period.ZERO
    val age = Period.between(birth, today).years
    val total = ChronoUnit.DAYS.between(birth, death).coerceAtLeast(1)
    val lived = ChronoUnit.DAYS.between(birth, today).coerceIn(0, total)
    val progress = lived.toFloat() / total

    Column(pad, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 残りの日数（大きく）
        Column(
            Modifier.fillMaxWidth().card().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("残りの日数", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%,d".format(days),
                    fontSize = if (days >= 10000) 72.sp else 92.sp,
                    fontWeight = FontWeight.Black,
                    color = Blue,
                    lineHeight = 96.sp,
                )
                Text("日", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(bottom = 16.dp, start = 4.dp))
            }
            Text("${rest.years}年${rest.months}ヶ月${rest.days}日", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        // 残りの週数・月数
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SmallStat("残りの週数", "%,d".format(weeks), "週", Modifier.weight(1f))
            SmallStat("残りの月数", "%,d".format(months), "月", Modifier.weight(1f))
        }
        // 人生の進捗
        Column(Modifier.fillMaxWidth().card().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("人生の進捗", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                Text(String.format(Locale.US, "%.1f%%", progress * 100), fontWeight = FontWeight.Black, color = Blue, fontSize = 18.sp)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).background(Bg, RoundedCornerShape(7.dp))) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Blue, RoundedCornerShape(7.dp)))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("現在 ${age}歳", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubInk)
                Text("寿命 ${lifespan}歳", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubInk)
            }
        }
        // 命日
        Row(Modifier.fillMaxWidth().card().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, null, tint = Blue, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(18.dp))
            Column {
                Text("命日", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubInk)
                Text("${death.year}年${death.monthValue}月${death.dayOfMonth}日", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier.card().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 40.sp, fontWeight = FontWeight.Black, color = Blue)
            Text(unit, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(bottom = 7.dp, start = 2.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteRow(
    fav: Fav,
    canUp: Boolean,
    canDown: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Text(
            fav.label,
            fontSize = 26.sp,
            color = Ink,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("名前を変更") }, onClick = { menu = false; onRename() })
            if (canUp) DropdownMenuItem(text = { Text("上へ移動") }, onClick = { menu = false; onMove(-1) })
            if (canDown) DropdownMenuItem(text = { Text("下へ移動") }, onClick = { menu = false; onMove(1) })
            DropdownMenuItem(text = { Text("お気に入りから外す", color = Danger) }, onClick = { menu = false; onRemove() })
        }
    }
}
