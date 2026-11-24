package com.evmap.mobileapp.feature.events.ui

import java.util.Locale

internal fun Double?.displayRating(locale: Locale = Locale.getDefault()): String =
    if (this == null) "—" else String.format(locale, "%.1f", this)
