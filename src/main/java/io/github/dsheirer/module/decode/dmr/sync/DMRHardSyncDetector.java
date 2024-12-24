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

/**
 * DMR sync detector that processes hard symbol (ie dibit) decisions.
 */
public class DMRHardSyncDetector extends DMRSyncDetector
{
    private static final long SYNC_MASK = 0xFFFFFFFFFFFFL;
    private static final int MAXIMUM_BIT_ERROR = 5;
    private long mValue;
    private DMRSyncPattern mDetectedPattern = DMRSyncPattern.UNKNOWN;

    /**
     * Constructs an instance
     */
    public DMRHardSyncDetector()
    {
    }

    public DMRSyncPattern getDetectedPattern()
    {
        return mDetectedPattern;
    }

    /**
     * Processes the dibit.
     * @param dibit to process
     * @return true if a sync pattern is detected.
     */
    public boolean process(Dibit dibit)
    {
        mValue = (Long.rotateLeft(mValue, 2) & SYNC_MASK) + dibit.getValue();

        int bitCount = Long.bitCount(mValue ^ DMRSyncPattern.BASE_STATION_DATA.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.BASE_STATION_DATA;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.BASE_STATION_VOICE.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.BASE_STATION_VOICE;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.MOBILE_STATION_DATA.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.MOBILE_STATION_DATA;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.MOBILE_STATION_VOICE.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.MOBILE_STATION_VOICE;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.DIRECT_DATA_TIMESLOT_1.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_1;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.DIRECT_DATA_TIMESLOT_2.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_2;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1;
            return true;
        }

        bitCount = Long.bitCount(mValue ^ DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2.getPattern());
        if(bitCount <= MAXIMUM_BIT_ERROR)
        {
            mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2;
            return true;
        }

        return false;
    }
}
