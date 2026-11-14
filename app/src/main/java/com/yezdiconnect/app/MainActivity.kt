package com.yezdiconnect.app

import android.content.ComponentName
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yezdiconnect.app.nav.PhoneNotificationListener
import com.yezdiconnect.app.ui.Permissions
import com.yezdiconnect.app.ui.YezdiAppUi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as YezdiApp
        setContent { YezdiAppUi(app) }
    }

    override fun onResume() {
        super.onResume()
        // Some phones (Samsung One UI in particular) silently unbind notification listeners.
        // Asking for a rebind is harmless when it's already connected.
        if (Permissions.hasNotificationAccess(this)) {
            NotificationListenerService.requestRebind(ComponentName(this, PhoneNotificationListener::class.java))
        }
    }
}
