package com.evmap.serverapp.shared.security

import com.evmap.serverapp.features.auth.application.PasswordChangedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketSessions {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val identities = ConcurrentHashMap<String, Pair<Long, String>>()

    fun opened(session: WebSocketSession) { sessions[session.id] = session }
    fun closed(sessionId: String) { sessions.remove(sessionId); identities.remove(sessionId) }
    fun authenticated(sessionId: String, userId: Long, token: String) { identities[sessionId] = userId to token }
    fun token(sessionId: String?): String? = sessionId?.let { identities[it]?.second }

    fun revoke(sessionId: String?) {
        if (sessionId == null) return
        identities.remove(sessionId)
        val session = sessions.remove(sessionId)
        runCatching { session?.close(CloseStatus.POLICY_VIOLATION) }
    }

    @TransactionalEventListener
    fun passwordChanged(event: PasswordChangedEvent) {
        identities.entries.filter { it.value.first == event.userId }.forEach { revoke(it.key) }
    }
}
