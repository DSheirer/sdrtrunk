/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.rtl.fc2580;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded FC2580 tuner configuration.  The librtlsdr FC2580 driver doesn't support gain control and the
 * tuner always uses its internal AGC.  The tuner covers 146-308 MHz and 438-924 MHz.
 */
public class FC2580TunerConfiguration extends RTL2832TunerConfiguration
{
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 146_000_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 924_000_000;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 15000;
    /**
     * Default gain in tenths of a dB.  Not used, since the tuner doesn't support gain control.
     */
    public static final int DEFAULT_GAIN = 0;

    /**
     * Default constructor for Jackson
     */
    public FC2580TunerConfiguration()
    {
        super(MINIMUM_TUNABLE_FREQUENCY_HZ, MAXIMUM_TUNABLE_FREQUENCY_HZ, DEFAULT_GAIN);
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public FC2580TunerConfiguration(String uniqueID)
    {
        super(uniqueID, DEFAULT_GAIN);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.FCI_FC2580;
    }
}
