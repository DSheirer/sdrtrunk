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

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventProvider;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.message.IMessage;
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
 * EDACS trunking control channel decoder using edacs-fm-style sync frame detection.
 * Demodulates complex I/Q samples to FM audio, digitizes via AFC threshold,
 * detects sync frame pattern, extracts and BCH-checks messages.
 *
 * <p>Tested against an EDACS Extended Addressing (EA) wideband (9600 baud) control channel only.  See
 * {@link DecodeConfigEDACS} for the tested scope.</p>
 */
public class EDACSDecoder extends Decoder implements IComplexSamplesListener, Listener<ComplexSamples>,
        ISourceEventListener, IDecoderStateEventProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(EDACSDecoder.class);

    private IDemodulator mFMDemodulator;
    private EDACSSyncDetector mSyncDetector;
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private Listener<DecoderStateEvent> mDecoderStateEventListener;
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();

    private float[] mResampleBuffer = new float[0];
    private double mResampleCursor = 0;
    private float mLastResampleInput = 0;
    private boolean mHasLastResampleInput = false;

    public EDACSDecoder()
    {
        this(false);
    }

    /**
     * Constructs an instance
     * @param esk true if the control channel uses EDACS Security Key (ESK)
     */
    public EDACSDecoder(boolean esk)
    {
        mFMDemodulator = FmDemodulatorFactory.getFmDemodulator();
        mSyncDetector = new EDACSSyncDetector(esk);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS;
    }

    @Override
    public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateEventListener = listener;
    }

    @Override
    public void removeDecoderStateListener()
    {
        mDecoderStateEventListener = null;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    @Override
    public void receive(ComplexSamples samples)
    {
        if(mIDecimationFilter == null)
        {
            return;
        }

        float[] decimatedI = mIDecimationFilter.decimateReal(samples.i());
        float[] decimatedQ = mQDecimationFilter.decimateReal(samples.q());

        float[] filteredI = mIBasebandFilter.filter(decimatedI);
        float[] filteredQ = mQBasebandFilter.filter(decimatedQ);

        float[] demodulated = mFMDemodulator.demodulate(filteredI, filteredQ);

        // Resample from the decimated channelizer rate to 48 kHz so EDACS
        // 9600 baud symbols have exactly 5 samples/symbol, matching DSD-FME's
        // rtl_fm path and center-sample symbol timing.
        float[] resampled = resample(demodulated, mDecimatedRate, 48000.0);

        mSyncDetector.process(resampled, getMessageListener());
    }

    private double mDecimatedRate = 48000.0;

    private void setSampleRate(double sampleRate)
    {
        int decimation = 1;
        while((sampleRate / decimation) >= 96000)
        {
            decimation *= 2;
        }

        mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);

        mDecimatedRate = sampleRate / decimation;

        // 98% of the EDACS signal power is within 6 kHz of the carrier.  The previous 9.6 kHz pass / 12 kHz stop
        // (after DSD-FME rtl_fm BW:24) let in noise that cost control channel messages; see the same filter in
        // EDACSProVoiceDecoder for the measurements and the tuner frequency error this tolerates.
        float[] coefficients = FilterFactory.getLowPass(mDecimatedRate, 6000, 8000, 60,
                io.github.dsheirer.dsp.window.WindowType.HAMMING, true);

        mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
        mQBasebandFilter = FilterFactory.getRealFilter(coefficients);

        mLog.debug("EDACS decoder sample rate: " + mDecimatedRate + " (decimation: " + decimation + ") -> 48 kHz");

        mSyncDetector.reset();
    }

    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
                mResampleCursor = 0;
                mHasLastResampleInput = false;
                setSampleRate(sourceEvent.getValue().doubleValue());
            }
        }
    }

    private float[] resample(float[] input, double inputRate, double outputRate)
    {
        if(input.length == 0)
        {
            return input;
        }

        if(!mHasLastResampleInput)
        {
            mLastResampleInput = input[0];
            mHasLastResampleInput = true;
            mResampleCursor = 1.0;
        }

        double step = inputRate / outputRate;
        int estimateLen = (int)(input.length / step) + 2;
        if(mResampleBuffer.length < estimateLen)
        {
            mResampleBuffer = new float[estimateLen];
        }

        int outIdx = 0;
        while(mResampleCursor < input.length)
        {
            int index = (int)mResampleCursor;
            double fraction = mResampleCursor - index;
            float previous = index == 0 ? mLastResampleInput : input[index - 1];
            float current = input[index];
            mResampleBuffer[outIdx++] = (float)(previous + (current - previous) * fraction);
            mResampleCursor += step;
        }

        mResampleCursor -= input.length;
        mLastResampleInput = input[input.length - 1];

        float[] result = new float[outIdx];
        System.arraycopy(mResampleBuffer, 0, result, 0, outIdx);
        return result;
    }
}
