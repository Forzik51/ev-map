package com.evmap.serverapp.features.chat.infra.read

import com.evmap.serverapp.features.chat.api.dto.ViewChat
import com.evmap.serverapp.features.chat.application.ChatNotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class ChatReadRepository(
    private val dsl: DSLContext
) {
    fun findByUserId(userId: Long): List<ViewChat> =
        dsl.fetch(
            """
            SELECT c.id,
                   c.name,
                   COUNT(DISTINCT uc_all.user_id) AS users_count,
                   MAX(m.sent_at) AS last_message_at
            FROM user_chat uc
            JOIN chat c ON c.id = uc.chat_id
            LEFT JOIN user_chat uc_all ON uc_all.chat_id = c.id
            LEFT JOIN message m ON m.chat_id = c.id
            WHERE uc.user_id = ?
            GROUP BY c.id, c.name
            ORDER BY MAX(m.sent_at) DESC NULLS LAST, c.id
            """.trimIndent(),
            userId
        ).map { toViewChat(it) }

    fun findById(chatId: Long): ViewChat =
        dsl.fetchOne(
            """
            SELECT c.id,
                   c.name,
                   COUNT(DISTINCT uc.user_id) AS users_count,
                   MAX(m.sent_at) AS last_message_at
            FROM chat c
            LEFT JOIN user_chat uc ON uc.chat_id = c.id
            LEFT JOIN message m ON m.chat_id = c.id
            WHERE c.id = ?
            GROUP BY c.id, c.name
            """.trimIndent(),
            chatId
        )?.let { toViewChat(it) } ?: throw ChatNotFoundException(chatId)

    fun findMessages(chatId: Long): List<String> {
        ensureChatExists(chatId)
        return dsl.fetch(
            """
            SELECT text
            FROM message
            WHERE chat_id = ?
            ORDER BY sent_at ASC, id ASC
            """.trimIndent(),
            chatId
        ).map { it.get("text", String::class.java) ?: "" }
    }

    fun searchByAllChats(query: String): List<ViewChat> =
        dsl.fetch(
            """
            SELECT c.id,
                   c.name,
                   COUNT(DISTINCT uc.user_id) AS users_count,
                   MAX(m.sent_at) AS last_message_at
            FROM chat c
            LEFT JOIN user_chat uc ON uc.chat_id = c.id
            LEFT JOIN message m ON m.chat_id = c.id
            WHERE c.name ILIKE ?
            GROUP BY c.id, c.name
            ORDER BY c.name ASC
            """.trimIndent(),
            "%$query%"
        ).map { toViewChat(it) }

    fun searchByChat(chatId: Long, query: String): List<String> {
        ensureChatExists(chatId)
        return dsl.fetch(
            """
            SELECT text
            FROM message
            WHERE chat_id = ?
              AND text ILIKE ?
            ORDER BY sent_at ASC, id ASC
            """.trimIndent(),
            chatId,
            "%$query%"
        ).map { it.get("text", String::class.java) ?: "" }
    }

    private fun ensureChatExists(chatId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM chat WHERE id = ?", chatId) != null
        if (!exists) throw ChatNotFoundException(chatId)
    }

    private fun toViewChat(record: org.jooq.Record): ViewChat {
        val lastMessageTs = record.get("last_message_at", Timestamp::class.java)
        return ViewChat(
            id = record.get("id", Long::class.java)!!,
            name = record.get("name", String::class.java)!!,
            usersCount = record.get("users_count", Int::class.java) ?: 0,
            lastMessageAt = lastMessageTs?.toInstant()?.toString()
        )
    }
}
