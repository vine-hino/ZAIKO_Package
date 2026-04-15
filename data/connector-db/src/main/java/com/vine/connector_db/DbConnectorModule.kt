package com.vine.connector_db

import com.vine.connector_api.InventoryGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DbConnectorModule {
    @Binds
    abstract fun bindInventoryGateway(
        impl: DbInventoryGateway,
    ): InventoryGateway
}