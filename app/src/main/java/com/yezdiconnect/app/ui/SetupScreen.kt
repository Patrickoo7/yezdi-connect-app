package com.yezdiconnect.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yezdiconnect.app.R

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    // Bumped on resume and after dialogs, so statuses refresh after returning from system settings.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { refresh++; onPauseOrDispose { } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }

    val hasLocation = remember(refresh) { Permissions.hasLocation(context) }
    val hasPostNotifications = remember(refresh) { Permissions.hasPostNotifications(context) }
    val hasNotificationAccess = remember(refresh) { Permissions.hasNotificationAccess(context) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PermissionItem(
            title = stringResource(R.string.perm_location),
            why = stringResource(R.string.perm_location_why),
            granted = hasLocation,
        ) { launcher.launch(Permissions.location) }

        if (Build.VERSION.SDK_INT >= 33) {
            PermissionItem(
                title = stringResource(R.string.perm_notifications),
                why = stringResource(R.string.perm_notifications_why),
                granted = hasPostNotifications,
            ) { launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) }
        }

        PermissionItem(
            title = stringResource(R.string.perm_notification_access),
            why = stringResource(R.string.perm_notification_access_why),
            granted = hasNotificationAccess,
        ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }
}

@Composable
private fun PermissionItem(title: String, why: String, granted: Boolean, onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(why, style = MaterialTheme.typography.bodySmall)
            }
            if (granted) {
                Text(stringResource(R.string.granted), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            } else {
                Button(onClick = onGrant, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.grant)) }
            }
        }
    }
}
