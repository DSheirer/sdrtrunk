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
package module.decode.p25.message.tsbk;

import alias.AliasList;
import bits.BinaryMessage;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.reference.DataUnitID;

public abstract class ChannelGrant extends ServiceMessage implements IdentifierReceiver
{
    public static final int[] PRIORITY = {85, 86, 87};
    public static final int[] CHANNEL_IDENTIFIER = {88, 89, 90, 91};
    public static final int[] CHANNEL_NUMBER = {92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};

    private IBandIdentifier mIdentifierUpdate;

    public ChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    /**
     * Indicates if this channel grant is for a TDMA channel
     */
    public boolean isTDMAChannel()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.isTDMA();
        }

        return false;
    }

    /**
     * 1 = Lowest, 4 = Default, 7 = Highest
     */
    public int getPriority()
    {
        return mMessage.getInt(PRIORITY);
    }

    public int getChannelIdentifier()
    {
        return mMessage.getInt(CHANNEL_IDENTIFIER);
    }

    public int getChannelNumber()
    {
        return mMessage.getInt(CHANNEL_NUMBER);
    }

    public String getChannel()
    {
        return getChannelIdentifier() + "-" + getChannelNumber();
    }

    @Override
    public void setIdentifierMessage(int identifier, IBandIdentifier message)
    {
        mIdentifierUpdate = message;
    }

    @Override
    public int[] getIdentifiers()
    {
        int[] identifiers = new int[1];
        identifiers[0] = getChannelIdentifier();

        return identifiers;
    }

    public long getDownlinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.getDownlinkFrequency(getChannelNumber());
        }

        return 0;
    }

    public long getUplinkFrequency()
    {
        if(mIdentifierUpdate != null)
        {
            return mIdentifierUpdate.getUplinkFrequency(getChannelNumber());
        }

        return 0;
    }
}
