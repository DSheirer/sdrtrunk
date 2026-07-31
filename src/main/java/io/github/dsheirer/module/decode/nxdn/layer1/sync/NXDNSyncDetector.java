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

package io.github.dsheirer.module.decode.nxdn.layer1.sync;

import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Base implementation of NXDN sync detector.
 */
public abstract class NXDNSyncDetector
{
    public static final long SYNC_PATTERN = 0xCDF59;
    public static final long SYNC_MASK = 0xFFFFF;
    public static final int SYNC_DIBIT_LENGTH = 10;
    public static final Dibit[] DIBITS = toDibits(SYNC_PATTERN, SYNC_DIBIT_LENGTH);
    public static final float[] SYMBOLS = toSymbols(DIBITS);
    private static Map<Integer,ReferenceSyncWaveform> sReferenceWaveformMap = new TreeMap<>();

    /**
     * Sync pattern length in dibits.
     */
    public int getSyncPatternDibitLength()
    {
        return SYNC_DIBIT_LENGTH;
    }

    /**
     * Dibits array for the sync pattern
     * @return dibits array.
     */
    public Dibit[] getSyncDibits()
    {
        return DIBITS;
    }

    /**
     * Symbols array for the sync pattern.
     * @return array of (ideal) soft symbol values.
     */
    public float[] getSyncSymbols()
    {
        return SYMBOLS;
    }

    /**
     * Converts the sync pattern to dibits.
     * @param pattern to convert
     * @param dibitLength in dibits
     * @return array of dibits for the sync pattern.
     */
    protected static Dibit[] toDibits(long pattern, int dibitLength)
    {
        Dibit[] dibits = new Dibit[dibitLength];
        long mask = 3;
        int dibitValue;

        for(int x = 0; x < dibitLength; x++)
        {
            dibitValue = (int)((pattern & mask) >> (2 * x));

            dibits[dibitLength - x - 1] = switch(dibitValue)
            {
                case 0 -> Dibit.D00_PLUS_1;
                case 1 -> Dibit.D01_PLUS_3;
                case 2 -> Dibit.D10_MINUS_1;
                default -> Dibit.D11_MINUS_3;
            };

            mask = mask << 2;
        }

        return dibits;
    }

    /**
     * Utility method to convert a dibit array into an array of ideal soft symbol values.
     * @param dibits for the sync pattern
     * @return ideal soft symbol array.
     */
    protected static float[] toSymbols(Dibit[] dibits)
    {
        float[] symbols = new float[dibits.length];

        for(int x = 0; x < dibits.length; x++)
        {
            symbols[x] = dibits[x].getIdealPhase();
        }

        return symbols;
    }

    /**
     * Creates a reference waveform representation of the sync pattern that can be used to correlate against a
     * candidate detected sync waveform to discriminate between a valid and invalid sync detection.  The generated
     * waveform will be aligned where the final sample is time-aligned to the final symbol.
     *
     * @param samplesPerSymbol count
     * @return a reference waveform and ideal correlation score.
     */
    public ReferenceSyncWaveform getReferenceWaveform(double samplesPerSymbol)
    {
        int key = (int)(samplesPerSymbol * 1000.0);

        if(sReferenceWaveformMap.containsKey(key))
        {
            return sReferenceWaveformMap.get(key);
        }

        float[] symbols = getSyncSymbols();
        int sampleCount = (int)Math.ceil(samplesPerSymbol * (symbols.length - 1)) + 1;
        float[] samples = new float[sampleCount];
        double sampleInterval = 1.0 / samplesPerSymbol;

        double[] x = new double[symbols.length + 2];
        double[] y = new double[symbols.length + 2];

        for(int i = -1; i < x.length - 1; i++)
        {
            x[i + 1] = i;

            if(0 <= i && i < symbols.length)
            {
                y[i + 1] = symbols[i];
            }
        }

        double start = symbols.length - 1;
        int samplePointer = sampleCount - 1;
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction function = interpolator.interpolate(x, y);

        float accumulator = 0;
        float sample;
        float max = 0;
        while(samplePointer >= 0)
        {
            sample = (float)function.value(start);

            if(Math.abs(sample) > max)
            {
                max = Math.abs(sample);
            }
            samples[samplePointer--] = sample;
            accumulator += sample * sample;
            start -= sampleInterval;
        }

        ReferenceSyncWaveform referenceWaveform = new ReferenceSyncWaveform(samples, accumulator, max);
        sReferenceWaveformMap.put(key, referenceWaveform);
        return referenceWaveform;
    }
}
