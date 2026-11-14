package com.yezdiconnect.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.yezdiconnect.app.BuildConfig
import com.yezdiconnect.app.R
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.nav.NavigationState
import com.yezdiconnect.app.nav.PhoneNotificationListener
import com.yezdiconnect.app.rides.RideTrackingService

private val INDIA_CENTER = LatLng(20.59, 78.96)

@SuppressLint("MissingPermission") // guarded by hasLocation
@Composable
fun MapScreen(app: YezdiApp) {
    val context = LocalContext.current
    var hasLocation by remember { mutableStateOf(Permissions.hasLocation(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocation = Permissions.hasLocation(context)
    }
    val live by app.rideRecorder.live.collectAsStateWithLifecycle()
    val maneuver by NavigationState.maneuver.collectAsStateWithLifecycle()
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(INDIA_CENTER, 4f) }
    var mapLoaded by remember { mutableStateOf(false) }

    // Center on the rider once when the screen opens.
    LaunchedEffect(hasLocation, mapLoaded) {
        if (!hasLocation || !mapLoaded) return@LaunchedEffect
        LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
            if (loc != null && camera.position.zoom < 10f) {
                camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f))
            }
        }
    }
    // Follow the rider while recording.
    LaunchedEffect(live?.path?.size, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        val last = live?.path?.lastOrNull() ?: return@LaunchedEffect
        camera.animate(CameraUpdateFactory.newLatLng(LatLng(last.first, last.second)))
    }

    Box(Modifier.fillMaxSize()) {
        if (BuildConfig.MAPS_API_KEY_SET) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                properties = MapProperties(isMyLocationEnabled = hasLocation),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocation, zoomControlsEnabled = false),
                onMapLoaded = { mapLoaded = true },
            ) {
                live?.path?.takeIf { it.size >= 2 }?.let { path ->
                    Polyline(points = path.map { LatLng(it.first, it.second) }, color = MaterialTheme.colorScheme.primary, width = 12f)
                }
            }
        } else {
            Text(
                stringResource(R.string.maps_key_missing),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }

        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigateBar()
            maneuver?.let { m ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${m.distance?.display() ?: ""}  ${m.type.name.replace('_', ' ').lowercase()}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        m.road?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }

        Card(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val ride = live
                if (ride == null) {
                    Text(stringResource(R.string.not_recording), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        if (Permissions.hasLocation(context)) RideTrackingService.start(context)
                        else permissionLauncher.launch(Permissions.location)
                    }) { Text(stringResource(R.string.start_ride)) }
                } else {
                    Column {
                        Text(formatKmh(ride.speedKmh), style = MaterialTheme.typography.titleLarge)
                        Text(formatKm(ride.distanceMeters), style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = { RideTrackingService.stop(context) }) { Text(stringResource(R.string.stop_ride)) }
                }
            }
        }
    }
}

/** Opens Google Maps turn-by-turn in two-wheeler mode; the notification listener forwards it to the dash. */
@Composable
private fun NavigateBar() {
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf("") }
    val go = {
        if (destination.isNotBlank()) {
            val uri = Uri.parse("google.navigation:q=${Uri.encode(destination.trim())}&mode=l")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(PhoneNotificationListener.GOOGLE_MAPS))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.google_maps_missing, Toast.LENGTH_LONG).show()
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.where_to)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { go() }),
            )
            Button(onClick = go, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.navigate)) }
        }
    }
}
