package com.evmap.serverapp.features.chat.infra.read

import com.evmap.serverapp.features.chat.api.dto.ViewAttachment
import com.evmap.serverapp.features.chat.api.dto.ViewChat
import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.api.dto.ViewReadEvent
import com.evmap.serverapp.features.chat.application.ChatAccessDeniedException
import com.evmap.serverapp.features.chat.application.ChatNotFoundException
import com.evmap.serverapp.features.chat.domian.ChatAttachment
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.OffsetDateTime

@Repository
class ChatReadRepository(
    private val dsl: DSLContext
) {
    fun findByUserId(userId: Long): List<ViewChat> = memberChats(userId)

    private fun memberChats(userId: Long, chatId: Long? = null, query: String? = null): List<ViewChat> =
        dsl.fetch(
            """
            SELECT c.id,
                   COALESCE(peer.username, c.name) AS name,
                   members.users_count, members.user_ids,
                   latest.sent_at AS last_message_at, latest.text AS last_message_text,
                   latest.kind AS last_attachment_kind, unread.unread_count
            FROM user_chat uc
            JOIN chat c ON c.id = uc.chat_id
            LEFT JOIN direct_chat dc ON dc.chat_id = c.id
            LEFT JOIN app_user peer ON peer.id = CASE WHEN dc.user_low = uc.user_id THEN dc.user_high ELSE dc.user_low END
            JOIN LATERAL (
                SELECT COUNT(*)::int AS users_count, ARRAY_AGG(member.user_id ORDER BY member.user_id) AS user_ids
                FROM user_chat member WHERE member.chat_id = c.id
            ) members ON true
            LEFT JOIN LATERAL (
                SELECT m.sent_at, m.text, a.kind
                FROM message m LEFT JOIN chat_attachment a ON a.message_id = m.id
                WHERE m.chat_id = c.id ORDER BY m.sent_at DESC, m.id DESC LIMIT 1
            ) latest ON true
            LEFT JOIN chat_read_state r ON r.chat_id = c.id AND r.user_id = uc.user_id
            LEFT JOIN message last_read ON last_read.id = r.last_read_message_id
            JOIN LATERAL (
                SELECT COUNT(*)::int AS unread_count FROM message m
                WHERE m.chat_id = c.id AND m.user_id <> uc.user_id
                  AND (last_read.id IS NULL OR (m.sent_at, m.id) > (last_read.sent_at, last_read.id))
            ) unread ON true
            WHERE uc.user_id = ?
            ${if (chatId != null) "AND c.id = ?" else ""}
            ${if (query != null) "AND COALESCE(peer.username, c.name) ILIKE ?" else ""}
            ORDER BY latest.sent_at DESC NULLS LAST, c.id
            """.trimIndent(),
            *listOfNotNull(userId, chatId, query?.let { "%$it%" }).toTypedArray(),
        ).map {
            toViewChat(it).copy(
                lastMessageText = it.get("last_message_text", String::class.java),
                lastAttachmentKind = it.get("last_attachment_kind", String::class.java),
                unreadCount = it.get("unread_count", Int::class.javaObjectType) ?: 0,
            )
        }

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

    fun findById(chatId: Long, userId: Long): ViewChat {
        requireMember(chatId, userId)
        return memberChats(userId, chatId = chatId).singleOrNull() ?: throw ChatAccessDeniedException(chatId)
    }

    fun findMessages(chatId: Long, userId: Long, beforeId: Long?, afterId: Long?, limit: Int): List<ViewMessage> {
        requireMember(chatId, userId)
        require(beforeId == null || afterId == null) { "Use one message cursor per request" }

        val conditions = mutableListOf("m.chat_id = ?")
        val bindings = mutableListOf<Any>(chatId)
        val cursor = beforeId ?: afterId
        if (cursor != null) {
            val cursorTime = dsl.fetchOne("SELECT sent_at FROM message WHERE id = ? AND chat_id = ?", cursor, chatId)
                ?.get(0, OffsetDateTime::class.java) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message cursor not found")
            val comparison = if (afterId != null) ">" else "<"
            conditions += "(m.sent_at, m.id) $comparison (CAST(? AS timestamptz), ?)"
            bindings += cursorTime
            bindings += cursor
        }
        bindings += limit
        val direction = if (afterId != null) "ASC" else "DESC"

        val rows = dsl.fetch(
            """
            SELECT m.id,
                   m.chat_id,
                   m.user_id,
                   u.username,
                   m.text,
                   m.sent_at,
                   a.message_id AS attachment_id, a.kind, a.file_name, a.content_type, a.size_bytes, a.duration_ms
            FROM message m
            LEFT JOIN app_user u ON u.id = m.user_id
            LEFT JOIN chat_attachment a ON a.message_id = m.id
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY m.sent_at $direction, m.id $direction
            LIMIT ?
            """.trimIndent(),
            *bindings.toTypedArray(),
        )

        return (if (afterId != null) rows else rows.reversed()).map { toViewMessage(it) }
    }

    fun searchByAllChats(query: String, userId: Long): List<ViewChat> = memberChats(userId, query = query)

    fun searchByChat(chatId: Long, query: String, userId: Long): List<String> {
        requireMember(chatId, userId)
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

    fun findReadStates(chatId: Long, userId: Long): List<ViewReadEvent> {
        requireMember(chatId, userId)
        return dsl.fetch(
            """
            SELECT r.user_id, m.id, m.sent_at FROM chat_read_state r
            JOIN message m ON m.id = r.last_read_message_id WHERE r.chat_id = ?
            """.trimIndent(), chatId,
        ).map { row -> ViewReadEvent(chatId, row.get("user_id", Long::class.java)!!, row.get("id", Long::class.java)!!,
            row.get("sent_at", OffsetDateTime::class.java)!!.toInstant().toString()) }
    }

    fun findAttachment(chatId: Long, userId: Long, attachmentId: Long): ChatAttachment {
        requireMember(chatId, userId)
        val row = dsl.fetchOne(
            """
            SELECT a.kind, a.file_name, a.content_type, a.content, a.duration_ms
            FROM chat_attachment a JOIN message m ON m.id = a.message_id
            JOIN user_chat uc ON uc.chat_id = m.chat_id AND uc.user_id = ?
            WHERE m.chat_id = ? AND a.message_id = ?
            """.trimIndent(), userId, chatId, attachmentId,
        ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found")
        return ChatAttachment(row.get("kind", String::class.java)!!, row.get("file_name", String::class.java)!!,
            row.get("content_type", String::class.java)!!, row.get("content", ByteArray::class.java)!!,
            row.get("duration_ms", Long::class.javaObjectType))
    }

    fun requireMember(chatId: Long, userId: Long) {
        ensureChatExists(chatId)
        ensureMember(chatId, userId)
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
            attachment = record.toViewAttachment(),
        )
    }
}

internal fun Record.toViewAttachment(): ViewAttachment? {
    val id = get("attachment_id", Long::class.javaObjectType) ?: return null
    return ViewAttachment(id, get("kind", String::class.java)!!, get("file_name", String::class.java)!!,
        get("content_type", String::class.java)!!, get("size_bytes", Int::class.java)!!,
        get("duration_ms", Long::class.javaObjectType))
}
