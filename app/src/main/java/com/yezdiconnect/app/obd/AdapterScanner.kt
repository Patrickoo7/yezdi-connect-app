package com.yezdiconnect.app.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FoundAdapter(val device: BluetoothDevice, val name: String, val ble: Boolean, val likelyObd: Boolean)

/**
 * Lists Bluetooth devices that could be OBD adapters: already-paired classic devices plus a
 * Bluetooth LE scan. Devices whose name looks like an OBD adapter are listed first.
 */
@SuppressLint("MissingPermission") // the UI asks for Bluetooth permissions before scanning
class AdapterScanner(context: Context) {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter

    private val _found = MutableStateFlow<List<FoundAdapter>>(emptyList())
    val found: StateFlow<List<FoundAdapter>> = _found.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    val bluetoothAvailable: Boolean get() = adapter != null
    val bluetoothOn: Boolean get() = adapter?.isEnabled == true

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull() ?: return
            add(FoundAdapter(result.device, name, ble = true, likelyObd = looksLikeObd(name)))
        }
    }

    fun start() {
        val a = adapter ?: return
        _found.value = emptyList()
        a.bondedDevices.orEmpty()
            .filter { it.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || it.type == BluetoothDevice.DEVICE_TYPE_DUAL }
            .forEach { d ->
                val name = d.name ?: d.address
                add(FoundAdapter(d, name, ble = false, likelyObd = looksLikeObd(name)))
            }
        val scanner = a.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(null, settings, callback) }.onSuccess { _scanning.value = true }
    }

    fun stop() {
        if (_scanning.value) runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        _scanning.value = false
    }

    private fun add(item: FoundAdapter) {
        val list = _found.value
        if (list.any { it.device.address == item.device.address && it.ble == item.ble }) return
        _found.value = (list + item).sortedWith(compareByDescending<FoundAdapter> { it.likelyObd }.thenBy { it.name.lowercase() })
    }

    companion object {
        private val obdNames = listOf("OBD", "ELM", "VLINK", "V-LINK", "VGATE", "ICAR", "VEEPEAK", "KONNWEI", "BAFX", "CARISTA", "VIECAR", "STN")

        fun looksLikeObd(name: String): Boolean = name.uppercase().let { n -> obdNames.any { it in n } }
    }
}
