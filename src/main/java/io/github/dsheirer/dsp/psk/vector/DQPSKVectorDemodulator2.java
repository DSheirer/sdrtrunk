/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.DMRMessageFramer;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DQPSK demodulator that uses Vector SIMD calculations for demodulating the sample stream.
 */
public class DQPSKVectorDemodulator2
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DQPSKVectorDemodulator2.class);
    private static final float OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1 = (float)(Math.PI / 4.0);
    private static final float OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3 = 3.0f * OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_1 = -OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_1;
    private static final float OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_MINUS_3 = -OPTIMAL_SAMPLING_POINT_RADIANS_QUADRANT_PLUS_3;
    private static final float SYMBOL_DECISION_POSITIVE = (float)(Math.PI / 2.0);
    private static final float SYMBOL_DECISION_NEGATIVE = -SYMBOL_DECISION_POSITIVE;


    private static final float TIMING_ERROR_LOOP_GAIN = 0.01f;
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;

    private float[] mIBuffer = new float[20]; //Initial size 20 for array copy, but gets resized on first buffer
    private float[] mQBuffer = new float[20];
    private int mSymbolRate;
    private float mSampleRate;
    private float mSamplesPerSymbol;
    private float mMu;
    private int mBufferOverlap;
    private int mInterpolationOffset;
    private Listener<Dibit> mSymbolListener;
    private static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("+#000.0;-#000.0");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");
    private static final DecimalFormat I_Q_FORMAT = new DecimalFormat("+#0.0000000;-#0.0000000");
    private final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private DecisionDirectedDQPSKSymbolDecoder mDecoder;
    private float mSymbolPointer = -5;
    private DmrSymbolProcessor2 mSymbolProcessor2 = new DmrSymbolProcessor2();

    /**
     * Constructor
     * @param symbolRate symbols per second
     */
    public DQPSKVectorDemodulator2(int symbolRate)
    {
        mSymbolRate = symbolRate;
        mDecoder = new DecisionDirectedDQPSKSymbolDecoder(mSymbolRate);
    }

    /**
     * Sets the listener to receive decoded symbols.
     * @param symbolListener to receive symbols.
     */
    public void setSymbolListener(Listener<Dibit> symbolListener)
    {
        mSymbolProcessor2.setListener(symbolListener);
//        mSymbolListener = symbolListener;
    }

    public void receive(ComplexSamples samples)
    {
        int sampleLength = samples.i().length;
        int bufferOverlap = mBufferOverlap;

        //Copy previous buffer residual samples to beginning of buffer.
        System.arraycopy(mIBuffer, mIBuffer.length - bufferOverlap, mIBuffer, 0, bufferOverlap);
        System.arraycopy(mQBuffer, mQBuffer.length - bufferOverlap, mQBuffer, 0, bufferOverlap);

        //Resize I/Q buffers if necessary
        int requiredBufferLength = sampleLength + bufferOverlap;
        if(mIBuffer.length != requiredBufferLength)
        {
            mIBuffer = Arrays.copyOf(mIBuffer, requiredBufferLength);
            mQBuffer = Arrays.copyOf(mQBuffer, requiredBufferLength);
        }

        //Append new samples to the residual samples from the previous buffer.
        System.arraycopy(samples.i(), 0, mIBuffer, bufferOverlap, sampleLength);
        System.arraycopy(samples.q(), 0, mQBuffer, bufferOverlap, sampleLength);
        //mIDecoded, mQDecoded and mPhase will be filled below during the decoding process.

        float[] interpolatedI = new float[VECTOR_SPECIES.length()];
        float[] interpolatedQ = new float[VECTOR_SPECIES.length()];
        float[] decodedPhases = new float[VECTOR_SPECIES.length()];
        FloatVector iPrevious, qPreviousConjugate, iCurrent, qCurrent, differentialI, differentialQ;

        //Differential demodulation.
        for(int x = 0; x < sampleLength; x += VECTOR_SPECIES.length())
        {
            iPrevious = FloatVector.fromArray(VECTOR_SPECIES, mIBuffer, x);
            qPreviousConjugate = FloatVector.fromArray(VECTOR_SPECIES, mQBuffer, x).neg(); //Complex Conjugate

            int offset = mInterpolationOffset + x;
            int index;
            for(int y = 0; y < VECTOR_SPECIES.length(); y++)
            {
                index = offset + y;
                interpolatedI[y] = mInterpolator.filter(mIBuffer, index, mMu);
                interpolatedQ[y] = mInterpolator.filter(mQBuffer, index, mMu);
            }
            iCurrent = FloatVector.fromArray(VECTOR_SPECIES, interpolatedI, 0);
            qCurrent = FloatVector.fromArray(VECTOR_SPECIES, interpolatedQ, 0);

            //Multiply current complex sample by the complex conjugate of previous complex sample.
            differentialI = iPrevious.mul(iCurrent).sub(qPreviousConjugate.mul(qCurrent));
            differentialQ = iPrevious.mul(qCurrent).add(iCurrent.mul(qPreviousConjugate));

            //Calculate phase angles using Arc Tangent and export to the decoded phases array.
            differentialQ.lanewise(VectorOperators.ATAN2, differentialI).intoArray(decodedPhases, 0);

            //Process decoded phase array to extract/accumulate symbols and update timing
//            mSymbolProcessor.process(decodedPhases);
            mSymbolProcessor2.process(decodedPhases);
        }
    }

    /**
     * Sets the sample rate
     * @param sampleRate of the incoming sample stream
     */
    public void setSampleRate(double sampleRate)
    {
        mSampleRate = (float)sampleRate;
        mSamplesPerSymbol = mSampleRate / mSymbolRate;
        mDecoder.setSampleRate(mSampleRate);
        mSymbolProcessor2.setSamplesPerSymbol(mSamplesPerSymbol);
        updateObservedSamplesPerSymbol(mSamplesPerSymbol);
    }

    /**
     * Updates the observed/actual samples per symbol to effect the interpolation point during differential decoding.
     * @param samplesPerSymbol to apply
     */
    private void updateObservedSamplesPerSymbol(float samplesPerSymbol)
    {
        mMu = samplesPerSymbol % 1; //Fractional part of the samples per symbol rate
        mInterpolationOffset = (int)Math.floor(samplesPerSymbol) - 4; //Interpolate at the middle of 8x samples
        mBufferOverlap = (int)Math.floor(samplesPerSymbol) + 4;
    }

    public static void main(String[] args)
    {
        LOGGER.info("Starting ...");

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

        IRealFilter iChannelFilter = FilterFactory.getRealFilter(coefficients);
        IRealFilter qChannelFilter = FilterFactory.getRealFilter(coefficients);

        double samplesPerSymbol = 50000.0 / 4800.0;
        int order = 16;
        float alpha = 0.2f;
        float[] matchedFilterCoefficients = FilterFactory.getRootRaisedCosine(samplesPerSymbol, order, alpha);

        IRealFilter iMatchedFilter = FilterFactory.getRealFilter(matchedFilterCoefficients);
        IRealFilter qMatchedFilter = FilterFactory.getRealFilter(matchedFilterCoefficients);

//        String directory = "D:\\DQPSK Equalizer Research\\"; //Windows
        String directory = "/media/denny/T9/DQPSK Equalizer Research/"; //Linux
        String file = directory + "DMR_1_CAPPLUS.wav";
//        String file = directory + "DMR_2_CAPPLUS.wav";
//        String file = directory + "DMR_3_CAPPLUS.wav";
        boolean autoReplay = false;

        DQPSKVectorDemodulator2 demodulator = new DQPSKVectorDemodulator2(4800);
        DMRMessageFramer framer = new DMRMessageFramer(null);
        demodulator.setSymbolListener(framer);
        Listener<IMessage> listener = new Listener<>()
        {
            private int mBitErrorCounter;

            @Override
            public void receive(IMessage iMessage)
            {
                int errors = 0;

                if(iMessage instanceof DMRBurst burst)
                {
                    errors = burst.getMessage().getCorrectedBitCount();
                    mBitErrorCounter += errors;
                }
//                else if(iMessage instanceof SyncLossMessage loss)
//                {
//                    errors = loss.getBitsProcessed();
//                    mBitErrorCounter += errors;
//                    return;
//                }

                System.out.println(">>MESSAGE: TS" + iMessage.getTimeslot() + " " + iMessage + " [" + errors + " / " + mBitErrorCounter + "]");
            }
        };
        framer.setListener(listener);

        Listener<INativeBuffer> nativeBufferListener = new Listener<INativeBuffer>()
        {
            private int mSampleCounter = 0;

            @Override
            public void receive(INativeBuffer iNativeBuffer)
            {
                Iterator<ComplexSamples> it = iNativeBuffer.iterator();
                while(it.hasNext())
                {
                    ComplexSamples unfiltered = it.next();

//                    float[] iGained = new float[unfiltered.i().length];
//                    float[] qGained = new float[unfiltered.q().length];
//
//                    for(int x = 0; x < unfiltered.i().length; x++)
//                    {
//                        iGained[x] = unfiltered.i()[x] * mGain;
//                        qGained[x] = unfiltered.q()[x] * mGain;
//                    }
//
//                    float[] iFiltered = iGained;
//                    float[] qFiltered = qGained;

                    float[] iFiltered = unfiltered.i();
                    float[] qFiltered = unfiltered.q();

                    iFiltered = iChannelFilter.filter(iFiltered);
                    qFiltered = qChannelFilter.filter(qFiltered);

//                    iFiltered = iMatchedFilter.filter(iFiltered);
//                    qFiltered = qMatchedFilter.filter(qFiltered);

                    ComplexSamples filtered = new ComplexSamples(iFiltered, qFiltered, unfiltered.timestamp());
                    demodulator.receive(filtered);

                    mSampleCounter += iFiltered.length;
                }
            }
        };
        try(ComplexWaveSource source = new ComplexWaveSource(new File(file), autoReplay))
        {
            source.setListener(nativeBufferListener);
            source.start();
            demodulator.setSampleRate(source.getSampleRate());

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
