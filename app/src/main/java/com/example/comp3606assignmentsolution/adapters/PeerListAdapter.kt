package com.example.comp3606assignmentsolution.adapters

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.comp3606assignmentsolution.R
import com.example.comp3606assignmentsolution.wifi_direct.WifiDirectActionInterface

class PeerListAdapter (private val actionInterface: WifiDirectActionInterface): RecyclerView.Adapter<PeerListAdapter.ViewHolder>() {
    private val adapterDataset = mutableListOf<WifiP2pDevice>()
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_device_name)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_device_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.peer_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return adapterDataset.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = adapterDataset[position]
        holder.nameTextView.text = device.deviceName
        holder.addressTextView.text = device.deviceAddress
        holder.itemView.setOnClickListener {
            actionInterface.onIntentToConnect(device)
        }
    }

    fun updateList(newList: MutableCollection<WifiP2pDevice>){
        adapterDataset.clear()
        adapterDataset.addAll(newList)
        notifyDataSetChanged()
    }
}