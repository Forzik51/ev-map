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
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

class EventReadRepositoryTest {

    @Test
    fun `findViewById should use id placeholder and map response`() {
        val provider = CapturingProvider { ctx ->
            if (ctx.sql().contains("FROM event", ignoreCase = true)) {
                arrayOf(eventRowResult(id = 42L, locationName = "Central Park"))
            } else {
                error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        val view = repository.findViewById(42L)

        assertEquals(42L, view.id)
        assertEquals("Central Park", view.locationName)
        assertEquals(7L, view.userId)
        assertEquals(2, view.repostsCount)
        assertEquals(3, view.ratingsCount)
        assertEquals(4, view.commentsCount)

        val executed = provider.executed.single()
        assertEquals(42L, executed.bindings().first())
        assertTrue(executed.sql().contains("WHERE e.id = ?"))
    }

    @Test
    fun `findViewById should throw EventNotFoundException when row is absent`() {
        val provider = CapturingProvider { ctx ->
            if (ctx.sql().contains("FROM event", ignoreCase = true)) {
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
                ctx.sql().contains("FROM event", ignoreCase = true) ->
                    arrayOf(eventsListResult(locationName = "City Hall"))
                else -> error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        val page = repository.findAllViews(PageRequest.of(0, 20))

        assertEquals(1, page.content.size)
        val item = page.content.first()
        assertEquals("City Hall", item.locationName)
        assertEquals(5, item.repostsCount)
        assertEquals(6, item.ratingsCount)
        assertEquals(7, item.commentsCount)
    }

    @Test
    fun `findCommentsByEventId should return pageable comments`() {
        val provider = CapturingProvider { ctx ->
            when {
                ctx.sql().contains("SELECT 1 FROM event", ignoreCase = true) -> arrayOf(existsResult())
                ctx.sql().contains("SELECT COUNT(*)", ignoreCase = true) && ctx.sql().contains("FROM comment", ignoreCase = true) ->
                    arrayOf(countResult(1L))
                ctx.sql().contains("FROM comment c", ignoreCase = true) -> arrayOf(commentListResult())
                else -> error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        val page = repository.findCommentsByEventId(10L, PageRequest.of(0, 20))

        assertEquals(1, page.content.size)
        val item = page.content.first()
        assertEquals(11L, item.id)
        assertEquals(10L, item.eventId)
        assertEquals("john", item.username)

        val commentsQuery = provider.executed.first { it.sql().contains("FROM comment c", ignoreCase = true) }
        assertEquals(10L, commentsQuery.bindings()[0])
    }

    @Test
    fun `findReactionsByEventId should return pageable reactions`() {
        val provider = CapturingProvider { ctx ->
            when {
                ctx.sql().contains("SELECT 1 FROM event", ignoreCase = true) -> arrayOf(existsResult())
                ctx.sql().contains("SELECT COUNT(*)", ignoreCase = true) && ctx.sql().contains("FROM rating", ignoreCase = true) ->
                    arrayOf(countResult(1L))
                ctx.sql().contains("FROM rating r", ignoreCase = true) -> arrayOf(reactionListResult())
                else -> error("Unexpected SQL: ${ctx.sql()}")
            }
        }

        val repository = EventReadRepository(mockDsl(provider))

        val page = repository.findReactionsByEventId(10L, PageRequest.of(0, 20))

        assertEquals(1, page.content.size)
        val item = page.content.first()
        assertEquals(10L, item.eventId)
        assertEquals(9L, item.userId)
        assertEquals(BigDecimal("4.5"), item.rating)
        assertEquals("anna", item.username)

        val reactionsQuery = provider.executed.first { it.sql().contains("FROM rating r", ignoreCase = true) }
        assertEquals(10L, reactionsQuery.bindings()[0])
    }

    private fun mockDsl(provider: MockDataProvider): DSLContext =
        DSL.using(MockConnection(provider), SQLDialect.POSTGRES)

    private fun eventRowResult(id: Long, locationName: String): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = eventFields()
        val result = dsl.newResult(
            fields.id,
            fields.name,
            fields.description,
            fields.startAt,
            fields.endAt,
            fields.userId,
            fields.locationName,
            fields.repostsCount,
            fields.ratingsCount,
            fields.commentsCount
        )
        val record = dsl.newRecord(
            fields.id,
            fields.name,
            fields.description,
            fields.startAt,
            fields.endAt,
            fields.userId,
            fields.locationName,
            fields.repostsCount,
            fields.ratingsCount,
            fields.commentsCount
        )

        record.set(fields.id, id)
        record.set(fields.name, "Event name")
        record.set(fields.description, "Event description")
        record.set(fields.startAt, Timestamp.from(Instant.parse("2026-03-22T10:00:00Z")))
        record.set(fields.endAt, Timestamp.from(Instant.parse("2026-03-22T12:00:00Z")))
        record.set(fields.userId, 7L)
        record.set(fields.locationName, locationName)
        record.set(fields.repostsCount, 2L)
        record.set(fields.ratingsCount, 3L)
        record.set(fields.commentsCount, 4L)

        result.add(record)
        return MockResult(1, result)
    }

    private fun emptyEventResult(): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = eventFields()
        val result = dsl.newResult(
            fields.id,
            fields.name,
            fields.description,
            fields.startAt,
            fields.endAt,
            fields.userId,
            fields.locationName,
            fields.repostsCount,
            fields.ratingsCount,
            fields.commentsCount
        )
        return MockResult(0, result)
    }

    private fun eventsListResult(locationName: String): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = eventFields()
        val result = dsl.newResult(
            fields.id,
            fields.name,
            fields.description,
            fields.startAt,
            fields.endAt,
            fields.userId,
            fields.locationName,
            fields.repostsCount,
            fields.ratingsCount,
            fields.commentsCount
        )
        val record = dsl.newRecord(
            fields.id,
            fields.name,
            fields.description,
            fields.startAt,
            fields.endAt,
            fields.userId,
            fields.locationName,
            fields.repostsCount,
            fields.ratingsCount,
            fields.commentsCount
        )

        record.set(fields.id, 1L)
        record.set(fields.name, "List event")
        record.set(fields.description, "List description")
        record.set(fields.startAt, Timestamp.from(Instant.parse("2026-03-22T10:00:00Z")))
        record.set(fields.endAt, null)
        record.set(fields.userId, 3L)
        record.set(fields.locationName, locationName)
        record.set(fields.repostsCount, 5L)
        record.set(fields.ratingsCount, 6L)
        record.set(fields.commentsCount, 7L)

        result.add(record)
        return MockResult(1, result)
    }

    private fun commentListResult(): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = commentFields()
        val result = dsl.newResult(
            fields.id,
            fields.text,
            fields.eventId,
            fields.parentCommentId,
            fields.commentAt,
            fields.userId,
            fields.username
        )
        val record = dsl.newRecord(
            fields.id,
            fields.text,
            fields.eventId,
            fields.parentCommentId,
            fields.commentAt,
            fields.userId,
            fields.username
        )

        record.set(fields.id, 11L)
        record.set(fields.text, "Comment text")
        record.set(fields.eventId, 10L)
        record.set(fields.parentCommentId, null)
        record.set(fields.commentAt, Timestamp.from(Instant.parse("2026-03-25T10:15:00Z")))
        record.set(fields.userId, 9L)
        record.set(fields.username, "john")

        result.add(record)
        return MockResult(1, result)
    }

    private fun reactionListResult(): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val fields = reactionFields()
        val result = dsl.newResult(
            fields.eventId,
            fields.userId,
            fields.rating,
            fields.username
        )
        val record = dsl.newRecord(
            fields.eventId,
            fields.userId,
            fields.rating,
            fields.username
        )

        record.set(fields.eventId, 10L)
        record.set(fields.userId, 9L)
        record.set(fields.rating, BigDecimal("4.5"))
        record.set(fields.username, "anna")

        result.add(record)
        return MockResult(1, result)
    }

    private fun existsResult(): MockResult {
        val dsl = DSL.using(SQLDialect.POSTGRES)
        val existsField = DSL.field("exists", Int::class.java)
        val result = dsl.newResult(existsField)
        val record = dsl.newRecord(existsField)
        record.set(existsField, 1)
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
        name = DSL.field("name", String::class.java),
        description = DSL.field("description", String::class.java),
        startAt = DSL.field("start_at", Timestamp::class.java),
        endAt = DSL.field("end_at", Timestamp::class.java),
        userId = DSL.field("user_id", Long::class.java),
        locationName = DSL.field("location_name", String::class.java),
        repostsCount = DSL.field("reposts_count", Long::class.java),
        ratingsCount = DSL.field("ratings_count", Long::class.java),
        commentsCount = DSL.field("comments_count", Long::class.java),
    )

    private fun commentFields(): CommentFields = CommentFields(
        id = DSL.field("id", Long::class.java),
        text = DSL.field("text", String::class.java),
        eventId = DSL.field("event_id", Long::class.java),
        parentCommentId = DSL.field("parent_comment_id", Long::class.java),
        commentAt = DSL.field("comment_at", Timestamp::class.java),
        userId = DSL.field("user_id", Long::class.java),
        username = DSL.field("username", String::class.java),
    )

    private fun reactionFields(): ReactionFields = ReactionFields(
        eventId = DSL.field("event_id", Long::class.java),
        userId = DSL.field("user_id", Long::class.java),
        rating = DSL.field("rating", BigDecimal::class.java),
        username = DSL.field("username", String::class.java),
    )

    private data class EventFields(
        val id: Field<Long>,
        val name: Field<String>,
        val description: Field<String>,
        val startAt: Field<Timestamp>,
        val endAt: Field<Timestamp>,
        val userId: Field<Long>,
        val locationName: Field<String>,
        val repostsCount: Field<Long>,
        val ratingsCount: Field<Long>,
        val commentsCount: Field<Long>,
    )

    private data class CommentFields(
        val id: Field<Long>,
        val text: Field<String>,
        val eventId: Field<Long>,
        val parentCommentId: Field<Long>,
        val commentAt: Field<Timestamp>,
        val userId: Field<Long>,
        val username: Field<String>,
    )

    private data class ReactionFields(
        val eventId: Field<Long>,
        val userId: Field<Long>,
        val rating: Field<BigDecimal>,
        val username: Field<String>,
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
