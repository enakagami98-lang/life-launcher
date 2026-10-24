plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
            isMinifyEnabled = false
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
