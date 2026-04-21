package com.vine.connector_db.di

import com.vine.connector_api.MasterReferenceGateway
import com.vine.connector_db.gateway.ServerMasterReferenceGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object ServerSyncModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    @Provides
    @Named("serverBaseUrl")
    fun provideServerBaseUrl(): String {
        return "http://192.168.11.3:8080"
    }

    @Provides
    @Singleton
    fun provideMasterReferenceGateway(
        httpClient: HttpClient,
        @Named("serverBaseUrl") baseUrl: String,
    ): MasterReferenceGateway {
        return ServerMasterReferenceGateway(httpClient, baseUrl)
    }
}