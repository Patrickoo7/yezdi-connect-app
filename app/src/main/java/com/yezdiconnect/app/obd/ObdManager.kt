package com.yezdiconnect.app.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ObdState {
    data object Idle : ObdState
    data class Connecting(val adapterName: String, val step: String) : ObdState
    data class Connected(val adapterName: String, val info: ObdInfo, val vin: String?) : ObdState
    data class Failed(val adapterName: String, val message: String) : ObdState
}

/** Latest readings plus a running fuel estimate for this connection. */
data class ObdLive(
    val values: Map<ObdPid, Double>,
    val batteryVolts: Double?,
    val fuelRate: FuelRate?,
    val tripKm: Double,
    val tripLitres: Double,
) {
    /** Estimated km/l since connecting; null until enough fuel has been used to be meaningful. */
    val tripKmPerLitre: Double? get() = if (tripLitres >= 0.02 && tripKm >= 0.5) tripKm / tripLitres else null
}

sealed interface FaultCodes {
    data object NotRead : FaultCodes
    data object Reading : FaultCodes
    data class Read(val codes: List<Dtc>, val at: Long) : FaultCodes
    data class Failed(val message: String) : FaultCodes
}

data class SavedAdapter(val address: String, val name: String, val ble: Boolean)

/**
 * Owns the connection to an OBD adapter. Read-only: polls engine values, reads fault codes and the
 * VIN; never clears codes or writes to the bike.
 */
class ObdManager(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<ObdState>(ObdState.Idle)
    val state: StateFlow<ObdState> = _state.asStateFlow()

    private val _live = MutableStateFlow<ObdLive?>(null)
    val live: StateFlow<ObdLive?> = _live.asStateFlow()

    private val _faultCodes = MutableStateFlow<FaultCodes>(FaultCodes.NotRead)
    val faultCodes: StateFlow<FaultCodes> = _faultCodes.asStateFlow()

    private val prefs = context.getSharedPreferences("obd", Context.MODE_PRIVATE)
    private var job: Job? = null
    @Volatile private var session: ElmSession? = null
    private var lastAttempt: Pair<BluetoothDevice, Boolean>? = null

    val savedAdapter: SavedAdapter?
        get() {
            val address = prefs.getString("address", null) ?: return null
            return SavedAdapter(address, prefs.getString("name", null) ?: address, prefs.getBoolean("ble", true))
        }

    @SuppressLint("MissingPermission") // the UI asks for Bluetooth permissions before connecting
    fun connect(device: BluetoothDevice, ble: Boolean) {
        disconnect()
        lastAttempt = device to ble
        val name = runCatching { device.name }.getOrNull() ?: device.address
        job = scope.launch {
            _state.value = ObdState.Connecting(name, "Connecting to the adapter…")
            var transport: ElmTransport? = null
            try {
                transport = if (ble) BleElmTransport.connect(context, device) else ClassicElmTransport.connect(device)
                val s = ElmSession(transport)
                _state.value = ObdState.Connecting(name, "Talking to the bike…")
                val info = s.initialize()
                val vin = runCatching { s.readVin() }.getOrNull()
                prefs.edit().putString("address", device.address).putString("name", name).putBoolean("ble", ble).apply()
                session = s
                _faultCodes.value = FaultCodes.NotRead
                _state.value = ObdState.Connected(name, info, vin)
                poll(s, info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ObdState.Failed(name, e.message ?: "Connection failed.")
            } finally {
                session = null
                transport?.close()
                _live.value = null
            }
        }
    }

    /** Reconnects to the last adapter that worked. */
    fun reconnect() {
        val saved = savedAdapter ?: return
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        connect(adapter.getRemoteDevice(saved.address), saved.ble)
    }

    /** Tries the last adapter again (the one that just failed), or the saved one. */
    fun retry() {
        lastAttempt?.let { (device, ble) -> connect(device, ble) } ?: reconnect()
    }

    fun disconnect() {
        job?.cancel()
        job = null
        _state.value = ObdState.Idle
    }

    fun readFaultCodes() {
        val s = session ?: return
        scope.launch {
            _faultCodes.value = FaultCodes.Reading
            _faultCodes.value = try {
                FaultCodes.Read(s.readFaultCodes(), System.currentTimeMillis())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FaultCodes.Failed(e.message ?: "Couldn't read fault codes.")
            }
        }
    }

    private suspend fun poll(s: ElmSession, info: ObdInfo) {
        val fast = listOf(ObdPid.RPM, ObdPid.SPEED, ObdPid.THROTTLE, ObdPid.INTAKE_PRESSURE, ObdPid.MAF, ObdPid.INTAKE_AIR_TEMP)
            .filter { it.pid in info.supportedPids }
        val slow = listOf(ObdPid.COOLANT_TEMP, ObdPid.ENGINE_LOAD, ObdPid.OIL_TEMP, ObdPid.FUEL_LEVEL, ObdPid.MODULE_VOLTAGE)
            .filter { it.pid in info.supportedPids }
        val values = mutableMapOf<ObdPid, Double>()
        var battery: Double? = null
        var tripKm = 0.0
        var tripLitres = 0.0
        var cycle = 0
        var last = SystemClock.elapsedRealtime()

        while (currentCoroutineContext().isActive) {
            val slowTurn = cycle % SLOW_EVERY == 0
            for (pid in if (slowTurn) fast + slow else fast) s.read(pid)?.let { values[pid] = it }
            if (slowTurn) battery = s.batteryVoltage() ?: battery

            val now = SystemClock.elapsedRealtime()
            val seconds = (now - last) / 1000.0
            last = now
            val speed = values[ObdPid.SPEED] ?: 0.0
            val rate = estimateFuel(values, speed)
            if (cycle > 0 && seconds in 0.0..MAX_GAP_S) {
                tripKm += speed * seconds / 3600
                if (rate != null) tripLitres += rate.litresPerHour * seconds / 3600
            }
            _live.value = ObdLive(values.toMap(), battery, rate, tripKm, tripLitres)
            cycle++
            delay(POLL_PAUSE_MS)
        }
    }

    companion object {
        private const val SLOW_EVERY = 5
        private const val POLL_PAUSE_MS = 150L
        private const val MAX_GAP_S = 10.0

        /** MAF if the bike has the sensor, otherwise speed-density from MAP + intake temp + RPM. */
        fun estimateFuel(values: Map<ObdPid, Double>, speedKmh: Double): FuelRate? {
            values[ObdPid.MAF]?.let { return FuelEconomy.fromMaf(it, speedKmh) }
            val map = values[ObdPid.INTAKE_PRESSURE] ?: return null
            val iat = values[ObdPid.INTAKE_AIR_TEMP] ?: return null
            val rpm = values[ObdPid.RPM] ?: return null
            return FuelEconomy.fromSpeedDensity(map, iat, rpm, speedKmh)
        }
    }

    /** For screenshot tests: show a connected state with sample readings. */
    internal fun showSample(state: ObdState, live: ObdLive?, codes: FaultCodes) {
        _state.value = state
        _live.value = live
        _faultCodes.value = codes
    }
}
