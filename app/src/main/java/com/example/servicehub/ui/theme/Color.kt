package com.example.servicehub.ui.theme

import androidx.compose.ui.graphics.Color

// Brand colors for ServiceHub (a calm blue with an orange accent)
val BrandBlue = Color(0xFF1E5EFF)
val BrandBlueDark = Color(0xFF0B3BB5)
val BrandOrange = Color(0xFFFF8A00)
val BackgroundGrey = Color(0xFFF6F7FB)
val TextDark = Color(0xFF1B1D29)

// One color per booking status, so each status is easy to recognise
val StatusRequested = Color(0xFFF2A100)   // amber: waiting for provider
val StatusAccepted = Color(0xFF1E5EFF)    // blue: confirmed
val StatusInProgress = Color(0xFF7B3FE4)  // purple: work is happening
val StatusCompleted = Color(0xFF1E9E5A)   // green: done
val StatusRejected = Color(0xFFD93025)    // red: provider said no
val StatusCancelled = Color(0xFF7A7F8C)   // grey: customer cancelled
