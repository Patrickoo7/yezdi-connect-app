package com.yezdiconnect.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yezdiconnect.app.R
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.nav.NavigationState

/**
 * Bike connection and live stats come here once the protocol is mapped.
 * Until then it shows what the app *would* send to the dash, to test the Google Maps bridge.
 */
@Composable
fun BikeScreen(app: YezdiApp) {
    val lastSent by app.dash.lastSent.collectAsStateWithLifecycle()
    val raw by NavigationState.lastRaw.collectAsStateWithLifecycle()
    val alert by NavigationState.lastAlert.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard(stringResource(R.string.bike_connection), stringResource(R.string.bike_waiting_protocol))
        InfoCard(stringResource(R.string.dash_last_sent), lastSent ?: stringResource(R.string.nothing_yet), mono = true)
        InfoCard(stringResource(R.string.maps_last_notification), raw ?: stringResource(R.string.nothing_yet), mono = true)
        InfoCard(
            stringResource(R.string.last_alert),
            alert?.let { "${it.kind}: ${it.from} (${it.app})" } ?: stringResource(R.string.nothing_yet),
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String, mono: Boolean = false) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, fontFamily = if (mono) FontFamily.Monospace else null)
        }
    }
}
