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
 * APCO-25 Channel interface.
 */
public interface IAPCO25Channel
{
    /**
     * Downlink frequency for the channel
     * @return frequency in hertz
     */
    long getDownlinkFrequency();

    /**
     * Uplink frequency for the channel
     * @return frequency in hertz
     */
    long getUplinkFrequency();

    /**
     * Frequency band identifiers (0 - 15)
     */
    int[] getFrequencyBandIdentifiers();

    /**
     * Assigns the frequency band message to the channel to use in calculating the up/downlink frequencies
     * @param frequencyBand
     */
    void setFrequencyBand(IFrequencyBand frequencyBand);

    /**
     * Number of timeslots.  FDMA has 1 timeslot and TDMA has at least 2 timeslots
     */
    int getTimestlotCount();


    /**
     * Indicates if the timeslot count is greater than 1;
     */
    boolean isTDMAChannel();
}
