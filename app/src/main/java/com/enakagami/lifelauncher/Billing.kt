package com.enakagami.lifelauncher

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play の定期購入（Play 版だけ）。
 * - 商品ID LOOSEN_YEARLY（年額）。3日間の無料体験は Play Console の「特典（オファー）」で設定する
 * - 定期購入が有効な間だけ Loosen の機能が使える。無いときは購入画面＋アプリ一覧だけ（スマホ自体は使える）
 * - 無料体験を使ったか・期限・課金は Google Play が管理するので、スマホの時計を変えてもずるはできない
 * GitHub 版（BuildConfig.PAYWALL=false）では常に「有効」。
 */
object Billing : PurchasesUpdatedListener {
    const val PRODUCT = "loosen_yearly"

    enum class State { CHECKING, ACTIVE, NONE, UNAVAILABLE }

    var state by mutableStateOf(if (BuildConfig.PAYWALL) State.CHECKING else State.ACTIVE)
        private set
    /** 商品の情報（値段の表示に使う）。Play につながらないと null */
    var details by mutableStateOf<ProductDetails?>(null)
        private set

    private var client: BillingClient? = null
    private lateinit var prefs: SharedPreferences

    /** 最後に「有効」を確認した時刻。電波が無いときは、7日以内に確認できていれば使える */
    private var lastActiveAt = 0L
    private const val OFFLINE_GRACE_MS = 7 * 86_400_000L

    /** 機能を止めるか（購入画面を出すか） */
    val locked: Boolean
        get() = BuildConfig.PAYWALL && (!genuine || when (state) {
            State.ACTIVE -> false
            // 確認中・Play につながらないときは、最近まで有効だったなら止めない
            State.CHECKING, State.UNAVAILABLE -> System.currentTimeMillis() - lastActiveAt > OFFLINE_GRACE_MS
            State.NONE -> true
        })

    /** 本物の鍵で署名されているか。改造して署名し直したアプリでは、購入済みでも機能を止める */
    private var genuine = true

    fun init(context: Context) {
        if (!BuildConfig.PAYWALL || client != null) return
        genuine = Integrity.genuine(context)
        prefs = context.applicationContext.getSharedPreferences("billing", Context.MODE_PRIVATE)
        lastActiveAt = prefs.getLong("lastActiveAt", 0)
        client = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        connect()
    }

    private fun connect() {
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else state = State.UNAVAILABLE
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    /** 定期購入の状態と商品情報を Play に聞き直す（「購入を確認」ボタン・画面に戻ったとき） */
    fun refresh() {
        val c = client ?: return
        if (!c.isReady) { connect(); return }
        c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases)
            else if (state == State.CHECKING) state = State.UNAVAILABLE
        }
        c.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT).setProductType(BillingClient.ProductType.SUBS).build()
            )).build()
        ) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = list.productDetailsList.firstOrNull()
            }
        }
    }

    private fun handle(purchases: List<Purchase>) {
        val mine = purchases.filter { PRODUCT in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        // 買ってから3日以内に「確認（acknowledge）」しないと Google が自動で返金するので、必ず確認する
        mine.filter { !it.isAcknowledged }.forEach { p ->
            client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {}
        }
        if (mine.isNotEmpty()) {
            state = State.ACTIVE
            lastActiveAt = System.currentTimeMillis()
            prefs.edit().putLong("lastActiveAt", lastActiveAt).apply()
        } else {
            state = State.NONE
            lastActiveAt = 0
            prefs.edit().putLong("lastActiveAt", 0).apply()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) handle(purchases)
        else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh()
    }

    /** 無料体験の特典があればそれを、無ければ通常の年額プランを選ぶ */
    private fun offerToken(d: ProductDetails): String? {
        val offers = d.subscriptionOfferDetails ?: return null
        val trial = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
        return (trial ?: offers.firstOrNull())?.offerToken
    }

    /** 3日間無料ありで「年額」がいくらか（その国の通貨で）。例 "$20.00"、"¥3,000" */
    fun yearlyPrice(): String? = details?.subscriptionOfferDetails
        ?.flatMap { it.pricingPhases.pricingPhaseList }
        ?.firstOrNull { it.priceAmountMicros > 0 }?.formattedPrice

    /** 無料体験を受けられるか（もう使った人には Play が特典を出さない） */
    fun hasTrial(): Boolean = details?.subscriptionOfferDetails
        ?.any { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } } == true

    /** Google Play の購入画面を出す */
    fun buy(activity: Activity): Boolean {
        val c = client ?: return false
        val d = details ?: return false
        val token = offerToken(d) ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(token).build()
        )).build()
        return c.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Google Play の「定期購入」管理画面（解約・支払い方法の変更） */
    fun manageUrl(packageName: String) =
        "https://play.google.com/store/account/subscriptions?sku=$PRODUCT&package=$packageName"
}
