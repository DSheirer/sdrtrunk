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
package io.github.dsheirer.source.tuner.rtl.fc0013;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded FC0013 tuner configuration
 */
public class FC0013TunerConfiguration extends RTL2832TunerConfiguration
{
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 13_500_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 1_907_999_890l;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 15000;
    /**
     * Gains in tenths of a dB for the legacy LNA gain settings G00 to G23, from the librtlsdr FC0013 LNA gain table.
     * The legacy settings used the same 24 register values in the same order.  Two of them (G03 and G04) are both
     * listed as -6.3 dB, so librtlsdr reports only 23 distinct gains.
     */
    private static final int[] LEGACY_GAINS = {-99, -73, -65, -63, -63, -60, -58, -54, 58, 61, 63, 65, 67, 68, 70, 71,
            179, 181, 182, 184, 186, 188, 191, 197};
    /**
     * Default gain in tenths of a dB
     */
    public static final int DEFAULT_GAIN = 67;

    /**
     * Default constructor for Jackson
     */
    public FC0013TunerConfiguration()
    {
        super(MINIMUM_TUNABLE_FREQUENCY_HZ, MAXIMUM_TUNABLE_FREQUENCY_HZ, DEFAULT_GAIN);
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public FC0013TunerConfiguration(String uniqueID)
    {
        super(uniqueID, DEFAULT_GAIN);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.FITIPOWER_FC0013;
    }

    /**
     * Converts the legacy LNA gain setting, G00 (lowest) to G23 (highest), to the librtlsdr gain for the same
     * LNA register value.
     */
    @JsonSetter("lnaGain")
    public void setLegacyLNAGain(String lnaGain)
    {
        if(lnaGain != null && lnaGain.matches("G\\d\\d"))
        {
            setGain(LEGACY_GAINS[Math.min(Integer.parseInt(lnaGain.substring(1)), LEGACY_GAINS.length - 1)]);
        }
    }
}
