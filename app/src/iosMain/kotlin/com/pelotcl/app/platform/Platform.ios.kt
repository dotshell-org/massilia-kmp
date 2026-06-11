package com.pelotcl.app.platform

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSBundle

actual class PlatformContext

actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Darwin

actual fun appVersionName(context: PlatformContext): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
