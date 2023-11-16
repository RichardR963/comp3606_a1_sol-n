package com.example.comp3606assignmentsolution

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.comp3606assignmentsolution.adapters.MarketplaceListAdapter
import com.example.comp3606assignmentsolution.fragments.WifiDirectInfoFragment
import com.example.comp3606assignmentsolution.models.MarketItem
import com.example.comp3606assignmentsolution.market.MarketInterface
import com.example.comp3606assignmentsolution.wifi_direct.WifiDirectActionInterface
import com.example.comp3606assignmentsolution.wifi_direct.WifiDirectInterface
import com.example.comp3606assignmentsolution.wifi_direct.WifiDirectManager


class MainActivity : AppCompatActivity(), WifiDirectInterface, WifiDirectActionInterface, MarketInterface {
    private val logTag = "ASSIGNMENT 1"

    /// The [requestCode] variable acts as an identifier for the app that's requesting the permissions.
    private val requestCode = 1234

    /// The [hasPermissions] variable acts as a flag indicating whether the app has the necessary permissions
    private var hasPermissions = false

    /// The [intentFilter] variable acts as an array of all the events that we want to listen to from the OS
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var wfdManager : WifiDirectManager? = null

    private val marketplaceListAdapter = MarketplaceListAdapter();


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // We need to determine if the app has the permissions to do what we want it to
        // when the activity is launched
        hasPermissions = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        if (SDK_INT >= 33){
            // If we're running on android SDK 33 or higher, we also need the NEARBY_WIFI_DEVICES permission
            hasPermissions = hasPermissions && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        }

        // If we do not have our permissions, we need to request them. Create an array of the permissions we want,
        // then send a request to the android OS
        if (!hasPermissions){
            var perm = arrayOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            if (SDK_INT >= 33){
                // Android 13 (API 33) requires the NEARBY_WIFI_DEVICES permission
                perm +=Manifest.permission.NEARBY_WIFI_DEVICES
            }

            ActivityCompat.requestPermissions(this, perm, requestCode)

        } else {
            setupWfdManager()
        }
        updateUI()

        // Set up my Recycler view
        val rv = findViewById<RecyclerView>(R.id.rv_marketplace)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = marketplaceListAdapter

    }


    /// This function is called by the OS itself after the user interacts with the permissions popups.
    /// We need to iterate through each of the permissions we requested and make sure that ALL are granted.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            this.requestCode -> {
                hasPermissions = true
                grantResults.forEach {
                    hasPermissions = hasPermissions && it==PackageManager.PERMISSION_GRANTED
                }
                if (hasPermissions){
                    setupWfdManager()

                    // If permissions was JUST granted, we need to manually register the intents for our app
                    // Since it would NOT have been caught by the onResume call
                    wfdManager?.also {
                        registerReceiver(it, intentFilter)
                    }
                }
                updateUI()
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    /// The onResume override ensures that our Wi-Fi Direct manager is able to receive the events listed in [intentFilter]
    /// when the app is running
    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        wfdManager?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }


    // This function updates the user interface based on the state of the application
    private fun updateUI() {
        val llPermissionDenied = findViewById<LinearLayout>(R.id.ll_permissions_denied)
        val llPermissionGranted = findViewById<LinearLayout>(R.id.ll_permissions_granted)

        llPermissionDenied.visibility = if (hasPermissions) View.GONE else View.VISIBLE
        llPermissionGranted.visibility = if (!hasPermissions) View.GONE else View.VISIBLE

    }

    fun openWfdDialog(view: View) {
        Log.e(logTag, "I clicked the Open WFD Dialog view $view")
        val fm: FragmentManager = supportFragmentManager

        val dialog = WifiDirectInfoFragment(this)
        dialog.show(fm, "WifiDirectFragment")

        wfdManager!!.addConnectivityInterface(dialog)

        fm.executePendingTransactions()

        dialog.dialog!!.setOnDismissListener {
            wfdManager!!.removeConnectivityInterface(dialog)
        }
        dialog.dialog!!.setOnCancelListener {
            wfdManager!!.removeConnectivityInterface(dialog)
        }
    }

    /// This function serves as a shortcut to the app's settings page.
    /// This was NOT required by the students. It's just here for completion’s sake
    fun openAppSettings(view: View) {
        Log.e(logTag, "I clicked the Open App settings view $view")

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
    }


    private fun setupWfdManager() {
        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)
        wfdManager!!.addConnectivityInterface(this)

    }

    override fun onWifiAdapterStateUpdated(adapterState: Boolean) {
        // Not implementing here
    }

    override fun onWfdConnectivityStateUpdated(connState: Boolean) {
        findViewById<LinearLayout>(R.id.ll_marketplace_entry).visibility = if (connState) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rv_marketplace).visibility = if (connState) View.VISIBLE else View.GONE
    }

    override fun onConnectivityInformationUpdated(ip: String, ssid: String, password: String, isGroupOwner: Boolean) {
        // Not implementing this here
    }

    override fun onPeerListUpdated(wifiP2pDevices: MutableCollection<WifiP2pDevice>) {
        // Not implementing this here
    }

    override fun onIntentToConnect(device: WifiP2pDevice) {
        wfdManager!!.connect(device)
    }

    override fun onDisconnectClick() {
        wfdManager!!.disconnect()
    }

    override fun onDiscoverClick() {
        wfdManager!!.discover()
    }

    fun listMarketplaceItem(view: View) {
        Log.e(logTag, "I clicked the button $view")

        val itemQuantity = findViewById<EditText>(R.id.et_item_quantity).text.toString()
        val sellerName = findViewById<EditText>(R.id.et_seller_name).text.toString()
        val itemName = findViewById<EditText>(R.id.et_item_name).text.toString()

        // Form validation. Should penalize?
        if (itemQuantity.isBlank() || sellerName.isBlank() || itemName.isBlank()) {
            Toast.makeText(applicationContext, "Please ensure you filled out all the fields", Toast.LENGTH_SHORT).show()
            return
        }
        wfdManager!!.addItem(MarketItem(
            sellerName, itemName, itemQuantity))
        Log.e(logTag, "I should list an item with the following details: (${itemQuantity}x) $itemName by $sellerName")

    }

    override fun onMarketplaceUpdated(item: MarketItem) {
        runOnUiThread{
            marketplaceListAdapter.updateList(item)
            findViewById<RecyclerView>(R.id.rv_marketplace).layoutManager?.scrollToPosition(0)

        }
    }
}