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

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.filter.interpolator.LinearInterpolator;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.gui.viewer.symbol.SymbolViewerFX;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Listener;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Demodulates filtered LSM I/Q samples to feed message framer for sync detection and framing.
 */
public class P25P1DemodulatorLSM
{
    private static final float HALF_PI = (float)(Math.PI / 2.0);
    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private static final float MAX_PLL = (float)(Math.PI / 3.0); //+/- 800 Hz
    private static final float OBJECTIVE_MAGNITUDE = 1.0f;
    private static final int SYMBOL_RATE = 4800;

    private final DibitToByteBufferAssembler mDibitAssembler = new DibitToByteBufferAssembler(300);
    private final FeedbackDecoder mFeedbackDecoder;
    private final P25P1MessageFramer mMessageFramer;
    private SymbolViewerFX mDebugSymbolViewer;
    private double mSamplePoint;
    private double mSamplesPerSymbol;
    private double mSamplesPerHalfSymbol;
    private float[] mBufferI;
    private float[] mBufferQ;
    private float mPLL = 0f;
    private float mSampleGain = 1f;
    private float mPreviousMiddleI, mPreviousMiddleQ, mPreviousCurrentI, mPreviousCurrentQ, mPreviousSymbolI = 0.7f, mPreviousSymbolQ = 0.7f;
    private int mBufferPointer;
    private int mBufferReserve;

    /**
     * Constructs an instance
     * @param messageFramer for receiving demodulated symbol stream and providing sync detection events.
     * @param feedbackDecoder (parent) for receiving PLL carrier offset error reports.
     */
    public P25P1DemodulatorLSM(P25P1MessageFramer messageFramer, FeedbackDecoder feedbackDecoder)
    {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
    }

    /**
     * Reset the PLL when the tuner source either changes center frequency or adjust the PPM value.
     */
    public void resetPLL()
    {
        mPLL = 0f;
    }

    /**
     * Debug utility for viewing I/Q waveform, demodulated waveform and symbol decision details, sequentially.
     */
    protected SymbolViewerFX getDebugSymbolViewer()
    {
        if(mDebugSymbolViewer == null)
        {
            mDebugSymbolViewer = new SymbolViewerFX();
        }

        return mDebugSymbolViewer;
    }

    /**
     * Primary input method for receiving a stream of filtered, pulse-shaped samples to process into symbols.
     * @param i inphase samples to process
     * @param q quadrature samples to process
     */
    public void process(float[] i, float[] q)
    {
        //Shadow copy heap member variables onto the stack
        double samplePoint = mSamplePoint;
        double samplesPerSymbol = mSamplesPerSymbol;
        double samplesPerHalfSymbol = mSamplesPerHalfSymbol;
        float pll = mPLL;
        float previousSymbolI = mPreviousSymbolI;
        float previousSymbolQ = mPreviousSymbolQ;
        float previousMiddleI = mPreviousMiddleI;
        float previousMiddleQ = mPreviousMiddleQ;
        float previousCurrentI = mPreviousCurrentI;
        float previousCurrentQ = mPreviousCurrentQ;
        float sampleGain = mSampleGain;
        int bufferPointer = mBufferPointer;

        float pllGain = 0.1f;
        double tedGain = samplesPerSymbol / 4;
        double maxTimingAdjustment = samplesPerSymbol / 25;
        double pointer, residual, timingAdjustment;
        float magnitude, phaseError = 0, requiredGain, softSymbol, pllI, pllQ, pllTemp;
        float iMiddle, qMiddle, iCurrent, qCurrent, iMiddleDemodulated, qMiddleDemodulated, iSymbol, qSymbol;
        int offset;
        Dibit hardSymbol;

        //I/Q buffers are the same length as incoming samples padded by an extra symbol length for processing space.
        int bufferReserve = mBufferReserve;
        int requiredLength = i.length + bufferReserve;

        if(mBufferI.length != requiredLength)
        {
            mBufferI = Arrays.copyOf(mBufferI, requiredLength);
            mBufferQ = Arrays.copyOf(mBufferQ, requiredLength);
        }

        //Transfer extra reserve samples from last processing iteration to the front of the I/Q buffers
        System.arraycopy(mBufferI, bufferPointer, mBufferI, 0, bufferReserve);
        System.arraycopy(mBufferQ, bufferPointer, mBufferQ, 0, bufferReserve);

        //Copy incoming I/Q samples to the processing buffers
        System.arraycopy(i, 0, mBufferI, bufferReserve, i.length);
        System.arraycopy(q, 0, mBufferQ, bufferReserve, q.length);
        bufferPointer = 0;
        int bufferReload = mBufferI.length - bufferReserve;

        while(bufferPointer < bufferReload)
        {
            bufferPointer++;
            samplePoint--;

            if(samplePoint < 1)
            {
                //Sample point is the middle sample, between the previous and current symbols.
                iMiddle = LinearInterpolator.calculate(mBufferI[bufferPointer], mBufferI[bufferPointer + 1], samplePoint);
                qMiddle = LinearInterpolator.calculate(mBufferQ[bufferPointer], mBufferQ[bufferPointer + 1], samplePoint);

                //Calculate offset to next symbol.
                pointer = bufferPointer + samplePoint + samplesPerHalfSymbol;
                offset = (int)Math.floor(pointer);
                residual = pointer - offset;
                iCurrent = LinearInterpolator.calculate(mBufferI[offset], mBufferI[offset + 1], residual);
                qCurrent = LinearInterpolator.calculate(mBufferQ[offset], mBufferQ[offset + 1], residual);

                //Adjust sample gain based on highest magnitude and apply to both middle and current samples.
                magnitude = (float)(Math.sqrt(Math.pow(iCurrent, 2.0) + Math.pow(qCurrent, 2.0)));

                if(magnitude > 0 && !Float.isInfinite(magnitude))
                {
                    requiredGain = constrain(OBJECTIVE_MAGNITUDE / magnitude, 500);
                    sampleGain += (requiredGain - sampleGain) * .05f;
                    sampleGain = Math.min(sampleGain, requiredGain);
                    sampleGain = Math.min(sampleGain, 500);
                }

                //Apply gain to the samples
                iMiddle *= sampleGain;
                qMiddle *= sampleGain;
                iCurrent *= sampleGain;
                qCurrent *= sampleGain;

                //Create I/Q representation of current PLL state
                pllI = (float)Math.cos(pll);
                pllQ = (float)Math.sin(pll);

                //Differential demodulation of middle sample
                iMiddleDemodulated = (previousMiddleI * iMiddle) - (-previousMiddleQ * qMiddle);
                qMiddleDemodulated = (previousMiddleI * qMiddle) + (-previousMiddleQ * iMiddle);

                //Rotate middle sample by the PLL offset
                pllTemp = (iMiddleDemodulated * pllI) - (qMiddleDemodulated * pllQ);
                qMiddleDemodulated = (qMiddleDemodulated * pllI) + (iMiddleDemodulated * pllQ);
                iMiddleDemodulated = pllTemp;

                //Differential demodulation of symbol
                iSymbol = (previousCurrentI * iCurrent) - (-previousCurrentQ * qCurrent);
                qSymbol = (previousCurrentI * qCurrent) + (-previousCurrentQ * iCurrent);

                //Rotate symbol by the PLL offset
                pllTemp = (iSymbol * pllI) - (qSymbol * pllQ);
                qSymbol = (qSymbol * pllI) + (iSymbol * pllQ);
                iSymbol = pllTemp;

                //Calculate the symbol (radians) from the I/Q values
                softSymbol = (float)Math.atan2(qSymbol, iSymbol);

                //Apply Gardner timing error detection and correction
                timingAdjustment = ((previousSymbolI - iSymbol) * iMiddleDemodulated) + ((previousSymbolQ - qSymbol) * qMiddleDemodulated);
                timingAdjustment = constrain(timingAdjustment, maxTimingAdjustment);
                timingAdjustment *= tedGain;
                samplePoint += timingAdjustment;

                //Phase Locked Loop adjustment. Note: don't adjust PLL when soft symbol value is zero.
                if(softSymbol != 0)
                {
                    hardSymbol = toDibit(softSymbol);
                    phaseError = constrain(softSymbol - hardSymbol.getIdealPhase(), .3f);
                    pll -= (phaseError * pllGain);
                    pll = constrain(pll, MAX_PLL);
                }
                else
                {
                    hardSymbol = Dibit.D00_PLUS_1;
                }

                //Message framer returns boolean if valid sync and NID were detected/decoded.  Send a PLL measured
                //error report up to the tuner source so that it can self correct for mistuned channel and also to
                //display the measured carrier offset value in the channel display
                if(mMessageFramer.processWithSoftSyncDetect(softSymbol, hardSymbol))
                {
                    mFeedbackDecoder.processPLLError(pll, SYMBOL_RATE);
                }

                mDibitAssembler.receive(hardSymbol);
                mFeedbackDecoder.broadcast(softSymbol);

//                if(bufferPointer >= 15)
//                {
//                    SymbolViewerFX viewer = getDebugSymbolViewer();
//                    CountDownLatch latch = new CountDownLatch(1);
//                    float[] rawI = Arrays.copyOfRange(mBufferI, bufferPointer - 15, bufferPointer + 15);
//                    float[] rawQ = Arrays.copyOfRange(mBufferQ, bufferPointer - 15, bufferPointer + 15);
//                    double middle = 15 + samplePoint;// - timingAdjustment;
//                    double previous = middle - samplesPerHalfSymbol;
//                    double current = middle + samplesPerHalfSymbol;
//                    double[] points = {previous, middle, current, //Timing
//                            previousCurrentI, iMiddle, iCurrent, //RawI
//                            previousCurrentQ, qMiddle, qCurrent, //RawQ
//                            previousSymbolI, iMiddleDemodulated, iSymbol,  //DemodI
//                            previousSymbolQ, qMiddleDemodulated, qSymbol}; //DemodQ
//                    float middleSymbol = normalize((float)Math.atan2(qMiddleDemodulated, iMiddleDemodulated) + pll);
//                    float[] symbols = {previousSymbol, middleSymbol, softSymbol};
//                    float adjustedPLL = pll + (phaseError * pllGain);
//                    viewer.receive(samplesPerSymbol, rawI, rawQ, sampleGain, adjustedPLL, points, symbols, latch);
//                    try
//                    {
//                        latch.await();
//                    }
//                    catch(InterruptedException e)
//                    {
//                        throw new RuntimeException(e);
//                    }
//                    previousSymbol = softSymbol;
//                }

                //Shuffle the values
                previousSymbolI = iSymbol;
                previousSymbolQ = qSymbol;
                previousMiddleI = iMiddle;
                previousMiddleQ = qMiddle;
                previousCurrentI = iCurrent;
                previousCurrentQ = qCurrent;

                //Add another symbol's worth of samples to the counter
                samplePoint += samplesPerSymbol;
            }
        }

        //Copy shadow variables back to member variables.
        mPLL = pll;
        mBufferPointer = bufferPointer;
        mPreviousMiddleI = previousMiddleI;
        mPreviousMiddleQ = previousMiddleQ;
        mPreviousCurrentI = previousCurrentI;
        mPreviousCurrentQ = previousCurrentQ;
        mPreviousSymbolI = previousSymbolI;
        mPreviousSymbolQ = previousSymbolQ;
        mSampleGain = sampleGain;
        mSamplePoint = samplePoint;
    }

    /**
     * Normalizes the value in radians to the range of +/-(2 * PI)
     * @param value to normalize
     * @return normalized value.
     */
    private static float normalize(float value)
    {
        if(value > Math.PI)
        {
            return value - TWO_PI;
        }
        else if(value < -Math.PI)
        {
            return value + TWO_PI;
        }

        return value;
    }

    /**
     * Constrains value to range:  -constraint to constrain
     * @param value to constrain
     * @param constraint as max positive value
     * @return constrained value.
     */
    private static double constrain(double value, double constraint)
    {
        if(Double.isNaN(value) || Double.isInfinite(value))
        {
            return 0.0;
        }
        else if(value > constraint)
        {
            return constraint;
        }

        return Math.max(value, -constraint);
    }

    /**
     * Constrains value to range:  -constraint to constrain
     * @param value to constrain
     * @param constraint as max positive value
     * @return constrained value.
     */
    private static float constrain(float value, float constraint)
    {
        if(Float.isNaN(value) || Float.isInfinite(value))
        {
            return 0.0f;
        }

        if(value > constraint)
        {
            return constraint;
        }

        return Math.max(value, -constraint);
    }

    /**
     * Sets or updates the samples per symbol
     * @param samplesPerSymbol to apply.
     */
    public void setSamplesPerSymbol(float samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mSamplesPerHalfSymbol = samplesPerSymbol / 2.0f;
        mSamplePoint = samplesPerSymbol;
        mBufferReserve = (int)Math.ceil(samplesPerSymbol);
        mBufferI = new float[mBufferReserve];
        mBufferQ = new float[mBufferReserve];
        mBufferPointer = 0;
    }

    /**
     * Registers the listener to receive demodulated bit stream buffers.
     * @param listener to register
     */
    public void setBufferListener(Listener<ByteBuffer> listener)
    {
        mDibitAssembler.setBufferListener(listener);
    }

    /**
     * Indicates if there is a registered buffer listener
     */
    public boolean hasBufferListener()
    {
        return mDibitAssembler.hasBufferListeners();
    }

    /**
     * Decodes the sample value to determine the correct QPSK quadrant and maps the value to a Dibit symbol.
     * @param softSymbol in radians.
     * @return symbol decision.
     */
    public static Dibit toDibit(float softSymbol)
    {
        if(softSymbol > 0)
        {
            return softSymbol > HALF_PI ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        }
        else
        {
            return softSymbol < -HALF_PI ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }
}
