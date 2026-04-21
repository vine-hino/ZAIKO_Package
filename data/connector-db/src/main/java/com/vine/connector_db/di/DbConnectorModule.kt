package com.vine.connector_db.di

import com.vine.connector_api.HomeDashboardGateway
import com.vine.connector_api.InventoryGateway
import com.vine.connector_db.gateway.HybridInventoryGateway
import com.vine.connector_db.gateway.ServerHomeDashboardGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DbConnectorModule {
    @Binds
    abstract fun bindInventoryGateway(
        impl: HybridInventoryGateway,
    ): InventoryGateway

    @Binds
    abstract fun bindHomeDashboardGateway(
        impl: ServerHomeDashboardGateway,
    ): HomeDashboardGateway
}