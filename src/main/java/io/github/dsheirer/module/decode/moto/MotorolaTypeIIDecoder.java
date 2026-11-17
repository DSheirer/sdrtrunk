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

package io.github.dsheirer.module.decode.moto;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventProvider;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.moto.osw.OswExtractor;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Motorola Type II trunking control channel decoder.
 * Demodulates complex I/Q samples to FM audio, resamples to 18 kHz (5 samples/symbol
 * at 3,600 baud), and extracts OSW messages.
 */
public class MotorolaTypeIIDecoder extends Decoder implements IComplexSamplesListener, Listener<ComplexSamples>,
        ISourceEventListener, IDecoderStateEventProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(MotorolaTypeIIDecoder.class);

    private static final double TARGET_SAMPLE_RATE = 18000.0;
    private static final int BASEBAND_PASS = 12500;
    private static final int BASEBAND_STOP = 14000;

    private IDemodulator mFMDemodulator;
    private OswExtractor mOswExtractor;
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private Listener<DecoderStateEvent> mDecoderStateEventListener;
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private final DecodeConfigMotorolaTypeII mDecodeConfig;

    private float[] mResampleBuffer = new float[0];
    private double mResamplePhase = 0;
    private float mPreviousSample;
    private boolean mResampleReady = false;
    private double mCurrentDecimatedRate;

    public MotorolaTypeIIDecoder(DecodeConfigMotorolaTypeII decodeConfig)
    {
        mDecodeConfig = decodeConfig;
        mFMDemodulator = FmDemodulatorFactory.getFmDemodulator();
        Bandplan bandplan = new Bandplan(decodeConfig.getBandplanType());
        mOswExtractor = new OswExtractor(bandplan);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MOTOROLA_TYPE_II;
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

        float[] resampled = resample(demodulated, mCurrentDecimatedRate, TARGET_SAMPLE_RATE);

        mOswExtractor.process(resampled, getMessageListener());
    }

    private void setSampleRate(double sampleRate)
    {
        int decimation = 1;
        while((sampleRate / decimation) >= 96000)
        {
            decimation *= 2;
        }

        mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);

        mCurrentDecimatedRate = sampleRate / decimation;

        float[] coefficients = FilterFactory.getLowPass(mCurrentDecimatedRate, BASEBAND_PASS, BASEBAND_STOP, 60,
                io.github.dsheirer.dsp.window.WindowType.HAMMING, true);

        mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
        mQBasebandFilter = FilterFactory.getRealFilter(coefficients);

        mLog.info("Motorola Type II decoder sample rate: " + mCurrentDecimatedRate +
                " (decimation: " + decimation + ") -> resample to " + TARGET_SAMPLE_RATE + " Hz");

        mOswExtractor.setSampleRate(TARGET_SAMPLE_RATE);
    }

    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
                mResamplePhase = 0;
                mResampleReady = false;
                setSampleRate(sourceEvent.getValue().doubleValue());
            }
        }
    }

    private float[] resample(float[] input, double inputRate, double outputRate)
    {
        double step = outputRate / inputRate;
        int estimateLen = (int)(input.length * step) + 2;
        if(mResampleBuffer.length < estimateLen)
            mResampleBuffer = new float[estimateLen];

        //Linear interpolation between the previous and current input samples.
        int outIdx = 0;
        for(int i = 0; i < input.length; i++)
        {
            mResamplePhase += step;
            while(mResamplePhase >= 1.0)
            {
                mResamplePhase -= 1.0;
                if(mResampleReady && outIdx < mResampleBuffer.length)
                {
                    //The output sample lies this fraction of an input sample before the current input sample.
                    float back = (float)(mResamplePhase / step);
                    mResampleBuffer[outIdx++] = input[i] + (mPreviousSample - input[i]) * back;
                }
                else
                    mResampleReady = true;
            }
            mPreviousSample = input[i];
        }

        float[] result = new float[outIdx];
        System.arraycopy(mResampleBuffer, 0, result, 0, outIdx);
        return result;
    }
}
