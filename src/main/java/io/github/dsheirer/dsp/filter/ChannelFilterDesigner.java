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
package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.window.WindowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Designs low-pass filters for complex sample channels, with a check of the designed frequency response.
 */
public class ChannelFilterDesigner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelFilterDesigner.class);
    private static final double MAXIMUM_STOP_BAND_DB = -40.0;
    private static final double MAXIMUM_PASS_BAND_DB = 1.0;
    private static final int FILTER_RESPONSE_CHECK_POINTS = 1024;
    private static final int FALLBACK_FILTER_SIZING_ATTENUATION_DB = 75;
    private static final int FALLBACK_FILTER_MAXIMUM_ATTENUATION_DB = 150;
    private static final int FALLBACK_FILTER_ATTENUATION_STEP_DB = 15;

    /**
     * Designs the low-pass channel filter for complex samples, where the channel bandwidth spans the two-sided
     * (positive and negative frequency) signal.  The stop band ripple provides approximately 46 dB of attenuation.
     *
     * The remez designer can converge on a filter that meets the specification at the design grid points but has a large
     * response spike outside of them, so the response of the designed filter is checked before it's used.
     *
     * @param sampleRate of the samples to filter in hertz
     * @param bandwidth of the channel in hertz
     * @return filter coefficients
     */
    public static float[] design(double sampleRate, double bandwidth)
    {
        int passBandStop = (int)(bandwidth * .4);
        int stopBandStart = (int)(bandwidth * .5);

        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(sampleRate)
                .gridDensity(16)
                .oddLength(true)
                .passBandCutoff(passBandStop)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandStart(stopBandStart)
                .stopBandAmplitude(0.0)
                .stopBandRipple(0.005)
                .build();

        float[] coefficients = null;

        try
        {
            coefficients = FilterFactory.getTaps(specification);
        }
        catch(FilterDesignException fde)
        {
            LOGGER.warn("Couldn't design remez channel filter for sample rate [" + sampleRate +
                    "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart + "]");
        }

        if(coefficients == null || !isValidFilterResponse(coefficients, sampleRate, passBandStop, stopBandStart))
        {
            LOGGER.info("Unable to use remez filter designer for sample rate [" + sampleRate + "] pass band stop [" +
                    passBandStop + "] and stop band start [" + stopBandStart +
                    "] - will proceed using simple low pass filter design");
            //Center the cutoff so the bandwidth matches the remez design and size the filter to meet the response limits.
            for(int attenuation = FALLBACK_FILTER_SIZING_ATTENUATION_DB; attenuation <= FALLBACK_FILTER_MAXIMUM_ATTENUATION_DB;
                attenuation += FALLBACK_FILTER_ATTENUATION_STEP_DB)
            {
                int length = FilterFactory.getLowPass(sampleRate, passBandStop, stopBandStart, attenuation,
                        WindowType.HAMMING, true).length;
                coefficients = FilterFactory.getLowPass(sampleRate, (passBandStop + stopBandStart) / 2, length, WindowType.HAMMING);

                if(isValidFilterResponse(coefficients, sampleRate, passBandStop, stopBandStart))
                {
                    break;
                }
            }
        }

        return coefficients;
    }

    /**
     * Checks the frequency response of the low-pass filter, relative to the response at DC.
     * @param coefficients of the filter
     * @param sampleRate of the filter in hertz
     * @param passBandStop frequency in hertz
     * @param stopBandStart frequency in hertz
     * @return true if the pass band and stop band responses are within limits, through to half the sample rate.
     */
    public static boolean isValidFilterResponse(float[] coefficients, double sampleRate, int passBandStop, int stopBandStart)
    {
        double dc = getResponseDb(coefficients, 0.0, sampleRate);
        double nyquist = sampleRate / 2.0;

        for(int x = 0; x <= FILTER_RESPONSE_CHECK_POINTS; x++)
        {
            double frequency = nyquist * x / FILTER_RESPONSE_CHECK_POINTS;
            double response = getResponseDb(coefficients, frequency, sampleRate) - dc;

            if((frequency <= passBandStop && response > MAXIMUM_PASS_BAND_DB) ||
               (frequency >= stopBandStart && response > MAXIMUM_STOP_BAND_DB))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the magnitude response of the filter at the specified frequency.
     */
    public static double getResponseDb(float[] coefficients, double frequency, double sampleRate)
    {
        double radians = 2.0 * Math.PI * frequency / sampleRate;
        double real = 0.0;
        double imaginary = 0.0;

        for(int x = 0; x < coefficients.length; x++)
        {
            real += coefficients[x] * Math.cos(radians * x);
            imaginary += coefficients[x] * Math.sin(radians * x);
        }

        return 20.0 * Math.log10(Math.hypot(real, imaginary));
    }
}
