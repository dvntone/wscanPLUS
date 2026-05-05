package com.wscanplus.app.nmea

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BleNmeaGattServer(
    private val context: Context,
    private val deviceName: String = DEFAULT_DEVICE_NAME,
) {
    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var isAdvertising: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    val subscriberCount: Int
        get() = subscribers.size

    private val subscribers = ConcurrentHashMap.newKeySet<BluetoothDevice>()
    private val mtuByAddress = ConcurrentHashMap<String, Int>()
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        if (!hasBlePermissions(context)) {
            lastError = "Missing BLE permissions"
            return
        }

        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
        if (manager == null || adapter == null || !adapter.isEnabled) {
            lastError = "Bluetooth unavailable"
            return
        }

        val service =
            BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val nmeaCharacteristic =
            BluetoothGattCharacteristic(
                NMEA_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            )
        val cccd =
            BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            )
        nmeaCharacteristic.addDescriptor(cccd)
        service.addCharacteristic(nmeaCharacteristic)

        val server = manager.openGattServer(context, callback)
        if (server == null) {
            lastError = "Unable to open GATT server"
            return
        }

        val advertisedName = adapter.name
        if (advertisedName != deviceName) {
            adapter.name = deviceName
        }

        if (!server.addService(service)) {
            server.close()
            lastError = "Unable to add BLE NMEA service"
            return
        }

        gattServer = server
        characteristic = nmeaCharacteristic
        advertiser = adapter.bluetoothLeAdvertiser
        isRunning = true
        lastError = null
        startAdvertising()
        Log.i(TAG, "BLE NMEA GATT server started")
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        isAdvertising = false
        isRunning = false
        subscribers.clear()
        mtuByAddress.clear()
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
        try {
            gattServer?.close()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to close BLE GATT server", error)
        }
        gattServer = null
        characteristic = null
    }

    @SuppressLint("MissingPermission")
    fun broadcast(line: String) {
        if (!isRunning || subscribers.isEmpty()) return
        val gatt = gattServer ?: return
        val target = characteristic ?: return
        subscribers.forEach { device ->
            val mtu = mtuByAddress[device.address] ?: DEFAULT_MTU
            chunkUtf8(line, mtu).forEach { payload ->
                target.value = payload
                gatt.notifyCharacteristicChanged(device, target, false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val bleAdvertiser = advertiser
        if (bleAdvertiser == null) {
            lastError = "BLE advertiser unavailable"
            return
        }

        val settings =
            AdvertiseSettings
                .Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()
        val data =
            AdvertiseData
                .Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        bleAdvertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                isAdvertising = true
                lastError = null
                Log.i(TAG, "BLE NMEA advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                isAdvertising = false
                lastError = "Advertise failure code=$errorCode"
                Log.e(TAG, "BLE NMEA advertising failed: $errorCode")
            }
        }

    private val callback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int,
            ) {
                if (newState != BluetoothGatt.STATE_CONNECTED) {
                    subscribers.remove(device)
                    mtuByAddress.remove(device.address)
                }
            }

            override fun onMtuChanged(
                device: BluetoothDevice,
                mtu: Int,
            ) {
                mtuByAddress[device.address] = mtu
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic,
            ) {
                val gatt = gattServer ?: return
                val value = characteristic.value ?: byteArrayOf()
                val response =
                    if (offset >= value.size) {
                        byteArrayOf()
                    } else {
                        value.copyOfRange(offset, value.size)
                    }
                gatt.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?,
            ) {
                val gatt = gattServer ?: return
                val descriptorValue = value ?: byteArrayOf()
                if (descriptor.uuid == CLIENT_CONFIG_UUID) {
                    when {
                        descriptorValue.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ->
                            subscribers += device
                        descriptorValue.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) ->
                            subscribers.remove(device)
                    }
                }
                if (responseNeeded) {
                    gatt.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptorValue)
                }
            }
        }

    companion object {
        private const val TAG = "BleNmeaGattServer"
        private const val DEFAULT_MTU = 23
        private const val DEFAULT_DEVICE_NAME = "wscan+ NMEA"
        val SERVICE_UUID: UUID = UUID.fromString("f1d5e3f4-2f95-4b30-9c94-0a50327dd201")
        val NMEA_CHARACTERISTIC_UUID: UUID = UUID.fromString("f1d5e3f4-2f95-4b30-9c94-0a50327dd202")
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        internal fun chunkUtf8(
            line: String,
            mtu: Int,
        ): List<ByteArray> {
            val bytes = line.toByteArray(StandardCharsets.UTF_8)
            val chunkSize = (mtu - 3).coerceAtLeast(1)
            if (bytes.isEmpty()) return listOf(byteArrayOf())

            val chunks = mutableListOf<ByteArray>()
            var index = 0
            while (index < bytes.size) {
                val end = (index + chunkSize).coerceAtMost(bytes.size)
                chunks += bytes.copyOfRange(index, end)
                index = end
            }
            return chunks
        }

        fun hasBlePermissions(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH,
                ) == PackageManager.PERMISSION_GRANTED
            }

            return listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ).all { permission ->
                ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
