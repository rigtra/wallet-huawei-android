package com.ipandai.edompet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
            "Card color",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(cardStyles) { style ->
                val selected = style.id == selectedId
                // 48dp touch target. The selection ring sits OUTSIDE the swatch with a gap so the
                // swatch itself is never clipped or stroked differently.
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .semantics {
                            contentDescription = style.name
                            this.selected = selected
                        }
                        .then(
                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable(role = Role.RadioButton) { onSelect(style.id) }
                        .padding(6.dp)
                        .background(style.brush(), CircleShape)
                        // Same thin outline on every swatch so dark ones stay visible on black
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )
            }
        }
    }
}
