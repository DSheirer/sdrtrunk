/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;

import java.util.Collections;
import java.util.List;

/**
 * Identifier Update - VHF/UHF Frequency Band details
 */
public class FrequencyBandUpdateVUHF extends OSPMessage implements IFrequencyBand
{
    private static final int[] FREQUENCY_BAND_IDENTIFIER = {16, 17, 18, 19};
    private static final int[] BANDWIDTH = {20, 21, 22, 23};
    private static final int TRANSMIT_OFFSET_SIGN = 24;
    private static final int[] TRANSMIT_OFFSET = {25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37};
    private static final int[] CHANNEL_SPACING = {38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] BASE_FREQUENCY = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public FrequencyBandUpdateVUHF(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ID:").append(getIdentifier());
        sb.append(" OFFSET:").append(getTransmitOffset());
        sb.append(" SPACING:").append(getChannelSpacing());
        sb.append(" BASE:").append(getBaseFrequency());
        sb.append(" FDMA BW:").append(getBandwidth());
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
        return getMessage().getInt(CHANNEL_SPACING) * 125;
    }

    @Override
    public long getBaseFrequency()
    {
        return getMessage().getLong(BASE_FREQUENCY) * 5;
    }

    @Override
    public int getBandwidth()
    {
        int bandwidth = getMessage().getInt(BANDWIDTH);

        if(bandwidth == 0x4)
        {
            return 6250;
        }
        else if(bandwidth == 0x5)
        {
            return 12500;
        }

        return 0;
    }

    @Override
    public long getTransmitOffset()
    {
        long offset = getMessage().getLong(TRANSMIT_OFFSET) * getChannelSpacing();

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
        return getBaseFrequency() + (getChannelSpacing() * channelNumber);
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

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
