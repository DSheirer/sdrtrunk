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
 * Motorola Type II control channel decoder.  Demodulates the 3600 baud FSK outbound signalling
 * from the channel's I/Q samples and emits each CRC-checked outbound signalling word (OSW) as a message.
 */
public class SmartNetDecoder extends Decoder implements IComplexSamplesListener, Listener<ComplexSamples>,
    ISourceEventListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartNetDecoder.class);
    private final SmartNetOSWFramer mFramer = new SmartNetOSWFramer(this::dispatch);
    private final Listener<SourceEvent> mSourceEventListener = this::process;
    private SmartNetDemodulator mDemodulator;
    private long mTimestamp;

    public SmartNetDecoder()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.SMARTNET;
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
        if(mDemodulator == null || Math.abs(mDemodulator.getSamplesPerSymbol() * SmartNetDemodulator.SYMBOL_RATE -
            sampleRate) > 1.0)
        {
            mDemodulator = new SmartNetDemodulator(sampleRate, bit -> mFramer.process(bit, mTimestamp));
            LOGGER.debug("SmartNet decoder sample rate [" + sampleRate + "]");
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
        SmartNetDemodulator demodulator = mDemodulator;

        if(demodulator != null)
        {
            mTimestamp = samples.timestamp();
            demodulator.process(samples.i(), samples.q(), samples.i().length);
            logStatistics();
        }
    }

    /**
     * Decode statistics for the log.  The control channel sends one OSW every 84 bits whether or not it is found, so
     * the bits processed give the number sent; counting only the OSWs that were found and failed their check
     * understates the loss (MSP Central MA at Summit 800, 2026-11-14: 4% failed the check, 15% were lost in all).
     * @param valid OSWs decoded
     * @param invalid OSWs found by their sync words that failed the error check
     * @param bits processed
     */
    static String statistics(long valid, long invalid, long bits)
    {
        long sent = Math.round(bits / (double)SmartNetOSWFramer.FRAME_LENGTH);

        if(valid == 0 || sent <= 0)
        {
            return "0 OSWs decoded - no signal";
        }

        sent = Math.max(sent, valid + invalid);
        long lost = sent - valid;
        return String.format("%d of about %d OSWs decoded - %.1f%% lost (%d failed the check, %d not found)", valid, sent,
            100.0 * lost / sent, invalid, lost - invalid);
    }

    private long mStatisticsTimestamp;
    private long mStatisticsValid;
    private long mStatisticsInvalid;
    private long mStatisticsBits;

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
            LOGGER.debug("Motorola Type II control channel last minute: " + statistics(valid, invalid,
                getBitCount() - mStatisticsBits));
        }

        mStatisticsTimestamp = mTimestamp;
        mStatisticsValid = getValidCount();
        mStatisticsInvalid = getInvalidCount();
        mStatisticsBits = getBitCount();
    }

    private void dispatch(SmartNetOSW osw)
    {
        getMessageListener().receive(osw);
    }

    public long getValidCount()
    {
        return mFramer.getValidCount();
    }

    public long getBitCount()
    {
        return mFramer.getBitCount();
    }

    public long getInvalidCount()
    {
        return mFramer.getInvalidCount();
    }
}
