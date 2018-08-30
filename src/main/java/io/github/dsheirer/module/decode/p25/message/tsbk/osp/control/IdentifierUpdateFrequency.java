/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public abstract class IdentifierUpdateFrequency extends TSBKMessage implements IFrequencyBand
{
    public static final int[] IDENTIFIER = {80, 81, 82, 83};
    public static final int[] CHANNEL_SPACING = {102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] BASE_FREQUENCY = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    public IdentifierUpdateFrequency(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public int getIdentifier()
    {
        return mMessage.getInt(IDENTIFIER);
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
    public abstract long getTransmitOffset();

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
