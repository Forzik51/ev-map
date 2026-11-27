package com.evmap.serverapp.shared.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ClientVersionFilter(@Value("\${app.minimum-client-version:0}") private val minimum: Int): OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest,response: HttpServletResponse,chain: FilterChain) {
        val version=request.getHeader("X-Evmap-Client-Version")?.toIntOrNull() ?: 0
        if(minimum>0 && request.requestURI.startsWith("/api/") && version<minimum) {
            response.status=426;response.contentType="application/json;charset=UTF-8"
            response.writer.write("""{"code":"UPDATE_REQUIRED","message":"Оновіть застосунок до останньої версії","minimumVersion":$minimum}""")
        } else chain.doFilter(request,response)
    }
}
