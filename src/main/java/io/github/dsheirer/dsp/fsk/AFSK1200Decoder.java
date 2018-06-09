/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.fsk;

import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.buffer.BooleanAveragingBuffer;
import io.github.dsheirer.buffer.FloatAveragingBuffer;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.mixer.IOscillator;
import io.github.dsheirer.dsp.mixer.Oscillator;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binary Frequency Shift Keying (2FSK) correlation decoder.  Provides normal or inverted decoded output.
 */
public class AFSK1200Decoder implements Listener<ReusableBuffer>, ISyncDetectListener, ISyncStateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AFSK1200Decoder.class);

    public enum Output
    {
        NORMAL, INVERTED
    }

    public static final double TARGET_SAMPLE_RATE = 7200.0d;
    public static final int SAMPLES_PER_SYMBOL = 6; //TARGET_SAMPLE_RATE / 1200;
    public static final float COARSE_TIMING_GAIN = 1.0f / 3.0f;
    protected static final float MEDIUM_TIMING_GAIN = 1.0f / 4.0f;
    protected static final float FINE_TIMING_GAIN = 1.0f / 5.0f;

    private static float[] sBandPassFilterCoefficients;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.bandPassBuilder()
            .sampleRate(8000)
            .stopFrequency1(1000)
            .passFrequencyBegin(1100)
            .passFrequencyEnd(1900)
            .stopFrequency2(2000)
            .stopRipple(0.000001)
            .passRipple(0.00001)
            .build();

        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sBandPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    private Correlator mCorrelator1200 = new Correlator(TARGET_SAMPLE_RATE, 1200.0, SAMPLES_PER_SYMBOL);
    private Correlator mCorrelator1800 = new Correlator(TARGET_SAMPLE_RATE, 1800.0, SAMPLES_PER_SYMBOL);
    private float[] mCorrelationValues1200;
    private float[] mCorrelationValues1800;

    protected boolean mNormalOutput;
    protected float mSymbolTimingGain = COARSE_TIMING_GAIN;
    protected SampleBuffer mSampleBuffer;
    protected ZeroCrossingErrorDetector mTimingErrorDetector = new ZeroCrossingErrorDetector(SAMPLES_PER_SYMBOL);
    protected SynchronizationMonitor mSynchronizationMonitor;
    private RealFIRFilter2 mBandPassFilter = new RealFIRFilter2(sBandPassFilterCoefficients);
    protected MessageFramer mMessageFramer;
    private boolean mSampleDecision;

    //Resample to an integral of the baud rate 1200 baud * 6 samples per symbol = 7200.0 Hertz
    private RealResampler mResampler = new RealResampler(8000.0, TARGET_SAMPLE_RATE, 2000, 1);

    public AFSK1200Decoder(int messageLength, SampleBuffer sampleBuffer, ZeroCrossingErrorDetector detector, boolean invertOutput)
    {
        mSynchronizationMonitor = new SynchronizationMonitor(messageLength);
        mSynchronizationMonitor.setListener(this);
        mTimingErrorDetector = detector;
        mSampleBuffer = sampleBuffer;
        mSampleBuffer.setTimingGain(mSymbolTimingGain);
        mResampler.setListener(new Decoder(SAMPLES_PER_SYMBOL));
        mNormalOutput = !invertOutput;
    }

    public AFSK1200Decoder(int messageLength, boolean invertOutput)
    {
        this(messageLength, new SampleBuffer(SAMPLES_PER_SYMBOL, COARSE_TIMING_GAIN),
            new ZeroCrossingErrorDetector(SAMPLES_PER_SYMBOL), invertOutput);
    }

    /**
     * Disposes of all references to prepare for garbage collection
     */
    public void dispose()
    {
    }

    /**
     * Implements the ISyncDetectedListener interface to be notified of message sync detection events.
     *
     * This allows the internal timing error detector to adjust symbol timing error gain levels according to the
     * message synchronization state to quickly adjust to initial signal streams or to reduce gain levels once
     * synchronization has been achieved.
     */
    @Override
    public void syncDetected()
    {
        mSynchronizationMonitor.syncDetected();
    }

    /**
     * Processes the buffer samples by converting all samples to boolean values reflecting if the sample value is
     * greater than zero (or not).  Average symbol timing offset is calculated for the full buffer and the offset is
     * adjusted and then each of the symbols are decoded using a simple majority decision.
     *
     * @param buffer containing 8.0 kHz unfiltered FM demodulated audio samples with sub-audible LTR signalling.
     */
    @Override
    public void receive(ReusableBuffer buffer)
    {
//        ReusableBuffer bandPassFiltered = mBandPassFilter.filter(buffer);
//        mResampler.resample(bandPassFiltered);
        mResampler.resample(buffer);
    }

    protected void dispatch(boolean symbol)
    {
        if(mMessageFramer != null)
        {
            mMessageFramer.receive(mNormalOutput ? symbol : !symbol);
        }
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

    public class Decoder implements Listener<ReusableBuffer>
    {
        private BooleanAveragingBuffer mSampleAveragingBuffer;

        private Decoder(int samplesPerSymbol)
        {
            mSampleAveragingBuffer = new BooleanAveragingBuffer(samplesPerSymbol);
        }

        @Override
        public void receive(ReusableBuffer buffer)
        {
            //Calculate correlation values against each 1200/1800 reference signal
            mCorrelationValues1200 = mCorrelator1200.process(buffer);
            mCorrelationValues1800 = mCorrelator1800.process(buffer);

            buffer.decrementUserCount();

            for(int x = 0; x < mCorrelationValues1200.length; x++)
            {
                //1200 = Mark (1) and 1800 = Space (0)
                mSampleDecision = mCorrelationValues1200[x] > mCorrelationValues1800[x];
//                mSampleDecision = mSampleAveragingBuffer.getAverage(mSampleDecision);
                mSampleBuffer.receive(mSampleDecision);
                mTimingErrorDetector.receive(mSampleDecision);

                if(mSampleBuffer.hasSymbol())
                {
                    dispatch(mSampleBuffer.getSymbol());

//                    mSampleBuffer.resetAndAdjust(-mTimingErrorDetector.getError());
                    mSampleBuffer.resetAndAdjust(0.0f);

                    mSynchronizationMonitor.increment();
                }
            }
        }
    }

    /**
     * Generates a correlation value for each FM demodulated signal against an established reference frequency,
     * correlated and averaged over separate periods relative to the samples per symbol period.
     */
    public class Correlator
    {
        private FloatAveragingBuffer mAveragingBuffer;
        private float[] mReferenceSamples;
        private float[] mDemodulatedSamples;
        private float[] mCorrelationValues;
        private float mCorrelationAccumulator;

        /**
         * Constructs a correlator instance for the specified parameters
         * @param sampleRate of the incoming sample stream.  Note: this should be an integral of the symbol rate.
         * @param frequency of the mark or space symbol to test for correlation
         * @param samplesPerSymbol to determine the correlation and averaging periods
         */
        public Correlator(double sampleRate, double frequency, int samplesPerSymbol)
        {
            //Adjust correlation period and averaging period to provide optimal correlation for a given samples/symbol.
            //These values are optimized for 6 samples per symbol with averaging over 7 samples and correlation over an
            //8 sample period.
            int correlationPeriod = samplesPerSymbol + 2;

            mAveragingBuffer = new FloatAveragingBuffer(samplesPerSymbol + 1);

            mDemodulatedSamples = new float[correlationPeriod];

            IOscillator referenceGenerator = new Oscillator(frequency, sampleRate);
            mReferenceSamples = referenceGenerator.generateReal(correlationPeriod);
        }

        /**
         * Processes each sample in the incoming sample buffer against a generated reference sample set for one symbol
         * period to derive a correlation value that is in-turn averaged over one symbol period.
         *
         * @param reusableBuffer containing FM demodulated samples
         * @return a reusable array of correlation values for each sample
         */
        public float[] process(ReusableBuffer reusableBuffer)
        {
            if(mCorrelationValues == null || mCorrelationValues.length != reusableBuffer.getSampleCount())
            {
                mCorrelationValues = new float[reusableBuffer.getSampleCount()];
            }

            float[] samples = reusableBuffer.getSamples();

            int y;

            for(int x = 0; x < samples.length; x++)
            {
                //Note: we're using array copy and dot product structures that the JRE can promote to SIMD intrinsics
                //when the host processor provides SIMD capabilities
                System.arraycopy(mDemodulatedSamples, 1, mDemodulatedSamples, 0, mDemodulatedSamples.length - 1);
                mDemodulatedSamples[mDemodulatedSamples.length - 1] = samples[x];

                mCorrelationAccumulator = 0.0f;

                for(y = 0; y < mDemodulatedSamples.length; y++)
                {
                    mCorrelationAccumulator += mDemodulatedSamples[y] * mReferenceSamples[y];
                }

                //Add the absolute value of correlation accumulator value to the averaging buffer and store the current
                // average as the correlation value for this sample
                mCorrelationValues[x] = mAveragingBuffer.get(Math.abs(mCorrelationAccumulator));
            }

            return mCorrelationValues;
        }
    }
}
