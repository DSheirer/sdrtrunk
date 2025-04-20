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
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.INativeBufferProvider;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderFactory;
import io.github.dsheirer.record.wave.IRecordingStatusListener;
import io.github.dsheirer.record.wave.NativeBufferWaveRecorder;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceEventListenerToProcessorAdapter;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.frequency.FrequencyController;
import io.github.dsheirer.source.tuner.frequency.FrequencyController.Tunable;
import io.github.dsheirer.source.tuner.manager.FrequencyErrorCorrectionManager;
import java.text.DecimalFormat;
import java.util.SortedSet;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base tuner controller implementation.
 */
public abstract class TunerController implements Tunable, ISourceEventProcessor, ISourceEventListener,
        INativeBufferProvider, Listener<INativeBuffer>, ITunerErrorListener
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerController.class);

    //Protects access to the native buffer broadcaster for adding, removing or checking for listener count.
    private ReentrantLock mBufferListenerLock = new ReentrantLock();
    private ReentrantLock mLock = new ReentrantLock();
    protected Broadcaster<INativeBuffer> mNativeBufferBroadcaster = new Broadcaster();
    protected FrequencyController mFrequencyController;
    private int mMiddleUnusableHalfBandwidth;
    private int mMeasuredFrequencyError;
    private double mUsableBandwidthPercentage;
    private SourceEventListenerToProcessorAdapter mSourceEventListener;
    private NativeBufferWaveRecorder mRecorder;
    private ITunerErrorListener mTunerErrorListener;
    private DecimalFormat mFrequencyErrorPPMFormat = new DecimalFormat("0.0");
    private FrequencyErrorCorrectionManager mFrequencyErrorCorrectionManager;

    /**
     * Abstract tuner controller class.  The tuner controller manages frequency bandwidth and currently tuned channels
     * that are being fed samples from the tuner.
     * @param tunerErrorListener to monitor errors produced from this tuner controller
     */
    public TunerController(ITunerErrorListener tunerErrorListener)
    {
        mTunerErrorListener = tunerErrorListener;
        mFrequencyController = new FrequencyController(this);
        mSourceEventListener = new SourceEventListenerToProcessorAdapter(this);
        mFrequencyErrorCorrectionManager = new FrequencyErrorCorrectionManager(this);
    }

    /**
     * Lock for controlling configuration access to this tuner controller and any embedded tuner to ensure that
     * configuration changes happen atomically.  There are at least two threads that can touch this controller:
     *
     * A: channel processing that changes the center tuned frequency and frequency correction.
     * b: user ui thread that can touch any of the frequency correction and gain controls.
     *
     * Generally, we'll lock on each of the primary configuration change points - getter and setter for:
     *      Frequency
     *      Frequency Correction
     *      Sample Rate
     *      Gain Controls
     *
     * @return lock for controlling access to tuner controller and em
     */
    public ReentrantLock getLock()
    {
        return mLock;
    }

    /**
     * Updates the frequency controller with the new minimum and maximum values.
     * @param minimum frequency Hertz
     * @param maximum frequency Hertz
     */
    public void setFrequencyExtents(long minimum, long maximum)
    {
        mFrequencyController.setMinimumFrequency(minimum);
        mFrequencyController.setMaximumFrequency(maximum);
    }

    /**
     * Frequency correction manager for this tuner controller.
     */
    public FrequencyErrorCorrectionManager getFrequencyErrorCorrectionManager()
    {
        return mFrequencyErrorCorrectionManager;
    }

    protected void dispose()
    {
        getFrequencyErrorCorrectionManager().dispose();
        mNativeBufferBroadcaster.clear();
        mFrequencyController.dispose();
        mSourceEventListener.dispose();
        mTunerErrorListener = null;
    }

    /**
     * Perform startup operations
     * @throws SourceException if there is an error that makes this tuner controller unusable
     */
    public abstract void start() throws SourceException;

    /**
     * Perform shutdown and disposal operations.
     *
     * Note: implementation should notify any native buffer listeners that we're shutting down.
     */
    public abstract void stop();

    /**
     * Tuner type for this controller
     */
    public abstract TunerType getTunerType();

    /**
     * Sets an unrecoverable error for this tuner controller that will be handled by the listener
     * @param errorMessage to set
     */
    @Override
    public void setErrorMessage(String errorMessage)
    {
        if(mTunerErrorListener != null)
        {
            mTunerErrorListener.setErrorMessage(errorMessage);
        }
    }

    /**
     * Indicates that the tuner has been removed from the system and requests the listener to process the cleanup.
     */
    @Override
    public void tunerRemoved()
    {
        //Request parent tuner process the shutdown and cleanup
        if(mTunerErrorListener != null)
        {
            mTunerErrorListener.tunerRemoved();
        }
    }

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
    public void apply(TunerConfiguration config) throws SourceException
    {
        //Set the frequency from the config.  If the config frequency generates an error, try setting the default
        //frequency before we give up on the tuner as having an error.
        try
        {
            setFrequency(config.getFrequency());
        }
        catch(Exception e)
        {
            mLog.warn("Unable to restore previous frequency [" + config.getFrequency() + "] trying default frequency");
            if(config.getFrequency() != TunerConfiguration.DEFAULT_FREQUENCY)
            {
                setFrequency(TunerConfiguration.DEFAULT_FREQUENCY);
                //If we're successful to here, update the config with the default frequency.
                config.setFrequency(TunerConfiguration.DEFAULT_FREQUENCY);
            }
        }

        if(config.getMinimumFrequency() > 0)
        {
            setMinimumFrequency(config.getMinimumFrequency());
        }
        if(config.getMaximumFrequency() > 0)
        {
            setMaximumFrequency(config.getMaximumFrequency());
        }
        setFrequencyCorrection(config.getFrequencyCorrection());
        getFrequencyErrorCorrectionManager().setEnabled(config.getAutoPPMCorrectionEnabled());
    }

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
                    addBufferListener((Listener<INativeBuffer>)sourceEvent.getSource());
                }
                break;
            case REQUEST_STOP_SAMPLE_STREAM:
                if(sourceEvent.getSource() instanceof Listener)
                {
                    removeBufferListener((Listener<INativeBuffer>)sourceEvent.getSource());
                }
                break;
            default:
                mLog.error("Ignoring unrecognized source event: " + sourceEvent.getEvent().name() + " from [" +
                    sourceEvent.getSource().getClass() + "]" );
        }
    }

    /**
     * Indicates if the sample rate control is locked by another process.  User interface controls should monitor source
     * events and check the locked state via this method to correctly render the enabled state of the sample rate control.
     *
     * @return true if the tuner controller is locked.
     */
    public boolean isLockedSampleRate()
    {
        //Note: this access is not protected by the mFrequencyControllerLock
        return mFrequencyController.isSampleRateLocked();
    }

    /**
     * Sets the sample rate locked state to the locked argument value.  This should only be changed by a downstream
     * consumer of samples to prevent users or other processes from modifying the sample rate of the tuner while
     * processing samples.
     *
     * @param locked true to indicate the tuner controller is in a locked state, otherwise false.
     */
    public void setLockedSampleRate(boolean locked)
    {
        try
        {
            //Note: this access is not protected by the mFrequencyControllerLock
            mFrequencyController.setSampleRateLocked(locked);
        }
        catch(SourceException se)
        {
            mLog.error("Couldn't set frequency controller locked state to: " + locked);
        }
    }

    public int getBandwidth()
    {
        //Note: this access is not protected by the mFrequencyControllerLock
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
        try
        {
            getLock().lock();
            mFrequencyController.setFrequency(frequency);
        }
        finally
        {
            getLock().unlock();
        }
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
        boolean canTune;

        try
        {
            getLock().lock();
            canTune = mFrequencyController.canTune(frequency);
        }
        finally
        {
            getLock().unlock();
        }

        return canTune;
    }

    public double getSampleRate()
    {
        //Note: this access is not protected by the mFrequencyControllerLock
        return mFrequencyController.getSampleRate();
    }

    public double getFrequencyCorrection()
    {
        double correction;

        try
        {
            getLock().lock();
            correction = mFrequencyController.getFrequencyCorrection();
        }
        finally
        {
            getLock().unlock();
        }

        return correction;
    }

    public void setFrequencyCorrection(double correction) throws SourceException
    {
        try
        {
            getLock().lock();
            mFrequencyController.setFrequencyCorrection(correction);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Minimum tunable frequency
     * @return minimum in Hertz
     */
    public long getMinimumFrequency()
    {
        long minimum;

        try
        {
            getLock().lock();
            minimum = mFrequencyController.getMinimumFrequency();
        }
        finally
        {
            getLock().unlock();
        }

        return minimum;
    }

    /**
     * Sets the minimum tunable frequency
     * @param minimum frequency in Hertz
     */
    public void setMinimumFrequency(long minimum)
    {
        try
        {
            getLock().lock();
            mFrequencyController.setMinimumFrequency(minimum);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Maximum tunable frequency
     * @return maximum in Hertz
     */
    public long getMaximumFrequency()
    {
        long maximum;

        try
        {
            getLock().lock();
            maximum = mFrequencyController.getMaximumFrequency();
        }
        finally
        {
            getLock().unlock();
        }

        return maximum;
    }

    /**
     * Sets the maximum tunable frequency
     * @param maximum in Hertz
     */
    public void setMaximumFrequency(long maximum)
    {
        try
        {
            getLock().lock();
            mFrequencyController.setMaximumFrequency(maximum);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public long getMinTunedFrequency() throws SourceException
    {
        long minTuned;

        try
        {
            getLock().lock();
            minTuned = mFrequencyController.getFrequency() - (getUsableBandwidth() / 2);
        }
        finally
        {
            getLock().unlock();
        }

        return minTuned;
    }

    public long getMaxTunedFrequency() throws SourceException
    {
        long maxTuned;

        try
        {
            getLock().lock();
            maxTuned = mFrequencyController.getFrequency() + (getUsableBandwidth() / 2);
        }
        finally
        {
            getLock().unlock();
        }

        return maxTuned;
    }

    /**
     * Half of the total bandwidth of the middle unusable bandwidth region.  This value is used to avoid a central DC
     * spike present in some tuners.
     */
    public int getMiddleUnusableHalfBandwidth()
    {
        return mMiddleUnusableHalfBandwidth;
    }

    /**
     * Sets the middle unusable half bandwidth value
     * @param halfBandwidth in Hertz
     */
    public void setMiddleUnusableHalfBandwidth(int halfBandwidth)
    {
        mMiddleUnusableHalfBandwidth = halfBandwidth;
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
     * Sets the usable bandwidth percentage -- this can change based on samplerate for some tuners
     */
    public void setUsableBandwidthPercentage(double usableBandwidthPercentage)
    {
        mUsableBandwidthPercentage = usableBandwidthPercentage;
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
     * Status of measured frequency and PPM error
     */
    public String getMeasuredErrorStatus()
    {
        if(hasMeasuredFrequencyError())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Measured Error: ");
            sb.append(getMeasuredFrequencyError());
            sb.append("Hz (");
            sb.append(mFrequencyErrorPPMFormat.format(getPPMFrequencyError()));
            sb.append("ppm)");
            return sb.toString();
        }

        return "";
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
        getFrequencyErrorCorrectionManager().updatePPM(getPPMFrequencyError());
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
        mFrequencyController.addSourceEventProcessor(processor);
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( ISourceEventProcessor processor )
    {
        mFrequencyController.removeSourceEventProcessor(processor);
    }

    /**
     * Adds the listener to receive complex buffer samples
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        mBufferListenerLock.lock();

        try
        {
            mNativeBufferBroadcaster.addListener(listener);
        }
        finally
        {
            mBufferListenerLock.unlock();
        }
    }

    /**
     * Removes the listener from receiving complex buffer samples
     */
    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        mBufferListenerLock.lock();

        try
        {
            mNativeBufferBroadcaster.removeListener(listener);
        }
        finally
        {
            mBufferListenerLock.unlock();
        }
    }

    /**
     * Indicates if there are any complex buffer listeners registered on this controller
     */
    @Override
    public boolean hasBufferListeners()
    {
        boolean hasListeners;

        mBufferListenerLock.lock();

        try
        {
            hasListeners = mNativeBufferBroadcaster.hasListeners();
        }
        finally
        {
            mBufferListenerLock.unlock();
        }

        return hasListeners;
    }

    /**
     * Broadcasts the buffer to any registered listeners
     */
    protected void broadcast(INativeBuffer complexSamples)
    {
        //Note: unprotected access to the broadcaster ... the broadcaster uses thread-save internal list
        mNativeBufferBroadcaster.broadcast(complexSamples);
    }

    /**
     * Implements the Listener<T> interface to receive and distribute complex buffers from subclass implementations
     */
    @Override
    public void receive(INativeBuffer nativeBuffer)
    {
        broadcast(nativeBuffer);
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

    /**
     * Records the complex I/Q buffers produced by the tuner
     * @param userPreferences to obtain a baseband recorder
     * @param statusListener to receive updates on recording file name and size
     * @param prefix for the recording file name (ie tuner class name)
     */
    public void startRecorder(UserPreferences userPreferences, IRecordingStatusListener statusListener, String prefix)
    {
        if(!isRecording())
        {
            mRecorder = RecorderFactory.getTunerRecorder(prefix + "_" + getFrequency(), userPreferences, statusListener);
            mRecorder.setSampleRate((float)getSampleRate());
            mRecorder.start();
            addBufferListener(mRecorder);
        }
    }

    /**
     * Stops the recording of complex I/Q buffers
     */
    public void stopRecorder()
    {
        if(isRecording())
        {
            removeBufferListener(mRecorder);
            mRecorder.stop();
            mRecorder = null;
        }
    }

    /**
     * Indicates if this tuner controller is currently reocording the complex I/Q sample buffers produced by this tuner
     */
    public boolean isRecording()
    {
        return mRecorder != null;
    }
}