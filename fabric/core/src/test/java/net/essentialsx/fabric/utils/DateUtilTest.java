package net.essentialsx.fabric.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilTest {
    @Test
    void parsesRelativeDurations() throws Exception {
        final long now = System.currentTimeMillis();
        final long inTwoHours = DateUtil.parseDateDiff("2h", true);
        final long delta = inTwoHours - now;
        assertTrue(delta > 2 * 60 * 60 * 1000L - 5000 && delta < 2 * 60 * 60 * 1000L + 5000, "delta was " + delta);
        assertTrue(DateUtil.parseDateDiff("1d", false) < now);
    }

    @Test
    void stripsTimePatternFromReasons() {
        assertEquals("griefing", DateUtil.removeTimePattern("2d griefing").trim());
    }
}
