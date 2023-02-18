/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.type;

import java.util.Map;
import java.util.TreeMap;

/**
 * DMR Tier III Announcement Message Type
 */
public enum AnnouncementType
{
    ANNOUNCE_OR_WITHDRAW_TSCC(0, "ANNOUNCE/WITHDRAW TSCC"),
    CALL_TIMER_PARAMETERS(1, "CALL TIMER PARAMETERS"),
    VOTE_NOW_ADVICE(2, "VOTE NOW ADVICE"),
    LOCAL_TIME(3, "BROADCAST LOCAL TIME"),
    MASS_REGISTRATION(4, "MASS REGISTRATION"),
    CHANNEL_FREQUENCY_ANNOUNCEMENT(5, "CHANNEL FREQUENCY"),
    ADJACENT_SITE_INFORMATION(6, "NEIGHBOR SITE INFORMATION"),
    GENERAL_SITE_INFORMATION(7, "SITE INFORMATION"),
    RESERVED_8(8, "RESERVED 8"),
    RESERVED_9(9, "RESERVED 9"),
    RESERVED_10(10, "RESERVED 10"),
    RESERVED_11(11, "RESERVED 11"),
    RESERVED_12(12, "RESERVED 12"),
    RESERVED_13(13, "RESERVED 13"),
    RESERVED_14(14, "RESERVED 14"),
    RESERVED_15(15, "RESERVED 15"),
    RESERVED_16(16, "RESERVED 16"),
    RESERVED_17(17, "RESERVED 17"),
    RESERVED_18(18, "RESERVED 18"),
    RESERVED_19(19, "RESERVED 19"),
    RESERVED_20(20, "RESERVED 20"),
    RESERVED_21(21, "RESERVED 21"),
    RESERVED_22(22, "RESERVED 22"),
    RESERVED_23(23, "RESERVED 23"),
    RESERVED_24(24, "RESERVED 24"),
    RESERVED_25(25, "RESERVED 25"),
    RESERVED_26(26, "RESERVED 26"),
    RESERVED_27(27, "RESERVED 27"),
    RESERVED_28(28, "RESERVED 28"),
    RESERVED_29(29, "RESERVED 29"),
    VENDOR_SPECIFIC_30(30, "VENDOR SPECIFIC 30"),
    VENDOR_SPECIFIC_31(31, "VENDOR SPECIFIC 30"),

    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    AnnouncementType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Value for the entry
     */
    public int getValue()
    {
        return mValue;
    }

    private static final Map<Integer,AnnouncementType> LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(AnnouncementType announcementType: AnnouncementType.values())
        {
            LOOKUP_MAP.put(announcementType.getValue(), announcementType);
        }
    }

    /**
     * Utility method to lookup the model type from the integer value
     * @param value 0-3
     * @return entry or UNKNOWN
     */
    public static AnnouncementType fromValue(int value)
    {
        if(LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
