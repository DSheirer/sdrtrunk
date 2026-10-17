/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.sample.Listener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Audio Broadcaster class
 */
public abstract class AbstractAudioBroadcaster<T extends BroadcastConfiguration> implements Listener<AudioRecording>
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractAudioBroadcaster.class);
    private Listener<BroadcastEvent> mBroadcastEventListener;
    private T mBroadcastConfiguration;
    protected ObjectProperty<BroadcastState> mBroadcastState = new SimpleObjectProperty<>(BroadcastState.READY);
    protected ObjectProperty<BroadcastState> mLastBadBroadcastState = new SimpleObjectProperty<>();
    protected int mStreamedAudioCount = 0;
    protected int mDuplicateRejectedAudioCount = 0;
    protected int mErrorAudioCount = 0;
    protected int mAgedOffAudioCount = 0;
    private volatile boolean mStatisticsLoaded;
    private Preferences mStatistics;

    public record BroadcastStatistics(int streamed, int duplicateRejected, int errors, int agedOff) {}

    /**
     * Constructs an instance
     * @param broadcastConfiguration to use for this broadcaster
     */
    public AbstractAudioBroadcaster(T broadcastConfiguration)
    {
        mBroadcastConfiguration = broadcastConfiguration;
    }

    /**
     * Observable broadcast state property
     */
    public ObjectProperty<BroadcastState> broadcastStateProperty()
    {
        return mBroadcastState;
    }

    /**
     * Observable last bad broadcast state property
     */
    public ObjectProperty<BroadcastState> lastBadBroadcastStateProperty()
    {
        return mLastBadBroadcastState;
    }

    /**
     * Current state of the broadcastAudio connection
     */
    public BroadcastState getBroadcastState()
    {
        return mBroadcastState.get();
    }

    /**
     * Sets or changes the broadcast state
     */
    public void setBroadcastState(BroadcastState broadcastState)
    {
        if(broadcastState == BroadcastState.CONNECTED)
        {
            mLastBadBroadcastState.setValue(null);
        }
        else if(broadcastState.isErrorState() || broadcastState.isWarningState())
        {
            mLastBadBroadcastState.setValue(broadcastState);
        }
        mBroadcastState.setValue(broadcastState);
        broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STATE_CHANGE));
    }

    /**
     * Last bad state of the broadcastAudio connection
     */
    public BroadcastState getLastBadBroadcastState()
    {
        return mLastBadBroadcastState.get();
    }

    /**
     * Starts the broadcaster
     */
    public abstract void start();

    /**
     * Stops the broadcaster
     */
    public abstract void stop();

    /**
     * Perform any shutdown related tasks.
     */
    public abstract void dispose();

    /**
     * Access the configuration for the broadcaster
     */
    public T getBroadcastConfiguration()
    {
        return mBroadcastConfiguration;
    }

    /**
     * Registers the listener to receive broadcast events/state changes
     */
    public void setListener(Listener<BroadcastEvent> listener)
    {
        mBroadcastEventListener = listener;
    }

    /**
     * Removes the listener from receiving broadcast events/state changes
     */
   public void removeListener()
   {
       mBroadcastEventListener = null;
   }

    /**
     * Broadcasts the event to any registered listener
     */
    protected void broadcast(BroadcastEvent event)
    {
        if(mBroadcastEventListener != null)
        {
            mBroadcastEventListener.receive(event);
        }
    }

    /**
     * Number of audio recordings streamed to remote server
     */
    public int getStreamedAudioCount()
    {
        loadStatistics();
        return mStreamedAudioCount;
    }

    /**
     * Increments the streamed or uploaded audio count by one
     */
    public synchronized void incrementStreamedAudioCount()
    {
        loadStatistics();
        mStreamedAudioCount++;
        persistStatistics();
    }

    /**
     * Number of audio recordings rejected by the remote service as duplicates.
     */
    public int getDuplicateRejectedAudioCount()
    {
        loadStatistics();
        return mDuplicateRejectedAudioCount;
    }

    /**
     * Increments the duplicate-rejected audio count by one.
     */
    public synchronized void incrementDuplicateRejectedAudioCount()
    {
        loadStatistics();
        mDuplicateRejectedAudioCount++;
        persistStatistics();
    }

    /**
     * Number of audio recordings that were removed for exceeding age limit
     */
    public int getAgedOffAudioCount()
    {
        loadStatistics();
        return mAgedOffAudioCount;
    }

    /**
     * Increments the aged-off audio count by one
     */
    public synchronized void incrementAgedOffAudioCount()
    {
        loadStatistics();
        mAgedOffAudioCount++;
        persistStatistics();
    }

    /**
     * Number of audio recordings awaiting streaming or upload
     */
    public abstract int getAudioQueueSize();

    /**
     * Total audio upload/stream error count
     */
    public int getAudioErrorCount()
    {
        loadStatistics();
        return mErrorAudioCount;
    }

    /**
     * Increments the error audio count by one
     */
    public synchronized void incrementErrorAudioCount()
    {
        loadStatistics();
        mErrorAudioCount++;
        persistStatistics();
    }

    public static BroadcastStatistics getPersistedStatistics(BroadcastConfiguration configuration)
    {
        Preferences preferences = statisticsNode(configuration);
        return new BroadcastStatistics(preferences.getInt("streamed", 0),
            preferences.getInt("duplicates", 0), preferences.getInt("errors", 0),
            preferences.getInt("agedOff", 0));
    }

    private synchronized void loadStatistics()
    {
        if(mStatisticsLoaded) { return; }
        mStatistics = statisticsNode(getBroadcastConfiguration());
        BroadcastStatistics saved = getPersistedStatistics(getBroadcastConfiguration());
        mStreamedAudioCount = saved.streamed();
        mDuplicateRejectedAudioCount = saved.duplicateRejected();
        mErrorAudioCount = saved.errors();
        mAgedOffAudioCount = saved.agedOff();
        mStatisticsLoaded = true;
    }

    private void persistStatistics()
    {
        if(mStatistics == null) { return; }
        mStatistics.putInt("streamed", mStreamedAudioCount);
        mStatistics.putInt("duplicates", mDuplicateRejectedAudioCount);
        mStatistics.putInt("errors", mErrorAudioCount);
        mStatistics.putInt("agedOff", mAgedOffAudioCount);
    }

    private static Preferences statisticsNode(BroadcastConfiguration configuration)
    {
        return Preferences.userNodeForPackage(AbstractAudioBroadcaster.class).node("statistics")
            .node(configuration.getPersistentStatisticsKey());
    }
}
