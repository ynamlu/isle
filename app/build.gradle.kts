import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 从 local.properties 读取模型服务配置（该文件不入版本库）。
// 支持任意 OpenAI 兼容协议的服务：API 地址 + 模型 ID + API Key。
// 构建时可提供模型地址和 ID，Key 只从应用内设置读取。
val localProps: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(vararg names: String): String = names.firstNotNullOfOrNull { name ->
    localProps.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
} ?: ""

val aiBaseUrl = localProp("AI_BASE_URL", "OPENAI_BASE_URL", "OPENAI_API_BASE")
val aiModel = localProp("AI_MODEL", "OPENAI_MODEL", "AI_MODEL_ID")
val shareQrUrl = localProp("SHARE_QR_URL").ifEmpty { "https://isle.ynatech.xyz/" }

/** 转义为 BuildConfig（Java 源码）中的字符串字面量，避免地址/Key 中的特殊字符破坏生成。 */
fun buildConfigStringLiteral(value: String): String = buildString {
    append('"')
    value.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
    append('"')
}

android {
    namespace = "com.isle.companion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.isle.companion"
        minSdk = 26
        targetSdk = 37
        versionCode = 19
        versionName = "2.3.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 构建只提供地址与模型默认值，私人密钥在应用内配置。
        buildConfigField("String", "DEMO_AI_BASE_URL", buildConfigStringLiteral(aiBaseUrl))
        buildConfigField("String", "DEMO_AI_MODEL", buildConfigStringLiteral(aiModel))
        buildConfigField("String", "DEMO_AI_API_KEY", buildConfigStringLiteral(""))
        buildConfigField("String", "SHARE_QR_URL", buildConfigStringLiteral(shareQrUrl))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    // ONNX Runtime 原生库体积较大：按 CPU 架构拆分，避免把全部架构打进同一个包。
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// 真实接口端到端测试默认跳过；用 -Disle.e2e=1 显式开启（避免 CI/离线环境误联网）。
tasks.withType<Test>().configureEach {
    if (providers.systemProperty("isle.e2e").orElse("").get() == "1") {
        systemProperty("isle.e2e", "1")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // 网络
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // 情绪手账二维码
    implementation(libs.zxing.core)

    // 液态玻璃（真实背景模糊）
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.blur.materials)

    // 端侧语义向量（记忆检索）
    implementation(libs.onnxruntime.android)

    // 图片
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    debugImplementation(libs.androidx.ui.tooling)

    // JVM 单元测试（地址拼接 / SSE 解析 / 配置回退等纯逻辑）
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
