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

package io.github.dsheirer.record;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.StringUtils;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors audio segments and upon completion records any audio segments that have been flagged as recordable
 */
public class AudioRecordingManager implements Listener<AudioSegment>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioRecordingManager.class);
    private LinkedTransferQueue<AudioSegment> mCompletedAudioSegmentQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mQueueProcessorHandle;
    private UserPreferences mUserPreferences;
    private int mUnknownAudioRecordingIndex = 1;

    /**
     * Constructs an instance
     * @param userPreferences to determine audio recording format
     */
    public AudioRecordingManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    /**
     * Starts the manager and begins audio segment recording.
     */
    public void start()
    {
        if(mQueueProcessorHandle == null)
        {
            mQueueProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(new QueueProcessor(),
                0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops the manager and records any remaining queued audio segments.
     */
    public void stop()
    {
        if(mQueueProcessorHandle != null)
        {
            mQueueProcessorHandle.cancel(true);
            processAudioSegments();
            mQueueProcessorHandle = null;
        }
    }

    /**
     * Primary receive method for incoming audio segments to be recorded
     */
    @Override
    public void receive(AudioSegment audioSegment)
    {
        audioSegment.completeProperty().addListener(new AudioSegmentCompletionMonitor(audioSegment));
    }

    /**
     * Processes audio segments that have been flagged as complete.
     * @param audioSegment
     */
    public void processCompletedAudioSegment(AudioSegment audioSegment)
    {
        //Debug
        if(audioSegment.getAudioBufferCount() == 0)
        {
            mLog.debug("Audio Segment detected with 0 audio buffers");
        }

        List<Identifier> toIdentifiers = audioSegment.getIdentifierCollection().getIdentifiers(Role.TO);

        if(toIdentifiers.isEmpty())
        {
            mLog.debug("Audio Segment detected with NO TO identifiers");
        }

        if(audioSegment.recordAudioProperty().get())
        {
            mCompletedAudioSegmentQueue.add(audioSegment);
        }
        else
        {
            audioSegment.decrementConsumerCount();
        }
    }

    /**
     * Processes any queued audio segments
     */
    private void processAudioSegments()
    {
        List<AudioSegment> audioSegments = new ArrayList<>();
        mCompletedAudioSegmentQueue.drainTo(audioSegments);

        if(!audioSegments.isEmpty())
        {
            RecordFormat recordFormat = mUserPreferences.getRecordPreference().getAudioRecordFormat();

            for(AudioSegment audioSegment: audioSegments)
            {
                Path path = getAudioRecordingPath(audioSegment.getIdentifierCollection(), recordFormat);

                try
                {
                    AudioSegmentRecorder.record(audioSegment, path, recordFormat);
                }
                catch(IOException ioe)
                {
                    mLog.error("Error recording audio segment to [" + path.toString() + "]");
                }

                audioSegment.decrementConsumerCount();
            }
        }
    }

    /**
     * Base path to recordings folder
     * @return
     */
    public Path getRecordingBasePath()
    {
        return mUserPreferences.getDirectoryPreference().getDirectoryRecording();
    }

    /**
     * Provides a formatted audio recording filename to use as the final audio filename.
     */
    private Path getAudioRecordingPath(IdentifierCollection identifierCollection, RecordFormat recordFormat)
    {
        StringBuilder sb = new StringBuilder();

        if(identifierCollection != null)
        {
            Identifier system = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

            if(system != null)
            {
                sb.append(((StringIdentifier)system).getValue()).append("_");
            }

            Identifier site = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);

            if(site != null)
            {
                sb.append(((StringIdentifier)site).getValue()).append("_");
            }

            Identifier channel = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);

            if(channel != null)
            {
                sb.append(((StringIdentifier)channel).getValue()).append("_");
            }

            Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

            if(to != null)
            {
                String toValue = ((IntegerIdentifier)to).getValue().toString().replace(":", "");
                sb.append("_TO_").append(toValue);
            }
            else
            {
                List<Identifier> toIdentifiers = identifierCollection.getIdentifiers(Role.TO);

                if(!toIdentifiers.isEmpty())
                {
                    sb.append("_TO_").append(toIdentifiers.get(0)).toString();
                }
            }

            Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

            if(from != null)
            {
                String fromValue = ((IntegerIdentifier)from).getValue().toString().replace(":", "");
                sb.append("_FROM_").append(fromValue);
            }
            else
            {
                List<Identifier> fromIdentifiers = identifierCollection.getIdentifiers(Role.FROM);

                if(!fromIdentifiers.isEmpty())
                {
                    sb.append("_FROM_").append(fromIdentifiers.get(0)).toString();
                }
            }
        }
        else
        {
            sb.append("audio_recording_no_metadata_").append(mUnknownAudioRecordingIndex++);

            if(mUnknownAudioRecordingIndex < 0)
            {
                mUnknownAudioRecordingIndex = 1;
            }
        }

        StringBuilder sbFinal = new StringBuilder();
        sbFinal.append(TimeStamp.getTimeStamp("_"));

        //Remove any illegal filename characters
        sbFinal.append(StringUtils.replaceIllegalCharacters(sb.toString()));
        sbFinal.append(recordFormat.getExtension());

        return getRecordingBasePath().resolve(sbFinal.toString());
    }


    /**
     * Audio segment completion monitor.  Listens for the audio segment's complete flag to be set and then
     * queues the audio segment for recording.
     */
    public class AudioSegmentCompletionMonitor implements ChangeListener<Boolean>
    {
        private AudioSegment mAudioSegment;

        public AudioSegmentCompletionMonitor(AudioSegment audioSegment)
        {
            mAudioSegment = audioSegment;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            mAudioSegment.completeProperty().removeListener(this);
            processCompletedAudioSegment(mAudioSegment);
        }
    }

    /**
     * Threaded queue processor to process/record each recordable audio segment
     */
    public class QueueProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processAudioSegments();
            }
            catch(Throwable t)
            {
                mLog.error("Error while processing queued audio segments to recordings", t);
            }
        }
    }
}
