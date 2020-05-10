/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr;

import java.util.EnumSet;

public enum DMRSyncPattern
{
    BASE_STATION_VOICE(0x755FD7DF75F7l, "BS VOICE"),
    BASE_STATION_VOICE_P90(0xEFFABEBAEFAEl, "BS VOICE CW90"),
    BASE_STATION_VOICE_N90(0x100541451051l, "BS VOICE CCW90"),
    BASE_STATION_VOICE_180(0x8AA028208A08l, "BS VOICE 180"),

    BASE_STATION_DATA(0xDFF57D75DF5Dl, "BS DATA"),
    BASE_STATION_DATA_P90(0xBAAFEBEFBAFBl, "BS DATA CW90"),
    BASE_STATION_DATA_N90(0x455014104504l, "BS DATA CCW90"),
    BASE_STATION_DATA_180(0x200A828A20A2l, "BS DATA 180"),

    MOBILE_STATION_VOICE(0x7F7D5DD57DFDl, "MS VOICE"),
    MOBILE_STATION_VOICE_P90(0xEAEBFBBFEBABl, "MS VOICE CW90"),
    MOBILE_STATION_VOICE_N90(0x151404401454l, "MS VOICE CCW90"),
    MOBILE_STATION_VOICE_180(0x8082A22A8202l, "MS VOICE 180"),

    MOBILE_STATION_DATA(0xD5D7F77FD757l, "MS DATA"),
    MOBILE_STATION_DATA_P90(0xBFBEAEEABEFEl, "MS DATA CW90"),
    MOBILE_STATION_DATA_N90(0x404151154101l, "MS DATA CCW90"),
    MOBILE_STATION_DATA_180(0x2A28088028A8l, "MS DATA 180"),


    MOBILE_STATION_REVERSE_CHANNEL(0x77D55F7DFD77l, "MS REVERSE"),
    DIRECT_MODE_DATA_TIMESLOT_1(0xF7FDD5DDFD55l, "DM DATA TS1"),
    DIRECT_MODE_DATA_TIMESLOT_2(0xD7557F5FF7F5l, "DM DATA TS2"),
    DIRECT_MODE_VOICE_TIMESLOT_1(0x5D577F7757FFl, "DM VOICE TS1"),
    DIRECT_MODE_VOICE_TIMESLOT_2(0x7DFFD5F55D5Fl, "DM VOICE TS2"),

    RESERVED(0xDD7FF5D757DDl, "RESERVED"),

    P25_PHASE1_ERROR_90_CCW( 0x96b07fdfe318l, "" ),//96b07fdfe318, 5ac1ff7f8c61, bcd0958e2b48
    P25_PHASE1_ERROR_90_CW(  0x001050551155l,"" ),
    P25_PHASE1_ERROR_180(    0xAA8A0A008800l,"" ),

    //These are used to identify the sync-less sub frames of the voice super frame
    VOICE_FRAME_B(-2, "VOICE B"),
    VOICE_FRAME_C(-3, "VOICE C"),
    VOICE_FRAME_D(-4, "VOICE D"),
    VOICE_FRAME_E(-5, "VOICE E"),
    VOICE_FRAME_F(-6, "VOICE F"),

    UNKNOWN(-1, "UNKNOWN");

    private long mPattern;
    private String mLabel;

    //Valid sync patterns (excluding the sync-less voice patterns)
    public static final EnumSet<DMRSyncPattern> SYNC_PATTERNS = EnumSet.range(BASE_STATION_VOICE, RESERVED);

    //Sync patterns containing a Common Announcement Channel (CACH)
    public static final EnumSet<DMRSyncPattern> CACH_PATTERNS = EnumSet.of(BASE_STATION_DATA, BASE_STATION_VOICE);

    //Sync patterns for Data Frames
    public static final EnumSet<DMRSyncPattern> DATA_PATTERNS = EnumSet.of(BASE_STATION_DATA, MOBILE_STATION_DATA);

    //Sync patterns for Voice Frames
    public static final EnumSet<DMRSyncPattern> VOICE_PATTERNS = EnumSet.of(BASE_STATION_VOICE, MOBILE_STATION_VOICE,
            VOICE_FRAME_B, VOICE_FRAME_C, VOICE_FRAME_D, VOICE_FRAME_E, VOICE_FRAME_F);

    /**
     * DMR Sync Patterns.  See TS 102-361-1, paragraph 9.1.1
     */
    DMRSyncPattern(long pattern, String label)
    {
        mPattern = pattern;
        mLabel = label;
    }

    /**
     * Pattern that represents the enum entry
     */
    public long getPattern()
    {
        return mPattern;
    }

    /**
     * String representation of the sync pattern
     */
    public String toString()
    {
        return mLabel;
    }

    /**
     * Indicates if a message using this sync pattern contains a Common Announcement Channel (CACH).
     */
    public boolean hasCACH()
    {
        return CACH_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a data sync pattern
     */
    public boolean isData()
    {
        return DATA_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a voice sync pattern
     */
    public boolean isVoice()
    {
        return VOICE_PATTERNS.contains(this);
    }

    /**
     * Lookup the DMR Sync Pattern from the transmitted value.
     * @param value to match to a pattern
     * @return the matching enum entry or UNKNOWN
     */
    public static DMRSyncPattern fromValue(long value)
    {
        if(value < 0) {
            for(DMRSyncPattern pattern: VOICE_PATTERNS)
            {
                if(pattern.getPattern() == value)
                {
                    return pattern;
                }
            }
            return UNKNOWN;
        }
        for(DMRSyncPattern pattern: SYNC_PATTERNS)
        {
            if(pattern.getPattern() == value)
            {
                return pattern;
            }
        }

        return UNKNOWN;
    }
}