package com.evmap.serverapp.features.auth.application

class InvalidCredentialsException : RuntimeException("Invalid username/email or password")

class InvalidRefreshTokenException : RuntimeException("Invalid refresh token")
