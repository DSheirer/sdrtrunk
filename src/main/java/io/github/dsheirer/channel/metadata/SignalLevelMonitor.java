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
package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import java.util.function.DoubleConsumer;

/**
 * Measures the average power of a channel's baseband samples once a second, in dB relative to full scale.
 */
public class SignalLevelMonitor extends Module implements IComplexSamplesListener
{
    private static final long INTERVAL_MS = 1000;

    private final DoubleConsumer mListener;
    private double mPowerSum;
    private long mSampleCount;
    private long mIntervalStart;

    /**
     * @param listener to receive each one-second level in dBFS
     */
    public SignalLevelMonitor(DoubleConsumer listener)
    {
        mListener = listener;
    }

    /**
     * Level in dBFS for the average power, or negative infinity for no power.
     */
    static double toDb(double averagePower)
    {
        return averagePower > 0 ? 10.0 * Math.log10(averagePower) : Double.NEGATIVE_INFINITY;
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this::process;
    }

    private void process(ComplexSamples samples)
    {
        float[] i = samples.i();
        float[] q = samples.q();

        for(int x = 0; x < i.length; x++)
        {
            mPowerSum += i[x] * i[x] + q[x] * q[x];
        }

        mSampleCount += i.length;
        long now = System.currentTimeMillis();

        if(mIntervalStart == 0)
        {
            mIntervalStart = now;
        }
        else if(now - mIntervalStart >= INTERVAL_MS && mSampleCount > 0)
        {
            mListener.accept(toDb(mPowerSum / mSampleCount));
            mPowerSum = 0;
            mSampleCount = 0;
            mIntervalStart = now;
        }
    }

    @Override
    public void reset()
    {
        mPowerSum = 0;
        mSampleCount = 0;
        mIntervalStart = 0;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }
}
