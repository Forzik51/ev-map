package com.evmap.serverapp.config

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.DriverManager
import java.util.UUID

@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
class DeploymentMigrationTest {
    private val user = System.getenv("EVMAP_TEST_DB_USER") ?: "reset_test"
    private val password = System.getenv("EVMAP_TEST_DB_PASSWORD") ?: ""

    @Test
    fun `empty database starts at B12 and contains reference data only`() = withDatabase { url ->
        migrations(url, true).migrate()
        assertEquals("0", scalar(url, "SELECT count(*) FROM app_user"))
        assertEquals("0", scalar(url, "SELECT count(*) FROM event"))
        assertEquals("2", scalar(url, "SELECT count(*) FROM report_category WHERE is_selectable"))
        assertEquals("B12__schema.sql", scalar(url, "SELECT script FROM flyway_schema_history WHERE version = '12'"))
        assertEquals("13", migrations(url, true).info().current().version.version)
    }

    @Test
    fun `database with V12 history upgrades using V13 and keeps its data`() = withDatabase { url ->
        installLegacyExtensions(url)
        migrations(url, false, target = "12", allowLegacySeed = true).migrate()
        val before = scalar(url, "SELECT count(*) FROM app_user")
        assertTrue(before.toInt() > 0)
        migrations(url, true).migrate()
        assertEquals(before, scalar(url, "SELECT count(*) FROM app_user"))
        assertEquals("V12__report_moderation.sql", scalar(url, "SELECT script FROM flyway_schema_history WHERE version = '12'"))
        assertEquals("13", migrations(url, true).info().current().version.version)
    }

    @Test
    fun `V2 is blocked before its SQL executes on a database at V1`() = withDatabase { url ->
        installLegacyExtensions(url)
        migrations(url, false, target = "1").migrate()
        execute(url, "INSERT INTO chat(id, nazwa) VALUES (42, 'Keep this chat')")
        val error = assertThrows(FlywayException::class.java) { migrations(url, true).migrate() }
        assertTrue(generateSequence<Throwable>(error) { it.cause }.any { it.message.orEmpty().contains("V2 is a destructive demo seed") })
        assertEquals("Keep this chat", scalar(url, "SELECT nazwa FROM chat WHERE id = 42"))
        assertEquals("1", migrations(url, false).info().current().version.version)
    }

    @Test
    fun `explicit baseline at V2 lets a copy of a legacy database skip the seed and keep its data`() = withDatabase { url ->
        installLegacyExtensions(url)
        migrations(url, false, target = "1").migrate()
        execute(url, "INSERT INTO chat(id, nazwa) VALUES (42, 'Keep this chat')")
        execute(url, "CREATE SCHEMA evmap_migration_archive; ALTER TABLE flyway_schema_history SET SCHEMA evmap_migration_archive")
        Flyway.configure().dataSource(url, user, password).schemas("evmap").baselineVersion("2").load().baseline()
        migrations(url, true).migrate()
        assertEquals("Keep this chat", scalar(url, "SELECT name FROM chat WHERE id = 42"))
        assertEquals("13", migrations(url, true).info().current().version.version)
    }

    private fun migrations(url: String, baseline: Boolean, target: String = "latest", allowLegacySeed: Boolean = false): Flyway {
        val locations = if (baseline) arrayOf("classpath:db/migration", "classpath:db/baseline") else arrayOf("classpath:db/migration")
        return Flyway.configure().dataSource(url, user, password).schemas("evmap")
            .locations(*locations).target(target)
            .apply { if (!allowLegacySeed) callbacks(PreventDemoSeed()) }
            .load()
    }

    private fun scalar(url: String, query: String): String = DriverManager.getConnection(url, user, password).use { db ->
        db.createStatement().use { sql -> sql.executeQuery(query).use { result -> check(result.next()); result.getString(1) } }
    }

    private fun execute(url: String, query: String) = DriverManager.getConnection(url, user, password).use { db ->
        db.createStatement().use { it.execute(query) }
    }

    private fun installLegacyExtensions(url: String) {
        execute(url, "CREATE SCHEMA evmap; CREATE EXTENSION postgis WITH SCHEMA evmap; CREATE EXTENSION pg_trgm WITH SCHEMA evmap")
    }

    private fun withDatabase(test: (String) -> Unit) {
        val testUrl = checkNotNull(System.getenv("EVMAP_TEST_DB_URL")).substringBefore('?')
        check(Regex("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/evmap_password_reset_test$").matches(testUrl))
        val host = testUrl.substringBeforeLast('/')
        val database = "evmap_migration_${UUID.randomUUID().toString().replace("-", "")}"
        DriverManager.getConnection("$host/postgres", user, password).use { db ->
            db.createStatement().use { sql ->
                sql.execute("CREATE DATABASE $database")
                try {
                    test("$host/$database?currentSchema=evmap,public")
                } finally {
                    sql.execute("DROP DATABASE $database")
                }
            }
        }
    }
}
