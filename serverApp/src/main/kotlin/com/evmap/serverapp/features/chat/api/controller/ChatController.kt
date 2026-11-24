package com.evmap.serverapp.features.chat.api.controller

import com.evmap.serverapp.features.chat.api.dto.CreateChat
import com.evmap.serverapp.features.chat.api.dto.MarkChatReadRequest
import com.evmap.serverapp.features.chat.api.dto.SendMessageRequest
import com.evmap.serverapp.features.chat.api.dto.ViewChat
import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.api.dto.ViewReadEvent
import com.evmap.serverapp.features.chat.application.command.AddUserToChat
import com.evmap.serverapp.features.chat.application.command.ChangeChatName
import com.evmap.serverapp.features.chat.application.command.CreateChat as CreateChatCommand
import com.evmap.serverapp.features.chat.application.command.DeleteChat
import com.evmap.serverapp.features.chat.application.command.DeleteUserFromChat
import com.evmap.serverapp.features.chat.application.command.MarkChatRead
import com.evmap.serverapp.features.chat.application.command.SendMessage
import com.evmap.serverapp.features.chat.application.command.chatAttachment
import com.evmap.serverapp.features.chat.application.query.GetAllChats
import com.evmap.serverapp.features.chat.application.query.GetAllChatsByUserId
import com.evmap.serverapp.features.chat.application.query.GetChatInfo
import com.evmap.serverapp.features.chat.application.query.GetMessagesByChatId
import com.evmap.serverapp.features.chat.application.query.SearchByAllChats
import com.evmap.serverapp.features.chat.application.query.SearchByChat
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import com.evmap.serverapp.features.chat.infra.read.ChatReadRepository
import com.evmap.serverapp.shared.security.CurrentUserProvider
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val createChat: CreateChatCommand,
    private val addUserToChat: AddUserToChat,
    private val changeChatName: ChangeChatName,
    private val deleteChat: DeleteChat,
    private val deleteUserFromChat: DeleteUserFromChat,
    private val sendMessage: SendMessage,
    private val markChatRead: MarkChatRead,
    private val getAllChats: GetAllChats,
    private val getAllChatsByUserId: GetAllChatsByUserId,
    private val getChatInfo: GetChatInfo,
    private val getMessagesByChatId: GetMessagesByChatId,
    private val searchByAllChats: SearchByAllChats,
    private val searchByChat: SearchByChat,
    private val currentUserProvider: CurrentUserProvider,
    private val chatRepository: ChatRepositoryPort,
    private val chatReadRepository: ChatReadRepository,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateChat): Long =
        createChat.handle(dto, currentUserProvider.get().id)

    @PostMapping("/direct/{peerId}")
    fun openDirect(@PathVariable peerId: Long): Long = chatRepository.openDirect(currentUserProvider.get().id, peerId)

    @GetMapping("/all")
    fun getAll(): List<ViewChat> {
        currentUserProvider.requireAdmin()
        return getAllChats.handle()
    }

    @GetMapping
    fun getAllByUserId(@RequestParam(required = false) userId: Long?): List<ViewChat> {
        val requestedUserId = userId ?: currentUserProvider.get().id
        currentUserProvider.requireSelfOrAdmin(requestedUserId)
        return getAllChatsByUserId.handle(requestedUserId)
    }

    @GetMapping("/{chatId}")
    fun getInfoById(@PathVariable chatId: Long): ViewChat = getChatInfo.handle(chatId, currentUserProvider.get().id)

    @GetMapping("/{chatId}/messages")
    fun getMessages(
        @PathVariable chatId: Long,
        @RequestParam(required = false) beforeId: Long?,
        @RequestParam(required = false) afterId: Long?,
        @RequestParam(required = false) limit: Int?,
    ): List<ViewMessage> =
        getMessagesByChatId.handle(
            chatId = chatId,
            userId = currentUserProvider.get().id,
            beforeId = beforeId,
            afterId = afterId,
            limit = limit,
        )

    @GetMapping("/search")
    fun searchByChatName(@RequestParam query: String): List<ViewChat> = searchByAllChats.handle(query, currentUserProvider.get().id)

    @GetMapping("/{chatId}/search")
    fun searchInChat(
        @PathVariable chatId: Long,
        @RequestParam query: String,
    ): List<String> = searchByChat.handle(chatId, query, currentUserProvider.get().id)

    @PostMapping("/{chatId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addUser(@PathVariable chatId: Long, @PathVariable userId: Long) {
        requireChatOwnerOrAdmin(chatId)
        addUserToChat.handle(chatId, userId)
    }

    @DeleteMapping("/{chatId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeUser(@PathVariable chatId: Long, @PathVariable userId: Long) {
        requireChatOwnerOrAdmin(chatId)
        deleteUserFromChat.handle(chatId, userId)
    }

    @PatchMapping("/{chatId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rename(@PathVariable chatId: Long, @RequestParam name: String) {
        requireChatOwnerOrAdmin(chatId)
        changeChatName.handle(chatId, name)
    }

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun send(
        @PathVariable chatId: Long,
        @Valid @RequestBody request: SendMessageRequest,
    ): ViewMessage =
        sendMessage.handle(
            chatId = chatId,
            userId = currentUserProvider.get().id,
            message = request.text,
            clientMessageId = request.clientMessageId,
        )

    @PostMapping("/{chatId}/attachments", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun attach(
        @PathVariable chatId: Long,
        @RequestParam kind: String,
        @RequestParam clientMessageId: UUID,
        @RequestParam(required = false) durationMs: Long?,
        @RequestPart("file") file: MultipartFile,
    ): ViewMessage {
        val userId = currentUserProvider.get().id
        chatReadRepository.requireMember(chatId, userId)
        return sendMessage.handle(chatId, userId, "", clientMessageId, chatAttachment(file, kind, durationMs))
    }

    @GetMapping("/{chatId}/attachments/{attachmentId}")
    fun attachment(@PathVariable chatId: Long, @PathVariable attachmentId: Long): ResponseEntity<ByteArray> {
        val attachment = chatReadRepository.findAttachment(chatId, currentUserProvider.get().id, attachmentId)
        val disposition = if (attachment.kind == "FILE") ContentDisposition.attachment() else ContentDisposition.inline()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(attachment.contentType))
            .contentLength(attachment.bytes.size.toLong())
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(attachment.name, Charsets.UTF_8).build().toString())
            .body(attachment.bytes)
    }

    @GetMapping("/{chatId}/read")
    fun readStates(@PathVariable chatId: Long): List<ViewReadEvent> = chatReadRepository.findReadStates(chatId, currentUserProvider.get().id)

    @PostMapping("/{chatId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markRead(
        @PathVariable chatId: Long,
        @Valid @RequestBody request: MarkChatReadRequest,
    ) {
        markChatRead.handle(chatId, currentUserProvider.get().id, request.lastReadMessageId)
    }

    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable chatId: Long) {
        requireChatOwnerOrAdmin(chatId)
        deleteChat.handle(chatId)
    }

    private fun requireChatOwnerOrAdmin(chatId: Long) {
        val currentUser = currentUserProvider.get()
        if (currentUser.isAdmin) return
        if (!chatRepository.isOwner(chatId, currentUser.id)) {
            throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied")
        }
    }
}
