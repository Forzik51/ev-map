package com.evmap.serverapp.features.event.api

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.application.EventNotFoundException
import com.evmap.serverapp.features.event.application.command.CreateEvent
import com.evmap.serverapp.features.event.application.query.GetAllEventsByUser
import com.evmap.serverapp.features.event.application.query.GetEventById
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [EventController::class])
@Import(EventApiExceptionHandler::class)
class EventControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var createEvent: CreateEvent

    @MockBean
    lateinit var getEventById: GetEventById

    @MockBean
    lateinit var getAllEvents: GetAllEventsByUser

    @Test
    fun `create should return 201 for valid payload`() {
        Mockito.doReturn(123L)
            .`when`(createEvent)
            .handle(ArgumentMatchers.any(CreateEvent::class.java))

        val body = """
            {
              "name": "Test event",
              "description": "Test description",
              "startsAt": "2026-03-22T10:00:00Z",
              "locationId": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
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
              "locationId": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
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
              "locationId": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
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

        mockMvc.perform(get("/api/events/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Event 99 not found"))
    }
}
