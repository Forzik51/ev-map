package com.evmap.serverapp.features.events.infra.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataEventJpa : JpaRepository<EventEntity, Long>