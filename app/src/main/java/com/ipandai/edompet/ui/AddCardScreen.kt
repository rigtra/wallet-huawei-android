package com.ipandai.edompet.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipandai.edompet.data.CODE_TYPE_QR
import com.ipandai.edompet.data.DEFAULT_STYLE_ID
import com.ipandai.edompet.data.MembershipCard
import com.ipandai.edompet.scan.SCAN_FORMATS
import com.ipandai.edompet.scan.codeTypeFor
import com.ipandai.edompet.scan.decodeFromImage
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddCardScreen(onAdd: (MembershipCard) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var merchantName by rememberSaveable { mutableStateOf("") }
    var codeValue by rememberSaveable { mutableStateOf("") }
    var codeType by rememberSaveable { mutableStateOf(CODE_TYPE_QR) }
    var styleId by rememberSaveable { mutableStateOf(DEFAULT_STYLE_ID) }
    var alertMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        codeValue = contents
        codeType = codeTypeFor(result.formatName)
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = withContext(Dispatchers.Default) { runCatching { decodeFromImage(context, uri) } }
            decoded.onSuccess { code ->
                if (code != null) {
                    codeValue = code.value
                    codeType = code.codeType
                    alertMessage = "Successfully detected ${code.codeType}: ${code.value}"
                } else {
                    alertMessage = "No valid QR Code or barcode found in selected image."
                }
            }.onFailure { alertMessage = "Failed to load image: ${it.message}" }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Add Membership", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Text("Quick Scan / Upload", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(
                onClick = {
                    scanLauncher.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(SCAN_FORMATS.map { it.name })
                            .setPrompt("Point the camera at a QR code or barcode")
                            .setBeepEnabled(false)
                            .setOrientationLocked(false)
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Scan Camera")
            }
            FilledTonalButton(
                onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Photo, null); Spacer(Modifier.width(6.dp)); Text("Upload Photo")
            }
        }

        Text("Card Details", style = MaterialTheme.typography.labelLarge)
        CardFields(
            merchantName, { merchantName = it },
            codeType, { codeType = it },
            codeValue, { codeValue = it },
            styleId, { styleId = it },
        )

        Button(
            onClick = {
                val name = merchantName.trim()
                onAdd(MembershipCard(merchantName = name, codeType = codeType, codeValue = codeValue, styleId = styleId))
                merchantName = ""; codeValue = ""; codeType = CODE_TYPE_QR
            },
            enabled = merchantName.isNotBlank() && codeValue.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.AddCircle, null); Spacer(Modifier.width(6.dp)); Text("Add & Send to Watch", fontWeight = FontWeight.Bold)
        }
    }

    alertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text("Scan Status") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { alertMessage = null }) { Text("OK") } },
        )
    }
}
