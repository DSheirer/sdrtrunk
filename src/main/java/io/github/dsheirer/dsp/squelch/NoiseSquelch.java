/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.squelch;

import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.sample.Listener;
import java.util.Arrays;

/**
 * Squelch processor for unfiltered, demodulated audio that operators by high-pass filtering the audio to remove below
 * 3,000 Hertz and processes 10-millisecond audio buffers to calculate the noise variance across the samples.  This
 * variance is compared to a noise threshold and uses time hysteresis in the form of consecutive buffers to toggle the
 * squelch open or closed.  Uses a delay buffer equal in length to the hysteresis plus two which enables locating the
 * precise transition point of squelch/un-squelch and passing the audio of the detected signal while blocking the audio
 * when there is no signal detected. Register an audio listener to receive squelch controlled audio and a squelch state
 * listener to receive squelch toggle indications.
 */
public class NoiseSquelch implements INoiseSquelchController
{
    public static final int DEFAULT_HYSTERESIS_OPEN_THRESHOLD = 4;
    public static final int DEFAULT_HYSTERESIS_CLOSE_THRESHOLD = 6;
    public static final int MINIMUM_HYSTERESIS_THRESHOLD = 1;
    public static final int MAXIMUM_HYSTERESIS_THRESHOLD = 10;
    public static final int VARIANCE_CALCULATION_WINDOW_MILLISECONDS = 10;
    public static final float DEFAULT_NOISE_OPEN_THRESHOLD = 0.1f;
    public static final float DEFAULT_NOISE_CLOSE_THRESHOLD = 0.19f;
    public static final float MINIMUM_NOISE_THRESHOLD = 0.1f;
    public static final float MAXIMUM_NOISE_THRESHOLD = 0.5f;
    private float[] mFilteredBuffer = new float[0];
    private float[] mAudioBuffer = new float[0];
    private float mMeanAccumulator;
    private float mNoiseOpenThreshold = DEFAULT_NOISE_OPEN_THRESHOLD;
    private float mNoiseCloseThreshold = DEFAULT_NOISE_CLOSE_THRESHOLD;
    private boolean mSquelch = true;
    private boolean mSquelchOverride = false;
    private int mMeanAccumulatorPointer;
    private int mVarianceWindowSize;
    private int mHysteresisOpenThreshold = DEFAULT_HYSTERESIS_OPEN_THRESHOLD;
    private int mHysteresisCloseThreshold = DEFAULT_HYSTERESIS_CLOSE_THRESHOLD;
    private int mHysteresisCount = 0;
    private int mSquelchStateBroadcastCounter = 0;
    private int mSquelchOpenIndex = 0;
    private int mAudioBufferFilterDelay;
    private IRealFilter mHighPassFilter;
    private Listener<float[]> mAudioListener;
    private Listener<SquelchState> mSquelchStateListener;
    private Listener<NoiseSquelchState> mNoiseSquelchStateListener;

    /**
     * Constructs an instance
     * @param noiseOpen in range 0.0 to 1.0 - recommend 0.1 to 0.2 (units of demodulated, high-pass filtered audio
     * sample variance where the high-pass filtered audio samples range -1.0 to 1.0 and maximum variance is half of that
     * range: 1.0)
     * @param hysteresisOpen in range 1 to 10 controls the squelch toggle hysteresis - recommend 4 (units of 10 ms)
     */
    public NoiseSquelch(float noiseOpen, float noiseClose, int hysteresisOpen, int hysteresisClose)
    {
        setNoiseThreshold(noiseOpen, noiseClose);
        setHysteresisThreshold(hysteresisOpen, hysteresisClose);
    }

    /**
     * Indicates if the current state is squelched.
     * @return true if squelched and false if not squelched.
     */
    public boolean isSquelched()
    {
        return mSquelch;
    }

    /**
     * Sets the manual squelch override.
     * @param override (true) or (false) to turn off manual squelch override.
     */
    @Override
    public void setSquelchOverride(boolean override)
    {
        mSquelchOverride = override;

        if(mSquelchOverride)
        {
            broadcast(SquelchState.UNSQUELCH);
        }
    }

    /**
     * Sets the noise thresholds
     * @param open in range 0.0-1.0 and less than or equal to close, recommend 0.1
     * @param close in range 0.0-1.0 and greater than or equal to open, recommend 0.2
     */
    public void setNoiseThreshold(float open, float close)
    {
        if(open < MINIMUM_NOISE_THRESHOLD || close > MAXIMUM_NOISE_THRESHOLD || open > close)
        {
            throw new IllegalArgumentException("Noise thresholds open/close [" + open + "/" + close + "] - open must be less than or equal to close and both in range " + MINIMUM_NOISE_THRESHOLD + "-" + MAXIMUM_NOISE_THRESHOLD);
        }

        mNoiseOpenThreshold = open;
        mNoiseCloseThreshold = close;
    }

    /**
     * Updates the hysteresis thresholds
     * @param open in range 1 to 10 and less than or equal to close, recommend 4.
     * @param close in range 1 to 10 and greater than or equal to open, recommend 6.
     */
    public void setHysteresisThreshold(int open, int close)
    {
        if(open < MINIMUM_HYSTERESIS_THRESHOLD || close > MAXIMUM_HYSTERESIS_THRESHOLD || open > close)
        {
            throw new IllegalArgumentException("Hysteresis threshold open [" + open + "] must be less than or equal to close [" +
                    close + "] and both in range " + MINIMUM_HYSTERESIS_THRESHOLD + "-" + MAXIMUM_HYSTERESIS_THRESHOLD);
        }

        mHysteresisOpenThreshold = open;
        mHysteresisCloseThreshold = close;
        mHysteresisCount = Math.min(mHysteresisCloseThreshold, mHysteresisCount);
    }

    /**
     * Registers the listener to receive noise squelch state updates every 100 milliseconds.
     * @param listener to register, or de-register by passing in a null.
     */
    public void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener)
    {
        mNoiseSquelchStateListener = listener;
    }

    /**
     * Registers the listener to receive squelch state indications.  Squelch state events will be sent to the listener
     * in concert with processed audio buffers.  An un-squelch event is followed by one or more audio buffers and then a
     * squelch event.
     * @param listener to register or de-register by passing in a null.
     */
    public void setSquelchStateListener(Listener<SquelchState> listener)
    {
        mSquelchStateListener = listener;
    }

    /**
     * Registers the listener to receive processed audio buffers in between un-squelch/squelch events.
     * @param listener to register or de-register by passing in a null.
     */
    public void setAudioListener(Listener<float[]> listener)
    {
        mAudioListener = listener;
    }

    /**
     * Broadcasts the current noise squelch state to an optionally registered listener.  This can be use used in the
     * user interface to display the state of the channel and noise squelch activity.
     * @param noise variance from the current 10-millisecond audio buffer.
     */
    private void broadcastNoiseSquelchState(float noise)
    {
        if(mNoiseSquelchStateListener != null)
        {
            mNoiseSquelchStateListener.receive(new NoiseSquelchState(mSquelch, mSquelchOverride, noise,
                    mNoiseOpenThreshold, mNoiseCloseThreshold, mHysteresisCount, mHysteresisOpenThreshold,
                    mHysteresisCloseThreshold));
        }
    }

    /**
     * Broadcasts from the unfiltered audio buffer to an optionally registered audio listener.  Note: the start and end
     * indices are pointers to the filtered audio buffer, and we dispatch from the unfiltered audio buffer which is offset
     * by half the length of the high-pass audio filter coefficients.  This method adjusts the index arguments by this
     * offset value to align the unfiltered audio with the high-pass filtered audio detection boundaries.
     *
     * @param start index of the filtered audio buffer
     * @param end index of the filtered audio buffer
     */
    private void broadcast(int start, int end)
    {
        if(mAudioListener != null)
        {
            mAudioListener.receive(Arrays.copyOfRange(mAudioBuffer, start, end));
        }
    }

    /**
     * Broadcasts the squelch state change to an optionally registered squelch state listener.
     * @param squelchState to dispatch.
     */
    private void broadcast(SquelchState squelchState)
    {
        //Override squelch state when we're in manual override
        if(squelchState == SquelchState.SQUELCH && mSquelchOverride)
        {
            squelchState = SquelchState.UNSQUELCH;
        }

        if(mSquelchStateListener != null)
        {
            mSquelchStateListener.receive(squelchState);
        }
    }

    /**
     * Sets the sample rate for the incoming demodulated audio sample stream.
     * @param sampleRate of the sample stream.
     */
    public void setSampleRate(double sampleRate)
    {
        float[] coefficients = FilterFactory.getHighPass((int)sampleRate, 3000, 31, WindowType.BLACKMAN_HARRIS_7);
        mHighPassFilter = new RealFIRFilter(coefficients);
        mVarianceWindowSize = (int)(sampleRate * (VARIANCE_CALCULATION_WINDOW_MILLISECONDS / 1000.0));

        //Offset for the filter delay between the audio buffer and the filtered audio noise detection buffer.
        mAudioBufferFilterDelay = (int)Math.ceil(coefficients.length / 2.0);
    }

    public void process(float[] samples)
    {
        float[] filtered = mHighPassFilter.filter(samples);

        int requiredLength = filtered.length + (mVarianceWindowSize * (mHysteresisCloseThreshold + 2));

        //Resize the delay buffers if the incoming sample buffer size changes.
        if(mFilteredBuffer.length != requiredLength)
        {
            mFilteredBuffer = Arrays.copyOf(mFilteredBuffer, requiredLength);

            //The audio buffer is longer to account for filter delay of the high pass filter and to align the indices
            //across the filtered and audio buffers.
            mAudioBuffer = Arrays.copyOf(mAudioBuffer, requiredLength + mAudioBufferFilterDelay);
        }

        //Shuffle the samples to the left in the delay buffers and copy in the new samples.
        int length = mFilteredBuffer.length - filtered.length;
        System.arraycopy(mFilteredBuffer, filtered.length, mFilteredBuffer, 0, length);
        System.arraycopy(filtered, 0, mFilteredBuffer, length, filtered.length);

        length = mAudioBuffer.length - samples.length;
        System.arraycopy(mAudioBuffer, samples.length, mAudioBuffer, 0, length);
        System.arraycopy(samples, 0, mAudioBuffer, length, samples.length);

        int squelchCloseIndex;
        //Adjust the squelch open index in sync with the left-shuffle of the audio samples.

        mSquelchOpenIndex = Math.max(mSquelchOpenIndex - samples.length, 0);

        //Process the newly added (filtered) samples in the delay buffer.
        for(int x = mFilteredBuffer.length - filtered.length; x < mFilteredBuffer.length; x++)
        {
            mMeanAccumulator += mFilteredBuffer[x];
            mMeanAccumulatorPointer++;

            if(mMeanAccumulatorPointer == mVarianceWindowSize)
            {
                float mean = mMeanAccumulator / mVarianceWindowSize;

                //Reset averaging variables
                mMeanAccumulator = 0.0f;
                mMeanAccumulatorPointer = 0;

                //Calculate the variance
                float varianceAccumulator1 = 0.0f, deviation, varianceAccumulator2 = 0.0f;
                for(int i = x - mVarianceWindowSize + 1; i <= x; i++)
                {
                    deviation = mFilteredBuffer[i] - mean;
                    varianceAccumulator1 += deviation * deviation;
                    varianceAccumulator2 += deviation;
                }

                //Formula from Apache Commons Math - Variance class.
                float noiseVariance = (varianceAccumulator1 - (varianceAccumulator2 * varianceAccumulator2 / mVarianceWindowSize)) / mVarianceWindowSize;
                boolean below = mSquelch ? (noiseVariance < mNoiseOpenThreshold) : (noiseVariance < mNoiseCloseThreshold);
                mHysteresisCount += (below ? 1 : -1);

                if(mSquelch && mHysteresisCount >= mHysteresisOpenThreshold)
                {
                    mSquelch = false;
                    mHysteresisCount = mHysteresisOpenThreshold;
                    mSquelchOpenIndex = findTransition(x);

                    if(!mSquelchOverride)
                    {
                        broadcast(SquelchState.UNSQUELCH);
                    }
                }
                else if(!mSquelch && mHysteresisCount <= 0)
                {
                    mSquelch = true;
                    mHysteresisCount = 0;
                    squelchCloseIndex = findTransition(x);

                    //Broadcast the partial audio segment ending at the transition point.
                    if(mSquelchOpenIndex < squelchCloseIndex)
                    {
                        //Only broadcast the truncated audio samples when we're not in squelch override
                        if(!mSquelchOverride)
                        {
                            broadcast(mSquelchOpenIndex, squelchCloseIndex);
                        }

                        broadcast(SquelchState.SQUELCH);
                    }

                    mSquelchOpenIndex = 0;
                }
                else if(mSquelch && !mSquelchOverride)
                {
                    int start = x - (mVarianceWindowSize * mHysteresisOpenThreshold) + 1;
                    int end = start + mVarianceWindowSize;
                    //Make sure start doesn't go negative
                    start = Math.max(0, start);
                    Arrays.fill(mAudioBuffer, start, end, 0.0f);
                }

                mHysteresisCount = Math.min(mHysteresisCount, mHysteresisCloseThreshold);
                mHysteresisCount = Math.max(mHysteresisCount, 0);

                //Broadcast squelch state to an optionally registered listener (the channel tab in user interface)
                mSquelchStateBroadcastCounter++;
                if(mSquelchStateBroadcastCounter >= 5)
                {
                    broadcastNoiseSquelchState(noiseVariance);
                    mSquelchStateBroadcastCounter -= 5;
                }
            }
        }

        if(mSquelchOverride)
        {
            broadcast(0, samples.length);
        }
        else if(!mSquelch && mSquelchOpenIndex < (samples.length))
        {
            mSquelchOpenIndex = Math.max(mSquelchOpenIndex, 0);
            //Dispatch audio after processing the audio buffer if we end in an un-squelch state.
            broadcast(mSquelchOpenIndex, samples.length);
            mSquelchOpenIndex = 0;
        }
    }

    /**
     * Identifies the squelch transition point across a hysteresis window delayed by 2x buffer periods from the current
     * processing index.  This enables fine-grained squelch control without losing audio during the un-squelch period or
     * allowing noise to leak into the squelch period at the end of the transmission. Uses a reduced 20-sample variance
     * calculation window extended to the hysteresis time period to detect the transition point.  Identifies the
     * transition point within +/- 20 audio samples.
     *
     * @param current buffer processing index.
     * @return buffer index of detected transition.
     */
    private int findTransition(int current)
    {
        //Search window is 10-ms buffer size extended to hysteresis period, delayed by 2x buffer lengths.
        int window = mSquelch ? mHysteresisCloseThreshold : mHysteresisOpenThreshold;

        //Thresholds here may seem reversed but keep in mind that the mSquelch state has already been flipped and thus
        // represents the state moving forward, and we need to detect the transition from the previous mSquelch state.
        float varianceThreshold = mSquelch ? mNoiseCloseThreshold : mNoiseOpenThreshold;
        int hysteresisThreshold = mSquelch ? mHysteresisCloseThreshold : mHysteresisOpenThreshold;

        //Look back window start and end indices are sized according the previous mSquelch state
        int start = current - (mVarianceWindowSize * (window + 2));
        int end = start + (mVarianceWindowSize * window);

        int hysteresisCount = 0;
        int windowSize = mVarianceWindowSize / 5; //Decrease transition variance window to 1/5th of the normal variance window
        int transitionPointer = 0;

        for(int x = start; x < end; x += windowSize)
        {
            float mean = getMean(x, x + windowSize);
            float variance = getVariance(x, x + windowSize, mean);

            //Detect transition from from un-squelch to squelch
            boolean exceedsThreshold = (mSquelch ? (variance > varianceThreshold) : (variance < varianceThreshold));

            if(exceedsThreshold)
            {
                if(hysteresisCount == 0)
                {
                    hysteresisCount++;
                    transitionPointer = x;
                }
                else
                {
                    hysteresisCount++;

                    if(hysteresisCount >= hysteresisThreshold)
                    {
                        return transitionPointer;
                    }
                }
            }
            else
            {
                hysteresisCount--;
                hysteresisCount = Math.max(0, hysteresisCount);
            }
        }

        return 0;
    }

    /**
     * Calculate the mean/average of values between the start and end indices.
     * @param start index
     * @param end index
     * @return mean value.
     */
    private float getMean(int start, int end)
    {
        if(start < 0)
        {
            start = 0;
        }

        if(end > mFilteredBuffer.length)
        {
            end = mFilteredBuffer.length;
        }

        float accumulator = 0.0f;

        for(int x = start; x < end; x++)
        {
            accumulator += mFilteredBuffer[x];
        }

        return accumulator / (end - start);
    }

    /**
     * Calculates the variance of the values between start and end indices using the supplied mean value.
     * @param start index
     * @param end index
     * @param mean of values between start and end, calculated previously
     * @return variance.
     */
    private float getVariance(int start, int end, float mean)
    {
        if(start < 0)
        {
            start = 0;
        }

        if(end > mFilteredBuffer.length)
        {
            end = mFilteredBuffer.length;
        }

        float varianceAccumulator1 = 0.0f, deviation, varianceAccumulator2 = 0.0f;

        for(int i = start; i < end; i++)
        {
            deviation = mFilteredBuffer[i] - mean;
            varianceAccumulator1 += deviation * deviation;
            varianceAccumulator2 += deviation;
        }

        int sampleSize = end - start;

        //Formula from Apache Commons Math - Variance class.
        return (varianceAccumulator1 - (varianceAccumulator2 * varianceAccumulator2 / sampleSize)) / sampleSize;
    }

    public static void main(String[] args)
    {
//        Path directory = Paths.get("/media/denny/T9/NBFM Squelch Research"); //Linux
//        Path file = directory.resolve("20250629_072704_453850000_Onondaga-County_Fire-EMS_OC-Fire-Disp_79_baseband.wav");
//        Path file = directory.resolve("DMR_1_CAPPLUS.wav");
//        Path file = directory.resolve("20250702_034109_145510000_R828D_V4_POLY_No_Antenna_Normal_Gain.wav");
//        Path file = directory.resolve("20250702_034301_145510000_R828D_V4_POLY_No_Antenna_Max_Gain.wav");
//        Path file = directory.resolve("20250702_034403_145510000_R828D_V4_POLY_No_Antenna_Min_Gain.wav");
//        Path file = directory.resolve("20250702_034541_145510000_R828D_V4_HET_No_Antenna_Min_Gain.wav");
//        Path file = directory.resolve("20250702_034635_145510000_R828D_V4_HET_No_Antenna_Max_Gain.wav");
//        Path file = directory.resolve("20250702_034742_145510000_R828D_V4_HET_No_Antenna_Normal_Gain.wav");
//        Path file = directory.resolve("20250703_043912_162500000_NOAA_25_khz_no_center_signal.wav");
//        Path file = directory.resolve("20250703_043958_162550000_NOAA_25_khz_yes_center_signal.wav");
//
//        DecodeConfigNBFM config = new DecodeConfigNBFM();
//        config.setBandwidth(DecodeConfigAnalog.Bandwidth.BW_12_5);
//        NBFMDecoder decoder = new NBFMDecoder(config);
//
//        try(ComplexWaveSource source = new ComplexWaveSource(file.toFile(), false))
//        {
//            source.setListener(iNativeBuffer -> {
//                Iterator<ComplexSamples> it = iNativeBuffer.iterator();
//
//                while(it.hasNext())
//                {
//                    ComplexSamples samples = it.next();
//                    decoder.receive(samples);
//                }
//            });
//            source.start();
//
//            decoder.getSourceEventListener().receive(SourceEvent.sampleRateChange(source.getSampleRate()));
//
//            while(true)
//            {
//                source.next(2048, true);
//            }
//        }
//        catch(IOException ioe)
//        {
//
//            ioe.printStackTrace();
//        }
//
//        System.out.println("Finished");
    }
}
