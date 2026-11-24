package com.evmap.serverapp.features.chat.infra.write

import com.evmap.serverapp.features.chat.application.ChatNotFoundException
import com.evmap.serverapp.features.chat.application.ChatAccessDeniedException
import com.evmap.serverapp.features.chat.domian.ChatAttachment
import com.evmap.serverapp.features.chat.domian.Chat
import com.evmap.serverapp.features.chat.domian.ChatMessage
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import com.evmap.serverapp.features.chat.infra.read.toViewAttachment
import com.evmap.serverapp.features.user.application.UserNotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class ChatRepositoryAdapter(
    private val dsl: DSLContext
) : ChatRepositoryPort {

    @Transactional
    override fun save(chat: Chat, creatorUserId: Long, participantUserIds: List<Long>): Long {
        val chatId = dsl.fetchOne(
            """
            INSERT INTO chat(name, created_by_user_id)
            VALUES (?, ?)
            RETURNING id
            """.trimIndent(),
            chat.name,
            creatorUserId,
        )?.get("id", Long::class.java) ?: error("Failed to create chat")

        val members = (participantUserIds + creatorUserId).distinct()
        members.forEach { userId ->
            dsl.execute(
                """
                INSERT INTO user_chat(chat_id, user_id, joined_at)
                VALUES (?, ?, CAST(? AS timestamptz))
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                chatId,
                userId,
                OffsetDateTime.now(ZoneOffset.UTC)
            )
        }
        return chatId
    }

    @Transactional
    override fun openDirect(userId: Long, peerId: Long): Long {
        require(peerId > 0 && peerId != userId) { "Choose another user for the conversation" }
        val low = minOf(userId, peerId)
        val high = maxOf(userId, peerId)
        // Both initiators acquire the same row locks in the same order.
        val accounts = dsl.fetch("SELECT id FROM app_user WHERE id IN (?, ?) ORDER BY id FOR UPDATE", low, high)
        if (accounts.size != 2) throw UserNotFoundException(peerId)
        dsl.fetchOne("SELECT chat_id FROM direct_chat WHERE user_low = ? AND user_high = ?", low, high)
            ?.get(0, Long::class.javaObjectType)?.let { return it }
        val chatId = save(Chat(name = "Direct chat"), userId, listOf(peerId))
        dsl.execute("INSERT INTO direct_chat(chat_id, user_low, user_high) VALUES (?, ?, ?)", chatId, low, high)
        return chatId
    }

    @Transactional
    override fun addUser(chatId: Long, userId: Long) {
        lockChat(chatId)
        requireGroup(chatId)
        dsl.execute(
            """
            INSERT INTO user_chat(chat_id, user_id, joined_at)
            VALUES (?, ?, CAST(? AS timestamptz))
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            chatId,
            userId,
            OffsetDateTime.now(ZoneOffset.UTC)
        )
    }

    @Transactional
    override fun removeUser(chatId: Long, userId: Long) {
        lockChat(chatId)
        requireGroup(chatId)
        dsl.execute("DELETE FROM message WHERE chat_id = ? AND user_id = ?", chatId, userId)
        dsl.execute("DELETE FROM user_chat WHERE chat_id = ? AND user_id = ?", chatId, userId)
    }

    override fun changeName(chatId: Long, name: String) {
        val updated = dsl.execute("UPDATE chat SET name = ? WHERE id = ?", name, chatId)
        if (updated == 0) throw ChatNotFoundException(chatId)
    }

    @Transactional
    override fun delete(chatId: Long) {
        lockChat(chatId)
        dsl.execute("DELETE FROM message WHERE chat_id = ?", chatId)
        dsl.execute("DELETE FROM user_chat WHERE chat_id = ?", chatId)
        dsl.execute("DELETE FROM chat WHERE id = ?", chatId)
    }

    @Transactional
    override fun sendMessage(
        chatId: Long,
        userId: Long,
        message: String,
        clientMessageId: UUID?,
        attachment: ChatAttachment?,
    ): ChatMessage {
        lockChat(chatId)
        ensureMember(chatId, userId)
        if (clientMessageId != null) {
            val existing = dsl.fetchOne(
                """
                SELECT m.id, m.text, m.sent_at, u.username, a.message_id AS attachment_id,
                       a.kind, a.file_name, a.content_type, a.size_bytes, a.duration_ms
                FROM message m JOIN app_user u ON u.id = m.user_id
                LEFT JOIN chat_attachment a ON a.message_id = m.id
                WHERE m.chat_id = ? AND m.user_id = ? AND m.client_message_id = ?
                """.trimIndent(), chatId, userId, clientMessageId,
            )
            if (existing != null) return ChatMessage(
                id = existing.get("id", Long::class.java)!!,
                chatId = chatId,
                senderId = userId,
                senderUsername = existing.get("username", String::class.java),
                text = existing.get("text", String::class.java)!!,
                sentAt = existing.get("sent_at", OffsetDateTime::class.java)!!,
                attachment = existing.toViewAttachment(),
                created = false,
            )
        }
        val inserted = dsl.fetchOne(
            """
            INSERT INTO message(text, sent_at, chat_id, user_id, client_message_id)
            VALUES (?, GREATEST(clock_timestamp(), (
                SELECT sent_at FROM message WHERE chat_id = ? ORDER BY sent_at DESC, id DESC LIMIT 1
            )), ?, ?, ?)
            RETURNING id, sent_at
            """.trimIndent(),
            message,
            chatId,
            chatId,
            userId,
            clientMessageId,
        ) ?: error("Failed to send message")
        val messageId = inserted.get("id", Long::class.java)!!
        if (attachment != null) dsl.execute(
            """
            INSERT INTO chat_attachment(message_id, kind, file_name, content_type, size_bytes, duration_ms, content)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(), messageId, attachment.kind, attachment.name, attachment.contentType,
            attachment.bytes.size, attachment.durationMs, attachment.bytes,
        )

        val senderUsername = dsl.fetchOne("SELECT username FROM app_user WHERE id = ?", userId)
            ?.get("username", String::class.java)

        return ChatMessage(
            id = messageId,
            chatId = chatId,
            senderId = userId,
            senderUsername = senderUsername,
            text = message,
            sentAt = inserted.get("sent_at", OffsetDateTime::class.java)!!,
            attachment = attachment?.view(messageId),
        )
    }

    @Transactional
    override fun markRead(chatId: Long, userId: Long, lastReadMessageId: Long): OffsetDateTime? {
        lockChat(chatId)
        ensureMember(chatId, userId)
        val sentAt = dsl.fetchOne(
            "SELECT sent_at FROM message WHERE id = ? AND chat_id = ?",
            lastReadMessageId,
            chatId,
        )?.get(0, OffsetDateTime::class.java) ?: throw ChatNotFoundException(chatId)

        val changed = dsl.execute(
            """
            INSERT INTO chat_read_state(chat_id, user_id, last_read_message_id, read_at)
            VALUES (?, ?, ?, CAST(? AS timestamptz))
            ON CONFLICT (chat_id, user_id)
            DO UPDATE SET last_read_message_id = EXCLUDED.last_read_message_id,
                          read_at = EXCLUDED.read_at
            WHERE chat_read_state.last_read_message_id IS NULL OR EXISTS (
                SELECT 1 FROM message old
                WHERE old.id = chat_read_state.last_read_message_id
                  AND (old.sent_at, old.id) < (CAST(? AS timestamptz), ?)
            )
            """.trimIndent(),
            chatId,
            userId,
            lastReadMessageId,
            OffsetDateTime.now(ZoneOffset.UTC),
            sentAt,
            lastReadMessageId,
        )
        return sentAt.takeIf { changed > 0 }
    }

    override fun isOwner(chatId: Long, userId: Long): Boolean =
        dsl.fetchOne(
            "SELECT 1 FROM chat WHERE id = ? AND created_by_user_id = ?",
            chatId,
            userId,
        ) != null

    private fun lockChat(chatId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM chat WHERE id = ? FOR UPDATE", chatId) != null
        if (!exists) throw ChatNotFoundException(chatId)
    }

    private fun requireGroup(chatId: Long) {
        require(dsl.fetchOne("SELECT 1 FROM direct_chat WHERE chat_id = ?", chatId) == null) {
            "A direct conversation has a fixed pair of participants"
        }
    }

    private fun ensureMember(chatId: Long, userId: Long) {
        val exists = dsl.fetchOne(
            "SELECT 1 FROM user_chat WHERE chat_id = ? AND user_id = ?",
            chatId,
            userId,
        ) != null
        if (!exists) throw ChatAccessDeniedException(chatId)
    }
}
