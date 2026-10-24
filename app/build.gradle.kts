plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 署名鍵の指紋（SHA-256）。改造して別の鍵で署名し直したアプリを見分けるのに使う
fun certSha256(): String {
    val ks = System.getenv("KEYSTORE_PATH") ?: return ""
    val store = java.security.KeyStore.getInstance("PKCS12")
    file(ks).inputStream().use { store.load(it, System.getenv("KEYSTORE_PASSWORD").toCharArray()) }
    val cert = store.getCertificate(System.getenv("KEY_ALIAS")) ?: return ""
    return java.security.MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        .joinToString("") { "%02x".format(it) }
}

android {
    namespace = "com.enakagami.lifelauncher"
    // Google Play は 2026/8/31 から新しいアプリに API 36 を求める
    compileSdk = 36

    defaultConfig {
        applicationId = "com.enakagami.lifelauncher"
        minSdk = 26
        targetSdk = 36
        // GitHub Actions の実行回数をバージョン番号にする（上書きインストールに必要）
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.$versionCode"
        // 本物の署名鍵の指紋。Google Play の鍵（Play Console の「アプリの署名」に出る SHA-256）が
        // GitHub の変数 PLAY_CERT_SHA256 に入っているときだけ確認する（入っていないと全員止まるため）
        val play = (System.getenv("PLAY_CERT_SHA256") ?: "").replace(":", "").trim().lowercase()
        val certs = if (play.isEmpty()) "" else listOf(certSha256(), play).filter { it.isNotEmpty() }.joinToString(",")
        buildConfigField("String", "CERTS", "\"$certs\"")
    }

    signingConfigs {
        create("release") {
            val ks = System.getenv("KEYSTORE_PATH")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    // 配る場所ごとに2種類作る
    //  play  : Google Play 用。定期購入（3日間無料→年額）が無いと機能が止まる。アプリIDは新しい名前
    //  github: いま GitHub のリリースで配っているもの（開発・テスト用）。課金なし。アプリIDは今のまま
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "PAYWALL", "false")
        }
        create("play") {
            dimension = "store"
            applicationId = "com.enakagami.loosen"
            buildConfigField("boolean", "PAYWALL", "true")
        }
    }

    buildTypes {
        release {
            // 中身を読みにくくし、使っていない部分を消す（盗用・改造対策とサイズ削減）
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // 訳の無い文字は英語で出るので、訳の抜けではビルドを止めない
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    // Google Play の定期購入
    implementation("com.android.billingclient:billing-ktx:8.0.0")
    // 目標・習慣の並べ替え（長押しドラッグ）
    implementation("sh.calvin.reorderable:reorderable:2.4.0")
}
