package com.yezdiconnect.app.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

private const val PROMPT = '>'

/**
 * ELM327 over Bluetooth LE. Adapters don't agree on UUIDs (FFF0/FFF1/FFF2, FFE0/FFE1, vendor ones),
 * so after discovery this picks, in a non-standard service, one characteristic that can notify
 * (adapter → phone) and one that can be written (phone → adapter); they may be the same one.
 */
@SuppressLint("MissingPermission") // callers check BLUETOOTH_CONNECT first
class BleElmTransport private constructor(
    private val gatt: BluetoothGatt,
    private val callback: Callback,
    private val write: BluetoothGattCharacteristic,
) : ElmTransport {

    private val lock = Mutex()

    override suspend fun request(command: String, timeoutMs: Long): String = lock.withLock {
        callback.clearBuffer()
        val bytes = (command + "\r").toByteArray(Charsets.US_ASCII)
        bytes.toList().chunked(20).forEach { chunk -> callback.write(gatt, write, chunk.toByteArray()) }
        withTimeoutOrNull(timeoutMs) { callback.awaitPrompt() } ?: throw ObdException("The adapter didn't answer \"$command\" in time.")
    }

    override fun close() {
        gatt.disconnect()
        gatt.close()
    }

    internal class Callback : BluetoothGattCallback() {
        val connected = CompletableDeferred<Boolean>()
        val discovered = CompletableDeferred<Boolean>()
        val mtuChanged = CompletableDeferred<Unit>()
        private var descriptorWritten = CompletableDeferred<Boolean>()
        private var written = CompletableDeferred<Boolean>()
        private val buffer = StringBuilder()
        private val promptSignal = Channel<Unit>(Channel.CONFLATED)
        @Volatile var lost = false

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connected.complete(true)
            } else {
                lost = true
                connected.complete(false)
                promptSignal.trySend(Unit)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuChanged.complete(Unit)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            discovered.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            descriptorWritten.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            written.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        // Android 13+
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            received(value)
        }

        // Android 12 and older (e.g. Galaxy M31)
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            received(characteristic.value ?: return)
        }

        private fun received(value: ByteArray) {
            val text = String(value, Charsets.US_ASCII)
            synchronized(buffer) { buffer.append(text) }
            if (PROMPT in text) promptSignal.trySend(Unit)
        }

        fun clearBuffer() {
            synchronized(buffer) { buffer.setLength(0) }
            promptSignal.tryReceive()
        }

        suspend fun awaitPrompt(): String {
            while (true) {
                synchronized(buffer) { if (PROMPT in buffer) return buffer.substring(0, buffer.indexOf(PROMPT)) }
                if (lost) throw ObdException("The adapter disconnected.")
                promptSignal.receive()
            }
        }

        @Suppress("DEPRECATION")
        suspend fun write(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic, data: ByteArray) {
            val type = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            written = CompletableDeferred()
            val started = if (Build.VERSION.SDK_INT >= 33) {
                gatt.writeCharacteristic(ch, data, type) == BluetoothStatusCodes.SUCCESS
            } else {
                ch.writeType = type
                ch.value = data
                gatt.writeCharacteristic(ch)
            }
            if (!started) throw ObdException("Couldn't send to the adapter.")
            withTimeout(2_000) { written.await() }
        }

        @Suppress("DEPRECATION")
        suspend fun enableNotifications(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(ch, true)
            val cccd = ch.getDescriptor(CCCD) ?: return
            val value = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            descriptorWritten = CompletableDeferred()
            val started = if (Build.VERSION.SDK_INT >= 33) {
                gatt.writeDescriptor(cccd, value) == BluetoothStatusCodes.SUCCESS
            } else {
                cccd.value = value
                gatt.writeDescriptor(cccd)
            }
            if (started) withTimeout(3_000) { descriptorWritten.await() }
        }
    }

    companion object {
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        /** Generic Access, Generic Attribute, Device Information, Battery: never the data channel. */
        private val standardServices = setOf("1800", "1801", "180a", "180f")

        suspend fun connect(context: Context, device: BluetoothDevice): BleElmTransport {
            val cb = Callback()
            val gatt = device.connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE)
                ?: throw ObdException("Couldn't start a Bluetooth connection.")
            try {
                if (withTimeoutOrNull(15_000) { cb.connected.await() } != true) throw ObdException("Couldn't connect to the adapter. Is it plugged in and powered?")
                // One GATT operation at a time: wait for the MTU reply (or give up) before discovery.
                if (gatt.requestMtu(185)) withTimeoutOrNull(3_000) { cb.mtuChanged.await() }
                if (withTimeoutOrNull(10_000) { gatt.discoverServices(); cb.discovered.await() } != true) {
                    throw ObdException("Connected, but the adapter didn't list its services.")
                }
                val (notify, write) = pickCharacteristics(gatt) ?: throw ObdException("This Bluetooth device doesn't look like an OBD adapter.")
                cb.enableNotifications(gatt, notify)
                return BleElmTransport(gatt, cb, write)
            } catch (e: Throwable) {
                gatt.disconnect()
                gatt.close()
                throw e
            }
        }

        private fun pickCharacteristics(gatt: BluetoothGatt): Pair<BluetoothGattCharacteristic, BluetoothGattCharacteristic>? {
            val candidates = gatt.services.filter { s ->
                val short = s.uuid.toString().substring(4, 8)
                !(s.uuid.toString().endsWith("-0000-1000-8000-00805f9b34fb") && short in standardServices)
            }
            for (service in candidates) {
                val chars = service.characteristics
                val notify = chars.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                }
                val write = chars.firstOrNull {
                    it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                }
                if (notify != null && write != null) return notify to write
            }
            return null
        }
    }
}

/** ELM327 over classic Bluetooth (serial port profile), used by many older adapters. */
@SuppressLint("MissingPermission")
class ClassicElmTransport private constructor(private val socket: BluetoothSocket) : ElmTransport {
    private val lock = Mutex()
    private val input = socket.inputStream
    private val output = socket.outputStream

    override suspend fun request(command: String, timeoutMs: Long): String = lock.withLock {
        withContext(Dispatchers.IO) {
            while (input.available() > 0) input.read() // drop leftovers
            output.write((command + "\r").toByteArray(Charsets.US_ASCII))
            output.flush()
            val sb = StringBuilder()
            val deadline = System.currentTimeMillis() + timeoutMs
            while (true) {
                if (input.available() > 0) {
                    val c = input.read()
                    if (c < 0) throw ObdException("The adapter disconnected.")
                    if (c.toChar() == PROMPT) break
                    sb.append(c.toChar())
                } else {
                    if (System.currentTimeMillis() > deadline) throw ObdException("The adapter didn't answer \"$command\" in time.")
                    Thread.sleep(10)
                }
            }
            sb.toString()
        }
    }

    override fun close() {
        runCatching { socket.close() }
    }

    companion object {
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        suspend fun connect(device: BluetoothDevice): ClassicElmTransport = withContext(Dispatchers.IO) {
            val socket = device.createRfcommSocketToServiceRecord(SPP)
            try {
                socket.connect()
            } catch (e: Exception) {
                runCatching { socket.close() }
                throw ObdException("Couldn't connect to the adapter. Pair it in Android's Bluetooth settings first (PIN is usually 1234 or 0000).")
            }
            ClassicElmTransport(socket)
        }
    }
}
