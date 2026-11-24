package com.evmap.serverapp.config

import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.callback.Context
import org.flywaydb.core.api.callback.Event
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Base64

@Configuration(proxyBeanMethods = false)
@Profile("prod")
class ProductionConfig(
    @Value("\${app.security.jwt.secret}") jwtSecret: String,
    passwordReset: PasswordResetProperties,
) {
    init {
        require(jwtSecret.isNotBlank() && jwtSecret.toByteArray(Charsets.UTF_8).size >= 32 &&
            jwtSecret != "dev-only-secret-key-change-me-please-32"
        ) { "Set APP_JWT_SECRET to a production secret of at least 32 bytes." }
        val resetKey = runCatching { Base64.getDecoder().decode(passwordReset.secret) }.getOrNull()
        require(resetKey?.size == 32 && passwordReset.secret != jwtSecret) {
            "Set APP_PASSWORD_RESET_SECRET to a separate Base64-encoded 32-byte random key."
        }
    }
}

@Configuration(proxyBeanMethods = false)
@Profile("prod | !test-legacy")
class MigrationSafetyConfig {
    @Bean
    fun preventDemoSeed(): Callback = PreventDemoSeed()
}

internal class PreventDemoSeed : Callback {
    override fun supports(event: Event, context: Context?): Boolean = event == Event.BEFORE_EACH_MIGRATE
    override fun canHandleInTransaction(event: Event, context: Context?): Boolean = true
    override fun getCallbackName(): String = "Prevent destructive legacy demo seed"

    override fun handle(event: Event, context: Context) {
        check(context.migrationInfo?.script != "V2__seed_sample_data_dev.sql") {
            "V2 is a destructive demo seed. Upgrade this legacy database on a copy using the documented migration procedure."
        }
    }
}
