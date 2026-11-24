package com.evmap.serverapp.features.list.domian

data class UserList(
    val id: Long? = null,
    val name: String,
    val userId: Long,
)

internal fun validatedListName(name: String): String = name.trim().also {
    require(it.length in 1..20) { "Enter a list name from 1 to 20 characters" }
}
