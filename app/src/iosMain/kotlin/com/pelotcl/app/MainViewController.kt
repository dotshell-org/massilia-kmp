package com.pelotcl.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.pelotcl.app.generic.ui.theme.PeloTheme
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.PlatformContext
import com.pelotcl.app.platform.appVersionName
import platform.UIKit.UIViewController

/**
 * iOS no-op platform context. On Android, PlatformContext is android.content.Context;
 * on iOS the platform actuals (FileSystem, Settings, …) don't need a real context, so a
 * single shared instance is enough. PlatformContext is `abstract` (to match the Android
 * typealias to the abstract android.content.Context), hence this concrete singleton.
 */
object IosPlatformContext : PlatformContext()

/**
 * Compose entry point, exported to Swift as `ComposeAppKt.MainViewController()`.
 * The iosApp Xcode target wraps this UIViewController in SwiftUI.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CompositionLocalProvider(LocalPlatformContext provides IosPlatformContext) {
        App()
    }
}

/**
 * Shared root composable. Minimal for now (proves the framework + ComposeUIViewController +
 * common UI/theme + a platform actual all work on-device). The full shared UI (PlanScreen,
 * navigation) lands once the map swap (§9) frees PlanScreen from androidMain.
 */
@Composable
fun App() {
    PeloTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pelo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Compose Multiplatform tourne sur iOS 🎉",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "version ${appVersionName(IosPlatformContext)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
