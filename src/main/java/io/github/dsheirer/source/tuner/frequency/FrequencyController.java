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
package io.github.dsheirer.source.tuner.frequency;

import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.InvalidFrequencyException;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrequencyController
{
    private final static Logger mLog = LoggerFactory.getLogger(FrequencyController.class);

    private Tunable mTunable;
    private long mFrequency = 101100000;
    private long mTunedFrequency = 101100000;
    private long mMinimumFrequency = 0;
    private long mMaximumFrequency = 0;
    private double mFrequencyCorrection = 0.0d;
    private double mSampleRate = 0.0d;
    private boolean mSampleRateLocked = false;
    private List<ISourceEventProcessor> mProcessors = new ArrayList<>();

    /**
     * Constructs an instance
     * @param tunable that can be controlled by this frequency controller.
     */
    public FrequencyController(Tunable tunable)
    {
        mTunable = tunable;
    }

    /**
     * Prepare for disposal of this instance.
     */
    public void dispose()
    {
        mProcessors.clear();
        mTunable = null;
    }

    /**
     * Indicates if the sample rate for this frequency controller is locked by a remote process.  A locked state
     * indicates that the sample rate cannot be changed.  The remote process will typically be a downstream
     * consumer of the data produced by the device being controlled (ie polyphase channelizer) that does not want
     * the sample rate to be changed while processing (ie channels have been allocated) is ongoing.
     *
     * @return true if locked or false if not locked.
     */
    public boolean isSampleRateLocked()
    {
        return mSampleRateLocked;
    }

    /**
     * Sets the lock state for this frequency controller.  Set to true when a consumer process requires that the
     * sample rate control be locked so that only the single consumer process makes changes to the sample rate.  This
     * is primarily to lock down user interface controls when the polyphase channelizer is producing channels.
     *
     * @param sampleRateLocked state
     */
    public void setSampleRateLocked(boolean sampleRateLocked) throws SourceException
    {
        mSampleRateLocked = sampleRateLocked;

        if(mSampleRateLocked)
        {
            broadcast(SourceEvent.lockedSampleRateState());
        }
        else
        {
            broadcast(SourceEvent.unlockedSampleRateState());
        }
    }

    /**
     * Get bandwidth in hertz
     */
    public int getBandwidth()
    {
        return (int)mSampleRate;
    }

    /**
     * Set sample rate in hertz
     */
    public void setSampleRate(int sampleRate) throws SourceException
    {
        if(sampleRate != mSampleRate)
        {
            if(!mSampleRateLocked)
            {
                mSampleRate = sampleRate;

                broadcastSampleRateChange();
            }
            else
            {
                mLog.warn("Cannot change sample rate while tuner is is LOCKED state.");
            }
        }
    }

    /**
     * Get sample rate in hertz
     */
    public double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Indicates if the specified frequency can be tuned (ie is within min/max frequency) by this controller.
     *
     * @param frequency to evaluate
     * @return true if the frequency falls within the tuning range of this controller
     */
    public boolean canTune(long frequency)
    {
        return getMinimumFrequency() <= frequency && frequency <= getMaximumFrequency();
    }

    /**
     * Get frequency in hertz
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Set frequency
     *
     * @param frequency in hertz
     * @throws SourceException if tunable doesn't support tuning a corrected
     *                         version of the requested frequency
     */
    public void setFrequency(long frequency) throws SourceException
    {
        setFrequency(frequency, true);
    }

    /**
     * Set frequency with optional broadcast of frequency change event.  This
     * method supports changing the frequency correction value without
     * broadcasting a frequency change event.
     */
    private void setFrequency(long frequency, boolean broadcastChange) throws SourceException
    {
        if(frequency < mMinimumFrequency)
        {
            throw new InvalidFrequencyException("Frequency [" + frequency + "] is below the minimum [" +
                    mMinimumFrequency + "]", frequency, mMinimumFrequency);
        }

        if(frequency > mMaximumFrequency)
        {
            throw new InvalidFrequencyException("Frequency [" + frequency + "] is above the maximum [" +
                    mMaximumFrequency + "]", frequency, mMaximumFrequency);
        }

        mFrequency = frequency;
        mTunedFrequency = getTunedFrequency(frequency);

        if(mTunable != null)
        {
            mTunable.setTunedFrequency(mTunedFrequency);
        }

        /* Broadcast to all listeners that the frequency has changed */
        if(broadcastChange)
        {
            broadcastFrequencyChange();
        }
    }

    public long getTunedFrequency()
    {
        return mTunedFrequency;
    }

    /**
     * Minimum tunable frequency
     * @return minimum in Hertz
     */
    public long getMinimumFrequency()
    {
        return mMinimumFrequency;
    }

    /**
     * Sets the minimum tunable frequency
     * @param minimum in Hertz
     */
    public void setMinimumFrequency(long minimum)
    {
        mMinimumFrequency = minimum;
    }

    /**
     * Maximum tunable frequency
     * @return maximum frequency in Hertz
     */
    public long getMaximumFrequency()
    {
        return mMaximumFrequency;
    }

    /**
     * Sets the maximum tunable frequency
     * @param maximum in Hertz
     */
    public void setMaximumFrequency(long maximum)
    {
        mMaximumFrequency = maximum;
    }

    /**
     * Calculate the tuned frequency by adding frequency correction to the corrected frequency.
     * @param correctedFrequency
     */
    private long getTunedFrequency(long correctedFrequency)
    {
        return (long)((double)correctedFrequency / (1.0 + (mFrequencyCorrection / 1000000.0)));
    }

    /**
     * Calculate the corrected frequency by subtracting frequency correction
     * from the tuned frequency.
     *
     * @param tunedFrequency
     */
    @SuppressWarnings("unused")
    private long getCorrectedFrequency(long tunedFrequency)
    {
        return (long)((double)tunedFrequency /
            (1.0 - (mFrequencyCorrection / 1000000.0)));
    }

    public double getFrequencyCorrection()
    {
        return mFrequencyCorrection;
    }

    public void setFrequencyCorrection(double correction) throws SourceException
    {
        mFrequencyCorrection = correction;

        if(mFrequency > 0)
        {
            setFrequency(mFrequency, true);
        }

        broadcastFrequencyCorrectionChange();
    }

    /**
     * Adds listener to receive frequency change events
     */
    public void addSourceEventProcessor(ISourceEventProcessor processor)
    {
        if(mTunable != null)
        {
            mTunable.getLock().lock();

            try
            {
                if(!mProcessors.contains(processor))
                {
                    mProcessors.add(processor);
                }
            }
            finally
            {
                mTunable.getLock().unlock();
            }
        }
    }

    /**
     * Removes listener from receiving frequency change events
     */
    public void removeSourceEventProcessor(ISourceEventProcessor processor)
    {
        if(mTunable != null)
        {
            mTunable.getLock().lock();

            try
            {
                mProcessors.remove(processor);
            }
            finally
            {
                mTunable.getLock().unlock();
            }
        }
    }


    /**
     * Broadcasts a change/update to the current (uncorrected) frequency or the
     * bandwidth/sample rate value.
     */
    protected void broadcastFrequencyChange() throws SourceException
    {
        broadcast(SourceEvent.frequencyChange(null, mFrequency));
    }

    /**
     * Broadcast a frequency error/correction value change
     */
    protected void broadcastFrequencyCorrectionChange() throws SourceException
    {
        broadcast(SourceEvent.frequencyCorrectionChange((int)mFrequencyCorrection));
    }

    /**
     * Broadcasts a sample rate change
     */
    protected void broadcastSampleRateChange() throws SourceException
    {
        broadcast(SourceEvent.sampleRateChange(mSampleRate));
    }

    public void broadcast(SourceEvent event) throws SourceException
    {
        if(mTunable != null)
        {
            mTunable.getLock().lock();

            try
            {
                for(ISourceEventProcessor processor : mProcessors)
                {
                    processor.process(event);
                }
            }
            finally
            {
                mTunable.getLock().unlock();
            }
        }
    }

    public interface Tunable
    {
        /**
         * Reentrant lock to synchronize threaded access to tuner controls.
         */
        ReentrantLock getLock();

        /**
         * Gets the tuned frequency of the device
         */
        long getTunedFrequency() throws SourceException;

        /**
         * Sets the tuned frequency of the device
         */
        void setTunedFrequency(long frequency) throws SourceException;

        /**
         * Gets the current bandwidth setting of the device
         */
        double getCurrentSampleRate() throws SourceException;

        /**
         * Indicates if this tunable can tune the frequency
         */
        boolean canTune(long frequency);
    }
}