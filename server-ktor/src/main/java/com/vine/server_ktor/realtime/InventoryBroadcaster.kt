package com.vine.server_ktor.realtime

import com.vine.inventory_contract.RealtimeStockMessage
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class InventoryBroadcaster(
    private val json: Json,
) {
    private val sessions = ConcurrentHashMap.newKeySet<DefaultWebSocketServerSession>()

    suspend fun handleSession(session: DefaultWebSocketServerSession) {
        sessions.add(session)

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    if (text == "ping") {
                        session.send(Frame.Text("pong"))
                    }
                }
            }
        } finally {
            sessions.remove(session)
        }
    }

    suspend fun broadcast(message: RealtimeStockMessage) {
        val serialized = json.encodeToString(message)
        val disconnected = mutableListOf<DefaultWebSocketServerSession>()

        sessions.forEach { session ->
            try {
                session.send(Frame.Text(serialized))
            } catch (_: Throwable) {
                disconnected.add(session)
            }
        }

        disconnected.forEach { sessions.remove(it) }
    }
}