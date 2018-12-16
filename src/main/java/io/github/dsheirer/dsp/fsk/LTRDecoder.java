/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.dsp.fsk;

import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.dsp.filter.dc.IIRSinglePoleDCRemovalFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logic Trunked Radio (LTR) 300-baud FSK decoder is designed to operate against an 8 kHz FM demodulated audio stream.
 *
 * This decoder employs a zero-crossing timing error detector with adjustable symbol timing error gain.  This decoder
 * also employs a synchronization monitor that automatically adjusts symbol timing error gain according to the quantity
 * of successful message sync pattern detections.  This is optimized for LTR signals that periodically transmit idle
 * bursts and transmit continuous message streams during voice transmissions.  The synchronization monitor adjusts to an
 * aggressive gain value when sync is lost and automatically reduces timing error gain as successive sync pattern
 * detections occur.
 *
 * FM demodulated sample buffers should not be filtered prior to this decoder.  An internal DC-removal filter coupled
 * with a low-pass filter removes any tuning offset and/or audio components above 300 Hertz.  The DC removal filter is
 * adjusted for a slower offset removal in order to prevent overly distorting the FSK signalling component.  This mild
 * adjustment may not fully compensate for mis-tuned signals and may result in missed decodes of idle bursts.  However,
 * the DC filter will track a mis-tuned signal to baseband quickly upon a continuous transmission.
 */
public class LTRDecoder implements Listener<ReusableFloatBuffer>, ISyncDetectListener, ISyncStateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRDecoder.class);

    public static final float SAMPLES_PER_SYMBOL = 8000.0f / 300.0f;

    protected static final float COARSE_TIMING_GAIN = 1.0f / 3.0f;
    protected static final float MEDIUM_TIMING_GAIN = 1.0f / 4.0f;
    protected static final float FINE_TIMING_GAIN = 1.0f / 5.0f;

    private static float[] sLowPassFilterCoefficients;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
            .sampleRate(8000)
            .gridDensity(16)
            .oddLength(true)
            .passBandCutoff(300)
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .stopBandStart(500)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.03) //Approximately 60 dB attenuation
            .build();

        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sLowPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    protected float mSymbolTimingGain = COARSE_TIMING_GAIN;
    protected SampleBuffer mSampleBuffer;
    protected ZeroCrossingErrorDetector mTimingErrorDetector = new ZeroCrossingErrorDetector(SAMPLES_PER_SYMBOL);
    protected SynchronizationMonitor mSynchronizationMonitor;
    private IIRSinglePoleDCRemovalFilter mDCFilter = new IIRSinglePoleDCRemovalFilter(0.99999f);
    private RealFIRFilter2 mLowPassFilter = new RealFIRFilter2(sLowPassFilterCoefficients);
    private MessageFramer mMessageFramer;

    private boolean mSampleDecision;

    /**
     * Implements a Logic Trunked Radio sub-audible 300 baud FSK signaling decoder
     *
     * @param messageLength in symbols
     */
    public LTRDecoder(int messageLength)
    {
        this(messageLength, new SampleBuffer(SAMPLES_PER_SYMBOL, COARSE_TIMING_GAIN),
            new ZeroCrossingErrorDetector(SAMPLES_PER_SYMBOL));
    }

    /**
     * Implements a Logic Trunked Radio sub-audible 300 baud FSK signaling decoder.
     *
     * @param messageLength in symbols
     * @param sampleBuffer to use for storing sample decisions
     * @param detector for symbol timing errors
     */
    public LTRDecoder(int messageLength, SampleBuffer sampleBuffer, ZeroCrossingErrorDetector detector)
    {
        mSynchronizationMonitor = new SynchronizationMonitor(messageLength);
        mSynchronizationMonitor.setListener(this);
        mTimingErrorDetector = detector;
        mSampleBuffer = sampleBuffer;
        mSampleBuffer.setTimingGain(mSymbolTimingGain);
    }

    /**
     * Implements the ISyncDetectedListener interface to be notified of message sync detection events.
     *
     * This allows the internal timing error detector to adjust symbol timing error gain levels according to the
     * message synchronization state to quickly adjust to initial signal streams or to reduce gain levels once
     * synchronization has been achieved.
     */
    @Override
    public void syncDetected(int bitErrors)
    {
        mSynchronizationMonitor.syncDetected(bitErrors);
    }

    @Override
    public void syncLost()
    {
        //no-op
    }

    /**
     * Processes the buffer samples by converting all samples to boolean values reflecting if the sample value is
     * greater than zero (or not).  Average symbol timing offset is calculated for the full buffer and the offset is
     * adjusted and then each of the symbols are decoded using a simple majority decision.
     *
     * @param buffer containing 8.0 kHz unfiltered FM demodulated audio samples with sub-audible LTR signalling.
     */
    @Override
    public void receive(ReusableFloatBuffer buffer)
    {
        ReusableFloatBuffer dcFiltered = mDCFilter.filter(buffer);
        ReusableFloatBuffer lowPassFiltered = mLowPassFilter.filter(dcFiltered);

        for(float sample : lowPassFiltered.getSamples())
        {
            mSampleDecision = sample > 0.0;

            mSampleBuffer.receive(mSampleDecision);
            mTimingErrorDetector.receive(mSampleDecision);

            if(mSampleBuffer.hasSymbol())
            {
                if(mMessageFramer != null)
                {
                    mMessageFramer.process(mSampleBuffer.getSymbol());
                }

                mSampleBuffer.resetAndAdjust(-mTimingErrorDetector.getError());

                mSynchronizationMonitor.increment();
            }
        }

        lowPassFiltered.decrementUserCount();
    }

    /**
     * Registers a listener to receive decoded LTR symbols.
     *
     * @param messageFramer to receive symbols.
     */
    public void setMessageFramer(MessageFramer messageFramer)
    {
        mMessageFramer = messageFramer;
    }

    /**
     * Removes the symbol listener from receiving decoded LTR symbols.
     */
    public void removeListener()
    {
        mMessageFramer = null;
    }


    /**
     * Implements the ISyncStateListener interface to receive synchronization state events and adjust the symbol timing
     * error gain levels on the internal timing error detector.
     *
     * Gain levels are defined relative to a unit gain of 1.0 over a number of symbol periods.  LTR uses an initial
     * ramp-up symbol reversal pattern of 0101 which provides three zero crossing detection opportunities for the timing
     * error detector, therefore we react to a COARSE gain state to average timing error over 3 symbols.
     *
     * @param syncState from an external synchronization state monitor
     */
    @Override
    public void setSyncState(SyncState syncState)
    {
        switch(syncState)
        {
            case FINE:
                mSymbolTimingGain = FINE_TIMING_GAIN;
                break;
            case MEDIUM:
                mSymbolTimingGain = MEDIUM_TIMING_GAIN;
                break;
            case COARSE:
                mSymbolTimingGain = COARSE_TIMING_GAIN;
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sync state level: " + syncState.name());
        }

        mSampleBuffer.setTimingGain(mSymbolTimingGain);
    }
}
