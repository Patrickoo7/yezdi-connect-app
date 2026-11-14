package com.yezdiconnect.app.nav

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.alerts.AlertKind
import com.yezdiconnect.app.alerts.PhoneAlert

/**
 * Reads two kinds of notifications once the user grants notification access:
 *  - Google Maps' ongoing navigation notification → next turn for the dash;
 *  - incoming call and message notifications → caller/sender for the dash.
 * Nothing is stored or sent anywhere except the bike's dash.
 */
class PhoneNotificationListener : NotificationListenerService() {

    private val dash get() = (application as YezdiApp).dash
    private var lastSentManeuver: Maneuver? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        when {
            sbn.packageName == GOOGLE_MAPS && n.isOngoingNavigation() -> onMapsNavigation(n)
            n.category == Notification.CATEGORY_CALL && sbn.packageName in DIALERS -> onAlert(sbn, AlertKind.CALL)
            n.category == Notification.CATEGORY_MESSAGE -> onAlert(sbn, AlertKind.MESSAGE)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == GOOGLE_MAPS && sbn.notification?.isOngoingNavigation() == true) {
            NavigationState.update(null, null)
            lastSentManeuver = null
            dash.clearNavigation()
        }
    }

    private fun onMapsNavigation(n: Notification) {
        val e = n.extras
        val lines = listOf(
            e.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            e.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        )
        val maneuver = ManeuverParser.parse(*lines.toTypedArray())
        NavigationState.update(maneuver, lines.filterNotNull().joinToString(" | "))
        // Google Maps reposts the notification often; only send real changes to the dash.
        if (maneuver != null && maneuver != lastSentManeuver) {
            lastSentManeuver = maneuver
            dash.showManeuver(maneuver)
        }
    }

    private fun onAlert(sbn: StatusBarNotification, kind: AlertKind) {
        // Group summaries and our own notifications are not real alerts.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (sbn.packageName == packageName) return
        val from = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val alert = PhoneAlert(kind, from, sbn.packageName, sbn.postTime)
        NavigationState.alert(alert)
        dash.showAlert(alert)
    }

    private fun Notification.isOngoingNavigation(): Boolean =
        flags and Notification.FLAG_ONGOING_EVENT != 0 &&
            (category == Notification.CATEGORY_NAVIGATION || category == null)

    companion object {
        const val GOOGLE_MAPS = "com.google.android.apps.maps"
        private val DIALERS = setOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui", // Samsung's incoming-call screen (One UI)
            "com.android.incallui",
            "com.android.server.telecom",
        )
    }
}
