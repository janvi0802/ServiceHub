package com.example.servicehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A colored circle with the person's initials, e.g. "Janvi Panchal" -> "JP". */
@Composable
fun Avatar(name: String, size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

/** First letter of the first two words, in capitals. Empty name -> "?". */
fun initialsOf(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return "?"
    return words.take(2).joinToString("") { it.first().uppercase() }
}
