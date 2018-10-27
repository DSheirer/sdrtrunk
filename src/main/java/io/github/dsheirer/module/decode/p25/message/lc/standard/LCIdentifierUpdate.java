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

package io.github.dsheirer.module.decode.p25.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;

import java.util.Collections;
import java.util.List;

/**
 * Echo of the status update from a subscriber when the destination of the update is another subscriber unit.
 */
public class LCIdentifierUpdate extends LinkControlWord implements IFrequencyBand
{
    private static final int[] FREQUENCY_BAND_IDENTIFIER = {8, 9, 10, 11};
    private static final int[] BANDWIDTH = {12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static final int TRANSMIT_OFFSET_SIGN = 21;
    private static final int[] TRANSMIT_OFFSET = {22, 23, 24, 25, 26, 27, 28, 29};
    private static final int[] CHANNEL_SPACING = {30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BASE_FREQUENCY = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
            65, 66, 67, 68, 69, 70, 71};

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCIdentifierUpdate(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ID:").append(getIdentifier());
        sb.append(" BW:").append(getBandwidth());
        sb.append(" OFFSET:").append(getTransmitOffset());
        sb.append(" SPACING:").append(getChannelSpacing());
        sb.append(" BASE:").append(getBaseFrequency());
        return sb.toString();
    }

    @Override
    public int getIdentifier()
    {
        return getMessage().getInt(FREQUENCY_BAND_IDENTIFIER);
    }

    @Override
    public long getChannelSpacing()
    {
        return getMessage().getInt(CHANNEL_SPACING) * 125l;
    }

    @Override
    public long getBaseFrequency()
    {
        return getMessage().getLong(BASE_FREQUENCY) * 5l;
    }

    @Override
    public int getBandwidth()
    {
        return getMessage().getInt(BANDWIDTH) * 125;
    }

    @Override
    public long getTransmitOffset()
    {
        long offset = getMessage().getLong(TRANSMIT_OFFSET) * 250000l;

        if(!getMessage().get(TRANSMIT_OFFSET_SIGN))
        {
            offset *= -1;
        }

        return offset;
    }

    /**
     * Indicates if the frequency band has a transmit option for the subscriber unit.
     */
    public boolean hasTransmitOffset()
    {
        return getMessage().getInt(TRANSMIT_OFFSET) != 0x80;
    }

    @Override
    public long getDownlinkFrequency(int channelNumber)
    {
        return getBaseFrequency() + (channelNumber * getChannelSpacing());
    }

    @Override
    public long getUplinkFrequency(int channelNumber)
    {
        if(hasTransmitOffset())
        {
            return getDownlinkFrequency(channelNumber) + getTransmitOffset();
        }

        return 0;
    }

    @Override
    public boolean isTDMA()
    {
        return false;
    }

    @Override
    public int getTimeslotCount()
    {
        return 1;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
