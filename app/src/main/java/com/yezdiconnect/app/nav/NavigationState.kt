package com.yezdiconnect.app.nav

import com.yezdiconnect.app.alerts.PhoneAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest navigation step and phone alert, shared between the notification listener and the UI. */
object NavigationState {
    private val _maneuver = MutableStateFlow<Maneuver?>(null)
    val maneuver: StateFlow<Maneuver?> = _maneuver.asStateFlow()

    private val _lastAlert = MutableStateFlow<PhoneAlert?>(null)
    val lastAlert: StateFlow<PhoneAlert?> = _lastAlert.asStateFlow()

    /** Raw text of the last Google Maps notification, for the debug card. */
    private val _lastRaw = MutableStateFlow<String?>(null)
    val lastRaw: StateFlow<String?> = _lastRaw.asStateFlow()

    internal fun update(maneuver: Maneuver?, raw: String?) {
        _maneuver.value = maneuver
        _lastRaw.value = raw
    }

    internal fun alert(alert: PhoneAlert) { _lastAlert.value = alert }
}
