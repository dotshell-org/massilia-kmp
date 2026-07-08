package eu.dotshell.massilia.generic.utils.map

import eu.dotshell.massilia.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Glides live vehicle markers along their line's trace between two feed ticks
 * (the RTM feed only refreshes once per minute, so raw updates teleport).
 *
 * A straight-line interpolation cuts corners — vehicles crossing buildings —
 * and orthogonally projecting each *intermediate* point can snap to the wrong
 * branch on hairpin sections. So the two endpoints are projected on the trace
 * once, and the marker follows the trace between the two curvilinear
 * abscissas: the robust variant of the same idea. Vehicles too far from any
 * trace (deviation, unmapped detour) fall back to the straight segment.
 *
 * @param traces line name (uppercase) -> list of paths, each path being a
 *   list of `[lon, lat]` coordinates (GeoJSON order).
 */
class VehiclePathInterpolator(traces: Map<String, List<List<List<Double>>>>) {

    private val polylinesByLine: Map<String, List<Polyline>> =
        traces.entries.associate { (name, paths) ->
            name.trim().uppercase() to paths.filter { it.size >= 2 }.map { Polyline(it) }
        }

    /** Precomputed interpolation for one vehicle between two ticks. */
    sealed interface Plan {
        /** Position at [fraction] in 0..1 as (latitude, longitude). */
        fun at(fraction: Double): Pair<Double, Double>
    }

    private class Static(private val lat: Double, private val lon: Double) : Plan {
        override fun at(fraction: Double) = lat to lon
    }

    private class Linear(
        private val fromLat: Double, private val fromLon: Double,
        private val toLat: Double, private val toLon: Double
    ) : Plan {
        override fun at(fraction: Double) = Pair(
            fromLat + (toLat - fromLat) * fraction,
            fromLon + (toLon - fromLon) * fraction
        )
    }

    private class AlongPath(
        private val polyline: Polyline,
        private val s0: Double,
        private val s1: Double
    ) : Plan {
        override fun at(fraction: Double) = polyline.pointAt(s0 + (s1 - s0) * fraction)
    }

    fun plan(from: SimpleVehiclePosition?, to: SimpleVehiclePosition): Plan {
        if (from == null || (from.latitude == to.latitude && from.longitude == to.longitude)) {
            return Static(to.latitude, to.longitude)
        }
        val linear = Linear(from.latitude, from.longitude, to.latitude, to.longitude)
        val polylines = polylinesByLine[to.lineName.trim().uppercase()] ?: return linear

        // Pick the path (direction/variant) closest to BOTH endpoints
        var bestPolyline: Polyline? = null
        var bestS0 = 0.0
        var bestS1 = 0.0
        var bestScore = Double.MAX_VALUE
        for (polyline in polylines) {
            val p0 = polyline.project(from.longitude, from.latitude)
            val p1 = polyline.project(to.longitude, to.latitude)
            val score = max(p0.distanceMeters, p1.distanceMeters)
            if (score < bestScore) {
                bestScore = score
                bestPolyline = polyline
                bestS0 = p0.abscissa
                bestS1 = p1.abscissa
            }
        }
        val polyline = bestPolyline ?: return linear
        if (bestScore > MAX_SNAP_METERS) return linear
        if (bestS0 == bestS1) return Static(to.latitude, to.longitude)
        return AlongPath(polyline, bestS0, bestS1)
    }

    private class Projection(val abscissa: Double, val distanceMeters: Double)

    /**
     * A path in a local equirectangular metric (meters), with cumulative
     * lengths for O(log n) point-at and O(n) projection.
     */
    private class Polyline(path: List<List<Double>>) {
        private val metersPerDegLon: Double = METERS_PER_DEG_LAT * cos(path.first()[1] * PI / 180.0)
        private val xs = DoubleArray(path.size)
        private val ys = DoubleArray(path.size)
        private val cumulative = DoubleArray(path.size)

        init {
            for (i in path.indices) {
                xs[i] = path[i][0] * metersPerDegLon
                ys[i] = path[i][1] * METERS_PER_DEG_LAT
                if (i > 0) {
                    val dx = xs[i] - xs[i - 1]
                    val dy = ys[i] - ys[i - 1]
                    cumulative[i] = cumulative[i - 1] + sqrt(dx * dx + dy * dy)
                }
            }
        }

        fun project(lon: Double, lat: Double): Projection {
            val px = lon * metersPerDegLon
            val py = lat * METERS_PER_DEG_LAT
            var bestDistSq = Double.MAX_VALUE
            var bestS = 0.0
            for (i in 0 until xs.size - 1) {
                val ax = xs[i]; val ay = ys[i]
                val bx = xs[i + 1]; val by = ys[i + 1]
                val abx = bx - ax; val aby = by - ay
                val lengthSq = abx * abx + aby * aby
                val t = if (lengthSq == 0.0) 0.0
                else (((px - ax) * abx + (py - ay) * aby) / lengthSq).coerceIn(0.0, 1.0)
                val cx = ax + abx * t; val cy = ay + aby * t
                val dx = px - cx; val dy = py - cy
                val distSq = dx * dx + dy * dy
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestS = cumulative[i] + sqrt(lengthSq) * t
                }
            }
            return Projection(bestS, sqrt(bestDistSq))
        }

        /** Point at curvilinear abscissa [s] (clamped) as (latitude, longitude). */
        fun pointAt(s: Double): Pair<Double, Double> {
            val clamped = s.coerceIn(0.0, cumulative.last())
            var index = cumulative.asList().binarySearch { it.compareTo(clamped) }
            if (index < 0) index = (-index - 1) - 1
            index = index.coerceIn(0, xs.size - 2)
            val segmentLength = cumulative[index + 1] - cumulative[index]
            val t = if (segmentLength == 0.0) 0.0 else (clamped - cumulative[index]) / segmentLength
            val x = xs[index] + (xs[index + 1] - xs[index]) * t
            val y = ys[index] + (ys[index + 1] - ys[index]) * t
            return Pair(y / METERS_PER_DEG_LAT, x / metersPerDegLon)
        }
    }

    companion object {
        private const val METERS_PER_DEG_LAT = 111_132.0

        // Beyond this distance from every path of its line, a vehicle is
        // considered off-route (deviation) and glides on a straight segment.
        private const val MAX_SNAP_METERS = 120.0
    }
}
