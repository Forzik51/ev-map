package com.evmap.serverapp.features.list.application

class ListNotFoundException(id: Long) : RuntimeException("List $id not found")
