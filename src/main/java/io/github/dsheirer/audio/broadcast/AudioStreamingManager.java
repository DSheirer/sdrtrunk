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

package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.AudioSegmentRecorder;
import io.github.dsheirer.record.RecordFormat;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio streaming manager monitors audio segments through completion and creates temporary streaming recordings on
 * disk and enqueues the temporary recording for streaming.
 */
public class AudioStreamingManager implements Listener<AudioSegment>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioStreamingManager.class);
    private LinkedTransferQueue<AudioSegment> mNewAudioSegments = new LinkedTransferQueue<>();
    private List<AudioSegment> mAudioSegments = new ArrayList<>();
    private Listener<AudioRecording> mAudioRecordingListener;
    private BroadcastFormat mBroadcastFormat;
    private UserPreferences mUserPreferences;
    private ScheduledFuture<?> mAudioSegmentProcessorFuture;
    private int mNextRecordingNumber = 1;

    /**
     * Constructs an instance
     * @param listener to receive completed audio recordings
     * @param broadcastFormat for temporary recordings
     * @param userPreferences to manage recording directories
     */
    public AudioStreamingManager(Listener<AudioRecording> listener, BroadcastFormat broadcastFormat, UserPreferences userPreferences)
    {
        mAudioRecordingListener = listener;
        mBroadcastFormat = broadcastFormat;
        mUserPreferences = userPreferences;
    }

    /**
     * Primary receive method
     */
    @Override
    public void receive(AudioSegment audioSegment)
    {
        mNewAudioSegments.add(audioSegment);
    }

    /**
     * Starts the scheduled audio segment processor
     */
    public void start()
    {
        if(mAudioSegmentProcessorFuture == null)
        {
            mAudioSegmentProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new AudioSegmentProcessor(),
                0, 250, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the scheduled audio segment processor
     */
    public void stop()
    {
        if(mAudioSegmentProcessorFuture != null)
        {
            mAudioSegmentProcessorFuture.cancel(true);
            mAudioSegmentProcessorFuture = null;
        }

        for(AudioSegment audioSegment: mNewAudioSegments)
        {
            audioSegment.decrementConsumerCount();
        }

        mNewAudioSegments.clear();

        for(AudioSegment audioSegment: mAudioSegments)
        {
            audioSegment.decrementConsumerCount();
        }

        mAudioSegments.clear();
    }

    /**
     * Main processing method to process audio segments
     */
    private void processAudioSegments()
    {
        mNewAudioSegments.drainTo(mAudioSegments);

        Iterator<AudioSegment> it = mAudioSegments.iterator();
        AudioSegment audioSegment;
        while(it.hasNext())
        {
            audioSegment = it.next();

            if(audioSegment.isDuplicate() && mUserPreferences.getCallManagementPreference().isDuplicateStreamingSuppressionEnabled())
            {
                it.remove();
                audioSegment.decrementConsumerCount();
            }
            else if(audioSegment.completeProperty().get())
            {
                it.remove();

                if(mAudioRecordingListener != null && audioSegment.hasBroadcastChannels())
                {
                    IdentifierCollection identifiers =
                            new IdentifierCollection(audioSegment.getIdentifierCollection().getIdentifiers());

                    if(identifiers.getToIdentifier() instanceof PatchGroupIdentifier patchGroupIdentifier)
                    {
                        if(mUserPreferences.getCallManagementPreference()
                                .getPatchGroupStreamingOption() == PatchGroupStreamingOption.TALKGROUPS)
                        {
                            //Decompose the patch group into the individual (patched) talkgroups and process the audio
                            //segment for each patched talkgroup.
                            PatchGroup patchGroup = patchGroupIdentifier.getValue();

                            List<Identifier> ids = new ArrayList<>();
                            ids.addAll(patchGroup.getPatchedTalkgroupIdentifiers());
                            ids.addAll(patchGroup.getPatchedRadioIdentifiers());

                            //If there are no patched radios/talkgroups, override user preference and stream as a patch group
                            if(ids.isEmpty() || audioSegment.getAliasList() == null)
                            {
                                processAudioSegment(audioSegment, identifiers, audioSegment.getBroadcastChannels());
                            }
                            else
                            {
                                AliasList aliasList = audioSegment.getAliasList();

                                for(Identifier identifier: ids)
                                {
                                    List<Alias> aliases = aliasList.getAliases(identifier);
                                    Set<BroadcastChannel> broadcastChannels = new HashSet<>();
                                    for(Alias alias: aliases)
                                    {
                                        broadcastChannels.addAll(alias.getBroadcastChannels());
                                    }

                                    if(!broadcastChannels.isEmpty())
                                    {
                                        MutableIdentifierCollection decomposedIdentifiers =
                                                new MutableIdentifierCollection(identifiers.getIdentifiers());
                                        //Remove patch group TO identifier & replace with the patched talkgroup/radio
                                        decomposedIdentifiers.remove(Role.TO);
                                        decomposedIdentifiers.update(identifier);
                                        processAudioSegment(audioSegment, decomposedIdentifiers, broadcastChannels);
                                    }
                                }
                            }
                        }
                        else
                        {
                            processAudioSegment(audioSegment, identifiers, audioSegment.getBroadcastChannels());
                        }
                    }
                    else
                    {
                        processAudioSegment(audioSegment, identifiers, audioSegment.getBroadcastChannels());
                    }
                }

                audioSegment.decrementConsumerCount();
            }
        }
    }

    /**
     * Processes an audio segment for streaming by creating a temporary MP3 recording and submitting the recording
     * to the specific broadcast channel(s).
     * @param audioSegment to process for streaming
     * @param identifierCollection to use for the streamed audio recording.
     * @param broadcastChannels to receive the audio recording
     */
    private void processAudioSegment(AudioSegment audioSegment, IdentifierCollection identifierCollection,
                                     Set<BroadcastChannel> broadcastChannels)
    {
        Path path = getTemporaryRecordingPath();
        long length = 0;

        for(float[] audioBuffer: audioSegment.getAudioBuffers())
        {
            length += audioBuffer.length;
        }

        length /= 8; //Sample rate is 8000 samples per second, or 8 samples per millisecond.

        try
        {
            AudioSegmentRecorder.record(audioSegment, path, RecordFormat.MP3, mUserPreferences, identifierCollection);

            AudioRecording audioRecording = new AudioRecording(path, broadcastChannels, identifierCollection,
                    audioSegment.getStartTimestamp(), length);
            mAudioRecordingListener.receive(audioRecording);
        }
        catch(IOException ioe)
        {
            mLog.error("Error recording temporary stream MP3");
        }
    }

    /**
     * Creates a temporary streaming recording file path
     */
    private Path getTemporaryRecordingPath()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(BroadcastModel.TEMPORARY_STREAM_FILE_SUFFIX);

        //Check for integer overflow and readjust negative value to 0
        if(mNextRecordingNumber < 0)
        {
            mNextRecordingNumber = 1;
        }

        int recordingNumber = mNextRecordingNumber++;

        sb.append(recordingNumber).append("_");
        sb.append(TimeStamp.getLongTimeStamp("_"));
        sb.append(mBroadcastFormat.getFileExtension());

        Path temporaryRecordingPath = mUserPreferences.getDirectoryPreference().getDirectoryStreaming().resolve(sb.toString());

        return temporaryRecordingPath;
    }

    /**
     * Scheduled runnable to process audio segments.
     */
    public class AudioSegmentProcessor implements Runnable
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
                mLog.error("Error processing audio segments for streaming", t);
            }
        }
    }
}
