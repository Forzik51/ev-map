package com.evmap.serverapp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration

@ConfigurationProperties("app.security.password-reset")
data class PasswordResetProperties(
    val secret: String = "",
    val codeTtl: Duration = Duration.ofMinutes(10),
    val resetTokenTtl: Duration = Duration.ofMinutes(5),
    val resendCooldown: Duration = Duration.ofSeconds(60),
    val maxAttempts: Int = 5,
    val requestsPerEmailPerHour: Int = 5,
    val requestsPerIpPerHour: Int = 20,
    val mailMaxAttempts: Int = 3,
) {
    init {
        require(!codeTtl.isNegative && !codeTtl.isZero && !resetTokenTtl.isNegative && !resetTokenTtl.isZero)
        require(!resendCooldown.isNegative && maxAttempts > 0)
        require(requestsPerEmailPerHour > 0 && requestsPerIpPerHour > 0 && mailMaxAttempts > 0)
    }
}

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PasswordResetProperties::class)
class PasswordResetConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun transactionTemplate(manager: PlatformTransactionManager): TransactionTemplate = TransactionTemplate(manager)
}
