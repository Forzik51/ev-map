package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.api.dto.CreateEvent as CreateEventRequest
import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service
import com.evmap.serverapp.features.event.application.toEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.security.MessageDigest

@Service
class CreateEvent(private val repo: EventRepositoryPort, private val dsl: DSLContext, private val json: ObjectMapper) {
    @Transactional
    fun handle(dto: CreateEventRequest, userId: Long): Long {
        val event = dto.toEvent(userId)
        val key = dto.clientRequestId ?: return repo.save(event)
        val hash = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(dto.copy(clientRequestId = null))).joinToString("") { "%02x".format(it) }
        // Unique insertion serializes concurrent retries in the same transaction as event creation.
        val inserted = dsl.execute("INSERT INTO event_creation_request(user_id,request_id,request_hash) VALUES(?,?,?) ON CONFLICT DO NOTHING", userId,key,hash)
        if (inserted == 0) {
            val row = dsl.fetchOne("SELECT request_hash,event_id FROM event_creation_request WHERE user_id=? AND request_id=? FOR UPDATE",userId,key)!!
            if (row.get("request_hash",String::class.java) != hash) throw ResponseStatusException(HttpStatus.CONFLICT,"Цей запит уже використано з іншими даними")
            return (row.get("event_id") as? Number)?.toLong() ?: throw ResponseStatusException(HttpStatus.GONE,"Подію вже видалено")
        }
        val id = repo.save(event)
        dsl.execute("UPDATE event_creation_request SET event_id=? WHERE user_id=? AND request_id=?", id,userId,key)
        return id
    }
}
