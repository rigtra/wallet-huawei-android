package com.ipandai.edompet.data

/**
 * Filters cards by merchant name OR code number, best matches first.
 *
 * Name is what people type most ("indo" -> Indomaret). Code covers the cashier case
 * ("what's the card ending in 4821?"). Codes are compared without spaces/dashes, because
 * printed barcodes are usually grouped ("1234 5678 9012") but stored as one string.
 *
 * Ranking: name starts with query > a word in the name starts with query >
 * name contains query > code contains query. Ties keep the wallet's own order.
 */
fun List<MembershipCard>.search(query: String): List<MembershipCard> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return this
    val qCode = q.compactCode()

    return mapNotNull { card ->
        val name = card.merchantName.lowercase()
        val rank = when {
            name.startsWith(q) -> 0
            name.split(' ', '-', '.', '_').any { it.startsWith(q) } -> 1
            name.contains(q) -> 2
            qCode.isNotEmpty() && card.codeValue.lowercase().compactCode().contains(qCode) -> 3
            else -> return@mapNotNull null
        }
        rank to card
    }
        .sortedBy { it.first } // stable: equal ranks stay in wallet order
        .map { it.second }
}

private fun String.compactCode(): String = filterNot { it.isWhitespace() || it == '-' }
