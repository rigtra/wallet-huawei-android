package com.ipandai.edompet.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipandai.edompet.wear.WatchState

private const val HEALTH_PACKAGE = "com.huawei.health"

private val Green = Color(0xFF34C759)
private val Orange = Color(0xFFFF9F0A)
private val Grey = Color(0xFF8E8E93)

fun watchStateTitle(state: WatchState, deviceName: String?): String = when (state) {
    WatchState.CHECKING -> "Looking for your watch…"
    WatchState.CONNECTED -> "Connected to ${deviceName ?: "your watch"}"
    WatchState.NO_WATCH -> "Watch not connected"
    WatchState.HEALTH_MISSING -> "Huawei Health needed"
    WatchState.NEEDS_PERMISSION -> "Allow watch access"
    WatchState.ERROR -> "Watch unavailable"
}

private fun dotColor(state: WatchState) = when (state) {
    WatchState.CONNECTED -> Green
    WatchState.CHECKING -> Grey
    else -> Orange
}

/** Calm, human status in place of raw error text. Tapping it explains what to do. */
@Composable
fun WatchStatusChip(state: WatchState, deviceName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(watchStateTitle(state, deviceName)) },
        leadingIcon = {
            Box {
                Icon(Icons.Filled.Watch, null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor(state))
                )
            }
        },
        modifier = modifier.height(36.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchStatusSheet(
    state: WatchState,
    deviceName: String?,
    pendingCount: Int,
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor(state)))
                Spacer(Modifier.width(10.dp))
                Text(watchStateTitle(state, deviceName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))

            val body = when (state) {
                WatchState.CONNECTED ->
                    if (pendingCount == 0) "All your cards are on your watch. New and edited cards sync automatically."
                    else "Sending $pendingCount ${if (pendingCount == 1) "change" else "changes"} to your watch…"
                WatchState.CHECKING -> "Hang on, checking Huawei Health for a paired watch."
                WatchState.HEALTH_MISSING -> "eDompet sends cards to your watch through the Huawei Health app. Install it, then pair your watch there."
                WatchState.NEEDS_PERMISSION -> "Let eDompet send cards to your watch. You'll see a Huawei Health prompt."
                WatchState.NO_WATCH -> "Make sure your watch is on, nearby, and connected in Huawei Health."
                WatchState.ERROR -> "Something went wrong talking to Huawei Health. Try again in a moment."
            }
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (state != WatchState.CONNECTED && state != WatchState.CHECKING) {
                Spacer(Modifier.height(20.dp))
                Text("To show cards on your watch", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Install Huawei Health on this phone",
                    "Pair your watch in Huawei Health",
                    "Allow eDompet to access your watch",
                    "Install eDompet on your watch",
                ).forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("${i + 1}.", modifier = Modifier.width(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(if (state == WatchState.CONNECTED) "Done" else "Not now") }
                when (state) {
                    WatchState.HEALTH_MISSING -> Button(onClick = { openHealthStore(context) }) { Text("Get Huawei Health") }
                    WatchState.NEEDS_PERMISSION -> Button(onClick = onConnect) { Text("Allow access") }
                    WatchState.NO_WATCH, WatchState.ERROR -> Button(onClick = onConnect) { Text("Try again") }
                    else -> Unit
                }
            }
        }
    }
}

private fun openHealthStore(context: android.content.Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$HEALTH_PACKAGE"))
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://consumer.huawei.com/en/mobileservices/health/")))
    }
}
