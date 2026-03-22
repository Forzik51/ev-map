package com.evmap.serverapp.features.event.domain

interface EventRepositoryPort {
    fun save(event: Event): Long
}