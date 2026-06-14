package com.pelotcl.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.pelotcl.app.generic.data.models.geojson.FeatureCollection
import com.pelotcl.app.generic.data.models.geojson.StopCollection
import com.pelotcl.app.generic.data.repository.offline.mapstyle.MapStyleCompat
import com.pelotcl.app.generic.service.TransportServiceProvider
import com.pelotcl.app.generic.ui.components.MapCanvas
import com.pelotcl.app.generic.ui.components.search.TransportSearchBar
import com.pelotcl.app.generic.ui.screens.Destination
import com.pelotcl.app.generic.ui.theme.AccentColor
import com.pelotcl.app.generic.ui.theme.PeloTheme
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor
import com.pelotcl.app.generic.ui.viewmodel.TransportLinesUiState
import com.pelotcl.app.generic.ui.viewmodel.TransportStopsUiState
import com.pelotcl.app.generic.ui.viewmodel.TransportViewModel
import com.pelotcl.app.generic.utils.location.LocationProvider
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.Log
import com.pelotcl.app.platform.PlatformContext
import org.maplibre.spatialk.geojson.Position
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
 * Shared root, assembled incrementally on iOS from the already-common Plan building blocks
 * (MapCanvas, TransportSearchBar, bottom nav, …), verified on the simulator, until it reaches
 * parity with the androidMain PlanScreen orchestrator (§9). `TransportServiceProvider.initialize`
 * replaces the Android `PeloApplication.onCreate` bootstrap (no Application on iOS).
 */
@Composable
fun App() {
    PeloTheme {
        val viewModel = remember {
            try {
                TransportServiceProvider.initialize(IosPlatformContext)
                TransportViewModel(IosPlatformContext)
            } catch (t: Throwable) {
                Log.e("iosApp", "Transport data init failed: ${t.message}")
                null
            }
        }

        // Initialize the Raptor library up front: it backs stop/line search. Doing the heavy
        // .bin load here (rather than lazily on the first search) avoids a mid-search hang/crash
        // and is what makes search return results. Mirrors MainActivity's startup preload.
        LaunchedEffect(viewModel) {
            val vm = viewModel ?: return@LaunchedEffect
            runCatching { vm.raptorRepository.initialize() }
                .onFailure { Log.e("iosApp", "Raptor init failed: ${it.message}") }
        }

        if (viewModel != null) {
            PlanScaffold(viewModel)
        } else {
            MapCanvas(
                modifier = Modifier.fillMaxSize(),
                styleUrl = MapStyleCompat.POSITRON.styleUrl,
            )
        }
    }
}

@Composable
private fun PlanScaffold(viewModel: TransportViewModel) {
    var selectedTab by remember { mutableStateOf(Destination.PLAN) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = PrimaryColor) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedTab == destination,
                        onClick = { selectedTab = destination },
                        icon = {
                            Icon(destination.icon, contentDescription = destination.contentDescription)
                        },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = AccentColor,
                            selectedIconColor = SecondaryColor,
                            unselectedIconColor = SecondaryColor,
                            selectedTextColor = SecondaryColor,
                            unselectedTextColor = SecondaryColor,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        PlanContent(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        )
    }
}

@Composable
private fun PlanContent(viewModel: TransportViewModel, modifier: Modifier = Modifier) {
    val linesState by viewModel.uiState.collectAsState()
    val stopsState by viewModel.stopsUiState.collectAsState()
    val lineRules = remember { TransportServiceProvider.getTransportLineRules() }

    val allLines = when (val s = linesState) {
        is TransportLinesUiState.Success -> s.lines
        is TransportLinesUiState.PartialSuccess -> s.lines
        else -> null
    }
    // Match Android: only the strong lines (metro/tram/funicular) on the map. Drawing every bus
    // trace is heavy and laggy. The bus traces still load in the VM; they're just not rendered.
    val strongLines = allLines?.filter { lineRules.isStrongLine(it.properties.lineName) }
    val stops = (stopsState as? TransportStopsUiState.Success)?.stops

    var userLocation by remember { mutableStateOf<Position?>(null) }
    val locationProvider = remember { LocationProvider(IosPlatformContext) }
    DisposableEffect(Unit) {
        locationProvider.startUpdates { point ->
            userLocation = Position(latitude = point.latitude, longitude = point.longitude)
        }
        onDispose { locationProvider.stopUpdates() }
    }

    Box(modifier) {
        MapCanvas(
            modifier = Modifier.fillMaxSize(),
            styleUrl = MapStyleCompat.POSITRON.styleUrl,
            initialLatitude = 45.75,
            initialLongitude = 4.85,
            initialZoom = 12.0,
            lines = strongLines?.let { FeatureCollection(features = it) },
            stops = stops?.let { StopCollection(features = it) },
            userLocation = userLocation,
            onLineClick = { lineName -> viewModel.selectLine(lineName) },
        )

        // The search bar manages its own margins (collapsed) and full width (expanded), like the
        // Android NavBar — so no extra horizontal padding here, only the status-bar inset.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TransportSearchBar(
                onSearchStops = { q -> viewModel.searchStops(q) },
                onSearchLines = { q -> viewModel.searchLines(q) },
                onStopPrimary = { },
                onLineSelected = { line -> viewModel.selectLine(line.lineName) },
            )
        }
    }
}
