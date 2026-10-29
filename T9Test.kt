package com.t9dialer.app.t9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T9Test {
    private fun lit(name: String, q: String): String? {
        val m = T9Name(name).match(q) ?: return null
        return name.mapIndexed { i, c -> if (i in m.highlights) c.uppercaseChar() else c.lowercaseChar() }.joinToString("")
    }

    @Test fun keyMapping() {
        assertEquals("5646", toT9("John"))
        assertEquals("5673", toT9("José"))
        assertEquals("3442", toT9("Ёлка"))
    }

    @Test fun nameMatches() {
        assertEquals("JOHN doe", lit("John Doe", "5646"))
        assertEquals("JOHN Doe", lit("John Doe", "56463"))
        assertEquals("john DOE", lit("John Doe", "363"))
        assertEquals("John Doe", lit("John Doe", "53"))
        assertNotNull(lit("John Michael Doe", "53"))
        assertNotNull(lit("Анна Петрова", "2552"))
        assertNull(T9Name("John Doe").match("54"))
    }

    @Test fun ranking() {
        assertTrue(T9Name("Jeff").match("53")!!.score > T9Name("John Doe").match("53")!!.score)
        assertTrue(T9Name("John Doe").match("53")!!.score > T9Name("John Michael Doe").match("53")!!.score)
    }

    @Test fun numberMatches() {
        val n = T9Number("+44 20 7946 0958")
        assertNotNull(n.match("+4420"))
        assertEquals(600, n.match("4420")!!.score)
        assertNotNull(n.match("02079"))
        val m = n.match("7946")!!
        assertEquals("7946", n.text.substring(m.highlights.first(), m.highlights.last() + 1))
        assertNull(n.match("1234"))
    }

    @Test fun wildcard() {
        assertEquals("John Doe", lit("John Doe", "5*3"))
        assertNull(T9Name("John Doe").match("*"))
        assertNotNull(T9Number("+91 99 58 959820").match("99*820"))
        assertNotNull(T9Number("*123#").match("*123#"))
    }

    @Test fun folding() {
        assertEquals("jose n", fold("José Ñ"))
    }

    @Test fun prefixes() {
        assertEquals("1234098", com.t9dialer.app.data.withDialPrefix("098", "1234"))
        assertEquals("+4420", com.t9dialer.app.data.withDialPrefix("+4420", "1234"))
        assertEquals("*123#", com.t9dialer.app.data.withDialPrefix("*123#", "1234"))
        assertEquals("+919810083013", com.t9dialer.app.data.withLongPressPrefix("09810083013", "+91"))
    }
}
