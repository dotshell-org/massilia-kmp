package com.pelotcl.app.platform

/**
 * iOS stub: no deferrable-work scheduler is wired yet (a BGTaskScheduler-backed implementation
 * can replace this when iOS becomes an active target). All operations log and no-op.
 */
actual class BackgroundScheduler actual constructor(context: PlatformContext) {

    actual fun scheduleTelemetryUpload(delaySeconds: Long) {
        Log.i(TAG, "scheduleTelemetryUpload($delaySeconds) — no-op on iOS")
    }

    actual fun cancelTelemetryUpload() {
        Log.i(TAG, "cancelTelemetryUpload — no-op on iOS")
    }

    actual fun ensureTrafficAlertsScheduled() {
        Log.i(TAG, "ensureTrafficAlertsScheduled — no-op on iOS")
    }

    private companion object {
        const val TAG = "BackgroundScheduler"
    }
}
