package com.example.comp3606assignmentsolution.wifi_direct

import android.net.wifi.p2p.WifiP2pDevice

interface WifiDirectActionInterface {
    fun onIntentToConnect(device: WifiP2pDevice)
    fun onDisconnectClick()
    fun onDiscoverClick()
}