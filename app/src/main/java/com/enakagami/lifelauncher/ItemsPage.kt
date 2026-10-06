package com.enakagami.lifelauncher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 目標（kind="goals"）と習慣（kind="habits"）で共通の画面 */
@Composable
fun ItemsPage(store: Store, kind: String, title: String) {
    val list = store.listFor(kind)
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }

    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        store.moveItem(kind, from.index, to.index)
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(title) {
            RoundButton(Icons.Filled.Add, "追加", Blue, Bg) { adding = true }
        }
        if (list.isEmpty()) {
            Text(
                "右上の＋から追加できます。\nタップで編集・削除、長押ししたまま上下に動かすと並べ替えできます。",
                color = SubInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { item ->
                ReorderableItem(reorder, key = item.id) { dragging ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (dragging) 0.85f else 1f)
                            .card()
                            .longPressDraggableHandle()
                            .clickable { editing = item }
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (kind == "goals") {
                            Icon(painterResource(R.drawable.ic_flag), null, tint = Blue, modifier = Modifier.size(22.dp))
                        } else {
                            Icon(Icons.Filled.Menu, null, tint = Blue, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(item.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = "${title}を追加",
            initial = "",
            onSave = { store.addItem(kind, it); adding = false },
            onDismiss = { adding = false },
        )
    }
    editing?.let { item ->
        TextInputDialog(
            title = "${title}を編集",
            initial = item.text,
            onSave = { store.updateItem(kind, item.id, it); editing = null },
            onDismiss = { editing = null },
            onDelete = { store.deleteItem(kind, item.id); editing = null },
        )
    }
}
