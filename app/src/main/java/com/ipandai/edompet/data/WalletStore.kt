package com.ipandai.edompet.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ipandai.edompet.wear.WatchState
import com.ipandai.edompet.wear.WearEngineManager

/**
 * Single source of truth for the cards, and the only place that talks to the watch.
 *
 * Sync is automatic: every add/edit/delete is queued and sent as soon as the watch is
 * connected, one message at a time. Nothing is lost if the watch is away; pending work is
 * persisted and retried on the next connection.
 */
class WalletStore(private val repo: CardRepository, private val wear: WearEngineManager) {

    /** Cards in display order (pinned first, then most recently used). */
    var cards by mutableStateOf(sortForDisplay(repo.load()))
        private set

    private val pendingDeletes = repo.loadPendingDeletes().toMutableSet()
    private var sending = false

    val pendingCount: Int get() = cards.count { !it.synced } + pendingDeletes.size

    init {
        wear.onConnected = { syncPending() }
    }

    fun add(card: MembershipCard) {
        val now = System.currentTimeMillis()
        commit(sortForDisplay(listOf(card.copy(createdAt = now, lastUsedAt = now, synced = false)) + cards))
        syncPending()
    }

    fun update(card: MembershipCard) {
        commit(cards.map { if (it.id == card.id) card.copy(synced = it.synced && it.sameContentAs(card)) else it })
        syncPending()
    }

    fun togglePin(id: String) {
        commit(sortForDisplay(cards.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }))
    }

    /**
     * Records that the card's code was shown. Order is NOT changed right away so the stack
     * doesn't shuffle under the user's thumb; recent cards move up on the next launch.
     */
    fun markUsed(id: String) {
        commit(cards.map { if (it.id == id) it.copy(lastUsedAt = System.currentTimeMillis()) else it })
    }

    /** Removes the card and returns what's needed to undo it. */
    fun delete(id: String): DeletedCard? {
        val index = cards.indexOfFirst { it.id == id }
        if (index < 0) return null
        val card = cards[index]
        commit(cards.filterNot { it.id == id })
        pendingDeletes += id
        repo.savePendingDeletes(pendingDeletes)
        syncPending()
        return DeletedCard(card, index)
    }

    fun undoDelete(deleted: DeletedCard) {
        val list = cards.toMutableList()
        // The delete may already have reached the watch, so send the card again.
        list.add(deleted.index.coerceIn(0, list.size), deleted.card.copy(synced = false))
        commit(list)
        pendingDeletes -= deleted.card.id
        repo.savePendingDeletes(pendingDeletes)
        syncPending()
    }

    /** "Sync all cards": resend everything, e.g. after the watch app was reinstalled. */
    fun resyncAll() {
        commit(cards.map { it.copy(synced = false) })
        syncPending()
    }

    fun syncPending() {
        if (sending || wear.state != WatchState.CONNECTED) return

        pendingDeletes.firstOrNull()?.let { id ->
            sending = true
            wear.sendDelete(id) { ok ->
                sending = false
                if (ok) {
                    pendingDeletes -= id
                    repo.savePendingDeletes(pendingDeletes)
                    syncPending()
                }
            }
            return
        }

        val next = cards.firstOrNull { !it.synced } ?: return
        sending = true
        wear.sendCard(next) { ok ->
            sending = false
            if (ok) {
                // Only mark synced if the card wasn't edited while the message was in flight.
                commit(cards.map { if (it.id == next.id && it.sameContentAs(next)) it.copy(synced = true) else it })
                syncPending()
            }
        }
    }

    private fun commit(list: List<MembershipCard>) {
        cards = list
        repo.save(list)
    }

    private fun sortForDisplay(list: List<MembershipCard>) =
        list.sortedWith(
            compareByDescending<MembershipCard> { it.pinned }
                .thenByDescending { it.lastUsedAt }
                .thenByDescending { it.createdAt }
        )
}

data class DeletedCard(val card: MembershipCard, val index: Int)
