package com.evmap.serverapp.shared.security

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

fun validOptionalPhone(value: String?): Boolean = value.isNullOrBlank() || (Regex("^\\+?[0-9 ()-]+$").matches(value.trim()) && value.count {it in '0'..'9'} in 7..15)
fun normalizedPhone(value: String?): String? = value?.trim()?.takeIf {it.isNotEmpty()}?.filter {it in '0'..'9' || it=='+'}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy=[OptionalPhoneValidator::class])
annotation class ValidOptionalPhone(val message: String="Укажіть телефон із 7–15 цифр або залиште поле порожнім",val groups: Array<KClass<*>> = [],val payload: Array<KClass<out Payload>> = [])
class OptionalPhoneValidator: ConstraintValidator<ValidOptionalPhone,String?> {
    override fun isValid(value: String?,context: ConstraintValidatorContext)=validOptionalPhone(value)
}
