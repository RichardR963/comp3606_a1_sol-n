package com.example.comp3606assignmentsolution.network
import android.util.Log
import com.example.comp3606assignmentsolution.market.MarketInterface
import com.example.comp3606assignmentsolution.models.MarketItem
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class NetworkManager(private val marketInterface: MarketInterface ) {
    private val logTag = "NetworkManager"
    private var socket: DatagramSocket? = null
    private val peerIpAddresses = mutableListOf<InetAddress>()
    private var listenThread: Thread
    private val port = 9835

    init {
        listenThread = Thread {
            listen()
        }
    }

    fun initialize(wfdIp: String) {
        cleanup()

        if (socket == null){
            socket = DatagramSocket(port, InetAddress.getByName(wfdIp))
            socket!!.broadcast = true
            socket!!.reuseAddress = true

            listenThread = Thread {
                listen()
            }
            listenThread.start()
        }
    }


    private fun processPacket(packet: DatagramPacket) {
        // I need to add the IP address of the sender to my peer list if it's not there
        if (!peerIpAddresses.contains(packet.address)){
            peerIpAddresses.add(packet.address)
        }

        val packetAsString = String(packet.data, 0, packet.length)
        if (packetAsString.contains("itemName")){
            val obj = JSONObject(packetAsString)
            marketInterface.onMarketplaceUpdated(
                MarketItem(
                    obj.getString("sellerName"),
                    obj.getString("itemName"),
                    obj.getString("itemQuantity")
                    )
            )
        }
        Log.e(logTag, packetAsString)
    }

    private fun listen(){
        while (socket != null && !socket!!.isClosed){
            try{
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet)
                if (packet.address != null){
                    processPacket(packet)
                }
            }catch (e: Exception) {
                Log.e(logTag, "Error occurred: $e")
                e.printStackTrace()
            }
        }
    }

    fun sendToAllPeers(item: String){
        Thread {
            if (socket != null){
                val itemData = item.toByteArray()
                peerIpAddresses.forEach{
                    val packet = DatagramPacket(itemData, itemData.size, it, port)
                    socket!!.send(packet)
                }
            }
        }.start()
    }

    fun sendToGroupOwner(item: String){
        Thread {
            if (socket != null){
                val itemData = item.toByteArray()
                val packet = DatagramPacket(itemData, itemData.size, InetAddress.getByName("192.168.49.1"), port)
                socket!!.send(packet)
            }
        }.start()
    }

    fun cleanup() {
        socket?.close()
        socket = null
        listenThread.interrupt()
    }
}