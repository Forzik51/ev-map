package com.evmap.serverapp.features.report

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.DriverManager
import java.sql.Statement
import java.util.UUID

@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
class ReportModerationMigrationTest {
    @Test
    fun `V12 recovers known statuses and original categories where V11 legacy data permits it`() {
        val baseUrl = requireNotNull(System.getenv("EVMAP_TEST_DB_URL")).substringBefore('?')
        require(Regex("^jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/evmap_password_reset_test$").matches(baseUrl))
        val schema = "report_migration_" + UUID.randomUUID().toString().replace("-", "")
        val url = "$baseUrl?currentSchema=$schema,evmap,public"
        val user = System.getenv("EVMAP_TEST_DB_USER") ?: "reset_test"
        val password = System.getenv("EVMAP_TEST_DB_PASSWORD") ?: ""
        try {
            Flyway.configure().dataSource(url, user, password).schemas(schema).target("11").load().migrate()
            DriverManager.getConnection(url, user, password).use { db ->
                db.createStatement().use { sql ->
                    sql.executeUpdate("INSERT INTO report_category(report_type) VALUES (' oPeN '), ('Resolved'), ('Other reason')")
                    sql.executeUpdate("UPDATE report SET category_id = (SELECT id FROM report_category WHERE report_type = 'Resolved') WHERE id = 1")
                    sql.executeUpdate(
                        """
                        INSERT INTO report(description, event_id, category_id, user_id)
                        SELECT 'imported ' || c.report_type, r.event_id, c.id, r.user_id
                        FROM report r CROSS JOIN report_category c
                        WHERE r.id = 1 AND c.report_type IN (' oPeN ', 'Resolved', 'Other reason')
                        """.trimIndent(),
                    )
                }
            }
            Flyway.configure().dataSource(url, user, password).schemas(schema).load().migrate()
            DriverManager.getConnection(url, user, password).use { db ->
                db.createStatement().use { sql ->
                    sql.executeQuery("SELECT status, category_id FROM report WHERE id = 1").use {
                        assertTrue(it.next())
                        assertEquals("RESOLVED", it.getString("status"))
                        assertEquals(2L, it.getLong("category_id"))
                    }
                    sql.executeQuery("SELECT outcome, decided_at, description, moderator_id, blocked_until FROM decision WHERE report_id = 1").use {
                        assertTrue(it.next())
                        assertNull(it.getString("outcome"))
                        assertNull(it.getObject("decided_at"))
                        assertTrue(it.getString("description").isNotBlank())
                        assertEquals(5L, it.getLong("moderator_id"))
                        assertTrue(it.getObject("blocked_until") != null)
                    }
                    sql.assertReport("imported  oPeN ", "OPEN", "Category not recovered", false)
                    sql.assertReport("imported Resolved", "RESOLVED", "Category not recovered", false)
                    sql.assertReport("imported Other reason", "OPEN", "Other reason", true)
                    sql.executeQuery(
                        "SELECT is_selectable FROM report_category WHERE report_type IN (' oPeN ', 'Resolved')",
                    ).use {
                        assertTrue(it.next())
                        assertFalse(it.getBoolean(1))
                        assertTrue(it.next())
                        assertFalse(it.getBoolean(1))
                    }
                }
            }
        } finally {
            DriverManager.getConnection(baseUrl, user, password).use { db ->
                db.createStatement().use { sql -> sql.execute("DROP SCHEMA IF EXISTS $schema CASCADE") }
            }
        }
    }

    private fun Statement.assertReport(description: String, status: String, category: String, selectable: Boolean) {
        connection.prepareStatement(
            """
            SELECT r.status, c.report_type, c.is_selectable
            FROM report r JOIN report_category c ON c.id = r.category_id
            WHERE r.description = ?
            """.trimIndent(),
        ).use { query ->
            query.setString(1, description)
            query.executeQuery().use {
                assertTrue(it.next())
                assertEquals(status, it.getString(1))
                assertEquals(category, it.getString(2))
                assertEquals(selectable, it.getBoolean(3))
            }
        }
    }
}
