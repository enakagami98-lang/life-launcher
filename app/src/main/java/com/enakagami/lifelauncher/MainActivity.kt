package com.enakagami.lifelauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/** 4枚の画面：0=寿命＋お気に入り / 1=アプリ一覧 / 2=目標 / 3=習慣 */
const val PAGES = 4
private const val START_PAGE = PAGES * 1000

/** アプリを開く処理（リマインダー対象なら、待ち画面をはさむ） */
val LocalOpenApp = compositionLocalOf<(String) -> Unit> { {} }
/** 画面に戻ってくるたびに増える数（許可の状態を読み直すため） */
val LocalResumeSignal = compositionLocalOf { 0 }

class MainActivity : ComponentActivity() {
    /** 増えるたびに「寿命の画面に戻る」合図 */
    private val homeSignal = mutableIntStateOf(0)
    /** 増えるたびに「アプリ一覧と今日の日付を読み直す」合図 */
    private val resumeSignal = mutableIntStateOf(0)
    /** 見守りサービスから「待ち画面を出して」と頼まれた内容 */
    private val guardRequest = mutableStateOf<GuardRequest?>(null)
    /** ホームボタンを押した回数（設定画面を閉じる合図） */
    private val homePress = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Guard.init(this)
        val store = Store(this)
        handleGuardIntent(intent)
        setContent {
            LauncherApp(
                store = store,
                homeSignal = homeSignal.intValue,
                resumeSignal = resumeSignal.intValue,
                homePress = homePress.intValue,
                guardRequest = guardRequest.value,
                setGuardRequest = { guardRequest.value = it },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // ホームボタンを押したとき
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeSignal.intValue++
            homePress.intValue++
            guardRequest.value = null
        }
        handleGuardIntent(intent)
    }

    private fun handleGuardIntent(intent: Intent?) {
        val pkg = intent?.getStringExtra("guard_pkg") ?: return
        val reason = intent.getStringExtra("guard_reason") ?: return
        if (reason == "session") guardRequest.value = GuardRequest(pkg, null, reason)
        intent.removeExtra("guard_pkg")
    }

    override fun onResume() {
        super.onResume()
        resumeSignal.intValue++
        // 見守りサービスを動かす（すでに動いていれば何もしない）
        runCatching { startForegroundService(Intent(this, GuardService::class.java)) }
    }

    // 他のアプリを開いた・画面を消したときは、次に戻ってきたら必ず寿命の画面から始める
    override fun onStop() {
        super.onStop()
        homeSignal.intValue++
        guardRequest.value = null
    }
}

@Composable
fun LauncherApp(
    store: Store,
    homeSignal: Int,
    resumeSignal: Int,
    homePress: Int,
    guardRequest: GuardRequest?,
    setGuardRequest: (GuardRequest?) -> Unit,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    val today = remember(resumeSignal) { LocalDate.now() }
    LaunchedEffect(resumeSignal) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }
    // 使えない時間帯の判定のため、時刻を15秒ごとに更新
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(resumeSignal) {
        while (true) { now = LocalDateTime.now(); delay(15_000) }
    }
    val lock = Guard.activeLock(now)

    val pager = rememberPagerState(initialPage = START_PAGE) { PAGES * 2000 }
    val lifeList = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    // 設定画面は、許可を出しに別の画面へ行って戻っても閉じないよう、ここで管理する
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(homePress) { showSettings = false }

    suspend fun goHome() {
        query = ""
        focus.clearFocus()
        pager.scrollToPage(START_PAGE)
        lifeList.scrollToItem(0)
    }

    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    // アプリ一覧から離れたら検索欄を空にしてキーボードを閉じる
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage % PAGES != 1) {
            query = ""
            focus.clearFocus()
        }
    }
    // 戻るボタンではアプリを閉じず、寿命の画面に戻る
    BackHandler { scope.launch { goHome() } }

    val openApp: (String) -> Unit = { key ->
        val pkg = key.substringBefore('/')
        if (Guard.isLimited(pkg) && !Guard.sessionActive(pkg)) setGuardRequest(GuardRequest(pkg, key, "open"))
        else launchApp(context, key)
    }

    val palette = Palette(Color(store.bgColor), Color(store.accentColor))
    // ステータスバーの文字色を背景に合わせる
    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView).apply {
                isAppearanceLightStatusBars = !palette.dark
                isAppearanceLightNavigationBars = !palette.dark
            }
        }
    }
    val scheme = if (palette.dark) darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, background = palette.bg, surface = palette.bg,
        onSurface = palette.ink, onBackground = palette.ink, onSurfaceVariant = palette.subInk,
        surfaceContainer = palette.card, surfaceContainerHigh = palette.card, outline = palette.subInk,
    ) else lightColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, background = palette.bg, surface = palette.bg,
        onSurface = palette.ink, onBackground = palette.ink, onSurfaceVariant = palette.subInk,
        surfaceContainer = palette.card, surfaceContainerHigh = palette.card, outline = palette.subInk,
    )

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalOpenApp provides openApp,
        LocalResumeSignal provides resumeSignal,
    ) {
        MaterialTheme(colorScheme = scheme) {
            Surface(Modifier.fillMaxSize(), color = palette.bg) {
                if (lock != null) {
                    LockScreen(lock.first, lock.second, now.toLocalTime())
                } else {
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                ) { page ->
                    // ホームに戻るたびに作り直し、開きっぱなしの入力画面やメニューを必ず閉じる
                    key(homeSignal) {
                        when (page % PAGES) {
                            0 -> LifePage(store, today, apps, lifeList, openSettings = { showSettings = true })
                            1 -> AppsPage(store, apps, query, onQuery = { query = it })
                            2 -> ItemsPage(store, kind = "goals", title = "目標")
                            else -> ItemsPage(store, kind = "habits", title = "習慣")
                        }
                    }
                }
                if (showSettings) {
                    SettingsDialog(store, apps, onDismiss = { showSettings = false })
                }
                guardRequest?.let { req ->
                    val label = apps.firstOrNull { it.pkg == req.pkg }?.label ?: req.pkg
                    GuardDialog(
                        req = req,
                        label = label,
                        onLaunch = { minutes ->
                            Guard.startSession(req.pkg, minutes)
                            setGuardRequest(null)
                            val key = req.key ?: apps.firstOrNull { it.pkg == req.pkg }?.key
                            if (key != null) launchApp(context, key)
                        },
                        onClose = { setGuardRequest(null) },
                    )
                }
                }
            }
        }
    }
}
