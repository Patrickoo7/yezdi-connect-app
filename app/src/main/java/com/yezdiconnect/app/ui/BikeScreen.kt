package com.yezdiconnect.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private const val NO_VALUE = "–"

/**
 * Live bike data goes in the tiles once the Bluetooth protocol is mapped (docs/YOUR-TASKS.txt).
 * Until then the tiles show "–", and the dash test section shows what the app *would* send.
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
        ScreenHeader(stringResource(R.string.tab_bike), stringResource(R.string.bike_subtitle))
        NoticeBox(stringResource(R.string.bike_waiting_protocol))

        SectionLabel(stringResource(R.string.section_live))
        TileRow {
            StatTile(NO_VALUE, stringResource(R.string.stat_speed), Modifier.weight(1f))
            StatTile(NO_VALUE, stringResource(R.string.stat_rpm), Modifier.weight(1f))
        }
        TileRow {
            StatTile(NO_VALUE, stringResource(R.string.stat_gear), Modifier.weight(1f))
            StatTile(NO_VALUE, stringResource(R.string.stat_battery), Modifier.weight(1f))
        }

        SectionLabel(stringResource(R.string.section_trip))
        TileRow {
            StatTile(NO_VALUE, stringResource(R.string.stat_odometer), Modifier.weight(1f))
            StatTile(NO_VALUE, stringResource(R.string.stat_range), Modifier.weight(1f))
        }
        TileRow {
            StatTile(NO_VALUE, stringResource(R.string.stat_economy), Modifier.weight(1f))
            StatTile(NO_VALUE, stringResource(R.string.stat_health), Modifier.weight(1f))
        }

        SectionLabel(stringResource(R.string.section_dash_test))
        DebugCard(stringResource(R.string.dash_last_sent), lastSent ?: stringResource(R.string.nothing_yet))
        DebugCard(stringResource(R.string.maps_last_notification), raw ?: stringResource(R.string.nothing_yet))
        DebugCard(
            stringResource(R.string.last_alert),
            alert?.let { "${it.kind}: ${it.from} (${it.app})" } ?: stringResource(R.string.nothing_yet),
        )
    }
}

@Composable
private fun DebugCard(title: String, body: String) {
    SurfaceCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, fontFamily = FontFamily.Monospace)
    }
}
