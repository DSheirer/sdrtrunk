/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package source.tuner.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import source.ISourceEventProcessor;
import source.InvalidFrequencyException;
import source.SourceEvent;
import source.SourceException;

import java.util.ArrayList;
import java.util.List;

public class FrequencyController
{
    private final static Logger mLog = LoggerFactory.getLogger(FrequencyController.class);

    private Tunable mTunable;
    private long mFrequency = 101100000;
    private long mTunedFrequency = 101100000;
    private long mMinimumFrequency;
    private long mMaximumFrequency;
    private double mFrequencyCorrection = 0.0d;
    private double mSampleRate = 0.0d;
    private long mCenterBlackoutBandwidth;
    private double mUsableBandwidthPercentage;
    private boolean mLocked;

    private List<ISourceEventProcessor> mProcessors = new ArrayList<>();

    public FrequencyController(Tunable tunable, long minFrequency, long maxFrequency, double frequencyCorrection,
                               long centerBlackoutBandwidth, double usableBandwidthPercentage)
    {
        mTunable = tunable;
        mMinimumFrequency = minFrequency;
        mMaximumFrequency = maxFrequency;
        mFrequencyCorrection = frequencyCorrection;
        mCenterBlackoutBandwidth = centerBlackoutBandwidth;
        mUsableBandwidthPercentage = usableBandwidthPercentage;
    }

    public boolean isLocked()
    {
        return mLocked;
    }

    /**
     * Sets the locked status.  When this control is locked, the sample rate cannot be changed and the center
     * tuned frequency can only be changed via the setLockedFrequency() method.
     *
     * Note: this method is package-private and is intended to be used (only) by the ChannelManager.
     *
     * @param locked
     */
    void setLocked(boolean locked)
    {
        mLocked = locked;
    }


    /**
     * Set sample rate/bandwidth in hertz
     */
    public void setSampleRate(int sampleRate) throws SourceException
    {
        if(isLocked())
        {
            throw new FrequencyControllerLockedException();
        }

        mSampleRate = sampleRate;

        broadcastSampleRateChange();
    }

    /**
     * Get sample rate/bandwidth in hertz
     */
    public double getSampleRate()
    {
        return mSampleRate;
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
        if(isLocked())
        {
            throw new FrequencyControllerLockedException();
        }

        setFrequency(frequency, true);
    }

    /**
     * Sets the frequency when the frequency controller is locked.  Note: this method is package-private and is
     * only intended to be used by the ChannelManager.
     *
     * @param frequency to set as the center tuned frequency
     * @throws SourceException if there is an error while setting the frequency
     */
    void setLockedFrequency(long frequency) throws SourceException
    {
        setFrequency(frequency, true);
    }

    /**
     * Indicates if the specified frequency can be tuned (ie is within min/max frequency) by this controller.
     * @param frequency to evaluate
     * @return true if the frequency falls within the tuning range of this controller
     */
    public boolean canTune(long frequency)
    {
        return getMinimumFrequency() <= frequency && frequency <= getMaximumFrequency();
    }

    /**
     * Set frequency with optional broadcast of frequency change event.  This
     * method supports changing the frequency correction value without
     * broadcasting a frequency change event.
     */
    private void setFrequency(long frequency, boolean broadcastChange) throws SourceException
    {
        long tunedFrequency = getTunedFrequency(frequency);

        if(tunedFrequency < mMinimumFrequency)
        {
            throw new InvalidFrequencyException("Requested frequency not valid", frequency, mMinimumFrequency);
        }

        if(tunedFrequency > mMaximumFrequency)
        {
            throw new InvalidFrequencyException("Requested frequency not valid", frequency, mMaximumFrequency);
        }

        mFrequency = frequency;

        mTunedFrequency = tunedFrequency;

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

    public long getMinimumFrequency()
    {
        return mMinimumFrequency;
    }

    public long getMaximumFrequency()
    {
        return mMaximumFrequency;
    }

    /**
     * Calculate the tuned frequency by adding frequency correction to the
     * corrected frequency.
     *
     * @param correctedFrequency
     */
    private long getTunedFrequency(long correctedFrequency)
    {
        return (long)((double)correctedFrequency /
            (1.0 + (mFrequencyCorrection / 1000000.0)));
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
    public void addListener(ISourceEventProcessor processor)
    {
        if(!mProcessors.contains(processor))
        {
            mProcessors.add(processor);
        }
    }

    /**
     * Removes listener from receiving frequency change events
     */
    public void removeFrequencyChangeProcessor(ISourceEventProcessor processor)
    {
        mProcessors.remove(processor);
    }


    /**
     * Broadcasts a change/update to the current (uncorrected) frequency or the
     * bandwidth/sample rate value.
     */
    protected void broadcastFrequencyChange() throws SourceException
    {
        broadcast(SourceEvent.frequencyChange(mFrequency));
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
        for(ISourceEventProcessor processor : mProcessors)
        {
            processor.process(event);
        }
    }

    /**
     * Identifies the bandwidth of a blackout frequency range centered about the center frequency.  This is
     * used to block the frequency range where a DC spike may exist.
     * @return center blackout bandwidth in hertz
     */
    public long getCenterBlackoutBandwidth()
    {
        return mCenterBlackoutBandwidth;
    }

    /**
     * Percentage of the available bandwidth that is usable.  Sources with filter roll-off at the ends of the
     * spectral range can limit the usable bandwidth to avoid providing samples at these spectral ends.
     *
     * @return bandwidth percentage (0.0 <> 1.0)
     */
    public double getUsableBandwidthPercentage()
    {
        return mUsableBandwidthPercentage;
    }

    /**
     * Usable bandwidth in hertz calculated as the sample rate constrained by the usable bandwidth percentage.
     */
    public int getUsableBandwidth()
    {
        return (int)(getSampleRate() * getUsableBandwidthPercentage());
    }

    public interface Tunable
    {
        /**
         * Gets the tuned frequency of the device
         */
        public long getTunedFrequency() throws SourceException;

        /**
         * Sets the tuned frequency of the device
         */
        public void setTunedFrequency(long frequency) throws SourceException;

        /**
         * Gets the current bandwidth setting of the device
         */
        public double getCurrentSampleRate() throws SourceException;

        /**
         * Indicates if this tunable can tune the frequency
         */
        public boolean canTune(long frequency);
    }
}
