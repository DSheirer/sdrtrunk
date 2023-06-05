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

package io.github.dsheirer.dsp.fsk;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.sample.Listener;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LTR decoder designed to work with 8 kHz input audio samples and decode 300-baud signalling.
 *
 * This decoder monitors the incoming sample stream looking for either a positive or negative sequence of samples where
 * the energy delta across the baud period exceeds a threshold value and causes the symbol state to flip.  The symbol
 * state remains across successive baud periods until the next state change sequence is detected.
 *
 * Valid LTR messages start with a zero symbol and transition to a positive symbol for the sync bit, this decoder
 * monitors for excessively long sequences of a one symbol and forces the symbol state back to a zero.
 */
public class LTRDecoder implements Listener<float[]>
{
    private static final Logger mLog = LoggerFactory.getLogger(LTRDecoder.class);
    //Slope calculation sumXX value is always the same for 27 evenly spaced samples
    private static final double SLOPE_CALCULATION_SUM_XX = 1637.999992057681;
    private static final float BAUD_LENGTH = 8000.0f / 300.0f;
    private static final float SLOPE_THRESHOLD = 0.0048f;
    private static final int MAX_ONES_SEQUENCE = 18;
    private static final int OVERLAP = 26;
    private static final int POST_TRANSITION_SAMPLES_TO_SKIP = 26;
    private static final int SLOPE_CALCULATION_LENGTH = 27;
    private static final int SYMBOL_TRANSITION_IDEAL_MIN = 11;
    private static final int SYMBOL_TRANSITION_IDEAL_MAX = 16;

    private static float[] sLowPassFilterCoefficients;
    private boolean mSymbol = false;
    private float[] mResidual = new float[OVERLAP];
    private float mBaudCounter = 0f;
    private float mMaxSlope = 0;
    private int mExcessiveOneSequenceCounter = 0;
    private Listener<boolean[]> mSymbolListener;

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

    private IRealFilter mLowPassFilter = FilterFactory.getRealFilter(sLowPassFilterCoefficients);

    /**
     * Constructs an instance
     */
    public LTRDecoder()
    {
    }

    /**
     * Registers the listener to receive decoded symbols.
     * @param listener to register
     */
    public void setListener(Listener<boolean[]> listener)
    {
        mSymbolListener = listener;
    }

    /**
     * Processes the demodulated 8 kHz audio samples to extract the 300-baud LTR signalling and delivers the decoded
     * symbol array to the registered listener.
     * @param samples to demodulate.
     */
    public void receive(float[] samples)
    {
        if(mSymbolListener != null)
        {
            float[] filtered = mLowPassFilter.filter(samples);

            int timingAdjust = 0;
            int samplesToSkip = 0;

            float[] buffer = new float[filtered.length + OVERLAP];
            boolean[] symbols = new boolean[(int)((filtered.length + mBaudCounter) / BAUD_LENGTH)];
            int symbolPointer = 0;
            float slope = 0;

            try
            {
                System.arraycopy(mResidual, 0, buffer, 0, mResidual.length);
                System.arraycopy(filtered, 0, buffer, mResidual.length, filtered.length);
                System.arraycopy(filtered, filtered.length - OVERLAP, mResidual, 0, OVERLAP);

                for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer++)
                {
                    //Don't calculate slope if we're within the baud period following last symbol transition
                    if(samplesToSkip > 0)
                    {
                        samplesToSkip--;
                    }
                    else
                    {
                        slope = calculateSlope(buffer, bufferPointer);

                        if(mSymbol)
                        {
                            //Loop for crest of slope at lowest point below the negative slope threshold for transition
                            if(slope > mMaxSlope && mMaxSlope < -SLOPE_THRESHOLD)
                            {
                                mSymbol = false;

                                //Coarse symbol timing adjust - ideal toggle is middle of baud, between 11 & 16 samples
                                if(mBaudCounter < SYMBOL_TRANSITION_IDEAL_MIN)
                                {
                                    timingAdjust = 1;
                                }
                                else if(mBaudCounter > SYMBOL_TRANSITION_IDEAL_MAX)
                                {
                                    timingAdjust = -1;
                                }
                                else
                                {
                                    timingAdjust = 0;
                                }

                                mBaudCounter += timingAdjust;
                                samplesToSkip = POST_TRANSITION_SAMPLES_TO_SKIP + timingAdjust;
                            }
                            else if(slope < mMaxSlope)
                            {
                                mMaxSlope = slope;
                            }
                        }
                        else
                        {
                            //Loop for crest of slope at the highest point above the positive threshold
                            if(slope < mMaxSlope && mMaxSlope > SLOPE_THRESHOLD)
                            {
                                mSymbol = true;

                                //Coarse symbol timing adjust - ideal toggle is middle of baud, between 11 & 16 samples
                                if(mBaudCounter < SYMBOL_TRANSITION_IDEAL_MIN)
                                {
                                    timingAdjust = 1;
                                }
                                else if(mBaudCounter > SYMBOL_TRANSITION_IDEAL_MAX)
                                {
                                    timingAdjust = -1;
                                }
                                else
                                {
                                    timingAdjust = 0;
                                }
                                mBaudCounter += timingAdjust;
                                samplesToSkip = 26 + timingAdjust;
                            }
                            else if(slope > mMaxSlope)
                            {
                                mMaxSlope = slope;
                            }
                        }
                    }

                    mBaudCounter++;

                    if(mBaudCounter > BAUD_LENGTH)
                    {
                        //Expand symbols array length if needed to account for symbol timing adjustments
                        if(symbolPointer >= symbols.length)
                        {
                            symbols = Arrays.copyOf(symbols, symbols.length + 1);
                        }

                        symbols[symbolPointer++] = mSymbol;

                        mBaudCounter -= BAUD_LENGTH;

                        //Check for a continuous string of ones and flip the symbol state to zero.
                        if(mSymbol)
                        {
                            mExcessiveOneSequenceCounter++;

                            if(mExcessiveOneSequenceCounter > MAX_ONES_SEQUENCE)  //Verify that this is the right threshold for a string of ones
                            {
                                mSymbol = false;
                                mMaxSlope = -1f;
                                mExcessiveOneSequenceCounter = 0;
                            }
                        }
                        else
                        {
                            mExcessiveOneSequenceCounter = 0;
                        }
                    }
                }
            }
            catch(Exception e)
            {
                mLog.warn("Unexpected error while processing LTR samples", e);
            }

            //Truncate the symbols array if we didn't fill it completely, due to symbol timing adjustments.
            if(symbolPointer != symbols.length)
            {
                mSymbolListener.receive(Arrays.copyOf(symbols, symbolPointer));
            }
            else
            {
                mSymbolListener.receive(symbols);
            }
        }
    }

    /**
     * Calculates the slope of the 27 samples in the values array starting at the specified offset.
     * @param samples to load from
     * @param offset to the sample start for the slope calculation
     */
    public float calculateSlope(float[] samples, int offset)
    {
        /** mean of accumulated x & y values, used in updating formulas */
        double xbar = 0;
        double ybar = samples[offset];

        double sumXY = 0;
        double fact1, dx, dy;

        for(int x = 1; x < SLOPE_CALCULATION_LENGTH; x++)
        {
            fact1 = 1.0f + x;
            dx = x - xbar;
            dy = samples[offset + x] - ybar;
            sumXY += dx * dy * (x / (1.0f + x));
            xbar += dx / fact1;
            ybar += dy / fact1;
        }

        return (float)(sumXY / SLOPE_CALCULATION_SUM_XX);
    }
}
