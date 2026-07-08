package eu.dotshell.massilia.generic.utils.map

import eu.dotshell.massilia.generic.data.models.realtime.vehiclepositions.SimpleVehiclePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VehiclePathInterpolatorTest {

    private fun vehicle(lat: Double, lon: Double, line: String = "B1") = SimpleVehiclePosition(
        vehicleId = "V1", lineName = line, latitude = lat, longitude = lon,
        bearing = null, destinationName = null, direction = null
    )

    // L-shaped trace around Marseille: east along lat 43.30, then north along lon 5.40
    private val lShape = listOf(
        listOf(5.380, 43.300),
        listOf(5.400, 43.300),
        listOf(5.400, 43.320)
    )

    private val interpolator = VehiclePathInterpolator(mapOf("B1" to listOf(lShape)))

    @Test
    fun `glide follows the corner of the trace instead of cutting the diagonal`() {
        val from = vehicle(43.300, 5.381)
        val to = vehicle(43.319, 5.400)
        val plan = interpolator.plan(from, to)

        val (midLat, midLon) = plan.at(0.5)
        // The halfway point must sit ON the L (near the corner), not on the
        // straight diagonal between the endpoints (~43.3095, ~5.3905).
        val onHorizontalBranch = abs(midLat - 43.300) < 1e-4
        val onVerticalBranch = abs(midLon - 5.400) < 1e-4
        assertTrue(
            "midpoint ($midLat, $midLon) should lie on the trace",
            onHorizontalBranch || onVerticalBranch
        )

        // Ends converge to the endpoints' projections
        val (startLat, startLon) = plan.at(0.0)
        assertEquals(43.300, startLat, 1e-4)
        assertEquals(5.381, startLon, 1e-4)
        val (endLat, endLon) = plan.at(1.0)
        assertEquals(43.319, endLat, 1e-4)
        assertEquals(5.400, endLon, 1e-4)
    }

    @Test
    fun `vehicle far from every trace glides on a straight segment`() {
        val from = vehicle(43.400, 5.500) // ~10 km off the L
        val to = vehicle(43.410, 5.510)
        val plan = interpolator.plan(from, to)
        val (midLat, midLon) = plan.at(0.5)
        assertEquals(43.405, midLat, 1e-6)
        assertEquals(5.505, midLon, 1e-6)
    }

    @Test
    fun `unknown line or first appearance stays static at the target`() {
        val target = vehicle(43.305, 5.395, line = "ZZ")
        val fresh = interpolator.plan(null, target)
        assertEquals(43.305 to 5.395, fresh.at(0.3))

        val movedUnknownLine = interpolator.plan(vehicle(43.30, 5.39, "ZZ"), target)
        // Unknown line -> linear, still ends at the target
        val (endLat, endLon) = movedUnknownLine.at(1.0)
        assertEquals(43.305, endLat, 1e-9)
        assertEquals(5.395, endLon, 1e-9)
    }
}
