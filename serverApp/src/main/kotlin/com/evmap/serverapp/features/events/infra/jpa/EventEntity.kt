package com.evmap.serverapp.features.events.infra.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "Wydarzenie")
class EventEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "nazwa", nullable = false)
    var name: String,

    @Column(name = "opis", nullable = false, length = 500)
    var description: String,

    @Column(name = "kiedy", nullable = false)
    var startsAt: java.time.Instant,

    @Column(name = "Lokalizacja_id", nullable = false)
    var locationId: Long,
)