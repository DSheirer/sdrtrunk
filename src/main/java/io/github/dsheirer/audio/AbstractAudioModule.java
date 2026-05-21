/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.PcmStreamManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base audio module implementation.
 */
public abstract class AbstractAudioModule extends Module implements IAudioSegmentProvider, IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractAudioModule.class);
    public static final long DEFAULT_SEGMENT_AUDIO_SAMPLE_LENGTH = 60 * 8000; // 1 minute @ 8kHz
    public static final int DEFAULT_TIMESLOT = 0;
    private final int mMaxSegmentAudioSampleLength;
    private Listener<AudioSegment> mAudioSegmentListener;
    protected MutableIdentifierCollection mIdentifierCollection;
    private Broadcaster<IdentifierUpdateNotification> mIdentifierUpdateNotificationBroadcaster = new Broadcaster<>();
    private AliasList mAliasList;
    private AudioSegment mAudioSegment;
    private int mAudioSampleCount = 0;
    private boolean mRecordAudioOverride;
    private int mTimeslot;

    // PCM stream state — used to broadcast decoded audio to connected TCP clients on port 9503
    private static final DateTimeFormatter PCM_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private volatile String mPcmCallId = null;
    private final AtomicInteger mPcmFrameSeq = new AtomicInteger(0);
    private final AtomicInteger mPcmFrameCount = new AtomicInteger(0);
    private String mPcmCachedSystem = "";
    private String mPcmCachedSite = "";
    private String mPcmCachedTalkgroup = "";
    private String mPcmCachedFrom = "";

    /**
     * Constructs an abstract audio module
     *
     * @param aliasList for aliasing identifiers
     * @param maxSegmentAudioSampleLength in milliseconds
     */
    public AbstractAudioModule(AliasList aliasList, int timeslot, long maxSegmentAudioSampleLength)
    {
        mAliasList = aliasList;
        mMaxSegmentAudioSampleLength = (int)(maxSegmentAudioSampleLength * 8); //Convert milliseconds to samples
        mTimeslot = timeslot;
        mIdentifierCollection = new MutableIdentifierCollection(getTimeslot());
        mIdentifierUpdateNotificationBroadcaster.addListener(mIdentifierCollection);
    }

    /**
     * Constructs an abstract audio module with a default maximum audio segment length and a default timeslot 0.
     */
    public AbstractAudioModule(AliasList aliasList)
    {
        this(aliasList, DEFAULT_TIMESLOT, DEFAULT_SEGMENT_AUDIO_SAMPLE_LENGTH);
    }

    /**
     * Timeslot for this audio module
     */
    protected int getTimeslot()
    {
        return mTimeslot;
    }

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
                mAudioSegment.decrementConsumerCount();
                mAudioSegment = null;
            }

            // PCM stream: broadcast call_end when the segment closes
            if(mPcmCallId != null)
            {
                PcmStreamManager pcmMgr = PcmStreamManager.getInstance();
                if(pcmMgr != null && pcmMgr.isRunning())
                {
                    pcmMgr.broadcastCallEnd(mPcmCallId, mPcmCachedSystem, mPcmCachedSite,
                            mPcmCachedTalkgroup, mPcmFrameCount.get());
                }
                mPcmCallId = null;
                mPcmCachedSystem = "";
                mPcmCachedSite = "";
                mPcmCachedTalkgroup = "";
                mPcmCachedFrom = "";
            }
        }
    }

    /**
     * Escapes a string for safe inclusion in a JSON value.
     */

    /** Returns the user-configured system name from the identifier collection, or empty string. */
    private String pcmGetSystem()
    {
        io.github.dsheirer.identifier.Identifier id =
                mIdentifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);
        if(id instanceof SystemConfigurationIdentifier)
            return pcmEscape(((SystemConfigurationIdentifier)id).getValue());
        return "";
    }

    /** Returns the user-configured site name from the identifier collection, or empty string. */
    private String pcmGetSite()
    {
        io.github.dsheirer.identifier.Identifier id =
                mIdentifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);
        if(id instanceof SiteConfigurationIdentifier)
            return pcmEscape(((SiteConfigurationIdentifier)id).getValue());
        return "";
    }

    private static String pcmEscape(String s)
    {
        if(s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", "");
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
    public AudioSegment getAudioSegment()
    {
        synchronized(this)
        {
            if(mAudioSegment == null)
            {
                mAudioSegment = new AudioSegment(mAliasList, getTimeslot());
                mAudioSegment.incrementConsumerCount();
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

                // PCM stream: broadcast call_start for the new segment
                PcmStreamManager pcmMgr = PcmStreamManager.getInstance();
                if(pcmMgr != null && pcmMgr.isRunning() && mPcmCallId == null)
                {
                    mPcmCallId = Long.toHexString(System.currentTimeMillis()).substring(4);
                    mPcmFrameSeq.set(0);
                    mPcmFrameCount.set(0);
                    // Cache metadata once at call_start — reused on every addAudio() frame
                    mPcmCachedSystem = pcmGetSystem();
                    mPcmCachedSite = pcmGetSite();
                    mPcmCachedTalkgroup = pcmEscape(mIdentifierCollection.getToIdentifier() != null
                            ? mIdentifierCollection.getToIdentifier().toString() : "");
                    mPcmCachedFrom = pcmEscape(mIdentifierCollection.getFromIdentifier() != null
                            ? mIdentifierCollection.getFromIdentifier().toString() : "");
                    pcmMgr.broadcastCallStart(mPcmCallId, mPcmCachedSystem, mPcmCachedSite,
                            mPcmCachedTalkgroup, mPcmCachedFrom,
                            LocalDateTime.now().format(PCM_TIMESTAMP_FMT));
                }
            }

            return mAudioSegment;
        }
    }

    public void addAudio(float[] audioBuffer)
    {
        AudioSegment audioSegment = getAudioSegment();

        //If the current segment exceeds the max samples length, close it so that a new segment gets generated
        //and then link the segments together
        if(mAudioSampleCount >= mMaxSegmentAudioSampleLength)
        {
            AudioSegment previous = getAudioSegment();
            closeAudioSegment();
            audioSegment = getAudioSegment();
            audioSegment.linkTo(previous);
        }

        // PCM stream: broadcast decoded audio to connected TCP clients (after getAudioSegment() so mPcmCallId is set)
        PcmStreamManager pcmMgr = PcmStreamManager.getInstance();
        if(pcmMgr != null && pcmMgr.isRunning() && mPcmCallId != null)
        {
            pcmMgr.broadcastPcm(mPcmCallId, mPcmCachedSystem, mPcmCachedSite,
                    mPcmCachedTalkgroup, mPcmCachedFrom,
                    mPcmFrameSeq.getAndIncrement(), audioBuffer);
            mPcmFrameCount.incrementAndGet();
        }

        try
        {
            audioSegment.addAudio(audioBuffer);
            mAudioSampleCount += audioBuffer.length;
        }
        catch(Exception e)
        {
            closeAudioSegment();
        }
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
