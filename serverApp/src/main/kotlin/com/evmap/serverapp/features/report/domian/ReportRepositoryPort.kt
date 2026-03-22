package com.evmap.serverapp.features.user.domian


interface ReportRepositoryPort {
    fun save(event: Chat): Long
}