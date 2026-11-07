package com.ipandai.edompet.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** SharedPreferences + JSON, same role as UserDefaults in the iOS app. */
class CardRepository(context: Context) {
    private val prefs = context.getSharedPreferences("edompet", Context.MODE_PRIVATE)
    private val saveKey = "saved_membership_cards"

    fun load(): List<MembershipCard> {
        val raw = prefs.getString(saveKey, null)
        if (raw != null) {
            runCatching { return decode(raw) }
        }
        val defaults = listOf(
            MembershipCard(merchantName = "Fore", codeType = CODE_TYPE_QR, codeValue = "19212828832", styleId = "teal"),
            MembershipCard(merchantName = "Indomaret", codeType = CODE_TYPE_QR, codeValue = "1234567890", styleId = "biru"),
            MembershipCard(merchantName = "AirAsia", codeType = CODE_TYPE_QR, codeValue = "AIRASIA-987654321", styleId = "merah"),
            MembershipCard(merchantName = "Starbucks", codeType = CODE_TYPE_BARCODE, codeValue = "1234567890123", styleId = "hijau"),
        )
        save(defaults)
        return defaults
    }

    fun save(cards: List<MembershipCard>) {
        val arr = JSONArray()
        cards.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("merchantName", c.merchantName)
                    .put("codeType", c.codeType)
                    .put("codeValue", c.codeValue)
                    .put("styleId", c.styleId)
            )
        }
        prefs.edit().putString(saveKey, arr.toString()).apply()
    }

    private fun decode(raw: String): List<MembershipCard> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MembershipCard(
                id = o.getString("id"),
                merchantName = o.getString("merchantName"),
                codeType = o.getString("codeType"),
                codeValue = o.getString("codeValue"),
                styleId = o.optString("styleId").ifEmpty { styleIdForLegacyHex(o.optString("backgroundColorHex")) },
            )
        }
    }
}
