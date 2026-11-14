package com.yezdiconnect.app

import android.app.Application
import com.yezdiconnect.app.dash.LoggingDashLink
import com.yezdiconnect.app.rides.RideDatabase
import com.yezdiconnect.app.rides.RideRecorder

/** Holds the app's long-lived objects (manual dependency wiring, no DI framework). */
class YezdiApp : Application() {
    val database by lazy { RideDatabase.create(this) }
    val rideRecorder by lazy { RideRecorder(database.rides()) }
    /** Replaced by the real Bluetooth dash link once the protocol is known. */
    val dash by lazy { LoggingDashLink() }
}
