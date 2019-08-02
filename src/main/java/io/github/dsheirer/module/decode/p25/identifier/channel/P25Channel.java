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

package io.github.dsheirer.module.decode.p25.identifier.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

import java.util.Objects;

public class P25Channel implements IChannelDescriptor
{
    private IFrequencyBand mFrequencyBand;
    private int mBandIdentifier;
    private int mChannelNumber;

    public P25Channel(int bandIdentifier, int channelNumber)
    {
        mBandIdentifier = bandIdentifier;
        mChannelNumber = channelNumber;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Frequency band information that backs this channel
     */
    public IFrequencyBand getFrequencyBand()
    {
        return mFrequencyBand;
    }

    public int getDownlinkBandIdentifier()
    {
        return mBandIdentifier;
    }

    public int getDownlinkChannelNumber()
    {
        return mChannelNumber;
    }

    public int getUplinkBandIdentifier()
    {
        return mBandIdentifier;
    }

    public int getUplinkChannelNumber()
    {
        return mChannelNumber;
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mFrequencyBand != null)
        {
            return mFrequencyBand.getDownlinkFrequency(getDownlinkChannelNumber());
        }

        return 0;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mFrequencyBand != null)
        {
            return mFrequencyBand.getUplinkFrequency(getUplinkChannelNumber());
        }

        return 0;
    }

    public boolean hasUplinkChannel()
    {
        return getUplinkChannelNumber() != 4095;
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        if(getDownlinkBandIdentifier() != getUplinkBandIdentifier())
        {
            int[] identifiers = new int[2];
            identifiers[0] = getDownlinkBandIdentifier();
            identifiers[1] = getUplinkBandIdentifier();
            return identifiers;
        }
        else
        {
            int[] identifiers = new int[1];
            identifiers[0] = getDownlinkBandIdentifier();
            return identifiers;
        }
    }

    @Override
    public void setFrequencyBand(IFrequencyBand frequencyBand)
    {
        mFrequencyBand = frequencyBand;
    }

    @Override
    public int getTimeslotCount()
    {
        if(mFrequencyBand != null)
        {
            return mFrequencyBand.getTimeslotCount();
        }

        return 1;
    }

    @Override
    public boolean isTDMAChannel()
    {
        if(mFrequencyBand != null)
        {
            return mFrequencyBand.isTDMA();
        }

        return false;
    }

    /**
     * Timeslot for a TDMA channel
     * @return timeslot or 0 if the channel is not a TDMA channel
     */
    public int getTimeslot()
    {
        if(isTDMAChannel())
        {
            return getDownlinkChannelNumber() % getTimeslotCount();
        }

        return 0;
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        if(getDownlinkBandIdentifier() == getUplinkBandIdentifier() && getDownlinkChannelNumber() == getUplinkChannelNumber())
        {
            return getDownlinkBandIdentifier() + "-" + (getDownlinkChannelNumber() / getTimeslotCount());
        }
        else if(hasUplinkChannel())
        {
            return getDownlinkBandIdentifier() + "-" + (getDownlinkChannelNumber() / getTimeslotCount()) + "/" +
                getUplinkBandIdentifier() + "-" + (getUplinkChannelNumber() / getTimeslotCount());
        }
        else
        {
            return getDownlinkBandIdentifier() + "-" + (getDownlinkChannelNumber() / getTimeslotCount()) + "/-----";
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        P25Channel that = (P25Channel)o;
        return mBandIdentifier == that.mBandIdentifier &&
            mChannelNumber == that.mChannelNumber;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mBandIdentifier, mChannelNumber);
    }
}
