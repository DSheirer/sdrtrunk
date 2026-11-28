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
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.backup.BackupInfo;
import io.github.dsheirer.database.backup.BackupManager;
import io.github.dsheirer.database.playlist.PlaylistItem;
import io.github.dsheirer.database.playlist.PlaylistItemType;
import io.github.dsheirer.database.playlist.PlaylistRecord;
import io.github.dsheirer.database.playlist.PlaylistRepository;
import io.github.dsheirer.playlist.PlaylistStore.PlaylistReplacedException;
import io.github.dsheirer.playlist.PlaylistStore.PlaylistStoreException;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.Settings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Playlist storage in the built-in (H2) database: import of the playlist files of older versions (which must not
 * change), incremental saves, playlist management, database switch and backup restore.
 */
public class PlaylistStoreTest
{
    @TempDir
    Path mTempDir;

    private Settings mSettings;
    private DatabaseManager mDatabaseManager;

    @BeforeEach
    void setUp()
    {
        clearPreferences();
        mSettings = new Settings();
        mSettings.getDirectorySettings().setDirectoryApplicationRoot(mTempDir);
        mSettings.getDatabaseSettings().setDatabaseConfiguration(h2(DatabaseConfiguration.DEFAULT_SCHEMA));
    }

    @AfterEach
    void tearDown()
    {
        if(mDatabaseManager != null)
        {
            mDatabaseManager.stop();
        }

        mSettings.getDirectorySettings().resetDirectoryApplicationRoot();
        clearPreferences();
    }

    private static void clearPreferences()
    {
        Setting.PLAYLIST_ACTIVE.with().remove();
        Setting.PLAYLIST_LIST.with().remove();
        Setting.PLAYLIST_PATH.with().remove();
        Setting.DATABASE_TYPE.with().remove();
        Setting.DATABASE_SCHEMA.with().remove();
    }

    private static DatabaseConfiguration h2(String schema)
    {
        return new DatabaseConfiguration(DatabaseType.H2, "", 0, "", schema, "", "");
    }

    private PlaylistStore startStore()
    {
        if(mDatabaseManager == null)
        {
            mDatabaseManager = new DatabaseManager(mSettings);
            mDatabaseManager.start();
        }

        return new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings());
    }

    private Path playlistDirectory()
    {
        return mSettings.getDirectorySettings().getDirectoryPlaylist();
    }

    private List<PlaylistItem> storedItems(String name) throws Exception
    {
        return mDatabaseManager.submit(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                return repository.readItems(connection, repository.find(connection, name).orElseThrow().id());
            }
        }).get();
    }

    private static Map<String,Long> idsByName(List<PlaylistItem> items)
    {
        Map<String,Long> ids = new HashMap<>();

        for(PlaylistItem item: items)
        {
            ids.put(item.type() + ":" + item.name(), item.id());
        }

        return ids;
    }

    @Test
    void importsPlaylistFilesOnceWithoutChangingThem() throws Exception
    {
        Path current = playlistDirectory().resolve("default.xml");
        Path other = playlistDirectory().resolve("Other.xml");
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), current);
        PlaylistV2 otherPlaylist = new PlaylistV2();
        otherPlaylist.getChannels().add(new Channel("Other Channel"));
        PlaylistCodec.writeFile(otherPlaylist, other);
        Setting.PLAYLIST_LIST.put(current + "," + other);

        FileTime time = FileTime.fromMillis(1_500_000_000_000L);
        Files.setLastModifiedTime(current, time);
        Files.setLastModifiedTime(other, time);
        byte[] currentBytes = Files.readAllBytes(current);
        byte[] otherBytes = Files.readAllBytes(other);

        PlaylistStore store = startStore();
        PlaylistV2 loaded = store.loadActive();

        assertEquals("default", store.getActiveName());
        assertEquals("default", mSettings.getPlaylistSettings().getActivePlaylist());
        assertEquals(2, loaded.getAliases().size());
        assertEquals(1, loaded.getChannels().size());
        assertEquals(1, loaded.getBroadcastConfigurations().size());
        assertEquals(1, loaded.getChannelMaps().size());
        assertEquals(1, loaded.getAliasListConfigurations().size());

        List<PlaylistRecord> playlists = store.list();
        assertEquals(List.of("default", "Other"), playlists.stream().map(PlaylistRecord::name).toList());
        assertEquals(2, playlists.getFirst().aliases());
        assertEquals(1, playlists.get(1).channels());

        //The files are unchanged, and no lock or backup files were created
        assertArrayEquals(currentBytes, Files.readAllBytes(current));
        assertArrayEquals(otherBytes, Files.readAllBytes(other));
        assertEquals(time, Files.getLastModifiedTime(current));
        assertEquals(time, Files.getLastModifiedTime(other));
        assertFalse(Files.exists(Path.of(current + ".backup")));
        assertFalse(Files.exists(Path.of(current + ".lck")));
        assertEquals(currentBytes.length, Files.size(current));

        //Saving changes doesn't touch the files either
        loaded.getAliases().getFirst().setName("Renamed");
        store.save(PlaylistCodec.getModelObjects(loaded), PlaylistManager.PLAYLIST_CURRENT_VERSION);
        assertArrayEquals(currentBytes, Files.readAllBytes(current));
        assertEquals(time, Files.getLastModifiedTime(current));

        //A changed file isn't imported again
        PlaylistCodec.writeFile(new PlaylistV2(), current);
        PlaylistStore second = new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings());
        PlaylistV2 reloaded = second.loadActive();
        assertEquals(2, reloaded.getAliases().size());
        assertEquals("Renamed", reloaded.getAliases().getFirst().getName());
    }

    @Test
    void importsBackupWhenOlderVersionSaveWasIncomplete() throws Exception
    {
        Path current = playlistDirectory().resolve("default.xml");
        Files.writeString(current, "<playlist version=\"4\"><alias name=\"trunc");
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), Path.of(current + ".backup"));
        Files.createFile(Path.of(current.toAbsolutePath() + ".lck"));

        PlaylistStore store = startStore();
        assertEquals(2, store.loadActive().getAliases().size());

        //The lock file is left for the older version to recover from
        assertTrue(Files.exists(Path.of(current.toAbsolutePath() + ".lck")));
    }

    @Test
    void createsDefaultPlaylistWithoutFiles() throws Exception
    {
        PlaylistStore store = startStore();
        PlaylistV2 playlist = store.loadActive();
        assertEquals(PlaylistStore.DEFAULT_PLAYLIST_NAME, store.getActiveName());
        assertTrue(playlist.getChannels().isEmpty());
        assertEquals(1, store.list().size());
    }

    @Test
    void saveWritesOnlyChangedItems() throws Exception
    {
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), playlistDirectory().resolve("default.xml"));
        PlaylistStore store = startStore();
        PlaylistV2 playlist = store.loadActive();
        Map<String,Long> before = idsByName(storedItems("default"));

        //No changes - nothing written
        assertFalse(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));

        //Change one alias, add an alias, remove the channel
        Alias changed = playlist.getAliases().getFirst();
        changed.setGroup("Changed Group");
        Alias added = new Alias("Added");
        added.setAliasListName("Metro");
        playlist.getAliases().add(added);
        playlist.getChannels().clear();
        assertTrue(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));

        List<PlaylistItem> items = storedItems("default");
        Map<String,Long> after = idsByName(items);
        assertEquals(before.get("ALIAS:Fire Dispatch"), after.get("ALIAS:Fire Dispatch"), "changed alias keeps its row");
        assertEquals(before.get("BROADCAST:Calls"), after.get("BROADCAST:Calls"));
        assertFalse(after.containsKey("CHANNEL:Control"));
        assertTrue(after.containsKey("ALIAS:Added"));
        assertEquals(6, items.size());

        //A new store reads the same content
        PlaylistV2 reloaded = new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings()).load("default");
        assertEquals(3, reloaded.getAliases().size());
        assertEquals("Changed Group", reloaded.getAliases().getFirst().getGroup());
        assertTrue(reloaded.getChannels().isEmpty());

        //A second save with the new rows tracked writes nothing
        assertFalse(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));
    }

    @Test
    void manyNewItemsAreWrittenTogether() throws Exception
    {
        PlaylistStore store = startStore();
        PlaylistV2 playlist = store.loadActive();

        for(int x = 0; x < 500; x++)
        {
            Alias alias = new Alias("Alias " + x);
            alias.setAliasListName("Metro");
            playlist.getAliases().add(alias);
        }

        assertTrue(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));
        assertEquals(500, storedItems("default").size());
        playlist.getAliases().get(250).setName("Changed");
        assertTrue(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));
        assertFalse(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));
        assertEquals("Changed", new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings())
            .load("default").getAliases().get(250).getName());
    }

    @Test
    void unreadableItemsAreKept() throws Exception
    {
        PlaylistStore store = startStore();
        PlaylistV2 playlist = store.loadActive();
        long playlistId = store.list().getFirst().id();

        mDatabaseManager.submit(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getPlaylistRepository().insertItem(connection, new PlaylistItem(0, playlistId,
                    PlaylistItemType.CHANNEL, "Broken", "<channel><decode_configuration type=\"NoSuchDecoder\"/>" +
                    "</channel>"));
            }
        }).get();

        playlist = store.load("default");
        assertTrue(playlist.getChannels().isEmpty());
        playlist.getAliases().add(new Alias("New"));
        store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION);
        assertTrue(idsByName(storedItems("default")).containsKey("CHANNEL:Broken"));
    }

    @Test
    void managesPlaylists() throws Exception
    {
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), playlistDirectory().resolve("default.xml"));
        PlaylistStore store = startStore();
        store.loadActive();

        store.copy("default", "Copy");
        assertEquals(2, store.load("Copy").getAliases().size());
        assertEquals("Copy", mSettings.getPlaylistSettings().getActivePlaylist());

        assertThrows(PlaylistStoreException.class, () -> store.delete("Copy"), "active playlist");
        assertThrows(PlaylistStoreException.class, () -> store.create("default"), "duplicate");
        assertThrows(PlaylistStoreException.class, () -> store.rename("Copy", "default"), "duplicate");
        assertThrows(PlaylistStoreException.class, () -> store.create("  "), "blank");

        store.rename("Copy", "Renamed");
        assertEquals("Renamed", store.getActiveName());
        assertEquals("Renamed", mSettings.getPlaylistSettings().getActivePlaylist());

        store.create("Empty");
        store.delete("Empty");
        assertEquals(List.of("default", "Renamed"), store.list().stream().map(PlaylistRecord::name).toList());

        //Export and import
        Path exported = mTempDir.resolve("exported.xml");
        store.exportFile("Renamed", exported);
        Path original = mTempDir.resolve("original.xml");
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), original);
        assertEquals(Files.readString(original), Files.readString(exported));

        store.importFile(exported, "Imported");
        assertEquals(2, store.load("Imported").getAliases().size());
        Files.writeString(mTempDir.resolve("bad.xml"), "not a playlist");
        assertThrows(PlaylistStoreException.class, () -> store.importFile(mTempDir.resolve("bad.xml"), "Bad"));
        assertEquals(3, store.list().size());

        //Active playlist that no longer exists falls back to the default playlist
        Setting.PLAYLIST_ACTIVE.put("Missing");
        PlaylistStore second = new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings());
        second.loadActive();
        assertEquals("default", second.getActiveName());
    }

    @Test
    void switchWritesRunningPlaylistToNewDatabase() throws Exception
    {
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), playlistDirectory().resolve("default.xml"));
        PlaylistStore store = startStore();
        PlaylistV2 playlist = store.loadActive();
        store.create("Second");

        //Unsaved change at the time of the switch
        playlist.getAliases().getFirst().setName("Unsaved");

        mDatabaseManager.switchDatabase(h2("sdrtrunk_switched"), DatabaseManager.ExistingSchemaOption.CLEAR, false,
            status -> {}).get();
        assertEquals("sdrtrunk_switched", mDatabaseManager.getDatabase().getConfiguration().schema());

        //Missing playlists are copied
        assertEquals(List.of("default", "Second"), store.list().stream().map(PlaylistRecord::name).toList());
        assertEquals("Fire Dispatch", new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings())
            .load("default").getAliases().getFirst().getName());

        //The save writes the running playlist to the new database
        assertTrue(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));
        assertEquals("Unsaved", new PlaylistStore(mDatabaseManager, mSettings.getPlaylistSettings())
            .load("default").getAliases().getFirst().getName());
        assertFalse(store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION));
    }

    @Test
    void restoreReplacesPlaylists() throws Exception
    {
        PlaylistCodec.writeFile(PlaylistCodecTest.createPlaylist(), playlistDirectory().resolve("default.xml"));
        PlaylistStore store = startStore();
        PlaylistV2 playlist = store.loadActive();
        CallDatabase database = mDatabaseManager.getDatabase();
        BackupInfo backup = BackupManager.writeBackup(database, mTempDir.resolve("backups"), null,
            System.currentTimeMillis());
        assertEquals(1, backup.metadata().playlists());

        //Change after the backup
        playlist.getAliases().clear();
        store.save(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION);
        store.create("After Backup");

        BackupManager.RestoreResult result = mDatabaseManager.restore(backup.path(), status -> {}).get();
        assertEquals(1, result.playlists());

        //The running playlist must not be written over the restored playlists
        playlist.getAliases().add(new Alias("Not Saved"));
        assertThrows(PlaylistReplacedException.class, () -> store.save(PlaylistCodec.getModelObjects(playlist),
            PlaylistManager.PLAYLIST_CURRENT_VERSION));

        store.unload();
        PlaylistV2 restored = store.loadActive();
        assertEquals(2, restored.getAliases().size());
        assertEquals(List.of("default"), store.list().stream().map(PlaylistRecord::name).toList());
        assertNotEquals(0, storedItems("default").size());

        //Saves work again after the reload
        restored.getAliases().add(new Alias("Saved"));
        assertTrue(store.save(PlaylistCodec.getModelObjects(restored), PlaylistManager.PLAYLIST_CURRENT_VERSION));
    }

    @Test
    void unavailableDatabaseFailsLoad() throws Exception
    {
        mSettings.getDatabaseSettings().setDatabaseConfiguration(new DatabaseConfiguration(DatabaseType.POSTGRESQL,
            "127.0.0.1", 1, "postgres", "sdrtrunk", "nobody", "nothing"));
        PlaylistStore store = startStore();
        assertFalse(mDatabaseManager.isConnected());
        assertThrows(PlaylistStoreException.class, store::loadActive);
        assertEquals(null, store.getActiveName());
        assertThrows(PlaylistStoreException.class, () -> store.save(new ArrayList<>(), 4));
    }
}
