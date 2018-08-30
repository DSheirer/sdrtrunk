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
 * APCO-25 Channel composed of separate uplink and downlink channel numbers and frequency bands.
 *
 * This class supports inserting band identifier message(s) that allows the uplink and downlink freuqencies
 * for the channel to be calculated.
 */
public class APCO25ExplicitChannel extends AbstractAPCO25Channel
{
    private int mDownlinkFrequencyBandIdentifier;
    private IFrequencyBand mDownlinkFrequencyBand;

    private int mUplinkFrequencyBandIdentifier;
    private int mUplinkChannelNumber;
    private IFrequencyBand mUplinkFrequencyBand;

    /**
     * Creates an explicit APCO-25 channel where the uplink and downlink frequency bands and channel numbers
     * can be different.
     */
    public APCO25ExplicitChannel(int downlinkFrequencyBand, int downlinkChannel, int uplinkFrequencyBand,
                                 int uplinkChannelNumber)
    {
        super(downlinkChannel);
        mDownlinkFrequencyBandIdentifier = downlinkFrequencyBand;
        mUplinkFrequencyBandIdentifier = uplinkFrequencyBand;
        mUplinkChannelNumber = uplinkChannelNumber;
    }

    @Override
    public int getTimestlotCount()
    {
        return mDownlinkFrequencyBand.getTimeslotCount();
    }

    /**
     * Frequency band identifiers for this channel
     */
    public int[] getFrequencyBandIdentifiers()
    {
        int[] identifiers = new int[2];
        identifiers[0] = mDownlinkFrequencyBandIdentifier;
        identifiers[1] = mUplinkFrequencyBandIdentifier;
        return identifiers;
    }

    public void setFrequencyBand(IFrequencyBand frequencyBand)
    {
        if(frequencyBand != null)
        {
            if(frequencyBand.getIdentifier() == mDownlinkFrequencyBandIdentifier)
            {
                mDownlinkFrequencyBand = frequencyBand;
            }
            else if(frequencyBand.getIdentifier() == mUplinkFrequencyBandIdentifier)
            {
                mUplinkFrequencyBand = frequencyBand;
            }
            else
            {
                throw new IllegalArgumentException("Frequency band  message with id [" + frequencyBand.getIdentifier() +
                    "] message is not correct for this channel - downlink band [" + mDownlinkFrequencyBandIdentifier +
                    "] uplink band [" + mUplinkFrequencyBandIdentifier + "]");
            }
        }
    }

    /**
     * Indicates if this channel has a downlink frequency
     */
    public boolean hasDownlink()
    {
        return mDownlinkFrequencyBand != null && isValidChannelNumber();
    }

    /**
     * Downlink channel number
     */
    public int getDownlinkChannelNumber()
    {
        return getValue();
    }

    /**
     * Downlink frequency for this channel.
     *
     * @return frequency in hertz
     */
    public long getDownlinkFrequency()
    {
        if(mDownlinkFrequencyBand != null)
        {
            return mDownlinkFrequencyBand.getDownlinkFrequency(getDownlinkChannelNumber());
        }

        return 0;
    }

    public int getUplinkChannelNumber()
    {
        return mUplinkChannelNumber;
    }

    /**
     * Indicates if this channel has an uplink frequency
     */
    public boolean hasUplink()
    {
        return mUplinkFrequencyBand != null && isValidChannelNumber();
    }

    /**
     * Uplink frequency for this channel.
     *
     * @return frequency in hertz
     */
    public long getUplinkFrequency()
    {
        if(mUplinkFrequencyBand != null)
        {
            return mUplinkFrequencyBand.getUplinkFrequency(getUplinkChannelNumber());
        }

        return 0;
    }

    /**
     * Creates a new APCO-25 explicit channel
     */
    public static APCO25ExplicitChannel create(int downlinkFrequencyBand, int downlinkChannelNumber,
                                               int uplinkFrequencyBand, int uplinkChannelNumber)
    {
        return new APCO25ExplicitChannel(downlinkFrequencyBand, downlinkChannelNumber, uplinkFrequencyBand,
            uplinkChannelNumber);
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DN:").append(mDownlinkFrequencyBandIdentifier).append("-").append(getDownlinkChannelNumber());
        sb.append(" UP:").append(mUplinkFrequencyBandIdentifier).append("-").append(getUplinkChannelNumber());
        return sb.toString();
    }
}
