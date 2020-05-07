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

package io.github.dsheirer.module.decode.dmr.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

import java.util.Objects;

public class DMRChannel implements IChannelDescriptor
{
    private IFrequencyBand mFrequencyBand;
    private int mBandIdentifier;
    private int mChannelNumber;

    public DMRChannel(int bandIdentifier, int channelNumber)
    {
        mBandIdentifier = bandIdentifier;
        mChannelNumber = channelNumber;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
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

        return mBandIdentifier;
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
        if(frequencyBand.getIdentifier() == getDownlinkBandIdentifier())
        {
            mFrequencyBand = frequencyBand;
        }
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
        return true;
    }

    /**
     * Timeslot for a TDMA channel
     * @return timeslot or 0 if the channel is not a TDMA channel
     */
    public int getTimeslot()
    {
        return 1;
    }

    /**
     * Logical channel number.  For Phase 1 channels this is the downlink channel number.  For Phase 2 channels, this
     * is the channel number without the timeslot specifier.
     */
    public int getDownlinkLogicalChannelNumber()
    {
        return getDownlinkChannelNumber() / getTimeslotCount();
    }

    /**
     * Logical channel number.  For Phase 1 channels this is the uplink channel number.  For Phase 2 channels, this
     * is the channel number without the timeslot specifier.
     */
    public int getUplinkLogicalChannelNumber()
    {
        return getUplinkChannelNumber() / getTimeslotCount();
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getDownlinkBandIdentifier() == getUplinkBandIdentifier() && getDownlinkChannelNumber() == getUplinkChannelNumber())
        {
            sb.append(getDownlinkBandIdentifier()).append("-").append(getDownlinkLogicalChannelNumber());
        }
        else if(hasUplinkChannel())
        {
            sb.append(getDownlinkBandIdentifier())
                .append("-")
                .append(getDownlinkLogicalChannelNumber())
                .append("/")
                .append(getUplinkBandIdentifier())
                .append("-")
                .append(getUplinkLogicalChannelNumber());
        }
        else
        {
            sb.append(getDownlinkBandIdentifier())
                .append("-")
                .append(getDownlinkLogicalChannelNumber())
                .append("/-----");
        }

        if(isTDMAChannel())
        {
            sb.append(" TS").append(getTimeslot());
        }

        return sb.toString();
    }

    /**
     * Designates channel equality as having the same band identifier and channel number.
     *
     * Note: Phase 2 channels also specify timeslot for a channel, but timeslot is not considered for equality.
     * @param o other object
     * @return true if both instances are P25 channels in the same band and channel number
     */
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null)
        {
            return false;
        }
        if(!(o instanceof DMRChannel))
        {
            return false;
        }
        DMRChannel that = (DMRChannel)o;
        return mBandIdentifier == that.mBandIdentifier && getDownlinkLogicalChannelNumber() == that.getDownlinkLogicalChannelNumber();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mBandIdentifier, getDownlinkLogicalChannelNumber());
    }
}
