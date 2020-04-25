/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ChannelType
{
    TYPE_0(AccessType.FDMA, 12500, 1, Vocoder.HALF_RATE),
    TYPE_1(AccessType.FDMA, 12500, 1, Vocoder.FULL_RATE),
    TYPE_2(AccessType.FDMA, 6250, 1, Vocoder.HALF_RATE),
    TYPE_3(AccessType.TDMA, 12500, 2, Vocoder.HALF_RATE),
    TYPE_4(AccessType.TDMA, 25000, 4, Vocoder.HALF_RATE),
    TYPE_5(AccessType.TDMA, 12500, 2, Vocoder.HALF_RATE), //HD8PSK simulcast
    UNKNOWN(AccessType.UNKNOWN, 0, 1, Vocoder.HALF_RATE);

    private AccessType mAccessType;
    private int mBandwidth;
    private int mSlotsPerCarrier;
    private Vocoder mVocoder;

    ChannelType(AccessType accessType, int bandwidth, int slots, Vocoder vocoder)
    {
        mAccessType = accessType;
        mBandwidth = bandwidth;
        mSlotsPerCarrier = slots;
        mVocoder = vocoder;
    }

    private final static Logger mLog = LoggerFactory.getLogger(ChannelType.class);

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isTDMA())
        {
            sb.append("TDMA");
            if(this.equals(TYPE_5))
            {
                sb.append(" H-D8PSK");
            }
            sb.append(" BW:").append(mBandwidth);
            sb.append(" TIMESLOTS:").append(mSlotsPerCarrier);
            sb.append(" VOCODER:").append(mVocoder.name());
        }
        else
        {
            sb.append("FDMA");
            sb.append(" BW:").append(mBandwidth);
            sb.append(" VOCODER:").append(mVocoder.name());
        }

        return sb.toString();
    }

    public boolean isTDMA()
    {
        return getAccessType() == AccessType.TDMA;
    }

    public static ChannelType fromValue(int value)
    {
        if(0 <= value && value <= 5)
        {
            return ChannelType.values()[value];
        }

        return ChannelType.UNKNOWN;
    }

    public AccessType getAccessType()
    {
        return mAccessType;
    }

    public int getBandwidth()
    {
        return mBandwidth;
    }

    public int getSlotsPerCarrier()
    {
        return mSlotsPerCarrier;
    }

    public Vocoder getVocoder()
    {
        return mVocoder;
    }
}