package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 色は設定で自由に変えられる。背景の明るさから、カードや文字の色を自動で決める
data class Palette(val bg: Color, val accent: Color) {
    val dark = bg.luminance() < 0.4f
    val card = if (dark) lerp(bg, Color.White, 0.10f) else lerp(bg, Color.Black, 0.05f)
    val ink = if (dark) Color(0xFFF2F3F7) else Color(0xFF1A1A1F)
    val subInk = ink.copy(alpha = 0.62f)
    val onAccent = if (accent.luminance() > 0.55f) Color(0xFF1A1A1F) else Color.White
}

val DefaultBg = Color(0xFFFFFFFF)
val DefaultAccent = Color(0xFF1043E5)
val LocalPalette = staticCompositionLocalOf { Palette(DefaultBg, DefaultAccent) }

val Blue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onAccent
val Bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
val CardBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.card
val Ink: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val SubInk: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.subInk
val Danger = Color(0xFFE5484D)

val CardShape = RoundedCornerShape(22.dp)

@Composable
fun Modifier.card(): Modifier = this.background(CardBg, CardShape)

/** 画面上部の見出し（左にタイトル、右にボタン） */
@Composable
fun PageHeader(title: String, action: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
        action()
    }
}

@Composable
fun RoundButton(
    icon: ImageVector,
    description: String,
    container: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = container, contentColor = tint),
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(28.dp))
    }
}

/** 文字を1行入力するダイアログ。onDelete を渡すと「削除」ボタンも出る */
@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSave(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("削除", color = Danger) }
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
        containerColor = Bg,
    )
}
