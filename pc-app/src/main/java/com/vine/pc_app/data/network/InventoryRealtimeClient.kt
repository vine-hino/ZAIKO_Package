package com.vine.pc_app.data.network

import com.vine.inventory_contract.RealtimeStockMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class InventoryRealtimeClient(
    private val client: HttpClient,
    private val wsUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun connect(onMessage: suspend (RealtimeStockMessage) -> Unit) {
        client.webSocket(urlString = wsUrl) {
            for (frame in incoming) {
                val textFrame = frame as? Frame.Text ?: continue
                val message = json.decodeFromString<RealtimeStockMessage>(textFrame.readText())
                onMessage(message)
            }
        }
    }
}