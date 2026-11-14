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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.RadioReferenceService;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.service.radioreference.FrequencyCandidate;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.service.radioreference.SiteRecord;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Playlist actions for radio reference frequencies and trunked system sites: find the playlist channel that already
 * decodes a frequency or site, enable or disable it, or add a channel and start it.  A frequency or site that already
 * has a playlist channel with the same protocol isn't added again.
 *
 * The add, enable and disable actions run in the background and report errors to the caller's error handler, on the
 * calling thread pool: the handler shows the message on its own UI thread.
 */
public class RadioReferencePlaylistActions
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioReferencePlaylistActions.class);
    private final PlaylistManager mPlaylistManager;

    /**
     * Constructs an instance
     * @param playlistManager for the channels, channel processing and the radio reference service
     */
    public RadioReferencePlaylistActions(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
    }

    /**
     * Decoder types that count as the same protocol when looking for an existing channel: P25 Phase 1 and Phase 2
     * (a Phase 2 system's control channel is often decoded with the Phase 1 decoder), and the LTR variants.
     */
    public static Set<DecoderType> getProtocolFamily(DecoderType decoderType)
    {
        if(decoderType == null)
        {
            return EnumSet.noneOf(DecoderType.class);
        }

        return switch(decoderType)
        {
            case P25_PHASE1, P25_PHASE2 -> EnumSet.of(DecoderType.P25_PHASE1, DecoderType.P25_PHASE2);
            case LTR, LTR_NET, PASSPORT -> EnumSet.of(DecoderType.LTR, DecoderType.LTR_NET, DecoderType.PASSPORT);
            default -> EnumSet.of(decoderType);
        };
    }

    /**
     * Decoder types for a site's control channel, from the site's radio reference protocol
     * @return decoder types, or an empty set when sdrtrunk can't decode the site's control channel
     */
    public static Set<DecoderType> getSiteDecoderTypes(SiteRecord site)
    {
        String protocol = site.protocol() != null ? site.protocol().toLowerCase() : "";

        if(protocol.startsWith("project 25"))
        {
            return getProtocolFamily(DecoderType.P25_PHASE1);
        }
        else if(protocol.startsWith("dmr"))
        {
            return EnumSet.of(DecoderType.DMR);
        }
        else if(protocol.startsWith("nxdn"))
        {
            return EnumSet.of(DecoderType.NXDN);
        }
        else if(protocol.startsWith("ltr"))
        {
            return getProtocolFamily(DecoderType.LTR);
        }
        else if(protocol.startsWith("mpt"))
        {
            return EnumSet.of(DecoderType.MPT1327);
        }

        //Motorola Type II, EDACS and other control channels aren't supported
        return EnumSet.noneOf(DecoderType.class);
    }

    /**
     * Indicates if the site's system has no control channel, so each repeater has its own channel: LTR, where every
     * repeater carries its own trunking data
     */
    public static boolean isRepeaterSite(SiteRecord site)
    {
        return getSiteDecoderTypes(site).contains(DecoderType.LTR);
    }

    /**
     * Indicates if sdrtrunk can decode the site: its control channel, which radio reference lists, or its repeaters
     */
    public static boolean isSupported(SiteRecord site)
    {
        return getUnsupportedReason(site) == null;
    }

    /**
     * Why sdrtrunk can't decode the site
     * @return reason, or null when the site is supported
     */
    public static String getUnsupportedReason(SiteRecord site)
    {
        if(getSiteDecoderTypes(site).isEmpty())
        {
            return (site.protocol() != null ? site.protocol() : "The protocol") + " isn't supported";
        }

        if(isRepeaterSite(site) ? site.channels().isEmpty() : getControlFrequencies(site).isEmpty())
        {
            return isRepeaterSite(site) ? "Radio Reference lists no repeaters" :
                "Radio Reference lists no control channel";
        }

        return null;
    }

    /**
     * Frequencies of all of the site's channels
     */
    public static List<Long> getSiteFrequencies(SiteRecord site)
    {
        List<Long> frequencies = new ArrayList<>();

        for(SiteRecord.SiteChannel channel: site.channels())
        {
            frequencies.add(channel.frequencyHz());
        }

        return frequencies;
    }

    /**
     * Control and alternate control channel frequencies of the site, control channels first
     */
    public static List<Long> getControlFrequencies(SiteRecord site)
    {
        List<Long> frequencies = new ArrayList<>();

        for(String use: List.of("Control", "Alternate control"))
        {
            for(SiteRecord.SiteChannel channel: site.channels())
            {
                if(use.equals(channel.use()))
                {
                    frequencies.add(channel.frequencyHz());
                }
            }
        }

        return frequencies;
    }

    /**
     * Decoder type for a conventional candidate's radio reference mode, or null when sdrtrunk has no decoder for it
     */
    public static DecoderType getDecoderType(FrequencyCandidate candidate)
    {
        ModeDecoderType mode = ModeDecoderType.fromName(candidate.mode());
        return mode.hasDecoderType() ? mode.getDecoderType() : null;
    }

    /**
     * Playlist channel, not a temporary traffic channel, tuned to any of the frequencies with one of the decoder types
     * @return channel, or null
     */
    public Channel findChannel(Collection<Long> frequencies, Set<DecoderType> decoderTypes)
    {
        return findChannel(getChannels(), frequencies, decoderTypes);
    }

    /**
     * Channel in the list, not a temporary traffic channel, tuned to any of the frequencies with one of the decoder
     * types
     * @return channel, or null
     */
    static Channel findChannel(List<Channel> channels, Collection<Long> frequencies, Set<DecoderType> decoderTypes)
    {
        if(frequencies.isEmpty() || decoderTypes.isEmpty())
        {
            return null;
        }

        for(Channel channel: channels)
        {
            if(channel.isTrafficChannel() || channel.getDecodeConfiguration() == null ||
                !decoderTypes.contains(channel.getDecodeConfiguration().getDecoderType()))
            {
                continue;
            }

            for(long frequency: getFrequencies(channel.getSourceConfiguration()))
            {
                if(frequencies.contains(frequency))
                {
                    return channel;
                }
            }
        }

        return null;
    }

    /**
     * Playlist channel that decodes the site, tuned to one of its control channels
     * @return channel, or null
     */
    public Channel findSiteChannel(SiteRecord site)
    {
        return findChannel(getControlFrequencies(site), getSiteDecoderTypes(site));
    }

    /**
     * Playlist channels that decode the site: its control channel, or each of its repeaters that has a channel
     */
    public List<Channel> findSiteChannels(SiteRecord site)
    {
        List<Channel> channels = new ArrayList<>();

        if(isRepeaterSite(site))
        {
            List<Channel> playlist = getChannels();

            for(long frequency: getSiteFrequencies(site))
            {
                Channel channel = findChannel(playlist, List.of(frequency), getSiteDecoderTypes(site));

                if(channel != null && !channels.contains(channel))
                {
                    channels.add(channel);
                }
            }
        }
        else
        {
            Channel channel = findSiteChannel(site);

            if(channel != null)
            {
                channels.add(channel);
            }
        }

        return channels;
    }

    /**
     * Frequencies of a repeater site that don't have a playlist channel yet
     */
    public List<Long> getMissingRepeaterFrequencies(SiteRecord site)
    {
        List<Long> missing = new ArrayList<>();
        List<Channel> playlist = getChannels();

        for(long frequency: getSiteFrequencies(site))
        {
            if(findChannel(playlist, List.of(frequency), getSiteDecoderTypes(site)) == null)
            {
                missing.add(frequency);
            }
        }

        return missing;
    }

    /**
     * Starts the channels that aren't decoding, in the background
     */
    public void enableAll(List<Channel> channels, Consumer<String> onError)
    {
        for(Channel channel: channels)
        {
            if(!channel.isProcessing())
            {
                enable(channel, onError);
            }
        }
    }

    /**
     * Stops the channels that are decoding, in the background
     */
    public void disableAll(List<Channel> channels, Consumer<String> onError)
    {
        for(Channel channel: channels)
        {
            if(channel.isProcessing())
            {
                disable(channel, onError);
            }
        }
    }

    /**
     * Playlist channel that decodes the conventional frequency with the same protocol
     * @return channel, or null
     */
    public Channel findConventionalChannel(FrequencyCandidate candidate)
    {
        return findChannel(List.of(candidate.frequencyHz()), getProtocolFamily(getDecoderType(candidate)));
    }

    /**
     * Starts decoding the playlist channel, in the background
     */
    public void enable(Channel channel, Consumer<String> onError)
    {
        ThreadPool.CACHED.execute(() -> {
            try
            {
                if(!channel.isProcessing())
                {
                    mPlaylistManager.getChannelProcessingManager().start(channel);
                }
            }
            catch(ChannelException ce)
            {
                mLog.error("Error starting channel [" + channel.getName() + "] - " + ce.getMessage());
                onError.accept("Can't start " + channel.getShortTitle() + ": " + ce.getMessage());
            }
        });
    }

    /**
     * Stops decoding the playlist channel, in the background
     */
    public void disable(Channel channel, Consumer<String> onError)
    {
        ThreadPool.CACHED.execute(() -> {
            try
            {
                mPlaylistManager.getChannelProcessingManager().stop(channel);
            }
            catch(ChannelException ce)
            {
                mLog.error("Error stopping channel [" + channel.getName() + "] - " + ce.getMessage());
                onError.accept("Can't stop " + channel.getShortTitle() + ": " + ce.getMessage());
            }
        });
    }

    /**
     * Radio reference details of a site, for creating its channels
     */
    private record SiteDetails(RadioReferenceDecoder decoder, io.github.dsheirer.rrapi.type.System system,
                               SystemInformation systemInformation, Site site)
    {
    }

    /**
     * Fetches the site's system details from radio reference.  Call from a background thread.
     * @return details, or null after reporting the error
     */
    private SiteDetails getSiteDetails(SiteRecord siteRecord, Consumer<String> onError)
    {
        try
        {
            RadioReference radioReference = mPlaylistManager.getRadioReference();
            RadioReferenceService service = radioReference.getService();
            SystemInformation systemInformation = service.getSystemInformation(siteRecord.systemId());
            Site site = null;

            for(Site candidate: service.getSites(siteRecord.systemId()))
            {
                if(candidate.getSiteId() == siteRecord.siteId())
                {
                    site = candidate;
                }
            }

            if(systemInformation == null || site == null)
            {
                onError.accept("Radio Reference didn't provide the details of " + siteRecord.systemName() + " site " +
                    siteRecord.siteNumber());
                return null;
            }

            RadioReferenceDecoder decoder = new RadioReferenceDecoder(radioReference.getUserPreferences(),
                service.getTypesMap(), service.getFlavorsMap(), service.getVoicesMap(), service.getTagsMap());
            io.github.dsheirer.rrapi.type.System system = new io.github.dsheirer.rrapi.type.System();
            system.setSystemId(siteRecord.systemId());
            system.setName(systemInformation.getName() != null ? systemInformation.getName() : siteRecord.systemName());
            system.setTypeId(systemInformation.getTypeId());
            system.setFlavorId(systemInformation.getFlavorId());
            system.setVoiceId(systemInformation.getVoiceId());
            return new SiteDetails(decoder, system, systemInformation, site);
        }
        catch(RadioReferenceException rre)
        {
            mLog.error("Error getting radio reference site details - " + rre.getMessage());
            onError.accept("Can't get the site details from Radio Reference: " + rre.getMessage());
            return null;
        }
    }

    /**
     * Adds playlist channels for the trunked system site and starts them, or starts the site's existing channels.
     * A site with a control channel gets one channel for its control channels.  A repeater site (LTR) gets a channel
     * for each repeater that doesn't have one yet, and all of its channels are started.  Fetches the system details
     * from radio reference in the background.
     */
    public void addSite(SiteRecord siteRecord, Consumer<String> onError)
    {
        addSite(siteRecord, null, onError);
    }

    /**
     * Adds playlist channels for the trunked system site and starts them, or starts the site's existing channels, like
     * {@link #addSite(SiteRecord, Consumer)}.  A new control channel starts on the first frequency when it's one of the
     * site's control or alternate control channels.
     * @param firstFrequency to start the new control channel on, e.g. the frequency the user picked, or null
     */
    public void addSite(SiteRecord siteRecord, Long firstFrequency, Consumer<String> onError)
    {
        ThreadPool.CACHED.execute(() -> {
            SiteDetails details = getSiteDetails(siteRecord, onError);

            if(details == null)
            {
                return;
            }

            if(isRepeaterSite(siteRecord))
            {
                List<Channel> channels = RadioReferenceChannelFactory.repeaters(details.decoder(), details.system(),
                    details.systemInformation(), details.site(), getMissingRepeaterFrequencies(siteRecord));
                List<Channel> existing = findSiteChannels(siteRecord);

                if(channels.isEmpty() && existing.isEmpty())
                {
                    onError.accept("sdrtrunk can't decode " + siteRecord.systemName() + " site " +
                        siteRecord.siteNumber() + (siteRecord.protocol() != null ? " (" + siteRecord.protocol() +
                        ")" : ""));
                    return;
                }

                enableAll(existing, onError);

                //The channel model's list backs the playlist editor, so it's changed on the JavaFX thread
                Platform.runLater(() -> {
                    for(Channel channel: channels)
                    {
                        mPlaylistManager.getChannelModel().addChannel(channel);
                        enable(channel, onError);
                    }
                });
                return;
            }

            Channel channel = RadioReferenceChannelFactory.trunkedSite(details.decoder(), details.system(),
                details.systemInformation(), details.site(), firstFrequency);

            if(channel == null)
            {
                onError.accept("sdrtrunk can't decode the control channel of " + siteRecord.systemName() +
                    " site " + siteRecord.siteNumber() + (siteRecord.protocol() != null ? " (" +
                    siteRecord.protocol() + ")" : ""));
                return;
            }

            addAndStart(channel, onError);
        });
    }

    /**
     * Adds a playlist channel for one repeater of a repeater site (LTR) and starts it, or starts its existing channel
     */
    public void addRepeater(SiteRecord siteRecord, long frequency, Consumer<String> onError)
    {
        ThreadPool.CACHED.execute(() -> {
            SiteDetails details = getSiteDetails(siteRecord, onError);

            if(details == null)
            {
                return;
            }

            List<Channel> channels = RadioReferenceChannelFactory.repeaters(details.decoder(), details.system(),
                details.systemInformation(), details.site(), List.of(frequency));

            if(channels.isEmpty())
            {
                onError.accept("sdrtrunk can't decode " + siteRecord.systemName() + (siteRecord.protocol() != null ?
                    " (" + siteRecord.protocol() + ")" : ""));
                return;
            }

            addAndStart(channels.get(0), onError);
        });
    }

    /**
     * Adds a playlist channel for the conventional frequency and starts it, or starts the existing channel
     */
    public void addConventional(FrequencyCandidate candidate, Consumer<String> onError)
    {
        ModeDecoderType mode = ModeDecoderType.fromName(candidate.mode());
        String[] source = candidate.source() != null ? candidate.source().split(" › ") : new String[0];
        //The source is owner, category and subcategory: the channel's system is the owner, the site the subcategory
        String system = source.length > 0 ? source[0] : null;
        String site = source.length > 2 ? source[2] : source.length > 1 ? source[1] : null;
        String name = candidate.alphaTag() != null ? candidate.alphaTag() : candidate.title();
        Channel channel = RadioReferenceChannelFactory.conventional(mode, candidate.frequencyHz(), system, site, name);

        if(channel == null)
        {
            onError.accept("sdrtrunk can't decode " + (candidate.mode() != null ? candidate.mode() : "the mode of") +
                " " + candidate.title());
            return;
        }

        addAndStart(channel, onError);
    }

    /**
     * Adds the channel to the playlist and starts it, unless the playlist already has a channel for its frequencies
     * and protocol, which is started instead
     */
    private void addAndStart(Channel channel, Consumer<String> onError)
    {
        Channel existing = findChannel(getFrequencies(channel.getSourceConfiguration()),
            getProtocolFamily(channel.getDecodeConfiguration().getDecoderType()));

        if(existing != null)
        {
            enable(existing, onError);
            return;
        }

        //The channel model's list backs the playlist editor, so it's changed on the JavaFX thread
        Platform.runLater(() -> {
            mPlaylistManager.getChannelModel().addChannel(channel);
            enable(channel, onError);
        });
    }

    /**
     * Copy of the playlist channels, safe to read from any thread
     */
    private List<Channel> getChannels()
    {
        for(int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                return new ArrayList<>(mPlaylistManager.getChannelModel().getChannels());
            }
            catch(ConcurrentModificationException e)
            {
                //The playlist changed while copying - try again
            }
        }

        return new ArrayList<>();
    }

    /**
     * Tuned frequencies of a channel source
     */
    public static List<Long> getFrequencies(SourceConfiguration sourceConfiguration)
    {
        if(sourceConfiguration instanceof SourceConfigTuner tuner)
        {
            return List.of(tuner.getFrequency());
        }
        else if(sourceConfiguration instanceof SourceConfigTunerMultipleFrequency multiple)
        {
            return new ArrayList<>(multiple.getFrequencies());
        }

        return List.of();
    }
}
