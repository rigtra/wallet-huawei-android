package com.ipandai.edompet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import com.ipandai.edompet.data.CardStyle
import com.ipandai.edompet.data.cardStyles

fun CardStyle.brush(): Brush = Brush.verticalGradient(listOf(colorFromHex(topHex), colorFromHex(bottomHex)))

/** Same parsing rules as the iOS `Color(hex:)` extension (3, 6 or 8 hex digits). */
fun colorFromHex(hex: String): Color {
    val clean = hex.filter { it.isLetterOrDigit() }
    val v = clean.toLongOrNull(16) ?: return Color.Black
    return when (clean.length) {
        3 -> Color(
            red = ((v shr 8) and 0xF).toInt() * 17,
            green = ((v shr 4) and 0xF).toInt() * 17,
            blue = (v and 0xF).toInt() * 17,
        )
        6 -> Color(0xFF000000 or v)
        8 -> Color(v) // AARRGGBB
        else -> Color.Black
    }
}

@Composable
fun CardStylePicker(selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Card Background Color",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cardStyles) { style ->
                val selected = style.id == selectedId
                // Selection ring sits OUTSIDE the swatch with a gap, so the swatch itself is never
                // clipped or stroked differently. border(0.dp) used to draw a clipped hairline,
                // which made unselected outlines look uneven.
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onSelect(style.id) }
                        .padding(5.dp)
                        .background(style.brush(), CircleShape)
                        // Same thin outline on every swatch so dark ones stay visible on black
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )
            }
        }
    }
}
