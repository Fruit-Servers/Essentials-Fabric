package net.essentialsx.fabric.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {
    @Test
    void joinsLists() {
        assertEquals("a, b, c", StringUtil.joinList(new Object[] {"a", "b", "c"}));
        assertEquals("a|b", StringUtil.joinList("|", "a", "b"));
        assertEquals("a, c", StringUtil.joinListSkip(", ", "b", "a", "b", "c"));
    }

    @Test
    void sanitisesNamesAndFiles() {
        assertEquals("hello", StringUtil.sanitizeString("he" + (char) 1 + "llo"));
        assertEquals("home_1", StringUtil.safeString("Home 1"));
        assertTrue(StringUtil.isReservedFileName("con"));
    }

    @Test
    void helpers() {
        assertEquals("abc...", StringUtil.abbreviate("abcdefgh", 6));
        assertNull(StringUtil.stripToNull("   "));
        assertTrue(StringUtil.startsWithIgnoreCase("Hello", "he"));
        final UUID uuid = UUID.randomUUID();
        assertEquals(uuid, StringUtil.toUUID(uuid.toString()));
        assertEquals(List.of("alpha", "alps"), StringUtil.partialMatches("al", List.of("alpha", "beta", "alps")));
    }
}
