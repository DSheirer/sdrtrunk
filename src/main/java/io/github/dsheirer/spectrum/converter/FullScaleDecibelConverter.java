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

package io.github.dsheirer.spectrum.converter;

/**
 * Converts complex DFT results to decibels relative to full scale (dBFS).  A complex tone with an amplitude of 1.0
 * (the full scale of the normalized samples) centered in a DFT bin converts to 0 dBFS.
 *
 * Use with a ComplexDftProcessor whose window is scaled to unity coherent gain (setUnityGainWindow(true)).  With
 * unity coherent gain the DFT magnitude of a full scale tone equals the DFT size N, so each bin's power is divided
 * by N squared.
 *
 * Noise spreads across the bins, so the noise floor of each bin is lower than the total noise power by the DFT
 * processing gain: 10 * log10(N / window noise bandwidth in bins).  Doubling the DFT size lowers the noise floor by
 * about 3 dB while tones stay at the same level.
 */
public class FullScaleDecibelConverter extends DFTResultsConverter
{
    /**
     * Constructs an instance.
     */
    public FullScaleDecibelConverter()
    {
    }

    /**
     * Converts interleaved complex DFT results to dBFS, with the lowest frequency first.
     */
    public static float[] convert(float[] results)
    {
        double size = results.length / 2.0;
        return ComplexDecibelConverter.convert(results, (float)(1.0 / (size * size)));
    }

    @Override
    public void receive(float[] results)
    {
        dispatch(convert(results));
    }
}
