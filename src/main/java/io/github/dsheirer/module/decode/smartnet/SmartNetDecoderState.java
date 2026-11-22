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

package io.github.dsheirer.module.decode.smartnet;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.smartnet.identifier.SmartNetRadio;
import io.github.dsheirer.module.decode.smartnet.identifier.SmartNetTalkgroup;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.heartbeat.IHeartbeatListener;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Motorola Type II decoder state.
 *
 * Control channel: pairs outbound signalling words into channel grants and passes them to the traffic channel manager.
 * A grant is two OSWs - the calling radio with command 0x308, then the talkgroup (group flag set) with the channel number
 * as its command.  A channel OSW without the 0x308 before it is an update for a call already in progress.  A channel
 * OSW without the group flag after a 0x308 is a private call to that radio.
 *
 * Voice (traffic) channel: there is nothing to decode - the audio is analog FM.  The state opens the audio when the
 * channel starts and keeps the call going until the traffic channel manager tears the channel down (the control
 * channel stopped announcing the call), or the configured call time limit passes.
 */
public class SmartNetDecoderState extends DecoderState implements IHeartbeatListener, IRealBufferListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartNetDecoderState.class);
    private static final double AUDIO_SAMPLE_RATE = 8000.0;
    private final ChannelType mChannelType;
    private final SmartNetTrafficChannelManager mTrafficChannelManager;
    private final SmartNetBandPlan mBandPlan;
    private final long mCallLimitMs;
    private final Listener<Heartbeat> mHeartbeatListener = heartbeat -> heartbeat();
    private LongSupplier mClock = System::currentTimeMillis;
    private SmartNetOSW mPrevious;
    private long mFrequency;
    private long mTrafficStart;
    private long mLastContinuation;
    private boolean mTrafficEnded;
    private boolean mTrafficStarted;
    private long mAudioSamples;
    private long mVoiceFrames;
    private long mLoggedControlFrequency;
    private final Listener<float[]> mAudioListener = audio -> mAudioSamples += audio.length;

    //Activity summary
    private long mOSWCount;
    private long mGrantCount;
    private long mSplitGrantCount;
    private long mUnaliasedCount;
    private long mOtherSiteGrantCount;
    private final Map<Integer,Integer> mSiteAnnouncements = new TreeMap<>();
    private long mDigitalGrantCount;
    //Talkgroup -> true when its last grant was digital (0x321), false when analog (0x308).  An update OSW does not
    //say which it is, so a call first seen from an update is started the way the talkgroup was last granted.
    private final Map<Integer,Boolean> mDigitalTalkgroups = new java.util.HashMap<>();

    //Ignore unaliased talkgroups / site number
    private final boolean mIgnoreUnaliasedTalkgroups;
    private final int mConfiguredSite;
    private final boolean mDigitalVoice;
    private AliasList mAliasList;
    private int mSite;              //site the control channel announces, 0 = not announced yet
    private int mSiteCandidate;     //a different site announced once - taken when it is announced again
    private final Map<Integer,Integer> mTalkgroupGrants = new TreeMap<>();
    private final Map<Integer,Integer> mChannelGrants = new TreeMap<>();

    /**
     * @param channelType STANDARD for the control channel, TRAFFIC for a voice channel it started
     * @param trafficChannelManager for the control channel, null for a voice channel
     * @param config for the channel
     */
    public SmartNetDecoderState(ChannelType channelType, SmartNetTrafficChannelManager trafficChannelManager,
                                DecodeConfigSmartNet config)
    {
        mChannelType = channelType;
        mTrafficChannelManager = trafficChannelManager;
        mBandPlan = config.getBandPlan();
        mCallLimitMs = config.getCallTimeoutSeconds() * 1000L;
        mIgnoreUnaliasedTalkgroups = config.getIgnoreUnaliasedTalkgroups();
        mConfiguredSite = config.getSite();
        mDigitalVoice = config.isDigital();
    }

    /**
     * Alias list of the channel: names for the activity summary and the "ignore unaliased talkgroups" check.
     */
    public void setAliasList(AliasList aliasList)
    {
        mAliasList = aliasList;
    }

    /**
     * Site the control channel announces, 0 when it has not announced one yet
     */
    public int getSite()
    {
        return mSite;
    }

    /**
     * True when a site is set for this channel and the control channel announces another one.
     */
    public boolean isOtherSite()
    {
        return mConfiguredSite != DecodeConfigSmartNet.SITE_ANY && mSite != 0 && mSite != mConfiguredSite;
    }

    /**
     * A site announcement.  The first one is taken as it is, unless a site is set for the channel and this is another
     * one; from then on a different site has to be announced twice in a row, so that a single misread control word
     * does not stop the calls.
     */
    private void siteAnnounced(int site)
    {
        mSiteAnnouncements.merge(site, 1, Integer::sum);

        if(site == mSite)
        {
            mSiteCandidate = 0;
            return;
        }

        boolean first = mSite == 0 && (mConfiguredSite == DecodeConfigSmartNet.SITE_ANY || site == mConfiguredSite);

        if(first || site == mSiteCandidate)
        {
            mSite = site;
            mSiteCandidate = 0;
            String where = "Motorola Type II control channel on " + format(mFrequency) + " announces site " + site;

            if(mConfiguredSite == DecodeConfigSmartNet.SITE_ANY)
            {
                LOGGER.info(where);
            }
            else if(site == mConfiguredSite)
            {
                LOGGER.info(where + " - the site set for this channel");
            }
            else
            {
                LOGGER.info(where + " - NOT the site set for this channel (" + mConfiguredSite + "): its calls are " +
                    "not followed and the channel goes on to its next control frequency");
            }
        }
        else
        {
            mSiteCandidate = site;
        }
    }

    private String aliasName(Identifier identifier)
    {
        AliasList aliasList = mAliasList;

        if(aliasList != null && identifier != null)
        {
            List<Alias> aliases = aliasList.getAliases(identifier);

            if(aliases != null && !aliases.isEmpty())
            {
                return aliases.get(0).getName();
            }
        }

        return null;
    }

    /**
     * Replaces the clock - for testing only
     */
    void setClock(LongSupplier clock)
    {
        mClock = clock;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.SMARTNET;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message instanceof SmartNetOSW osw)
        {
            mOSWCount++;
            logCommands(osw);

            if(mChannelType != ChannelType.TRAFFIC && mFrequency != 0 && mFrequency != mLoggedControlFrequency)
            {
                mLoggedControlFrequency = mFrequency;
                LOGGER.info("Motorola Type II control channel decoding on " + format(mFrequency));
            }

            if(mChannelType != ChannelType.TRAFFIC && osw.isSiteAnnouncement())
            {
                siteAnnounced(osw.getSite());
            }

            SmartNetOSW previous = mPrevious;
            mPrevious = osw;

            //A control channel of another site does not count as this channel's control channel: without these events
            //a channel with several control frequencies goes on to the next one.
            if(!isOtherSite())
            {
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));
            }

            if(mTrafficChannelManager == null || !mBandPlan.isChannel(osw.getCommand()))
            {
                return;
            }

            long frequency = mBandPlan.getFrequency(osw.getCommand());

            if(frequency == mFrequency || frequency == getCurrentFrequency())
            {
                return; //announcements of the control channel itself
            }

            //The calling radio is in the OSW sent straight before this one.  When a control word was lost in between,
            //the previous decoded OSW belongs to another call and its radio must not be used (Summit 800, 2026-11-12:
            //604 of 21,270 grant lines named a radio that no other line of the call named).
            //With a site set, calls are followed only while the control channel is known to be that site's
            if(mConfiguredSite != DecodeConfigSmartNet.SITE_ANY && mSite != mConfiguredSite)
            {
                mOtherSiteGrantCount++;
                return;
            }

            boolean digitalGrant = previous != null && previous.getCommand() == SmartNetOSW.CMD_DIGITAL_UNIT_ID;
            boolean grant = previous != null && (previous.getCommand() == SmartNetOSW.CMD_UNIT_ID || digitalGrant);

            if(grant && !osw.followsPrevious())
            {
                grant = false;
                mSplitGrantCount++;
            }
            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
            ic.remove(IdentifierClass.USER);
            DecodeEventType type;
            String details = null;
            boolean follow = true;
            boolean digital = grant && digitalGrant;

            if(osw.isGroup())
            {
                ic.update(SmartNetTalkgroup.createTo(osw.getTalkgroup()));

                if(grant)
                {
                    mDigitalTalkgroups.put(osw.getTalkgroup(), digitalGrant);
                }
                else
                {
                    digital = mDigitalTalkgroups.getOrDefault(osw.getTalkgroup(), false);
                }

                if(digital)
                {
                    details = "DIGITAL";
                }

                if(grant)
                {
                    ic.update(SmartNetRadio.createFrom(previous.getAddress()));
                }

                if(osw.isEncrypted())
                {
                    type = DecodeEventType.CALL_GROUP_ENCRYPTED;
                    details = digital ? "DIGITAL ENCRYPTED - NOT FOLLOWED" : "ENCRYPTED - NOT FOLLOWED";
                    follow = false;
                }
                else
                {
                    type = DecodeEventType.CALL_GROUP;
                }

                if(follow && mIgnoreUnaliasedTalkgroups && mAliasList != null &&
                    aliasName(SmartNetTalkgroup.createTo(osw.getTalkgroup())) == null)
                {
                    type = DecodeEventType.CALL_GROUP;
                    details = digital ? "DIGITAL NO ALIAS - NOT FOLLOWED" : "NO ALIAS - NOT FOLLOWED";
                    follow = false;
                    mUnaliasedCount++;
                }

                if(osw.isEmergency())
                {
                    details = details == null ? "EMERGENCY" : "EMERGENCY " + details;
                }
            }
            else if(grant)
            {
                ic.update(SmartNetRadio.createTo(osw.getAddress()));
                ic.update(SmartNetRadio.createFrom(previous.getAddress()));
                type = DecodeEventType.CALL_UNIT_TO_UNIT;
            }
            else
            {
                return; //individual channel OSW that is not a grant
            }

            if(grant && LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Motorola Type II grant: radio " + previous.getAddress() + " -> " +
                    (osw.isGroup() ? "talkgroup " + osw.getTalkgroup() + " status " + osw.getStatus() : "radio " +
                        osw.getAddress()) + " on channel " + osw.getCommand() + " " + format(frequency) +
                    (digital ? " DIGITAL" : ""));
            }

            if(grant)
            {
                mGrantCount++;
                mDigitalGrantCount += digital ? 1 : 0;
                mChannelGrants.merge(osw.getCommand(), 1, Integer::sum);

                if(osw.isGroup())
                {
                    mTalkgroupGrants.merge(osw.getTalkgroup(), 1, Integer::sum);
                }
            }

            mTrafficChannelManager.processGrant(SmartNetChannel.create(osw.getCommand(), mBandPlan), ic, type,
                details, follow, digital, osw.getTimestamp());
        }
        else if(mChannelType == ChannelType.TRAFFIC &&
            message instanceof io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDUMessage)
        {
            //Voice channel of a digital call: one P25 voice frame is 180 ms of audio
            mAudioSamples += 1440;
            mVoiceFrames++;
        }
    }

    /**
     * Voice channel: keeps the call open while the channel runs, up to the call time limit.
     */
    private void heartbeat()
    {
        //A voice channel's sample source runs its own thread from the moment it is created and sends a heartbeat every
        //20 ms, so one can arrive before start() has run.  The call start time is then still 0, the call looked older
        //than its limit, and the voice channel was shut down as it started: the whole call was lost (Cambridge
        //2026-11-14/15: 328 of 12,189 EDACS calls, logged as "voice channel open 0.0 s").
        if(mChannelType != ChannelType.TRAFFIC || mTrafficEnded || !mTrafficStarted)
        {
            return;
        }

        long now = mClock.getAsLong();

        if(now - mTrafficStart > mCallLimitMs)
        {
            mTrafficEnded = true;
            broadcast(new DecoderStateEvent(this, Event.END, State.CALL));
        }
        else if(now - mLastContinuation >= 1000)
        {
            mLastContinuation = now;
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
        }
    }

    @Override
    public Listener<Heartbeat> getHeartbeatListener()
    {
        return mHeartbeatListener;
    }

    @Override
    public void start()
    {
        super.start();

        if(mChannelType == ChannelType.TRAFFIC)
        {
            mTrafficStart = mClock.getAsLong();
            mLastContinuation = mTrafficStart;
            mTrafficEnded = false;
            mTrafficStarted = true;
            mAudioSamples = 0;
            mVoiceFrames = 0;
            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
        }
    }

    /**
     * Voice channel: one log line per followed call - how long the voice channel was open and how much audio got past
     * the squelch (0 s means nothing was heard, so there was nothing to play or stream).
     */
    @Override
    public void stop()
    {
        super.stop();

        if(mChannelType == ChannelType.TRAFFIC && mTrafficStarted)
        {
            mTrafficStarted = false;
            long frequency = mFrequency != 0 ? mFrequency : getCurrentFrequency();
            //DEBUG - per-call detail, written when its Diagnostics category is on
            LOGGER.debug(String.format("Motorola Type II call TO:%s FROM:%s on %s - voice channel open %.1f s, " +
                    "audio %.1f s%s", first(Role.TO), first(Role.FROM), format(frequency),
                (mClock.getAsLong() - mTrafficStart) / 1000.0, mAudioSamples / AUDIO_SAMPLE_RATE,
                (mDigitalVoice ? ", digital (P25 voice frames " + mVoiceFrames + ")" : "") +
                    (mTrafficEnded ? " - stopped at the call limit" : "")));
        }
    }

    private final Map<Integer,Integer> mMinuteCommands = new TreeMap<>();
    private long mMinuteStart;

    /**
     * Diagnostics (DEBUG): the commands seen each minute, most frequent first
     */
    private void logCommands(SmartNetOSW osw)
    {
        if(!LOGGER.isDebugEnabled())
        {
            return;
        }

        mMinuteCommands.merge(osw.getCommand(), 1, Integer::sum);

        if(osw.getTimestamp() - mMinuteStart >= 60_000)
        {
            if(mMinuteStart > 0)
            {
                StringBuilder sb = new StringBuilder();
                mMinuteCommands.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(12)
                    .forEach(e -> sb.append(String.format(" %03X:%d", e.getKey(), e.getValue())));
                LOGGER.debug("Motorola Type II OSW commands last minute (hex:count):" + sb + " - calling radios " +
                    "not used so far: " + mSplitGrantCount);
            }

            mMinuteCommands.clear();
            mMinuteStart = osw.getTimestamp();
        }
    }

    private String first(Role role)
    {
        List<Identifier> identifiers = getIdentifierCollection().getIdentifiers(role);
        return identifiers.isEmpty() ? "-" : identifiers.get(0).toString();
    }

    private static String format(long frequency)
    {
        return frequency > 0 ? String.format("%.4f MHz", frequency / 1e6) : "unknown frequency";
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        return mAudioListener;
    }

    @Override
    public void init()
    {
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET ->
            {
                if(mChannelType != ChannelType.TRAFFIC)
                {
                    resetState();
                }
            }
            case NOTIFICATION_SOURCE_FREQUENCY ->
            {
                if(event.getFrequency() != mFrequency)
                {
                    mSite = 0;          //another control frequency - its site is not known until it announces it
                    mSiteCandidate = 0;
                }

                mFrequency = event.getFrequency();
            }
            default ->
            {
            }
        }
    }

    /**
     * Number of times a calling radio was not used because a control word was lost between it and the channel OSW.
     */
    public long getSplitGrantCount()
    {
        return mSplitGrantCount;
    }

    public long getGrantCount()
    {
        return mGrantCount;
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Activity Summary - Decoder: Motorola Type II\n");
        sb.append(DIVIDER1);
        sb.append("Band plan: ").append(mBandPlan).append("\n");
        sb.append("Site announced by the control channel: ").append(mSite == 0 ? "none yet" : String.valueOf(mSite));
        sb.append(mConfiguredSite == DecodeConfigSmartNet.SITE_ANY ? "" : "   (site set for this channel: " +
            mConfiguredSite + (isOtherSite() ? " - NOT THIS ONE, calls are not followed)" : ")")).append("\n");
        sb.append("OSWs decoded: ").append(mOSWCount).append("\n");
        sb.append("Channel grants: ").append(mGrantCount).append("   of them digital (P25 voice): ")
            .append(mDigitalGrantCount).append("\n");
        sb.append("Calling radios not used (control word lost before the channel word): ").append(mSplitGrantCount)
            .append("\n");
        sb.append(DIVIDER2).append("Grants by channel:\n");
        mChannelGrants.forEach((channel, count) -> sb.append(String.format("  %4d  %.4f MHz  %d%n", channel,
            mBandPlan.getFrequency(channel) / 1e6, count)));
        sb.append(DIVIDER2).append("Grants by talkgroup:\n");
        mTalkgroupGrants.forEach((talkgroup, count) -> {
            String name = aliasName(SmartNetTalkgroup.createTo(talkgroup));
            sb.append(String.format("  %5d  %-32s %d%n", talkgroup, name != null ? name : "", count));
        });

        if(mIgnoreUnaliasedTalkgroups)
        {
            sb.append(DIVIDER2).append("Ignore unaliased talkgroups is ON - call announcements not followed: ")
                .append(mUnaliasedCount).append("\n");
        }

        if(mConfiguredSite != DecodeConfigSmartNet.SITE_ANY)
        {
            sb.append(DIVIDER2).append("Call announcements not followed because the site was another one or not yet " +
                "known: ").append(mOtherSiteGrantCount).append("\n");
        }

        if(mSiteAnnouncements.size() > 1)
        {
            sb.append(DIVIDER2).append("Site announcements (site: count):");
            mSiteAnnouncements.forEach((site, count) -> sb.append("  ").append(site).append(": ").append(count));
            sb.append("\n");
        }
        return sb.toString();
    }
}
