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

package io.github.dsheirer.source.tuner;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the sample drop monitor with simulated buffer arrival times.
 */
public class SampleDropMonitorTest
{
    private static final double SAMPLE_RATE = 10_000_000.0;
    private static final int BUFFER = 65_536;
    private static final double BUFFER_NANOS = BUFFER / SAMPLE_RATE * 1e9;

    /**
     * Simulated tuner stream: buffers are produced at the sample rate (scaled by the tuner clock error) and arrive
     * with optional delivery jitter.
     */
    private static class Stream
    {
        private final SampleDropMonitor mMonitor = new SampleDropMonitor(() -> "test", () -> SAMPLE_RATE);
        private final Random mRandom = new Random(42);
        private final double mClockScale;
        private final double mJitterNanos;
        private double mProduced = 1_000_000_000.0;

        Stream(double clockPpm, double jitterNanos)
        {
            mClockScale = 1.0 + clockPpm / 1e6;
            mJitterNanos = jitterNanos;
        }

        /**
         * Produces buffers for the duration, delivering all of them.
         */
        void run(double seconds)
        {
            run(seconds, 0);
        }

        /**
         * Produces buffers for the duration, dropping one of every dropInterval buffers (0 = none).
         */
        void run(double seconds, int dropInterval)
        {
            long count = (long)(seconds * 1e9 / BUFFER_NANOS);

            for(long x = 1; x <= count; x++)
            {
                mProduced += BUFFER_NANOS / mClockScale;

                if(dropInterval > 0 && x % dropInterval == 0)
                {
                    continue;
                }

                double jitter = mJitterNanos > 0 ? mRandom.nextDouble() * mJitterNanos : 0;
                mMonitor.process(BUFFER, (long)(mProduced + jitter));
            }
        }

        void pause(double seconds)
        {
            mProduced += seconds * 1e9;
        }

        long dropped()
        {
            return mMonitor.getTotalDroppedSamples();
        }
    }

    @Test
    void cleanStreamWithClockErrorAndJitterHasNoDrops()
    {
        Stream stream = new Stream(-100, 30_000_000);
        stream.run(600);
        assertEquals(0, stream.dropped());

        stream = new Stream(100, 30_000_000);
        stream.run(600);
        assertEquals(0, stream.dropped());
    }

    @Test
    void singleDeliveryStallIsNotADrop()
    {
        Stream stream = new Stream(0, 0);
        stream.run(30);
        //A 500 ms delivery stall: the buffers produced during the stall are all delivered when it ends
        double stallEnd = stream.mProduced + 0.5e9;

        while(stream.mProduced + BUFFER_NANOS < stallEnd)
        {
            stream.mProduced += BUFFER_NANOS;
            stream.mMonitor.process(BUFFER, (long)stallEnd);
        }

        stream.run(30);
        assertEquals(0, stream.dropped());
    }

    @Test
    void droppedBuffersAreDetected()
    {
        Stream stream = new Stream(50, 10_000_000);
        stream.run(60);
        //Drop 1 of every 100 buffers (1%) for 60 seconds = ~0.6 seconds of samples
        stream.run(60, 100);
        stream.run(30);
        double expected = 60.0 * SAMPLE_RATE / 100.0;
        double dropped = stream.dropped();
        assertTrue(Math.abs(dropped - expected) < expected * 0.1, "Dropped " + dropped + " expected " + expected);
    }

    @Test
    void streamRestartIsNotADrop()
    {
        Stream stream = new Stream(0, 0);
        stream.run(20);
        stream.pause(10);
        stream.run(20);
        assertEquals(0, stream.dropped());
    }
}
