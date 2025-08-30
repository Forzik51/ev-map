package com.evmap.serverapp.features.events.domain

interface EventRepositoryPort {
    fun save(event: Event): Long
}