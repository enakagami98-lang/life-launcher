package com.enakagami.lifelauncher

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Play 版で定期購入が無いときの画面。Loosen の機能は止めるが、スマホは使えるように
 * 下にアプリ一覧を出し、ほかのホームアプリへ切り替えるボタンも置く（スマホを人質にしない）。
 */
@Composable
fun PaywallScreen(store: Store, apps: List<AppInfo>, query: String, onQuery: (String) -> Unit) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    LaunchedEffect(resume) { Billing.refresh() }
    val price = Billing.yearlyPrice()

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).card().padding(20.dp)) {
            Text(stringResource(R.string.pay_title), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.pay_body), color = SubInk, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(12.dp))
            if (Billing.details == null) {
                Text(stringResource(R.string.pay_unavailable), color = Danger, fontSize = 13.sp, lineHeight = 19.sp)
            } else {
                Button(
                    onClick = { (context as? Activity)?.let { Billing.buy(it) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
                ) {
                    Text(stringResource(if (Billing.hasTrial()) R.string.pay_trial else R.string.pay_subscribe), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (price != null) {
                    Text(stringResource(R.string.pay_then, price), color = SubInk, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Text(stringResource(R.string.pay_cancel_note), color = SubInk, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 6.dp))
            TextButton(onClick = { Billing.refresh() }) { Text(stringResource(R.string.pay_restore)) }
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) { Text(stringResource(R.string.pay_switch_home), color = SubInk) }
            Text(stringResource(R.string.pay_limited), color = SubInk, fontSize = 12.sp, lineHeight = 17.sp)
        }
        // スマホは使えるように、アプリ一覧はそのまま
        AppsPage(store, apps, query, onQuery)
    }
}
