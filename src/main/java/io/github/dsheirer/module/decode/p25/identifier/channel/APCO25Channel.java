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
package io.github.dsheirer.module.decode.p25.identifier.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO-25 Channel composed of a frequency band and a channel number within the frequency band.
 *
 * This class supports inserting a band identifier message that allows the uplink and downlink freuqencies
 * for the channel to be calculated.
 */
public class APCO25Channel extends Identifier<P25Channel> implements IChannelDescriptor, Comparable<IChannelDescriptor>
{
    /**
     * Creates an APCO-25 channel identifier
     */
    public APCO25Channel(P25Channel p25Channel)
    {
        super(p25Channel, IdentifierClass.NETWORK, Form.CHANNEL, Role.BROADCAST);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return getValue().getFrequencyBandIdentifiers();
    }

    @Override
    public int getTimeslotCount()
    {
        return getValue().getTimeslotCount();
    }

    @Override
    public boolean isTDMAChannel()
    {
        return getValue().isTDMAChannel();
    }

    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        getValue().setFrequencyBand(bandIdentifier);
    }

    /**
     * Downlink frequency for this channel.
     *
     * @return frequency in hertz
     */
    public long getDownlinkFrequency()
    {
        return getValue().getDownlinkFrequency();
    }

    /**
     * Uplink frequency for this channel.
     *
     * @return frequency in hertz
     */
    public long getUplinkFrequency()
    {
        return getValue().getUplinkFrequency();
    }

    /**
     * Timeslot for this channel
     * @return timeslot
     */
    public int getTimeslot()
    {
        return getValue().getTimeslot();
    }


    /**
     * Creates a new APCO-25 identifier
     */
    public static APCO25Channel create(int frequencyBand, int channelNumber)
    {
        return new APCO25Channel(new P25Channel(frequencyBand, channelNumber));
    }

    /**
     * Creates a new APCO-25 identifier with the same frequencyBand, and a different channelNumber representing the
     * requested timeslot.
     * @param requestedTimeslot to decorate as.
     * @return decorated channel.
     */
    public APCO25Channel decorateAs(int requestedTimeslot)
    {
        if(getTimeslot() == requestedTimeslot)
        {
            return this;
        }

        P25Channel existing = getValue();

        int channelNumber = existing.getChannelNumber();
        if(requestedTimeslot == 1)
        {
            channelNumber--;
        }
        else
        {
            channelNumber++;
        }

        P25Channel decoratedChannel = new P25Channel(existing.getBandIdentifier(), channelNumber);

        if(existing.getFrequencyBand() != null)
        {
            decoratedChannel.setFrequencyBand(existing.getFrequencyBand());
        }

        return new APCO25Channel(decoratedChannel);
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || !(o instanceof APCO25Channel) || ((APCO25Channel)o).getValue() == null)
        {
            return false;
        }

        return getValue().equals(((APCO25Channel)o).getValue());
    }

    @Override
    public int hashCode()
    {
        return getValue().hashCode();
    }

    @Override
    public int compareTo(IChannelDescriptor o)
    {
        if(o == null)
        {
            return -1;
        }

        return Long.compare(getDownlinkFrequency(), o.getDownlinkFrequency());
    }
}
