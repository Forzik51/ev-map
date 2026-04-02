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
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
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

    @MockitoBean
    lateinit var createEvent: CreateEventCommand

    @MockitoBean
    lateinit var getAllEventCategories: GetAllEventCategories

    @MockitoBean
    lateinit var getEventById: GetEventById

    @MockitoBean
    lateinit var getAllEventsByUser: GetAllEventsByUser

    @MockitoBean
    lateinit var getEventsLine: GetEventsLine

    @MockitoBean
    lateinit var getCommentsByEventId: GetCommentsByEventId

    @MockitoBean
    lateinit var getReactionsByEventId: GetReactionsByEventId

    @MockitoBean
    lateinit var updateEvent: UpdateEvent

    @MockitoBean
    lateinit var deleteEvent: DeleteEvent

    @MockitoBean
    lateinit var addComment: AddComment

    @MockitoBean
    lateinit var addReaction: AddReaction

    @MockitoBean
    lateinit var addShare: AddShare

    @MockitoBean
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
