package com.vine.pc_app.data

import com.vine.pc_app.data.network.InventoryMovementClient
import com.vine.pc_app.data.network.InventoryRealtimeClient
import com.vine.pc_data_postgres.InboundJsonImporter
import com.vine.pc_data_postgres.InboundRepository
import com.vine.pc_data_postgres.OutboundJsonImporter
import com.vine.pc_data_postgres.OutboundRepository
import com.vine.pc_data_postgres.PgConfig
import com.vine.pc_data_postgres.PostgresDataSourceFactory
import com.vine.pc_data_postgres.PostgresInboundRepository
import com.vine.pc_data_postgres.PostgresOutboundRepository
import com.vine.pc_data_postgres.PostgresStocktakeRepository
import com.vine.pc_data_postgres.StocktakeJsonImporter
import com.vine.pc_data_postgres.StocktakeRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.vine.pc_app.data.network.StocktakeServerClient
import com.vine.pc_app.data.network.StockBalanceClient
import com.vine.pc_app.data.network.MasterLookupClient
import com.vine.pc_data_postgres.MasterRepository
import com.vine.pc_data_postgres.PostgresMasterRepository

object PcDependencies {
    private val config = PgConfig(
        host = "localhost",
        port = 5432,
        database = "zaiko_pc",
        user = "postgres",
        password = "Tetu1109Tetu",
    )

    private val dataSource by lazy {
        PostgresDataSourceFactory.create(config)
    }

    val stocktakeRepository: StocktakeRepository by lazy {
        val repository = PostgresStocktakeRepository(dataSource)
        repository.bootstrap()
        repository
    }

    val stocktakeJsonImporter: StocktakeJsonImporter by lazy {
        stocktakeRepository.bootstrap()
        StocktakeJsonImporter(dataSource)
    }

    val inboundRepository: InboundRepository by lazy {
        val repository = PostgresInboundRepository(dataSource)
        repository.bootstrap()
        repository
    }

    val inboundJsonImporter: InboundJsonImporter by lazy {
        inboundRepository.bootstrap()
        InboundJsonImporter(dataSource)
    }

    val outboundRepository: OutboundRepository by lazy {
        val repository = PostgresOutboundRepository(dataSource)
        repository.bootstrap()
        repository
    }

    val outboundJsonImporter: OutboundJsonImporter by lazy {
        outboundRepository.bootstrap()
        OutboundJsonImporter(dataSource)
    }

    private val serverJson by lazy {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }

    private val serverHttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(serverJson)
            }
            install(WebSockets)
        }
    }

    val inventoryMovementClient: InventoryMovementClient by lazy {
        InventoryMovementClient(
            client = serverHttpClient,
            baseUrl = "http://127.0.0.1:8080",
        )
    }

    val inventoryRealtimeClient: InventoryRealtimeClient by lazy {
        InventoryRealtimeClient(
            client = serverHttpClient,
            wsUrl = "ws://127.0.0.1:8080/ws/inventory",
            json = serverJson,
        )
    }

    val stocktakeServerClient: StocktakeServerClient by lazy {
        StocktakeServerClient(
            client = serverHttpClient,
            baseUrl = "http://127.0.0.1:8080",
        )
    }

    val stockBalanceClient: StockBalanceClient by lazy {
        StockBalanceClient(
            client = serverHttpClient,
            baseUrl = "http://127.0.0.1:8080",
        )
    }

    val masterLookupClient: MasterLookupClient by lazy {
        MasterLookupClient(
            client = serverHttpClient,
            baseUrl = "http://127.0.0.1:8080",
        )
    }

    val masterRepository: MasterRepository by lazy {
        val repository = PostgresMasterRepository(dataSource)
        repository.bootstrap()
        repository
    }
}