package com.yezdiconnect.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.yezdiconnect.app.nav.PhoneNotificationListener

object Permissions {
    val location = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasLocation(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasPostNotifications(c: Context) =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Whether Android's battery optimization is off for this app (needed to keep rides and the dash link alive). */
    fun ignoresBatteryOptimizations(c: Context): Boolean =
        c.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(c.packageName)

    /** Notification access is a special permission granted in system settings, not by a dialog. */
    fun hasNotificationAccess(c: Context): Boolean {
        val enabled = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(c, PhoneNotificationListener::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
