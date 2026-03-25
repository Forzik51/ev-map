package com.evmap.serverapp.features.event.infra.write

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "event")
class EventEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "name", nullable = false, length = 30)
    var name: String,

    @Column(name = "description", nullable = false, length = 500)
    var description: String,

    @Column(name = "start_at", nullable = false)
    var startsAt: java.time.Instant,

    @Column(name = "end_at")
    var endsAt: java.time.Instant? = null,

    @Column(name = "location_id", nullable = false)
    var locationId: Long,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: java.time.Instant,
)
