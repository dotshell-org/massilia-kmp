package com.pelotcl.app.generic.utils.graphics

import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import android.util.LruCache
import com.pelotcl.app.generic.data.models.geojson.StopFeature

/**
 * Android-specific BusIconHelper with resource ID resolution.
 * Core logic (desserte parsing, drawable name mapping) is in [LineIconResolver].
 */
object BusIconHelper {

    private val desserteCache = LruCache<String, List<String>>(500)
    private val resourceIdCache = HashMap<String, Int>(256)

    // ── Delegated to LineIconResolver ──────────────────────────────────

    fun getDrawableNameForLineName(lineName: String): String =
        LineIconResolver.getDrawableNameForLineName(lineName)

    fun getAllLinesForStop(stopFeature: StopFeature): List<String> {
        val desserte = stopFeature.properties.desserte
        desserteCache.get(desserte)?.let { return it }
        val result = LineIconResolver.parseDesserte(desserte)
        desserteCache.put(desserte, result)
        return result
    }

    // ── Android-specific resource resolution ───────────────────────────

    /**
     * Resolves a line name to its drawable resource ID, using a cache to avoid
     * repeated calls to resources.getIdentifier() (which uses reflection internally).
     */
    @Suppress("DiscouragedApi")
    fun getResourceIdForLine(context: Context, lineName: String): Int {
        val drawableName = getDrawableNameForLineName(lineName)
        if (drawableName.isBlank()) return 0
        return resourceIdCache.getOrPut(drawableName) {
            context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        }
    }

    /**
     * Resolves a raw drawable name to its resource ID, using a cache.
     */
    @Suppress("DiscouragedApi")
    fun getResourceIdForDrawableName(context: Context, drawableName: String): Int {
        if (drawableName.isBlank()) return 0
        return resourceIdCache.getOrPut(drawableName) {
            context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        }
    }

    /**
     * Pre-populates the resourceIdCache with ALL drawable resource IDs in a single reflection pass.
     * This replaces ~960 individual getIdentifier() calls (each using reflection) with one bulk scan
     * of R.drawable fields. Call once at startup on a background thread.
     */
    fun preloadResourceIds(context: Context) {
        if (resourceIdCache.isNotEmpty()) return
        try {
            val drawableClass = Class.forName($$"$${context.packageName}.R$drawable")
            for (field in drawableClass.fields) {
                resourceIdCache[field.name] = field.getInt(null)
            }
        } catch (e: Exception) {
            Log.w("BusIconHelper", "Failed to preload resource IDs: ${e.message}")
        }
    }

    /**
     * Trims the cache under memory pressure.
     */
    fun trimCache(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                desserteCache.evictAll()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                desserteCache.trimToSize(desserteCache.maxSize() / 2)
            }
        }
    }
}
