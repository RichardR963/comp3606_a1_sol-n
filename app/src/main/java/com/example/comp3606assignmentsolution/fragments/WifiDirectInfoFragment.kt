 package com.example.comp3606assignmentsolution.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.comp3606assignmentsolution.R
import com.example.comp3606assignmentsolution.adapters.PeerListAdapter
import com.example.comp3606assignmentsolution.wifi_direct.WifiDirectActionInterface
import com.example.comp3606assignmentsolution.wifi_direct.WifiDirectInterface

 class WifiDirectInfoFragment(
     private val actionInterface: WifiDirectActionInterface
 ) : DialogFragment(), WifiDirectInterface {
     private val logTag = "WifiDirectInfoFragment"
     private val rvAdapter = PeerListAdapter(actionInterface)
     private lateinit var view: View
     override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
         val builder = AlertDialog.Builder(requireActivity())
         val inflater = requireActivity().layoutInflater
         view = inflater.inflate(R.layout.fragment_wifi_direct_info, null)

         view.findViewById<Button>(R.id.btn_discover_peers).setOnClickListener {
             actionInterface.onDiscoverClick()
         }

         view.findViewById<Button>(R.id.btn_disconnect).setOnClickListener {
             actionInterface.onDisconnectClick()
         }

         val rvPeerList = view.findViewById<RecyclerView>(R.id.rv_peerList)
         rvPeerList.layoutManager = LinearLayoutManager(context)
         rvPeerList.adapter = rvAdapter

         builder.setView(view)
             .setTitle("Wifi Direct Information")
             .setPositiveButton("OK") { dialog, _ ->
                 dialog.dismiss()
             }
             .setNegativeButton("Cancel") { dialog, _ ->
                 dialog.cancel()
             }

         return builder.create()
     }

     override fun onWifiAdapterStateUpdated(adapterState: Boolean) {
         view.findViewById<LinearLayout>(R.id.ll_wifi_off).visibility = if (!adapterState) View.VISIBLE else View.GONE
         view.findViewById<RelativeLayout>(R.id.rl_wifi_on).visibility = if (adapterState) View.VISIBLE else View.GONE
     }

     override fun onWfdConnectivityStateUpdated(connState: Boolean) {
        Log.e(logTag, "WiFi Direct state updated: $connState")
         if (this::view.isInitialized){
             view.findViewById<LinearLayout>(R.id.ll_wfd_disconnected).visibility = if (!connState) View.VISIBLE else View.GONE
             view.findViewById<LinearLayout>(R.id.ll_wfd_connected).visibility = if (connState) View.VISIBLE else View.GONE
         }

     }

     @SuppressLint("SetTextI18n")
     override fun onConnectivityInformationUpdated(ip: String, ssid: String, password: String, isGroupOwner: Boolean) {
         Log.e(logTag, "Information Updated: $ip, $ssid, $password, $isGroupOwner")
         view.findViewById<TextView>(R.id.tv_device_ip).text = "My IP Address: $ip"
         view.findViewById<TextView>(R.id.tv_network_ssid).text = "Network SSID: $ssid"
         view.findViewById<TextView>(R.id.tv_network_password).text = "Network password: $password"
         view.findViewById<TextView>(R.id.tv_go_status).text = "Group Owner: ${if (isGroupOwner) "True" else "False"}"
     }

     override fun onPeerListUpdated(wifiP2pDevices: MutableCollection<WifiP2pDevice>) {
         Log.e(logTag, "PeerList Updated $wifiP2pDevices")
         rvAdapter.updateList(wifiP2pDevices)
     }

 }