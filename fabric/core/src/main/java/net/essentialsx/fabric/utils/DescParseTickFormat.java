package net.essentialsx.fabric.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static net.essentialsx.fabric.text.I18n.tlLiteral;

public final class DescParseTickFormat {
    public static final Map<String, Integer> nameToTicks = new LinkedHashMap<>();
    public static final Set<String> resetAliases = new HashSet<>();
    public static final int ticksAtMidnight = 18000;
    public static final int ticksPerDay = 24000;
    public static final int ticksPerHour = 1000;
    public static final double ticksPerMinute = 1000d / 60d;
    public static final double ticksPerSecond = 1000d / 60d / 60d;
    private static final SimpleDateFormat SDFTwentyFour = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private static final SimpleDateFormat SDFTwelve = new SimpleDateFormat("h:mm aa", Locale.ENGLISH);

    static {
        SDFTwentyFour.setTimeZone(TimeZone.getTimeZone("GMT"));
        SDFTwelve.setTimeZone(TimeZone.getTimeZone("GMT"));
        nameToTicks.put("sunrise", 23000);
        nameToTicks.put("dawn", 23000);
        nameToTicks.put("daystart", 0);
        nameToTicks.put("day", 0);
        nameToTicks.put("morning", 1000);
        nameToTicks.put("midday", 6000);
        nameToTicks.put("noon", 6000);
        nameToTicks.put("afternoon", 9000);
        nameToTicks.put("sunset", 12000);
        nameToTicks.put("dusk", 12000);
        nameToTicks.put("sundown", 12000);
        nameToTicks.put("nightfall", 12000);
        nameToTicks.put("nightstart", 14000);
        nameToTicks.put("night", 14000);
        nameToTicks.put("midnight", 18000);
        resetAliases.add("reset");
        resetAliases.add("normal");
        resetAliases.add("default");
    }

    private DescParseTickFormat() {
    }

    public static long parse(String desc) throws NumberFormatException {
        desc = desc.toLowerCase(Locale.ENGLISH).replaceAll("[^A-Za-z0-9:]", "");
        try {
            return parseTicks(desc);
        } catch (final NumberFormatException ignored) {
        }
        try {
            return parse24(desc);
        } catch (final NumberFormatException ignored) {
        }
        try {
            return parse12(desc);
        } catch (final NumberFormatException ignored) {
        }
        try {
            return parseAlias(desc);
        } catch (final NumberFormatException ignored) {
        }
        throw new NumberFormatException();
    }

    public static long parseTicks(String desc) throws NumberFormatException {
        if (!desc.matches("^[0-9]+ti?c?k?s?$")) {
            throw new NumberFormatException();
        }
        desc = desc.replaceAll("[^0-9]", "");
        return Long.parseLong(desc) % 24000;
    }

    public static long parse24(String desc) throws NumberFormatException {
        if (!desc.matches("^[0-9]{2}[^0-9]?[0-9]{2}$")) {
            throw new NumberFormatException();
        }
        desc = desc.toLowerCase(Locale.ENGLISH).replaceAll("[^0-9]", "");
        if (desc.length() != 4) {
            throw new NumberFormatException();
        }
        final int hours = Integer.parseInt(desc.substring(0, 2));
        final int minutes = Integer.parseInt(desc.substring(2, 4));
        return hoursMinutesToTicks(hours, minutes);
    }

    public static long parse12(String desc) throws NumberFormatException {
        if (!desc.matches("^[0-9]{1,2}([^0-9]?[0-9]{2})?(pm|am)$")) {
            throw new NumberFormatException();
        }
        int hours = 0;
        int minutes = 0;
        desc = desc.toLowerCase(Locale.ENGLISH);
        final String parsetime = desc.replaceAll("[^0-9]", "");
        if (parsetime.length() > 4) {
            throw new NumberFormatException();
        }
        if (parsetime.length() == 4) {
            hours += Integer.parseInt(parsetime.substring(0, 2));
            minutes += Integer.parseInt(parsetime.substring(2, 4));
        } else if (parsetime.length() == 3) {
            hours += Integer.parseInt(parsetime.substring(0, 1));
            minutes += Integer.parseInt(parsetime.substring(1, 3));
        } else if (parsetime.length() == 2) {
            hours += Integer.parseInt(parsetime.substring(0, 2));
        } else if (parsetime.length() == 1) {
            hours += Integer.parseInt(parsetime.substring(0, 1));
        } else {
            throw new NumberFormatException();
        }
        if (desc.endsWith("pm") && hours != 12) {
            hours += 12;
        }
        if (desc.endsWith("am") && hours == 12) {
            hours -= 12;
        }
        return hoursMinutesToTicks(hours, minutes);
    }

    public static long hoursMinutesToTicks(final int hours, final int minutes) {
        long ret = ticksAtMidnight;
        ret += hours * ticksPerHour;
        ret += (minutes / 60.0) * ticksPerHour;
        ret %= ticksPerDay;
        return ret;
    }

    public static long parseAlias(final String desc) throws NumberFormatException {
        final Integer ret = nameToTicks.get(desc);
        if (ret == null) {
            throw new NumberFormatException();
        }
        return ret;
    }

    public static boolean meansReset(final String desc) {
        return resetAliases.contains(desc);
    }

    public static String format(final long ticks) {
        return tlLiteral("timeFormat", format24(ticks), format12(ticks), formatTicks(ticks));
    }

    public static String formatTicks(final long ticks) {
        return (ticks % ticksPerDay) + " ticks";
    }

    public static String format24(final long ticks) {
        synchronized (SDFTwentyFour) {
            return formatDateFormat(ticks, SDFTwentyFour);
        }
    }

    public static String format12(final long ticks) {
        synchronized (SDFTwelve) {
            return formatDateFormat(ticks, SDFTwelve);
        }
    }

    public static String formatDateFormat(final long ticks, final SimpleDateFormat format) {
        final Date date = ticksToDate(ticks);
        return format.format(date);
    }

    public static Date ticksToDate(long ticks) {
        ticks = ticks - ticksAtMidnight + ticksPerDay;
        final long days = ticks / ticksPerDay;
        ticks -= days * ticksPerDay;
        final long hours = ticks / ticksPerHour;
        ticks -= hours * ticksPerHour;
        final long minutes = (long) Math.floor(ticks / ticksPerMinute);
        final double dticks = ticks - minutes * ticksPerMinute;
        final long seconds = (long) Math.floor(dticks / ticksPerSecond);
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ENGLISH);
        cal.setLenient(true);
        cal.set(0, Calendar.JANUARY, 1, 0, 0, 0);
        cal.add(Calendar.DAY_OF_YEAR, (int) days);
        cal.add(Calendar.HOUR_OF_DAY, (int) hours);
        cal.add(Calendar.MINUTE, (int) minutes);
        cal.add(Calendar.SECOND, (int) seconds + 1);
        return cal.getTime();
    }
}
