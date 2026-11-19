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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasPriorityChangedEvent;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.audio.AbstractAudioModule;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.am.AMTalkgroup;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog;
import io.github.dsheirer.module.decode.nbfm.NBFMTalkgroup;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.ChannelMuteSettings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Now Playing channel table right-click action: mutes or unmutes a channel's audio to the local speakers.  Recording
 * and streaming are not affected.
 *
 * When the channel has an identifiable alias, Mute is a shortcut to that alias's Listen toggle in the alias editor and
 * is saved in the playlist.  When it has none, the channel gets its own mute, stored in the user preferences and
 * applied at the channel's audio modules.  Exactly one of the two governs a channel at a time.
 */
public class ChannelMuteAction
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelMuteAction.class);

    /**
     * Prefix the traffic channel managers give a traffic channel's name, ahead of the parent channel's name.
     */
    private static final String TRAFFIC_CHANNEL_NAME_PREFIX = "T-";

    /**
     * The alias each channel's most recent call was addressed to, keyed by getChannelMuteKey().  Used between calls,
     * when the channel has no live TO identifier.  Kept in memory only.
     */
    private final Map<String,Alias> mLastCallAlias = new ConcurrentHashMap<>();
    private final ChannelProcessingManager mChannelProcessingManager;
    private final PlaylistManager mPlaylistManager;
    private final ChannelMuteSettings mChannelMuteSettings;

    /**
     * Constructs an instance
     * @param playlistManager for the alias model and processing chains
     * @param channelMuteSettings for the mute state of channels with no alias
     */
    public ChannelMuteAction(PlaylistManager playlistManager, ChannelMuteSettings channelMuteSettings)
    {
        mPlaylistManager = playlistManager;
        mChannelProcessingManager = playlistManager.getChannelProcessingManager();
        mChannelMuteSettings = channelMuteSettings;
    }

    /**
     * Resolves the single alias whose Listen setting governs a channel's audio, or null when no single alias can be
     * identified.  Mute must change exactly the alias the user would have toggled by hand: falling back to every
     * alias in the channel's alias list would silence every other channel that shares the list.
     *
     * Resolution, most specific first:
     *
     * 1. The live TO identifier's alias, when it resolves to exactly one.  On a trunked system this is the talkgroup
     *    producing the audio.
     * 2. For a conventional NBFM or AM channel, the alias of the channel's configured talkgroup.  The live TO
     *    identifier only exists during a transmission, and the channel's alias list may hold several aliases.
     * 3. The alias the channel's most recent call was addressed to.  A digital conventional channel has no configured
     *    talkgroup, and its TO identifier is cleared when the call ends.
     * 4. The live FROM identifier's alias, when it resolves to exactly one.
     * 5. The only alias in the channel's alias list, when the list holds exactly one.
     *
     * Anything else is ambiguous and returns null.
     *
     * @param metadata for the channel's live identifiers
     * @param channel to resolve for
     * @return the governing alias, or null
     */
    Alias resolveChannelAlias(ChannelMetadata metadata, Channel channel)
    {
        List<Alias> toAliases = metadata.getToIdentifierAliases();

        if(toAliases != null && toAliases.size() == 1)
        {
            mLastCallAlias.put(getChannelMuteKey(channel), toAliases.get(0));
            return toAliases.get(0);
        }

        Alias configuredAlias = resolveConfiguredTalkgroupAlias(channel);

        if(configuredAlias != null)
        {
            return configuredAlias;
        }

        Alias lastCallAlias = mLastCallAlias.get(getChannelMuteKey(channel));

        if(lastCallAlias != null && mPlaylistManager.getAliasModel().getAliases().contains(lastCallAlias))
        {
            return lastCallAlias;
        }

        List<Alias> fromAliases = metadata.getFromIdentifierAliases();

        if(fromAliases != null && fromAliases.size() == 1)
        {
            return fromAliases.get(0);
        }

        String aliasListName = channel.getAliasListName();

        if(aliasListName != null && !aliasListName.isEmpty())
        {
            Alias only = null;

            for(Alias alias : mPlaylistManager.getAliasModel().getAliases())
            {
                if(alias.hasList() && aliasListName.equalsIgnoreCase(alias.getAliasListName()))
                {
                    if(only != null)
                    {
                        //More than one alias in the list - ambiguous
                        return null;
                    }

                    only = alias;
                }
            }

            return only;
        }

        return null;
    }

    /**
     * Records the TO alias of a channel whose TO identifier changed, so that resolveChannelAlias() can still find it
     * after the call ends.  Called on decoder threads.
     *
     * @param metadata that changed
     */
    void rememberCallAlias(ChannelMetadata metadata)
    {
        Channel channel = mChannelProcessingManager.getChannelMetadataModel().getChannelFromMetadata(metadata);

        if(channel != null)
        {
            List<Alias> toAliases = metadata.getToIdentifierAliases();

            if(toAliases != null && toAliases.size() == 1)
            {
                mLastCallAlias.put(getChannelMuteKey(channel), toAliases.get(0));
            }
        }
    }

    /**
     * Re-applies the mute of a channel that has no alias when its processing chain starts.  A channel muted through
     * its alias needs nothing here, because each new audio segment takes its monitor priority from its aliases.  A
     * channel with no alias is muted at its audio modules, which are new with each processing chain.
     *
     * @param channel that started
     */
    void reapplyChannelMute(Channel channel)
    {
        ChannelMetadata first = getFirstMetadata(channel);

        if(first != null && resolveChannelAlias(first, channel) == null
            && mChannelMuteSettings.isChannelMuted(getChannelMuteKey(channel)))
        {
            setProcessingChainMuted(mChannelProcessingManager.getProcessingChain(channel), true);
        }
    }

    /**
     * Resolves the alias of a conventional NBFM or AM channel's configured talkgroup, or null for any other channel or
     * when no single alias in the channel's alias list carries that talkgroup.
     *
     * @param channel to resolve for
     * @return the alias for the channel's configured talkgroup, or null
     */
    private Alias resolveConfiguredTalkgroupAlias(Channel channel)
    {
        if(!(channel.getDecodeConfiguration() instanceof DecodeConfigAnalog analogConfig))
        {
            return null;
        }

        String aliasListName = channel.getAliasListName();

        if(aliasListName == null || aliasListName.isEmpty())
        {
            return null;
        }

        AliasList aliasList = mPlaylistManager.getAliasModel().getAliasList(aliasListName);

        if(aliasList == null)
        {
            return null;
        }

        //Alias lookup is keyed on protocol, so the identifier must be the type the decoder publishes
        Identifier talkgroup = switch(analogConfig.getDecoderType())
        {
            case NBFM -> new NBFMTalkgroup(analogConfig.getTalkgroup());
            case AM -> new AMTalkgroup(analogConfig.getTalkgroup());
            default -> null;
        };

        if(talkgroup == null)
        {
            return null;
        }

        List<Alias> aliases = aliasList.getAliases(talkgroup);

        return (aliases != null && aliases.size() == 1) ? aliases.get(0) : null;
    }

    /**
     * Builds the identity under which the mute state of a channel with no alias is stored.
     *
     * Channel.getChannelID() changes on every run and every traffic channel gets a new one; system, site and name come
     * from the playlist and do not.  Traffic channels are named "T-" plus the parent channel's name and carry the
     * parent's system and site, so removing that prefix (from traffic channels only) gives a channel and its traffic
     * channels one identity.
     *
     * @param channel to identify
     * @return mute identity for the channel, never null
     */
    static String getChannelMuteKey(Channel channel)
    {
        String name = channel.getName() != null ? channel.getName() : "";

        if(channel.isTrafficChannel() && name.startsWith(TRAFFIC_CHANNEL_NAME_PREFIX))
        {
            name = name.substring(TRAFFIC_CHANNEL_NAME_PREFIX.length());
        }

        String system = channel.getSystem() != null ? channel.getSystem() : "";
        String site = channel.getSite() != null ? channel.getSite() : "";

        return (system + "|" + site + "|" + name).toLowerCase();
    }

    /**
     * Indicates if a channel's audio is muted to the local speakers.
     *
     * The channel's own mute is also checked when an alias resolves: a trunked channel muted between calls (no alias)
     * that then gets a call (alias) is still muted at its audio modules, and the menu must offer Unmute.
     * setChannelMuted() clears both on unmute, so the state cannot get stuck.
     *
     * @param metadata for the channel's live identifiers
     * @param channel to check
     * @return true if muted
     */
    boolean isChannelMuted(ChannelMetadata metadata, Channel channel)
    {
        Alias alias = resolveChannelAlias(metadata, channel);

        if(alias != null && alias.getPlaybackPriority() == Priority.DO_NOT_MONITOR)
        {
            return true;
        }

        return mChannelMuteSettings.isChannelMuted(getChannelMuteKey(channel));
    }

    /**
     * Mutes or unmutes a channel's audio to the local speakers.  Recording and streaming are not affected.
     *
     * @param metadata for the channel's live identifiers
     * @param channel to mute or unmute
     * @param mute true to mute
     */
    void setChannelMuted(ChannelMetadata metadata, Channel channel, boolean mute)
    {
        Alias alias = resolveChannelAlias(metadata, channel);

        if(alias != null)
        {
            setAliasMuted(alias, mute);

            //The alias holds the state now; discard any mute the channel got while it had no alias
            mChannelMuteSettings.setChannelMuted(getChannelMuteKey(channel), false);
        }
        else
        {
            String muteKey = getChannelMuteKey(channel);
            mChannelMuteSettings.setChannelMuted(muteKey, mute);

            for(Map.Entry<Channel,ProcessingChain> entry : mChannelProcessingManager.getProcessingChains().entrySet())
            {
                if(getChannelMuteKey(entry.getKey()).equals(muteKey))
                {
                    setProcessingChainMuted(entry.getValue(), mute);
                }
            }

            mLog.info("Channel [{}] (no alias) {} from the Now Playing window", muteKey, mute ? "muted" : "unmuted");
        }
    }

    /**
     * Sets the Listen setting of the alias governing a channel, as the alias editor's Listen toggle does.
     *
     * A segment's monitor priority is resolved from its aliases when the segment is created, so every running
     * processing chain governed by this alias has its current segment ended (and any channel mute cleared), which
     * makes the change heard immediately rather than at the end of the current call.
     *
     * @param alias governing the channel
     * @param mute true to mute (Listen off), false to unmute
     */
    private void setAliasMuted(Alias alias, boolean mute)
    {
        alias.setCallPriority(mute ? Priority.DO_NOT_MONITOR : Priority.DEFAULT_PRIORITY);
        mPlaylistManager.schedulePlaylistSave();
        MyEventBus.getGlobalEventBus().post(new AliasPriorityChangedEvent(alias));

        for(Map.Entry<Channel,ProcessingChain> entry : mChannelProcessingManager.getProcessingChains().entrySet())
        {
            ChannelMetadata chainMetadata = getFirstMetadata(entry.getKey());

            if(chainMetadata != null && resolveChannelAlias(chainMetadata, entry.getKey()) == alias)
            {
                setProcessingChainMuted(entry.getValue(), false);
            }
        }

        mLog.info("Alias [{}] in list [{}] {} from the Now Playing window", alias.getName(),
                alias.getAliasListName(), mute ? "muted" : "unmuted");
    }

    /**
     * Mutes or unmutes every audio module in a processing chain.
     *
     * @param processingChain to apply to, may be null
     * @param mute true to mute
     */
    private static void setProcessingChainMuted(ProcessingChain processingChain, boolean mute)
    {
        if(processingChain == null)
        {
            return;
        }

        for(Module module : processingChain.getModules())
        {
            if(module instanceof AbstractAudioModule audioModule)
            {
                audioModule.setMuted(mute);
            }
        }
    }

    /**
     * Returns any one channel metadata for a channel, used to resolve the governing alias of each running channel.
     *
     * @param channel to look up
     * @return one of the channel's metadata, or null if it has none
     */
    private ChannelMetadata getFirstMetadata(Channel channel)
    {
        ChannelMetadataModel model = mChannelProcessingManager.getChannelMetadataModel();

        for(ChannelMetadata candidate : model.getChannelMetadata())
        {
            if(channel.equals(model.getChannelFromMetadata(candidate)))
            {
                return candidate;
            }
        }

        return null;
    }
}
