package com.yezdiconnect.app.rides

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yezdiconnect.app.MainActivity
import com.yezdiconnect.app.R
import com.yezdiconnect.app.YezdiApp
import kotlinx.coroutines.launch

/** Records GPS while a ride is active, also with the screen off. Started from the map screen. */
class RideTrackingService : LifecycleService() {

    private val recorder get() = (application as YezdiApp).rideRecorder
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val locations = result.locations
            lifecycleScope.launch {
                for (l in locations) {
                    recorder.onLocation(l.time, l.latitude, l.longitude, l.speed * 3.6, l.accuracy)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                lifecycleScope.launch {
                    recorder.stop(System.currentTimeMillis())
                    stopSelf()
                }
            }
            else -> startRecording()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // the UI only starts this after location permission is granted
    private fun startRecording() {
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        lifecycleScope.launch { recorder.start(System.currentTimeMillis()) }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateDistanceMeters(2f)
            .build()
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_ride), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RideTrackingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ride)
            .setContentTitle(getString(R.string.ride_recording))
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop_ride), stop)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ride"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.yezdiconnect.app.STOP_RIDE"

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, RideTrackingService::class.java))

        fun stop(context: Context) =
            context.startService(Intent(context, RideTrackingService::class.java).setAction(ACTION_STOP))
    }
}
