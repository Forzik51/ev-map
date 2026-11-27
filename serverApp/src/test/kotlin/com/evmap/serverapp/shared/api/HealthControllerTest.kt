package com.evmap.serverapp.shared.api

import com.evmap.serverapp.config.SecurityConfig
import com.evmap.serverapp.shared.security.AuthVersionValidator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(HealthController::class)
@Import(SecurityConfig::class)
@TestPropertySource(properties = ["app.security.jwt.secret=test-health-secret-at-least-32-bytes-long"])
class HealthControllerTest {
    @Autowired lateinit var mvc: MockMvc
    @MockitoBean lateinit var jdbc: JdbcTemplate
    @MockitoBean lateinit var authVersionValidator: AuthVersionValidator

    @Test
    fun `readiness is available without authentication`() {
        `when`(jdbc.queryForObject("SELECT 1", Int::class.java)).thenReturn(1)
        mvc.perform(get("/health")).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun `database failure returns unavailable without exposing diagnostics`() {
        `when`(jdbc.queryForObject("SELECT 1", Int::class.java))
            .thenThrow(DataAccessResourceFailureException("private database details"))
        mvc.perform(get("/health")).andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.message").doesNotExist())
    }

    @Test
    fun `application APIs still require authentication`() {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized)
    }
}
