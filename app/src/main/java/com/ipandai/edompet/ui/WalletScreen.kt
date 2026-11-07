package com.ipandai.edompet.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.scan.renderCode
import com.ipandai.edompet.wear.WearEngineManager

private val CARD_HEIGHT = 180.dp
private val COLLAPSED_PEEK = 55.dp // iOS used spacing -125 on a 180pt card

@Composable
fun WalletScreen(
    cards: List<MembershipCard>,
    wear: WearEngineManager,
    onSync: (MembershipCard) -> Unit,
    onEdit: (MembershipCard) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showingCode by remember { mutableStateOf<MembershipCard?>(null) }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            "Wallet",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        StatusBar(wear)

        if (cards.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.CreditCard, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No Memberships Yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Tap 'Add Card' tab to scan or upload your cards.", color = Color.Gray, fontSize = 12.sp)
            }
            return@Column
        }

        val peek by animateDpAsState(
            targetValue = if (selectedId == null) COLLAPSED_PEEK else CARD_HEIGHT + 16.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "stack",
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 60.dp)
        ) {
            cards.forEachIndexed { index, card ->
                val isLast = index == cards.lastIndex
                WalletCard(
                    card = card,
                    isExpanded = selectedId == card.id,
                    onSync = { onSync(card) },
                    onEdit = { onEdit(card) },
                    onShowCode = { showingCode = card },
                    modifier = Modifier
                        // Report a shorter height so the next card overlaps this one (Wallet-style stack).
                        // Later children draw on top, matching the iOS zIndex ordering.
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            val h = if (isLast) p.height else peek.roundToPx()
                            layout(p.width, h) { p.place(0, 0) }
                        }
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            selectedId = if (selectedId == card.id) null else card.id
                        },
                )
            }
        }
    }

    showingCode?.let { CodeDialog(it) { showingCode = null } }
}

@Composable
private fun StatusBar(wear: WearEngineManager) {
    val status = wear.connectionStatus.lowercase()
    val badge = when {
        "connected" in status && "not connected" !in status && "no watch" !in status -> Color(0xFF34C759)
        "ok" in status || "sent" in status -> Color(0xFF34C759)
        "checking" in status || "sending" in status -> Color(0xFFFF9500)
        else -> Color(0xFFFF3B30)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(badge))
        Spacer(Modifier.width(6.dp))
        Text(wear.connectionStatus, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { wear.checkDeviceConnection() }) { Text("Refresh", fontSize = 11.sp, color = Color.White) }
        TextButton(
            onClick = { wear.sendPing() },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
        ) { Text("Ping", fontSize = 11.sp, color = Color.White) }
    }
}

@Composable
private fun WalletCard(
    card: MembershipCard,
    isExpanded: Boolean,
    onSync: () -> Unit,
    onEdit: () -> Unit,
    onShowCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val style = card.style
    val textColor = colorFromHex(style.textHex)
    Column(
        modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(style.brush())
            .border(1.dp, textColor.copy(alpha = 0.15f), shape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                card.merchantName,
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Icon(
                if (card.isBarcode) Icons.Filled.ViewWeek else Icons.Filled.QrCode2,
                null,
                tint = textColor.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    card.codeType.uppercase(),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    card.codeValue,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
            }
            if (isExpanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(Icons.Filled.Edit, "Edit", textColor, onEdit)
                    RoundIcon(if (card.isBarcode) Icons.Filled.ViewWeek else Icons.Filled.QrCode2, "Show code", textColor, onShowCode)
                    TextButton(
                        onClick = onSync,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF007AFF)),
                    ) {
                        Icon(Icons.Filled.Watch, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = tint.copy(alpha = 0.18f),
            contentColor = tint,
        ),
    ) { Icon(icon, label, modifier = Modifier.size(18.dp)) }
}

/** Large, high-contrast code for the cashier to scan. */
@Composable
private fun CodeDialog(card: MembershipCard, onDismiss: () -> Unit) {
    val bitmap = remember(card) {
        if (card.isBarcode) renderCode(card.codeValue, true, 900, 300) else renderCode(card.codeValue, false, 700, 700)
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(card.merchantName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(16.dp))
            if (bitmap != null) {
                Image(
                    bitmap.asImageBitmap(),
                    contentDescription = card.codeValue,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().height(if (card.isBarcode) 120.dp else 260.dp),
                )
            } else {
                Text("Can't render this code", color = Color.Red)
            }
            Spacer(Modifier.height(12.dp))
            Text(card.codeValue, fontFamily = FontFamily.Monospace, color = Color.Black)
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}
