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

package io.github.dsheirer.module.decode.dmr.channel;

/**
 * DMR Channel with Absolute frequency values
 */
public class DMRAbsoluteChannel extends DMRChannel
{
    private long mDownlinkFrequency;
    private long mUplinkFrequency;

    /**
     * Constructs an instance
     * @param lcn logical channel number
     * @param timeslot for the channel
     * @param downlinkFrequency frequency in hz
     * @param uplinkFrequency frequency in hz
     */
    public DMRAbsoluteChannel(int lcn, int timeslot, long downlinkFrequency, long uplinkFrequency)
    {
        super(lcn, timeslot);
        mDownlinkFrequency = downlinkFrequency;
        mUplinkFrequency = uplinkFrequency;
    }

    /**
     * Downlink frequency
     * @return value in Hertz
     */
    @Override
    public long getDownlinkFrequency()
    {
        return mDownlinkFrequency;
    }

    /**
     * Uplink frequency
     * @return value in Hertz
     */
    @Override
    public long getUplinkFrequency()
    {
        return mUplinkFrequency;
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getValue());
        sb.append(" ").append(getDownlinkFrequency() / 1E6d);

        return sb.toString();
    }

    @Override
    public DMRChannel getSisterTimeslot()
    {
        DMRAbsoluteChannel other = new DMRAbsoluteChannel(getChannelNumber(), getTimeslot() == 1 ? 2 : 1,
                getDownlinkFrequency(), getUplinkFrequency());
        return other;
    }
}
