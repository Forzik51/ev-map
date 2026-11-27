package com.evmap.serverapp.shared.api

import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(private val jdbc: JdbcTemplate) {
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> = try {
        check(jdbc.queryForObject("SELECT 1", Int::class.java) == 1)
        ResponseEntity.ok(mapOf("status" to "UP"))
    } catch (_: DataAccessException) {
        ResponseEntity.status(503).body(mapOf("status" to "DOWN"))
    }
}
