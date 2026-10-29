package com.isle.companion.data.remote

/**
 * 统一的模型服务异常：message 直接面向用户展示。
 */
class OpenAiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
