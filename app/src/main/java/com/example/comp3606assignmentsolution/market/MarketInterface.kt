package com.example.comp3606assignmentsolution.market

import com.example.comp3606assignmentsolution.models.MarketItem

interface MarketInterface {
    fun onMarketplaceUpdated(item: MarketItem)
}