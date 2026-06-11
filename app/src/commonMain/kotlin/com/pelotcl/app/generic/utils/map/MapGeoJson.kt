package com.pelotcl.app.generic.utils.map

import com.pelotcl.app.generic.data.models.geojson.FeatureCollection
import com.pelotcl.app.generic.data.models.geojson.StopCollection
import com.pelotcl.app.generic.utils.LineColorHelper
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Converts a transport [FeatureCollection] (line geometries with MultiLineString
 * coordinates and a non-standard `multiLineStringGeometry` field) into a standard
 * GeoJSON FeatureCollection string suitable for a maplibre-compose GeoJSON source.
 *
 * Each output feature carries `lineName` and a resolved `color` property so a
 * LineLayer can colour lines with a data-driven expression. Replaces the former
 * Gson-based GeoJSON construction.
 */
fun FeatureCollection.toLinesGeoJson(): String = buildJsonObject {
    put("type", "FeatureCollection")
    putJsonArray("features") {
        for (feature in features) {
            addJsonObject {
                put("type", "Feature")
                put("id", feature.id)
                putJsonObject("geometry") {
                    put("type", "MultiLineString")
                    putJsonArray("coordinates") {
                        for (line in feature.multiLineStringGeometry.coordinates) {
                            addJsonArray {
                                for (point in line) {
                                    addJsonArray {
                                        for (coordinate in point) add(coordinate)
                                    }
                                }
                            }
                        }
                    }
                }
                putJsonObject("properties") {
                    put("lineName", feature.properties.lineName)
                    put("color", LineColorHelper.getColorForLine(feature))
                }
            }
        }
    }
}.toString()

/**
 * Converts a [StopCollection] (transport stops, Point geometry) into a standard
 * GeoJSON FeatureCollection string for a maplibre-compose GeoJSON source.
 * Each feature carries `nom` and `desserte` properties.
 */
fun StopCollection.toStopsGeoJson(): String = buildJsonObject {
    put("type", "FeatureCollection")
    putJsonArray("features") {
        for (stop in features) {
            addJsonObject {
                put("type", "Feature")
                put("id", stop.id)
                putJsonObject("geometry") {
                    put("type", "Point")
                    putJsonArray("coordinates") {
                        for (coordinate in stop.geometry.coordinates) add(coordinate)
                    }
                }
                putJsonObject("properties") {
                    put("nom", stop.properties.nom)
                    put("desserte", stop.properties.desserte)
                }
            }
        }
    }
}.toString()
