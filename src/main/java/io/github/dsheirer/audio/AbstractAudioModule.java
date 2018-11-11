/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.audio;

import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;

/**
 * Base audio module implementation.
 */
public abstract class AbstractAudioModule extends Module implements IAudioPacketProvider, IdentifierUpdateListener,
    ISquelchStateListener
{
    //Static unique audio channel identifier.
    private static int AUDIO_CHANNEL_ID_GENERATOR = 1;

    private int mAudioChannelId = AUDIO_CHANNEL_ID_GENERATOR++;
    private Listener<ReusableAudioPacket> mAudioPacketListener;
    private MutableIdentifierCollection mIdentifierCollection = new MutableIdentifierCollection();

    /**
     * Constructs an abstract audio module
     */
    public AbstractAudioModule()
    {
    }

    /**
     * Unique channel identifier for use in tagging audio packets with an audio channel ID so that they can
     * be identified within a combined audio packet stream
     */
    public int getAudioChannelId()
    {
        return mAudioChannelId;
    }

    /**
     * Receive updated identifiers from decoder state(s).
     */
    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return mIdentifierCollection;
    }

    /**
     * Identifier collection containing the current set of identifiers received from the decoder state(s).
     */
    public MutableIdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Registers an audio packet listener to receive the output from this audio module.
     */
    @Override
    public void setAudioPacketListener(Listener<ReusableAudioPacket> listener)
    {
        mAudioPacketListener = listener;
    }

    /**
     * Unregisters the audio packet listener from receiving audio packets from this module.
     */
    @Override
    public void removeAudioPacketListener()
    {
        mAudioPacketListener = null;
    }

    /**
     * Registered listener for receiving audio packets produced by this module
     */
    public Listener<ReusableAudioPacket> getAudioPacketListener()
    {
        return mAudioPacketListener;
    }

    /**
     * Indicates if there is a listener registered to receive audio packets from this audio module.
     */
    public boolean hasAudioPacketListener()
    {
        return mAudioPacketListener != null;
    }

}
