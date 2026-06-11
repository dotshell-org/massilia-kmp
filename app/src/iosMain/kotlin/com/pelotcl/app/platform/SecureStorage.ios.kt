package com.pelotcl.app.platform

import platform.Foundation.NSUserDefaults

/**
 * iOS best-effort implementation backed by NSUserDefaults. A Keychain-backed store
 * can replace this when iOS becomes an active target.
 */
actual class SecureStorage actual constructor(context: PlatformContext, name: String) {

    private val defaults = NSUserDefaults(suiteName = name)

    actual fun getString(key: String): String? = defaults.stringForKey(key)

    actual fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)

    actual fun getLong(key: String, defaultValue: Long): Long =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else defaultValue

    actual fun putLong(key: String, value: Long) = defaults.setInteger(value, forKey = key)

    actual fun remove(key: String) = defaults.removeObjectForKey(key)
}
