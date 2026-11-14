package com.yezdiconnect.app

import android.app.Application
import com.yezdiconnect.app.dash.LoggingDashLink
import com.yezdiconnect.app.obd.ObdManager
import com.yezdiconnect.app.rides.RideDatabase
import com.yezdiconnect.app.rides.RideRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the app's long-lived objects (manual dependency wiring, no DI framework). */
class YezdiApp : Application() {
    /** Lives as long as the app process; for work that outlives a screen (e.g. the OBD link). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { RideDatabase.create(this) }
    val rideRecorder by lazy { RideRecorder(database.rides()) }
    /** Replaced by the real Bluetooth dash link once the protocol is known. */
    val dash by lazy { LoggingDashLink() }
    val obd by lazy { ObdManager(this, appScope) }
}
