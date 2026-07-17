package eu.dotshell.massilia

import eu.dotshell.massilia.generic.data.network.geocoding.PhotonResponse
import eu.dotshell.massilia.generic.data.repository.geocoding.isNearMarseille
import eu.dotshell.massilia.generic.data.repository.geocoding.mapPhotonFeatures
import eu.dotshell.massilia.generic.data.repository.geocoding.photonFeatureToResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Photon response mapping: GeoJSON [lon, lat] ordering, label/detail building,
 * region filtering and deduplication — on fixture JSON, no HTTP involved.
 */
class GeocodingRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    // A realistic Photon payload: a POI, a house-number address, an unusable feature,
    // an out-of-region hit (Paris) and a duplicate of the POI.
    private val fixture = """
    {
      "type": "FeatureCollection",
      "features": [
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.3810, 43.2455] },
          "properties": { "name": "Carrefour Bonneveine", "street": "Avenue de Hambourg",
                          "postcode": "13008", "city": "Marseille", "osm_key": "shop", "osm_value": "supermarket" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.3690, 43.2990] },
          "properties": { "housenumber": "12", "street": "Rue de la République",
                          "postcode": "13002", "city": "Marseille", "osm_key": "place", "osm_value": "house" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.37, 43.29] },
          "properties": { "postcode": "13000", "city": "Marseille" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [2.2945, 48.8584] },
          "properties": { "name": "Tour Eiffel", "city": "Paris" }
        },
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [5.3810, 43.2455] },
          "properties": { "name": "Carrefour Bonneveine", "street": "Avenue de Hambourg",
                          "postcode": "13008", "city": "Marseille" }
        }
      ]
    }
    """.trimIndent()

    @Test
    fun mapsFixtureFilteringAndDeduplicating() {
        val response = json.decodeFromString<PhotonResponse>(fixture)
        assertEquals(5, response.features.size)

        val results = mapPhotonFeatures(response.features, limit = 6)

        // Unusable + Paris + duplicate dropped, Photon order preserved
        assertEquals(2, results.size)

        val poi = results[0]
        assertEquals("Carrefour Bonneveine", poi.label)
        assertEquals("Avenue de Hambourg, 13008 Marseille", poi.detail)
        // GeoJSON order is [lon, lat]
        assertEquals(43.2455, poi.lat, 1e-9)
        assertEquals(5.3810, poi.lon, 1e-9)

        val address = results[1]
        assertEquals("12 Rue de la République", address.label)
        assertEquals("13002 Marseille", address.detail)
    }

    @Test
    fun limitIsApplied() {
        val response = json.decodeFromString<PhotonResponse>(fixture)
        val results = mapPhotonFeatures(response.features, limit = 1)
        assertEquals(1, results.size)
        assertEquals("Carrefour Bonneveine", results[0].label)
    }

    @Test
    fun featureWithoutNameOrStreetIsDropped() {
        val response = json.decodeFromString<PhotonResponse>(fixture)
        assertNull(photonFeatureToResult(response.features[2]))
    }

    @Test
    fun featureWithoutCoordinatesIsDropped() {
        val broken = json.decodeFromString<PhotonResponse>(
            """{"features":[{"geometry":{"coordinates":[]},"properties":{"name":"Nowhere"}}]}"""
        )
        assertNull(photonFeatureToResult(broken.features[0]))
    }

    @Test
    fun regionFilterKeepsMarseilleAreaOnly() {
        assertTrue("Marseille center", isNearMarseille(43.2965, 5.3698))
        assertTrue("Aix-en-Provence (~28 km)", isNearMarseille(43.5297, 5.4474))
        assertFalse("Paris (~660 km)", isNearMarseille(48.8584, 2.2945))
        assertFalse("Lyon (~275 km)", isNearMarseille(45.7640, 4.8357))
    }
}
