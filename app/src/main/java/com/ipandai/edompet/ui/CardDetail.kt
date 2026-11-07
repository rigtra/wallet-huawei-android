package com.ipandai.edompet.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.scan.renderCode

/**
 * The one screen that matters at checkout: big code, white background, max brightness.
 * [cardModifier] carries the shared-element transition from the wallet stack.
 */
@Composable
fun CardDetail(
    card: MembershipCard,
    cardModifier: Modifier,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    BoostBrightness()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onTogglePin) {
                Icon(
                    if (card.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    if (card.pinned) "Unpin" else "Pin to top",
                    tint = Color.White,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit card", tint = Color.White) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete card", tint = Color.White) }
        }

        CardFace(card, modifier = cardModifier)

        Spacer(Modifier.height(20.dp))
        CodePanel(card)

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (card.synced) Icons.Filled.Watch else Icons.Filled.Schedule,
                null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (card.synced) "On your watch" else "Will appear on your watch once it's connected",
                color = Color.Gray,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CodePanel(card: MembershipCard) {
    val bitmap = remember(card.codeValue, card.codeType) {
        if (card.isBarcode) renderCode(card.codeValue, true, 1000, 340) else renderCode(card.codeValue, false, 800, 800)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bitmap != null) {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = "${card.codeType} for ${card.merchantName}",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = if (card.isBarcode) Modifier.fillMaxWidth().height(120.dp)
                else Modifier.fillMaxWidth(0.82f).aspectRatio(1f),
            )
        } else {
            Text("This code can't be displayed. Check the number in Edit.", color = Color(0xFFB00020), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            card.codeValue,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )
    }
}

/** Max screen brightness + keep the screen on while the code is visible, restored on exit. */
@Composable
private fun BoostBrightness() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(Unit) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = previous }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
