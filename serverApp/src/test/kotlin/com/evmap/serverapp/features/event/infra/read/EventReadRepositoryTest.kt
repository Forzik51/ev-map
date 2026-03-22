package com.evmap.serverapp.features.event.infra.read

import com.evmap.serverapp.features.event.application.EventNotFoundException
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockExecuteContext
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import java.sql.Timestamp
import java.time.Instant

class EventReadRepositoryTest {

    @Test
    fun `findViewById should use id placeholder and map response`() {
        val provider = CapturingProvider { ctx ->
            if (ctx.sql().contains("FROM Wydarzenie", ignoreCase = true)) {
                arrayOf(eventRowResult(id = 42L, locationName = "POINT(21.0122 52.2297)"))
            } else {
                error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        val view = repository.findViewById(42L)

        assertEquals(42L, view.id)
        assertEquals("POINT(21.0122 52.2297)", view.locationName)

        val executed = provider.executed.single()
        assertEquals(42L, executed.bindings().first())
        assertTrue(executed.sql().contains("WHERE w.id = ?"))
    }

    @Test
    fun `findViewById should throw EventNotFoundException when row is absent`() {
        val provider = CapturingProvider { ctx ->
            if (ctx.sql().contains("FROM Wydarzenie", ignoreCase = true)) {
                arrayOf(emptyEventResult())
            } else {
                error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        assertThrows(EventNotFoundException::class.java) {
            repository.findViewById(999L)
        }
    }

    @Test
    fun `findAllViews should map location_name alias`() {
        val provider = CapturingProvider { ctx ->
            when {
                ctx.sql().contains("SELECT COUNT(*)", ignoreCase = true) -> arrayOf(countResult(1L))
                ctx.sql().contains("FROM wydarzenie", ignoreCase = true) ->
                    arrayOf(eventsListResult(locationName = "Park"))
                else -> error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        val page = repository.findAllViews(PageRequest.of(0, 20))

        assertEquals(1, page.content.size)
        assertEquals("Park", page.content.first().locationName)
    }

    private fun mockDsl(provider: MockDataProvider): DSLContext =
        DSL.using(MockConnection(provider), SQLDialect.POSTGRES)

    private fun eventRowResult(id: Long, locationName: String): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = eventFields()
        val result = dsl.newResult(fields.id, fields.name, fields.description, fields.whenTs, fields.locationName)
        val record = dsl.newRecord(fields.id, fields.name, fields.description, fields.whenTs, fields.locationName)

        record.set(fields.id, id)
        record.set(fields.name, "Event name")
        record.set(fields.description, "Event description")
        record.set(fields.whenTs, Timestamp.from(Instant.parse("2026-03-22T10:00:00Z")))
        record.set(fields.locationName, locationName)

        result.add(record)
        return MockResult(1, result)
    }

    private fun emptyEventResult(): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = eventFields()
        val result = dsl.newResult(fields.id, fields.name, fields.description, fields.whenTs, fields.locationName)
        return MockResult(0, result)
    }

    private fun eventsListResult(locationName: String): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = eventFields()
        val result = dsl.newResult(fields.id, fields.name, fields.description, fields.whenTs, fields.locationName)
        val record = dsl.newRecord(fields.id, fields.name, fields.description, fields.whenTs, fields.locationName)

        record.set(fields.id, 1L)
        record.set(fields.name, "List event")
        record.set(fields.description, "List description")
        record.set(fields.whenTs, Timestamp.from(Instant.parse("2026-03-22T10:00:00Z")))
        record.set(fields.locationName, locationName)

        result.add(record)
        return MockResult(1, result)
    }

    private fun countResult(total: Long): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val countField = DSL.field("count", Long::class.java)
        val result = dsl.newResult(countField)
        val record = dsl.newRecord(countField)
        record.set(countField, total)
        result.add(record)
        return MockResult(1, result)
    }

    private fun eventFields(): EventFields = EventFields(
        id = DSL.field("id", Long::class.java),
        name = DSL.field("nazwa", String::class.java),
        description = DSL.field("opis", String::class.java),
        whenTs = DSL.field("kiedy", Timestamp::class.java),
        locationName = DSL.field("location_name", String::class.java)
    )

    private data class EventFields(
        val id: Field<Long>,
        val name: Field<String>,
        val description: Field<String>,
        val whenTs: Field<Timestamp>,
        val locationName: Field<String>
    )

    private class CapturingProvider(
        private val delegate: (MockExecuteContext) -> Array<MockResult>
    ) : MockDataProvider {

        val executed = mutableListOf<MockExecuteContext>()

        override fun execute(ctx: MockExecuteContext): Array<MockResult> {
            executed += ctx
            return delegate(ctx)
        }
    }
}
