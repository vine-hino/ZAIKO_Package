package com.vine.pc_app

import com.vine.pc_data_postgres.InboundJsonImporter
import com.vine.pc_data_postgres.InboundRepository
import com.vine.pc_data_postgres.PgConfig
import com.vine.pc_data_postgres.PostgresDataSourceFactory
import com.vine.pc_data_postgres.PostgresInboundRepository
import com.vine.pc_data_postgres.PostgresStocktakeRepository
import com.vine.pc_data_postgres.StocktakeJsonImporter
import com.vine.pc_data_postgres.StocktakeRepository
import com.vine.pc_data_postgres.OutboundJsonImporter
import com.vine.pc_data_postgres.OutboundRepository
import com.vine.pc_data_postgres.PostgresOutboundRepository

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
}