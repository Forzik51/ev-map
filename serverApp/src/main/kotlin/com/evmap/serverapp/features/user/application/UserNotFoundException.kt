package com.evmap.serverapp.features.user.application

class UserNotFoundException(id: Long) : RuntimeException("User $id not found")
