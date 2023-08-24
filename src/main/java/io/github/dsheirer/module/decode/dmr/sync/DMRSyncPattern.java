/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr.sync;

import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.EnumSet;

/**
 * DMR burst sync patterns enumeration.
 *
 * Note: in addition to the ETSI-defined patterns, there are additional entries that are used as placeholder sync
 * patterns for tracking bursts across voice super frames and for tracking empty timeslots for direct mode.
 */
public enum DMRSyncPattern
{
    BASE_STATION_DATA(0xDFF57D75DF5DL, "BS DATA"),
    BASE_STATION_VOICE(0x755FD7DF75F7L, "BS VOICE A"),

    //These are used to identify the sync-less sub frames of a voice super frame
    BS_VOICE_FRAME_B("BS VOICE B"),
    BS_VOICE_FRAME_C("BS VOICE C"),
    BS_VOICE_FRAME_D("BS VOICE D"),
    BS_VOICE_FRAME_E("BS VOICE E"),
    BS_VOICE_FRAME_F("BS VOICE F"),

    MOBILE_STATION_DATA(0xD5D7F77FD757L, "MS DATA"),
    MOBILE_STATION_VOICE(0x7F7D5DD57DFDL, "MS VOICE A"),
    REVERSE_CHANNEL(0x77D55F7DFD77L, "MS REVERSE"),

    //These are used to identify the sync-less sub frames of a voice super frame
    MS_VOICE_FRAME_B("MS VOICE B"),
    MS_VOICE_FRAME_C("MS VOICE C"),
    MS_VOICE_FRAME_D("MS VOICE D"),
    MS_VOICE_FRAME_E("MS VOICE E"),
    MS_VOICE_FRAME_F("MS VOICE F"),

    DIRECT_EMPTY_TIMESLOT("DIRECT EMPTY TIMESLOT"),

    DIRECT_DATA_TIMESLOT_1(0xF7FDD5DDFD55L, "DIRECT DATA TS 1"),
    DIRECT_DATA_TIMESLOT_2(0xD7557F5FF7F5L, "DIRECT DATA TS 2"),
    DIRECT_VOICE_TIMESLOT_1(0x5D577F7757FFL, "DIRECT VOICE A TS 1"),
    DIRECT_VOICE_TIMESLOT_2(0x7DFFD5F55D5FL, "DIRECT VOICE A TS 2"),

    //These are used to identify the sync-less sub frames of a voice super frame
    DIRECT_VOICE_FRAME_B("DIRECT VOICE B"),
    DIRECT_VOICE_FRAME_C("DIRECT VOICE C"),
    DIRECT_VOICE_FRAME_D("DIRECT VOICE D"),
    DIRECT_VOICE_FRAME_E("DIRECT VOICE E"),
    DIRECT_VOICE_FRAME_F("DIRECT VOICE F"),

    RESERVED(0xDD7FF5D757DDl, "RESERVED"),
    UNKNOWN("UNKNOWN");

    private long mPattern;
    private String mLabel;

    /**
     * DMR Sync Patterns.  See TS 102-361-1, paragraph 9.1.1
     */
    DMRSyncPattern(long pattern, String label)
    {
        mPattern = pattern;
        mLabel = label;
    }

    /**
     * Alternate constructor for syncs that don't have defined patterns.
     */
    DMRSyncPattern(String label)
    {
        mLabel = label;
    }

    //Valid sync patterns (excluding the sync-less voice patterns)
    public static final EnumSet<DMRSyncPattern> SYNC_PATTERNS = EnumSet.of(BASE_STATION_DATA, BASE_STATION_VOICE,
        MOBILE_STATION_DATA, MOBILE_STATION_VOICE, DIRECT_DATA_TIMESLOT_1, DIRECT_DATA_TIMESLOT_2,
            DIRECT_VOICE_TIMESLOT_1, DIRECT_VOICE_TIMESLOT_2);

    //Sync patterns containing a Common Announcement Channel (CACH)
    private static final EnumSet<DMRSyncPattern> CACH_PATTERNS = EnumSet.of(BASE_STATION_DATA, BASE_STATION_VOICE,
        BS_VOICE_FRAME_B, BS_VOICE_FRAME_C, BS_VOICE_FRAME_D, BS_VOICE_FRAME_E, BS_VOICE_FRAME_F);

    //Direct Mode TS 1 Sync Patterns
    private static final EnumSet<DMRSyncPattern> DIRECT_MODE_TS1_PATTERNS = EnumSet.of(DIRECT_VOICE_TIMESLOT_1,
            DIRECT_DATA_TIMESLOT_1);

    //Direct Mode TS 2 Sync Patterns
    private static final EnumSet<DMRSyncPattern> DIRECT_MODE_TS2_PATTERNS = EnumSet.of(DIRECT_VOICE_TIMESLOT_2,
            DIRECT_DATA_TIMESLOT_2);

    //Direct Mode Pseudo Voice Frame Patterns
    private static final EnumSet<DMRSyncPattern> DIRECT_MODE_VOICE_PATTERNS = EnumSet.range(DIRECT_VOICE_FRAME_B,
            DIRECT_VOICE_FRAME_F);

    //Mobile Station Sync Patterns
    private static final EnumSet<DMRSyncPattern> MOBILE_STATION_PATTERNS = EnumSet.of(MOBILE_STATION_DATA,
            MOBILE_STATION_VOICE, MS_VOICE_FRAME_B, MS_VOICE_FRAME_C, MS_VOICE_FRAME_D, MS_VOICE_FRAME_E,
            MS_VOICE_FRAME_F);

    //Voice frame patterns
    private static final EnumSet<DMRSyncPattern> VOICE_PATTERNS = EnumSet.of(BASE_STATION_VOICE, BS_VOICE_FRAME_B,
            BS_VOICE_FRAME_C, BS_VOICE_FRAME_D, BS_VOICE_FRAME_E, BS_VOICE_FRAME_F, MOBILE_STATION_VOICE,
            MS_VOICE_FRAME_B, MS_VOICE_FRAME_C, MS_VOICE_FRAME_D, MS_VOICE_FRAME_E, MS_VOICE_FRAME_F,
            DIRECT_VOICE_TIMESLOT_1, DIRECT_VOICE_TIMESLOT_2, DIRECT_EMPTY_TIMESLOT, DIRECT_VOICE_FRAME_B,
            DIRECT_VOICE_FRAME_C, DIRECT_VOICE_FRAME_D, DIRECT_VOICE_FRAME_E, DIRECT_VOICE_FRAME_F);

    /**
     * Pattern that represents the enum entry
     */
    public long getPattern()
    {
        return mPattern;
    }

    /**
     * Indicates if a message using this sync pattern contains a Common Announcement Channel (CACH).
     */
    public boolean hasCACH()
    {
        return CACH_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a Direct mode sync pattern, either timeslot 1 or 2.
     */
    public boolean isDirect()
    {
        return isDirectTS1() || isDirectTS2() || isDirectVoice();
    }

    /**
     * Indicates if this is Direct Mode sync pattern for timeslot 1
     * @return true if Direct Mode TS1
     */
    public boolean isDirectTS1()
    {
        return DIRECT_MODE_TS1_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is Direct Mode sync pattern for timeslot 2
     * @return true if Direct Mode TS2
     */
    public boolean isDirectTS2()
    {
        return DIRECT_MODE_TS2_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is Direct Mode voice sync pattern
     * @return true if Direct Mode Voice
     */
    public boolean isDirectVoice()
    {
        return DIRECT_MODE_VOICE_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a mobile station sync pattern
     * @return true if a mobile station sync pattern.
     */
    public boolean isMobileStationSyncPattern()
    {
        return MOBILE_STATION_PATTERNS.contains(this);
    }

    /**
     * Indicates if this is a mobile sync pattern, either mobile station or direct mode.
     * @return true if a mobile sync pattern.
     */
    public boolean isMobileSyncPattern()
    {
        return isMobileStationSyncPattern() || isDirect();
    }

    /**
     * Indicates if the current sync pattern is a VOICE pattern.
     * @return true if a voice sync pattern.
     */
    public boolean isVoicePattern()
    {
        return VOICE_PATTERNS.contains(this);
    }

    /**
     * Determines the next voice sync pattern that follows the argument pattern.
     * @param pattern for the current burst
     * @return pattern for the next burst, or UNKNOWN
     */
    public static DMRSyncPattern getNextVoice(DMRSyncPattern pattern)
    {
        return switch (pattern)
        {
            case BASE_STATION_VOICE -> BS_VOICE_FRAME_B;
            case BS_VOICE_FRAME_B -> BS_VOICE_FRAME_C;
            case BS_VOICE_FRAME_C -> BS_VOICE_FRAME_D;
            case BS_VOICE_FRAME_D -> BS_VOICE_FRAME_E;
            case BS_VOICE_FRAME_E -> BS_VOICE_FRAME_F;

            case MOBILE_STATION_VOICE -> MS_VOICE_FRAME_B;
            case MS_VOICE_FRAME_B -> MS_VOICE_FRAME_C;
            case MS_VOICE_FRAME_C -> MS_VOICE_FRAME_D;
            case MS_VOICE_FRAME_D -> MS_VOICE_FRAME_E;
            case MS_VOICE_FRAME_E -> MS_VOICE_FRAME_F;

            case DIRECT_VOICE_TIMESLOT_1, DIRECT_VOICE_TIMESLOT_2 -> DIRECT_VOICE_FRAME_B;
            case DIRECT_VOICE_FRAME_B -> DIRECT_VOICE_FRAME_C;
            case DIRECT_VOICE_FRAME_C -> DIRECT_VOICE_FRAME_D;
            case DIRECT_VOICE_FRAME_D -> DIRECT_VOICE_FRAME_E;
            case DIRECT_VOICE_FRAME_E -> DIRECT_VOICE_FRAME_F;

            //Empty slot pattern remains the same to continue accounting for the empty timeslot.
            case DIRECT_EMPTY_TIMESLOT -> DIRECT_EMPTY_TIMESLOT;

            default -> UNKNOWN;
        };
    }

    /**
     * String representation of the sync pattern
     */
    public String toString()
    {
        return mLabel;
    }

    /**
     * Converts the DMR sync pattern to a float array of ideal phase values for each Dibit to use for correlation against
     * a stream of transmitted symbol phases.
     * @return symbols array representing the sync pattern.
     */
    public float[] toSymbols()
    {
        float[] symbols = new float[24];
        Dibit[] dibits = toDibits();

        for(int x = 0; x < 24; x++)
        {
            symbols[x] = dibits[x].getIdealPhase();
        }

        return symbols;
    }

    /**
     * Converts the DMR sync pattern to an array of dibit symbols.  Note: only the +3 and -3 dibit symbols are used in
     * sync patterns.
     * @return dibit symbols array representing the sync pattern.
     */
    public Dibit[] toDibits()
    {
        Dibit[] dibits = new Dibit[24];
        long value = getPattern();
        long mask = 3;
        long dibitValue;

        for(int x = 0; x < 24; x++)
        {
            dibitValue = (value & mask) >> (2 * x);

            if(dibitValue == 1)
            {
                dibits[23 - x] = Dibit.D01_PLUS_3;
            }
            else
            {
                dibits[23 - x] = Dibit.D11_MINUS_3;
            }

            mask = mask << 2;
        }

        return dibits;
    }
}