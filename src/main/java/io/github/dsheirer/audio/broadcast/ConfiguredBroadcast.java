/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite observable object that joins a broadcast configuration and (optional) constructed audio broadcaster
 */
public class ConfiguredBroadcast
{
    private final static Logger mLog = LoggerFactory.getLogger(ConfiguredBroadcast.class);
    private BroadcastConfiguration mBroadcastConfiguration;
    private AbstractAudioBroadcaster mAudioBroadcaster;
    private ObjectProperty<BroadcastState> mBroadcastState = new SimpleObjectProperty<>();
    private ObjectProperty<BroadcastState> mLastBadBroadcastState = new SimpleObjectProperty<>();

    /**
     * Constructs an instance
     * @param broadcastConfiguration for the instance
     */
    public ConfiguredBroadcast(BroadcastConfiguration broadcastConfiguration)
    {
        mBroadcastConfiguration = broadcastConfiguration;
        mBroadcastConfiguration.validProperty().addListener((observable, oldValue, newValue) -> updateBroadcastState());
        updateBroadcastState();
    }

    /**
     * Configuration for this broadcast
     */
    public BroadcastConfiguration getBroadcastConfiguration()
    {
        return mBroadcastConfiguration;
    }

    /**
     * Enabled state of the configuration
     */
    public BooleanProperty enabledProperty()
    {
        return mBroadcastConfiguration.enabledProperty();
    }

    /**
     * Name of the broadcast configuration
     */
    public StringProperty nameProperty()
    {
        return mBroadcastConfiguration.nameProperty();
    }

    /**
     * Server type for the broadcast configuration
     */
    public BroadcastServerType getBroadcastServerType()
    {
        return mBroadcastConfiguration.getBroadcastServerType();
    }

    /**
     * Broadcast state of the configured audio broadcaster (optional)
     */
    public ObjectProperty<BroadcastState> broadcastStateProperty()
    {
        return mBroadcastState;
    }

    /**
     * Last bad broadcast state of the configured audio broadcaster (optional)
     */
    public ObjectProperty<BroadcastState> lastBadBroadcastStateProperty()
    {
        return mLastBadBroadcastState;
    }

    /**
     * Sets the audio broadcaster
     * @param audioBroadcaster to use for this configuration
     */
    public void setAudioBroadcaster(AbstractAudioBroadcaster audioBroadcaster)
    {
        mBroadcastState.unbind();
        mLastBadBroadcastState.unbind();
        mAudioBroadcaster = audioBroadcaster;

        if(audioBroadcaster != null)
        {
            mBroadcastState.bind(mAudioBroadcaster.broadcastStateProperty());
            mLastBadBroadcastState.bind(mAudioBroadcaster.lastBadBroadcastStateProperty());
        }
        else
        {
            updateBroadcastState();
        }
    }

    private void updateBroadcastState()
    {
        if(!mBroadcastState.isBound())
        {
            if(mBroadcastConfiguration.isValid())
            {
                mBroadcastState.setValue(BroadcastState.READY);
            }
            else
            {
                mBroadcastState.setValue(BroadcastState.CONFIGURATION_ERROR);
            }
            mLastBadBroadcastState.setValue(null);
        }
    }

    /**
     * Optional audio broadcaster created from the configuration
     */
    public AbstractAudioBroadcaster getAudioBroadcaster()
    {
        return mAudioBroadcaster;
    }

    /**
     * Indicates if this configured broadcast has a non-null audio broadcaster assigned
     */
    public boolean hasAudioBroadcaster()
    {
        return mAudioBroadcaster != null;
    }

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<ConfiguredBroadcast, Observable[]> extractor()
    {
        return (ConfiguredBroadcast b) -> new Observable[] {b.nameProperty(), b.enabledProperty(),
            b.broadcastStateProperty(), b.getBroadcastConfiguration().validProperty()};
    }
}
