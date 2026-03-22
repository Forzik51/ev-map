package com.evmap.serverapp.features.user.domian


interface UserRepositoryPort {
    fun save(event: Chat): Long
}