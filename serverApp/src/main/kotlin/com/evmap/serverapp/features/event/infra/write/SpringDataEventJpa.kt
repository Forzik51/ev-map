package com.evmap.serverapp.features.event.infra.write

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataEventJpa : JpaRepository<EventEntity, Long>