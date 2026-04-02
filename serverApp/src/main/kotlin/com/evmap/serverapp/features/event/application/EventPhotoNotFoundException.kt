package com.evmap.serverapp.features.event.application

class EventPhotoNotFoundException(eventId: Long, photoId: Long? = null) : RuntimeException(
    if (photoId == null) "Photo for event $eventId not found"
    else "Photo $photoId for event $eventId not found"
)
