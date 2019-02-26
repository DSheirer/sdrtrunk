/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ChannelNumber;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ISCHSequence;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.SuperframeSequence;

/**
 * Inter-slot signalling channel informational (ISCH-I) parsing class
 */
public class InterSlotSignallingChannel
{
    private static final int[] RESERVED = {0, 1};
    private static final int[] CHANNEL_NUMBER = {2, 3};
    private static final int[] ISCH_SEQUENCE = {4, 5};
    private static final int INBOUND_SACCH_FREE_INDICATOR = 6;
    private static final int[] SUPERFRAME_SEQUENCE = {7, 8};

    private CorrectedBinaryMessage mMessage;

    /**
     * Constructs the ISCH-I parsing class
     *
     * @param message containing bits
     */
    public InterSlotSignallingChannel(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    /**
     * Channel number.
     *
     * @return channel number 0 or 1
     */
    public ChannelNumber getChannelNumber()
    {
        return ChannelNumber.fromValue(mMessage.getInt(CHANNEL_NUMBER));
    }

    /**
     * Indicates this ISCH sequence location within a superframe
     *
     * @return location 1, 2, or 3
     */
    public ISCHSequence getIschSequence()
    {
        return ISCHSequence.fromValue(mMessage.getInt(ISCH_SEQUENCE));
    }

    /**
     * Indicates if the next inbound SACCH timeslot is free for mobile access
     *
     * @return true if the inbound SACCH is open/free
     */
    public boolean isInboundSacchFree()
    {
        return mMessage.get(INBOUND_SACCH_FREE_INDICATOR);
    }

    /**
     * Superframe sequence/location within an ultraframe
     *
     * @return location, 1-4
     */
    public SuperframeSequence getSuperframeSequence()
    {
        return SuperframeSequence.fromValue(mMessage.getInt(SUPERFRAME_SEQUENCE));
    }

    /**
     * Decoded string representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ISCHI");

        ChannelNumber channelNumber = getChannelNumber();

        if(channelNumber == ChannelNumber.VOICE_CHANNEL_0)
        {
            sb.append(" VCH0");
        }
        else if(channelNumber == ChannelNumber.VOICE_CHANNEL_1)
        {
            sb.append(" VCH1");
        }

        switch(getSuperframeSequence())
        {
            case SUPERFRAME_1:
                sb.append(" SF1");
                break;
            case SUPERFRAME_2:
                sb.append(" SF2");
                break;
            case SUPERFRAME_3:
                sb.append(" SF3");
                break;
            case SUPERFRAME_4:
                sb.append(" SF4");
                break;
        }

        sb.append(isInboundSacchFree() ? " SACCH:FREE" : "SACCH:BUSY");

        return sb.toString();
    }
}
