package com.pelotcl.app.generic.data.telemetry

actual fun emitTelemetryEvent(event: TelemetryEvent) {
    TelemetryEmitter.emit(event)
}
