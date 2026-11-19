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

package io.github.dsheirer.module.decode.smartnet;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-pass filter for Motorola Type II voice channel audio.
 *
 * Type II voice channels carry sub-audible low-speed data (150 baud, strongest near 75 Hz, with energy to about
 * 225 Hz) under the voice.  On MSP Monson (2026-11-10) it was 30 dB above the audio in the pauses between words; the
 * standard audio high-pass (-32 dB from 200 Hz) leaves it at about the level of the pauses, where it is heard as a
 * tone or buzz.  This filter stops 250 Hz and below by about 55 dB and passes from 350 Hz.
 */
public class LowSpeedDataFilter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LowSpeedDataFilter.class);
    private static float[] sCoefficients;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
            .sampleRate(8000)
            .stopBandCutoff(250)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.0015)
            .passBandStart(350)
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .build();

        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sCoefficients = designer.getImpulseResponse();
            }
            else
            {
                LOGGER.warn("Type II low-speed data filter design is not valid");
            }
        }
        catch(FilterDesignException fde)
        {
            LOGGER.error("Type II low-speed data filter design error", fde);
        }
    }

    private LowSpeedDataFilter()
    {
    }

    /**
     * @return a new filter instance (filters keep state - one per channel), or null when the design failed
     */
    public static IRealFilter create()
    {
        return sCoefficients != null ? FilterFactory.getRealFilter(sCoefficients) : null;
    }

    /**
     * Filter coefficients, for testing
     */
    static float[] getCoefficients()
    {
        return sCoefficients;
    }
}
