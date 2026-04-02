package com.evmap.serverapp.features.user.application

class UserPhotoNotFoundException(userId: Long) : RuntimeException("Photo for user $userId not found")
