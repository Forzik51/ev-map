package com.evmap.serverapp.shared.email.application

import com.evmap.serverapp.shared.email.domain.EmailMessage

interface EmailSenderPort {
    fun send(message: EmailMessage)
}
