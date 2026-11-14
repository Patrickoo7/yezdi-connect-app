package com.yezdiconnect.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yezdiconnect.app.R
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.nav.NavigationState
import com.yezdiconnect.app.obd.AdapterScanner
import com.yezdiconnect.app.obd.FaultCodes
import com.yezdiconnect.app.obd.FillUpEntry
import com.yezdiconnect.app.obd.FuelEconomy
import com.yezdiconnect.app.obd.ObdLive
import com.yezdiconnect.app.obd.ObdPid
import com.yezdiconnect.app.obd.ObdState
import com.yezdiconnect.app.rides.FillUp
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun BikeScreen(app: YezdiApp) {
    val obdState by app.obd.state.collectAsStateWithLifecycle()
    val live by app.obd.live.collectAsStateWithLifecycle()
    val codes by app.obd.faultCodes.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.tab_bike), stringResource(R.string.bike_subtitle))

        SectionLabel(stringResource(R.string.section_obd))
        ObdAdapterCard(app, obdState, onFind = { showPicker = true })

        live?.let { LiveEngine(it) }

        SectionLabel(stringResource(R.string.section_health))
        HealthCard(codes, connected = obdState is ObdState.Connected, onRead = app.obd::readFaultCodes)

        SectionLabel(stringResource(R.string.section_fuel))
        FuelSection(app)

        SectionLabel(stringResource(R.string.section_bike_bluetooth))
        NoticeBox(stringResource(R.string.bike_waiting_protocol))
        DashTestCards(app)
    }

    if (showPicker) AdapterPicker(app, onDismiss = { showPicker = false })
}

@Composable
private fun ObdAdapterCard(app: YezdiApp, state: ObdState, onFind: () -> Unit) {
    val context = LocalContext.current
    val saved = app.obd.savedAdapter
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Permissions.hasBluetooth(context)) app.obd.reconnect()
    }
    SurfaceCard {
        when (state) {
            ObdState.Idle -> {
                Text(stringResource(R.string.obd_intro), style = MaterialTheme.typography.bodyMedium, color = YezdiColors.Muted)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.obd_find_adapter), onFind)
                if (saved != null) {
                    SecondaryButton(stringResource(R.string.obd_reconnect, saved.name), Modifier.padding(top = 8.dp)) {
                        if (Permissions.hasBluetooth(context)) app.obd.reconnect() else permission.launch(Permissions.bluetooth)
                    }
                }
            }
            is ObdState.Connecting -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), color = YezdiColors.Accent, strokeWidth = 2.dp)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.adapterName, style = MaterialTheme.typography.titleMedium)
                    Text(state.step, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
                }
                TextButton(onClick = app.obd::disconnect) { Text(stringResource(R.string.cancel), color = YezdiColors.Muted) }
            }
            is ObdState.Connected -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.obd_connected), style = MaterialTheme.typography.titleMedium, color = YezdiColors.Ok)
                        Text(state.adapterName, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
                    }
                    SecondaryButton(stringResource(R.string.obd_disconnect), onClick = app.obd::disconnect)
                }
                Spacer(Modifier.height(10.dp))
                InfoLine(stringResource(R.string.obd_protocol), state.info.protocol)
                state.vin?.let { InfoLine(stringResource(R.string.obd_vin), it) }
                InfoLine(stringResource(R.string.obd_values), state.info.supportedPids.size.toString())
            }
            is ObdState.Failed -> {
                Text(state.adapterName, style = MaterialTheme.typography.titleMedium)
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = YezdiColors.Error, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.obd_try_again), onClick = {
                    if (Permissions.hasBluetooth(context)) app.obd.retry() else permission.launch(Permissions.bluetooth)
                })
                SecondaryButton(stringResource(R.string.obd_find_adapter), Modifier.padding(top = 8.dp), onFind)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, YezdiColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = YezdiColors.Text),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

private fun fmt(v: Double, decimals: Int = 0) = String.format(Locale.getDefault(), "%.${decimals}f", v)

/** Live tiles for whatever the bike's engine computer supports. */
@Composable
private fun LiveEngine(live: ObdLive) {
    val v = live.values
    val tiles = buildList {
        v[ObdPid.SPEED]?.let { add(fmt(it) + " km/h" to R.string.stat_speed) }
        v[ObdPid.RPM]?.let { add(fmt(it) to R.string.stat_rpm) }
        v[ObdPid.COOLANT_TEMP]?.let { add(fmt(it) + " °C" to R.string.stat_engine_temp) }
        v[ObdPid.OIL_TEMP]?.let { add(fmt(it) + " °C" to R.string.stat_oil_temp) }
        v[ObdPid.THROTTLE]?.let { add(fmt(it) + " %" to R.string.stat_throttle) }
        v[ObdPid.ENGINE_LOAD]?.let { add(fmt(it) + " %" to R.string.stat_engine_load) }
        (live.batteryVolts ?: v[ObdPid.MODULE_VOLTAGE])?.let { add(fmt(it, 1) + " V" to R.string.stat_battery) }
        v[ObdPid.INTAKE_AIR_TEMP]?.let { add(fmt(it) + " °C" to R.string.stat_intake_temp) }
        v[ObdPid.FUEL_LEVEL]?.let { add(fmt(it) + " %" to R.string.stat_fuel_level) }
    }
    SectionLabel(stringResource(R.string.section_live))
    tiles.chunked(2).forEach { row ->
        TileRow {
            row.forEach { (value, label) -> StatTile(value, stringResource(label), Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    TileRow {
        StatTile(
            live.tripKmPerLitre?.let { fmt(it, 1) + " km/l" } ?: "–",
            stringResource(R.string.stat_trip_economy_est),
            Modifier.weight(1f),
            valueColor = YezdiColors.Accent,
        )
        StatTile(
            live.fuelRate?.let { r -> r.kmPerLitre?.let { fmt(it, 0) + " km/l" } ?: (fmt(r.litresPerHour, 2) + " L/h") } ?: "–",
            stringResource(R.string.stat_now_economy_est),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun HealthCard(codes: FaultCodes, connected: Boolean, onRead: () -> Unit) {
    SurfaceCard {
        when (codes) {
            FaultCodes.NotRead -> Text(
                stringResource(if (connected) R.string.health_not_read else R.string.health_connect_first),
                style = MaterialTheme.typography.bodyMedium, color = YezdiColors.Muted,
            )
            FaultCodes.Reading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), color = YezdiColors.Accent, strokeWidth = 2.dp)
                Text(stringResource(R.string.health_reading), modifier = Modifier.padding(start = 12.dp))
            }
            is FaultCodes.Failed -> Text(codes.message, color = YezdiColors.Error, style = MaterialTheme.typography.bodyMedium)
            is FaultCodes.Read -> if (codes.codes.isEmpty()) {
                Text(stringResource(R.string.health_ok), style = MaterialTheme.typography.titleMedium, color = YezdiColors.Ok)
                Text(formatDateTime(codes.at), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
            } else {
                codes.codes.forEachIndexed { i, dtc ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dtc.code, style = MaterialTheme.typography.titleMedium, color = YezdiColors.Error, fontFamily = FontFamily.Monospace)
                        if (dtc.pending) {
                            Text(stringResource(R.string.health_pending), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Warn, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(dtc.description, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
                }
                Text(stringResource(R.string.health_note), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, modifier = Modifier.padding(top = 12.dp))
            }
        }
        if (connected && codes !is FaultCodes.Reading) {
            PrimaryButton(stringResource(R.string.health_read), onRead, Modifier.padding(top = 14.dp))
        }
    }
}

@Composable
private fun FuelSection(app: YezdiApp) {
    val dao = app.database.rides()
    val fills by remember { dao.fillUps() }.collectAsStateWithLifecycle(initialValue = emptyList())
    var adding by remember { mutableStateOf(false) }
    val economy = remember(fills) { FuelEconomy.fromFillUps(fills.map { FillUpEntry(it.odometerKm, it.litres, it.fullTank) }) }

    TileRow {
        StatTile(economy?.let { fmt(it.average, 1) + " km/l" } ?: "–", stringResource(R.string.fuel_average), Modifier.weight(1f), valueColor = YezdiColors.Accent)
        StatTile(economy?.let { fmt(it.last, 1) + " km/l" } ?: "–", stringResource(R.string.fuel_last_tank), Modifier.weight(1f))
    }
    SurfaceCard {
        if (economy == null) {
            Text(stringResource(R.string.fuel_how), style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted)
            Spacer(Modifier.height(12.dp))
        }
        fills.take(5).forEach { f ->
            Text(
                "${formatDateTime(f.time)} · ${fmt(f.odometerKm)} km · ${fmt(f.litres, 2)} L" +
                    if (f.fullTank) "" else " · " + stringResource(R.string.fuel_partial),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        PrimaryButton(stringResource(R.string.fuel_add), onClick = { adding = true })
    }
    if (adding) FillUpDialog(lastOdometer = fills.firstOrNull()?.odometerKm, onDismiss = { adding = false }) { fill ->
        adding = false
        app.appScope.launch { dao.insertFillUp(fill) }
    }
}

@Composable
private fun FillUpDialog(lastOdometer: Double?, onDismiss: () -> Unit, onSave: (FillUp) -> Unit) {
    var odometer by remember { mutableStateOf("") }
    var litres by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(true) }
    val odo = odometer.replace(',', '.').toDoubleOrNull()
    val lit = litres.replace(',', '.').toDoubleOrNull()
    val valid = odo != null && lit != null && lit > 0 && lit < 50 && (lastOdometer == null || odo > lastOdometer)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = YezdiColors.Accent, unfocusedBorderColor = YezdiColors.Outline, cursorColor = YezdiColors.Accent,
        focusedLabelColor = YezdiColors.Accent,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YezdiColors.Surface,
        title = { Text(stringResource(R.string.fuel_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    odometer, { odometer = it },
                    label = { Text(stringResource(R.string.fuel_odometer)) },
                    singleLine = true, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = lastOdometer?.let { { Text(stringResource(R.string.fuel_odometer_last, fmt(it))) } },
                )
                OutlinedTextField(
                    litres, { litres = it },
                    label = { Text(stringResource(R.string.fuel_litres)) },
                    singleLine = true, colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(Modifier.clickable { full = !full }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(full, { full = it }, colors = CheckboxDefaults.colors(checkedColor = YezdiColors.Accent))
                    Text(stringResource(R.string.fuel_full_tank))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(FillUp(time = System.currentTimeMillis(), odometerKm = odo!!, litres = lit!!, fullTank = full))
            }) { Text(stringResource(R.string.save), color = if (valid) YezdiColors.Accent else YezdiColors.Muted) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = YezdiColors.Muted) } },
    )
}

/** Lists nearby/paired Bluetooth devices; tapping one connects to it as an OBD adapter. */
@Composable
private fun AdapterPicker(app: YezdiApp, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { AdapterScanner(context) }
    var permitted by remember { mutableStateOf(Permissions.hasBluetooth(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = Permissions.hasBluetooth(context)
    }
    val found by scanner.found.collectAsStateWithLifecycle()
    val scanning by scanner.scanning.collectAsStateWithLifecycle()

    DisposableEffect(permitted) {
        if (!permitted) launcher.launch(Permissions.bluetooth) else scanner.start()
        onDispose { scanner.stop() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YezdiColors.Surface,
        title = { Text(stringResource(R.string.obd_pick_title)) },
        text = {
            Column {
                when {
                    !scanner.bluetoothAvailable -> Text(stringResource(R.string.obd_no_bluetooth))
                    !permitted -> Text(stringResource(R.string.obd_need_permission))
                    !scanner.bluetoothOn -> Text(stringResource(R.string.obd_bluetooth_off))
                    else -> {
                        Text(
                            stringResource(if (scanning) R.string.obd_scanning else R.string.obd_pick_hint),
                            style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted,
                        )
                        LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                            items(found, key = { it.device.address + it.ble }) { item ->
                                Column(
                                    Modifier.fillMaxWidth().clickable {
                                        scanner.stop()
                                        app.obd.connect(item.device, item.ble)
                                        onDismiss()
                                    }.padding(vertical = 10.dp),
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, color = if (item.likelyObd) YezdiColors.Accent else YezdiColors.Text)
                                    Text(
                                        (if (item.ble) "Bluetooth LE" else "Bluetooth (paired)") + " · " + item.device.address,
                                        style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = YezdiColors.Muted) } },
    )
}

@Composable
private fun DashTestCards(app: YezdiApp) {
    val lastSent by app.dash.lastSent.collectAsStateWithLifecycle()
    val raw by NavigationState.lastRaw.collectAsStateWithLifecycle()
    val alert by NavigationState.lastAlert.collectAsStateWithLifecycle()
    DebugCard(stringResource(R.string.dash_last_sent), lastSent ?: stringResource(R.string.nothing_yet))
    DebugCard(stringResource(R.string.maps_last_notification), raw ?: stringResource(R.string.nothing_yet))
    DebugCard(
        stringResource(R.string.last_alert),
        alert?.let { "${it.kind}: ${it.from} (${it.app})" } ?: stringResource(R.string.nothing_yet),
    )
}

@Composable
private fun DebugCard(title: String, body: String) {
    SurfaceCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodySmall, color = YezdiColors.Muted, fontFamily = FontFamily.Monospace)
    }
}
