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
import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.settings.Settings;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private Settings mSettings;
    private StreamActivityModel mStreamActivityModel;
    private ScheduledFuture<?> mAudioSegmentProcessorFuture;
    private int mNextRecordingNumber = 1;

    /**
     * Constructs an instance
     * @param listener to receive completed audio recordings
     * @param broadcastFormat for temporary recordings
     * @param settings to manage recording directories
     */
    public AudioStreamingManager(Listener<AudioRecording> listener, BroadcastFormat broadcastFormat, Settings settings)
    {
        this(listener, broadcastFormat, settings, null);
    }

    /**
     * Constructs an instance
     * @param listener to receive completed audio recordings
     * @param broadcastFormat for temporary recordings
     * @param settings to manage recording directories
     * @param streamActivityModel to receive a row for each call and stream that the call is selected for, or null
     */
    public AudioStreamingManager(Listener<AudioRecording> listener, BroadcastFormat broadcastFormat, Settings settings,
                                 StreamActivityModel streamActivityModel)
    {
        mAudioRecordingListener = listener;
        mBroadcastFormat = broadcastFormat;
        mSettings = settings;
        mStreamActivityModel = streamActivityModel;
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

            if(audioSegment.isDuplicate() && mSettings.getCallManagementSettings().isDuplicateStreamingSuppressionEnabled())
            {
                it.remove();

                if(audioSegment.hasBroadcastChannels())
                {
                    IdentifierCollection identifiers =
                        new IdentifierCollection(audioSegment.getIdentifierCollection().getIdentifiers());
                    addActivities(audioSegment, identifiers, audioSegment.getBroadcastChannels(), null,
                        StreamStatus.SUPPRESSED, "Duplicate call - duplicate call streaming is suppressed " +
                            "(Call Management settings)");
                }

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
                        if(mSettings.getCallManagementSettings()
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
                                Set<BroadcastChannel> decomposedChannels = new HashSet<>();

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
                                        decomposedChannels.addAll(broadcastChannels);
                                        processAudioSegment(audioSegment, decomposedIdentifiers, broadcastChannels,
                                            getSelectingAliases(aliasList, identifier, "Patched"));
                                    }
                                }

                                //Streams of the patch group aliases that no patched talkgroup or radio streams to
                                Set<BroadcastChannel> notStreamed = new HashSet<>(audioSegment.getBroadcastChannels());
                                notStreamed.removeAll(decomposedChannels);

                                if(!notStreamed.isEmpty())
                                {
                                    addActivities(audioSegment, identifiers, notStreamed, null, StreamStatus.SUPPRESSED,
                                        "Patch group calls are streamed as the patched talkgroups (Call Management " +
                                            "settings) and no patched talkgroup or radio has an alias for this stream");
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
        processAudioSegment(audioSegment, identifierCollection, broadcastChannels, null);
    }

    /**
     * Processes an audio segment for streaming by creating a temporary MP3 recording and submitting the recording
     * to the specific broadcast channel(s).
     * @param audioSegment to process for streaming
     * @param identifierCollection to use for the streamed audio recording.
     * @param broadcastChannels to receive the audio recording
     * @param selectingAliases by stream, or null to use the aliases that added each broadcast channel to the segment
     */
    private void processAudioSegment(AudioSegment audioSegment, IdentifierCollection identifierCollection,
                                     Set<BroadcastChannel> broadcastChannels,
                                     Map<BroadcastChannel,List<StreamActivity.SelectingAlias>> selectingAliases)
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
            AudioSegmentRecorder.record(audioSegment, path, RecordFormat.MP3, mSettings, identifierCollection);

            AudioRecording audioRecording = new AudioRecording(path, broadcastChannels, identifierCollection,
                    audioSegment.getStartTimestamp(), length);

            if(mStreamActivityModel != null)
            {
                audioRecording.setStreamActivities(mStreamActivityModel, addActivities(audioSegment,
                    identifierCollection, broadcastChannels, selectingAliases, StreamStatus.PENDING, null));
            }

            mAudioRecordingListener.receive(audioRecording);
        }
        catch(IOException ioe)
        {
            mLog.error("Error recording temporary stream MP3");
            addActivities(audioSegment, identifierCollection, broadcastChannels, selectingAliases, StreamStatus.ERROR,
                "Error creating the temporary streaming recording: " + ioe.getMessage());
        }
    }

    /**
     * Adds a stream activity row for each broadcast channel.
     * @param audioSegment of the call
     * @param identifierCollection of the streamed call
     * @param broadcastChannels that the call is selected for
     * @param selectingAliases by stream, or null to use the aliases that added each broadcast channel to the segment
     * @param status of the rows
     * @param detail of the status, or null
     * @return rows by stream name
     */
    private Map<String,StreamActivity> addActivities(AudioSegment audioSegment,
                                                     IdentifierCollection identifierCollection,
                                                     Set<BroadcastChannel> broadcastChannels,
                                                     Map<BroadcastChannel,List<StreamActivity.SelectingAlias>> selectingAliases,
                                                     StreamStatus status, String detail)
    {
        Map<String,StreamActivity> activities = new LinkedHashMap<>();

        if(mStreamActivityModel == null)
        {
            return activities;
        }

        try
        {
            StreamActivity.Builder call = createCall(audioSegment, identifierCollection);

            for(BroadcastChannel broadcastChannel: broadcastChannels)
            {
                String streamName = broadcastChannel.getChannelName();

                if(streamName != null && !activities.containsKey(streamName))
                {
                    List<StreamActivity.SelectingAlias> aliases = selectingAliases != null ?
                        selectingAliases.get(broadcastChannel) : getSelectingAliases(audioSegment, broadcastChannel);
                    call.selectingAliases(aliases);
                    activities.put(streamName, mStreamActivityModel.add(call, streamName, null, status, detail));
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error adding streaming activity for call", e);
        }

        return activities;
    }

    /**
     * Call details for the stream activity rows.
     */
    private StreamActivity.Builder createCall(AudioSegment audioSegment, IdentifierCollection identifierCollection)
    {
        Identifier to = identifierCollection.getToIdentifier();
        Identifier from = identifierCollection.getFromIdentifier();
        Protocol protocol = to != null ? to.getProtocol() : (from != null ? from.getProtocol() : null);

        return new StreamActivity.Builder()
            .callKey(audioSegment.getId())
            .callStart(audioSegment.getStartTimestamp())
            .duration(getDuration(audioSegment))
            .to(format(to))
            .from(format(from))
            .aliasList(audioSegment.getAliasList() != null ? audioSegment.getAliasList().getName() : null)
            .protocol(protocol != null && protocol != Protocol.UNKNOWN ? protocol.toString() : null)
            .system(getString(identifierCollection, Form.SYSTEM))
            .site(getString(identifierCollection, Form.SITE))
            .channel(getString(identifierCollection, Form.CHANNEL));
    }

    /**
     * Aliases that added the broadcast channel to the audio segment.
     */
    private List<StreamActivity.SelectingAlias> getSelectingAliases(AudioSegment audioSegment,
                                                                    BroadcastChannel broadcastChannel)
    {
        List<StreamActivity.SelectingAlias> aliases = new ArrayList<>();

        for(Map.Entry<Alias,Identifier> entry: audioSegment.getBroadcastChannelAliases(broadcastChannel).entrySet())
        {
            aliases.add(new StreamActivity.SelectingAlias(entry.getKey().getName(), describe(entry.getValue(), null)));
        }

        return aliases;
    }

    /**
     * Aliases of the identifier that stream to each broadcast channel.
     * @param aliasList with the aliases
     * @param identifier to alias
     * @param label for the identifier, e.g. Patched, or null to label it with its role
     */
    private Map<BroadcastChannel,List<StreamActivity.SelectingAlias>> getSelectingAliases(AliasList aliasList,
                                                                                         Identifier identifier,
                                                                                         String label)
    {
        Map<BroadcastChannel,List<StreamActivity.SelectingAlias>> aliasesByChannel = new LinkedHashMap<>();

        for(Alias alias: aliasList.getAliases(identifier))
        {
            for(BroadcastChannel broadcastChannel: alias.getBroadcastChannels())
            {
                aliasesByChannel.computeIfAbsent(broadcastChannel, key -> new ArrayList<>())
                    .add(new StreamActivity.SelectingAlias(alias.getName(), describe(identifier, label)));
            }
        }

        return aliasesByChannel;
    }

    /**
     * Describes the identifier that an alias matched, e.g. To 1234
     */
    private String describe(Identifier identifier, String label)
    {
        if(identifier == null)
        {
            return null;
        }

        if(label == null)
        {
            label = switch(identifier.getRole())
            {
                case TO -> "To";
                case FROM -> "From";
                default -> identifier.getForm() != null ? identifier.getForm().toString() : null;
            };
        }

        String value = format(identifier);
        return label != null ? label + " " + value : value;
    }

    private String format(Identifier identifier)
    {
        if(identifier == null)
        {
            return null;
        }

        try
        {
            return mSettings.getTalkgroupFormatSettings().format(identifier);
        }
        catch(Exception e)
        {
            return String.valueOf(identifier);
        }
    }

    private static String getString(IdentifierCollection collection, Form form)
    {
        Identifier identifier = collection.getIdentifier(IdentifierClass.CONFIGURATION, form, Role.ANY);
        return identifier != null && identifier.getValue() != null ? identifier.getValue().toString() : null;
    }

    /**
     * Audio duration of the segment in milliseconds (8 kHz audio)
     */
    private static long getDuration(AudioSegment audioSegment)
    {
        long samples = 0;

        for(float[] audioBuffer: audioSegment.getAudioBuffers())
        {
            samples += audioBuffer.length;
        }

        return samples / 8;
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

        Path temporaryRecordingPath = mSettings.getDirectorySettings().getDirectoryStreaming().resolve(sb.toString());

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
