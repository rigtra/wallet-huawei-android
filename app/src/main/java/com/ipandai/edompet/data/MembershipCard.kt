package com.ipandai.edompet.data

import java.util.UUID

const val CODE_TYPE_QR = "QR Code"
const val CODE_TYPE_BARCODE = "Barcode"

data class MembershipCard(
    val id: String = UUID.randomUUID().toString(),
    val merchantName: String,
    val codeType: String,
    val codeValue: String,
    val styleId: String = DEFAULT_STYLE_ID,
    /** Pinned cards always stay at the top of the wallet. */
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** Last time the code was opened; recent cards float to the top on the next launch. */
    val lastUsedAt: Long = System.currentTimeMillis(),
    /** False until the watch confirms it received the latest version of this card. */
    val synced: Boolean = false,
) {
    /** Fields the watch cares about. A sync only counts if these didn't change mid-flight. */
    fun sameContentAs(other: MembershipCard): Boolean =
        merchantName == other.merchantName && codeType == other.codeType &&
            codeValue == other.codeValue && styleId == other.styleId

    val isBarcode: Boolean get() = codeType.lowercase().contains("barcode")
    val style: CardStyle get() = styleFor(styleId)
}

/**
 * Two-stop vertical gradient (top -> bottom) taken from the "color gradient" palettes.
 * Each pair stays on one side (both dark or both bright) so one text color stays readable
 * across the whole card. WCAG contrast of the text color, checked along the gradient:
 * the top stop (22sp bold merchant name) is >= 3:1 and the bottom stop (code number) is >= 5.5:1.
 */
data class CardStyle(
    val id: String,
    val name: String,
    val topHex: String,
    val bottomHex: String,
    val darkText: Boolean,
) {
    val textHex: String get() = if (darkText) DARK_TEXT_HEX else "#FFFFFF"

    /** Single color for places that can't draw a gradient (e.g. the watch). */
    val solidHex: String get() = bottomHex
}

const val DARK_TEXT_HEX = "#151311" // "Obsidian Ink"
const val DEFAULT_STYLE_ID = "biru"

/** Ordered by the rainbow (merah, jingga, kuning, hijau, biru, nila, ungu), then pink and dark neutrals. */
val cardStyles = listOf(
    CardStyle("merah", "Kinetic Vermilion", "#FA2D1A", "#7C170D", darkText = false),
    CardStyle("jingga", "Kinetic Amber", "#EC7267", "#F47504", darkText = true),
    CardStyle("kuning", "Solar Silk", "#FFE26D", "#E9DB00", darkText = true),
    CardStyle("hijau_neon", "Viridian Glitch", "#08FC8B", "#BEDA63", darkText = true),
    CardStyle("hijau", "Jade Botanical", "#479C73", "#013936", darkText = false),
    CardStyle("teal", "Abyssal Teal", "#018989", "#0F464B", darkText = false),
    CardStyle("biru", "Velocity Azure", "#2174C4", "#2E4C8C", darkText = false),
    CardStyle("nila", "Electric Amethyst", "#8C70E9", "#2E4C8C", darkText = false),
    CardStyle("ungu", "Royal Amethyst", "#7E49B3", "#141414", darkText = false),
    CardStyle("pink", "Vivacity Orchid", "#CE3B89", "#511028", darkText = false),
    CardStyle("maroon", "Sangria Deep", "#7A1229", "#34151B", darkText = false),
    CardStyle("hitam", "Obsidian Ink", "#4B262F", "#151311", darkText = false),
)

fun styleFor(id: String?): CardStyle =
    cardStyles.firstOrNull { it.id == id } ?: cardStyles.first { it.id == DEFAULT_STYLE_ID }

/** Maps the old solid-color presets (cards saved before gradients) to the closest new style. */
fun styleIdForLegacyHex(hex: String?): String = when (hex?.uppercase()) {
    "#1E5AA8" -> "biru"
    "#00754A" -> "hijau"
    "#D71920" -> "merah"
    "#5C2D91" -> "ungu"
    "#C85100" -> "jingga"
    "#005F73" -> "teal"
    "#2B3A42", "#1A1A1A" -> "hitam"
    "#6B0F1A" -> "maroon"
    "#1B263B" -> "nila"
    else -> DEFAULT_STYLE_ID
}
