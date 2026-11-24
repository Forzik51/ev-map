package com.evmap.serverapp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import com.evmap.serverapp.features.auth.PasswordResetIntegrationTest
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
class ServerAppApplicationTests {
    companion object {
        @JvmStatic @DynamicPropertySource
        fun database(properties: DynamicPropertyRegistry) = PasswordResetIntegrationTest.database(properties)
    }

    @Test
    fun contextLoads() {
    }

}
