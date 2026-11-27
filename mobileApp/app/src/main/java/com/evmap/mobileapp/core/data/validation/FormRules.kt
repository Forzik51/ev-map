package com.evmap.mobileapp.core.data.validation

fun validOptionalPhone(value: String?): Boolean = value.isNullOrBlank() || (Regex("^\\+?[0-9 ()-]+$").matches(value.trim()) && value.count {it in '0'..'9'} in 7..15)
