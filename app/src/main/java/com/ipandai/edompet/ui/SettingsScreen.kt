package com.ipandai.edompet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipandai.edompet.BuildConfig
import com.ipandai.edompet.wear.WatchState
import com.ipandai.edompet.wear.WearEngineManager

@Composable
fun SettingsScreen(
    wear: WearEngineManager,
    pendingCount: Int,
    onWatchStatusClick: () -> Unit,
    onSyncAll: () -> Unit,
) {
    var pingResult by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Settings",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )

        SectionHeader("Watch")
        Row(
            Icons.Outlined.Watch,
            watchStateTitle(wear.state, wear.deviceName),
            when (wear.state) {
                WatchState.CONNECTED -> "Cards sync automatically"
                WatchState.CHECKING -> "Checking…"
                else -> "Tap to see how to connect"
            },
            trailingDot = if (wear.state == WatchState.CONNECTED) Color(0xFF34C759) else Color(0xFFFF9F0A),
            onClick = onWatchStatusClick,
        )
        Row(
            Icons.Outlined.Sync,
            "Sync all cards to watch",
            if (pendingCount == 0) "Everything is up to date" else "$pendingCount waiting to sync",
            onClick = onSyncAll,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.08f))
        SectionHeader("Developer")
        Row(
            Icons.Outlined.NetworkPing,
            "Ping watch app",
            pingResult ?: "Checks whether eDompet is installed and running on the watch",
            onClick = { pingResult = "Pinging…"; wear.ping { pingResult = it } },
        )
        Row(Icons.Outlined.Info, "Last Wear Engine response", wear.debugLog, onClick = null)

        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.08f))
        SectionHeader("About")
        Row(Icons.Outlined.Info, "Version", BuildConfig.VERSION_NAME, onClick = null)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun Row(icon: ImageVector, title: String, subtitle: String?, trailingDot: Color? = null, onClick: (() -> Unit)?) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 3) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = trailingDot?.let { { Box(Modifier.size(10.dp).clip(CircleShape).background(it)) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
