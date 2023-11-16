package com.example.comp3606assignmentsolution.wifi_direct

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.util.Log
import com.example.comp3606assignmentsolution.market.MarketInterface
import com.example.comp3606assignmentsolution.models.MarketItem
import com.example.comp3606assignmentsolution.network.NetworkManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class WifiDirectManager(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val marketInterface: MarketInterface
): BroadcastReceiver(), ActionListener{
    private val logTag = "WFD Manager"
    private var connectivityInterface : MutableList<WifiDirectInterface> = mutableListOf()
    private var deviceInfo : WifiP2pInfo? = null
    private val networkManager: NetworkManager = NetworkManager(marketInterface)
    private val marketItems = mutableListOf<MarketItem>()

    init {
        requestDeviceInfo()
    }

    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                /// For the students, updating the user interface to tell the user that their Wi-Fi is off was NOT necessary
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                connectivityInterface.forEach {connInterface ->
                    connInterface.onWifiAdapterStateUpdated(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    peers?.deviceList?.let { peerList ->
                        connectivityInterface.forEach { connInterface ->
                            connInterface.onPeerListUpdated(peerList)
                        }
                    }
                    Log.e(logTag,"Peer List Updated: $peers")
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                requestDeviceInfo()
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's Wi-Fi state changing wasn't necessary in this assignment. This is usually
                // used to update the device's state again
            }
        }
    }

    private fun requestDeviceInfo() {
        manager.requestConnectionInfo(channel) {
            deviceInfo = it

            connectivityInterface.forEach {connInterface ->
                connInterface.onWfdConnectivityStateUpdated(it.groupFormed)
            }

            if (it.groupFormed){
                val wfdIp = getWFDIpAddress()
                networkManager.initialize(wfdIp)
                manager.requestGroupInfo(channel) { groupInfo ->
                    if (!groupInfo.isGroupOwner){
                        networkManager.sendToGroupOwner("hello")
                    }

                    connectivityInterface.forEach { connInterface ->
                        connInterface.onConnectivityInformationUpdated(
                            wfdIp,
                            groupInfo.networkName,
                            groupInfo.passphrase,
                            groupInfo.isGroupOwner
                        )
                    }
                }
            } else {
                networkManager.cleanup()
            }
        }
    }

    override fun onSuccess() {
        Log.e(logTag, "The call was a success")
    }

    override fun onFailure(p0: Int) {
        Log.e(logTag, "The call was NOT successful. Error code $p0")
    }


    fun addConnectivityInterface(interfaceInstance: WifiDirectInterface){
        // Add the new interface to the list for future updates
        connectivityInterface.add(interfaceInstance)

        // When a new interface is set, update its connectivity state
        requestDeviceInfo()
    }

    fun removeConnectivityInterface(interfaceInstance: WifiDirectInterface){
        connectivityInterface.remove(interfaceInstance)
    }

    // This is a function to get the Wi-Fi Direct ipv4 address
    private fun getWFDIpAddress(): String{
        try {
            val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (ntwInterface in interfaces) {
                if (ntwInterface.name.startsWith("p2p")) {
                    val addresses: List<InetAddress> = Collections.list(ntwInterface.inetAddresses)
                    for (address in addresses){
                        if (!address.isLoopbackAddress) {
                            val hostAddress : String? = address.hostAddress
                            if (hostAddress != null && hostAddress.indexOf(':')<0) {//get ipv4
                                return hostAddress
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(logTag, "error in getting Wi-Fi Direct IP address")
        }
        return "Unknown IP Address"
    }

    fun disconnect() {
        manager.removeGroup(channel, this)
        networkManager.cleanup()
    }

    fun connect(device: WifiP2pDevice){
        val config = WifiP2pConfig().apply {
            this.deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel,config, this)
    }

    fun discover() {
        manager.discoverPeers(channel, this)
    }

    fun addItem(marketItem: MarketItem) {
        if (deviceInfo!= null){
            if (deviceInfo!!.isGroupOwner){
                marketItems.add(marketItem)
                marketInterface.onMarketplaceUpdated(marketItem)
                networkManager.sendToAllPeers(marketItem.toJson())
            } else {
                networkManager.sendToGroupOwner(marketItem.toJson())
            }
        }
    }
}