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

import java.text.DecimalFormat;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight sample drop detector for tuners that don't provide sample counters or hardware timestamps.  Counts the
 * samples delivered by the tuner and compares the count against the number of samples expected from the sample rate
 * and the elapsed (host) time.  Logs a warning when the tuner consistently delivers fewer samples than expected, which
 * indicates that samples are being dropped (ie by the USB bus or because the host isn't keeping up).
 *
 * Detection: each evaluation window (5 seconds) produces a sample deficit (expected minus received).  The deficit is
 * accumulated with a cumulative sum that subtracts an allowance for the clock difference between the tuner and the
 * host, and that never goes below zero.  Buffer delivery jitter (ie a delayed buffer) adds a deficit to one window
 * that the following window gives back, while dropped samples are never given back.  So drops are only declared
 * when the accumulated deficit exceeds the threshold at two consecutive evaluations.
 *
 * The stream is assumed to be stopped (ie no buffer listeners) or restarted when buffers stop arriving for longer than
 * the restart gap, and detection restarts with the next buffer.  Detection also restarts when the sample rate changes.
 *
 * Note: process() is invoked on the tuner's sample stream thread and is not thread safe.
 */
public class SampleDropMonitor
{
    private static final Logger LOG = LoggerFactory.getLogger(SampleDropMonitor.class);
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00");
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    //Evaluation window
    static final long WINDOW_NANOS = 5 * NANOS_PER_SECOND;
    //Buffer arrival gap that indicates the stream was stopped and restarted
    static final long RESTART_GAP_NANOS = 2 * NANOS_PER_SECOND;
    //Minimum interval between warnings, so that drops are summarized instead of logged every window
    static final long LOG_INTERVAL_NANOS = 60 * NANOS_PER_SECOND;
    //Allowance for the tuner versus host clock difference and sample rate rounding (parts per million).  Drop rates
    //below this aren't detected.
    static final double CLOCK_ALLOWANCE = 500.0 / 1_000_000.0;
    //Accumulated deficit that indicates dropped samples, in milliseconds of samples
    static final double THRESHOLD_MILLISECONDS = 20.0;

    private final DoubleSupplier mSampleRateSupplier;
    private final Supplier<String> mTunerNameSupplier;
    private boolean mStarted = false;
    private long mWindowStart;
    private long mLastBuffer;
    private long mLastLog;
    private long mWindowSampleCount;
    private double mSampleRate;
    private double mAccumulatedDeficit;
    private boolean mThresholdExceeded;
    //Start of the period summarized by the next warning: the detection (re)start or the previous warning.
    private long mReportStart;
    private double mUnreportedDroppedSamples;
    private long mTotalDroppedSamples;

    /**
     * Constructs an instance
     * @param tunerNameSupplier provides the tuner name for log messages.
     * @param sampleRateSupplier provides the tuner's current sample rate.
     */
    public SampleDropMonitor(Supplier<String> tunerNameSupplier, DoubleSupplier sampleRateSupplier)
    {
        mTunerNameSupplier = tunerNameSupplier;
        mSampleRateSupplier = sampleRateSupplier;
    }

    /**
     * Total number of samples detected as dropped since this monitor was created.
     */
    public long getTotalDroppedSamples()
    {
        return mTotalDroppedSamples;
    }

    /**
     * Processes a buffer of samples received from the tuner.
     * @param sampleCount number of (complex) samples in the buffer.
     */
    public void process(int sampleCount)
    {
        process(sampleCount, System.nanoTime());
    }

    /**
     * Processes a buffer of samples received from the tuner.
     * @param sampleCount number of (complex) samples in the buffer.
     * @param now arrival time in nanoseconds (System.nanoTime()).
     */
    void process(int sampleCount, long now)
    {
        if(!mStarted || now - mLastBuffer > RESTART_GAP_NANOS)
        {
            restart(now);
            return;
        }

        mLastBuffer = now;
        //The samples of each buffer arriving within the window (after the window's starting buffer) were produced during
        //the window.
        mWindowSampleCount += sampleCount;

        long elapsed = now - mWindowStart;

        if(elapsed >= WINDOW_NANOS)
        {
            evaluate(now, elapsed);
        }
    }

    /**
     * Restarts detection.  The buffer at the restart time starts the window and isn't counted, since its samples were
     * produced before the window start.
     */
    private void restart(long now)
    {
        mStarted = true;
        mWindowStart = now;
        mLastBuffer = now;
        mLastLog = now - LOG_INTERVAL_NANOS;
        mReportStart = now;
        mUnreportedDroppedSamples = 0.0;
        mWindowSampleCount = 0;
        mSampleRate = mSampleRateSupplier.getAsDouble();
        mAccumulatedDeficit = 0.0;
        mThresholdExceeded = false;
    }

    /**
     * Evaluates the completed window and starts the next window.
     */
    private void evaluate(long now, long elapsed)
    {
        double sampleRate = mSampleRateSupplier.getAsDouble();

        if(sampleRate != mSampleRate || sampleRate <= 0)
        {
            restart(now);
            return;
        }

        double expected = sampleRate * elapsed / NANOS_PER_SECOND;
        double deficit = expected - mWindowSampleCount;
        mAccumulatedDeficit = Math.max(0.0, mAccumulatedDeficit + deficit - (expected * CLOCK_ALLOWANCE));

        if(mAccumulatedDeficit > sampleRate * THRESHOLD_MILLISECONDS / 1000.0)
        {
            //Require the deficit to persist across a second window to rule out buffer delivery jitter.
            if(mThresholdExceeded)
            {
                mUnreportedDroppedSamples += mAccumulatedDeficit;
                mTotalDroppedSamples += (long)mAccumulatedDeficit;
                mAccumulatedDeficit = 0.0;
                mThresholdExceeded = false;
            }
            else
            {
                mThresholdExceeded = true;
            }
        }
        else
        {
            mThresholdExceeded = false;
        }

        if(mUnreportedDroppedSamples > 0 && now - mLastLog >= LOG_INTERVAL_NANOS)
        {
            double period = (double)(now - mReportStart) / NANOS_PER_SECOND;
            double milliseconds = mUnreportedDroppedSamples / sampleRate * 1000.0;
            double percent = mUnreportedDroppedSamples / (sampleRate * period) * 100.0;
            LOG.warn("Tuner [" + mTunerNameSupplier.get() + "] is dropping samples - approximately [" +
                    (long)mUnreportedDroppedSamples + "] samples (" + Math.round(milliseconds) + " ms, " +
                    PERCENT_FORMAT.format(percent) + "% of the sample stream) were lost over the last " +
                    Math.round(period) + " seconds.  The USB bus or the computer isn't keeping up with the tuner's " +
                    "sample rate.  Dropped samples cause decoders to lose sync and cause choppy audio.  Try a " +
                    "different USB port or USB controller, avoid USB hubs and extension cables, don't share the USB " +
                    "controller with other tuners, or lower the tuner sample rate.");
            mLastLog = now;
            mReportStart = now;
            mUnreportedDroppedSamples = 0.0;
        }

        mWindowStart = now;
        mWindowSampleCount = 0;
    }
}
