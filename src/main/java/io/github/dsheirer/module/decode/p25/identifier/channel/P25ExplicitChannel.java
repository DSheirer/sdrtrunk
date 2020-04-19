/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;

public class P25ExplicitChannel extends P25Channel implements Comparable<P25Channel>
{
    private IFrequencyBand mUplinkFrequencyBand;
    private int mUplinkBandIdentifier;
    private int mUplinkChannelNumber;

    public P25ExplicitChannel(int downlinkBandIdentifier, int downlinkChannelNumber,
                              int uplinkBandIdentifier, int uplinkChannelNumber)
    {
        super(downlinkBandIdentifier, downlinkChannelNumber);
        mUplinkBandIdentifier = uplinkBandIdentifier;
        mUplinkChannelNumber = uplinkChannelNumber;
    }

    @Override
    public int getUplinkBandIdentifier()
    {
        return mUplinkBandIdentifier;
    }

    @Override
    public int getUplinkChannelNumber()
    {
        return mUplinkChannelNumber;
    }

    @Override
    public long getUplinkFrequency()
    {
        if(mUplinkFrequencyBand != null)
        {
            //Note: we explicitly use the downlink frequency from the uplink channel frequency band here
            return mUplinkFrequencyBand.getDownlinkFrequency(getUplinkChannelNumber());
        }

        return 0;
    }

    @Override
    public void setFrequencyBand(IFrequencyBand frequencyBand)
    {
        if(frequencyBand.getIdentifier() == getUplinkBandIdentifier())
        {
            mUplinkFrequencyBand = frequencyBand;
        }

        super.setFrequencyBand(frequencyBand);
    }

    @Override
    public int compareTo(P25Channel other)
    {
        //Ignores the uplink channel ... for ordering we'll simply order by the downlink channel
        if(getDownlinkBandIdentifier() == other.getDownlinkBandIdentifier())
        {
            return Integer.compare(getDownlinkChannelNumber(), other.getDownlinkChannelNumber());
        }
        else
        {
            return Integer.compare(getDownlinkBandIdentifier(), other.getDownlinkBandIdentifier());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof P25ExplicitChannel)) return false;
        return compareTo((P25ExplicitChannel) o) == 0;
    }
}
