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

package io.github.dsheirer.module.decode.squelchDecoder.dcs;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Digital Coded Squelch (DCS) demodulator/decoder designed to work with FM demodulated 8 kHz input audio samples and
 * decode 134.4-baud signalling present in the unfiltered demodulated audio stream.
 *
 * This decoder monitors the incoming sample stream looking for either a positive or negative sequence of samples where
 * the slope of these values exceeds a threshold and causes the symbol state to flip.  The symbol state remains across
 * successive baud periods until the next opposing slope state change sequence is detected.
 *
 * This decoder uses a slope detector to decode the 134.4 bits per second stream and identify the DCS code.  It is
 * designed to work with 8 kHz audio sample rate.  Each baud period is (8,000 / 134.4) = 59.52 samples per symbol and
 * a full DCS tone sequence is 23 bits * 59.52 samples/bit = 1369 samples.  The DCS tone sequence is continuously
 * transmitted at a rate of 134.4 bits-per-second / 23 = 5.84 repeats per second while the source continues transmitting.
 *
 * This decoder was modified to optionally run in inlineMode, when it is directly called from NBFM decoder for
 * channel level DCS detection and muting. This determination is made during instantiation.
 *
 */
public class DCSDecoder extends Decoder implements IRealBufferListener, Listener<float[]>
{
    private static final Logger mLog = LoggerFactory.getLogger(DCSDecoder.class);
    //Slope calculation sumXX value is always the same for 59 evenly spaced samples
    private static final double SLOPE_CALCULATION_SUM_XX = 2247.5;
    private static final float BAUD_LENGTH = 8000.0f / 134.4f;
    private static final float SLOPE_THRESHOLD = 0.002750f;
    private static final int MAX_ONES_SEQUENCE = 6;
    private static final int OVERLAP = (int)Math.ceil(BAUD_LENGTH);
    private static final int POST_TRANSITION_SAMPLES_TO_SKIP = 30;
    private static final int SLOPE_CALCULATION_PERIOD = 30;
    private static final int IDEAL_SYMBOL_TRANSITION_MIN = 11;
    private static final int IDEAL_SYMBOL_TRANSITION_MAX = 19;
    private static final int CODE_MASK = 0x7FFFFF; //23-bit mask
    private static final int LOSS_CODEWORDS = 4;
    private static final int CONFIRMATION_COUNT = 2;

    private static float[] sLowPassFilterCoefficients;
    private boolean mSymbol = false;
    private float mBaudCounter = 0f;
    private float mMaxSlope = 0;
    private float[] mResidual = new float[OVERLAP];
    private int mCode = 0;
    private int mExcessiveOneSequenceCounter = 0;
    private boolean mInlineMode = true;
    private Set<DCSCode> mTargetCodes = null;
    private int mConfirmationCounter = 0;
    private int mCodewordsSinceMatch = 0;
    private DCSCode mConfiguredCode = null;
    private int mSamplesToSkip = 0;
    private DCSMessage mDCSMessage = null;
    private boolean mMuted = true;


    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(8000)
                .gridDensity(16)
                .oddLength(true)
                .passBandCutoff(200)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandStart(300)
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
    public DCSDecoder(Set<DCSCode> targetCodes)
    {
        if(targetCodes != null && !targetCodes.isEmpty())
        {
            DCSCode configuredCode = targetCodes.iterator().next();
            mConfiguredCode = DCSCode.hasValue(configuredCode.getValue()) ? configuredCode : DCSCode.UNKNOWN;
        }
        mTargetCodes = targetCodes;
        mInlineMode = true;
    }

    public DCSDecoder()
    {
        mInlineMode = false;
    }

    /**
     * Decoder type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DCS;
    }

    /**
     * Implementation of the IRealBufferListener interface
     */
    @Override
    public Listener<float[]> getBufferListener()
    {
        return this::receive;
    }
    /**
     * Processes the demodulated 8 kHz audio samples to extract the 300-baud LTR signalling and delivers the decoded
     * symbol array to the registered listener.
     * @param samples to demodulate.
     */
    public void receive(float[] samples)
    {
        process(samples);
    }

    public DCSMessage process(float[] samples)
    {
        if(!mInlineMode)
        {
            mDCSMessage = new DCSMessage();
        }
        else
        {
            mDCSMessage = new DCSMessage(mConfiguredCode);
        }
        if(getMessageListener() != null)
        {
            float[] filtered = mLowPassFilter.filter(samples);
            float[] buffer = new float[filtered.length + OVERLAP];
            int timingAdjust = 0;
            float slope = 0;

            try
            {
                System.arraycopy(mResidual, 0, buffer, 0, mResidual.length);
                System.arraycopy(filtered, 0, buffer, mResidual.length, filtered.length);
                System.arraycopy(filtered, filtered.length - OVERLAP, mResidual, 0, OVERLAP);

                for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer++)
                {
                    //Don't calculate slope if we're within the baud period following the last symbol transition
                    if(mSamplesToSkip > 0)
                    {
                        mSamplesToSkip--;
                    }
                    else
                    {
                        slope = calculateSlope(buffer, bufferPointer);

                        if(mSymbol)
                        {
                            //Look for crest of slope at lowest point below the negative slope threshold for transition
                            if(slope > mMaxSlope && mMaxSlope < -SLOPE_THRESHOLD)
                            {
                                mSymbol = false;

                                //Coarse symbol timing adjust - ideal toggle is middle of baud, between 11 & 19 samples
                                if(mBaudCounter < IDEAL_SYMBOL_TRANSITION_MIN)
                                {
                                    timingAdjust = 1;
                                }
                                else if(mBaudCounter > IDEAL_SYMBOL_TRANSITION_MAX)
                                {
                                    timingAdjust = -1;
                                }
                                else
                                {
                                    timingAdjust = 0;
                                }

                                mBaudCounter += timingAdjust;
                                mSamplesToSkip = POST_TRANSITION_SAMPLES_TO_SKIP + timingAdjust;
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

                                //Coarse symbol timing adjust - ideal toggle is middle of baud, between 11 & 19 samples
                                if(mBaudCounter < IDEAL_SYMBOL_TRANSITION_MIN)
                                {
                                    timingAdjust = 1;
                                }
                                else if(mBaudCounter > IDEAL_SYMBOL_TRANSITION_MAX)
                                {
                                    timingAdjust = -1;
                                }
                                else
                                {
                                    timingAdjust = 0;
                                }

                                mBaudCounter += timingAdjust;
                                mSamplesToSkip = POST_TRANSITION_SAMPLES_TO_SKIP + timingAdjust;
                            }
                            else if(slope > mMaxSlope)
                            {
                                mMaxSlope = slope;
                            }
                        }
                    }

                    mBaudCounter++;

                    //Update the current code value once we've processed a baud/symbol period of samples
                    if(mBaudCounter > BAUD_LENGTH)
                    {
                        //Update the code value adding in the current symbol
                        mCode = (Integer.rotateLeft(mCode, 1) + (mSymbol ? 1 : 0)) & CODE_MASK;
                        if(!mInlineMode)
                        {
                            if(DCSCode.hasValue(mCode))
                            {
                                mDCSMessage.setDCSCode(DCSCode.fromValue(mCode));
                                getMessageListener().receive(mDCSMessage);
                            }
                        }
                        else
                        {
                            if(DCSCode.hasValue(mCode))
                            {
                                detectionLogicTree(DCSCode.fromValue(mCode));
                            }
                            else
                            {
                                detectionLogicTree(null);
                            }

                        }

                        mBaudCounter -= BAUD_LENGTH;

                        //Check for a continuous sequence of ones that exceeds the max (6) for any DCS code and
                        //flip/revert the symbol state back to zero.
                        if(mSymbol)
                        {
                            mExcessiveOneSequenceCounter++;

                            if(mExcessiveOneSequenceCounter > MAX_ONES_SEQUENCE)
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
                mLog.warn("Unexpected error while processing DCS samples", e);
            }
        }
        return mDCSMessage;
    }

    /**
     * Calculates the slope of the 30 samples in the values array starting at the specified offset.  The baud period
     * is slightly more than 59 samples, however the voltage state transition occurs over a 30-sample period.
     *
     * Note: code modified from Apache Commons Math library, SimpleRegression class.
     * @param samples to load from
     * @param offset to the sample start for the slope calculation
     */
    public float calculateSlope(float[] samples, int offset)
    {
        /** mean of accumulated x & y values, used in updating formulas */
        double xbar = 0;
        double ybar = samples[offset];

//        double sumXX = 0;
        double sumXY = 0;
        double fact1, dx, dy;

        for(int x = 1; x < SLOPE_CALCULATION_PERIOD; x++)
        {
            fact1 = 1.0 + x;
//            final double fact2 = x / (1.0 + x);
            dx = x - xbar;
            dy = samples[offset + x] - ybar;
//            sumXX += dx * dx * fact2;
            sumXY += dx * dy * (x / (1.0 + x));
            xbar += dx / fact1;
            ybar += dy / fact1;
        }

        //Transfer the sumXX value to SLOPE_CALCULATION_SUM_XX since it's a constant value
//        mLog.info("SumXX: " + sumXX);

        return (float)(sumXY / SLOPE_CALCULATION_SUM_XX);
    }

    public void reset()
    {
        mSymbol = false;
        mBaudCounter = 0;
        mMaxSlope = 0;
        mCode = 0;
        mExcessiveOneSequenceCounter = 0;
        mSamplesToSkip = 0;
        mConfirmationCounter = 0;
        mCodewordsSinceMatch = 0;
        mResidual = new float[OVERLAP];
    }

    /**
     * Called when noiseSquelch closes in inlineMode
     * @return DCSMessage
     */
    public DCSMessage inlineReset()
    {
        reset();
        mMuted = true;
        DCSMessage message = new DCSMessage(mConfiguredCode);
        message.setMessage("Noise squelch closed.");
        message.setDCSCode(null);
        message.setMutedStatus(true);
        message.setCallEvent(DecoderStateEvent.Event.END);
        message.setCodeState(DCSMessage.SquelchCodeState.LOST);
        return message;
    }
    /**
     * Handles detection logic of a decoded DCS code in inlineMode
     * @param newCode currently detected DCS code, or null if no code detected
     */
    private void detectionLogicTree(DCSCode newCode)
    {
        if (mMuted)
        {
            if (newCode != null && mTargetCodes.contains(newCode))
            {
                if (mConfirmationCounter >= CONFIRMATION_COUNT)
                {
                    // unmute and report detected code
                    mMuted = false;
                    mDCSMessage.setMutedStatus(false);
                    mDCSMessage.setDCSCode(newCode);
                    mDCSMessage.setMessage("Correct DCS code detected, confirmation passed, now unmuting.");
                    mDCSMessage.setCallEvent(DecoderStateEvent.Event.START);
                    mDCSMessage.setCodeState(DCSMessage.SquelchCodeState.ACCEPTED);
                    mCodewordsSinceMatch = 0;
                }
                else
                {
                    // increment counter and wait for next detection
                    mConfirmationCounter++;
                    mDCSMessage.setDCSCode(newCode);
                    mDCSMessage.setMessage("Waiting for consecutive correct detections.");
                }
            }
            else    // wrong tone or no tone
            {
                mConfirmationCounter = 0;
                mDCSMessage.setMutedStatus(true);
                mDCSMessage.setDCSCode(newCode);
                // no other state changes at this time.
            }
        }
        else    // unmuted and call is ongoing
        {
            if (newCode != null && mTargetCodes.contains(newCode))
            {
                // all is good, call continues
                mCodewordsSinceMatch = 0;
                mDCSMessage.setMutedStatus(false);
                mDCSMessage.setDCSCode(newCode);
                mDCSMessage.setCallEvent(DecoderStateEvent.Event.CONTINUATION);
            }
            else
            {
                if (mCodewordsSinceMatch >= LOSS_CODEWORDS)
                {
                    // Mute audio and report rejected code or lost tone
                    mMuted = true;
                    mDCSMessage.setMutedStatus(true);
                    mDCSMessage.setDCSCode(newCode);
                    mDCSMessage.setMessage("Incorrect DCS code or code lost, now muting.");
                    mDCSMessage.setCallEvent(DecoderStateEvent.Event.END);
                    mConfirmationCounter = 0;
                    if(newCode != null)
                    {
                        mDCSMessage.setCodeState(DCSMessage.SquelchCodeState.REJECTED);
                    }
                    else
                    {
                        mDCSMessage.setCodeState(DCSMessage.SquelchCodeState.LOST);
                    }
                }
                else
                {
                    // all is still good with the call, but a bad tone was decoded and the closeCounter hasn't
                    //  reached a threshold yet.
                    mCodewordsSinceMatch++;
                    mDCSMessage.setMutedStatus(false);
                    mDCSMessage.setDCSCode(newCode);
                    mDCSMessage.setMessage("Waiting for consecutive incorrect or no code.");
                    mDCSMessage.setCallEvent(DecoderStateEvent.Event.CONTINUATION);
                }
            }
        }
    }
}
