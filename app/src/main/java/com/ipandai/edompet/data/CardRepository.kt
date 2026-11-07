package com.ipandai.edompet.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** SharedPreferences + JSON, same role as UserDefaults in the iOS app. */
class CardRepository(context: Context) {
    private val prefs = context.getSharedPreferences("edompet", Context.MODE_PRIVATE)
    private val saveKey = "saved_membership_cards"
    private val pendingDeletesKey = "pending_watch_deletes"

    fun load(): List<MembershipCard> {
        val raw = prefs.getString(saveKey, null)
        if (raw != null) {
            runCatching { return decode(raw) }
        }
        val now = System.currentTimeMillis()
        val defaults = listOf(
            MembershipCard(merchantName = "Fore", codeType = CODE_TYPE_QR, codeValue = "19212828832", styleId = "teal", lastUsedAt = now),
            MembershipCard(merchantName = "Indomaret", codeType = CODE_TYPE_QR, codeValue = "1234567890", styleId = "biru", lastUsedAt = now - 1),
            MembershipCard(merchantName = "AirAsia", codeType = CODE_TYPE_QR, codeValue = "AIRASIA-987654321", styleId = "merah", lastUsedAt = now - 2),
            MembershipCard(merchantName = "Starbucks", codeType = CODE_TYPE_BARCODE, codeValue = "1234567890123", styleId = "hijau", lastUsedAt = now - 3),
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
                    .put("pinned", c.pinned)
                    .put("createdAt", c.createdAt)
                    .put("lastUsedAt", c.lastUsedAt)
                    .put("synced", c.synced)
            )
        }
        prefs.edit().putString(saveKey, arr.toString()).apply()
    }

    /** Ids deleted on the phone that the watch hasn't confirmed deleting yet. */
    fun loadPendingDeletes(): Set<String> = prefs.getStringSet(pendingDeletesKey, emptySet())!!.toSet()

    fun savePendingDeletes(ids: Set<String>) {
        prefs.edit().putStringSet(pendingDeletesKey, ids.toSet()).apply()
    }

    private fun decode(raw: String): List<MembershipCard> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val created = o.optLong("createdAt", 0L)
            MembershipCard(
                id = o.getString("id"),
                merchantName = o.getString("merchantName"),
                codeType = o.getString("codeType"),
                codeValue = o.getString("codeValue"),
                styleId = o.optString("styleId").ifEmpty { styleIdForLegacyHex(o.optString("backgroundColorHex")) },
                pinned = o.optBoolean("pinned", false),
                createdAt = created,
                lastUsedAt = o.optLong("lastUsedAt", created),
                synced = o.optBoolean("synced", false),
            )
        }
    }
}
