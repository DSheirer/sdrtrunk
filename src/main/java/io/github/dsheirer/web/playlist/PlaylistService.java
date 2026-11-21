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

package io.github.dsheirer.web.playlist;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.playlist.PlaylistConfig.AliasConfig;
import io.github.dsheirer.web.playlist.PlaylistConfig.ChannelConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Web API edits to the active playlist: channels, aliases and channel processing.  Every change runs on the
 * {@link ModelThread}, where the version check and the change that follows it can't interleave with another edit,
 * and goes through the same model objects and notifications as the desktop editors, so the playlist saves itself and
 * open editors see the change.
 *
 * Updates and deletes need the resource's current version (ETag) as If-Match: a stale version gets 412, so an edit
 * never silently overwrites a change made elsewhere, including in the desktop editors.
 */
public class PlaylistService
{
    private final ChannelModel mChannelModel;
    private final AliasModel mAliasModel;
    private final ChannelProcessingManager mProcessingManager;
    private final AliasRegistry mAliasRegistry;

    /**
     * Constructs an instance
     * @param channelModel channels of the active playlist
     * @param aliasModel aliases of the active playlist
     * @param processingManager to start and stop channels, or null when channels can't be started (tests)
     * @param aliasRegistry ids for aliases
     */
    public PlaylistService(ChannelModel channelModel, AliasModel aliasModel, ChannelProcessingManager processingManager,
                           AliasRegistry aliasRegistry)
    {
        mChannelModel = channelModel;
        mAliasModel = aliasModel;
        mProcessingManager = processingManager;
        mAliasRegistry = aliasRegistry;
    }

    public AliasRegistry getAliasRegistry()
    {
        return mAliasRegistry;
    }

    /**
     * A representation with its version
     */
    public record Versioned<T>(T value, String version) {}

    /**
     * Result of a channel update
     * @param restartRequired true when the channel is processing: the change applies the next time it starts
     */
    public record ChannelUpdate(Versioned<ChannelConfig> channel, boolean restartRequired) {}

    //----------------------------------------------------------------------------------------------------------------
    // Channels
    //----------------------------------------------------------------------------------------------------------------

    public Versioned<ChannelConfig> getChannel(int id)
    {
        return ModelThread.call(() -> versioned(findChannel(id)));
    }

    public Versioned<ChannelConfig> createChannel(ChannelConfig config)
    {
        PlaylistConfig.Parsed parsed = config.parse();

        return ModelThread.call(() -> {
            Channel channel = new Channel();
            parsed.applyTo(channel);
            ensureAliasList(channel.getAliasListName());
            mChannelModel.addChannel(channel);
            return versioned(channel);
        });
    }

    public ChannelUpdate updateChannel(int id, String ifMatch, ChannelConfig config)
    {
        PlaylistConfig.Parsed parsed = config.parse();

        return ModelThread.call(() -> {
            Channel channel = findChannel(id);
            checkVersion(ifMatch, PlaylistConfig.version(channel));
            parsed.applyTo(channel);
            ensureAliasList(channel.getAliasListName());
            //Notifies the playlist (save) and the open editors, as a desktop edit does
            mChannelModel.receive(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_CONFIGURATION_CHANGE));
            return new ChannelUpdate(versioned(channel), channel.isProcessing());
        });
    }

    /**
     * Deletes the channel, stopping it first when it is processing.
     */
    public void deleteChannel(int id, String ifMatch)
    {
        ModelThread.call(() -> {
            Channel channel = findChannel(id);
            checkVersion(ifMatch, PlaylistConfig.version(channel));

            if(channel.isProcessing() && mProcessingManager != null)
            {
                mProcessingManager.stop(channel);
            }

            mChannelModel.removeChannel(channel);
            return null;
        });
    }

    /**
     * Starts processing the channel.
     * @throws ApiException 409 when it can't start, for example when no tuner can provide the frequency
     */
    public void start(int id)
    {
        ModelThread.call(() -> {
            Channel channel = findChannel(id);

            if(mProcessingManager == null)
            {
                throw ApiException.unavailable("Channel processing isn't available");
            }

            if(!channel.isProcessing())
            {
                try
                {
                    mProcessingManager.start(channel);
                }
                catch(ChannelException ce)
                {
                    throw ApiException.conflict("The channel couldn't start: " + ce.getMessage());
                }
            }

            return null;
        });
    }

    /**
     * Stops processing the channel.
     */
    public void stop(int id)
    {
        ModelThread.call(() -> {
            Channel channel = findChannel(id);

            if(mProcessingManager == null)
            {
                throw ApiException.unavailable("Channel processing isn't available");
            }

            if(channel.isProcessing())
            {
                try
                {
                    mProcessingManager.stop(channel);
                }
                catch(ChannelException ce)
                {
                    throw ApiException.conflict("The channel couldn't stop: " + ce.getMessage());
                }
            }

            return null;
        });
    }

    private Channel findChannel(int id)
    {
        for(Channel channel: mChannelModel.getChannels())
        {
            if(channel.getChannelID() == id)
            {
                return channel;
            }
        }

        throw ApiException.notFound("No channel with id " + id);
    }

    private static Versioned<ChannelConfig> versioned(Channel channel)
    {
        ChannelConfig config = ChannelConfig.of(channel);
        return new Versioned<>(config, PlaylistJson.version(config));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Aliases
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Alias list names with their alias counts, in name order
     */
    public Map<String,Integer> getAliasLists()
    {
        return ModelThread.call(() -> {
            Map<String,Integer> lists = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for(String name: mAliasModel.getListNames())
            {
                lists.put(name, 0);
            }

            for(Alias alias: mAliasModel.getAliases())
            {
                if(alias.getAliasListName() != null)
                {
                    lists.merge(alias.getAliasListName(), 1, Integer::sum);
                }
            }

            return lists;
        });
    }

    /**
     * Aliases in id order.
     * @param list only this alias list, or null for all
     * @param afterId paging cursor, 0 for the first page
     * @param limit page size: reads one extra so the caller knows whether there is another page
     */
    public List<Versioned<AliasConfig>> listAliases(String list, long afterId, int limit)
    {
        return ModelThread.call(() -> {
            List<Versioned<AliasConfig>> aliases = new ArrayList<>();

            for(Alias alias: mAliasModel.getAliases())
            {
                if(list == null || list.equalsIgnoreCase(alias.getAliasListName()))
                {
                    long id = mAliasRegistry.getId(alias);

                    if(id > afterId)
                    {
                        aliases.add(versioned(id, alias));
                    }
                }
            }

            aliases.sort(Comparator.comparingLong(alias -> alias.value().id()));
            return aliases.size() > limit + 1 ? new ArrayList<>(aliases.subList(0, limit + 1)) : aliases;
        });
    }

    public Versioned<AliasConfig> getAlias(long id)
    {
        return ModelThread.call(() -> versioned(id, findAlias(id)));
    }

    public Versioned<AliasConfig> createAlias(AliasConfig config)
    {
        return ModelThread.call(() -> {
            Alias alias = new Alias();
            config.applyTo(alias);
            ensureAliasList(alias.getAliasListName());
            mAliasModel.addAlias(alias);
            return versioned(mAliasRegistry.getId(alias), alias);
        });
    }

    public Versioned<AliasConfig> updateAlias(long id, String ifMatch, AliasConfig config)
    {
        return ModelThread.call(() -> {
            Alias alias = findAlias(id);
            checkVersion(ifMatch, PlaylistConfig.version(alias));

            //Validate on a copy first, so a bad request leaves the alias unchanged
            config.applyTo(new Alias());
            String previousList = alias.getAliasListName();
            config.applyTo(alias);
            ensureAliasList(alias.getAliasListName());

            //Update the lookup lists that the decoders use, as the desktop alias editor does on save
            if(previousList != null && !previousList.equals(alias.getAliasListName()))
            {
                io.github.dsheirer.alias.AliasList previous = mAliasModel.getAliasList(previousList);

                if(previous != null)
                {
                    previous.removeAlias(alias);
                }
            }

            io.github.dsheirer.alias.AliasList current = mAliasModel.getAliasList(alias.getAliasListName());

            if(current != null)
            {
                current.updateAlias(alias);
            }

            return versioned(id, alias);
        });
    }

    public void deleteAlias(long id, String ifMatch)
    {
        ModelThread.call(() -> {
            Alias alias = findAlias(id);
            checkVersion(ifMatch, PlaylistConfig.version(alias));
            mAliasModel.removeAlias(alias);
            mAliasRegistry.remove(alias);
            return null;
        });
    }

    private Alias findAlias(long id)
    {
        Alias alias = mAliasRegistry.getAlias(id);

        if(alias == null || !mAliasModel.aliasList().contains(alias))
        {
            throw ApiException.notFound("No alias with id " + id);
        }

        return alias;
    }

    private static Versioned<AliasConfig> versioned(long id, Alias alias)
    {
        return new Versioned<>(AliasConfig.of(id, alias), PlaylistConfig.version(alias));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Support
    //----------------------------------------------------------------------------------------------------------------

    private void ensureAliasList(String name)
    {
        if(name != null && !mAliasModel.getListNames().contains(name))
        {
            mAliasModel.addAliasList(name);
        }
    }

    /**
     * Checks the If-Match version.
     * @throws ApiException 428 when it is missing, 412 when it isn't the current version
     */
    static void checkVersion(String ifMatch, String current)
    {
        if(ifMatch == null || ifMatch.isBlank())
        {
            throw new ApiException(428, "Precondition required", "Send the ETag from the last GET as If-Match, " +
                "so the change doesn't overwrite an edit made elsewhere");
        }

        if(ifMatch.trim().equals("*"))
        {
            return;
        }

        for(String candidate: ifMatch.split(","))
        {
            String tag = candidate.trim();

            if(tag.startsWith("W/"))
            {
                tag = tag.substring(2);
            }

            if(tag.equals(current))
            {
                return;
            }
        }

        throw new ApiException(412, "Precondition failed", "It was changed since you read it (current version " +
            current + ").  Read it again and reapply your change.");
    }
}
