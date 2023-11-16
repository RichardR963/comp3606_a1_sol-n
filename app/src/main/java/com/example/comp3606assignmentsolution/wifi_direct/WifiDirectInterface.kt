package com.example.comp3606assignmentsolution.wifi_direct

import android.net.wifi.p2p.WifiP2pDevice

interface WifiDirectInterface {
    fun onWifiAdapterStateUpdated(adapterState: Boolean)
    fun onWfdConnectivityStateUpdated(connState: Boolean)
    fun onConnectivityInformationUpdated(ip: String, ssid: String, password: String, isGroupOwner: Boolean)
    fun onPeerListUpdated(wifiP2pDevices: MutableCollection<WifiP2pDevice>)
}