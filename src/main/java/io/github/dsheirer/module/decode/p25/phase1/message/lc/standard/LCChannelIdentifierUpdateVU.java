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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.Collections;
import java.util.List;

/**
 * Channel identifier update VHF/UHF
 */
public class LCChannelIdentifierUpdateVU extends LinkControlWord implements IFrequencyBand
{
    private static final IntField FREQUENCY_BAND_IDENTIFIER = IntField.length4(OCTET_1_BIT_8);
    private static final IntField BANDWIDTH = IntField.length4(OCTET_1_BIT_8 + 4);
    private static final int TRANSMIT_OFFSET_SIGN = 16;
    private static final FragmentedIntField TRANSMIT_OFFSET = FragmentedIntField.of(17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29);
    private static final FragmentedIntField CHANNEL_SPACING = FragmentedIntField.of(30, 31, 32, 33, 34, 35, 36, 37, 38, 39);
    private static final LongField BASE_FREQUENCY = LongField.length32(OCTET_5_BIT_40);

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCChannelIdentifierUpdateVU(CorrectedBinaryMessage message)
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
        return getInt(FREQUENCY_BAND_IDENTIFIER);
    }

    @Override
    public long getChannelSpacing()
    {
        return getInt(CHANNEL_SPACING) * 125l;
    }

    @Override
    public long getBaseFrequency()
    {
        return getLong(BASE_FREQUENCY) * 5l;
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
        return getInt(TRANSMIT_OFFSET) != 0x80;
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
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
