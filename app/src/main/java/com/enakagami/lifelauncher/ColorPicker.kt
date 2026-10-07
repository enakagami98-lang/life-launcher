package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ワンタップで選べる見本の色 */
val PresetColors = listOf(
    "白" to 0xFFFFFFFF, "黒" to 0xFF000000, "紺" to 0xFF0F1B2D, "深緑" to 0xFF27444F,
    "ベージュ" to 0xFFF5EFE6, "グレー" to 0xFFE9EBF0, "青" to 0xFF1043E5, "赤" to 0xFFE53935,
    "緑" to 0xFF1E9E5A, "オレンジ" to 0xFFF28C28, "紫" to 0xFF7B4DFF, "ピンク" to 0xFFE91E8C,
).map { (n, c) -> n to Color(c) }

fun Color.hex() = "#%06X".format(0xFFFFFF and toArgb())

fun parseHex(s: String): Color? {
    val t = s.removePrefix("#")
    if (t.length != 6) return null
    return t.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

/** 色を選ぶダイアログ：見本の色＋四角（鮮やかさ・明るさ）＋虹色バー（色合い）＋カラーコード */
@Composable
fun ColorPickerDialog(title: String, initial: Color, onPick: (Color) -> Unit, onDismiss: () -> Unit) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(start[0]) }
    var s by remember { mutableFloatStateOf(start[1]) }
    var v by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf(initial.hex()) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, 1f)))

    fun setColor(c: Color, updateHex: Boolean = true) {
        val a = FloatArray(3); android.graphics.Color.colorToHSV(c.toArgb(), a)
        // 白・黒・灰色は色合いが決まらないので、今の色合いを保つ
        if (a[1] > 0f) h = a[0]
        s = a[1]; v = a[2]
        if (updateHex) hex = c.hex()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 見本
                PresetColors.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { (name, c) ->
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(c)
                                    .border(if (c.hex() == color.hex()) 3.dp else 1.dp, if (c.hex() == color.hex()) Blue else SubInk, CircleShape)
                                    .semantics { contentDescription = name }
                                    .clickable { setColor(c) }
                            )
                        }
                    }
                }
                // 鮮やかさ（横）と明るさ（縦）の四角
                var box by remember { mutableStateOf(IntSize(1, 1)) }
                val density = LocalDensity.current
                Box(
                    Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        .onSizeChanged { box = it }
                        .semantics { contentDescription = "色の四角" }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                fun update(p: Offset) {
                                    s = (p.x / box.width).coerceIn(0f, 1f)
                                    v = 1f - (p.y / box.height).coerceIn(0f, 1f)
                                    hex = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))).hex()
                                }
                                val down = awaitFirstDown()
                                update(down.position)
                                drag(down.id) { update(it.position); it.consume() }
                            }
                        }
                ) {
                    val r = with(density) { 10.dp.roundToPx() }
                    Box(
                        Modifier.offset { IntOffset((s * box.width).toInt() - r, ((1 - v) * box.height).toInt() - r) }
                            .size(20.dp).border(3.dp, Color.White, CircleShape)
                    )
                }
                // 色合いの虹色バー
                var bar by remember { mutableStateOf(IntSize(1, 1)) }
                Box(
                    Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp))
                        .background(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        .onSizeChanged { bar = it }
                        .semantics { contentDescription = "虹色のバー" }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                fun update(p: Offset) {
                                    h = (p.x / bar.width).coerceIn(0f, 1f) * 359.9f
                                    if (s == 0f) s = 1f
                                    if (v == 0f) v = 1f
                                    hex = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))).hex()
                                }
                                val down = awaitFirstDown()
                                update(down.position)
                                drag(down.id) { update(it.position); it.consume() }
                            }
                        }
                ) {
                    val r = with(density) { 11.dp.roundToPx() }
                    Box(
                        Modifier.offset { IntOffset((h / 360f * bar.width).toInt() - r, 0) }
                            .size(22.dp).border(3.dp, Color.White, CircleShape)
                    )
                }
                // 選んだ色とカラーコード
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(color).border(1.dp, SubInk, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { t ->
                            val clean = "#" + t.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            hex = clean
                            parseHex(clean)?.let { setColor(it, updateHex = false) }
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(color) }) { Text("決定", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

/** 設定画面に並べる色見本（丸） */
@Composable
fun Swatch(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(30.dp).clip(CircleShape).background(color).border(1.dp, SubInk, CircleShape))
}
