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

package io.github.dsheirer.playlist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasListConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.database.playlist.PlaylistItem;
import io.github.dsheirer.database.playlist.PlaylistItemType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts playlists between the playlist XML file format, the playlist model objects and the stored playlist items.
 * Each stored item is the same Jackson XML that the item has inside a playlist file, so a playlist exported from the
 * database is a normal playlist file that older versions of sdrtrunk can load.
 */
public final class PlaylistCodec
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistCodec.class);
    private static final ObjectMapper MAPPER = createMapper();

    private PlaylistCodec()
    {
    }

    private static ObjectMapper createMapper()
    {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        return new XmlMapper(xmlModule).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Reads a playlist file
     * @throws IOException if the file can't be read or isn't a playlist
     */
    public static PlaylistV2 readFile(Path path) throws IOException
    {
        try(InputStream in = Files.newInputStream(path))
        {
            PlaylistV2 playlist = MAPPER.readValue(in, PlaylistV2.class);

            if(playlist == null)
            {
                throw new IOException("The file is empty");
            }

            return playlist;
        }
    }

    /**
     * Indicates if the file is a playlist file that can be read.
     */
    public static boolean isPlaylist(Path path)
    {
        if(path == null || !Files.exists(path))
        {
            return false;
        }

        try
        {
            readFile(path);
            return true;
        }
        catch(IOException ioe)
        {
            LOGGER.error("Unable to read playlist file [" + path + "] - " + ioe.getMessage());
            return false;
        }
    }

    /**
     * Writes the playlist to a playlist file (indented), replacing an existing file.  The playlist is written to a
     * temporary file first so that an error doesn't leave a partial file.
     */
    public static void writeFile(PlaylistV2 playlist, Path path) throws IOException
    {
        Path temp = path.resolveSibling(path.getFileName() + ".part");

        try(OutputStream out = Files.newOutputStream(temp))
        {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(out, playlist);
        }
        catch(IOException ioe)
        {
            Files.deleteIfExists(temp);
            throw ioe;
        }

        Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Item type of a playlist model object
     * @throws IllegalArgumentException for an object that isn't a playlist item
     */
    public static PlaylistItemType getType(Object item)
    {
        return switch(item)
        {
            case Alias alias -> PlaylistItemType.ALIAS;
            case AliasListConfiguration configuration -> PlaylistItemType.ALIAS_LIST;
            case BroadcastConfiguration configuration -> PlaylistItemType.BROADCAST;
            case Channel channel -> PlaylistItemType.CHANNEL;
            case ChannelMap channelMap -> PlaylistItemType.CHANNEL_MAP;
            default -> throw new IllegalArgumentException("Not a playlist item: " + item.getClass());
        };
    }

    /**
     * Model class for the item type
     */
    public static Class<?> getModelClass(PlaylistItemType type)
    {
        return switch(type)
        {
            case ALIAS -> Alias.class;
            case ALIAS_LIST -> AliasListConfiguration.class;
            case BROADCAST -> BroadcastConfiguration.class;
            case CHANNEL -> Channel.class;
            case CHANNEL_MAP -> ChannelMap.class;
        };
    }

    /**
     * Name of a playlist model object, for the item row
     */
    public static String getName(Object item)
    {
        return switch(item)
        {
            case Alias alias -> alias.getName();
            case AliasListConfiguration configuration -> configuration.getName();
            case BroadcastConfiguration configuration -> configuration.getName();
            case Channel channel -> channel.getName();
            case ChannelMap channelMap -> channelMap.getName();
            default -> null;
        };
    }

    /**
     * Serializes the playlist model object to its stored form.  The object is written as its item type's base class,
     * so that the type of a broadcast configuration is included.
     * @throws IOException on serialization error
     */
    public static String serialize(Object item) throws IOException
    {
        return MAPPER.writerFor(getModelClass(getType(item))).writeValueAsString(item);
    }

    /**
     * Creates the stored (unsaved) item for the playlist model object
     */
    public static PlaylistItem toItem(Object item) throws IOException
    {
        return new PlaylistItem(0, 0, getType(item), getName(item), serialize(item));
    }

    /**
     * Deserializes a stored item to its playlist model object
     * @throws IOException on deserialization error
     */
    public static Object deserialize(PlaylistItem item) throws IOException
    {
        Object value = MAPPER.readValue(item.config(), getModelClass(item.type()));

        if(value == null)
        {
            throw new IOException("Empty playlist item");
        }

        return value;
    }

    /**
     * The model objects of the playlist in stored item order: aliases, alias lists, broadcast configurations, channel
     * maps and then channels.
     */
    public static List<Object> getModelObjects(PlaylistV2 playlist)
    {
        List<Object> objects = new ArrayList<>();
        addAll(objects, playlist.getAliases());
        addAll(objects, playlist.getAliasListConfigurations());
        addAll(objects, playlist.getBroadcastConfigurations());
        addAll(objects, playlist.getChannelMaps());
        addAll(objects, playlist.getChannels());
        return objects;
    }

    private static void addAll(List<Object> objects, List<?> items)
    {
        if(items != null)
        {
            for(Object item: items)
            {
                if(item != null)
                {
                    objects.add(item);
                }
            }
        }
    }

    /**
     * Creates the stored (unsaved) items for each item of the playlist file
     */
    public static List<PlaylistItem> toItems(PlaylistV2 playlist) throws IOException
    {
        List<PlaylistItem> items = new ArrayList<>();

        for(Object object: getModelObjects(playlist))
        {
            items.add(toItem(object));
        }

        return items;
    }

    /**
     * Adds the deserialized model object to the playlist.
     */
    public static void add(PlaylistV2 playlist, Object object)
    {
        switch(object)
        {
            case Alias alias -> playlist.getAliases().add(alias);
            case AliasListConfiguration configuration -> playlist.getAliasListConfigurations().add(configuration);
            case BroadcastConfiguration configuration -> playlist.getBroadcastConfigurations().add(configuration);
            case Channel channel -> playlist.getChannels().add(channel);
            case ChannelMap channelMap -> playlist.getChannelMaps().add(channelMap);
            default -> throw new IllegalArgumentException("Not a playlist item: " + object.getClass());
        }
    }

    /**
     * Creates a playlist from stored items.  Items that can't be deserialized are logged and left out.
     * @param items stored
     * @param version of the stored playlist
     */
    public static PlaylistV2 fromItems(List<PlaylistItem> items, int version)
    {
        PlaylistV2 playlist = new PlaylistV2();
        playlist.setVersion(version);

        for(PlaylistItem item: items)
        {
            try
            {
                add(playlist, deserialize(item));
            }
            catch(Exception e)
            {
                LOGGER.error("Unable to read stored playlist " + item.type() + " item [" + item.name() + "] id " +
                    item.id() + " - " + e.getMessage());
            }
        }

        return playlist;
    }
}
