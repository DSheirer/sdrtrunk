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
package io.github.dsheirer.event;

import io.github.dsheirer.call.CallManager;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.settings.identifier.TalkgroupFormatSettings;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Collects the decode events from all channels: saves each event to the database and broadcasts it to the event
 * listeners (Events tab, and later external subscribers), so they see the same event activity.
 *
 * Decoders update an event after it is first broadcast (for example the duration of a call, or the identifiers as
 * they are decoded), sometimes without broadcasting it again.  The manager keeps checking a received event for
 * changes until it hasn't changed for {@link #ACTIVE_WINDOW_MS}, and a later broadcast of the same event resumes the
 * checks.  Events are identified by object identity, so each event has one row that is updated as it changes.
 *
 * Events are processed on a single event manager thread every 500 milliseconds.
 */
public class EventManager implements Listener<IDecodeEvent>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);
    private static final long PROCESSING_INTERVAL_MS = 500;
    static final long ACTIVE_WINDOW_MS = 30_000;
    private final LinkedTransferQueue<IDecodeEvent> mQueue = new LinkedTransferQueue<>();
    /**
     * Tracking state by event.  Weak keys, so that the entry is removed when the decoders no longer reference the
     * event.  The tracking state must not reference the event.
     */
    private final Map<IDecodeEvent,TrackedEvent> mTrackedEvents = new WeakHashMap<>();
    private final List<ActiveEvent> mActiveEvents = new ArrayList<>();
    private final Broadcaster<EventRecord> mEventBroadcaster = new Broadcaster<>();
    private final ScheduledExecutorService mExecutor =
        Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk event manager"));
    private final TalkgroupFormatSettings mTalkgroupFormatSettings;
    private final DatabaseManager mDatabaseManager;
    private long mNextEventKey = 1;
    private ScheduledFuture<?> mProcessorHandle;

    /**
     * Constructs an instance
     * @param talkgroupFormatSettings for formatting the TO and FROM identifiers
     * @param databaseManager to store the events, or null to only broadcast the events
     */
    public EventManager(TalkgroupFormatSettings talkgroupFormatSettings, DatabaseManager databaseManager)
    {
        mTalkgroupFormatSettings = talkgroupFormatSettings;
        mDatabaseManager = databaseManager;
    }

    /**
     * Starts processing events
     */
    public void start()
    {
        if(mProcessorHandle == null)
        {
            mProcessorHandle = mExecutor.scheduleWithFixedDelay(this::processSafely, PROCESSING_INTERVAL_MS,
                PROCESSING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops processing, after saving the final state of the events received so far.
     */
    public void stop()
    {
        if(mProcessorHandle != null)
        {
            mProcessorHandle.cancel(false);
            mProcessorHandle = null;

            try
            {
                mExecutor.submit(this::process).get(10, TimeUnit.SECONDS);
            }
            catch(Exception e)
            {
                LOGGER.error("Error processing the remaining events during shutdown", e);
            }
        }

        mExecutor.shutdownNow();
    }

    /**
     * Registers the listener to receive a snapshot of each event when it is first received and when it changes.
     * Snapshots of the same event share the same {@link EventRecord#getEventKey()}.  Notified on the event manager
     * thread.
     */
    public void addEventListener(Listener<EventRecord> listener)
    {
        mEventBroadcaster.addListener(listener);
    }

    public void removeEventListener(Listener<EventRecord> listener)
    {
        mEventBroadcaster.removeListener(listener);
    }

    /**
     * Receives decode events from all channels.  Invoked on the channel processing threads.
     */
    @Override
    public void receive(IDecodeEvent event)
    {
        if(event != null)
        {
            mQueue.add(event);
        }
    }

    private void processSafely()
    {
        try
        {
            process();
        }
        catch(Throwable t)
        {
            LOGGER.error("Error while processing decode events", t);
        }
    }

    /**
     * Processes new events and checks the active events for changes.  Event manager thread only.
     */
    void process()
    {
        long now = System.currentTimeMillis();
        IDecodeEvent event = mQueue.poll();

        while(event != null)
        {
            TrackedEvent tracked = mTrackedEvents.get(event);

            if(tracked == null)
            {
                tracked = new TrackedEvent(mNextEventKey++);
                mTrackedEvents.put(event, tracked);
            }

            tracked.mLastActivity = now;

            if(!tracked.mActive)
            {
                tracked.mActive = true;
                mActiveEvents.add(new ActiveEvent(event, tracked));
            }

            event = mQueue.poll();
        }

        Iterator<ActiveEvent> it = mActiveEvents.iterator();

        while(it.hasNext())
        {
            ActiveEvent active = it.next();
            TrackedEvent tracked = active.tracked();

            try
            {
                if(tracked.update(active.event()))
                {
                    tracked.mLastActivity = now;
                }
            }
            catch(RuntimeException re)
            {
                //The decoder may be changing the event's identifiers on another thread - try again next time
                LOGGER.debug("Unable to read decode event - " + re.getMessage());
            }

            if(now - tracked.mLastActivity > ACTIVE_WINDOW_MS)
            {
                tracked.mActive = false;
                it.remove();
            }
        }
    }

    /**
     * Number of events that are checked for changes (for testing).
     */
    int getActiveEventCount()
    {
        return mActiveEvents.size();
    }

    /**
     * Updates the event record fields from the decode event.
     */
    void update(EventRecord record, IDecodeEvent event)
    {
        IdentifierCollection collection = event.getIdentifierCollection();
        Identifier to = collection != null ? collection.getToIdentifier() : null;
        Identifier from = collection != null ? collection.getFromIdentifier() : null;

        record.setStartTime(event.getTimeStart());
        record.setEndTime(Math.max(0, event.getTimeEnd()));
        record.setDuration(event.getDuration());
        record.setEventType((event.getEventType() != null ? event.getEventType() : DecodeEventType.UNKNOWN).name());

        Protocol protocol = event.getProtocol();

        if(protocol == null || protocol == Protocol.UNKNOWN)
        {
            protocol = to != null ? to.getProtocol() : (from != null ? from.getProtocol() : null);
        }

        if((protocol == null || protocol == Protocol.UNKNOWN) && collection != null)
        {
            Identifier decoder = collection.getIdentifier(IdentifierClass.CONFIGURATION, Form.DECODER_TYPE, Role.ANY);

            if(decoder instanceof DecoderTypeConfigurationIdentifier dtci && dtci.getValue() != null)
            {
                protocol = dtci.getValue().getProtocol();
            }
        }

        record.setProtocol(protocol != null && protocol != Protocol.UNKNOWN ? protocol.name() : null);
        record.setSystem(getString(collection, Form.SYSTEM));
        record.setSite(getString(collection, Form.SITE));
        record.setChannel(getString(collection, Form.CHANNEL));
        record.setAliasList(getString(collection, Form.ALIAS_LIST));

        IChannelDescriptor descriptor = event.getChannelDescriptor();
        record.setChannelDescriptor(descriptor != null ? descriptor.toString() : null);
        long frequency = descriptor != null ? descriptor.getDownlinkFrequency() : 0;

        if(frequency <= 0 && collection != null)
        {
            Identifier configured = collection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY,
                Role.ANY);

            if(configured instanceof FrequencyConfigurationIdentifier fci && fci.getValue() != null)
            {
                frequency = fci.getValue();
            }
        }

        record.setFrequency(Math.max(0, frequency));
        record.setTimeslot(event.hasTimeslot() ? event.getTimeslot() : -1);

        record.setToId(format(collection, Role.TO));
        record.setToValue(CallManager.getValue(to));
        record.setToForm(to != null ? to.getForm().name() : null);
        record.setToFullyQualified(to instanceof FullyQualifiedTalkgroupIdentifier fqt ?
            fqt.getFullyQualifiedTalkgroupAddress() : (to instanceof FullyQualifiedRadioIdentifier fqr ?
            fqr.getFullyQualifiedRadioAddress() : null));
        record.setFromId(format(collection, Role.FROM));
        record.setFromValue(CallManager.getValue(from));
        record.setFromForm(from != null ? from.getForm().name() : null);
        record.setFromFullyQualified(from instanceof FullyQualifiedRadioIdentifier fqr ?
            fqr.getFullyQualifiedRadioAddress() : null);
        record.setPatchedTalkgroups(CallManager.getPatchedTalkgroups(to));
        record.setTalkerAlias(getTalkerAlias(collection));
        record.setDetails(event.getDetails() != null && !event.getDetails().isBlank() ? event.getDetails() : null);
    }

    private static String getString(IdentifierCollection collection, Form form)
    {
        if(collection == null)
        {
            return null;
        }

        Identifier identifier = collection.getIdentifier(IdentifierClass.CONFIGURATION, form, Role.ANY);
        return identifier != null && identifier.getValue() != null ? identifier.getValue().toString() : null;
    }

    /**
     * Formats the identifiers with the role as a comma separated list, without talker aliases, like the Now Playing
     * events table.
     */
    private String format(IdentifierCollection collection, Role role)
    {
        if(collection == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for(Identifier identifier: collection.getIdentifiers(role))
        {
            if(identifier.getForm() == Form.TALKER_ALIAS)
            {
                continue;
            }

            if(!sb.isEmpty())
            {
                sb.append(",");
            }

            if(identifier.getForm() == Form.TALKGROUP || identifier.getForm() == Form.RADIO ||
                identifier.getForm() == Form.PATCH_GROUP)
            {
                sb.append(mTalkgroupFormatSettings != null ? mTalkgroupFormatSettings.format(identifier) :
                    identifier.toString());
            }
            else
            {
                sb.append(identifier);
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    private static String getTalkerAlias(IdentifierCollection collection)
    {
        if(collection == null)
        {
            return null;
        }

        List<String> aliases = new ArrayList<>();

        for(Identifier identifier: collection.getIdentifiers(Form.TALKER_ALIAS))
        {
            if(identifier.getValue() != null)
            {
                aliases.add(identifier.getValue().toString());
            }
        }

        return aliases.isEmpty() ? null : String.join(",", aliases);
    }

    /**
     * Content of an event snapshot that is compared to detect a change that needs to be saved and broadcast.
     */
    private static String getSignature(EventRecord record)
    {
        return record.getStartTime() + "|" + record.getEndTime() + "|" + record.getDuration() + "|" +
            record.getEventType() + "|" + record.getProtocol() + "|" + record.getSystem() + "|" + record.getSite() +
            "|" + record.getChannel() + "|" + record.getChannelDescriptor() + "|" + record.getFrequency() + "|" +
            record.getTimeslot() + "|" + record.getToId() + "|" + record.getToForm() + "|" +
            record.getToFullyQualified() + "|" + record.getFromId() + "|" + record.getFromForm() + "|" +
            record.getFromFullyQualified() + "|" + record.getAliasList() + "|" + record.getPatchedTalkgroups() + "|" +
            record.getTalkerAlias() + "|" + record.getDetails();
    }

    /**
     * An event that is checked for changes, with a strong reference to the event while it's active.
     */
    private record ActiveEvent(IDecodeEvent event, TrackedEvent tracked) {}

    /**
     * Tracking state of one event.  Must not reference the event (weak map value).  Event manager thread only.
     */
    private class TrackedEvent
    {
        private final EventRecord mRecord = new EventRecord();
        private final DatabaseManager.RowHandle mHandle;
        private String mLastSignature;
        private long mLastActivity;
        private boolean mActive;

        TrackedEvent(long eventKey)
        {
            mRecord.setEventKey(eventKey);
            mHandle = mDatabaseManager != null ? mDatabaseManager.createHandle() : null;
        }

        /**
         * Updates the record from the event and publishes a snapshot when it changed.
         * @return true if the event changed
         */
        boolean update(IDecodeEvent event)
        {
            EventManager.this.update(mRecord, event);
            String signature = getSignature(mRecord);

            if(Objects.equals(signature, mLastSignature))
            {
                return false;
            }

            mLastSignature = signature;
            EventRecord snapshot = mRecord.copy();

            if(mDatabaseManager != null)
            {
                //The database writer sets the row id on its own copy
                mDatabaseManager.save(mHandle, snapshot.copy());
            }

            mEventBroadcaster.broadcast(snapshot);
            return true;
        }
    }
}
