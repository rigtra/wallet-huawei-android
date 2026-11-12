package com.ipandai.edompet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.data.search
import com.ipandai.edompet.wear.WearEngineManager

private val COLLAPSED_PEEK = 64.dp

/**
 * Wallet tab. Tapping a card opens its code full-screen with a shared-element transition;
 * long-press opens quick actions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WalletScreen(
    cards: List<MembershipCard>,
    /** Search text from the glass bar; blank shows the whole wallet. */
    query: String,
    openCardId: String?,
    wear: WearEngineManager,
    onOpen: (String) -> Unit,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (MembershipCard) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onWatchStatusClick: () -> Unit,
) {
    SharedTransitionLayout(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            targetState = openCardId,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "wallet",
        ) { id ->
            val open = id?.let { target -> cards.firstOrNull { it.id == target } }
            if (open == null) {
                WalletList(
                    cards = cards,
                    query = query,
                    wear = wear,
                    shared = this@SharedTransitionLayout,
                    visibility = this@AnimatedContent,
                    onOpen = onOpen,
                    onAdd = onAdd,
                    onEdit = onEdit,
                    onTogglePin = onTogglePin,
                    onDelete = onDelete,
                    onWatchStatusClick = onWatchStatusClick,
                )
            } else {
                CardDetail(
                    card = open,
                    cardModifier = with(this@SharedTransitionLayout) {
                        Modifier.sharedElement(
                            rememberSharedContentState("card-${open.id}"),
                            animatedVisibilityScope = this@AnimatedContent,
                            boundsTransform = { _, _ -> spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow) },
                        )
                    },
                    onClose = onClose,
                    onTogglePin = { onTogglePin(open.id) },
                    onEdit = { onEdit(open) },
                    onDelete = { onDelete(open.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WalletList(
    cards: List<MembershipCard>,
    query: String,
    wear: WearEngineManager,
    shared: SharedTransitionScope,
    visibility: AnimatedVisibilityScope,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (MembershipCard) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onWatchStatusClick: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                "Wallet",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )
            WatchStatusChip(
                state = wear.state,
                deviceName = wear.deviceName,
                onClick = onWatchStatusClick,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp),
            )

            if (cards.isEmpty()) {
                EmptyWallet(onAdd)
                return@Column
            }

            val searching = query.isNotBlank()
            val shown = remember(cards, query) { cards.search(query) }
            if (searching && shown.isEmpty()) {
                NoResults(query)
                return@Column
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // Bottom padding keeps the last card clear of the "Add card" button and the glass bar
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 180.dp)
            ) {
                if (searching) {
                    Text(
                        if (shown.size == 1) "1 card" else "${shown.size} cards",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
                shown.forEachIndexed { index, card ->
                    var menuOpen by remember(card.id) { mutableStateOf(false) }
                    // Search results are laid out in full (no stack) so the code number is visible
                    val collapsed = !searching && index != shown.lastIndex
                    Box(
                        Modifier
                            // Report a shorter height so the next card overlaps this one (Wallet-style stack).
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                val h = if (collapsed) COLLAPSED_PEEK.roundToPx() else p.height
                                layout(p.width, h) { p.place(0, 0) }
                            }
                            .then(if (searching) Modifier.padding(bottom = 12.dp) else Modifier)
                    ) {
                        CardFace(
                            card,
                            sync = card.syncBadge(),
                            modifier = with(shared) {
                                Modifier.sharedElement(
                                    rememberSharedContentState("card-${card.id}"),
                                    animatedVisibilityScope = visibility,
                                    boundsTransform = { _, _ -> spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow) },
                                )
                            }.clip(CARD_SHAPE).combinedClickable(
                                onClickLabel = "Show code",
                                onLongClickLabel = "More actions",
                                onLongClick = { menuOpen = true },
                                onClick = { onOpen(card.id) },
                            ),
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            // Stacked cards only show a 64dp strip; open the menu right under it
                            offset = DpOffset(16.dp, if (collapsed) COLLAPSED_PEEK - CARD_HEIGHT else 0.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (card.pinned) "Unpin" else "Pin to top") },
                                leadingIcon = { Icon(if (card.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null) },
                                onClick = { menuOpen = false; onTogglePin(card.id) },
                            )
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { menuOpen = false; onEdit(card) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menuOpen = false; onDelete(card.id) },
                            )
                        }
                    }
                }
                if (!searching) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Tap a card to show its code. Press and hold for more options.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No cards match \u201C${query.trim()}\u201D", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Try part of the store name or a few digits of the code.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
        // Keep the message above the keyboard + search bar
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun EmptyWallet(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CreditCard, null, tint = Color.Gray, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("No cards yet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Scan the QR code or barcode on a membership card to keep it here.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.size(8.dp))
            Text("Add your first card")
        }
    }
}
