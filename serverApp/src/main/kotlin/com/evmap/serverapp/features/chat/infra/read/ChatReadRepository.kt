package com.evmap.serverapp.features.chat.infra.read

import com.evmap.serverapp.features.chat.api.dto.ViewChat
import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.application.ChatAccessDeniedException
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
                   MAX(m.sent_at) AS last_message_at,
                   COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT uc_all.user_id), NULL), ARRAY[]::bigint[]) AS user_ids
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

    fun findAll(): List<ViewChat> =
        dsl.fetch(
            """
            SELECT c.id,
                   c.name,
                   COUNT(DISTINCT uc.user_id) AS users_count,
                   MAX(m.sent_at) AS last_message_at,
                   COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT uc.user_id), NULL), ARRAY[]::bigint[]) AS user_ids
            FROM chat c
            LEFT JOIN user_chat uc ON uc.chat_id = c.id
            LEFT JOIN message m ON m.chat_id = c.id
            GROUP BY c.id, c.name
            ORDER BY MAX(m.sent_at) DESC NULLS LAST, c.id
            """.trimIndent()
        ).map { toViewChat(it) }

    fun findById(chatId: Long): ViewChat =
        dsl.fetchOne(
            """
            SELECT c.id,
                   c.name,
                   COUNT(DISTINCT uc.user_id) AS users_count,
                   MAX(m.sent_at) AS last_message_at,
                   COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT uc.user_id), NULL), ARRAY[]::bigint[]) AS user_ids
            FROM chat c
            LEFT JOIN user_chat uc ON uc.chat_id = c.id
            LEFT JOIN message m ON m.chat_id = c.id
            WHERE c.id = ?
            GROUP BY c.id, c.name
            """.trimIndent(),
            chatId
        )?.let { toViewChat(it) } ?: throw ChatNotFoundException(chatId)

    fun findMessages(chatId: Long, userId: Long, beforeId: Long?, limit: Int): List<ViewMessage> {
        ensureChatExists(chatId)
        ensureMember(chatId, userId)

        val conditions = mutableListOf("m.chat_id = ?")
        val bindings = mutableListOf<Any>(chatId)
        if (beforeId != null) {
            conditions += "m.id < ?"
            bindings += beforeId
        }
        bindings += limit

        val rows = dsl.fetch(
            """
            SELECT m.id,
                   m.chat_id,
                   m.user_id,
                   u.username,
                   m.text,
                   m.sent_at
            FROM message m
            LEFT JOIN app_user u ON u.id = m.user_id
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY m.sent_at DESC, m.id DESC
            LIMIT ?
            """.trimIndent(),
            *bindings.toTypedArray(),
        )

        return rows.reversed().map { toViewMessage(it) }
    }

    fun searchByAllChats(query: String): List<ViewChat> =
        dsl.fetch(
            """
            SELECT c.id,
                   c.name,
                   COUNT(DISTINCT uc.user_id) AS users_count,
                   MAX(m.sent_at) AS last_message_at,
                   COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT uc.user_id), NULL), ARRAY[]::bigint[]) AS user_ids
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

    private fun ensureMember(chatId: Long, userId: Long) {
        val exists = dsl.fetchOne(
            "SELECT 1 FROM user_chat WHERE chat_id = ? AND user_id = ?",
            chatId,
            userId,
        ) != null
        if (!exists) throw ChatAccessDeniedException(chatId)
    }

    private fun toViewChat(record: org.jooq.Record): ViewChat {
        val lastMessageTs = record.get("last_message_at", Timestamp::class.java)
        return ViewChat(
            id = record.get("id", Long::class.java)!!,
            name = record.get("name", String::class.java)!!,
            usersCount = record.get("users_count", Int::class.java) ?: 0,
            lastMessageAt = lastMessageTs?.toInstant()?.toString(),
            userIds = extractUserIds(record),
        )
    }

    private fun extractUserIds(record: org.jooq.Record): List<Long> {
        val raw = record.get("user_ids")
        return when (raw) {
            is java.sql.Array -> (raw.array as? Array<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
            is Array<*> -> raw.mapNotNull { (it as? Number)?.toLong() }
            is LongArray -> raw.toList()
            is Collection<*> -> raw.mapNotNull { (it as? Number)?.toLong() }
            else -> emptyList()
        }
    }

    private fun toViewMessage(record: org.jooq.Record): ViewMessage {
        val sentAt = requireNotNull(record.get("sent_at", Timestamp::class.java)) {
            "sent_at is null for message row"
        }
        return ViewMessage(
            id = requireNotNull(record.get("id", Long::class.java)) { "id is null for message row" },
            chatId = requireNotNull(record.get("chat_id", Long::class.java)) { "chat_id is null for message row" },
            senderId = requireNotNull(record.get("user_id", Long::class.java)) { "user_id is null for message row" },
            senderUsername = record.get("username", String::class.java),
            text = requireNotNull(record.get("text", String::class.java)) { "text is null for message row" },
            sentAt = sentAt.toInstant().toString(),
        )
    }
}
