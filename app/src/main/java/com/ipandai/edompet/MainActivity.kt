package com.ipandai.edompet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ipandai.edompet.wear.WatchState
import com.ipandai.edompet.data.CardRepository
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.data.WalletStore
import com.ipandai.edompet.scan.CaptureResult
import com.ipandai.edompet.scan.DecodedCode
import com.ipandai.edompet.scan.rememberCodeCapture
import com.ipandai.edompet.ui.AddSourceSheet
import com.ipandai.edompet.ui.CardEditorScreen
import com.ipandai.edompet.ui.SettingsScreen
import com.ipandai.edompet.ui.WalletScreen
import com.ipandai.edompet.ui.WatchStatusSheet
import com.ipandai.edompet.wear.WearEngineManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repo: CardRepository
    private lateinit var wear: WearEngineManager
    private lateinit var store: WalletStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repo = CardRepository(applicationContext)
        wear = WearEngineManager(this)
        store = WalletStore(repo, wear)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color(0xFF1C1C1E))) {
                MainScreen(store, wear)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Silent check: never pops the Huawei permission prompt on its own.
        // The prompt only appears when the user taps "Allow access" in the watch sheet.
        wear.refresh(askPermission = false)
    }
}

/** What the editor overlay is doing right now. */
private data class EditorTarget(val existing: MembershipCard?, val prefill: DecodedCode?)

@Composable
private fun MainScreen(store: WalletStore, wear: WearEngineManager) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showWatchSheet by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(id: String) {
        val deleted = store.delete(id) ?: return
        if (openCardId == id) openCardId = null
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "${deleted.card.merchantName} deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) store.undoDelete(deleted)
        }
    }

    fun showMessage(text: String) = scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(text) }

    val capture = rememberCodeCapture { result ->
        when (result) {
            is CaptureResult.Found -> editor = EditorTarget(existing = null, prefill = result.code)
            CaptureResult.NothingInPhoto -> showMessage("No QR code or barcode found in that photo")
            is CaptureResult.Failed -> showMessage(result.message)
        }
    }

    BackHandler(openCardId != null && editor == null) { openCardId = null }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Black,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                // In the Scaffold slot so the snackbar pushes it up instead of covering it
                AnimatedVisibility(
                    tab == 0 && openCardId == null && store.cards.isNotEmpty(),
                    enter = fadeIn(), exit = fadeOut(),
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddSheet = true },
                        icon = { Icon(Icons.Filled.Add, null) },
                        text = { Text("Add card") },
                    )
                }
            },
            bottomBar = {
                // Hidden while a code is on screen: nothing should compete with it at checkout
                AnimatedVisibility(openCardId == null, enter = fadeIn(), exit = fadeOut()) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0, onClick = { tab = 0 },
                            icon = { Icon(if (tab == 0) Icons.Filled.CreditCard else Icons.Outlined.CreditCard, null) },
                            label = { Text("Wallet") },
                        )
                        NavigationBarItem(
                            selected = tab == 1, onClick = { tab = 1 },
                            icon = { Icon(if (tab == 1) Icons.Filled.Settings else Icons.Outlined.Settings, null) },
                            label = { Text("Settings") },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = if (openCardId == null) padding.calculateBottomPadding() else 0.dp)) {
                when (tab) {
                    0 -> WalletScreen(
                        cards = store.cards,
                        openCardId = openCardId,
                        wear = wear,
                        onOpen = { id -> store.markUsed(id); openCardId = id },
                        onClose = { openCardId = null },
                        onAdd = { showAddSheet = true },
                        onEdit = { card -> editor = EditorTarget(existing = card, prefill = null) },
                        onTogglePin = { id -> store.togglePin(id) },
                        onDelete = ::deleteWithUndo,
                        onWatchStatusClick = { showWatchSheet = true },
                    )
                    else -> SettingsScreen(
                        wear = wear,
                        pendingCount = store.pendingCount,
                        onWatchStatusClick = { showWatchSheet = true },
                        onSyncAll = {
                            store.resyncAll()
                            showMessage(
                                if (wear.state == WatchState.CONNECTED) "Sending all cards to your watch…"
                                else "Cards will sync when your watch connects"
                            )
                        },
                    )
                }
            }
        }

        // Full-screen editor slides up over everything, including the bottom bar
        AnimatedVisibility(
            visible = editor != null,
            enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { it } + fadeOut(),
        ) {
            // Keep showing the last target while the exit animation runs
            val target = remember { mutableStateOf<EditorTarget?>(null) }
            if (editor != null) target.value = editor
            target.value?.let { t ->
                CardEditorScreen(
                    existing = t.existing,
                    prefill = t.prefill,
                    onSave = { card ->
                        if (t.existing == null) store.add(card) else store.update(card)
                        editor = null
                        if (t.existing == null) { tab = 0; openCardId = null }
                    },
                    onDelete = t.existing?.let { existing -> { editor = null; deleteWithUndo(existing.id) } },
                    onClose = { editor = null },
                )
            }
        }
    }

    if (showAddSheet) {
        AddSourceSheet(
            onScan = { showAddSheet = false; capture.scan() },
            onPhoto = { showAddSheet = false; capture.pickPhoto() },
            onManual = { showAddSheet = false; editor = EditorTarget(existing = null, prefill = null) },
            onDismiss = { showAddSheet = false },
        )
    }

    if (showWatchSheet) {
        WatchStatusSheet(
            state = wear.state,
            deviceName = wear.deviceName,
            pendingCount = store.pendingCount,
            onConnect = { wear.refresh(askPermission = true) },
            onDismiss = { showWatchSheet = false },
        )
    }
}
