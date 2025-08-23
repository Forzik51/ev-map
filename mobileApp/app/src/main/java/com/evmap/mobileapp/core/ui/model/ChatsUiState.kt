package com.evmap.mobileapp.core.ui.model

import core.ui.model.ChatUi


data class ChatsUiState(
    val items: List<ChatUi> = emptyList(),
    val isSelecting: Boolean = false,
    val selection: Set<String> = emptySet()
)