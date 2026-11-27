package com.evmap.serverapp.shared.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockFilterChain

class ClientVersionFilterTest {
    @Test fun `legacy client receives an update response before nullable data`() {
        val request=MockHttpServletRequest("GET","/api/events")
        val response=MockHttpServletResponse()
        ClientVersionFilter(2).doFilter(request,response,MockFilterChain())
        assertEquals(426,response.status)
        assertTrue(response.contentAsString.contains("UPDATE_REQUIRED"))
    }
    @Test fun `compatible client and health requests are accepted`() {
        val filter=ClientVersionFilter(2)
        val request=MockHttpServletRequest("GET","/api/events").apply {addHeader("X-Evmap-Client-Version","2")}
        val chain=MockFilterChain()
        filter.doFilter(request,MockHttpServletResponse(),chain)
        assertNotNull(chain.request)
        val health=MockFilterChain()
        filter.doFilter(MockHttpServletRequest("GET","/health"),MockHttpServletResponse(),health)
        assertNotNull(health.request)
    }
}
