package com.example.comp3606assignmentsolution.models

import org.json.JSONObject

class MarketItem (val sellerName: String, val itemName: String, val itemQuantity: String) {
    fun toJson(): String{
        return JSONObject(mapOf(
            "sellerName" to this.sellerName,
            "itemName" to this.itemName,
            "itemQuantity" to this.itemQuantity
        )).toString()
    }
}