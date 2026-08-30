package net.essentialsx.fabric.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatUtilTest {
    @Test
    void stripsLegacyFormatting() {
        assertEquals("hello", FormatUtil.stripFormat("\u00a7ahel\u00a7llo"));
        assertEquals("&ahello", FormatUtil.stripFormat("&ahello"));
    }

    @Test
    void replacesAmpersandCodes() {
        assertEquals("\u00a7ahello", FormatUtil.replaceFormat("&ahello"));
        assertEquals("&ahello", FormatUtil.unformatString("\u00a7ahello"));
    }

    @Test
    void validatesIps() {
        assertTrue(FormatUtil.validIP("127.0.0.1"));
        assertFalse(FormatUtil.validIP("999.1.1.1"));
        assertFalse(FormatUtil.validIP("not an ip"));
    }
}
