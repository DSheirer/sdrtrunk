/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.dsp.gain.complex.ComplexGain;
import io.github.dsheirer.dsp.gain.complex.ComplexGainFactory;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmrDecoderWithIsi implements Listener<INativeBuffer>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DQPSKVectorDemodulator.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");
    private static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("+#000.0;-#000.0");
    private static final DecimalFormat I_Q_FORMAT = new DecimalFormat("+#0.0000000;-#0.0000000");
    private IRealFilter mChannelFilterI;
    private IRealFilter mChannelFilterQ;
    private IRealFilter mMatchedFilterI;
    private IRealFilter mMatchedFilterQ;
//    private ComplexGain mGain = ComplexGainFactory.getComplexGain(250.0f);
    private ComplexGain mGain = ComplexGainFactory.getComplexGain(1.0f);
    private CostasLoopTemp mPll = new CostasLoopTemp(50000.0f, 4800.0f);
    private float[] mIBuffer = new float[20]; //Initial size 20 for array copy, but gets resized on first buffer
    private float[] mQBuffer = new float[20];
    private float[] mIDecoded = new float[20];
    private float[] mQDecoded = new float[20];
    private int mSymbolRate;
    private float mSampleRate;
    private float mSamplesPerSymbol;
    private float mMu;
    private int mBufferOverlap;
    private int mInterpolationOffset;
    private Listener<List<Dibit>> mListener;
    private final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private DecisionDirectedDQPSKSymbolDecoder mDecoder;

    public DmrDecoderWithIsi(int symbolRate, float pllFrequency)
    {
        mPll.setLoopFrequency(pllFrequency);
        mSymbolRate = symbolRate;
        mDecoder = new DecisionDirectedDQPSKSymbolDecoder(mSymbolRate);

        float[] coefficients = null;
        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(50000)
                .passBandCutoff(5100)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandAmplitude(0.0)
                .stopBandStart(6500)
                .stopBandRipple(0.01)
                .build();

        try
        {
            coefficients = FilterFactory.getTaps(specification);//
        }
        catch(Exception fde) //FilterDesignException
        {
            System.out.println("Error");
        }

        mChannelFilterI = FilterFactory.getRealFilter(coefficients);
        mChannelFilterQ = FilterFactory.getRealFilter(coefficients);

        int matchedFilterSymbolCount = 8;
        float alpha = 0.2f;
        float[] matchedCoefficients = FilterFactory.getRootRaisedCosine(50000.0 / 4800.0, matchedFilterSymbolCount, alpha);
        mMatchedFilterI = FilterFactory.getRealFilter(matchedCoefficients);
        mMatchedFilterQ = FilterFactory.getRealFilter(matchedCoefficients);
    }

    public void setSampleRate(double sampleRate)
    {
        mSampleRate = (float)sampleRate;
        mSamplesPerSymbol = mSampleRate / mSymbolRate;
        mDecoder.setSampleRate(mSampleRate);
        mMu = mSamplesPerSymbol - (int)mSamplesPerSymbol; //Fractional part
        mInterpolationOffset = (int)Math.floor(mSamplesPerSymbol) - 4;
        mBufferOverlap = (int)Math.floor(mSamplesPerSymbol) + 4;
    }

    @Override
    public void receive(INativeBuffer iNativeBuffer)
    {
        Iterator<ComplexSamples> it = iNativeBuffer.iterator();
        while(it.hasNext())
        {
            ComplexSamples complexSamples = it.next();

//            complexSamples = mGain.apply(complexSamples);
            float[] iChannelFiltered = mChannelFilterI.filter(complexSamples.i());
            float[] qChannelFiltered = mChannelFilterQ.filter(complexSamples.q());
//            float[] iMatchFiltered = mMatchedFilterI.filter(iChannelFiltered);
//            float[] qMatchFiltered = mMatchedFilterQ.filter(qChannelFiltered);
            float[] iMatchFiltered = mMatchedFilterI.filter(complexSamples.i());
            float[] qMatchFiltered = mMatchedFilterQ.filter(complexSamples.q());
            complexSamples = new ComplexSamples(iMatchFiltered, qMatchFiltered, complexSamples.timestamp());
//            complexSamples = new ComplexSamples(iChannelFiltered, qChannelFiltered, complexSamples.timestamp());

            StringBuilder sb = new StringBuilder();

            int sampleBufferLength = complexSamples.i().length;

            //Copy previous buffer residual samples to beginning of buffer.
            System.arraycopy(mIBuffer, mIBuffer.length - mBufferOverlap, mIBuffer, 0, mBufferOverlap);
            System.arraycopy(mQBuffer, mQBuffer.length - mBufferOverlap, mQBuffer, 0, mBufferOverlap);
            System.arraycopy(mIDecoded, mIDecoded.length - mBufferOverlap, mIDecoded, 0, mBufferOverlap);
            System.arraycopy(mQDecoded, mQDecoded.length - mBufferOverlap, mQDecoded, 0, mBufferOverlap);

            //Resize I/Q buffers if necessary
            int requiredBufferLength = complexSamples.i().length + mBufferOverlap;
            if(mIBuffer.length != requiredBufferLength)
            {
                mIBuffer = Arrays.copyOf(mIBuffer, requiredBufferLength);
                mQBuffer = Arrays.copyOf(mQBuffer, requiredBufferLength);
                mIDecoded = Arrays.copyOf(mIDecoded, requiredBufferLength);
                mQDecoded = Arrays.copyOf(mQDecoded, requiredBufferLength);
            }

            //Append new samples to the residual samples from the previous buffer.
            System.arraycopy(complexSamples.i(), 0, mIBuffer, mBufferOverlap, sampleBufferLength);
            System.arraycopy(complexSamples.q(), 0, mQBuffer, mBufferOverlap, sampleBufferLength);

            float[] iRotated = new float[mIBuffer.length];
            float[] qRotated = new float[mQBuffer.length];

            for(int y = 0; y < mIBuffer.length; y++)
            {
                Complex vector = mPll.incrementAndGetCurrentVector();
                iRotated[y] = (mIBuffer[y] * vector.inphase()) - (mQBuffer[y] * vector.quadrature());
                qRotated[y] = (mIBuffer[y] * vector.quadrature()) + (vector.inphase() * mQBuffer[y]);
            }

            //Differential demodulation.  Note: we adjust to the nearest integer offset of samples per symbol and incur a
            //small phase error penalty during differential decoding to achieve processing gains of using vector operations.
            for(int x = 0; x < sampleBufferLength; x ++)
            {
                int offset = mInterpolationOffset + x;

//                float iPrevious = mIBuffer[x];
//                float qPreviousConjugate = -mQBuffer[x];
//                float iCurrent = mInterpolator.filter(mIBuffer, offset, mMu);
//                float qCurrent = mInterpolator.filter(mQBuffer, offset, mMu);

                float iCurrent = mIBuffer[x];
                float qCurrent = mQBuffer[x];
                float iPrevious = mInterpolator.filter(mIBuffer, offset, mMu);
                float qPreviousConjugate = -mInterpolator.filter(mQBuffer, offset, mMu);

                float differentialI = (iPrevious * iCurrent) - (qPreviousConjugate * qCurrent);
                float differentialQ = (iPrevious * qCurrent) + (iCurrent * qPreviousConjugate);
                double interpolatedRadians = Math.atan2(differentialQ, differentialI);

                iCurrent = iRotated[x];
                qCurrent = qRotated[x];
                iPrevious = mInterpolator.filter(iRotated, offset, mMu);
                qPreviousConjugate = -mInterpolator.filter(qRotated, offset, mMu);

                differentialI = (iPrevious * iCurrent) - (qPreviousConjugate * qCurrent);
                differentialQ = (iPrevious * qCurrent) + (iCurrent * qPreviousConjugate);
                double rotatedRadians = Math.atan2(differentialQ, differentialI);

                System.out.println(DECIMAL_FORMAT.format(interpolatedRadians) + "," + DECIMAL_FORMAT.format(rotatedRadians));
            }

            System.out.println(sb);
        }
    }

    public static void main(String[] args)
    {
        float pllFrequency = 0.0f;

        LOGGER.info("Starting ...");

        String file = "/home/denny/SDRTrunk/recordings/20230819_064211_451250000_SaiaNet_Syracuse_Control_29_baseband.wav";
//        String file = "/home/denny/SDRTrunk/recordings/20230819_064344_454575000_JPJ_Communications_(DMR)_Madison_Control_28_baseband.wav";
        boolean autoReplay = false;
        DmrDecoderWithIsi decoder = new DmrDecoderWithIsi(4800, pllFrequency);

        try(ComplexWaveSource source = new ComplexWaveSource(new File(file), autoReplay))
        {
            source.setListener(decoder);
            source.start();
            decoder.setSampleRate(source.getSampleRate());

            while(true)
            {
                source.next(2048, true);
                //wait
            }
        }
        catch(IOException ioe)
        {
            LOGGER.error("Error", ioe);
        }

        LOGGER.info("Finished");
    }
}
