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
import org.json.JSONObject

/**
 * Android port of the iOS WearEngineManager.
 *
 * Talks to the Huawei watch through the Huawei Health app (must be installed and the
 * watch paired there). The phone app's package name + signing certificate SHA-256 must be
 * registered for the Wear Engine service in the Huawei Developer console.
 */
class WearEngineManager(private val activity: Activity) {

    var connectionStatus by mutableStateOf("Ready to test")
        private set
    var lastResponse by mutableStateOf("-")
        private set

    private val main = Handler(Looper.getMainLooper())
    private var targetDevice: Device? = null

    private val p2pClient: P2pClient = HiWear.getP2pClient(activity)
        .setPeerPkgName(WATCH_PKG_NAME)
        .setPeerFingerPrint(WATCH_FINGERPRINT)

    /** Ask the user for Wear Engine permission (via Huawei Health), then find a connected watch. */
    fun checkDeviceConnection() {
        setStatus("Checking connection...")
        val authCallback = object : AuthCallback {
            override fun onOk(permissions: Array<out Permission>?) = findConnectedDevice()
            override fun onCancel() = setStatus("Error: Wear Engine permission denied")
        }
        HiWear.getAuthClient(activity)
            .requestPermission(authCallback, Permission.DEVICE_MANAGER)
            .addOnFailureListener { e -> setStatus("Error: ${describe(e)}") }
    }

    private fun findConnectedDevice() {
        HiWear.getDeviceClient(activity).bondedDevices
            .addOnSuccessListener { devices ->
                val connected = devices?.firstOrNull { it.isConnected }
                targetDevice = connected
                setStatus(if (connected != null) "Connected: ${connected.name ?: "Watch"}" else "No Watch connected")
            }
            .addOnFailureListener { e -> setStatus("Error: ${describe(e)}") }
    }

    /** Checks whether the watch app is installed / running (Wear Engine ping). */
    fun sendPing() {
        val device = targetDevice ?: return setStatus("Failed: Watch not connected")
        setStatus("Sending PING...")
        val callback = PingCallback { code ->
            when (code) {
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_RUNNING -> {
                    setStatus("PING OK: watch app running")
                    main.post { lastResponse = "PING" }
                }
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_RUNNING -> setStatus("PING: watch app installed, not running")
                WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_EXIT -> setStatus("PING: watch app not installed")
                else -> setStatus("PING failed (code $code)")
            }
        }
        p2pClient.ping(device, callback)
            .addOnFailureListener { e -> setStatus("PING failed: ${describe(e)}") }
    }

    /** Sends the card as JSON — same payload keys as the iOS app, so the watch app needs no change. */
    fun sendCardData(
        cardId: String,
        merchantName: String,
        codeType: String,
        codeValue: String,
        backgroundColor: String,
        gradientColors: List<String>,
        textColor: String,
    ) {
        val device = targetDevice ?: return setStatus("Failed: Watch not connected")
        setStatus("Sending $merchantName...")

        val json = JSONObject()
            .put("id", cardId) // lets the watch replace a re-synced card instead of duplicating it
            .put("merchantName", merchantName)
            .put("codeType", codeType)
            .put("codeValue", codeValue)
            .put("backgroundColor", backgroundColor)
            // New keys (the iOS app never sent these); a watch app that ignores them still works
            .put("gradientColors", org.json.JSONArray(gradientColors))
            .put("textColor", textColor)
            .toString()

        val message = Message.Builder().setPayload(json.toByteArray(Charsets.UTF_8)).build()
        val callback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                if (resultCode == WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS) {
                    setStatus("$merchantName card sent!")
                    main.post { lastResponse = json }
                } else {
                    setStatus("Send failed (code $resultCode)")
                }
            }

            override fun onSendProgress(progress: Long) = Unit
        }
        p2pClient.send(device, message, callback)
            .addOnFailureListener { e -> setStatus("Send failed: ${describe(e)}") }
    }

    private fun setStatus(text: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) connectionStatus = text
        else main.post { connectionStatus = text }
    }

    private fun describe(e: Exception): String {
        val msg = e.message ?: e.javaClass.simpleName
        return if (msg.contains("Health app not exist", ignoreCase = true)) "Huawei Health app not installed" else msg
    }

    companion object {
        /** Bundle name of the app running ON THE WATCH (same value the iOS app used). */
        const val WATCH_PKG_NAME = "com.ipandai.edompet"

        /**
         * Signing fingerprint of the watch app. TODO: replace with the value from your watch
         * app's signing profile in DevEco Studio / AppGallery Connect (format for lite
         * wearables like GT 4 is usually "<bundleName>_<base64 public key>").
         */
        const val WATCH_FINGERPRINT = "REPLACE_WITH_WATCH_APP_FINGERPRINT"
    }
}
