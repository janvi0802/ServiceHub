package com.example.servicehub.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * ServiceHub colours.
 * Idea (like Urban Company / Airbnb): one calm brand colour, white cards on a soft grey page,
 * dark grey text instead of pure black, and soft tinted status colours.
 */

// Brand
val BrandBlue = Color(0xFF3355E0)       // main brand colour (buttons, links, customer side)
val BrandBlueDark = Color(0xFF2440B8)   // pressed / gradient end
val BrandBlueSoft = Color(0xFFEEF2FF)   // very light tint behind blue icons
val BrandOrange = Color(0xFFEA7A0C)     // provider side accent
val BrandOrangeSoft = Color(0xFFFFF3E6)

// Neutrals
val BackgroundGrey = Color(0xFFF5F6FA)  // page background
val Line = Color(0xFFE6E8EF)            // thin borders and dividers
val TextDark = Color(0xFF14171F)        // main text
val TextMuted = Color(0xFF6B7280)       // secondary text

// One colour per booking status (used as text colour on a light tint of itself)
val StatusRequested = Color(0xFFD97706)   // amber: waiting for provider
val StatusAccepted = Color(0xFF2563EB)    // blue: confirmed
val StatusInProgress = Color(0xFF7C3AED)  // violet: work is happening
val StatusCompleted = Color(0xFF16A34A)   // green: done
val StatusRejected = Color(0xFFDC2626)    // red: provider said no
val StatusCancelled = Color(0xFF64748B)   // slate: cancelled
