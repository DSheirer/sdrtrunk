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

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasListConfiguration;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelRange;
import io.github.dsheirer.database.playlist.PlaylistItem;
import io.github.dsheirer.database.playlist.PlaylistItemType;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.protocol.Protocol;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that each playlist item serializes to a stored item and back without change, and that a playlist made from
 * stored items writes the same playlist file as the original playlist.
 */
public class PlaylistCodecTest
{
    @TempDir
    Path mTempDir;

    static PlaylistV2 createPlaylist()
    {
        PlaylistV2 playlist = new PlaylistV2();

        Alias alias = new Alias("Fire Dispatch");
        alias.setAliasListName("Metro");
        alias.setGroup("Fire");
        alias.addAliasID(new Talkgroup(Protocol.APCO25, 1234));
        playlist.getAliases().add(alias);

        Alias alias2 = new Alias("Police <Main> & \"Ops\" ✓");
        alias2.setAliasListName("Metro");
        alias2.addAliasID(new Talkgroup(Protocol.APCO25, 4321));
        playlist.getAliases().add(alias2);

        AliasListConfiguration aliasList = new AliasListConfiguration("Metro");
        aliasList.setRadioReferenceSystemId(1234);
        aliasList.setRadioReferenceSystemName("Metro P25");
        playlist.getAliasListConfigurations().add(aliasList);

        BroadcastifyCallConfiguration broadcast = new BroadcastifyCallConfiguration();
        broadcast.setName("Calls");
        broadcast.setHost("https://api.example.com/call-upload");
        playlist.getBroadcastConfigurations().add(broadcast);

        ChannelMap channelMap = new ChannelMap("Map");
        channelMap.addRange(new ChannelRange(1, 100, 851_000_000, 12_500));
        playlist.getChannelMaps().add(channelMap);

        Channel channel = new Channel("Control");
        channel.setSystem("Metro");
        channel.setAliasListName("Metro");
        channel.setAutoStart(true);
        channel.setDecodeConfiguration(new DecodeConfigP25Phase1());
        playlist.getChannels().add(channel);
        return playlist;
    }

    @Test
    void itemsRoundTrip() throws Exception
    {
        for(Object object: PlaylistCodec.getModelObjects(createPlaylist()))
        {
            PlaylistItem item = PlaylistCodec.toItem(object);
            Object copy = PlaylistCodec.deserialize(item);
            assertEquals(object.getClass(), copy.getClass(), "class of " + item.type());
            assertEquals(item.config(), PlaylistCodec.serialize(copy), "round trip of " + item.type());
            assertEquals(PlaylistCodec.getName(object), item.name());
        }
    }

    @Test
    void broadcastTypeIsKept() throws Exception
    {
        PlaylistItem item = PlaylistCodec.toItem(createPlaylist().getBroadcastConfigurations().getFirst());
        assertEquals(PlaylistItemType.BROADCAST, item.type());
        Object copy = PlaylistCodec.deserialize(item);
        assertInstanceOf(BroadcastifyCallConfiguration.class, copy);
        assertEquals("https://api.example.com/call-upload", ((BroadcastConfiguration)copy).getHost());
    }

    @Test
    void playlistFromItemsMatchesFile() throws Exception
    {
        PlaylistV2 playlist = createPlaylist();
        Path original = mTempDir.resolve("original.xml");
        PlaylistCodec.writeFile(playlist, original);

        //File -> items -> playlist -> file
        List<PlaylistItem> items = PlaylistCodec.toItems(PlaylistCodec.readFile(original));
        PlaylistV2 restored = PlaylistCodec.fromItems(items, playlist.getVersion());
        Path copy = mTempDir.resolve("copy.xml");
        PlaylistCodec.writeFile(restored, copy);

        assertEquals(java.nio.file.Files.readString(original), java.nio.file.Files.readString(copy));
        assertEquals(2, restored.getAliases().size());
        assertEquals(1, restored.getChannels().size());
        assertTrue(PlaylistCodec.isPlaylist(copy));
        assertFalse(PlaylistCodec.isPlaylist(mTempDir.resolve("missing.xml")));
    }
}
