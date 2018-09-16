/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2017 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferProvider;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceEventListenerToProcessorAdapter;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.frequency.FrequencyController;
import io.github.dsheirer.source.tuner.frequency.FrequencyController.Tunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;

public abstract class TunerController implements Tunable, ISourceEventProcessor, ISourceEventListener,
    IReusableComplexBufferProvider, Listener<ReusableComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerController.class);
    protected ReusableBufferBroadcaster mReusableBufferBroadcaster = new ReusableBufferBroadcaster();
    protected FrequencyController mFrequencyController;
    private int mMiddleUnusableHalfBandwidth;
    private double mUsableBandwidthPercentage;
    private Listener<SourceEvent> mSourceEventListener;
    private int mMeasuredFrequencyError;

    /**
     * Abstract tuner controller class.  The tuner controller manages frequency bandwidth and currently tuned channels
     * that are being fed samples from the tuner.
     *
     * @param minimumFrequency minimum uncorrected tunable frequency
     * @param maximumFrequency maximum uncorrected tunable frequency
     * @param middleUnusableHalfBandwidth is the +/- value center DC spike to avoid for channels
     * @param usableBandwidth percentage of usable bandwidth relative to space at the extreme ends of the spectrum
     * @throws SourceException - for any issues related to constructing the
     *                         class, tuning a frequency, or setting the bandwidth
     */
    public TunerController(long minimumFrequency, long maximumFrequency, int middleUnusableHalfBandwidth, double usableBandwidth)
    {
        mFrequencyController = new FrequencyController(this, minimumFrequency, maximumFrequency, 0.0d);
        mMiddleUnusableHalfBandwidth = middleUnusableHalfBandwidth;
        mUsableBandwidthPercentage = usableBandwidth;
        mSourceEventListener = new SourceEventListenerToProcessorAdapter(this);
    }

    /**
     * Dispose of this tuner controller and prepare for shutdown.
     */
    public abstract void dispose();

    /**
     * Number of samples contained in each complex buffer provided by this tuner.
     *
     * @return number of complex sample in each buffer.
     */
    public abstract int getBufferSampleCount();

    /**
     * Duration in milliseconds for each sample buffer provided by this tuner
     */
    public long getBufferDuration()
    {
        return (long)(1000.0 / (getSampleRate() / (double)getBufferSampleCount()));
    }

    /**
     * Implements the ISourceEventListener interface to receive requests from sample consumers
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    /**
     * Applies the settings in the tuner configuration
     */
    public abstract void apply(TunerConfiguration config) throws SourceException;

    /**
     * Responds to requests to set the frequency
     */
    @Override
    public void process(SourceEvent sourceEvent ) throws SourceException
    {
        switch(sourceEvent.getEvent())
        {
            case REQUEST_FREQUENCY_CHANGE:
                setFrequency( sourceEvent.getValue().longValue() );
                break;
            case REQUEST_START_SAMPLE_STREAM:
                if(sourceEvent.getSource() instanceof Listener)
                {
                    addBufferListener((Listener<ReusableComplexBuffer>)sourceEvent.getSource());
                }
                break;
            case REQUEST_STOP_SAMPLE_STREAM:
                if(sourceEvent.getSource() instanceof Listener)
                {
                    removeBufferListener((Listener<ReusableComplexBuffer>)sourceEvent.getSource());
                }
                break;
            default:
                mLog.error("Ignoring unrecognized source event: " + sourceEvent.getEvent().name() + " from [" +
                    sourceEvent.getSource().getClass() + "]" );
        }
    }

    /**
     * Indicates if the frequency and sample rate controls are locked by another process.  User interface controls
     * should monitor source events and check the locked state via this method to correctly render the enabled state
     * of the frequency and sample rate controls.
     *
     * @return true if the tuner controller is locked.
     */
    public boolean isLocked()
    {
        return mFrequencyController.isLocked();
    }

    /**
     * Sets the frequency and sample rate locked state to the locked argument value.  This should only be changed
     * by a downstream consumer of samples to prevent users or other processes from modifying the center frequency
     * and/or sample rate of the tuner while processing samples.
     *
     * @param locked true to indicate the tuner controller is in a locked state, otherwise false.
     */
    public void setLocked(boolean locked)
    {
        try
        {
            mFrequencyController.setLocked(locked);
        }
        catch(SourceException se)
        {
            mLog.error("Couldn't set frequency controller locked state to: " + locked);
        }
    }

    public int getBandwidth()
    {
        return mFrequencyController.getBandwidth();
    }

    /**
     * Sets the center frequency of the local oscillator.
     *
     * @param frequency in hertz
     * @throws SourceException - if the tuner has any issues
     */
    public void setFrequency(long frequency) throws SourceException
    {
        mFrequencyController.setFrequency(frequency);
    }

    /**
     * Gets the center frequency of the local oscillator
     *
     * @return frequency in hertz
     */
    public long getFrequency()
    {
        return mFrequencyController.getFrequency();
    }

    @Override
    public boolean canTune(long frequency)
    {
        return mFrequencyController.canTune(frequency);
    }

    public double getSampleRate()
    {
        return mFrequencyController.getSampleRate();
    }

    public double getFrequencyCorrection()
    {
        return mFrequencyController.getFrequencyCorrection();
    }

    public void setFrequencyCorrection(double correction) throws SourceException
    {
        mFrequencyController.setFrequencyCorrection(correction);
    }

    public long getMinFrequency()
    {
        return mFrequencyController.getMinimumFrequency();
    }

    public long getMaxFrequency()
    {
        return mFrequencyController.getMaximumFrequency();
    }

    public long getMinTunedFrequency() throws SourceException
    {
        return mFrequencyController.getFrequency() - (getUsableBandwidth() / 2);
    }

    public long getMaxTunedFrequency() throws SourceException
    {
        return mFrequencyController.getFrequency() + (getUsableBandwidth() / 2);
    }

    /**
     * Total bandwidth of the middle unusable bandwidth region.  This value is used to avoid a central DC spike
     * present in some tuners.
     */
    public int getMiddleUnusableHalfBandwidth()
    {
        return mMiddleUnusableHalfBandwidth;
    }

    /**
     * Indicates if this tuner controller has a middle unusable bandwidth region.
     */
    public boolean hasMiddleUnusableBandwidth()
    {
        return mMiddleUnusableHalfBandwidth != 0;
    }

    /**
     * Usable bandwidth - total bandwidth minus the unusable space at either end of the spectrum.
     */
    public int getUsableBandwidth()
    {
        return (int)(getBandwidth() * mUsableBandwidthPercentage);
    }

    /**
     * Usable half bandwidth - total bandwidth minus unusable space at either end of the spectrum.
     *
     * Note: this does not account for any DC spike protected frequency region at the center of the tuner
     */
    public int getUsableHalfBandwidth()
    {
        return (int)(getUsableBandwidth() / 2);
    }

    /**
     * Current measured frequency error as received/reported from certain downstream decoders.
      * @return measured frequency error in hertz averaged over 1-second intervals
     */
    public int getMeasuredFrequencyError()
    {
        return mMeasuredFrequencyError;
    }

    /**
     * Indicates if this tuner controller has a non-zero measured frequency error value.
     */
    public boolean hasMeasuredFrequencyError()
    {
        return mMeasuredFrequencyError != 0;
    }

    /**
     * Calculates the current PPM error value as measured by certain downstream decoders.
     * @return
     */
    public double getPPMFrequencyError()
    {
        if(hasMeasuredFrequencyError())
        {
            return mMeasuredFrequencyError / ((double)getFrequency() / 1E6d);
        }

        return 0.0d;
    }

    /**
     * Sets the measured frequency error average.
     * @param measuredFrequencyError in hertz averaged over a 5 second interval.
     */
    public void setMeasuredFrequencyError(int measuredFrequencyError)
    {
        mMeasuredFrequencyError = measuredFrequencyError;
    }

    /**
     * Sets the listener to be notified any time that the tuner changes frequency
     * or bandwidth/sample rate.
     *
     * Note: this is normally used by the Tuner.  Any additional listeners can
     * be registered on the tuner.
     */
    public void addListener( ISourceEventProcessor processor )
    {
        mFrequencyController.addListener(processor);
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( ISourceEventProcessor processor )
    {
        mFrequencyController.removeFrequencyChangeProcessor(processor);
    }

    /**
     * Adds the listener to receive complex buffer samples
     */
    @Override
    public void addBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mReusableBufferBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving complex buffer samples
     */
    @Override
    public void removeBufferListener(Listener<ReusableComplexBuffer> listener)
    {
        mReusableBufferBroadcaster.removeListener(listener);
    }

    /**
     * Indicates if there are any complex buffer listeners registered on this controller
     */
    @Override
    public boolean hasBufferListeners()
    {
        return mReusableBufferBroadcaster.hasListeners();
    }

    /**
     * Broadcasts the buffer to any registered listeners
     */
    protected void broadcast(ReusableComplexBuffer reusableComplexBuffer)
    {
        mReusableBufferBroadcaster.broadcast(reusableComplexBuffer);
    }

    /**
     * Implements the Listener<T> interface to receive and distribute complex buffers from subclass implementations
     */
    @Override
    public void receive(ReusableComplexBuffer complexBuffer)
    {
        broadcast(complexBuffer);
    }

    /**
     * Indicates if the current center frequency and bandwidth is correct to source the tuner channel
     *
     * @param tunerChannel to test
     * @return true if the current center frequency and bandwidth is correct for the channel
     */
    public boolean isTunedFor(TunerChannel tunerChannel)
    {
        try
        {
            if(tunerChannel.getMinFrequency() < getMinTunedFrequency())
            {
                return false;
            }

            if(tunerChannel.getMaxFrequency() > getMaxTunedFrequency())
            {
                return false;
            }

            if(hasMiddleUnusableBandwidth())
            {
                long minAvoid = getFrequency() - getMiddleUnusableHalfBandwidth();
                long maxAvoid = getFrequency() + getMiddleUnusableHalfBandwidth();

                if(tunerChannel.overlaps(minAvoid, maxAvoid))
                {
                    return false;
                }
            }
        }
        catch(SourceException se)
        {
            return false;
        }

        return true;
    }

    /**
     * Indicates if the current center frequency and bandwidth is correct to source the tuner channel set
     *
     * @param tunerChannels to test
     * @return true if the current center frequency and bandwidth is correct for the channel set
     */
    public boolean isTunedFor(SortedSet<TunerChannel> tunerChannels)
    {
        try
        {
            if(tunerChannels.first().getMinFrequency() < getMinTunedFrequency())
            {
                return false;
            }

            if(tunerChannels.last().getMaxFrequency() > getMaxTunedFrequency())
            {
                return false;
            }

            if(hasMiddleUnusableBandwidth())
            {
                long minAvoid = getFrequency() - getMiddleUnusableHalfBandwidth();
                long maxAvoid = getFrequency() + getMiddleUnusableHalfBandwidth();

                for(TunerChannel tunerChannel: tunerChannels)
                {
                    if(tunerChannel.overlaps(minAvoid, maxAvoid))
                    {
                        return false;
                    }
                }
            }
        }
        catch(SourceException se)
        {
            return false;
        }

        return true;
    }
}