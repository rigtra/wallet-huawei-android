package com.ipandai.edompet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Height of the floating bar. Content and floating buttons keep this much room at the bottom. */
val GLASS_BAR_HEIGHT = 64.dp
/** Gap between the bar and the bottom of the screen (on top of the system nav bar inset). */
val GLASS_BAR_MARGIN = 12.dp

private val Accent = Color(0xFF7FB2FF)
private val Pill = RoundedCornerShape(percent = 50)

/**
 * Frosted-glass surface: translucent dark fill, a light sheen from the top, a hairline rim
 * that is brighter on the top edge, and a soft drop shadow so it looks like it floats.
 */
fun Modifier.glass(shape: Shape): Modifier = this
    .shadow(elevation = 18.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
    .clip(shape)
    .background(Color(0xFF2A2A2E).copy(alpha = 0.72f))
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))))
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.06f))),
        shape = shape,
    )

/**
 * Floating glass navigation: a pill with the tabs plus a round search button.
 * Tapping search morphs the bar into a search field (the keyboard pushes it up).
 */
@Composable
fun GlassBottomBar(
    tab: Int,
    onTabSelected: (Int) -> Unit,
    searching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = searching,
        transitionSpec = {
            (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.92f)) togetherWith
                (fadeOut(spring(stiffness = Spring.StiffnessMedium)) + scaleOut(targetScale = 0.92f))
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "glass-bar",
    ) { isSearching ->
        if (isSearching) {
            SearchRow(query, onQueryChange, onSearchClose)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .height(GLASS_BAR_HEIGHT)
                        .glass(Pill)
                        .padding(6.dp)
                        .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabItem("Wallet", Icons.Filled.CreditCard, Icons.Outlined.CreditCard, tab == 0) { onTabSelected(0) }
                    TabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, tab == 1) { onTabSelected(1) }
                }
                GlassCircleButton(Icons.Outlined.Search, "Search cards", onClick = onSearchOpen)
            }
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    selectedIcon: ImageVector,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(if (selected) Accent else Color.White.copy(alpha = 0.75f), label = "tab-tint")
    Row(
        Modifier
            .fillMaxHeight()
            .clip(Pill)
            .then(
                // The selected tab gets its own raised, lighter capsule (like the reference design)
                if (selected) Modifier
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), Pill)
                else Modifier
            )
            .clickable(role = Role.Tab, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (selected) selectedIcon else icon, contentDescription = if (selected) null else label, tint = tint)
        // Label only on the selected tab; it slides out of the icon
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(label, color = tint, fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun GlassCircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(GLASS_BAR_HEIGHT)
            .glass(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(GLASS_BAR_HEIGHT)
                .glass(Pill)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Merchant name or code", color = Color.White.copy(alpha = 0.45f), fontSize = 16.sp, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Clear search") { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Cancel, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
        GlassCircleButton(Icons.Filled.Close, "Close search") { keyboard?.hide(); onClose() }
    }
}
