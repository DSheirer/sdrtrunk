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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import java.util.Collections;
import java.util.List;

/**
 * Identifier update (frequency band) - VHF/UHF bands
 */
public class FrequencyBandUpdateVUHF extends MacStructure implements IFrequencyBand
{
    private static final IntField FREQUENCY_BAND_IDENTIFIER = IntField.length4(OCTET_2_BIT_8);
    private static final IntField BANDWIDTH = IntField.length4(OCTET_2_BIT_8 + 4);
    private static final int TRANSMIT_OFFSET_SIGN = OCTET_3_BIT_16;
    private static final IntField TRANSMIT_OFFSET = IntField.range(17, 29);
    private static final IntField CHANNEL_SPACING = IntField.range(30, 39);
    private static final LongField BASE_FREQUENCY = LongField.length32(OCTET_6_BIT_40);

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public FrequencyBandUpdateVUHF(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
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
        return getInt(FREQUENCY_BAND_IDENTIFIER);
    }

    @Override
    public long getChannelSpacing()
    {
        return getInt(CHANNEL_SPACING) * 125;
    }

    @Override
    public long getBaseFrequency()
    {
        return getLong(BASE_FREQUENCY) * 5; //Units of 5 Hz
    }

    @Override
    public int getBandwidth()
    {
        int bandwidth = getInt(BANDWIDTH);

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
        long offset = getInt(TRANSMIT_OFFSET) * getChannelSpacing();

        if(!getMessage().get(TRANSMIT_OFFSET_SIGN + getOffset()))
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
        return getInt(TRANSMIT_OFFSET) != 0x80;
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
