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
package io.github.dsheirer.identifier.integer.channel;

import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;

/**
 * APCO-25 Channel composed of a frequency band and a channel number within the frequency band.
 *
 * This class supports inserting a band identifier message that allows the uplink and downlink freuqencies
 * for the channel to be calculated.
 */
public class APCO25Channel extends AbstractAPCO25Channel
{
    private int mFrequencyBandIdentifier;
    private IFrequencyBand mFrequencyBand;

    /**
     * Creates an APCO-25 channel and identifying frequency band
     *
     * @param frequencyBandId
     * @param channel
     */
    public APCO25Channel(int frequencyBandId, int channel)
    {
        super(channel);

        mFrequencyBandIdentifier = frequencyBandId;
    }

    @Override
    public int getTimestlotCount()
    {
        return mFrequencyBand.getTimeslotCount();
    }

    /**
     * Frequency band identifier for this channel
     */
    public int[] getFrequencyBandIdentifiers()
    {
        int[] identifiers = new int[1];
        identifiers[0] = mFrequencyBandIdentifier;
        return identifiers;
    }

    public void setFrequencyBand(IFrequencyBand frequencyBand)
    {
        if(frequencyBand != null)
        {
            if(frequencyBand.getIdentifier() != mFrequencyBandIdentifier)
            {
                throw new IllegalArgumentException("Frequency band message is not correct for this channel [" +
                    getValue() + "] and band [" + mFrequencyBandIdentifier + "]");
            }

            mFrequencyBand = frequencyBand;
        }
    }

    /**
     * Indicates if this channel has a downlink frequency
     */
    public boolean hasDownlink()
    {
        return mFrequencyBand != null && isValidChannelNumber();
    }

    /**
     * Downlink frequency for this channel.
     *
     * @return frequency in hertz
     */
    public long getDownlinkFrequency()
    {
        if(mFrequencyBand != null)
        {
            return mFrequencyBand.getDownlinkFrequency(getChannelNumber());
        }

        return 0;
    }

    public int getChannelNumber()
    {
        return getValue();
    }

    /**
     * Indicates if this channel has an uplink frequency
     */
    public boolean hasUplink()
    {
        return mFrequencyBand != null && isValidChannelNumber();
    }

    /**
     * Uplink frequency for this channel.
     *
     * @return frequency in hertz
     */
    public long getUplinkFrequency()
    {
        if(mFrequencyBand != null)
        {
            return mFrequencyBand.getUplinkFrequency(getChannelNumber());
        }

        return 0;
    }

    /**
     * Creates a new APCO-25 identifier
     */
    public static APCO25Channel create(int frequencyBand, int channelNumber)
    {
        return new APCO25Channel(frequencyBand, channelNumber);
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        return mFrequencyBandIdentifier + "-" + getChannelNumber();
    }
}
