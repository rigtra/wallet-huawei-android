package com.ipandai.edompet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipandai.edompet.data.CardStyle
import com.ipandai.edompet.data.MembershipCard

val CARD_HEIGHT = 180.dp
val CARD_SHAPE = RoundedCornerShape(16.dp)

enum class SyncBadge { ON_WATCH, PENDING, HIDDEN }

@Composable
fun CardFace(card: MembershipCard, modifier: Modifier = Modifier, sync: SyncBadge = SyncBadge.HIDDEN) {
    CardFace(
        merchantName = card.merchantName,
        codeType = card.codeType,
        codeValue = card.codeValue,
        style = card.style,
        pinned = card.pinned,
        sync = sync,
        modifier = modifier,
    )
}

/** The visual card. Used in the wallet stack, the detail screen and the editor's live preview. */
@Composable
fun CardFace(
    merchantName: String,
    codeType: String,
    codeValue: String,
    style: CardStyle,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    sync: SyncBadge = SyncBadge.HIDDEN,
) {
    val textColor = colorFromHex(style.textHex)
    val isBarcode = codeType.lowercase().contains("barcode")
    Column(
        modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .shadow(8.dp, CARD_SHAPE)
            .clip(CARD_SHAPE)
            .background(style.brush())
            .border(1.dp, textColor.copy(alpha = 0.15f), CARD_SHAPE)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                merchantName.ifBlank { "Merchant name" },
                color = if (merchantName.isBlank()) textColor.copy(alpha = 0.5f) else textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (pinned) {
                Icon(Icons.Filled.PushPin, "Pinned", tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                if (isBarcode) Icons.Filled.ViewWeek else Icons.Filled.QrCode2,
                null,
                tint = textColor.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    codeType.uppercase(),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    codeValue.ifBlank { "Code number" },
                    color = if (codeValue.isBlank()) textColor.copy(alpha = 0.5f) else textColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (sync) {
                SyncBadge.ON_WATCH -> Icon(Icons.Filled.Watch, "On your watch", tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                SyncBadge.PENDING -> Icon(Icons.Filled.Schedule, "Waiting to sync", tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                SyncBadge.HIDDEN -> Unit
            }
        }
    }
}

fun MembershipCard.syncBadge(): SyncBadge = if (synced) SyncBadge.ON_WATCH else SyncBadge.PENDING
