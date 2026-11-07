package com.ipandai.edompet.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Camera scan + photo decode, shared by the "+" sheet and the card editor. */
class CodeCapture(val scan: () -> Unit, val pickPhoto: () -> Unit)

sealed interface CaptureResult {
    data class Found(val code: DecodedCode) : CaptureResult
    data object NothingInPhoto : CaptureResult
    data class Failed(val message: String) : CaptureResult
}

@Composable
fun rememberCodeCapture(onResult: (CaptureResult) -> Unit): CodeCapture {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = rememberUpdatedState(onResult)

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult // user backed out
        callback.value(CaptureResult.Found(DecodedCode(contents, codeTypeFor(result.formatName))))
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = withContext(Dispatchers.Default) { runCatching { decodeFromImage(context, uri) } }
            decoded.onSuccess { code ->
                if (code != null) {
                    callback.value(CaptureResult.Found(code))
                } else {
                    callback.value(CaptureResult.NothingInPhoto)
                }
            }.onFailure { callback.value(CaptureResult.Failed(it.message ?: "Couldn't open that photo")) }
        }
    }

    return remember {
        CodeCapture(
            scan = {
                scanLauncher.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(SCAN_FORMATS.map { it.name })
                        .setPrompt("Point the camera at the QR code or barcode")
                        .setBeepEnabled(false)
                        .setOrientationLocked(false)
                )
            },
            pickPhoto = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}
