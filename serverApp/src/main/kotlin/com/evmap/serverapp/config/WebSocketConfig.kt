package com.evmap.serverapp.config

import com.evmap.serverapp.features.auth.infra.AuthRepository
import com.evmap.serverapp.shared.security.WebSocketSessions

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.WebSocketHandlerDecorator
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtDecoder: JwtDecoder,
    private val sessions: WebSocketSessions,
    private val users: AuthRepository,
) : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.setPreserveReceiveOrder(true)
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
                if (accessor.command in setOf(StompCommand.DISCONNECT, StompCommand.UNSUBSCRIBE) || accessor.command == null) return message
                try {
                    val token = if (accessor.command == StompCommand.CONNECT) bearerToken(accessor) else sessions.token(accessor.sessionId)
                    if (token.isNullOrBlank()) throw AccessDeniedException("Authentication required")
                    val jwt = jwtDecoder.decode(token)
                    val userId = jwt.subject.toLongOrNull() ?: throw AccessDeniedException("Invalid user")
                    if (accessor.command == StompCommand.CONNECT) {
                        sessions.authenticated(requireNotNull(accessor.sessionId), userId, token)
                        // Close the decode/register race with a concurrent password reset.
                        jwtDecoder.decode(token)
                        accessor.user = UsernamePasswordAuthenticationToken(jwt, token, emptyList())
                    } else if (accessor.command == StompCommand.SUBSCRIBE || accessor.command == StompCommand.SEND) {
                        val prefix = if (accessor.command == StompCommand.SUBSCRIBE) "topic" else "app"
                        val chatId = chatId(accessor.destination, prefix)
                        if (!users.isChatMember(chatId, userId)) throw AccessDeniedException("Chat access denied")
                    } else throw AccessDeniedException("Command not allowed")
                } catch (ex: Exception) {
                    sessions.revoke(accessor.sessionId)
                    throw ex
                }
                return message
            }
        })
    }

    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration.addDecoratorFactory { handler ->
            object : WebSocketHandlerDecorator(handler) {
                override fun afterConnectionEstablished(session: WebSocketSession) {
                    sessions.opened(session)
                    super.afterConnectionEstablished(session)
                }
                override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
                    sessions.closed(session.id)
                    super.afterConnectionClosed(session, closeStatus)
                }
            }
        }
    }

    override fun configureClientOutboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
                val accessor = StompHeaderAccessor.wrap(message)
                if (accessor.messageType != SimpMessageType.MESSAGE) return message
                return try {
                    val jwt = jwtDecoder.decode(sessions.token(accessor.sessionId) ?: throw AccessDeniedException("Session expired"))
                    val userId = jwt.subject.toLongOrNull() ?: throw AccessDeniedException("Invalid user")
                    if (!users.isChatMember(chatId(accessor.destination, "topic"), userId)) throw AccessDeniedException("Chat access denied")
                    message
                } catch (ex: Exception) {
                    sessions.revoke(accessor.sessionId)
                    null
                }
            }
        })
    }

    private fun chatId(destination: String?, prefix: String): Long =
        Regex("^/$prefix/chats/([0-9]+)/(messages|typing|read)$").matchEntire(destination.orEmpty())
            ?.groupValues?.get(1)?.toLongOrNull() ?: throw AccessDeniedException("Destination not allowed")

    private fun bearerToken(accessor: StompHeaderAccessor): String? {
        val authorization = accessor.getFirstNativeHeader("Authorization")
        if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ")) {
            return authorization.removePrefix("Bearer ").trim()
        }
        return accessor.getFirstNativeHeader("access_token")
    }
}
