package com.evmap.serverapp.features.user.api.controller

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.application.query.GetAllEventsByUser
import com.evmap.serverapp.features.user.api.dto.CreateUser
import com.evmap.serverapp.features.user.api.dto.UpdateProfile
import com.evmap.serverapp.features.user.api.dto.ViewUser
import com.evmap.serverapp.features.user.api.dto.ViewUserShort
import com.evmap.serverapp.features.user.application.command.CreateUser as CreateUserCommand
import com.evmap.serverapp.features.user.application.command.DeleteUser
import com.evmap.serverapp.features.user.application.command.EditUserInfo
import com.evmap.serverapp.features.user.application.command.FollowUser
import com.evmap.serverapp.features.user.application.command.UnfollowUser
import com.evmap.serverapp.features.user.application.query.GetAllUsers
import com.evmap.serverapp.features.user.application.query.GetFollowedUserById
import com.evmap.serverapp.features.user.application.query.GetFollowingUserById
import com.evmap.serverapp.features.user.application.query.GetUserById
import com.evmap.serverapp.shared.security.CurrentUserProvider
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val createUser: CreateUserCommand,
    private val deleteUser: DeleteUser,
    private val editUserInfo: EditUserInfo,
    private val followUser: FollowUser,
    private val unfollowUser: UnfollowUser,
    private val getAllUsers: GetAllUsers,
    private val getUserById: GetUserById,
    private val getFollowedUserById: GetFollowedUserById,
    private val getFollowingUserById: GetFollowingUserById,
    private val currentUserProvider: CurrentUserProvider,
    private val getEvents: GetAllEventsByUser,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateUser): Long = createUser.handle(dto)

    @GetMapping
    fun getAll(): List<ViewUser> = getAllUsers.handle()

    @GetMapping("/{userId}")
    fun getById(@PathVariable userId: Long): ViewUser = getUserById.handle(userId)

    @GetMapping("/{userId}/events")
    fun events(@PathVariable userId: Long, @PageableDefault(size = 20) pageable: Pageable): Page<ViewEvent> {
        getUserById.handle(userId)
        return getEvents.handle(pageable, userId)
    }

    @GetMapping("/{userId}/followers")
    fun getFollowers(@PathVariable userId: Long): List<ViewUserShort> = getFollowedUserById.handle(userId)

    @GetMapping("/{userId}/following")
    fun getFollowing(@PathVariable userId: Long): List<ViewUserShort> = getFollowingUserById.handle(userId)

    @GetMapping("/me")
    fun getMe(): ViewUser = getUserById.handle(currentUserProvider.get().id)

    @PatchMapping("/me")
    fun updateMe(@Valid @RequestBody profile: UpdateProfile): ViewUser =
        editUserInfo.handle(currentUserProvider.get().id, profile)

    @PatchMapping("/{userId}/description")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateDescription(@PathVariable userId: Long, @RequestParam description: String) {
        currentUserProvider.requireSelfOrAdmin(userId)
        editUserInfo.handle(userId, description)
    }

    @PostMapping("/{userId}/follow/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun follow(@PathVariable userId: Long, @PathVariable targetUserId: Long) {
        currentUserProvider.requireSelfOrAdmin(userId)
        followUser.handle(currentUserProvider.get().id, targetUserId)
    }

    @DeleteMapping("/{userId}/follow/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfollow(@PathVariable userId: Long, @PathVariable targetUserId: Long) {
        currentUserProvider.requireSelfOrAdmin(userId)
        unfollowUser.handle(currentUserProvider.get().id, targetUserId)
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable userId: Long) {
        currentUserProvider.requireSelfOrAdmin(userId)
        deleteUser.handle(userId)
    }
}
