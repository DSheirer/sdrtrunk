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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EDACS control channel decoder.  Demodulates the 9600 baud FSK outbound control channel from the
 * channel's I/Q samples and emits each frame whose first message passes the BCH check.
 */
public class EDACSDecoder extends Decoder implements IComplexSamplesListener, Listener<ComplexSamples>,
    ISourceEventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EDACSDecoder.class);
    private final EDACSMessageFramer mFramer = new EDACSMessageFramer(this::dispatch);
    private final Listener<SourceEvent> mSourceEventListener = this::process;
    private EDACSDemodulator mDemodulator;
    private double mSampleRate;
    private long mTimestamp;

    /**
     * Constructs an instance for a system without an EDACS System Key
     */
    public EDACSDecoder()
    {
        this(0);
    }

    /**
     * Constructs an instance
     * @param systemKey EDACS System Key (ESK), 0 for none
     */
    public EDACSDecoder(int systemKey)
    {
        mFramer.setSystemKey(systemKey);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS;
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    /**
     * Sets the channel sample rate.  Public for offline testing.
     */
    public void setSampleRate(double sampleRate)
    {
        if(mDemodulator == null || Math.abs(mSampleRate - sampleRate) > 1.0)
        {
            mSampleRate = sampleRate;
            mDemodulator = new EDACSDemodulator(sampleRate, bit -> mFramer.process(bit, mTimestamp));
            LOGGER.debug("EDACS decoder sample rate [" + sampleRate + "]");
        }
    }

    private void process(SourceEvent sourceEvent)
    {
        if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
        {
            setSampleRate(sourceEvent.getValue().doubleValue());
        }
    }

    @Override
    public void receive(ComplexSamples samples)
    {
        EDACSDemodulator demodulator = mDemodulator;

        if(demodulator != null)
        {
            mTimestamp = samples.timestamp();
            demodulator.process(samples.i(), samples.q(), samples.i().length);
            logStatistics();
        }
    }

    private long mStatisticsTimestamp;
    private long mStatisticsValid;
    private long mStatisticsInvalid;

    /**
     * Diagnostics (DEBUG): decode statistics once a minute
     */
    private void logStatistics()
    {
        if(mTimestamp - mStatisticsTimestamp < 60_000)
        {
            return;
        }

        if(mStatisticsTimestamp > 0 && LOGGER.isDebugEnabled())
        {
            long valid = getValidCount() - mStatisticsValid;
            long invalid = getInvalidCount() - mStatisticsInvalid;
            LOGGER.debug("EDACS control channel last minute: " + valid + " messages decoded, " + invalid + " failed the check" +
                (valid + invalid > 0 ? String.format(" (%.1f%%)", 100.0 * invalid / (valid + invalid)) : " - no signal"));
        }

        mStatisticsTimestamp = mTimestamp;
        mStatisticsValid = getValidCount();
        mStatisticsInvalid = getInvalidCount();
    }

    private void dispatch(EDACSMessage message)
    {
        getMessageListener().receive(message);
    }

    public long getValidCount()
    {
        return mFramer.getValidCount();
    }

    public long getInvalidCount()
    {
        return mFramer.getInvalidCount();
    }
}
