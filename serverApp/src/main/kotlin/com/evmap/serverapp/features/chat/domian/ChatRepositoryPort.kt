package com.evmap.serverapp.features.user.domian


interface ChatRepositoryPort {
    fun save(event: Chat): Long
}