package net.essentialsx.fabric.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberUtilTest {
    @Test
    void integerChecks() {
        assertTrue(NumberUtil.isInt("42"));
        assertTrue(NumberUtil.isInt("-1"));
        assertFalse(NumberUtil.isInt("4.2"));
        assertFalse(NumberUtil.isInt("abc"));
        assertTrue(NumberUtil.isPositiveInt("3"));
        assertFalse(NumberUtil.isPositiveInt("0"));
        assertTrue(NumberUtil.isNumeric("123"));
        assertTrue(NumberUtil.isHexadecimal("ff00aa"));
        assertFalse(NumberUtil.isHexadecimal("zz"));
    }

    @Test
    void parsesDecimalsWithSuffixes() throws Exception {
        assertEquals(0, new BigDecimal("1500").compareTo(NumberUtil.parseStringToBDecimal("1.5k")));
        assertEquals(0, new BigDecimal("2000000").compareTo(NumberUtil.parseStringToBDecimal("2m")));
        assertEquals(0, new BigDecimal("12.5").compareTo(NumberUtil.parseStringToBDecimal("12,5", Locale.GERMANY)));
        assertThrows(Exception.class, () -> NumberUtil.parseStringToBDecimal("nope"));
    }

    @Test
    void clampsRanges() {
        assertEquals(5, NumberUtil.constrainToRange(10, 0, 5));
        assertEquals(0, NumberUtil.constrainToRange(-3, 0, 5));
        assertEquals("1.5", NumberUtil.formatDouble(1.5));
        assertEquals("1.25", NumberUtil.formatDouble(1.254));
    }
}
