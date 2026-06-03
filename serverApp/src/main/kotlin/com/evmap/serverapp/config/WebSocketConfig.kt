package com.evmap.serverapp.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtDecoder: JwtDecoder,
) : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = StompHeaderAccessor.wrap(message)
                if (accessor.command == StompCommand.CONNECT) {
                    val token = bearerToken(accessor)
                    if (!token.isNullOrBlank()) {
                        val jwt = jwtDecoder.decode(token)
                        accessor.user = UsernamePasswordAuthenticationToken(jwt, token, emptyList())
                    }
                }
                return message
            }
        })
    }

    private fun bearerToken(accessor: StompHeaderAccessor): String? {
        val authorization = accessor.getFirstNativeHeader("Authorization")
        if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ")) {
            return authorization.removePrefix("Bearer ").trim()
        }
        return accessor.getFirstNativeHeader("access_token")
    }
}
