package com.evmap.serverapp.features.auth.application

import com.evmap.serverapp.features.auth.infra.AuthUser
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class JwtTokenService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${app.security.jwt.issuer:evmap}") private val issuer: String,
    @Value("\${app.security.jwt.access-token-minutes:15}") private val accessTokenMinutes: Long,
) {
    val accessTokenTtl: Duration = Duration.ofMinutes(accessTokenMinutes)

    fun createAccessToken(user: AuthUser): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenTtl))
            .subject(user.id.toString())
            .claim("userId", user.id)
            .claim("username", user.username)
            .claim("role", user.role)
            .build()
        val headers = JwsHeader.with(MacAlgorithm.HS256).build()

        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }
}
