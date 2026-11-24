package com.evmap.serverapp.features.auth.application

class InvalidPasswordResetCode : RuntimeException("The code is invalid or has expired. Request a new code.")
class InvalidPasswordResetToken : RuntimeException("Your password reset session has expired. Start again.")
class PasswordResetRateLimited(val retryAfterSeconds: Long) : RuntimeException("Too many requests. Please try again later.")
data class PasswordChangedEvent(val userId: Long)
