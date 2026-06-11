package com.pelotcl.app.generic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pelotcl.app.generic.data.models.geojson.FeatureCollection
import com.pelotcl.app.generic.utils.map.toLinesGeoJson
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * Cross-platform map canvas built on maplibre-compose (declarative).
 *
 * Increment 5: renders real, externally-provided GeoJSON:
 *  - [linesGeoJson]: a GeoJSON FeatureCollection of transport line geometries.
 *  - [userLocation]: the device location, drawn as a blue dot.
 *
 * Per-line colouring (data-driven expression), stops and itinerary layers come
 * next. Built alongside the legacy [MapLibreView]; not yet wired into PlanScreen.
 */
@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    styleUrl: String,
    initialLatitude: Double = 45.75,
    initialLongitude: Double = 4.85,
    initialZoom: Double = 10.0,
    lines: FeatureCollection? = null,
    userLocation: Position? = null,
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = initialLatitude, longitude = initialLongitude),
            zoom = initialZoom,
        )
    )

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(styleUrl),
        cameraState = cameraState,
    ) {
        if (lines != null) {
            val linesGeoJson = remember(lines) { lines.toLinesGeoJson() }
            val lineSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(linesGeoJson))
            LineLayer(
                id = "transport-lines",
                source = lineSource,
                color = feature["color"].convertToColor(),
                width = const(3.dp),
            )
        }

        if (userLocation != null) {
            val userSource = rememberGeoJsonSource(
                data = GeoJsonData.JsonString(
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${userLocation.longitude},${userLocation.latitude}]},"properties":{}}"""
                )
            )
            CircleLayer(
                id = "user-location",
                source = userSource,
                radius = const(8.dp),
                color = const(Color(0xFF3B82F6)),
            )
        }
    }
}
