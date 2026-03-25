package com.evmap.serverapp.features.event.api

import com.evmap.serverapp.features.event.api.controller.EventController
import com.evmap.serverapp.features.event.api.dto.CreateEvent as CreateEventRequest
import com.evmap.serverapp.features.event.application.EventNotFoundException
import com.evmap.serverapp.features.event.application.command.AddComment
import com.evmap.serverapp.features.event.application.command.AddReaction
import com.evmap.serverapp.features.event.application.command.AddShare
import com.evmap.serverapp.features.event.application.command.CreateEvent as CreateEventCommand
import com.evmap.serverapp.features.event.application.command.DeleteEvent
import com.evmap.serverapp.features.event.application.command.RemoveShare
import com.evmap.serverapp.features.event.application.command.UpdateEvent
import com.evmap.serverapp.features.event.application.query.GetAllEventCategories
import com.evmap.serverapp.features.event.application.query.GetAllEventsByUser
import com.evmap.serverapp.features.event.application.query.GetCommentsByEventId
import com.evmap.serverapp.features.event.application.query.GetEventById
import com.evmap.serverapp.features.event.application.query.GetEventsLine
import com.evmap.serverapp.features.event.application.query.GetReactionsByEventId
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(controllers = [EventController::class])
@Import(EventApiExceptionHandler::class)
class EventControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var createEvent: CreateEventCommand

    @MockBean
    lateinit var getAllEventCategories: GetAllEventCategories

    @MockBean
    lateinit var getEventById: GetEventById

    @MockBean
    lateinit var getAllEventsByUser: GetAllEventsByUser

    @MockBean
    lateinit var getEventsLine: GetEventsLine

    @MockBean
    lateinit var getCommentsByEventId: GetCommentsByEventId

    @MockBean
    lateinit var getReactionsByEventId: GetReactionsByEventId

    @MockBean
    lateinit var updateEvent: UpdateEvent

    @MockBean
    lateinit var deleteEvent: DeleteEvent

    @MockBean
    lateinit var addComment: AddComment

    @MockBean
    lateinit var addReaction: AddReaction

    @MockBean
    lateinit var addShare: AddShare

    @MockBean
    lateinit var removeShare: RemoveShare

    @Test
    fun `create should return 201 for valid payload`() {
        val expected = CreateEventRequest(
            name = "Test event",
            description = "Test description",
            startsAt = Instant.parse("2026-03-22T10:00:00Z"),
            locationId = 1L,
            userId = 1L,
        )

        Mockito.`when`(createEvent.handle(expected)).thenReturn(123L)

        val body = """
            {
              "name": "Test event",
              "description": "Test description",
              "startsAt": "2026-03-22T10:00:00Z",
              "locationId": 1,
              "userId": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
                .with(user("test-user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `create should return 400 for blank name`() {
        val body = """
            {
              "name": "   ",
              "description": "Test description",
              "startsAt": "2026-03-22T10:00:00Z",
              "locationId": 1,
              "userId": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
                .with(user("test-user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `create should return 400 for invalid startsAt format`() {
        val body = """
            {
              "name": "Test event",
              "description": "Test description",
              "startsAt": "not-an-instant",
              "locationId": 1,
              "userId": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
                .with(user("test-user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `get should return 404 when event does not exist`() {
        Mockito.doThrow(EventNotFoundException(99L))
            .`when`(getEventById)
            .handle(99L)

        mockMvc.perform(get("/api/events/99").with(user("test-user")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event 99 not found"))
    }
}
