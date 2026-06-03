package com.evmap.serverapp.features.chat.infra.write

import com.evmap.serverapp.features.chat.application.ChatNotFoundException
import com.evmap.serverapp.features.chat.application.ChatAccessDeniedException
import com.evmap.serverapp.features.chat.domian.Chat
import com.evmap.serverapp.features.chat.domian.ChatMessage
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

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

    override fun addUser(chatId: Long, userId: Long) {
        ensureChatExists(chatId)
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
        ensureChatExists(chatId)
        dsl.execute("DELETE FROM message WHERE chat_id = ? AND user_id = ?", chatId, userId)
        dsl.execute("DELETE FROM user_chat WHERE chat_id = ? AND user_id = ?", chatId, userId)
    }

    override fun changeName(chatId: Long, name: String) {
        val updated = dsl.execute("UPDATE chat SET name = ? WHERE id = ?", name, chatId)
        if (updated == 0) throw ChatNotFoundException(chatId)
    }

    @Transactional
    override fun delete(chatId: Long) {
        ensureChatExists(chatId)
        dsl.execute("DELETE FROM message WHERE chat_id = ?", chatId)
        dsl.execute("DELETE FROM user_chat WHERE chat_id = ?", chatId)
        dsl.execute("DELETE FROM chat WHERE id = ?", chatId)
    }

    override fun sendMessage(chatId: Long, userId: Long, message: String): ChatMessage {
        ensureChatExists(chatId)
        ensureMember(chatId, userId)
        val sentAt = OffsetDateTime.now(ZoneOffset.UTC)
        val inserted = dsl.fetchOne(
            """
            INSERT INTO message(text, sent_at, chat_id, user_id)
            VALUES (?, CAST(? AS timestamptz), ?, ?)
            RETURNING id, sent_at
            """.trimIndent(),
            message,
            sentAt,
            chatId,
            userId
        ) ?: error("Failed to send message")

        val senderUsername = dsl.fetchOne("SELECT username FROM app_user WHERE id = ?", userId)
            ?.get("username", String::class.java)

        return ChatMessage(
            id = inserted.get("id", Long::class.java)!!,
            chatId = chatId,
            senderId = userId,
            senderUsername = senderUsername,
            text = message,
            sentAt = inserted.get("sent_at", OffsetDateTime::class.java) ?: sentAt,
        )
    }

    override fun markRead(chatId: Long, userId: Long, lastReadMessageId: Long) {
        ensureChatExists(chatId)
        ensureMember(chatId, userId)
        val belongsToChat = dsl.fetchOne(
            "SELECT 1 FROM message WHERE id = ? AND chat_id = ?",
            lastReadMessageId,
            chatId,
        ) != null
        if (!belongsToChat) throw ChatNotFoundException(chatId)

        dsl.execute(
            """
            INSERT INTO chat_read_state(chat_id, user_id, last_read_message_id, read_at)
            VALUES (?, ?, ?, CAST(? AS timestamptz))
            ON CONFLICT (chat_id, user_id)
            DO UPDATE SET last_read_message_id = EXCLUDED.last_read_message_id,
                          read_at = EXCLUDED.read_at
            """.trimIndent(),
            chatId,
            userId,
            lastReadMessageId,
            OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

    override fun isOwner(chatId: Long, userId: Long): Boolean =
        dsl.fetchOne(
            "SELECT 1 FROM chat WHERE id = ? AND created_by_user_id = ?",
            chatId,
            userId,
        ) != null

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
}
