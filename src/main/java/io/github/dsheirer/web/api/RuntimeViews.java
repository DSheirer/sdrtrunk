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

package io.github.dsheirer.web.api;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.call.CallManager;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.settings.identifier.TalkgroupFormatSettings;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * JSON representations of tuners, channels and channel runtime status.
 */
public final class RuntimeViews
{
    private RuntimeViews()
    {
    }

    /**
     * URL safe key for a discovered tuner.  Tuner ids can contain spaces, colons and, for recording tuners, a file
     * path, so the API addresses tuners by this key.
     */
    public static String tunerKey(DiscoveredTuner tuner)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tuner.getId().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A discovered tuner
     * @param key for the /tuners/{key} endpoints
     * @param id as sdrtrunk names it, for display
     * @param tunerClass hardware or source class, for example RTL2832, AIRSPY, RECORDING_TUNER
     * @param status ENABLED, DISABLED, REMOVED or ERROR
     * @param frequency center frequency in Hz, or null when the tuner isn't running
     * @param sampleRate in samples per second, or null when the tuner isn't running
     * @param usableBandwidth in Hz, or null when the tuner isn't running
     * @param frequencyCorrection in PPM, or null when the tuner isn't running
     * @param channelCount channels currently sourced from the tuner
     */
    public record TunerView(String key, String id, String name, String tunerClass, String tunerType, String status,
                            boolean enabled, String error, Long frequency, Double sampleRate, Integer usableBandwidth,
                            Double frequencyCorrection, Long minimumFrequency, Long maximumFrequency,
                            boolean sampleRateLocked, int channelCount)
    {
        public static TunerView of(DiscoveredTuner discovered)
        {
            Tuner tuner = discovered.hasTuner() ? discovered.getTuner() : null;
            TunerController controller = tuner != null ? tuner.getTunerController() : null;
            int channels = 0;

            try
            {
                channels = tuner != null && tuner.getChannelSourceManager() != null ?
                    tuner.getChannelSourceManager().getTunerChannelCount() : 0;
            }
            catch(Exception e)
            {
                //Count isn't available while the tuner is starting or stopping
            }

            return new TunerView(tunerKey(discovered), discovered.getId(),
                tuner != null ? tuner.getPreferredName() : discovered.getId(),
                discovered.getTunerClass() != null ? discovered.getTunerClass().name() : null,
                tuner != null && tuner.getTunerType() != null ? tuner.getTunerType().name() : null,
                discovered.getTunerStatus().name(), discovered.isEnabled(), discovered.getErrorMessage(),
                controller != null ? controller.getFrequency() : null,
                controller != null ? controller.getSampleRate() : null,
                controller != null ? controller.getUsableBandwidth() : null,
                controller != null ? controller.getFrequencyCorrection() : null,
                controller != null ? controller.getMinimumFrequency() : null,
                controller != null ? controller.getMaximumFrequency() : null,
                controller != null && controller.isLockedSampleRate(), channels);
        }
    }

    /**
     * A channel sourced from a tuner
     * @param frequency center frequency of the channel in Hz
     * @param offset from the tuner center frequency in Hz
     * @param bandwidth in Hz
     */
    public record TunerChannelView(long frequency, long offset, int bandwidth) {}

    /**
     * The channels sourced from a tuner
     */
    public static List<TunerChannelView> tunerChannels(DiscoveredTuner discovered)
    {
        List<TunerChannelView> views = new ArrayList<>();

        if(discovered.hasTuner() && discovered.getTuner().getChannelSourceManager() != null)
        {
            long center = discovered.getTuner().getTunerController().getFrequency();

            for(TunerChannel channel: List.copyOf(discovered.getTuner().getChannelSourceManager().getTunerChannels()))
            {
                views.add(new TunerChannelView(channel.getFrequency(), channel.getFrequency() - center,
                    channel.getBandwidth()));
            }
        }

        return views;
    }

    /**
     * Where a channel's samples come from
     * @param type TUNER, TUNER_MULTIPLE_FREQUENCIES, RECORDING or NONE
     * @param frequencies in Hz
     * @param preferredTuner name of the preferred tuner, or null for any
     */
    public record SourceView(String type, List<Long> frequencies, String preferredTuner)
    {
        static SourceView of(SourceConfiguration source)
        {
            if(source instanceof SourceConfigTuner tuner)
            {
                return new SourceView(source.getSourceType().name(), List.of(tuner.getFrequency()),
                    tuner.getPreferredTuner());
            }

            if(source instanceof SourceConfigTunerMultipleFrequency multiple)
            {
                return new SourceView(source.getSourceType().name(), List.copyOf(multiple.getFrequencies()),
                    multiple.getPreferredTuner());
            }

            return new SourceView(source != null ? source.getSourceType().name() : "NONE", List.of(), null);
        }
    }

    /**
     * A channel configuration in the active playlist, with whether it is processing
     * @param id channel id, stable while the playlist is loaded
     * @param decoder decoder type, for example P25_PHASE1, DMR, NBFM
     */
    public record ChannelView(int id, String name, String system, String site, String aliasList, String decoder,
                              SourceView source, boolean autoStart, Integer autoStartOrder, boolean processing)
    {
        public static ChannelView of(Channel channel)
        {
            DecodeConfiguration decode = channel.getDecodeConfiguration();
            return new ChannelView(channel.getChannelID(), channel.getName(), channel.getSystem(), channel.getSite(),
                channel.getAliasListName(), decode != null ? decode.getDecoderType().name() : null,
                SourceView.of(channel.getSourceConfiguration()), channel.isAutoStart(), channel.getAutoStartOrder(),
                channel.isProcessing());
        }
    }

    /**
     * Runtime state of one channel timeslot (a channel without timeslots has one entry)
     * @param key unique per channel timeslot: the channel id, and the timeslot when there is one
     * @param channelId of the channel, which can be a traffic channel that a control channel started
     * @param channelType STANDARD, or TRAFFIC for a traffic channel
     * @param state decoder state, for example IDLE, CALL, CONTROL, ENCRYPTED, FADE, TEARDOWN
     * @param frequency current channel frequency in Hz, or null
     */
    public record ChannelStatusView(String key, int channelId, String channelName, String channelType, String system,
                                    String site, Integer timeslot, String state, Long frequency, String decoder,
                                    RecordViews.IdentifierView to, RecordViews.IdentifierView from,
                                    String talkerAlias)
    {
        public static ChannelStatusView of(ChannelMetadata metadata, Channel channel, TalkgroupFormatSettings format)
        {
            Integer timeslot = metadata.hasTimeslot() ? metadata.getTimeslot() : null;
            int channelId = channel != null ? channel.getChannelID() : -1;
            return new ChannelStatusView(key(channelId, timeslot), channelId,
                channel != null ? channel.getName() : null,
                channel != null ? channel.getChannelType().name() : null,
                metadata.hasSystemConfigurationIdentifier() ? metadata.getSystemConfigurationIdentifier().getValue() :
                    (channel != null ? channel.getSystem() : null),
                metadata.hasSiteConfigurationIdentifier() ? metadata.getSiteConfigurationIdentifier().getValue() :
                    (channel != null ? channel.getSite() : null),
                timeslot,
                metadata.hasDecoderStateIdentifier() && metadata.getChannelStateIdentifier().getValue() != null ?
                    metadata.getChannelStateIdentifier().getValue().name() : null,
                metadata.hasFrequencyConfigurationIdentifier() ?
                    metadata.getFrequencyConfigurationIdentifier().getValue() : null,
                metadata.hasDecoderTypeIdentifier() && metadata.getDecoderTypeConfigurationIdentifier().getValue() !=
                    null ? metadata.getDecoderTypeConfigurationIdentifier().getValue().name() : null,
                identifier(metadata.getToIdentifier(), metadata.getToIdentifierAliases(), format),
                identifier(metadata.getFromIdentifier(), metadata.getFromIdentifierAliases(), format),
                metadata.hasTalkerAliasIdentifier() ? String.valueOf(metadata.getTalkerAliasIdentifier().getValue()) :
                    null);
        }

        /**
         * Key of a channel timeslot
         */
        public static String key(int channelId, Integer timeslot)
        {
            return timeslot != null ? channelId + ":" + timeslot : String.valueOf(channelId);
        }
    }

    private static RecordViews.IdentifierView identifier(Identifier identifier, List<Alias> aliases,
                                                         TalkgroupFormatSettings format)
    {
        if(identifier == null)
        {
            return null;
        }

        String formatted;

        try
        {
            formatted = format != null ? format.format(identifier) : String.valueOf(identifier.getValue());
        }
        catch(Exception e)
        {
            formatted = String.valueOf(identifier.getValue());
        }

        Alias alias = aliases != null && !aliases.isEmpty() ? aliases.getFirst() : null;
        String form = identifier instanceof PatchGroupIdentifier ? "PATCH_GROUP" : identifier.getForm().name();
        return RecordViews.IdentifierView.of(formatted, CallManager.getValue(identifier), form, null, alias);
    }
}
