/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;

public class ChannelIdentifierUpdateFrequency extends TDULinkControlMessage implements IFrequencyBand
{
    public static final int[] IDENTIFIER = {72, 73, 74, 75};
    public static final int[] BANDWIDTH = {88, 89, 90, 91, 92, 93, 94, 95, 96};
    public static final int[] TRANSMIT_OFFSET = {97, 98, 99, 112, 113, 114, 115, 116,
        117};
    public static final int[] CHANNEL_SPACING = {118, 119, 120, 121, 122, 123, 136,
        137, 138, 139};
    public static final int[] BASE_FREQUENCY = {140, 141, 142, 143, 144, 145, 146,
        147, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186,
        187, 188, 189, 190, 191, 192, 193, 194, 195};

    public ChannelIdentifierUpdateFrequency(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getEventType()
    {
        return LinkControlOpcode.CHANNEL_IDENTIFIER_UPDATE.getDescription();
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" IDEN:" + getIdentifier());

        sb.append(" BASE:" + getBaseFrequency());

        sb.append(" BW:" + getBandwidth());

        sb.append(" SPACING:" + getChannelSpacing());

        sb.append(" OFFSET:" + getTransmitOffset());

        return sb.toString();
    }

    @Override
    public int getIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
    }

    /**
     * Channel bandwidth in hertz
     */
    @Override
    public int getBandwidth()
    {
        return mMessage.getInt(BANDWIDTH) * 125;
    }

    @Override
    public long getChannelSpacing()
    {
        return mMessage.getLong(CHANNEL_SPACING) * 125l;
    }

    @Override
    public long getBaseFrequency()
    {
        return mMessage.getLong(BASE_FREQUENCY) * 5l;
    }

    @Override
    public long getTransmitOffset()
    {
        return -1 * mMessage.getLong(TRANSMIT_OFFSET) * 250000l;
    }

    @Override
    public long getDownlinkFrequency(int channelNumber)
    {
        return getBaseFrequency() + (channelNumber * getChannelSpacing());
    }

    @Override
    public long getUplinkFrequency(int channelNumber)
    {
        return getDownlinkFrequency(channelNumber) + getTransmitOffset();
    }

    @Override
    public boolean isTDMA()
    {
        return false;
    }
}
