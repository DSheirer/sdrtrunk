/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.call;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.CallRecorder;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the life-cycle of every call: the single audio segment listener for calls.
 *
 * Each audio segment is a call.  The call manager creates the call record as soon as the call has audio (or is
 * identified as encrypted), updates it as the call identifiers change and completes it when the call ends.  Each
 * change is saved to the call database and broadcast to the call listeners (Calls tab, and later external
 * subscribers), so they see the same call activity.  The {@link CallRecorder} records the call audio and fills in
 * the recording details of the call record.
 *
 * Calls are processed on a single call manager thread every 200 milliseconds.
 */
public class CallManager implements Listener<AudioSegment>
{
    private final static Logger mLog = LoggerFactory.getLogger(CallManager.class);
    private static final long PROCESSING_INTERVAL_MS = 200;
    private final LinkedTransferQueue<AudioSegment> mNewAudioSegmentQueue = new LinkedTransferQueue<>();
    private final List<TrackedCall> mTrackedCalls = new ArrayList<>();
    private final Broadcaster<CallRecord> mCallBroadcaster = new Broadcaster<>();
    private final ScheduledExecutorService mExecutor =
        Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk call manager"));
    private final UserPreferences mUserPreferences;
    private final DatabaseManager mDatabaseManager;
    private final CallRecorder mCallRecorder;
    private ScheduledFuture<?> mProcessorHandle;

    /**
     * Constructs an instance
     * @param userPreferences for call recording and identifier formatting
     * @param databaseManager to store the calls, or null to track and record calls without storing them
     */
    public CallManager(UserPreferences userPreferences, DatabaseManager databaseManager)
    {
        mUserPreferences = userPreferences;
        mDatabaseManager = databaseManager;
        mCallRecorder = new CallRecorder(userPreferences);
    }

    /**
     * Starts the manager and begins call tracking and audio recording.
     */
    public void start()
    {
        if(mProcessorHandle == null)
        {
            mProcessorHandle = mExecutor.scheduleWithFixedDelay(this::processSafely, 0, PROCESSING_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the manager, completing any calls and recordings that are still in progress.
     */
    public void stop()
    {
        if(mProcessorHandle != null)
        {
            mProcessorHandle.cancel(false);
            mProcessorHandle = null;

            try
            {
                mExecutor.submit(() -> {
                    process();

                    for(TrackedCall trackedCall: mTrackedCalls)
                    {
                        trackedCall.complete();
                    }

                    mTrackedCalls.clear();
                }).get(10, TimeUnit.SECONDS);
            }
            catch(Exception e)
            {
                mLog.error("Error completing calls in progress during shutdown", e);
            }

            mExecutor.shutdownNow();
        }
    }

    /**
     * Registers the listener to receive a snapshot of each call when it starts, changes and completes.  Snapshots
     * of the same call share the same {@link CallRecord#getCallKey()}, which is the {@link AudioSegment#getId()} of
     * the call's audio segment.  Notified on the call recorder thread.
     */
    public void addCallListener(Listener<CallRecord> listener)
    {
        mCallBroadcaster.addListener(listener);
    }

    public void removeCallListener(Listener<CallRecord> listener)
    {
        mCallBroadcaster.removeListener(listener);
    }

    /**
     * Primary receive method for incoming audio segments.  The consumer count of the segment is already incremented
     * for this manager.
     */
    @Override
    public void receive(AudioSegment audioSegment)
    {
        mNewAudioSegmentQueue.add(audioSegment);
    }

    private void processSafely()
    {
        try
        {
            process();
        }
        catch(Throwable t)
        {
            mLog.error("Error while processing calls", t);
        }
    }

    /**
     * Processes new and tracked audio segments.  Call recorder thread only.
     */
    private void process()
    {
        AudioSegment audioSegment = mNewAudioSegmentQueue.poll();

        while(audioSegment != null)
        {
            mTrackedCalls.add(new TrackedCall(audioSegment, audioSegment.getId()));
            audioSegment = mNewAudioSegmentQueue.poll();
        }

        Iterator<TrackedCall> it = mTrackedCalls.iterator();

        while(it.hasNext())
        {
            TrackedCall trackedCall = it.next();

            if(trackedCall.process())
            {
                it.remove();
            }
        }
    }

    /**
     * Updates the call record fields from the audio segment.
     */
    void update(CallRecord call, AudioSegment audioSegment)
    {
        IdentifierCollection collection = audioSegment.getIdentifierCollection();
        AliasList aliasList = audioSegment.getAliasList();
        Identifier to = collection.getToIdentifier();
        Identifier from = collection.getFromIdentifier();

        Protocol protocol = to != null ? to.getProtocol() : (from != null ? from.getProtocol() : null);

        if(protocol == null || protocol == Protocol.UNKNOWN)
        {
            Identifier decoder = collection.getIdentifier(IdentifierClass.CONFIGURATION, Form.DECODER_TYPE, Role.ANY);

            if(decoder instanceof DecoderTypeConfigurationIdentifier dtci && dtci.getValue() != null)
            {
                protocol = dtci.getValue().getProtocol();
            }
        }

        call.setProtocol(protocol != null ? protocol.name() : null);
        call.setSystem(getString(collection, Form.SYSTEM));
        call.setSite(getString(collection, Form.SITE));
        call.setChannel(getString(collection, Form.CHANNEL));

        Identifier frequency = collection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);

        if(frequency instanceof FrequencyConfigurationIdentifier fci && fci.getValue() != null)
        {
            call.setFrequency(fci.getValue());
        }

        call.setTimeslot(audioSegment.getTimeslot());

        var format = mUserPreferences.getTalkgroupFormatPreference();
        call.setToId(to != null ? format.format(to) : null);
        call.setToValue(getValue(to));
        call.setToForm(to != null ? to.getForm().name() : null);
        call.setToFullyQualified(to instanceof FullyQualifiedTalkgroupIdentifier fqt ?
            fqt.getFullyQualifiedTalkgroupAddress() : null);
        call.setFromId(from != null ? format.format(from) : null);
        call.setFromValue(getValue(from));
        call.setFromForm(from != null ? from.getForm().name() : null);
        call.setFromFullyQualified(from instanceof FullyQualifiedRadioIdentifier fqr ?
            fqr.getFullyQualifiedRadioAddress() : null);
        call.setAliasList(aliasList != null ? aliasList.getName() : null);
        call.setPatchedTalkgroups(getPatchedTalkgroups(to));

        List<Identifier> talkerAliases = collection.getIdentifiers(Form.TALKER_ALIAS);
        call.setTalkerAlias(talkerAliases.isEmpty() ? null : talkerAliases.getFirst().toString());
        call.setEncrypted(audioSegment.isEncrypted());
        call.setDuplicate(audioSegment.isDuplicate());
        call.setPriority(audioSegment.monitorPriorityProperty().get());
        call.setAliasRecord(audioSegment.recordAudioProperty().get());
        call.setStartTime(audioSegment.getStartTimestamp());
    }

    /**
     * Talkgroups patched into the patch group TO identifier, or an empty list.
     */
    static List<PatchedTalkgroup> getPatchedTalkgroups(Identifier to)
    {
        if(!(to instanceof PatchGroupIdentifier pgi) || pgi.getValue() == null)
        {
            return List.of();
        }

        List<PatchedTalkgroup> patched = new ArrayList<>();

        for(TalkgroupIdentifier talkgroup: pgi.getValue().getPatchedTalkgroupIdentifiers())
        {
            if(talkgroup != null && talkgroup.getValue() != null)
            {
                patched.add(new PatchedTalkgroup(talkgroup.getValue(),
                    talkgroup instanceof FullyQualifiedTalkgroupIdentifier fqt ?
                        fqt.getFullyQualifiedTalkgroupAddress() : null));
            }
        }

        return patched;
    }

    private static String getString(IdentifierCollection collection, Form form)
    {
        Identifier identifier = collection.getIdentifier(IdentifierClass.CONFIGURATION, form, Role.ANY);
        return identifier != null && identifier.getValue() != null ? identifier.getValue().toString() : null;
    }

    /**
     * Numeric value of a talkgroup, patch group (the patch group talkgroup) or radio identifier, or null.
     */
    static Long getValue(Identifier identifier)
    {
        if(identifier instanceof PatchGroupIdentifier pgi && pgi.getValue() != null &&
            pgi.getValue().getPatchGroup() != null)
        {
            return pgi.getValue().getPatchGroup().getValue().longValue();
        }

        if(identifier instanceof IntegerIdentifier ii && ii.getValue() != null)
        {
            return ii.getValue().longValue();
        }

        return null;
    }

    /**
     * Content of a call snapshot that is compared to detect a change that needs to be saved and broadcast.
     */
    private static String getSignature(CallRecord call)
    {
        return call.getStartTime() + "|" + call.getCallState() + "|" + call.getProtocol() + "|" + call.getSystem() +
            "|" + call.getSite() + "|" + call.getChannel() + "|" + call.getFrequency() + "|" + call.getToId() + "|" +
            call.getToForm() + "|" + call.getFromId() + "|" + call.getFromForm() + "|" + call.getAliasList() + "|" + call.getPatchedTalkgroups() + "|" +
            call.getTalkerAlias() + "|" +
            call.isEncrypted() + "|" + call.isDuplicate() + "|" + call.getPriority() + "|" + call.isAliasRecord() + "|" +
            call.getRecordingState() + "|" + call.getFilePath() + "|" + call.getEndTime();
    }

    /**
     * A call that is being tracked and possibly recorded.  Call recorder thread only.
     */
    private class TrackedCall
    {
        private final AudioSegment mAudioSegment;
        private final CallRecord mCall = new CallRecord();
        private final DatabaseManager.CallHandle mHandle;
        private final CallRecorder.Recording mRecording;
        private String mLastSignature;
        private boolean mPublished;

        TrackedCall(AudioSegment audioSegment, long callKey)
        {
            mAudioSegment = audioSegment;
            mRecording = mCallRecorder.createRecording(audioSegment);
            mCall.setCallKey(callKey);
            mHandle = mDatabaseManager != null ? mDatabaseManager.createHandle() : null;
        }

        /**
         * Processes the call.
         * @return true when the call is complete and should no longer be tracked
         */
        boolean process()
        {
            boolean complete = mAudioSegment.isComplete();
            boolean hasAudio = mAudioSegment.hasAudio();

            if(complete)
            {
                if(hasAudio || mPublished || mAudioSegment.isEncrypted())
                {
                    complete();
                }

                mAudioSegment.decrementConsumerCount();
                return true;
            }

            //Wait for audio (or an encrypted call) before creating the call, so that segments that never have audio
            //don't create empty calls
            if(!hasAudio && !mAudioSegment.isEncrypted())
            {
                return false;
            }

            update(mCall, mAudioSegment);
            mRecording.update(mCall);
            publish();
            return false;
        }

        /**
         * Completes the call and its recording.
         */
        void complete()
        {
            update(mCall, mAudioSegment);
            mRecording.complete(mCall);
            mCall.setEndTime(mAudioSegment.hasAudio() ? mAudioSegment.getEndTimestamp() : System.currentTimeMillis());
            mCall.setCallState(CallState.COMPLETE);
            publish();
        }

        /**
         * Saves and broadcasts a snapshot of the call when it changed.
         */
        private void publish()
        {
            String signature = getSignature(mCall);

            if(!Objects.equals(signature, mLastSignature))
            {
                mLastSignature = signature;
                CallRecord snapshot = mCall.copy();
                mPublished = true;

                if(mDatabaseManager != null)
                {
                    //The database writer sets the row id on its own copy
                    mDatabaseManager.save(mHandle, snapshot.copy());
                }

                mCallBroadcaster.broadcast(snapshot);
            }
        }
    }
}
