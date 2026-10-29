package com.t9dialer.app.data

import com.t9dialer.app.t9.fold

class ContactHit(
    val entry: PhoneEntry,
    val nameHighlights: IntArray,
    val numberHighlights: IntArray,
    val score: Int,
    val lastCall: Long,
)

class RecentHit(
    val recent: RecentGroup,
    val nameHighlights: IntArray,
    val numberHighlights: IntArray,
    val score: Int,
)

/** Call log matches are shown first, then contacts. */
class SearchResult(val calls: List<RecentHit>, val contacts: List<ContactHit>) {
    val isEmpty get() = calls.isEmpty() && contacts.isEmpty()

    companion object {
        val EMPTY = SearchResult(emptyList(), emptyList())
    }
}

private class M(val score: Int, val hl: IntArray)

object Search {
    private val NONE = IntArray(0)
    private const val MAX_CALLS = 5
    private const val MAX_CONTACTS = 150

    /** Keypad search: T9 on names, digits on numbers. */
    fun run(query: String, state: PhoneState): SearchResult {
        if (query.isEmpty()) return SearchResult.EMPTY
        return collect(
            state,
            contact = { it.name.match(query)?.let { m -> M(m.score, m.highlights) } to it.number.match(query)?.let { m -> M(m.score, m.highlights) } },
            recent = { (it.name?.match(query)?.let { m -> M(m.score, m.highlights) }) to it.number.match(query)?.let { m -> M(m.score, m.highlights) } },
        )
    }

    /** Typed search from the keyboard: plain letters, accents ignored. */
    fun runText(text: String, state: PhoneState): SearchResult {
        val needle = fold(text.trim())
        if (needle.isEmpty()) return SearchResult.EMPTY
        val digits = needle.filter { it.isDigit() }.takeIf { it.length == needle.count { c -> c != ' ' && c != '-' } && it.isNotEmpty() }
        return collect(
            state,
            contact = { lettersMatch(it.folded, needle) to digits?.let { d -> it.number.match(d)?.let { m -> M(m.score, m.highlights) } } },
            recent = { (it.folded?.let { f -> lettersMatch(f, needle) }) to digits?.let { d -> it.number.match(d)?.let { m -> M(m.score, m.highlights) } } },
        )
    }

    private fun lettersMatch(folded: String, needle: String): M? {
        val at = folded.indexOf(needle)
        if (at < 0) return null
        val score = when {
            at == 0 -> 1000
            !folded[at - 1].isLetterOrDigit() -> 900
            else -> 700
        }
        return M(score, IntArray(needle.length) { at + it })
    }

    private inline fun collect(
        state: PhoneState,
        contact: (IndexedContact) -> Pair<M?, M?>,
        recent: (IndexedRecent) -> Pair<M?, M?>,
    ): SearchResult {
        val contacts = ArrayList<ContactHit>()
        for (item in state.contactIndex) {
            val (byName, byNumber) = contact(item)
            if (byName == null && byNumber == null) continue
            val stat = state.stats[item.entry.key]
            val boost = minOf(stat?.count ?: 0, 20) * 3 + if (item.entry.starred) 15 else 0
            contacts += ContactHit(
                item.entry,
                byName?.hl ?: NONE,
                byNumber?.hl ?: NONE,
                maxOf(byName?.score ?: 0, byNumber?.score ?: 0) + boost,
                stat?.last ?: 0L,
            )
        }
        contacts.sortWith(
            compareByDescending<ContactHit> { it.score }
                .thenByDescending { it.lastCall }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.entry.name }
        )

        val calls = ArrayList<RecentHit>()
        for (item in state.recentIndex) {
            val (byName, byNumber) = recent(item)
            if (byName == null && byNumber == null) continue
            calls += RecentHit(item.recent, byName?.hl ?: NONE, byNumber?.hl ?: NONE, maxOf(byName?.score ?: 0, byNumber?.score ?: 0))
        }
        calls.sortWith(compareByDescending<RecentHit> { it.score }.thenByDescending { it.recent.date })

        return SearchResult(calls.take(MAX_CALLS), contacts.take(MAX_CONTACTS))
    }
}
