package com.enakagami.lifelauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 4枚の画面：0=寿命＋お気に入り / 1=アプリ一覧 / 2=目標 / 3=習慣 */
const val PAGES = 4
private const val START_PAGE = PAGES * 1000

class MainActivity : ComponentActivity() {
    /** 増えるたびに「寿命の画面に戻る」合図 */
    private val homeSignal = mutableIntStateOf(0)
    /** 増えるたびに「アプリ一覧と今日の日付を読み直す」合図 */
    private val resumeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val store = Store(this)
        setContent { LauncherApp(store, homeSignal.intValue, resumeSignal.intValue) }
    }

    // ホームボタンを押したとき
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal.intValue++
    }

    override fun onResume() {
        super.onResume()
        resumeSignal.intValue++
    }

    // 他のアプリを開いた・画面を消したときは、次に戻ってきたら必ず寿命の画面から始める
    override fun onStop() {
        super.onStop()
        homeSignal.intValue++
    }
}

@Composable
fun LauncherApp(store: Store, homeSignal: Int, resumeSignal: Int) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    val today = remember(resumeSignal) { LocalDate.now() }
    LaunchedEffect(resumeSignal) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    val pager = rememberPagerState(initialPage = START_PAGE) { PAGES * 2000 }
    val lifeList = rememberLazyListState()
    var query by remember { mutableStateOf("") }

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

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            background = Bg,
            surface = Bg,
            onSurface = Ink,
            onBackground = Ink,
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
            ) { page ->
                when (page % PAGES) {
                    0 -> LifePage(store, today, apps, lifeList)
                    1 -> AppsPage(store, apps, query, onQuery = { query = it })
                    2 -> ItemsPage(store, kind = "goals", title = "目標")
                    else -> ItemsPage(store, kind = "habits", title = "習慣")
                }
            }
        }
    }
}
