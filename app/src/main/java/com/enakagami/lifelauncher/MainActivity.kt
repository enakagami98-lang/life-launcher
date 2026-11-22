package com.enakagami.lifelauncher

import android.app.Activity
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
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

/**
 * 画面の並び：寿命＋お気に入り → アプリ一覧 → 習慣 → 目標 → 振り返り日記 → タスク → 身体の調子（寿命の1つ手前）。
 * 習慣・目標・日記・タスクは設定の「表示するページ」で隠せる（OPTIONAL_PAGES）。2026-11-16 に目標と習慣を入れ替えた
 */
fun pageIds(store: Store) = listOf("life", "apps") + OPTIONAL_PAGES.keys.filter { it !in store.hiddenPages } + "condition"

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
        Early.firstSeen(this)
        Spend.init(this)
        Billing.init(this)
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
        Guard.launcherResumed = true
        // 集中タイムの画面に戻ってきた（簡易モードで、集中の画面から開いたアプリを閉じた）
        Guard.focusOpened = null
        // 簡易モードでは、ホームに戻ってきたら今回の利用は終わり（次に開くときはまた待つ）
        if (!Guard.hasUsageAccess(this)) Guard.endCurrentSession()
        resumeSignal.intValue++
        Billing.refresh()
        // 見守りが止まっていた日の使用時間を、スマホ本体の記録で補う
        Guard.syncFromSystem()
        // 支払いの通知の読み取りが切れていたら、つなぎ直す
        PaymentListener.rebind(this)
        // 見守りサービスを動かす（すでに動いていれば何もしない）
        runCatching { startForegroundService(Intent(this, GuardService::class.java)) }
    }

    override fun onPause() {
        super.onPause()
        Guard.launcherResumed = false
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

    // ページの数は設定で変わる。変わったら最初（寿命の画面）から作り直す
    val pages = pageIds(store)
    val count = pages.size
    val startPage = count * 1000
    val pager = key(count) { rememberPagerState(initialPage = startPage) { count * 2000 } }
    var query by remember { mutableStateOf("") }
    // 設定画面は、許可を出しに別の画面へ行って戻っても閉じないよう、ここで管理する
    var showSettings by remember { mutableStateOf(false) }
    // 使い方：入れたばかり（まだ誕生日も無い）のときは自動で。設定の「使い方をもう一度見る」からも
    var showTutorial by remember { mutableStateOf(!store.tutorialDone && store.birth == null) }
    // 振り返り日記の1日分・今日の言葉の全画面。ホームボタンで閉じる（書いた分は保存済み）
    var panel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(homePress) { showSettings = false; panel = null }

    suspend fun goHome() {
        query = ""
        focus.clearFocus()
        pager.scrollToPage(startPage)
    }

    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    // アプリ一覧から離れたら検索欄を空にしてキーボードを閉じる
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage % count != 1) {
            query = ""
            focus.clearFocus()
        }
    }
    // 戻るボタンではアプリを閉じず、寿命の画面に戻る
    BackHandler { scope.launch { goHome() } }

    val openApp: (String) -> Unit = { key ->
        val pkg = key.substringBefore('/')
        if (!Billing.locked && Guard.isLimited(pkg) && !Guard.sessionActive(pkg)) setGuardRequest(GuardRequest(pkg, key, "open"))
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
        LocalOpenPanel provides { panel = it },
    ) {
        MaterialTheme(colorScheme = scheme) {
          // 改行：日本語は文節の切れ目で、どの言語も行の長さをそろえて折り返す（「す。」だけ次の行、を防ぐ）。単語の途中では切らない
          ProvideTextStyle(
              LocalTextStyle.current.merge(
                  TextStyle(
                      lineBreak = LineBreak(LineBreak.Strategy.Balanced, LineBreak.Strictness.Strict, LineBreak.WordBreak.Phrase),
                  ),
              ),
          ) {
            Surface(Modifier.fillMaxSize(), color = palette.bg) {
                if (lock != null && !Billing.locked) {
                    LockScreen(stringResource(lock.first), lock.second, now.toLocalTime(), night = lock.first == R.string.lock_night)
                } else if (showTutorial) {
                    TutorialScreen(store, apps, onFinish = { store.finishTutorial(); showTutorial = false })
                } else if (Billing.locked) {
                    // Play 版で定期購入が無いとき：機能は止めて、購入画面＋アプリ一覧
                    PaywallScreen(store, apps, query, onQuery = { query = it })
                } else if (Guard.focusEndSignal >= 0 && Guard.memoDue(now) != null) {
                    // おやすみ時間・集中タイムが終わった：その間に書いたメモ（「閉じる」でしか消えない）
                    MemoScreen(Guard.memoDue(now)!!)
                } else if (Guard.focusEndSignal >= 0 && Guard.focusCelebrate()) {
                    // （focusEndSignal を読むのは、集中の時間になった瞬間にここを描き直すため）
                    // 集中タイムが最後まで終わった：おつかれさまでした
                    FocusDoneScreen()
                } else if (Guard.focusActive()) {
                    // 集中タイム中：電話と選んだアプリしか使えない
                    FocusScreen(apps)
                    guardRequest?.let { req -> GuardDialogFor(req, apps, context, setGuardRequest) }
                } else if (Guard.needGoal(now)) {
                    // 夜のおやすみ時間のあと最初：今日の目標を決めるまでホーム画面の代わりに出す
                    GoalScreen()
                } else if (Guard.askPending) {
                    // ロック解除時の「今何してる？」。答えるまでホーム画面の代わりに出す
                    AskScreen(now.toLocalTime())
                } else {
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                ) { page ->
                    // ホームに戻るたびに作り直し、開きっぱなしの入力画面やメニューを必ず閉じる
                    key(homeSignal) {
                        when (pages[page % count]) {
                            "life" -> LifePage(store, today, apps, openSettings = { showSettings = true },
                                openCondition = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } })
                            "apps" -> AppsPage(store, apps, query, onQuery = { query = it })
                            "habits" -> ItemsPage(store, kind = "habits")
                            "goals" -> ItemsPage(store, kind = "goals")
                            "journal" -> JournalPage(store, today)
                            "tasks" -> TasksPage(store, today)
                            else -> ConditionPage(store, today)
                        }
                    }
                }
                panel?.let { p ->
                    key(p) {
                        if (p == "quote") QuoteScreen(store, today) { panel = null }
                        else JournalPanel(store, p) { panel = null }
                    }
                }
                // その日に初めて目標・タスクを登録したときの日めくり
                if (Guard.tearPending) TearOverlay(store, LocalDate.now()) { Guard.tearPending = false }
                if (showSettings) {
                    SettingsDialog(store, apps, onDismiss = { showSettings = false },
                        onTutorial = { showSettings = false; showTutorial = true })
                }
                guardRequest?.let { req -> GuardDialogFor(req, apps, context, setGuardRequest) }
                }
            }
          }
        }
    }
}

/** 使いすぎ防止の待ち画面（ホーム画面と、集中タイムの画面の両方から出す） */
@Composable
private fun GuardDialogFor(req: GuardRequest, apps: List<AppInfo>, context: android.content.Context, setGuardRequest: (GuardRequest?) -> Unit) {
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
