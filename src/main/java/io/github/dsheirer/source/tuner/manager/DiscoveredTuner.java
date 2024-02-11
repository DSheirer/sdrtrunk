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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A discovered tuner that may be accessible for use.
 */
public abstract class DiscoveredTuner implements ITunerErrorListener
{
    private Logger mLog = LoggerFactory.getLogger(DiscoveredTuner.class);
    private TunerStatus mTunerStatus = TunerStatus.ENABLED;
    private boolean mEnabled = true;
    private String mErrorMessage;
    private List<IDiscoveredTunerStatusListener> mListeners = new CopyOnWriteArrayList();
    protected Tuner mTuner;
    protected TunerConfiguration mTunerConfiguration;

    /**
     * Tuner Class
     */
    public abstract TunerClass getTunerClass();

    /**
     * Current status of the discovered tuner
     */
    public TunerStatus getTunerStatus()
    {
        return mTunerStatus;
    }

    /**
     * Logs current state of the tuner
     */
    public void logState()
    {
        mLog.info(getDiagnosticReport());
    }

    /**
     * Generates a state report for this tuner.
     * @return
     */
    public String getDiagnosticReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered Tuner: ").append(getId());
        sb.append("\n\tClass:").append(getClass());

        if(hasTuner())
        {
            sb.append("\n\tTuner Class:").append(getTuner().getClass());
            sb.append("\n\tTuner Controller Class:").append(getTuner().getTunerController().getClass());
            sb.append("\n\tFrequency:").append(getTuner().getTunerController().getFrequency());
            sb.append("\n\tError:").append(getErrorMessage());
            sb.append("\n\tChannel Manager Class:").append(getTuner().getChannelSourceManager().getClass());
            sb.append("\n\tChannel Manager:").append(getTuner().getChannelSourceManager().getStateDescription());
        }
        else
        {
            sb.append("\n\tTuner - no tuner");
        }

        return sb.toString();
    }

    /**
     * Sets the status of the discovered tuner and notifies registered listeners of the status change.
     * @param tunerStatus to set
     */
    private void setTunerStatus(TunerStatus tunerStatus)
    {
        setTunerStatus(tunerStatus, true);
    }

    /**
     * Sets the status of the discovered tuner and optionally notifies registered listeners of the status change.
     * @param tunerStatus to set
     * @param notifyListeners true to notify and false to not notify
     */
    private void setTunerStatus(TunerStatus tunerStatus, boolean notifyListeners)
    {
        if(mTunerStatus != tunerStatus)
        {
            TunerStatus previous = mTunerStatus;
            mTunerStatus = tunerStatus;

            if(notifyListeners)
            {
                broadcast(this, previous, mTunerStatus);
            }
        }
    }

    /**
     * Indicates if this discovered tuner is enabled and usable.
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Sets the enabled state of this discovered tuner
     */
    public void setEnabled(boolean enabled)
    {
        //If there was a change in state
        if(mEnabled ^ enabled)
        {
            mErrorMessage = null;

            mEnabled = enabled;

            if(mEnabled)
            {
                start();
                setTunerStatus(TunerStatus.ENABLED);
            }
            else
            {
                stop();
                setTunerStatus(TunerStatus.DISABLED);
            }
        }
    }

    /**
     * Indicates if this discovered tuner is available and usable.  Use this method to check if a discovered tuner is
     * available prior to access the tuner directly via the getTuner() method.
     */
    public boolean isAvailable()
    {
        return getTunerStatus().isAvailable();
    }

    /**
     * An identifier for this discovered tuner where this identifier when combined with the discovered tuner type
     * is a globally unique value.
     *
     * Note: this identifier will be used to persist the enabled/disabled state of each discoverable tuner.  Therefore,
     * this identifier must be consistent across application run cycles in order to correctly manage disabled tuners.
     * @return globally unique string identifier for the discovered tuner.
     */
    public abstract String getId();

    /**
     * Access a started and initialized.
     *
     * Use the isAvailable() method to check if this tuner is available prior to invoking this method to avoid a
     * null tuner instance.
     *
     * @return started and initialized tuner
     */
    public Tuner getTuner()
    {
        return mTuner;
    }

    /**
     * Indicates if this discovered tuner is started and has a fully constructed tuner instance
     */
    public boolean hasTuner()
    {
        return getTuner() != null;
    }

    /**
     * Tuner configuration for this tuner
     */
    public TunerConfiguration getTunerConfiguration()
    {
        return mTunerConfiguration;
    }

    /**
     * Sets the tuner configuration for this tuner.
     */
    public void setTunerConfiguration(TunerConfiguration tunerConfiguration)
    {
        mTunerConfiguration = tunerConfiguration;

        if(hasTuner())
        {
            try
            {
                getTuner().getTunerController().apply(mTunerConfiguration);
            }
            catch(SourceException se)
            {
                mLog.error("Error applying tuner configuration [" + mTunerConfiguration.getClass() +
                        "] to discovered tuner [" + getId() + "}", se);
            }
        }
    }

    /**
     * Indicates if this discovered tuner has a tuner configuration
     */
    public boolean hasTunerConfiguration()
    {
        return mTunerConfiguration != null;
    }

    /**
     * Adds a tuner status change listener to monitor this discovered tuner for changes in status.
     */
    public void addTunerStatusListener(IDiscoveredTunerStatusListener listener)
    {
        if(!mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a tuner status change listener from monitoring this discovered tuner for changes in status.
     */
    public void removeTunerStatusListener(IDiscoveredTunerStatusListener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Broadcasts the tuner status change to all registered listeners.
     * @param tuner that was changed
     * @param previous tuner status
     * @param current tuner status
     */
    private void broadcast(DiscoveredTuner tuner, TunerStatus previous, TunerStatus current)
    {
        for(IDiscoveredTunerStatusListener listener: mListeners)
        {
            listener.tunerStatusUpdated(tuner, previous, current);
        }
    }

    /**
     * Sets this tuner to an error state and applies the error message
     * @param errorMessage to set
     */
    @Override
    public void setErrorMessage(String errorMessage)
    {
        mErrorMessage = errorMessage;
        mLog.info("Tuner Error - Stopping - " + getId() + " Error: " + errorMessage);
        stop();
        setTunerStatus(TunerStatus.ERROR);
    }

    @Override
    public void tunerRemoved()
    {
        setTunerStatus(TunerStatus.REMOVED);
    }

    /**
     * Indicates if this tuner has an error message.
     */
    public boolean hasErrorMessage()
    {
        return mErrorMessage != null;
    }

    /**
     * Optional error message.
     * @return error message if there is one, or null if there is not.
     */
    public String getErrorMessage()
    {
        return mErrorMessage;
    }

    /**
     * Fully instantiate and start this discovered tuner to make it usable within the application.  Implementations
     * should attempt to instantiate the tuner and assign it to mTuner variable.  If there is an error, invoke the
     * setErrorMessage() to signal the tuner is unusable.
     */
    public abstract void start();

    /**
     * Attempts to restart a tuner that's currently in an error state
     */
    public void restart()
    {
        if(getTunerStatus() == TunerStatus.ERROR)
        {
            mErrorMessage = null;

            if(isEnabled())
            {
                //Change status to enabled so that we can attempt to start, but don't notify listeners yet.
                setTunerStatus(TunerStatus.ENABLED);
                start();
            }
            else
            {
                setTunerStatus(TunerStatus.DISABLED);
            }
        }
    }

    /**
     * Stop this discovered tuner, notify registered listeners/consumers and release any resources that it is using.
     */
    public void stop()
    {
        if(hasTuner())
        {
            mLog.info("Stopping Tuner: " + getId());
            getTuner().stop();
            mTuner = null;
        }
    }
}
