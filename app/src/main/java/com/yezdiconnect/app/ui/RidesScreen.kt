package com.yezdiconnect.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.yezdiconnect.app.BuildConfig
import com.yezdiconnect.app.R
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.rides.Ride

@Composable
fun RidesScreen(app: YezdiApp, onOpen: (Long) -> Unit) {
    val dao = app.database.rides()
    val rides by remember { dao.finishedRides() }.collectAsStateWithLifecycle(initialValue = emptyList())
    if (rides.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_rides), modifier = Modifier.padding(24.dp))
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(rides, key = { it.id }) { ride ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(ride.id) }.padding(16.dp)) {
                Text(formatDateTime(ride.startedAt), style = MaterialTheme.typography.titleMedium)
                Text(rideSummary(ride), style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}

private fun rideSummary(ride: Ride): String = buildList {
    add(formatKm(ride.distanceMeters))
    ride.durationMillis?.let { add(formatDuration(it)) }
    ride.avgSpeedKmh?.let { add("avg " + formatKmh(it)) }
    add("max " + formatKmh(ride.maxSpeedKmh))
}.joinToString(" · ")

@Composable
fun RideDetailScreen(app: YezdiApp, rideId: Long) {
    val dao = app.database.rides()
    val ride by remember(rideId) { dao.observeRide(rideId) }.collectAsStateWithLifecycle(initialValue = null)
    val points by remember(rideId) { dao.points(rideId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val camera = rememberCameraPositionState()
    val path = points.map { LatLng(it.lat, it.lng) }
    var mapLoaded by remember { mutableStateOf(false) }

    // Fitting bounds needs the map's size, so wait until it has loaded.
    LaunchedEffect(path.size, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        if (path.size >= 2) {
            val bounds = LatLngBounds.builder().apply { path.forEach(::include) }.build()
            camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 80))
        } else if (path.size == 1) {
            camera.move(CameraUpdateFactory.newLatLngZoom(path.first(), 15f))
        }
    }

    Column(Modifier.fillMaxSize()) {
        ride?.let { r ->
            Column(Modifier.padding(16.dp)) {
                Text(formatDateTime(r.startedAt), style = MaterialTheme.typography.titleMedium)
                Text(rideSummary(r), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (BuildConfig.MAPS_API_KEY_SET) {
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(0.dp).weight(1f),
                cameraPositionState = camera,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapLoaded = { mapLoaded = true },
            ) {
                if (path.size >= 2) Polyline(points = path, color = MaterialTheme.colorScheme.primary, width = 12f)
            }
        }
    }
}
