package net.essentialsx.fabric.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescParseTickFormatTest {
    @Test
    void parsesAliasesAndClockFormats() {
        assertEquals(DescParseTickFormat.parseAlias("day"), DescParseTickFormat.parse("day"));
        assertEquals(DescParseTickFormat.parse24("06:00"), DescParseTickFormat.parse("06:00"));
        assertEquals(DescParseTickFormat.parse12("6am"), DescParseTickFormat.parse("6am"));
        assertEquals(1000L, DescParseTickFormat.parseTicks("1000t"));
        assertThrows(NumberFormatException.class, () -> DescParseTickFormat.parse("not-a-time"));
    }

    @Test
    void ticksRoundTripThroughClockFormats() {
        final long noon = DescParseTickFormat.hoursMinutesToTicks(12, 0);
        assertEquals("12:00", DescParseTickFormat.format24(noon));
        assertEquals(noon, DescParseTickFormat.parse24("12:00"));
        assertTrue(DescParseTickFormat.formatTicks(noon).endsWith("ticks"));
    }

    @Test
    void resetKeywords() {
        assertTrue(DescParseTickFormat.meansReset("reset"));
        assertTrue(DescParseTickFormat.meansReset("normal"));
    }
}
