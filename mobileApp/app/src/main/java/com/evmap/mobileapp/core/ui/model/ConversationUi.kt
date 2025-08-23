package com.evmap.mobileapp.core.ui.model

import core.ui.model.MessageUi

data class ConversationUi(
    val messages: List<MessageUi> = emptyList(),
    val input: String = "",
    val isRecording: Boolean = false,
    val recordSeconds: Int = 0
)