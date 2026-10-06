package com.enakagami.lifelauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background

@Composable
fun AppsPage(store: Store, apps: List<AppInfo>, query: String, onQuery: (String) -> Unit) {
    val context = LocalContext.current
    // 非表示にしたアプリは一覧にも検索にも出さない
    val visible = apps.filter { it.pkg !in store.hidden }
    val shown = if (query.isBlank()) visible
    else visible.filter { it.label.contains(query.trim(), ignoreCase = true) }

    Column(Modifier.fillMaxSize().imePadding()) {
        PageHeader("アプリ")
        // 検索欄（アプリ名を入力してから開く「ひと手間」の入口）
        Row(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Box {
                    if (query.isEmpty()) Text("アプリ名で検索", fontSize = 20.sp, color = SubInk.copy(alpha = 0.6f))
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 20.sp, color = Ink),
                        cursorBrush = SolidColor(Blue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let { launchApp(context, it.key) } }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Filled.Search, "検索", tint = Ink)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.key }) { app ->
                AppRow(
                    app = app,
                    isFavorite = store.isFavorite(app.key),
                    onOpen = { launchApp(context, app.key) },
                    onToggleFavorite = {
                        if (store.isFavorite(app.key)) store.removeFavorite(app.key)
                        else store.addFavorite(app.key, app.label)
                    },
                    onHide = { store.hide(app.pkg) },
                )
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppInfo,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Text(
            app.label,
            fontSize = 24.sp,
            color = Ink,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
                .padding(horizontal = 24.dp, vertical = 14.dp),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) "お気に入りから外す" else "お気に入りに追加") },
                onClick = { menu = false; onToggleFavorite() },
            )
            DropdownMenuItem(
                text = { Text("非表示にする", color = Danger) },
                onClick = { menu = false; onHide() },
            )
        }
    }
}
