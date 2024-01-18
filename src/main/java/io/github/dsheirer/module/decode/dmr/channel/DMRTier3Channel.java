/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * DMR Tier III Trunking channel. This channel uses a logical channel number and a timeslot.
 */
public class DMRTier3Channel extends DMRChannel
{
    /**
     * Constructs an instance.  Note: radio reference uses a one based index, so we add a value of one to the
     * calculated logical slot value for visual compatibility for users.
     *
     * @param channel number or repeater number
     * @param timeslot either 1 or 2.
     */
    public DMRTier3Channel(int channel, int timeslot)
    {
        super(channel, timeslot);
    }

    /**
     * Tier III Channel ID
     * @return channel ID
     */
    public int getChannelId()
    {
        return getChannelNumber() * 2 + getTimeslot();
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" LCN:").append(getChannelNumber());
        sb.append(" CHANID:").append(getChannelId());
        return sb.toString();
    }

    @Override
    public DMRChannel getSisterTimeslot()
    {
        DMRTier3Channel other = new DMRTier3Channel(getChannelNumber(), getTimeslot() == 1 ? 2 : 1);
        other.setTimeslotFrequency(getTimeslotFrequency());
        return other;
    }
}
