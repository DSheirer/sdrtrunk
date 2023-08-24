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
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.module.decode.dmr.DMRMessageFramer;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DQPSK demodulator that uses Vector SIMD calculations for demodulating the sample stream.
 */
public class DQPSKVectorDemodulator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DQPSKVectorDemodulator.class);
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
    private float[] mIDecoded = new float[20];
    private float[] mQDecoded = new float[20];
    private float[] mPhase = new float[20];
    private int mSymbolRate;
    private float mSampleRate;
    private float mSamplesPerSymbol;
    private float mMu;
    private int mBufferOverlap;
    private int mInterpolationOffset;
    private Listener<List<Dibit>> mListener;
    private static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("+#000.0;-#000.0");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+#0.00000;-#0.00000");
    private static final DecimalFormat I_Q_FORMAT = new DecimalFormat("+#0.0000000;-#0.0000000");
    private final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private DecisionDirectedDQPSKSymbolDecoder mDecoder;
    private float mSymbolPointer = -5;

    /**
     * Constructor
     * @param symbolRate symbols per second
     */
    public DQPSKVectorDemodulator(int symbolRate)
    {
        mSymbolRate = symbolRate;
        mDecoder = new DecisionDirectedDQPSKSymbolDecoder(mSymbolRate);
    }

    /**
     * Sets the listener to receive decoded symbols.
     * @param listener to receive symbols.
     */
    public void setListener(Listener<List<Dibit>> listener)
    {
        mListener = listener;
    }

    public void receive(ComplexSamples complexSamples)
    {
        int sampleBufferLength = complexSamples.i().length;

        //Copy previous buffer residual samples to beginning of buffer.
        System.arraycopy(mIBuffer, mIBuffer.length - mBufferOverlap, mIBuffer, 0, mBufferOverlap);
        System.arraycopy(mQBuffer, mQBuffer.length - mBufferOverlap, mQBuffer, 0, mBufferOverlap);
        System.arraycopy(mIDecoded, mIDecoded.length - mBufferOverlap, mIDecoded, 0, mBufferOverlap);
        System.arraycopy(mQDecoded, mQDecoded.length - mBufferOverlap, mQDecoded, 0, mBufferOverlap);
        System.arraycopy(mPhase, mPhase.length - 9, mPhase, 0, 8);

        //Resize I/Q buffers if necessary
        int requiredBufferLength = complexSamples.i().length + mBufferOverlap;
        if(mIBuffer.length != requiredBufferLength)
        {
            mIBuffer = Arrays.copyOf(mIBuffer, requiredBufferLength);
            mQBuffer = Arrays.copyOf(mQBuffer, requiredBufferLength);
            mIDecoded = Arrays.copyOf(mIDecoded, requiredBufferLength);
            mQDecoded = Arrays.copyOf(mQDecoded, requiredBufferLength);
            mPhase = Arrays.copyOf(mPhase, sampleBufferLength + 8);
        }

        //Append new samples to the residual samples from the previous buffer.
        System.arraycopy(complexSamples.i(), 0, mIBuffer, mBufferOverlap, sampleBufferLength);
        System.arraycopy(complexSamples.q(), 0, mQBuffer, mBufferOverlap, sampleBufferLength);
        //mIDecoded, mQDecoded and mPhase will be filled below during the decoding process.

        float[] interpolatedI = new float[VECTOR_SPECIES.length()];
        float[] interpolatedQ = new float[VECTOR_SPECIES.length()];

        FloatVector iPrevious, qPreviousConjugate, iCurrent, qCurrent, differentialI, differentialQ;

        //Differential demodulation.
        for(int x = 0; x < sampleBufferLength; x += VECTOR_SPECIES.length())
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


            //Multiply current complex symbol times complex conjugate of previous complex symbol.
            differentialI = iPrevious.mul(iCurrent).sub(qPreviousConjugate.mul(qCurrent));
            differentialQ = iPrevious.mul(qCurrent).add(iCurrent.mul(qPreviousConjugate));

            //Write decoded samples to arrays
            differentialI.intoArray(mIDecoded, x + mBufferOverlap);
            differentialQ.intoArray(mQDecoded, x + mBufferOverlap);

            //Calculate phase angle using Arc Tangent and export to the phase array.
            differentialQ.lanewise(VectorOperators.ATAN2, differentialI).intoArray(mPhase, x + 4);
        }

        decode(mPhase, sampleBufferLength);
    }

    /**
     * Decodes the symbols from the differentially demodulated sample phasers by downsampling to the symbol rate and
     * making symbol decisions.
     * @param samples to decode
     */
    private void decode(float[] samples, int sampleCount)
    {
        StringBuilder sb = new StringBuilder();
        float debugAdjustment = 0;

        List<Dibit> symbols = new ArrayList<>();
        float sample = 0;
        float timingError = 0;

        for(int x = 0; x < sampleCount; x++)
        {
            if(mSymbolPointer >= mSamplesPerSymbol)
            {
                try
                {
                    sample = mInterpolator.filter(samples, x, mSymbolPointer % 1);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    System.out.println("Error - x [" + x + "] symbol pointer [" + mSymbolPointer + "] samples length [" + samples.length + "] sample count [" + sampleCount + "]");
                    System.exit(0);
                }
                Dibit symbol = toSymbol(sample);
                symbols.add(symbol);
                timingError = DQPSKTimingErrorCalculator.calculate(symbol, samples[x + 4], sample, samples[x + 5]);
                mSymbolPointer += (timingError * TIMING_ERROR_LOOP_GAIN);
                mSymbolPointer -= mSamplesPerSymbol;

                //debug
                debugAdjustment += (timingError * TIMING_ERROR_LOOP_GAIN);
            }

            sb.append(DECIMAL_FORMAT.format(samples[x])).append(",");
            sb.append(DECIMAL_FORMAT.format(sample)).append(",");
            sb.append(DECIMAL_FORMAT.format(debugAdjustment)).append("\n");

            mSymbolPointer++;
        }

//        System.out.println(sb);

        if(mListener != null)
        {
            mListener.receive(symbols);
        }
    }

    /**
     * Decodes the sample value to determine the correct QPSK quadrant and maps the value to a Dibit symbol.
     * @param sample in radians.
     * @return symbol decision.
     */
    private static Dibit toSymbol(float sample)
    {
        if(sample > 0)
        {
            return sample > SYMBOL_DECISION_POSITIVE ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        }
        else
        {
            return sample < SYMBOL_DECISION_NEGATIVE ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }

    /**
     * Sets the sample rate to determine the
     * @param sampleRate
     */
    public void setSampleRate(double sampleRate)
    {
        mSampleRate = (float)sampleRate;
        mSamplesPerSymbol = mSampleRate / mSymbolRate;
        mDecoder.setSampleRate(mSampleRate);
        mMu = mSamplesPerSymbol - (int)mSamplesPerSymbol; //Fractional part of the samples per symbol rate
        mInterpolationOffset = (int)Math.floor(mSamplesPerSymbol) - 4; //Interpolate at the middle of 8x samples
        mBufferOverlap = (int)Math.floor(mSamplesPerSymbol) + 4;
    }

    /**
     * Broadcasts the demodulated dibit symbols to a registered listener
     * @param symbols to broadcast
     */
    protected void broadcast(List<Dibit> symbols)
    {
        if(mListener != null)
        {
            mListener.receive(symbols);
        }
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

        int matchedFilterSymbolCount = 16;
        float alpha = 0.2f;
        float[] matchedFilterCoefficients = FilterFactory.getRootRaisedCosine(50000.0 / 2400.0, matchedFilterSymbolCount, alpha);

        IRealFilter iMatchedFilter = FilterFactory.getRealFilter(matchedFilterCoefficients);
        IRealFilter qMatchedFilter = FilterFactory.getRealFilter(matchedFilterCoefficients);

        String file = "/home/denny/SDRTrunk/recordings/20230819_064211_451250000_SaiaNet_Syracuse_Control_29_baseband.wav";
//        String file = "/home/denny/SDRTrunk/recordings/20230819_064344_454575000_JPJ_Communications_(DMR)_Madison_Control_28_baseband.wav";
        boolean autoReplay = false;

        DQPSKVectorDemodulator demodulator = new DQPSKVectorDemodulator(4800);
        DMRMessageFramer framer = new DMRMessageFramer(null);
        demodulator.setListener(dibits -> {
            for(Dibit dibit : dibits)
            {
                framer.receive(dibit);
            }
        });
        framer.setListener(iMessage -> System.out.println(iMessage));

        DMRDecoder legacy = new DMRDecoder(new DecodeConfigDMR());
        legacy.setMessageListener(iMessage -> System.out.println(iMessage));


        Listener<INativeBuffer> nativeBufferListener = (INativeBuffer iNativeBuffer) ->
        {
            Iterator<ComplexSamples> it = iNativeBuffer.iterator();
            while(it.hasNext())
            {
                ComplexSamples unfiltered = it.next();
//                float[] iFiltered = iMatchedFilter.filter(unfiltered.i());
//                float[] qFiltered = qMatchedFilter.filter(unfiltered.q());
                float[] iFiltered = iChannelFilter.filter(unfiltered.i());
                float[] qFiltered = qChannelFilter.filter(unfiltered.q());

//                iFiltered = iMatchedFilter.filter(iFiltered);
//                qFiltered = qMatchedFilter.filter(qFiltered);

                ComplexSamples filtered = new ComplexSamples(iFiltered, qFiltered, unfiltered.timestamp());
                demodulator.receive(filtered);
//                demodulator.receive(unfiltered);
//                legacy.receive(unfiltered);
            }
        };
        try(ComplexWaveSource source = new ComplexWaveSource(new File(file), autoReplay))
        {
            source.setListener(nativeBufferListener);
            source.start();
            demodulator.setSampleRate(source.getSampleRate());
            legacy.setSampleRate(source.getSampleRate());
            legacy.start();

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
