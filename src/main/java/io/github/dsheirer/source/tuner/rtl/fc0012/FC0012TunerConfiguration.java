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
package io.github.dsheirer.source.tuner.rtl.fc0012;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded FC0012 tuner configuration.  The librtlsdr FC0012 driver supports manual LNA gain only.
 */
public class FC0012TunerConfiguration extends RTL2832TunerConfiguration
{
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 22_000_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 948_600_000;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 15000;
    /**
     * Default gain in tenths of a dB
     */
    public static final int DEFAULT_GAIN = 179;

    /**
     * Default constructor for Jackson
     */
    public FC0012TunerConfiguration()
    {
        super(MINIMUM_TUNABLE_FREQUENCY_HZ, MAXIMUM_TUNABLE_FREQUENCY_HZ, DEFAULT_GAIN);
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public FC0012TunerConfiguration(String uniqueID)
    {
        super(uniqueID, DEFAULT_GAIN);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.FITIPOWER_FC0012;
    }
}
