package eu.dotshell.massilia

import eu.dotshell.massilia.generic.data.repository.offline.search.SearchHistoryItem
import eu.dotshell.massilia.generic.data.repository.offline.search.SearchType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the search-history serialization compatibility around the ADDRESS entries
 * (coordinates persisted so addresses re-select without re-geocoding).
 */
class SearchHistoryItemTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun legacyEntriesWithoutCoordinateFieldsStillDecode() {
        val legacy = """
            [{"query":"Vieux Port","type":"STOP","lines":["M1","M2"],"timestamp":1720000000000},
             {"query":"T2","type":"LINE","timestamp":1720000000001}]
        """.trimIndent()
        val items = json.decodeFromString<List<SearchHistoryItem>>(legacy)

        assertEquals(2, items.size)
        assertEquals(SearchType.STOP, items[0].type)
        assertNull(items[0].lat)
        assertNull(items[0].detail)
    }

    @Test
    fun addressEntryRoundTripsWithCoordinates() {
        val address = SearchHistoryItem(
            query = "Carrefour Bonneveine",
            type = SearchType.ADDRESS,
            lat = 43.2455,
            lon = 5.3810,
            detail = "Avenue de Hambourg, 13008 Marseille"
        )
        val decoded = json.decodeFromString<SearchHistoryItem>(json.encodeToString(SearchHistoryItem.serializer(), address))

        assertEquals(address.query, decoded.query)
        assertEquals(SearchType.ADDRESS, decoded.type)
        assertEquals(43.2455, decoded.lat!!, 1e-9)
        assertEquals(5.3810, decoded.lon!!, 1e-9)
        assertEquals(address.detail, decoded.detail)
    }
}
