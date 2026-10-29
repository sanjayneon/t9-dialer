package com.t9dialer.app.t9

import java.text.Normalizer

/**
 * Maps characters to phone keypad digits. Latin and Cyrillic layouts are both
 * active at once, so names in either script are searchable without a setting.
 * Accented letters (é, ñ, ü...) fall back to their base letter.
 */
object T9Keys {
    private val table: Map<Char, Char> = HashMap<Char, Char>().apply {
        fun assign(digit: Char, chars: String) = chars.forEach { this[it] = digit }
        assign('2', "abc"); assign('3', "def"); assign('4', "ghi"); assign('5', "jkl")
        assign('6', "mno"); assign('7', "pqrs"); assign('8', "tuv"); assign('9', "wxyz")
        assign('2', "абвг"); assign('3', "дежзё"); assign('4', "ийкл"); assign('5', "мноп")
        assign('6', "рсту"); assign('7', "фхцч"); assign('8', "шщъы"); assign('9', "ьэюя")
        for (d in '0'..'9') this[d] = d
    }

    /** Letters printed beside each key. Search always understands both alphabets; this only changes the labels. */
    val latinLetters: Map<Char, String> = mapOf(
        '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL",
        '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '0' to "+",
    )
    val cyrillicLetters: Map<Char, String> = mapOf(
        '2' to "АБВГ", '3' to "ДЕЖЗ", '4' to "ИЙКЛ", '5' to "МНОП",
        '6' to "РСТУ", '7' to "ФХЦЧ", '8' to "ШЩЪЫ", '9' to "ЬЭЮЯ", '0' to "+",
    )

    fun digitOf(c: Char): Char? {
        val lower = c.lowercaseChar()
        table[lower]?.let { return it }
        val base = Normalizer.normalize(lower.toString(), Normalizer.Form.NFD)
        if (base.isNotEmpty()) table[base[0]]?.let { return it }
        return null
    }
}

/** A match: how good it is, and which characters of the source text to highlight. */
class T9Match(val score: Int, val highlights: IntArray)

/**
 * A contact name prepared for T9 search. The query can match:
 *  - the start of any word ("doe" -> John Doe)
 *  - across consecutive words ("johnd" -> John Doe)
 *  - initials and word prefixes mixed ("jd", "jodo", "j doe")
 *  - with up to two words skipped ("jd" -> John Michael Doe, ranked lower)
 */
class T9Name(val text: String) {

    private class Word(val digits: String, val charIndex: IntArray)
    private class Seg(val word: Int, val len: Int)

    private val words: List<Word>
    private val flat: String
    private val flatIndex: IntArray

    init {
        val out = ArrayList<Word>()
        val sb = StringBuilder()
        val idx = ArrayList<Int>()
        fun flush() {
            if (sb.isNotEmpty()) {
                out += Word(sb.toString(), idx.toIntArray())
                sb.clear(); idx.clear()
            }
        }
        text.forEachIndexed { i, c ->
            val d = T9Keys.digitOf(c)
            if (d == null) flush() else { sb.append(d); idx += i }
        }
        flush()
        words = if (out.size > MAX_WORDS) out.subList(0, MAX_WORDS) else out
        flat = words.joinToString("") { it.digits }
        flatIndex = words.flatMap { it.charIndex.toList() }.toIntArray()
    }

    fun match(query: String): T9Match? {
        if (query.isEmpty() || words.isEmpty()) return null
        if ('*' in query) return wildcard(query, flat, flatIndex)
        var bestScore = Int.MIN_VALUE
        var best: List<Seg>? = null
        val path = ArrayList<Seg>(4)

        fun dfs(qpos: Int, minWord: Int, skipped: Int) {
            if (qpos == query.length) {
                val s = score(path, skipped)
                if (s > bestScore) { bestScore = s; best = path.toList() }
                return
            }
            val last = if (path.isEmpty()) words.lastIndex
                       else minOf(words.lastIndex, minWord + MAX_SKIP)
            for (w in minWord..last) {
                val digits = words[w].digits
                val maxLen = minOf(digits.length, query.length - qpos)
                var common = 0
                while (common < maxLen && digits[common] == query[qpos + common]) common++
                for (len in common downTo 1) {
                    val skip = if (path.isEmpty()) 0 else w - minWord
                    path += Seg(w, len)
                    dfs(qpos + len, w + 1, skipped + skip)
                    path.removeAt(path.lastIndex)
                }
            }
        }

        dfs(0, 0, 0)
        val segs = best ?: return null
        val hl = IntArray(segs.sumOf { it.len })
        var k = 0
        for (s in segs) for (i in 0 until s.len) hl[k++] = words[s.word].charIndex[i]
        return T9Match(bestScore, hl)
    }

    private fun score(path: List<Seg>, skipped: Int): Int {
        var s = 1000
        s -= path[0].word * 60          // match starts at a later word
        s -= (path.size - 1) * 25       // split across more words
        s -= skipped * 80               // words jumped over
        for (seg in path) if (seg.len == words[seg.word].digits.length) s += 10
        return s
    }

    private companion object {
        const val MAX_WORDS = 8
        const val MAX_SKIP = 2
    }
}

/** A phone number prepared for search: "+44 20 7946 0958" -> "+442079460958". */
class T9Number(val text: String) {
    val digits: String
    private val charIndex: IntArray

    init {
        val sb = StringBuilder()
        val idx = ArrayList<Int>()
        text.forEachIndexed { i, c ->
            if (c.isDigit() || c == '*' || c == '#' || (c == '+' && sb.isEmpty())) {
                sb.append(c); idx += i
            }
        }
        digits = sb.toString()
        charIndex = idx.toIntArray()
    }

    fun match(query: String): T9Match? {
        if (query.isEmpty()) return null
        if ('*' in query) {
            // A real "*" in the number (like *123#) still matches literally first
            find(query)?.let { return it }
            return wildcard(query, digits, charIndex)
        }
        find(query)?.let { return it }
        // Typed with a national trunk 0: "020 7946..." should find "+44 20 7946..."
        if (query.length > 1 && query[0] == '0' && query[1] != '0') {
            find(query.substring(1))?.let { return T9Match(it.score - 20, it.highlights) }
        }
        return null
    }

    private fun find(q: String): T9Match? {
        val pos = digits.indexOf(q)
        if (pos < 0) return null
        // "4420..." typed without the "+" still counts as a prefix of "+4420..."
        val isPrefix = pos == 0 || (pos == 1 && digits[0] == '+')
        val score = if (isPrefix) 600 else 500 - minOf(pos, 50)
        return T9Match(score, IntArray(q.length) { charIndex[pos + it] })
    }
}

/**
 * "*" stands for any run of keys: "5*3" finds a J followed somewhere later by a D.
 * Pieces must appear in order. A query of only "*" matches nothing.
 */
private fun wildcard(query: String, digits: String, index: IntArray): T9Match? {
    val parts = query.split('*').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    val hl = ArrayList<Int>()
    var pos = 0
    for (part in parts) {
        val at = digits.indexOf(part, pos)
        if (at < 0) return null
        for (i in at until at + part.length) hl += index[i]
        pos = at + part.length
    }
    return T9Match(450, hl.toIntArray())
}

/** Lower-cases and strips accents one character at a time, so positions line up with the original. */
fun fold(s: String): String = buildString(s.length) {
    for (c in s) {
        val lower = c.lowercaseChar()
        val base = Normalizer.normalize(lower.toString(), Normalizer.Form.NFD)
        append(if (base.isNotEmpty()) base[0] else lower)
    }
}

/** Builds the digit query a typed letter string would produce, e.g. "john" -> "5646". */
fun toT9(s: String): String = s.mapNotNull { T9Keys.digitOf(it) }.joinToString("")
