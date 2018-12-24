/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.identifier.channel;

/**
 * APCO-25 Channel composed of separate uplink and downlink channel numbers and frequency bands.
 *
 * This class supports inserting band identifier message(s) that allows the uplink and downlink freuqencies
 * for the channel to be calculated.
 */
public class APCO25ExplicitChannel extends APCO25Channel
{
    public APCO25ExplicitChannel(P25ExplicitChannel p25ExplicitChannel)
    {
        super(p25ExplicitChannel);
    }

    /**
     * Creates a new APCO-25 explicit channel
     */
    public static APCO25ExplicitChannel create(int downlinkFrequencyBand, int downlinkChannelNumber,
                                               int uplinkFrequencyBand, int uplinkChannelNumber)
    {
        return new APCO25ExplicitChannel(new P25ExplicitChannel(downlinkFrequencyBand, downlinkChannelNumber,
            uplinkFrequencyBand, uplinkChannelNumber));
    }
}
