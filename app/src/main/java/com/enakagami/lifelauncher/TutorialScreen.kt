package com.enakagami.lifelauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val STEPS = 7

/** このアプリが「ホームアプリ」になっているか */
fun isDefaultHome(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = context.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
    }
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return context.packageManager.resolveActivity(i, 0)?.activityInfo?.packageName == context.packageName
}

/**
 * インストール直後の使い方（チュートリアル）。説明を読みながら、その場で実際に設定していく。
 * 1 ようこそ → 2 寿命 → 3 ホームアプリ → 4 お気に入り → 5 使いすぎ防止 → 6 トップに出す情報 → 7 使い方
 * 設定の「使い方をもう一度見る」からも開ける。
 */
@Composable
fun TutorialScreen(store: Store, apps: List<AppInfo>, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    // 4・5・6で選んだもの（「次へ」で保存する）
    val favKeys = remember { mutableStateListOf<String>().apply { addAll(store.favorites.map { it.key }) } }
    val limitPkgs = remember { mutableStateListOf<String>().apply { addAll(Guard.limits.keys) } }
    var limitMin by remember { mutableIntStateOf(60) }
    val widgetsOn = remember { mutableStateListOf<String>().apply { addAll(store.widgets) } }
    // 寿命の入力
    val b = store.birth
    var year by remember { mutableStateOf(b?.year?.toString() ?: "") }
    var month by remember { mutableStateOf(b?.monthValue?.toString() ?: "") }
    var day by remember { mutableStateOf(b?.dayOfMonth?.toString() ?: "") }
    var life by remember { mutableStateOf(store.lifespan.toString()) }
    val birth = runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    val lifeNum = life.toIntOrNull()
    val birthOk = birth != null && birth.year >= 1900 && !birth.isAfter(LocalDate.now())
    val lifeOk = birthOk && lifeNum != null && lifeNum in 1..150 && birth!!.plusYears(lifeNum.toLong()).isAfter(LocalDate.now())

    fun next() {
        when (step) {
            1 -> if (birthOk && lifeOk) store.saveProfile(birth!!, lifeNum!!)
            3 -> {
                store.favorites.filter { it.key !in favKeys }.forEach { store.removeFavorite(it.key) }
                favKeys.forEach { k -> apps.firstOrNull { it.key == k }?.let { store.addFavorite(it.key, it.label) } }
            }
            4 -> {
                Guard.limits.keys.filter { it !in limitPkgs }.forEach { Guard.removeLimit(it) }
                limitPkgs.forEach { Guard.setLimit(it, limitMin) }
            }
            5 -> store.saveWidgets(ALL_WIDGETS.keys.filter { it in widgetsOn })
        }
        if (step == STEPS - 1) onFinish() else step++
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // 上：進み具合とスキップ
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(STEPS) { i ->
                    Box(Modifier.size(width = if (i == step) 22.dp else 8.dp, height = 8.dp).clip(CircleShape)
                        .background(if (i <= step) Blue else SubInk.copy(alpha = 0.25f)))
                }
            }
            if (step < STEPS - 1) TextButton(onClick = onFinish) { Text(stringResource(R.string.tut_skip), color = SubInk) }
        }
        Spacer(Modifier.height(12.dp))
        // 中：各ステップ
        Column(Modifier.weight(1f)) {
            when (step) {
                0 -> StepWelcome()
                1 -> StepLife(year, month, day, life, { year = it }, { month = it }, { day = it }, { life = it }, birthOk, lifeOk, birth, lifeNum)
                2 -> StepHome()
                3 -> StepPickApps(R.string.tut4_title, R.string.tut4_body, apps.distinctBy { it.key }.map { it.key to it.label }, favKeys)
                4 -> StepLimits(apps, limitPkgs, limitMin) { limitMin = it }
                5 -> StepWidgets(widgetsOn)
                else -> StepDone()
            }
        }
        // 下：戻る・次へ
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back), color = SubInk, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            val enabled = step != 1 || (birthOk && lifeOk)
            Button(
                onClick = { next() },
                enabled = enabled,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
            ) {
                Text(
                    stringResource(when (step) { 0 -> R.string.tut_start; STEPS - 1 -> R.string.tut_done; else -> R.string.tut_next }),
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Title(id: Int) {
    Text(stringResource(id), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Body(id: Int) {
    Text(stringResource(id), color = SubInk, fontSize = 15.sp, lineHeight = 23.sp)
    Spacer(Modifier.height(16.dp))
}

// ---- 1 ようこそ ----
@Composable
private fun StepWelcome() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.app_name), color = Blue, fontSize = 44.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Title(R.string.tut1_title)
        Body(R.string.tut1_body)
    }
}

// ---- 2 寿命 ----
@Composable
private fun StepLife(
    year: String, month: String, day: String, life: String,
    onYear: (String) -> Unit, onMonth: (String) -> Unit, onDay: (String) -> Unit, onLife: (String) -> Unit,
    birthOk: Boolean, lifeOk: Boolean, birth: LocalDate?, lifeNum: Int?,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Title(R.string.tut2_title)
        Body(R.string.tut2_body)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Num(stringResource(R.string.year), year, 4, Modifier.weight(1.4f), onYear)
            Num(stringResource(R.string.month), month, 2, Modifier.weight(1f), onMonth)
            Num(stringResource(R.string.day), day, 2, Modifier.weight(1f), onDay)
        }
        Spacer(Modifier.height(10.dp))
        Num(stringResource(R.string.s_lifespan), life, 3, Modifier.fillMaxWidth(), onLife)
        Spacer(Modifier.height(18.dp))
        when {
            birthOk && lifeOk -> {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), birth!!.plusYears(lifeNum!!.toLong()))
                Column(Modifier.fillMaxWidth().card().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%,d".format(days), color = Blue, fontSize = 56.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.tut2_result, "%,d".format(days)), color = Ink, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            }
            year.length == 4 && month.isNotEmpty() && day.isNotEmpty() && !birthOk ->
                Text(stringResource(R.string.err_birth), color = Danger, fontSize = 13.sp)
            birthOk && life.isNotEmpty() && !lifeOk -> Text(stringResource(R.string.err_life), color = Danger, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Num(label: String, value: String, max: Int, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(max)) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}

// ---- 3 ホームアプリ ----
@Composable
private fun StepHome() {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    var tick by remember { mutableIntStateOf(0) }
    val isHome = remember(resume, tick) { isDefaultHome(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Title(R.string.tut3_title)
        Body(R.string.tut3_body)
        if (isHome) {
            Row(Modifier.fillMaxWidth().card().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, tint = Blue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.tut3_ok), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = {
                    val rm = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    else runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
            ) { Text(stringResource(R.string.tut3_button), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

// ---- 4 お気に入り（と 5 の一覧でも使う） ----
@Composable
private fun ColumnScope.StepPickApps(titleId: Int, bodyId: Int, items: List<Pair<String, String>>, picked: MutableList<String>) {
    Title(titleId)
    Body(bodyId)
    Text(stringResource(R.string.tut_selected, picked.size), color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    AppChecklist(items, picked, Modifier.weight(1f))
}

@Composable
private fun AppChecklist(items: List<Pair<String, String>>, picked: MutableList<String>, modifier: Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(items, key = { it.first }) { (key, label) ->
            val on = key in picked
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable { if (on) picked.remove(key) else picked.add(key) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (on) Blue else Color.Transparent)
                        .border(2.dp, if (on) Blue else SubInk, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (on) Icon(Icons.Filled.Check, null, tint = OnAccent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(14.dp))
                Text(label, color = Ink, fontSize = 17.sp)
            }
        }
    }
}

// ---- 5 使いすぎ防止 ----
@Composable
private fun ColumnScope.StepLimits(apps: List<AppInfo>, picked: MutableList<String>, minutes: Int, onMinutes: (Int) -> Unit) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val overlayOk = remember(resume) { Guard.hasOverlay(context) }
    val usageOk = remember(resume) { Guard.hasUsageAccess(context) }
    Title(R.string.tut5_title)
    Text(stringResource(R.string.tut5_body), color = SubInk, fontSize = 14.sp, lineHeight = 21.sp)
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.tut5_limit), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        listOf(30, 60, 90, 120).forEach { m ->
            val sel = m == minutes
            Text(
                stringResource(R.string.dur_m, m),
                color = if (sel) OnAccent else Ink, fontSize = 13.sp,
                modifier = Modifier.padding(end = 6.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) Blue else Color.Transparent)
                    .border(1.dp, if (sel) Blue else SubInk, RoundedCornerShape(16.dp))
                    .clickable { onMinutes(m) }.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.tut_selected, picked.size), color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    AppChecklist(apps.distinctBy { it.pkg }.map { it.pkg to it.label }, picked, Modifier.weight(1f))
    // 必要な許可（選んだときだけ）
    if (picked.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.tut5_perm), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        PermRow(stringResource(R.string.s_overlay), overlayOk) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        PermRow(stringResource(R.string.s_usage), usageOk) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (!usageOk) Text(stringResource(R.string.restricted_help), color = SubInk, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PermRow(label: String, ok: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (ok) Text(stringResource(R.string.allowed), color = SubInk, fontSize = 13.sp)
        else TextButton(onClick = onClick) { Text(stringResource(R.string.allow), fontWeight = FontWeight.Bold) }
    }
}

// ---- 6 トップに出す情報 ----
@Composable
private fun ColumnScope.StepWidgets(on: MutableList<String>) {
    Title(R.string.tut6_title)
    Body(R.string.tut6_body)
    LazyColumn(Modifier.weight(1f)) {
        items(ALL_WIDGETS.keys.toList()) { id ->
            val name = stringResource(ALL_WIDGETS[id]!!)
            val desc = stringResource(R.string.w_show, name)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = id in on,
                    onCheckedChange = { if (it) on.add(id) else on.remove(id) },
                    modifier = Modifier.semantics { contentDescription = desc },
                    colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = OnAccent),
                )
            }
        }
    }
}

// ---- 7 使い方 ----
@Composable
private fun StepDone() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Title(R.string.tut7_title)
        Body(R.string.tut7_body)
        listOf(R.string.tut7_page_left, R.string.tut7_page_top, R.string.tut7_page_right, R.string.tut7_longpress).forEach { id ->
            Text(stringResource(id), color = Ink, fontSize = 15.sp, lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).card().padding(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.tut7_tip), color = SubInk, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
