package com.isle.companion.util

import android.os.Build

/**
 * 运行环境判断。
 */
object DeviceUtils {

    /**
     * 是否运行在 Android 模拟器（QEMU）上。
     *
     * 部分 ARM64 模拟器缺少 ONNX Runtime 所需的 CPU 指令，端侧向量推理会在
     * native 层直接触发 SIGILL（无法被 try/catch 捕获），因此调用方需提前降级。
     */
    val isEmulator: Boolean by lazy {
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk") ||
            Build.MODEL.contains("Emulator")
    }
}
