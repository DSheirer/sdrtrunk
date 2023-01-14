/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.identifier.tone;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * AMBE tones enumeration
 *
 * Note: this uses identical values as the Tone enumeraton in the JMBE library
 */
public enum AmbeTone
{
    DTMF_0("0", "DTMF 0"),
    DTMF_1("1", "DTMF 1"),
    DTMF_2("2", "DTMF 2"),
    DTMF_3("3", "DTMF 3"),
    DTMF_4("4", "DTMF 4"),
    DTMF_5("5", "DTMF 5"),
    DTMF_6("6", "DTMF 6"),
    DTMF_7("7", "DTMF 7"),
    DTMF_8("8", "DTMF 8"),
    DTMF_9("9", "DTMF 9"),
    DTMF_A("A", "DTMF A"),
    DTMF_B("B", "DTMF B"),
    DTMF_C("C", "DTMF C"),
    DTMF_D("D", "DTMF D"),
    DTMF_STAR("*", "DTMF *"),
    DTMF_POUND("#", "DTMF #"),

    KNOX_0("0", "KNOX 0"),
    KNOX_1("1", "KNOX 1"),
    KNOX_2("2", "KNOX 2"),
    KNOX_3("3", "KNOX 3"),
    KNOX_4("4", "KNOX 4"),
    KNOX_5("5", "KNOX 5"),
    KNOX_6("6", "KNOX 6"),
    KNOX_7("7", "KNOX 7"),
    KNOX_8("8", "KNOX 8"),
    KNOX_9("9", "KNOX 9"),
    KNOX_A("A", "KNOX A"),
    KNOX_B("B", "KNOX B"),
    KNOX_C("C", "KNOX C"),
    KNOX_D("D", "KNOX D"),
    KNOX_STAR("*", "KNOX "),
    KNOX_POUND("#", "KNOX #"),

    BUSY_TONE("BUSY TONE", "BUSY TONE"),
    CALL_PROGRESS("CALL PROGRESS", "CALL PROGRESS TONE"),
    DIAL_TONE("DIAL TONE", "DIAL TONE"),
    RINGING_TONE("RINGING TONE", "RINGING TONE"),

    HZ_156_25("156.25", "TONE 156.25"),
    HZ_187_50("187.50", "TONE 187.50"),
    HZ_218_75("218.75", "TONE 218.75"),
    HZ_250_00("250.00", "TONE 250.00"),
    HZ_281_25("281.25", "TONE 281.25"),
    HZ_312_50("312.50", "TONE 312.50"),
    HZ_343_75("343.75", "TONE 343.75"),
    HZ_375_00("375.00", "TONE 375.00"),
    HZ_406_25("406.25", "TONE 406.25"),
    HZ_437_50("437.50", "TONE 437.50"),
    HZ_468_75("468.75", "TONE 468.75"),
    HZ_500_00("500.00", "TONE 500.00"),
    HZ_531_25("531.25", "TONE 531.25"),
    HZ_562_50("562.50", "TONE 562.50"),
    HZ_593_75("593.75", "TONE 593.75"),
    HZ_625_00("625.00", "TONE 625.00"),
    HZ_656_25("656.25", "TONE 656.25"),
    HZ_687_50("687.5", "TONE 687.50"),
    HZ_718_75("718.75", "TONE 718.75"),
    HZ_750_00("750.00", "TONE 750.00"),
    HZ_781_25("781.25", "TONE 781.25"),
    HZ_812_50("812.50", "TONE 812.50"),
    HZ_843_75("843.75", "TONE 843.75"),
    HZ_875_00("875.00", "TONE 875.00"),
    HZ_906_25("906.25", "TONE 906.25"),
    HZ_937_50("937.50", "TONE 937.50"),
    HZ_968_75("968.75", "TONE 968.75"),
    HZ_1000_00("1000.00", "TONE 1000.00"),
    HZ_1031_25("1031.25", "TONE 1031.25"),
    HZ_1062_50("1062.50", "TONE 1062.50"),
    HZ_1093_75("1093.75", "TONE 1093.75"),
    HZ_1125_00("1125.00", "TONE 1125.00"),
    HZ_1156_25("1156.25", "TONE 1156.25"),
    HZ_1187_50("1187.50", "TONE 1187.50"),
    HZ_1218_75("1218.75", "TONE 1218.75"),
    HZ_1250_00("1250.00", "TONE 1250.00"),
    HZ_1281_25("1281.25", "TONE 1281.25"),
    HZ_1312_50("1312.50", "TONE 1312.50"),
    HZ_1343_75("1343.75", "TONE 1343.75"),
    HZ_1375_00("1375.00", "TONE 1375.00"),
    HZ_1406_25("1406.25", "TONE 1406.25"),
    HZ_1437_50("1437.50", "TONE 1437.50"),
    HZ_1468_75("1468.75", "TONE 1468.75"),
    HZ_1500_00("1500.00", "TONE 1500.00"),
    HZ_1531_25("1531.25", "TONE 1531.25"),
    HZ_1562_50("1562.50", "TONE 1562.50"),
    HZ_1593_75("1593.75", "TONE 1593.75"),
    HZ_1625_00("1625.00", "TONE 1625.00"),
    HZ_1656_25("1656.25", "TONE 1656.25"),
    HZ_1687_50("1687.50", "TONE 1687.50"),
    HZ_1718_75("1718.75", "TONE 1718.75"),
    HZ_1750_00("1750.00", "TONE 1750.00"),
    HZ_1781_25("1781.25", "TONE 1781.25"),
    HZ_1812_50("1812.50", "TONE 1812.50"),
    HZ_1843_75("1843.75", "TONE 1843.75"),
    HZ_1875_00("1875.00", "TONE 1875.00"),
    HZ_1906_25("1906.25", "TONE 1906.25"),
    HZ_1937_50("1937.50", "TONE 1937.50"),
    HZ_1968_75("1968.75", "TONE 1968.75"),
    HZ_2000_00("2000.00", "TONE 2000.00"),
    HZ_2031_25("2031.25", "TONE 2031.25"),
    HZ_2062_50("2062.50", "TONE 2062.50"),
    HZ_2093_75("2093.75", "TONE 2093.75"),
    HZ_2125_00("2125.00", "TONE 2125.00"),
    HZ_2156_25("2156.25", "TONE 2156.25"),
    HZ_2187_50("2187.50", "TONE 2187.50"),
    HZ_2218_75("2218.75", "TONE 2218.75"),
    HZ_2250_00("2250.00", "TONE 2250.00"),
    HZ_2281_25("2281.25", "TONE 2281.25"),
    HZ_2312_50("2312.50", "TONE 2312.50"),
    HZ_2343_75("2343.75", "TONE 2343.75"),
    HZ_2375_00("2375.00", "TONE 2375.00"),
    HZ_2406_25("2406.25", "TONE 2406.25"),
    HZ_2437_50("2437.50", "TONE 2437.50"),
    HZ_2468_75("2468.75", "TONE 2468.75"),
    HZ_2500_00("2500.00", "TONE 2500.00"),
    HZ_2531_25("2531.25", "TONE 2531.25"),
    HZ_2562_50("2562.50", "TONE 2562.50"),
    HZ_2593_75("2593.75", "TONE 2593.75"),
    HZ_2625_00("2625.00", "TONE 2625.00"),
    HZ_2656_25("2656.25", "TONE 2656.25"),
    HZ_2718_75("2718.75", "TONE 2718.75"),
    HZ_2687_50("2687.50", "TONE 2687.50"),
    HZ_2750_00("2750.00", "TONE 2750.00"),
    HZ_2781_25("2781.25", "TONE 2781.25"),
    HZ_2812_50("2812.50", "TONE 2812.50"),
    HZ_2843_75("2843.75", "TONE 2843.75"),
    HZ_2875_00("2875.00", "TONE 2875.00"),
    HZ_2906_25("2906.25", "TONE 2906.25"),
    HZ_2937_50("2937.50", "TONE 2937.50"),
    HZ_2968_75("2968.75", "TONE 2968.75"),
    HZ_3000_00("3000.00", "TONE 3000.00"),
    HZ_3031_25("3031.25", "TONE 3031.25"),
    HZ_3062_50("3062.50", "TONE 3062.50"),
    HZ_3093_75("3093.75", "TONE 3093.75"),
    HZ_3125_00("3125.00", "TONE 3125.00"),
    HZ_3156_25("3156.25", "TONE 3156.25"),
    HZ_3187_50("3187.50", "TONE 3187.50"),
    HZ_3218_75("3218.75", "TONE 3218.75"),
    HZ_3250_00("3250.00", "TONE 3250.00"),
    HZ_3281_25("3281.25", "TONE 3281.25"),
    HZ_3312_50("3312.50", "TONE 3312.50"),
    HZ_3343_75("3343.75", "TONE 3343.75"),
    HZ_3375_00("3375.00", "TONE 3375.00"),
    HZ_3406_25("3406.25", "TONE 3406.25"),
    HZ_3437_50("3437.50", "TONE 3437.50"),
    HZ_3468_75("3468.75", "TONE 3468.75"),
    HZ_3500_00("3500.00", "TONE 3500.00"),
    HZ_3531_25("3531.25", "TONE 3531.25"),
    HZ_3562_50("3562.50", "TONE 3562.50"),
    HZ_3593_75("3593.75", "TONE 3593.75"),
    HZ_3625_00("3625.00", "TONE 3625.00"),
    HZ_3656_25("3656.25", "TONE 3656.25"),
    HZ_3687_50("3687.50", "TONE 3687.50"),
    HZ_3718_75("3718.75", "TONE 3718.75"),
    HZ_3750_00("3750.00", "TONE 3750.00"),
    HZ_3781_25("3781.25", "TONE 3781.25"),
    HZ_3812_50("3812.50", "TONE 3812.50"),

    INVALID("INVALID", "INVALID");

    private String mValue;
    private String mLabel;

    public static final EnumSet<AmbeTone> ALL_VALID_TONES = EnumSet.range(AmbeTone.DTMF_0, AmbeTone.HZ_3812_50);

    public static final EnumSet<AmbeTone> CALL_PROGRESS_TONES = EnumSet.range(AmbeTone.BUSY_TONE, AmbeTone.RINGING_TONE);
    public static final EnumSet<AmbeTone> DISCRETE_TONES = EnumSet.range(AmbeTone.HZ_156_25, AmbeTone.HZ_3812_50);
    public static final EnumSet<AmbeTone> DTMF_TONES = EnumSet.range(AmbeTone.DTMF_0, AmbeTone.DTMF_POUND);
    public static final EnumSet<AmbeTone> KNOX_TONES = EnumSet.range(AmbeTone.KNOX_0, AmbeTone.KNOX_POUND);

    private static final Map<String,AmbeTone> CALL_PROGRESS_LOOKUP_MAP = new TreeMap<>();
    private static final Map<String,AmbeTone> DTMF_LOOKUP_MAP = new TreeMap<>();
    private static final Map<String,AmbeTone> KNOX_LOOKUP_MAP = new TreeMap<>();
    private static final Map<String,AmbeTone> TONE_LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(AmbeTone tone : CALL_PROGRESS_TONES)
        {
            CALL_PROGRESS_LOOKUP_MAP.put(tone.getValue(), tone);
        }
        for(AmbeTone tone : DTMF_TONES)
        {
            DTMF_LOOKUP_MAP.put(tone.getValue(), tone);
        }
        for(AmbeTone tone : KNOX_TONES)
        {
            KNOX_LOOKUP_MAP.put(tone.getValue(), tone);
        }
        for(AmbeTone tone : DISCRETE_TONES)
        {
            TONE_LOOKUP_MAP.put(tone.getValue(), tone);
        }
    }

    /**
     * Constructs a tone entry
     *
     * @param value for the tone
     * @param label for display value
     */
    AmbeTone(String value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Value of the tone as produced by the JMBE library
     */
    public String getValue()
    {
        return mValue;
    }


    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the tone entry from the type and value parameters.
     * @param type of tone, one of: CALL PROGRESS, DTMF, KNOX, or TONE
     * @param tone value with the tone type set
     * @return tone entry or INVALID
     */
    public static AmbeTone fromValues(String type, String tone)
    {
        if(type != null && tone != null)
        {
            switch(type)
            {
                case "CALL PROGRESS":
                    if(CALL_PROGRESS_LOOKUP_MAP.containsKey(tone))
                    {
                        return CALL_PROGRESS_LOOKUP_MAP.get(tone);
                    }
                    break;
                case "DTMF":
                    if(DTMF_LOOKUP_MAP.containsKey(tone))
                    {
                        return DTMF_LOOKUP_MAP.get(tone);
                    }
                    break;
                case "KNOX":
                    if(KNOX_LOOKUP_MAP.containsKey(tone))
                    {
                        return KNOX_LOOKUP_MAP.get(tone);
                    }
                    break;
                case "TONE":
                    if(TONE_LOOKUP_MAP.containsKey(tone))
                    {
                        return TONE_LOOKUP_MAP.get(tone);
                    }
                    break;
            }
        }

        return INVALID;
    }
}
