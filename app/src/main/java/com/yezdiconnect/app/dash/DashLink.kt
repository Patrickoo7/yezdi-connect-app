package com.yezdiconnect.app.dash

import android.util.Log
import com.yezdiconnect.app.alerts.PhoneAlert
import com.yezdiconnect.app.nav.Maneuver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the app can show on the bike's dashboard. */
interface DashLink {
    fun showManeuver(maneuver: Maneuver)
    fun clearNavigation()
    fun showAlert(alert: PhoneAlert)
}

/**
 * Stand-in until the dash command format is known (docs/YOUR-TASKS.txt, task 2).
 * It records what would be sent so the Bike screen can show it, and logs it.
 */
class LoggingDashLink : DashLink {
    private val _lastSent = MutableStateFlow<String?>(null)
    val lastSent: StateFlow<String?> = _lastSent.asStateFlow()

    override fun showManeuver(maneuver: Maneuver) = record("NAV ${maneuver.type} ${maneuver.distance?.display() ?: "-"} ${maneuver.road.orEmpty()}")
    override fun clearNavigation() = record("NAV clear")
    override fun showAlert(alert: PhoneAlert) = record("${alert.kind} ${alert.from}")

    private fun record(line: String) {
        _lastSent.value = line
        Log.i("DashLink", line)
    }
}
