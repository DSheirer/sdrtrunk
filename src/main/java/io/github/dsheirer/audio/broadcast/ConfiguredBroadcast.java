/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.audio.broadcast;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;

/**
 * Composite observable object that joins a broadcast configuration and (optional) constructed audio broadcaster
 */
public class ConfiguredBroadcast
{
    private BroadcastConfiguration mBroadcastConfiguration;
    private AudioBroadcaster mAudioBroadcaster;
    private ObjectProperty<BroadcastState> mBroadcastState = new SimpleObjectProperty<>();

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
     * Sets the audio broadcaster
     * @param audioBroadcaster to use for this configuration
     */
    public void setAudioBroadcaster(AudioBroadcaster audioBroadcaster)
    {
        mBroadcastState.unbind();
        mAudioBroadcaster = audioBroadcaster;

        if(audioBroadcaster != null)
        {
            mBroadcastState.bind(mAudioBroadcaster.broadcastStateProperty());
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
        }
    }

    /**
     * Optional audio broadcaster created from the configuration
     */
    public AudioBroadcaster getAudioBroadcaster()
    {
        return mAudioBroadcaster;
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
