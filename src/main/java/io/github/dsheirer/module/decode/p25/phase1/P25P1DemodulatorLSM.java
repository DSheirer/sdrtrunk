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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demodulates filtered LSM I/Q samples to feed message framer for sync detection and framing.
 *
 * Includes carrier frequency pre-correction to compensate for frequency offset introduced by the
 * polyphase channelizer.  The channelizer output typically has a carrier offset of several hundred
 * Hz which exceeds the PLL's tracking range (MAX_PLL = +/- 60 degrees), causing the PLL and Gardner
 * timing error detector to converge to an incorrect operating point.  The carrier pre-correction
 * estimates the offset from the first 5000 I/Q samples and applies a numerically controlled oscillator
 * (NCO) to remove the offset before demodulation.
 */
public class P25P1DemodulatorLSM
{
    private static final Logger mLog = LoggerFactory.getLogger(P25P1DemodulatorLSM.class);
    private static final float HALF_PI = (float)(Math.PI / 2.0);
    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private static final float MAX_PLL = (float)(Math.PI / 3.0); //+/- 800 Hz
    private static final float OBJECTIVE_MAGNITUDE = 1.0f;
    private static final int SYMBOL_RATE = 4800;
    private static final int CARRIER_ESTIMATE_SAMPLES = 5000;

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

    //Carrier frequency pre-correction NCO state
    private float mCarrierPhaseIncrement = 0f;
    private float mCarrierPhase = 0f;
    private boolean mCarrierEstimated = false;
    private float[] mCarrierEstI;
    private float[] mCarrierEstQ;
    private int mCarrierEstCount = 0;

    /**
     * Constructs an instance
     * @param messageFramer for receiving demodulated symbol stream and providing sync detection events.
     * @param feedbackDecoder (parent) for receiving PLL carrier offset error reports.
     */
    public P25P1DemodulatorLSM(P25P1MessageFramer messageFramer, FeedbackDecoder feedbackDecoder)
    {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
        mCarrierEstI = new float[CARRIER_ESTIMATE_SAMPLES];
        mCarrierEstQ = new float[CARRIER_ESTIMATE_SAMPLES];
    }

    /**
     * Reset the PLL when the tuner source either changes center frequency or adjust the PPM value.
     */
    public void resetPLL()
    {
        mPLL = 0f;
        mCarrierEstimated = false;
        mCarrierEstCount = 0;
        mCarrierPhaseIncrement = 0f;
        mCarrierPhase = 0f;
        mCarrierEstI = new float[CARRIER_ESTIMATE_SAMPLES];
        mCarrierEstQ = new float[CARRIER_ESTIMATE_SAMPLES];
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
     * Estimates the carrier frequency offset from collected I/Q samples and configures the NCO
     * to pre-correct subsequent samples.  Uses the average instantaneous frequency measured from
     * the argument of consecutive sample products: freq = mean(angle(z[n] * conj(z[n-1]))).
     */
    private void estimateCarrier()
    {
        double sumAngle = 0;

        for(int n = 1; n < mCarrierEstCount; n++)
        {
            //Compute z[n] * conj(z[n-1])
            float prodI = mCarrierEstI[n] * mCarrierEstI[n - 1] + mCarrierEstQ[n] * mCarrierEstQ[n - 1];
            float prodQ = mCarrierEstQ[n] * mCarrierEstI[n - 1] - mCarrierEstI[n] * mCarrierEstQ[n - 1];
            sumAngle += Math.atan2(prodQ, prodI);
        }

        mCarrierPhaseIncrement = (float)(sumAngle / (mCarrierEstCount - 1));
        mCarrierPhase = 0f;
        mCarrierEstimated = true;

        double sampleRate = SYMBOL_RATE * mSamplesPerSymbol;
        double carrierHz = mCarrierPhaseIncrement * sampleRate / (2.0 * Math.PI);
        mLog.info("Carrier offset estimate: {} Hz ({} rad/sample at {} Hz sample rate)",
            String.format("%.1f", carrierHz), String.format("%.6f", mCarrierPhaseIncrement),
            String.format("%.0f", sampleRate));

        //Release estimation buffers
        mCarrierEstI = null;
        mCarrierEstQ = null;
    }

    /**
     * Primary input method for receiving a stream of filtered, pulse-shaped samples to process into symbols.
     * Collects initial samples for carrier offset estimation, then applies NCO pre-correction before demodulation.
     * @param i inphase samples to process
     * @param q quadrature samples to process
     */
    public void process(float[] i, float[] q)
    {
        //Collect samples for carrier estimation during startup
        if(!mCarrierEstimated)
        {
            int toCopy = Math.min(i.length, CARRIER_ESTIMATE_SAMPLES - mCarrierEstCount);

            if(toCopy > 0)
            {
                System.arraycopy(i, 0, mCarrierEstI, mCarrierEstCount, toCopy);
                System.arraycopy(q, 0, mCarrierEstQ, mCarrierEstCount, toCopy);
                mCarrierEstCount += toCopy;
            }

            if(mCarrierEstCount >= CARRIER_ESTIMATE_SAMPLES)
            {
                estimateCarrier();
            }
            else
            {
                return; //Not enough samples yet for carrier estimation
            }
        }

        //Apply carrier frequency pre-correction via NCO rotation
        float[] iCorrected = new float[i.length];
        float[] qCorrected = new float[q.length];

        for(int n = 0; n < i.length; n++)
        {
            float cosPhase = (float)Math.cos(mCarrierPhase);
            float sinPhase = (float)Math.sin(mCarrierPhase);
            iCorrected[n] = i[n] * cosPhase + q[n] * sinPhase;
            qCorrected[n] = q[n] * cosPhase - i[n] * sinPhase;
            mCarrierPhase += mCarrierPhaseIncrement;

            //Keep phase in [-PI, PI] range
            if(mCarrierPhase > (float)Math.PI)
            {
                mCarrierPhase -= TWO_PI;
            }
            else if(mCarrierPhase < -(float)Math.PI)
            {
                mCarrierPhase += TWO_PI;
            }
        }

        //Continue with carrier-corrected samples
        processDemodulation(iCorrected, qCorrected);
    }

    /**
     * Performs the actual demodulation of carrier-corrected I/Q samples.
     */
    private void processDemodulation(float[] i, float[] q)
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
