package com.ipandai.edompet.wear

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.PingCallback
import com.huawei.wearengine.p2p.SendCallback
import com.ipandai.edompet.data.MembershipCard
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

enum class WatchState { CHECKING, CONNECTED, NO_WATCH, HEALTH_MISSING, NEEDS_PERMISSION, ERROR }

/**
 * Talks to the Huawei watch through the Huawei Health app (must be installed and the watch
 * paired there). The phone app's package name + signing certificate SHA-256 must be
 * registered for the Wear Engine service in the Huawei Developer console.
 */
class WearEngineManager(private val activity: Activity) {

    var state by mutableStateOf(WatchState.CHECKING)
        private set
    var deviceName by mutableStateOf<String?>(null)
        private set

    /** Raw technical detail of the last call, shown only in Settings > Developer. */
    var debugLog by mutableStateOf("-")
        private set

    /** Called on the main thread whenever a watch becomes connected. */
    var onConnected: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private var targetDevice: Device? = null

    private val p2pClient: P2pClient = HiWear.getP2pClient(activity)
        .setPeerPkgName(WATCH_PKG_NAME)
        .setPeerFingerPrint(WATCH_FINGERPRINT)

    /**
     * Re-checks the connection. Only shows the Huawei permission prompt when [askPermission]
     * is true (i.e. the user tapped "Connect watch"), never on its own.
     */
    fun refresh(askPermission: Boolean) {
        onMain { state = WatchState.CHECKING }
        HiWear.getAuthClient(activity).checkPermission(Permission.DEVICE_MANAGER)
            .addOnSuccessListener { granted ->
                when {
                    granted == true -> findConnectedDevice()
                    askPermission -> requestPermission()
                    else -> onMain { state = WatchState.NEEDS_PERMISSION }
                }
            }
            .addOnFailureListener { e -> handleError(e) }
    }

    private fun requestPermission() {
        val callback = object : AuthCallback {
            override fun onOk(permissions: Array<out Permission>?) = findConnectedDevice()
            override fun onCancel() = onMain { state = WatchState.NEEDS_PERMISSION }
        }
        HiWear.getAuthClient(activity)
            .requestPermission(callback, Permission.DEVICE_MANAGER)
            .addOnFailureListener { e -> handleError(e) }
    }

    private fun findConnectedDevice() {
        HiWear.getDeviceClient(activity).bondedDevices
            .addOnSuccessListener { devices ->
                val connected = devices?.firstOrNull { it.isConnected }
                onMain {
                    targetDevice = connected
                    deviceName = connected?.name
                    state = if (connected != null) WatchState.CONNECTED else WatchState.NO_WATCH
                    debugLog = "Bonded: ${devices?.size ?: 0}, connected: ${connected?.name ?: "none"}"
                    if (connected != null) onConnected?.invoke()
                }
            }
            .addOnFailureListener { e -> handleError(e) }
    }

    private fun handleError(e: Exception) {
        val msg = e.message ?: e.javaClass.simpleName
        onMain {
            debugLog = msg
            state = if (msg.contains("Health app not exist", ignoreCase = true)) WatchState.HEALTH_MISSING else WatchState.ERROR
        }
    }

    /** Developer tool: checks whether the watch app is installed / running. */
    fun ping(onResult: (String) -> Unit) {
        val device = targetDevice ?: return onResult("Watch not connected")
        val callback = PingCallback { code ->
            val text = when (code) {
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_RUNNING -> "Watch app is running"
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_RUNNING -> "Watch app installed, not running"
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_EXIT -> "Watch app not installed"
                else -> "Ping failed (code $code)"
            }
            onMain { debugLog = text; onResult(text) }
        }
        p2pClient.ping(device, callback)
            .addOnFailureListener { e -> onMain { debugLog = e.message ?: "Ping failed"; onResult(debugLog) } }
    }

    /**
     * Sends a card. Payload keeps the iOS keys (merchantName, codeType, codeValue,
     * backgroundColor) and adds id, gradientColors and textColor.
     */
    fun sendCard(card: MembershipCard, onResult: (Boolean) -> Unit) {
        val style = card.style
        val json = JSONObject()
            .put("action", "upsert")
            .put("id", card.id)
            .put("merchantName", card.merchantName)
            .put("codeType", if (card.isBarcode) "barcode" else "qrcode")
            .put("codeValue", card.codeValue)
            .put("backgroundColor", style.solidHex)
            .put("gradientColors", JSONArray(listOf(style.topHex, style.bottomHex)))
            .put("textColor", style.textHex)
        send(json, onResult)
    }

    /** Tells the watch to remove a card the user deleted on the phone. */
    fun sendDelete(cardId: String, onResult: (Boolean) -> Unit) {
        send(JSONObject().put("action", "delete").put("id", cardId), onResult)
    }

    private fun send(json: JSONObject, onResult: (Boolean) -> Unit) {
        val device = targetDevice ?: return onMain { onResult(false) }
        val done = AtomicBoolean(false)
        fun finish(ok: Boolean, log: String) {
            if (done.compareAndSet(false, true)) onMain { debugLog = log; onResult(ok) }
        }
        val message = Message.Builder().setPayload(json.toString().toByteArray(Charsets.UTF_8)).build()
        val callback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                val ok = resultCode == WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS
                finish(ok, if (ok) "Sent: $json" else "Send failed (code $resultCode)")
            }

            override fun onSendProgress(progress: Long) = Unit
        }
        p2pClient.send(device, message, callback)
            .addOnFailureListener { e -> finish(false, "Send failed: ${e.message}") }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    companion object {
        /** Bundle name of the app running ON THE WATCH. */
        const val WATCH_PKG_NAME = "com.ipandai.edompet.watch"

        /**
         * Signing fingerprint of the watch app: "<bundleName>_<base64 public key>", where the key
         * is the developer (leaf) cert in the AGC debug certificate "edompet-debug".
         * A release build of the watch app signed with a different cert needs a new value here.
         */
        const val WATCH_FINGERPRINT =
            "com.ipandai.edompet.watch_MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjn3SDp8hJ/GwEVaJXSJPT1UAvVCovkD5cqL2ZoyB1zJdDjSEt9skdqmIjW8X4siVGSYqHXJyQ8kQud8jGVZfSA=="
    }
}
