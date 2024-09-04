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

package io.github.dsheirer.audio;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio segment containing all related metadata and a dynamic collection of audio packets.  An audio segment can be
 * a discrete (ie start/stop) audio event, or it may be a time-constrained portion of an ongoing continuous audio
 * broadcast.  Since the audio segment is held in memory until all consumers have finished processing the segment,
 * producers should constrain the duration of each audio segment to a reasonable duration.
 *
 * Producers can link time-constrained audio segments from a continous broadcast (e.g. FM radio station) so that
 * consumers can identify audio segments that belong to a continous stream.  This linkage presents a memory leak
 * potential.  Therefore, the audio segment uses the consumer counter to trigger an unlinking via the dispose() mehtod.
 * Accurate accounting of consumer count via the increment/decrementConsumerCount() methods is essential for good
 * memory management.
 *
 * Producers will add audio buffers and update identifiers throughout the life-cycle of an audio segment.  The producer
 * will signal the completion of an audio segment by setting the complete property to true.  This allows consumers the
 * option to process the audio buffers throughout the life-cycle of the segment, or to process all of the buffers once
 * the segment is complete.
 */
public class AudioSegment implements Listener<IdentifierUpdateNotification>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioSegment.class);
    private BooleanProperty mComplete = new SimpleBooleanProperty(false);
    private BooleanProperty mDuplicate = new SimpleBooleanProperty(false);
    private BooleanProperty mEncrypted = new SimpleBooleanProperty(false);
    private BooleanProperty mRecordAudio = new SimpleBooleanProperty(false);
    private IntegerProperty mMonitorPriority = new SimpleIntegerProperty(Priority.DEFAULT_PRIORITY);
    private ObservableSet<BroadcastChannel> mBroadcastChannels = FXCollections.observableSet(new HashSet<>());
    private MutableIdentifierCollection mIdentifierCollection = new MutableIdentifierCollection();
    private Broadcaster<IdentifierUpdateNotification> mIdentifierUpdateNotificationBroadcaster = new Broadcaster<>();
    private List<float[]> mAudioBuffers = new CopyOnWriteArrayList();
    private AtomicInteger mConsumerCount = new AtomicInteger();
    private AliasList mAliasList;
    private long mStartTimestamp = System.currentTimeMillis();
    private long mSampleCount = 0;
    private boolean mDisposing = false;
    private AudioSegment mLinkedAudioSegment;
    private int mTimeslot;

    /**
     * Constructs an instance
     *
     * @param aliasList for accessing aliases associated with identifiers for this audio segment.
     */
    public AudioSegment(AliasList aliasList, int timeslot)
    {
        mAliasList = aliasList;
        mTimeslot = timeslot;
        mIdentifierCollection.setTimeslot(timeslot);
    }

    /**
     * Timeslot for this audio segment
     */
    public int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Creation timestamp for this audio segment
     * @return long milliseconds since epoch
     */
    public long getStartTimestamp()
    {
        return mStartTimestamp;
    }

    /**
     * End timestamp as calculated from start timestamp and current sample count
     */
    public long getEndTimestamp()
    {
        return mStartTimestamp + getDuration();
    }

    /**
     * Current duration of this audio segment.  Note: this is a dynamic value until the complete property is set to true.
     *
     * @return duration in milliseconds
     */
    public long getDuration()
    {
        return (mSampleCount / 8); //8 kHz audio generates 8 samples per millisecond
    }

    /**
     * Indicates if the audio segment contains encrypted audio.
     *
     * @return encrypted property
     */
    public BooleanProperty encryptedProperty()
    {
        return mEncrypted;
    }

    /**
     * Indicates if this audio segment is encrypted
     * @return true if encrypted.
     */
    public boolean isEncrypted()
    {
        return mEncrypted.get();
    }

    /**
     * The complete property is used by the audio segment producer to signal that the segment is complete and no
     * additional audio or identifiers will be added to the segment.
     *
     * @return complete property
     */
    public BooleanProperty completeProperty()
    {
        return mComplete;
    }

    /**
     * Indicates if this audio segment is completed.
     * @return true if completed.
     */
    public boolean isComplete()
    {
        return mComplete.get();
    }

    /**
     * Duplicate call audio property.  This flag is set to true whenever a duplicate call detection function detects
     * an audio segment is a duplicate.
     */
    public BooleanProperty duplicateProperty()
    {
        return mDuplicate;
    }

    /**
     * An observable list of broadcast channels for this audio segment.  Broadcast channels are added to this segment
     * across the life-cycle of the segment.  As each new alias identifier is added to the segment, any broadcast
     * channels assigned to the alias are added to this list.  The audio segment producer can also add broadcast
     * channels to this list.
     *
     * @return observable set of broadcast channels
     */
    public ObservableSet<BroadcastChannel> broadcastChannelsProperty()
    {
        return mBroadcastChannels;
    }

    /**
     * Set of broadcast channels from identifier associated aliases for this segment.
     */
    public Set<BroadcastChannel> getBroadcastChannels()
    {
        return Collections.unmodifiableSet(mBroadcastChannels);
    }

    /**
     * Indicates if this segment has audio streaming broadcast channels specified.
     */
    public boolean hasBroadcastChannels()
    {
        return !mBroadcastChannels.isEmpty();
    }

    /**
     * Property to signal that this audio segment should be recorded.  This property can either be set by the producer
     * of the audio segment, or it can be flipped to true by any aliases that are added to this segment that require
     * audio associated with the identifier to be recorded.
     *
     * @return
     */
    public BooleanProperty recordAudioProperty()
    {
        return mRecordAudio;
    }

    /**
     * Audio playback/monitor priority specified by identifier associated aliases for this segment.
     */
    public IntegerProperty monitorPriorityProperty()
    {
        return mMonitorPriority;
    }

    /**
     * Indicates if at least one of the identifier associated aliases for this segments specifies Do Not Monitor.
     */
    public boolean isDoNotMonitor()
    {
        return mMonitorPriority.get() <= Priority.DO_NOT_MONITOR;
    }

    /**
     * Alias list for this audio segment
     */
    public AliasList getAliasList()
    {
        return mAliasList;
    }

    /**
     * Indicates if this audio segment is linked to a preceding audio segment.
     */
    public boolean isLinked()
    {
        return mLinkedAudioSegment != null;
    }

    /**
     * Indicates if this audio segment is linked to the argument audio segment
     * @param audioSegment to check for linkage
     * @return true if this segment is linked to the argument
     */
    public boolean isLinkedTo(AudioSegment audioSegment)
    {
        return isLinked() && audioSegment != null && mLinkedAudioSegment.equals(audioSegment);
    }

    /**
     * Optional linked audio segment is the audio segment that precedes this audio segment in a continuous audio
     * broadcast.
     *
     * @return linked audio segment or null.
     */
    public AudioSegment getLinkedAudioSegment()
    {
        return mLinkedAudioSegment;
    }

    /**
     * Links this audio segment to a preceeding/previous audio segment
     * @param previousAudioSegment to set as the predecessor
     */
    public void linkTo(AudioSegment previousAudioSegment)
    {
        mLinkedAudioSegment = previousAudioSegment;
    }

    /**
     * Immutable identifier collection for this audio segment
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Adds the collection of identifiers to this segment's identifier collection
     * @param identifiers to pre-load into this audio segment
     */
    void addIdentifiers(Collection<Identifier> identifiers)
    {
        for(Identifier identifier: identifiers)
        {
            addIdentifier(identifier);
        }
    }

    /**
     * Unmodifiable copy of the list of audio buffers for this segment.
     *
     * @return list of audio buffers
     */
    public List<float[]> getAudioBuffers()
    {
        return Collections.unmodifiableList(mAudioBuffers);
    }

    /**
     * Count of audio buffers contained in this segment.
     *
     * Note: audio buffers can be added to an audio segment throughout the segment's life-cycle by the audio producer.
     */
    public int getAudioBufferCount()
    {
        return mAudioBuffers.size();
    }

    /**
     * Gets the audio buffer at the specified index
     * @param index of the buffer to fetch
     * @return audio buffer
     * @throws IllegalArgumentException if requested index is not valid
     */
    public float[] getAudioBuffer(int index)
    {
        if(0 <= index && index < getAudioBufferCount())
        {
            return mAudioBuffers.get(index);
        }
        else
        {
            throw new IllegalArgumentException("Requested audio buffer at index [" + index + "] does not exist");
        }
    }

    /**
     * Indicates if this audio segment has one or more audio buffers
     */
    public boolean hasAudio()
    {
        return !mAudioBuffers.isEmpty();
    }

    /**
     * Removes all audio buffers and decrements the user count on each so that the audio buffer can be reclaimed.
     */
    private void dispose()
    {
        mDisposing = true;
        mAudioBuffers.clear();
        mIdentifierCollection.clear();
        mIdentifierUpdateNotificationBroadcaster.clear();
        mLinkedAudioSegment = null;
    }

    /**
     * Increments the consumer count to indicate that a consumer is currently processing this segment.  When the
     * consumer count returns to zero, this indicates that all consumers are finished with the audio segment and the
     * resources can be reclaimed.
     *
     * Consumer count should only be increased by the producer of the audio segment, or if a consumer distributes the
     * segment to additional consumers.
     */
    public void incrementConsumerCount()
    {
        mConsumerCount.incrementAndGet();
    }

    /**
     * Decrements the consumer count.  Consumers of this audio segment should invoke this method to signal that they
     * will no longer need this audio segment.  When all consumers are finished with an audio segment, the audio
     * segment resources will be reclaimed.
     */
    public void decrementConsumerCount()
    {
        int count = mConsumerCount.decrementAndGet();

        if(count <= 0)
        {
            dispose();
        }
    }

    /**
     * Adds an audio buffer to this segment.  Note the producer of the audio buffer should increment the user count
     * of the buffer prior to adding it to this segment.  This segment will decrement the audio buffer user count once
     * all consumers of this audio segment have de-registered via the decrementConsumerCount() method.
     *
     * @param audioBuffer to add to this segment
     */
    public void addAudio(float[] audioBuffer)
    {
        if(audioBuffer == null)
        {
            throw new IllegalArgumentException("Can't add null audio buffer");
        }

        if(mDisposing)
        {
            throw new IllegalStateException("Can't add audio to an audio segment that is being disposed");
        }

        if(mAudioBuffers.isEmpty())
        {
            mStartTimestamp = System.currentTimeMillis() - 20;
        }

        mAudioBuffers.add(audioBuffer);
        mSampleCount += audioBuffer.length;
    }

    /**
     * Adds a listener to receive identifier update notifications
     */
    public void addIdentifierUpdateNotificationListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateNotificationBroadcaster.addListener(listener);
    }

    /**
     * Removes the identifier update listener.
     */
    public void removeIdentifierUpdateNotificationListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateNotificationBroadcaster.removeListener(listener);
    }

    /**
     * Adds the identifier to this audio segment and updates record, priority and streaming properties.
     *
     * Note: identifiers pass to this method must be checked for timeslot match.
     */
    public void addIdentifier(Identifier identifier)
    {
        mIdentifierCollection.update(identifier);

        /**
         * If we have a late-add encryption key, set the encrypted flag to true.
         */
        if(identifier instanceof EncryptionKeyIdentifier eki)
        {
            mEncrypted.set(eki.isEncrypted());
        }

        List<Alias> aliases = mAliasList.getAliases(identifier);

        for(Alias alias: aliases)
        {
            if(alias.isRecordable())
            {
                mRecordAudio.set(true);
            }

            //Add all broadcast channels for the alias ... let the set handle duplication.
            mBroadcastChannels.addAll(alias.getBroadcastChannels());

            //Only assign a playback priority if it is lower priority than the current setting.
            int playbackPriority = alias.getPlaybackPriority();

            if(playbackPriority < mMonitorPriority.get())
            {
                mMonitorPriority.set(playbackPriority);
            }
        }
    }

    /**
     * Indicates if this audio segment has been flagged as a duplicate audio call
     */
    public boolean isDuplicate()
    {
        return mDuplicate.get();
    }

    /**
     * Sets the duplicate audio call flag for this audio segment
     * @param duplicate true if this is a duplicate audio segment
     */
    public void setDuplicate(boolean duplicate)
    {
        mDuplicate.set(duplicate);
    }

    /**
     * Processes identifier update notifications and updates audio segment properties using the associated aliases
     * from the alias list.
     *
     * @param identifierUpdateNotification containing an identifier.
     */
    @Override
    public void receive(IdentifierUpdateNotification identifierUpdateNotification)
    {
        //Only process add updates that match this timeslot
        if(identifierUpdateNotification.getTimeslot() == getTimeslot())
        {
            if(identifierUpdateNotification.isAdd() || identifierUpdateNotification.isSilentAdd())
            {
                addIdentifier(identifierUpdateNotification.getIdentifier());
            }

            mIdentifierUpdateNotificationBroadcaster.broadcast(identifierUpdateNotification);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Audio Segment\n      Timeslot:").append(getTimeslot()).append("\n");
        sb.append("    Start Time:").append(new Date(getStartTimestamp()));
        sb.append("        Linked:").append(isLinked()).append("\n");
        sb.append("    Recordable:").append(recordAudioProperty().get()).append("\n");
        sb.append("Do Not Monitor:").append(isDoNotMonitor()).append("\n");
        sb.append("     Stream To:").append(getBroadcastChannels()).append("\n");
        sb.append("   Identifiers:\n").append(getIdentifierCollection()).append("\n");
        return sb.toString();
    }
}
