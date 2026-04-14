package com.vine.zaiko_package.di

import com.vine.connector_api.ConnectionSelector
import com.vine.connector_api.ConnectionType
import com.vine.connector_api.InboundRequest
import com.vine.connector_api.InventoryConnector
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.SubmitResult
import com.vine.connector_cloud.CloudInventoryConnector
import com.vine.connector_db.DirectDbInventoryConnector
import com.vine.connector_fake.FakeInventoryConnector
import com.vine.connector_ftp.FtpInventoryConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private class InMemoryConnectionSelector(
    initialType: ConnectionType = ConnectionType.FAKE,
) : ConnectionSelector {
    private var selectedType: ConnectionType = initialType

    override fun current(): ConnectionType = selectedType

    override fun change(type: ConnectionType) {
        selectedType = type
    }
}

private class SwitchableInventoryGateway(
    private val selector: ConnectionSelector,
    private val connectors: Map<ConnectionType, InventoryConnector>,
) : InventoryGateway {

    override fun currentConnectionType(): ConnectionType = selector.current()

    override suspend fun registerInbound(request: InboundRequest): SubmitResult {
        val connector = connectors[selector.current()]
            ?: return SubmitResult(
                accepted = false,
                message = "選択された接続先が見つかりません",
            )

        return connector.registerInbound(request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object InventoryGatewayModule {

    @Provides
    @Singleton
    fun provideConnectionSelector(): ConnectionSelector {
        return InMemoryConnectionSelector(
            initialType = ConnectionType.FAKE,
        )
    }

    @Provides
    @Singleton
    fun provideFtpInventoryConnector(): FtpInventoryConnector = FtpInventoryConnector()

    @Provides
    @Singleton
    fun provideDirectDbInventoryConnector(): DirectDbInventoryConnector = DirectDbInventoryConnector()

    @Provides
    @Singleton
    fun provideCloudInventoryConnector(): CloudInventoryConnector = CloudInventoryConnector()

    @Provides
    @Singleton
    fun provideInventoryGateway(
        selector: ConnectionSelector,
        fakeConnector: FakeInventoryConnector,
        ftpConnector: FtpInventoryConnector,
        directDbConnector: DirectDbInventoryConnector,
        cloudConnector: CloudInventoryConnector,
    ): InventoryGateway {
        val connectors = mapOf(
            ConnectionType.FAKE to fakeConnector,
            ConnectionType.FTP to ftpConnector,
            ConnectionType.DIRECT_DB to directDbConnector,
            ConnectionType.CLOUD to cloudConnector,
        )

        return SwitchableInventoryGateway(
            selector = selector,
            connectors = connectors,
        )
    }
}