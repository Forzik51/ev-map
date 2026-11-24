package com.evmap.serverapp.shared.security

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

// Keep registration and recovery consistent; BCrypt accepts at most 72 UTF-8 bytes.
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordValidator::class])
annotation class ValidPassword(
    val message: String = "Password must contain at least 6 characters and at most 72 UTF-8 bytes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class PasswordValidator : ConstraintValidator<ValidPassword, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value != null && value.isNotBlank() && value.length >= 6 && value.toByteArray(Charsets.UTF_8).size <= 72
}
