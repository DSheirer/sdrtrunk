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

package io.github.dsheirer.module.decode.p25.phase1;

/**
 * Designs the P25 Phase 1 C4FM receive filter applied to the FM discriminator output.
 *
 * TIA-102.BAAA specifies a raised cosine (alpha 0.2) Nyquist filter cascaded with an inverse sinc (x / sin(x)) shaping
 * filter at the transmitter, and an integrate and dump filter (sinc response) at the receiver, so that the overall
 * response is the raised cosine with no intersymbol interference at the symbol sampling instants.
 *
 * The transmitted signal is band limited to (1 + alpha) * symbolRate / 2 (2880 Hz), but an integrate and dump filter
 * has slowly decaying sidelobes that pass the FM discriminator noise above the signal band, where the discriminator
 * noise power spectral density rises with the square of frequency.  This designer produces a linear phase FIR filter
 * with the integrate and dump (sinc) response across the signal band and a stopband above it, which preserves the zero
 * ISI property while rejecting the out of band noise.
 */
public class P25P1FilterDesigner
{
    /** P25 C4FM raised cosine roll-off factor. */
    public static final double P25_ALPHA = 0.2;

    /** Least-squares weight applied in the stopband, relative to a weight of 1.0 in the passband. */
    private static final double STOPBAND_WEIGHT = 1.0;

    /** Frequency grid points per filter coefficient used for the least-squares fit. */
    private static final int GRID_DENSITY = 16;

    /**
     * Designs the band limited integrate and dump receive filter.
     *
     * @param sampleRate in Hertz
     * @param symbolRate in symbols per second
     * @param stopBandStart frequency in Hertz where the stopband starts.  The passband extends to the signal band
     * edge of (1 + alpha) * symbolRate / 2 and the region between the passband edge and the stopband start is a
     * transition band that is excluded from the fit.
     * @param symbolSpan filter length in symbol periods
     * @return filter taps, odd length, linear phase, unity gain at DC
     */
    public static float[] designReceiveFilter(double sampleRate, double symbolRate, double stopBandStart, int symbolSpan)
    {
        if(sampleRate <= 0 || symbolRate <= 0)
        {
            throw new IllegalArgumentException("Sample rate and symbol rate must be positive");
        }

        if(symbolSpan < 1)
        {
            throw new IllegalArgumentException("Symbol span must be at least 1");
        }

        double nyquist = sampleRate / 2.0;
        double passBandEdge = (1.0 + P25_ALPHA) * symbolRate / 2.0;

        if(stopBandStart <= passBandEdge || stopBandStart >= nyquist)
        {
            throw new IllegalArgumentException("Stop band start [" + stopBandStart + "] must be between the pass band " +
                    "edge [" + passBandEdge + "] and Nyquist [" + nyquist + "]");
        }

        //Odd tap count so the filter has an integer group delay of (length - 1) / 2 samples
        int length = (int)Math.ceil(symbolSpan * sampleRate / symbolRate);

        if(length % 2 == 0)
        {
            length++;
        }

        int middle = (length - 1) / 2;
        int coefficientCount = middle + 1;
        double symbolPeriod = 1.0 / symbolRate;

        //A symmetric filter has the zero-phase response A(f) = c[0] + sum(m=1..middle) c[m] * 2cos(2*pi*f*m/fs)
        //where c[m] = h[middle - m] = h[middle + m]. Accumulate the weighted normal equations (B'WB)c = B'Wd over a
        //dense frequency grid from DC to Nyquist, skipping the transition band.
        double[][] normal = new double[coefficientCount][coefficientCount];
        double[] rhs = new double[coefficientCount];
        double[] basis = new double[coefficientCount];
        int gridSize = GRID_DENSITY * coefficientCount;

        for(int g = 0; g <= gridSize; g++)
        {
            double frequency = nyquist * g / gridSize;
            double desired;
            double weight;

            if(frequency <= passBandEdge)
            {
                desired = sinc(frequency * symbolPeriod);
                weight = 1.0;
            }
            else if(frequency >= stopBandStart)
            {
                desired = 0.0;
                weight = STOPBAND_WEIGHT;
            }
            else
            {
                continue; //Transition band
            }

            basis[0] = 1.0;

            for(int m = 1; m < coefficientCount; m++)
            {
                basis[m] = 2.0 * Math.cos(2.0 * Math.PI * frequency * m / sampleRate);
            }

            for(int i = 0; i < coefficientCount; i++)
            {
                double weighted = weight * basis[i];
                rhs[i] += weighted * desired;

                for(int j = i; j < coefficientCount; j++)
                {
                    normal[i][j] += weighted * basis[j];
                }
            }
        }

        //Fill in the lower triangle of the symmetric normal matrix
        for(int i = 0; i < coefficientCount; i++)
        {
            for(int j = 0; j < i; j++)
            {
                normal[i][j] = normal[j][i];
            }
        }

        double[] coefficients = solve(normal, rhs);

        //Expand to the full symmetric impulse response and normalize to unity gain at DC
        double[] taps = new double[length];
        double sum = 0.0;

        for(int m = 0; m < coefficientCount; m++)
        {
            taps[middle - m] = coefficients[m];
            taps[middle + m] = coefficients[m];
        }

        for(double tap : taps)
        {
            sum += tap;
        }

        float[] output = new float[length];

        for(int n = 0; n < length; n++)
        {
            output[n] = (float)(taps[n] / sum);
        }

        return output;
    }

    /**
     * Sinc: sin(x) / x where x = pi * f * T, the frequency response of an integrate and dump filter of length T.
     *
     * @param normalizedFrequency f * T (frequency divided by the symbol rate)
     */
    private static double sinc(double normalizedFrequency)
    {
        if(normalizedFrequency == 0.0)
        {
            return 1.0;
        }

        double x = Math.PI * normalizedFrequency;
        return Math.sin(x) / x;
    }

    /**
     * Solves Ax = b using Gaussian elimination with partial pivoting. A and b are modified.
     */
    private static double[] solve(double[][] a, double[] b)
    {
        int n = b.length;

        for(int column = 0; column < n; column++)
        {
            int pivot = column;

            for(int row = column + 1; row < n; row++)
            {
                if(Math.abs(a[row][column]) > Math.abs(a[pivot][column]))
                {
                    pivot = row;
                }
            }

            double[] swapRow = a[column];
            a[column] = a[pivot];
            a[pivot] = swapRow;
            double swapValue = b[column];
            b[column] = b[pivot];
            b[pivot] = swapValue;

            for(int row = column + 1; row < n; row++)
            {
                double factor = a[row][column] / a[column][column];
                b[row] -= factor * b[column];

                for(int k = column; k < n; k++)
                {
                    a[row][k] -= factor * a[column][k];
                }
            }
        }

        double[] x = new double[n];

        for(int row = n - 1; row >= 0; row--)
        {
            double sum = b[row];

            for(int k = row + 1; k < n; k++)
            {
                sum -= a[row][k] * x[k];
            }

            x[row] = sum / a[row][row];
        }

        return x;
    }
}
