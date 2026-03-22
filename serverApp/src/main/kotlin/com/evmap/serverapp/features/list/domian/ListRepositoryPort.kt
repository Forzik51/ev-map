package com.evmap.serverapp.features.user.domian


interface ListRepositoryPort {
    fun save(event: Chat): Long
}