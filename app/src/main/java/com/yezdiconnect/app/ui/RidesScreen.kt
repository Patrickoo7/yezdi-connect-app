package com.yezdiconnect.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                stringResource(R.string.tab_rides),
                stringResource(R.string.rides_subtitle),
                Modifier.padding(bottom = 4.dp),
            )
        }
        if (rides.isEmpty()) {
            item { NoticeBox(stringResource(R.string.no_rides)) }
        } else {
            item {
                TileRow {
                    StatTile(rides.size.toString(), stringResource(R.string.stat_rides), Modifier.weight(1f))
                    StatTile(formatKm(rides.sumOf { it.distanceMeters }), stringResource(R.string.stat_total), Modifier.weight(1f))
                }
            }
            item { SectionLabel(stringResource(R.string.ride_history)) }
            items(rides, key = { it.id }) { ride ->
                SurfaceCard(Modifier.clip(MaterialTheme.shapes.medium).clickable { onOpen(ride.id) }) {
                    Text(formatDateTime(ride.startedAt), style = MaterialTheme.typography.titleMedium)
                    Text(rideSummary(ride), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
                }
            }
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
    val context = LocalContext.current
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

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ride?.let { r ->
            ScreenHeader(stringResource(R.string.ride_title), formatDateTime(r.startedAt))
            TileRow {
                StatTile(formatKm(r.distanceMeters), stringResource(R.string.stat_distance), Modifier.weight(1f))
                StatTile(r.durationMillis?.let(::formatDuration) ?: "–", stringResource(R.string.stat_time), Modifier.weight(1f))
            }
            TileRow {
                StatTile(r.avgSpeedKmh?.let(::formatKmh) ?: "–", stringResource(R.string.stat_avg_speed), Modifier.weight(1f))
                StatTile(formatKmh(r.maxSpeedKmh), stringResource(R.string.stat_max_speed), Modifier.weight(1f))
            }
        }
        if (BuildConfig.MAPS_API_KEY_SET) {
            val mapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(0.dp).weight(1f).clip(MaterialTheme.shapes.medium),
                cameraPositionState = camera,
                properties = MapProperties(mapStyleOptions = mapStyle),
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapLoaded = { mapLoaded = true },
            ) {
                if (path.size >= 2) Polyline(points = path, color = YezdiColors.Accent, width = 12f)
            }
        }
    }
}
