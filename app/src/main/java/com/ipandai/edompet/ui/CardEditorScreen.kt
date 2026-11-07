package com.ipandai.edompet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ipandai.edompet.data.CODE_TYPE_BARCODE
import com.ipandai.edompet.data.CODE_TYPE_QR
import com.ipandai.edompet.data.DEFAULT_STYLE_ID
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.data.styleFor
import com.ipandai.edompet.scan.CaptureResult
import com.ipandai.edompet.scan.DecodedCode
import com.ipandai.edompet.scan.rememberCodeCapture

/**
 * Full-screen add/edit form with a live preview of the card at the top.
 * [existing] == null means a new card; [prefill] carries a code that was just scanned.
 */
@Composable
fun CardEditorScreen(
    existing: MembershipCard?,
    prefill: DecodedCode?,
    onSave: (MembershipCard) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.merchantName ?: "") }
    var codeType by rememberSaveable { mutableStateOf(existing?.codeType ?: prefill?.codeType ?: CODE_TYPE_QR) }
    var codeValue by rememberSaveable { mutableStateOf(existing?.codeValue ?: prefill?.value ?: "") }
    var styleId by rememberSaveable { mutableStateOf(existing?.styleId ?: DEFAULT_STYLE_ID) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val capture = rememberCodeCapture { result ->
        when (result) {
            is CaptureResult.Found -> { codeValue = result.code.value; codeType = result.code.codeType; captureError = null }
            CaptureResult.NothingInPhoto -> captureError = "No QR code or barcode found in that photo. Try a sharper, closer shot."
            is CaptureResult.Failed -> captureError = result.message
        }
    }

    val isBarcode = codeType == CODE_TYPE_BARCODE
    val trimmedName = name.trim()
    val trimmedCode = codeValue.trim()
    val canSave = trimmedName.isNotEmpty() && trimmedCode.isNotEmpty()
    val dirty = existing?.let {
        it.merchantName != trimmedName || it.codeType != codeType || it.codeValue != trimmedCode || it.styleId != styleId
    } ?: (trimmedName.isNotEmpty() || trimmedCode.isNotEmpty())


    fun save() {
        if (!canSave) return
        val base = existing ?: MembershipCard(merchantName = trimmedName, codeType = codeType, codeValue = trimmedCode)
        onSave(base.copy(merchantName = trimmedName, codeType = codeType, codeValue = trimmedCode, styleId = styleId))
    }

    fun close() = if (dirty) confirmDiscard = true else onClose()
    BackHandler { close() }

    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // A new card usually arrives with a scanned code: jump straight to naming it.
        if (existing == null) runCatching { nameFocus.requestFocus() }
    }

    // Surface sets the content color (white on black); a bare Column would default to black text
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::close) { Icon(Icons.Filled.Close, "Close") }
            Text(
                if (existing == null) "New card" else "Edit card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = ::save, enabled = canSave) { Text("Save", fontWeight = FontWeight.Bold) }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Live preview: what you see is exactly what lands in the wallet
            CardFace(
                merchantName = trimmedName,
                codeType = codeType,
                codeValue = trimmedCode,
                style = styleFor(styleId),
                modifier = Modifier.padding(top = 4.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Merchant name") },
                placeholder = { Text("e.g. Indomaret") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Code type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(CODE_TYPE_QR, CODE_TYPE_BARCODE).forEachIndexed { i, type ->
                        SegmentedButton(
                            selected = codeType == type,
                            onClick = { codeType = type },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(type) }
                    }
                }
            }

            OutlinedTextField(
                value = codeValue,
                onValueChange = { codeValue = it; captureError = null },
                label = { Text(if (isBarcode) "Barcode number" else "QR code content") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                isError = captureError != null,
                supportingText = captureError?.let { hint -> { Text(hint, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = capture.scan, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.QrCodeScanner, null); Spacer(Modifier.padding(4.dp)); Text("Scan")
                }
                OutlinedButton(onClick = capture.pickPhoto, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.padding(4.dp)); Text("From photo")
                }
            }

            CardStylePicker(styleId) { styleId = it }

            if (onDelete != null) {
                TextButton(
                    // No confirm dialog: deleting shows an Undo snackbar, same as everywhere else
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Outlined.Delete, null); Spacer(Modifier.padding(4.dp)); Text("Delete card")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to this card won't be saved.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}
