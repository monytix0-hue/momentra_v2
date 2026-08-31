package com.example.momentra.ui.shell.empty.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupJoinLinkTest {
    @Test
    fun parsesShortDisplayPathAndCustomScheme() {
        assertEquals("abcdhkmn", GroupJoinLink.parse("momentra.app/j/abcdhkmn"))
        assertEquals("abcdhkmn", GroupJoinLink.parse("https://momentra.app/j/abcdhkmn"))
        assertEquals("abcdhkmn", GroupJoinLink.parse("momentra://j/abcdhkmn"))
        assertEquals("abcdhkmn", GroupJoinLink.parse("abcdhkmn"))
    }

    @Test
    fun rejectsJwtPayloads() {
        val jwt =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0In0.abc"
        assertNull(GroupJoinLink.parse(jwt))
        assertNull(GroupJoinLink.parse("https://api.example.com/join?token=$jwt"))
    }
}
