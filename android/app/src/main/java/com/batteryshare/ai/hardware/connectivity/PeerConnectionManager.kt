package com.batteryshare.ai.hardware.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import com.batteryshare.ai.domain.model.BatteryTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

enum class PeerTransport {
    BLUETOOTH,
    WIFI_DIRECT
}

enum class AuthorizationState {
    IDLE,
    AWAITING_REMOTE_CONSENT,
    AWAITING_LOCAL_CONSENT,
    AUTHORIZED,
    REJECTED
}

data class DiscoveredPeer(
    val id: String,
    val name: String,
    val transport: PeerTransport,
    val isPairedOrAvailable: Boolean
)

data class RemotePeerTelemetry(
    val deviceName: String,
    val batteryPct: Int,
    val temperatureCelsius: Float?,
    val supportsUsbOtg: Boolean,
    val supportsReverseWirelessHint: Boolean,
    val syncedAtMs: Long
)

data class PeerConnectionState(
    val bluetoothSupported: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val wifiDirectSupported: Boolean = false,
    val isScanning: Boolean = false,
    val peers: List<DiscoveredPeer> = emptyList(),
    val selectedPeer: DiscoveredPeer? = null,
    val authorizationState: AuthorizationState = AuthorizationState.IDLE,
    val remoteTelemetry: RemotePeerTelemetry? = null,
    val networkStatusMessage: String = "Aucun appareil appairé. La connexion réseau synchronise uniquement la télémétrie (aucun transfert électrique sans fil réseau).",
    val isElectricalTransfer: Boolean = false
)

class PeerConnectionManager(private val context: Context) {

    companion object {
        private val APP_BT_UUID: UUID = UUID.fromString("8a1e7c30-4f2b-4d91-9b7a-1000battery1")
        private const val SERVICE_NAME = "BatteryShareAI_Sync"
        private const val WIFI_P2P_PORT = 8988
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val wifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var wifiP2pChannel: WifiP2pManager.Channel? = null

    private var btServerSocket: BluetoothServerSocket? = null
    private var activeBtSocket: BluetoothSocket? = null
    private var wifiServerSocket: ServerSocket? = null

    private val _state = MutableStateFlow(
        PeerConnectionState(
            bluetoothSupported = bluetoothAdapter != null,
            bluetoothEnabled = bluetoothAdapter?.isEnabled == true,
            wifiDirectSupported = wifiP2pManager != null
        )
    )
    val state: StateFlow<PeerConnectionState> = _state.asStateFlow()

    private var receiversRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    if (device != null) {
                        val peerName = runCatching { device.name }.getOrNull() ?: "Appareil Bluetooth"
                        addOrUpdatePeer(
                            DiscoveredPeer(
                                id = device.address,
                                name = peerName,
                                transport = PeerTransport.BLUETOOTH,
                                isPairedOrAvailable = device.bondState == BluetoothDevice.BOND_BONDED
                            )
                        )
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _state.update { it.copy(isScanning = false) }
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestWifiPeers()
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    wifiP2pChannel?.let { channel ->
                        wifiP2pManager?.requestConnectionInfo(channel) { info ->
                            handleWifiConnectionInfo(info)
                        }
                    }
                }
            }
        }
    }

    fun initialize() {
        if (wifiP2pManager != null && wifiP2pChannel == null) {
            wifiP2pChannel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null)
        }
        if (!receiversRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(discoveryReceiver, filter)
            }
            receiversRegistered = true
        }
        refreshAdapterState()
    }

    fun refreshAdapterState() {
        _state.update {
            it.copy(
                bluetoothSupported = bluetoothAdapter != null,
                bluetoothEnabled = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false),
                wifiDirectSupported = wifiP2pManager != null
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(localTelemetry: BatteryTelemetry) {
        refreshAdapterState()
        _state.update {
            it.copy(
                isScanning = true,
                networkStatusMessage = "Recherche Bluetooth et Wi-Fi Direct en cours (synchronisation données uniquement)..."
            )
        }

        // Appareils Bluetooth déjà appairés
        runCatching {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                addOrUpdatePeer(
                    DiscoveredPeer(
                        id = device.address,
                        name = device.name ?: device.address,
                        transport = PeerTransport.BLUETOOTH,
                        isPairedOrAvailable = true
                    )
                )
            }
            if (bluetoothAdapter?.isEnabled == true) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
                bluetoothAdapter.startDiscovery()
            }
        }

        // Serveur RFCOMM d'écoute
        startBluetoothServer(localTelemetry)

        // Découverte Wi-Fi Direct
        wifiP2pChannel?.let { channel ->
            runCatching {
                wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _state.update {
                            it.copy(networkStatusMessage = "Découverte Wi-Fi Direct et Bluetooth active.")
                        }
                    }

                    override fun onFailure(reason: Int) {
                        _state.update {
                            it.copy(
                                isScanning = false,
                                networkStatusMessage = "Wi-Fi Direct indisponible (code $reason). Vérifiez le Wi-Fi et les autorisations."
                            )
                        }
                    }
                })
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestWifiPeers() {
        val channel = wifiP2pChannel ?: return
        runCatching {
            wifiP2pManager?.requestPeers(channel) { peers: WifiP2pDeviceList ->
                peers.deviceList.forEach { device: WifiP2pDevice ->
                    addOrUpdatePeer(
                        DiscoveredPeer(
                            id = device.deviceAddress,
                            name = device.deviceName.ifBlank { device.deviceAddress },
                            transport = PeerTransport.WIFI_DIRECT,
                            isPairedOrAvailable = device.status == WifiP2pDevice.AVAILABLE ||
                                device.status == WifiP2pDevice.CONNECTED
                        )
                    )
                }
            }
        }
    }

    fun selectAndRequestAuthorization(peer: DiscoveredPeer, localTelemetry: BatteryTelemetry) {
        _state.update {
            it.copy(
                selectedPeer = peer,
                authorizationState = AuthorizationState.AWAITING_LOCAL_CONSENT,
                networkStatusMessage = "En attente d'autorisation mutuelle avec ${peer.name}..."
            )
        }
        if (peer.transport == PeerTransport.WIFI_DIRECT) {
            val channel = wifiP2pChannel ?: return
            val config = WifiP2pConfig().apply { deviceAddress = peer.id }
            runCatching {
                wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _state.update {
                            it.copy(
                                authorizationState = AuthorizationState.AWAITING_REMOTE_CONSENT,
                                networkStatusMessage = "Lien Wi-Fi Direct initié avec ${peer.name}. Validez l'autorisation sur les deux téléphones."
                            )
                        }
                    }

                    override fun onFailure(reason: Int) {
                        _state.update {
                            it.copy(
                                authorizationState = AuthorizationState.REJECTED,
                                networkStatusMessage = "Échec de connexion Wi-Fi Direct (code $reason)."
                            )
                        }
                    }
                })
            }
        } else {
            connectBluetoothClient(peer, localTelemetry)
        }
    }

    fun approveLocalAuthorization(localTelemetry: BatteryTelemetry) {
        val selected = _state.value.selectedPeer ?: return
        _state.update {
            it.copy(
                authorizationState = AuthorizationState.AUTHORIZED,
                networkStatusMessage = "Appairage autorisé avec ${selected.name}. Synchronisation télémétrie active (aucun flux électrique via Wi-Fi/BT)."
            )
        }
        sendTelemetryPayloadOverActiveSocket(localTelemetry)
    }

    fun rejectAuthorization() {
        _state.update {
            it.copy(
                authorizationState = AuthorizationState.REJECTED,
                networkStatusMessage = "Connexion refusée par l'utilisateur."
            )
        }
        closeSockets()
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothServer(localTelemetry: BatteryTelemetry) {
        if (bluetoothAdapter?.isEnabled != true || btServerSocket != null) return
        scope.launch {
            runCatching {
                btServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, APP_BT_UUID)
                val socket = btServerSocket?.accept() ?: return@runCatching
                activeBtSocket = socket
                readAndSendBluetoothTelemetry(socket, localTelemetry)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetoothClient(peer: DiscoveredPeer, localTelemetry: BatteryTelemetry) {
        val adapter = bluetoothAdapter ?: return
        scope.launch {
            runCatching {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
                val device = adapter.getRemoteDevice(peer.id)
                val socket = device.createRfcommSocketToServiceRecord(APP_BT_UUID)
                socket.connect()
                activeBtSocket = socket
                readAndSendBluetoothTelemetry(socket, localTelemetry)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        networkStatusMessage = "Socket Bluetooth en attente de BatteryShare AI sur ${peer.name} (${err.localizedMessage ?: "non joignable"})."
                    )
                }
            }
        }
    }

    private fun readAndSendBluetoothTelemetry(socket: BluetoothSocket, localTelemetry: BatteryTelemetry) {
        scope.launch {
            runCatching {
                val writer = PrintWriter(socket.outputStream, true)
                writer.println(serializeTelemetryJson(localTelemetry))
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                val line = reader.readLine()
                if (!line.isNullOrBlank()) {
                    parseRemoteTelemetry(line)
                }
            }
        }
    }

    private fun handleWifiConnectionInfo(info: WifiP2pInfo) {
        if (!info.groupFormed) return
        scope.launch {
            if (info.isGroupOwner) {
                runCatching {
                    wifiServerSocket?.close()
                    wifiServerSocket = ServerSocket(WIFI_P2P_PORT)
                    val client = wifiServerSocket?.accept() ?: return@runCatching
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val line = reader.readLine()
                    if (!line.isNullOrBlank()) parseRemoteTelemetry(line)
                    client.close()
                }
            } else {
                val host = info.groupOwnerAddress?.hostAddress ?: return@launch
                runCatching {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, WIFI_P2P_PORT), 5000)
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println(serializeTelemetryJson(BatteryTelemetry()))
                    }
                }
            }
        }
    }

    private fun sendTelemetryPayloadOverActiveSocket(localTelemetry: BatteryTelemetry) {
        val socket = activeBtSocket ?: return
        scope.launch {
            runCatching {
                val writer = PrintWriter(socket.outputStream, true)
                writer.println(serializeTelemetryJson(localTelemetry))
            }
        }
    }

    private fun serializeTelemetryJson(telemetry: BatteryTelemetry): String {
        return JSONObject().apply {
            put("deviceName", Build.MODEL ?: "Android")
            put("batteryPct", telemetry.levelPct)
            put("tempC", telemetry.temperatureCelsius?.toDouble() ?: JSONObject.NULL)
            put("usbOtg", context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_USB_HOST))
            put("reverseQiHint", false)
            put("syncedAtMs", System.currentTimeMillis())
        }.toString()
    }

    private fun parseRemoteTelemetry(rawJson: String) {
        runCatching {
            val json = JSONObject(rawJson)
            val remote = RemotePeerTelemetry(
                deviceName = json.optString("deviceName", "Pair"),
                batteryPct = json.optInt("batteryPct", 0),
                temperatureCelsius = if (json.isNull("tempC")) null else json.optDouble("tempC").toFloat(),
                supportsUsbOtg = json.optBoolean("usbOtg", false),
                supportsReverseWirelessHint = json.optBoolean("reverseQiHint", false),
                syncedAtMs = json.optLong("syncedAtMs", System.currentTimeMillis())
            )
            _state.update {
                it.copy(
                    remoteTelemetry = remote,
                    authorizationState = AuthorizationState.AUTHORIZED,
                    networkStatusMessage = "Télémétrie synchronisée avec ${remote.deviceName} (${remote.batteryPct}%)."
                )
            }
        }
    }

    private fun addOrUpdatePeer(peer: DiscoveredPeer) {
        _state.update { current ->
            val filtered = current.peers.filterNot { it.id == peer.id && it.transport == peer.transport }
            current.copy(peers = filtered + peer)
        }
    }

    private fun closeSockets() {
        runCatching { activeBtSocket?.close() }
        runCatching { btServerSocket?.close() }
        runCatching { wifiServerSocket?.close() }
        activeBtSocket = null
        btServerSocket = null
        wifiServerSocket = null
    }

    fun release() {
        closeSockets()
        if (receiversRegistered) {
            runCatching { context.unregisterReceiver(discoveryReceiver) }
            receiversRegistered = false
        }
    }
}
