package com.evmap.mobileapp.feature.profile.data

import com.evmap.mobileapp.core.data.dto.UserDto
import com.evmap.mobileapp.core.data.dto.UserShortDto
import com.evmap.mobileapp.feature.feed.data.toAbsoluteMediaUrl
import core.ui.model.ProfileUserUi
import core.ui.model.UserFollowUi

fun UserDto.toProfileUserUi(): ProfileUserUi =
    ProfileUserUi(
        id = id.toString(),
        username = username,
        avatarUrl = path.toAbsoluteMediaUrl(),
        title = listOfNotNull(name, surname).joinToString(" ").ifBlank { username },
        bio = pageDescription,
        followers = followersCount,
        following = followingCount,
    )

fun UserShortDto.toUserFollowUi(isFollowing: Boolean): UserFollowUi =
    UserFollowUi(
        user = ProfileUserUi(
            id = id.toString(),
            username = username,
            bio = pageDescription,
        ),
        isFollowing = isFollowing,
    )
