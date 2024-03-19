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
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.reference.ChannelType;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.util.FastMath;

/**
 * Identifier update (frequency band) - TDMA extended
 */
public class FrequencyBandUpdateTDMAExtended extends MacStructure implements IFrequencyBand
{
    private static final IntField FREQUENCY_BAND_IDENTIFIER = IntField.length4(OCTET_3_BIT_16);
    private static final IntField CHANNEL_TYPE = IntField.length4(OCTET_3_BIT_16 + 4);
    private static final int TRANSMIT_OFFSET_SIGN = 24;
    private static final IntField TRANSMIT_OFFSET = IntField.range(25, 37);
    private static final IntField CHANNEL_SPACING = IntField.range(38, 47);
    private static final LongField BASE_FREQUENCY = LongField.length32(OCTET_7_BIT_48);
    private static final IntField WACN = IntField.length20(OCTET_11_BIT_80);
    private static final IntField SYSTEM = IntField.length12(OCTET_13_BIT_96 + 4);
    private ChannelType mChannelType;
    private Identifier mWacn;
    private Identifier mSystem;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public FrequencyBandUpdateTDMAExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" ").append(getChannelType());
        sb.append(" WACN:").append(getWacn());
        sb.append(" SYSTEM:").append(getSystem());
        return sb.toString();
    }

    public Identifier getWacn()
    {
        if(mWacn == null)
        {
            mWacn = APCO25Wacn.create(getInt(WACN));
        }

        return mWacn;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getInt(SYSTEM));
        }

        return mSystem;
    }

    public ChannelType getChannelType()
    {
        if(mChannelType == null)
        {
            mChannelType = ChannelType.fromValue(getInt(CHANNEL_TYPE));
        }

        return mChannelType;
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
        return getLong(BASE_FREQUENCY) * 5;
    }

    @Override
    public int getBandwidth()
    {
        return getChannelType().getBandwidth();
    }

    @Override
    public long getTransmitOffset()
    {
        long offset = (long)getInt(TRANSMIT_OFFSET) * getChannelType().getBandwidth();

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
        return getBaseFrequency() + (getChannelSpacing() * (int)(FastMath.floor(channelNumber / getTimeslotCount())));
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
        return getChannelType().isTDMA();
    }

    @Override
    public int getTimeslotCount()
    {
        return getChannelType().getSlotsPerCarrier();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
