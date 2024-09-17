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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.reference.ChannelType;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.util.FastMath;

/**
 * Frequency band update TDMA multi-block format for a foreign system.
 *
 * Note: removed the IFrequencyBand interface for this message so that it doesn't get processed as a frequency band for
 * the current system which would likely collide with the real frequency band for the current system.
 */
public class AMBTCFrequencyBandUpdateTDMA extends AMBTCMessage // implements IFrequencyBand
{
    private static final IntField HEADER_IDENTIFIER = IntField.length4(OCTET_3_BIT_24);
    private static final IntField HEADER_CHANNEL_TYPE = IntField.length4(OCTET_3_BIT_24 + 4);
    private static final FragmentedIntField HEADER_WACN = FragmentedIntField.of(32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 64, 65, 66, 67);
    private static final IntField HEADER_SYSTEM = IntField.length12(OCTET_8_BIT_64 + 4);
    private static final LongField BLOCK_0_BASE_FREQUENCY = LongField.length32(OCTET_0_BIT_0);
    private static final int BLOCK_0_TRANSMIT_OFFSET_SIGN = OCTET_4_BIT_32;
    private static final IntField BLOCK_0_TRANSMIT_OFFSET = IntField.range(OCTET_4_BIT_32 + 1, OCTET_4_BIT_32 + 13);
    private static final IntField BLOCK_0_CHANNEL_SPACING = IntField.range(OCTET_5_BIT_40 + 6, OCTET_5_BIT_40 + 15);

    private ChannelType mChannelType;
    private Identifier mWacn;
    private Identifier mSystem;

    /**
     * Constructs an instance
     * @param PDUSequence containing the header and block 0
     * @param nac for the system
     * @param timestamp of the PDU sequence
     */
    public AMBTCFrequencyBandUpdateTDMA(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWacn());
        sb.append(" FOREIGN SYSTEM:").append(getSystem());
        sb.append(" ID:").append(getIdentifier());
        sb.append(" OFFSET:").append(getTransmitOffset());
        sb.append(" SPACING:").append(getChannelSpacing());
        sb.append(" BASE:").append(getBaseFrequency());
        sb.append(" ").append(getChannelType());
        return sb.toString();
    }

    public Identifier getWacn()
    {
        if(mWacn == null)
        {
            mWacn = APCO25Wacn.create(getHeader().getMessage().getInt(HEADER_WACN));
        }

        return mWacn;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getHeader().getMessage().getInt(HEADER_SYSTEM));
        }

        return mSystem;
    }

    public ChannelType getChannelType()
    {
        if(mChannelType == null)
        {
            mChannelType = ChannelType.fromValue(getHeader().getMessage().getInt(HEADER_CHANNEL_TYPE));
        }

        return mChannelType;
    }

    public int getIdentifier()
    {
        return getHeader().getMessage().getInt(HEADER_IDENTIFIER);
    }

    public long getChannelSpacing()
    {
        if(hasDataBlock(0))
        {
            return getDataBlock(0).getMessage().getInt(BLOCK_0_CHANNEL_SPACING) * 125L;
        }

        return 0;
    }

    public long getBaseFrequency()
    {
        if(hasDataBlock(0))
        {
            return getDataBlock(0).getMessage().getLong(BLOCK_0_BASE_FREQUENCY) * 5L;
        }

        return 0L;
    }

    public int getBandwidth()
    {
        return getChannelType().getBandwidth();
    }

    public long getTransmitOffset()
    {
        if(hasDataBlock(0))
        {
            long offset = getDataBlock(0).getMessage().getInt(BLOCK_0_TRANSMIT_OFFSET) * getChannelSpacing();

            if(!getDataBlock(0).getMessage().get(BLOCK_0_TRANSMIT_OFFSET_SIGN))
            {
                offset *= -1;
            }

            return offset;
        }

        return 0L;
    }

    /**
     * Indicates if the frequency band has a transmit option for the subscriber unit.
     */
    public boolean hasTransmitOffset()
    {
        return hasDataBlock(0) && getDataBlock(0).getMessage().getInt(BLOCK_0_TRANSMIT_OFFSET) != 0x80;
    }

    public long getDownlinkFrequency(int channelNumber)
    {
        return getBaseFrequency() + (getChannelSpacing() * (int)(FastMath.floor(channelNumber / getTimeslotCount())));
    }

    public long getUplinkFrequency(int channelNumber)
    {
        if(hasTransmitOffset())
        {
            return getDownlinkFrequency(channelNumber) + getTransmitOffset();
        }

        return 0;
    }

    public boolean isTDMA()
    {
        return getChannelType().isTDMA();
    }

    public int getTimeslotCount()
    {
        return getChannelType().getSlotsPerCarrier();
    }

    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
