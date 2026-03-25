package com.evmap.serverapp.features.chat.infra.write

import com.evmap.serverapp.features.chat.application.ChatNotFoundException
import com.evmap.serverapp.features.chat.domian.Chat
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
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
            INSERT INTO chat(name)
            VALUES (?)
            RETURNING id
            """.trimIndent(),
            chat.name
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

    override fun sendMessage(chatId: Long, userId: Long, message: String) {
        ensureChatExists(chatId)
        dsl.execute(
            """
            INSERT INTO message(text, sent_at, chat_id, user_id)
            VALUES (?, CAST(? AS timestamptz), ?, ?)
            """.trimIndent(),
            message,
            OffsetDateTime.now(ZoneOffset.UTC),
            chatId,
            userId
        )
    }

    private fun ensureChatExists(chatId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM chat WHERE id = ?", chatId) != null
        if (!exists) throw ChatNotFoundException(chatId)
    }
}
