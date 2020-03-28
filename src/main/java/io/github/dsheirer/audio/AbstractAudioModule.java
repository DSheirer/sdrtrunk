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

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base audio module implementation.
 */
public abstract class AbstractAudioModule extends Module implements IAudioSegmentProvider, IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractAudioModule.class);
    private static final int MAX_SEGMENT_AUDIO_SAMPLE_LENGTH = 8000 * 60 * 1; //8 kHz - 1 minute

    private Listener<AudioSegment> mAudioSegmentListener;
    protected MutableIdentifierCollection mIdentifierCollection = new MutableIdentifierCollection();
    private Broadcaster<IdentifierUpdateNotification> mIdentifierUpdateNotificationBroadcaster = new Broadcaster<>();
    private AliasList mAliasList;
    private AudioSegment mAudioSegment;
    private int mAudioSampleCount = 0;
    private boolean mRecordAudioOverride;

    /**
     * Constructs an abstract audio module
     */
    public AbstractAudioModule(AliasList aliasList)
    {
        mAliasList = aliasList;
        mIdentifierUpdateNotificationBroadcaster.addListener(mIdentifierCollection);
    }

    protected abstract int getTimeslot();

    /**
     * Closes the current audio segment
     */
    protected void closeAudioSegment()
    {
        synchronized(this)
        {
            if(mAudioSegment != null)
            {
                mAudioSegment.completeProperty().set(true);
                mIdentifierUpdateNotificationBroadcaster.removeListener(mAudioSegment);
                mAudioSegment = null;
            }
        }
    }

    @Override
    public void stop()
    {
        closeAudioSegment();
    }

    /**
     * Gets the current audio segment, or creates a new audio segment as necessary and broadcasts it to any registered
     * listener(s).
     */
    protected AudioSegment getAudioSegment()
    {
        synchronized(this)
        {
            if(mAudioSegment == null)
            {
                mAudioSegment = new AudioSegment(mAliasList, getTimeslot());
                mAudioSegment.addIdentifiers(mIdentifierCollection.getIdentifiers());
                mIdentifierUpdateNotificationBroadcaster.addListener(mAudioSegment);

                if(mRecordAudioOverride)
                {
                    mAudioSegment.recordAudioProperty().set(true);
                }

                if(mAudioSegmentListener != null)
                {
                    mAudioSegment.incrementConsumerCount();
                    mAudioSegmentListener.receive(mAudioSegment);
                }

                mAudioSampleCount = 0;
            }

            return mAudioSegment;
        }
    }

    protected void addAudio(float[] audioBuffer)
    {
        AudioSegment audioSegment = getAudioSegment();

        //If the current segment exceeds the max samples length, close it so that a new segment gets generated
        //and then link the segments together
        if(mAudioSampleCount >= MAX_SEGMENT_AUDIO_SAMPLE_LENGTH)
        {
            AudioSegment previous = getAudioSegment();
            closeAudioSegment();
            audioSegment = getAudioSegment();
            audioSegment.linkTo(previous);
        }

        audioSegment.addAudio(audioBuffer);
        mAudioSampleCount += audioBuffer.length;
    }

    /**
     * Sets all audio segments as recordable when the argument is true.  Otherwise, defers to the aliased identifiers
     * from the identifier collection to determine whether to record the audio or not.
     * @param recordAudio set to true to mark all audio as recordable.
     */
    public void setRecordAudio(boolean recordAudio)
    {
        mRecordAudioOverride = recordAudio;

        if(mRecordAudioOverride)
        {
            synchronized(this)
            {
                if(mAudioSegment != null)
                {
                    mAudioSegment.recordAudioProperty().set(true);
                }
            }
        }
    }

    /**
     * Receive updated identifiers from decoder state(s).
     */
    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return mIdentifierUpdateNotificationBroadcaster;
    }

    /**
     * Identifier collection containing the current set of identifiers received from the decoder state(s).
     */
    public MutableIdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Registers an audio segment listener to receive the output from this audio module.
     */
    @Override
    public void setAudioSegmentListener(Listener<AudioSegment> listener)
    {
        mAudioSegmentListener = listener;
    }

    /**
     * Unregisters the audio segment listener from receiving audio segments from this module.
     */
    @Override
    public void removeAudioSegmentListener()
    {
        mAudioSegmentListener = null;
    }
}
