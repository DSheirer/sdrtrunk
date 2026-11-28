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
package io.github.dsheirer.dsp.filter.channelizer.ols;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.ArrayList;
import java.util.List;

/**
 * Signal generation and measurement helpers for the channelizer quality tests.
 */
public class SignalTools
{
    public static final int CHUNK = 65536;

    /**
     * Feeds a sum of complex tones through a path in chunks of interleaved samples.
     * @param path to drive
     * @param sampleRate of the input
     * @param frequencies of the tones relative to the tuner center
     * @param amplitudes of the tones
     * @param length in samples
     */
    public static void drive(ChannelizerPaths.Path path, double sampleRate, double[] frequencies, double[] amplitudes,
                             int length)
    {
        drive(path, sampleRate, frequencies, amplitudes, 0, length);
    }

    /**
     * Feeds a sum of complex tones, with phase referenced to absolute input sample zero, starting at absolute sample
     * offset.
     */
    public static void drive(ChannelizerPaths.Path path, double sampleRate, double[] frequencies, double[] amplitudes,
                             long offset, int length)
    {
        int tones = frequencies.length;
        double[] stepI = new double[tones];
        double[] stepQ = new double[tones];

        for(int t = 0; t < tones; t++)
        {
            stepI[t] = Math.cos(2.0 * Math.PI * frequencies[t] / sampleRate);
            stepQ[t] = Math.sin(2.0 * Math.PI * frequencies[t] / sampleRate);
        }

        for(int start = 0; start < length; start += CHUNK)
        {
            int count = Math.min(CHUNK, length - start);
            double[] acc = new double[count * 2];

            for(int t = 0; t < tones; t++)
            {
                //Reseed each chunk from the exact phase so the recurrence error does not accumulate
                double phase = 2.0 * Math.PI * ((frequencies[t] / sampleRate * (offset + start)) % 1.0);
                double pi = amplitudes[t] * Math.cos(phase);
                double pq = amplitudes[t] * Math.sin(phase);
                double si = stepI[t];
                double sq = stepQ[t];

                for(int x = 0; x < count; x++)
                {
                    acc[2 * x] += pi;
                    acc[2 * x + 1] += pq;
                    double next = pi * si - pq * sq;
                    pq = pi * sq + pq * si;
                    pi = next;
                }
            }

            float[] samples = new float[count * 2];

            for(int x = 0; x < samples.length; x++)
            {
                samples[x] = (float)acc[x];
            }

            path.receive(samples, offset + start);
        }
    }

    /**
     * Collects a channel's output stream and the length of each buffer it arrived in.
     */
    public static class Collector implements Listener<ComplexSamples>
    {
        private final List<ComplexSamples> mBuffers = new ArrayList<>();

        @Override
        public void receive(ComplexSamples samples)
        {
            mBuffers.add(new ComplexSamples(samples.i().clone(), samples.q().clone(), samples.timestamp()));
        }

        public void clear()
        {
            mBuffers.clear();
            mCache = null;
        }

        public int length()
        {
            int length = 0;

            for(ComplexSamples buffer : mBuffers)
            {
                length += buffer.i().length;
            }

            return length;
        }

        public List<ComplexSamples> getBuffers()
        {
            return mBuffers;
        }

        /**
         * Samples from absolute output index start (inclusive) to end (exclusive).
         */
        public double[][] get(int start, int end)
        {
            double[][] all = get();
            end = Math.min(end, all[0].length);
            int length = Math.max(0, end - start);
            double[] i = new double[length];
            double[] q = new double[length];
            System.arraycopy(all[0], start, i, 0, length);
            System.arraycopy(all[1], start, q, 0, length);
            return new double[][]{i, q};
        }

        /**
         * Position of each output sample within the buffer it arrived in, and the buffer length.
         */
        public int[][] positions()
        {
            int length = length();
            int[] position = new int[length];
            int[] size = new int[length];
            int pointer = 0;

            for(ComplexSamples buffer : mBuffers)
            {
                for(int x = 0; x < buffer.i().length; x++)
                {
                    position[pointer] = x;
                    size[pointer++] = buffer.i().length;
                }
            }

            return new int[][]{position, size};
        }

        public double[][] get()
        {
            if(mCache != null && mCacheLength == length())
            {
                return mCache;
            }

            int length = length();
            double[] i = new double[length];
            double[] q = new double[length];
            int pointer = 0;

            for(ComplexSamples buffer : mBuffers)
            {
                for(int x = 0; x < buffer.i().length; x++)
                {
                    i[pointer] = buffer.i()[x];
                    q[pointer++] = buffer.q()[x];
                }
            }

            mCache = new double[][]{i, q};
            mCacheLength = length;
            return mCache;
        }

        private double[][] mCache;
        private int mCacheLength;
    }

    /**
     * Least squares fit of complex tone amplitudes at known frequencies to samples from index start.
     * @return for each tone {re, im}, then a final row {residual power, mean signal power}
     */
    public static double[][] fit(double[][] samples, long start, double sampleRate, double[] frequencies)
    {
        int tones = frequencies.length;
        int length = samples[0].length;
        //Normal equations A^H A c = A^H y with complex basis e^{j2pi f n}
        double[][] gramI = new double[tones][tones];
        double[][] gramQ = new double[tones][tones];
        double[] rhsI = new double[tones];
        double[] rhsQ = new double[tones];

        for(int a = 0; a < tones; a++)
        {
            for(int b = 0; b < tones; b++)
            {
                double df = 2.0 * Math.PI * (frequencies[b] - frequencies[a]) / sampleRate;
                for(int n = 0; n < length; n++)
                {
                    double angle = df * (start + n);
                    gramI[a][b] += Math.cos(angle);
                    gramQ[a][b] += Math.sin(angle);
                }
            }

            double w = -2.0 * Math.PI * frequencies[a] / sampleRate;

            for(int n = 0; n < length; n++)
            {
                double angle = w * (start + n);
                double ci = Math.cos(angle);
                double cq = Math.sin(angle);
                double yi = samples[0][n];
                double yq = samples[1][n];
                rhsI[a] += yi * ci - yq * cq;
                rhsQ[a] += yi * cq + yq * ci;
            }
        }

        double[][] solution = solveComplex(gramI, gramQ, rhsI, rhsQ);
        double residual = 0.0;
        double power = 0.0;

        for(int n = 0; n < length; n++)
        {
            double mi = 0.0;
            double mq = 0.0;

            for(int t = 0; t < tones; t++)
            {
                double angle = 2.0 * Math.PI * frequencies[t] / sampleRate * (start + n);
                double ci = Math.cos(angle);
                double cq = Math.sin(angle);
                mi += solution[t][0] * ci - solution[t][1] * cq;
                mq += solution[t][0] * cq + solution[t][1] * ci;
            }

            double ei = samples[0][n] - mi;
            double eq = samples[1][n] - mq;
            residual += ei * ei + eq * eq;
            power += mi * mi + mq * mq;
        }

        double[][] result = new double[tones + 1][];
        System.arraycopy(solution, 0, result, 0, tones);
        result[tones] = new double[]{residual / length, power / length};
        return result;
    }

    /**
     * Per-sample squared error of a single tone fit, for block-position analysis.
     */
    public static double[] errors(double[][] samples, long start, double sampleRate, double frequency, double[] amplitude)
    {
        int length = samples[0].length;
        double[] errors = new double[length];

        for(int n = 0; n < length; n++)
        {
            double angle = 2.0 * Math.PI * frequency / sampleRate * (start + n);
            double mi = amplitude[0] * Math.cos(angle) - amplitude[1] * Math.sin(angle);
            double mq = amplitude[0] * Math.sin(angle) + amplitude[1] * Math.cos(angle);
            double ei = samples[0][n] - mi;
            double eq = samples[1][n] - mq;
            errors[n] = ei * ei + eq * eq;
        }

        return errors;
    }

    /**
     * Mean power from index start.
     */
    public static double power(double[][] samples)
    {
        double power = 0.0;
        int length = samples[0].length;

        for(int n = 0; n < length; n++)
        {
            power += samples[0][n] * samples[0][n] + samples[1][n] * samples[1][n];
        }

        return power / Math.max(1, length);
    }

    /**
     * Gaussian elimination with partial pivoting on a small complex system.
     */
    private static double[][] solveComplex(double[][] ai, double[][] aq, double[] bi, double[] bq)
    {
        int n = bi.length;
        double[][] mi = new double[n][n + 1];
        double[][] mq = new double[n][n + 1];

        for(int r = 0; r < n; r++)
        {
            for(int c = 0; c < n; c++)
            {
                mi[r][c] = ai[r][c];
                mq[r][c] = aq[r][c];
            }

            mi[r][n] = bi[r];
            mq[r][n] = bq[r];
        }

        for(int p = 0; p < n; p++)
        {
            int best = p;

            for(int r = p + 1; r < n; r++)
            {
                if(Math.hypot(mi[r][p], mq[r][p]) > Math.hypot(mi[best][p], mq[best][p]))
                {
                    best = r;
                }
            }

            double[] t = mi[p];
            mi[p] = mi[best];
            mi[best] = t;
            t = mq[p];
            mq[p] = mq[best];
            mq[best] = t;

            for(int r = p + 1; r < n; r++)
            {
                double denominator = mi[p][p] * mi[p][p] + mq[p][p] * mq[p][p];
                double fi = (mi[r][p] * mi[p][p] + mq[r][p] * mq[p][p]) / denominator;
                double fq = (mq[r][p] * mi[p][p] - mi[r][p] * mq[p][p]) / denominator;

                for(int c = p; c <= n; c++)
                {
                    double vi = mi[p][c] * fi - mq[p][c] * fq;
                    double vq = mi[p][c] * fq + mq[p][c] * fi;
                    mi[r][c] -= vi;
                    mq[r][c] -= vq;
                }
            }
        }

        double[][] x = new double[n][2];

        for(int r = n - 1; r >= 0; r--)
        {
            double si = mi[r][n];
            double sq = mq[r][n];

            for(int c = r + 1; c < n; c++)
            {
                si -= mi[r][c] * x[c][0] - mq[r][c] * x[c][1];
                sq -= mi[r][c] * x[c][1] + mq[r][c] * x[c][0];
            }

            double denominator = mi[r][r] * mi[r][r] + mq[r][r] * mq[r][r];
            x[r][0] = (si * mi[r][r] + sq * mq[r][r]) / denominator;
            x[r][1] = (sq * mi[r][r] - si * mq[r][r]) / denominator;
        }

        return x;
    }
}
