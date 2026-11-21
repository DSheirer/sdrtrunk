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

import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.playlist.PlaylistItem;
import io.github.dsheirer.database.playlist.PlaylistRecord;
import io.github.dsheirer.database.playlist.PlaylistRepository;
import io.github.dsheirer.settings.playlist.PlaylistSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores playlists in the database.  Tracks the database row of each model object (channel, alias, etc.) of the
 * loaded playlist, so that a save writes only the items that were added, changed or removed since the last save.
 *
 * The first time that the database has no playlists, the playlist files that older versions of sdrtrunk use are
 * imported.  The files are only read: they are never changed or deleted, so an older version still loads them.
 *
 * Methods block while the database thread does the work, so don't invoke them on the database thread.
 */
public class PlaylistStore
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistStore.class);
    public static final String DEFAULT_PLAYLIST_NAME = "default";
    private static final long TIMEOUT_SECONDS = 120;
    /**
     * Above this many new items, a save replaces all of the playlist's rows in a batch instead of inserting the new
     * items one at a time.
     */
    private static final int MAX_INDIVIDUAL_INSERTS = 100;

    private final DatabaseManager mDatabaseManager;
    private final PlaylistSettings mPlaylistSettings;
    private PlaylistRecord mActive;
    private CallDatabase mStoredDatabase;
    private int mStoredRestoreCount;
    private Map<Object,StoredItem> mStoredItems = new IdentityHashMap<>();

    /**
     * Constructs an instance
     * @param databaseManager for the database
     * @param playlistSettings for the active playlist name and the playlist files of older versions
     */
    public PlaylistStore(DatabaseManager databaseManager, PlaylistSettings playlistSettings)
    {
        mDatabaseManager = databaseManager;
        mPlaylistSettings = playlistSettings;
    }

    /**
     * Name of the loaded playlist, or null when no playlist is loaded
     */
    public synchronized String getActiveName()
    {
        return mActive != null ? mActive.name() : null;
    }

    /**
     * Loads the active playlist (from the preferences).  Imports the playlist files of older versions when the
     * database has no playlists, and creates an empty default playlist when there are no files either.  When the
     * active playlist doesn't exist, loads the default playlist, or else the first playlist.
     * @return the loaded playlist
     * @throws PlaylistStoreException when the database isn't available or the playlist can't be read
     */
    public synchronized PlaylistV2 loadActive() throws PlaylistStoreException
    {
        String imported = importPlaylistFiles();
        String name = imported != null ? imported : mPlaylistSettings.getActivePlaylist();
        List<PlaylistRecord> playlists = list();
        PlaylistRecord selected = find(playlists, name);

        if(selected == null)
        {
            selected = find(playlists, DEFAULT_PLAYLIST_NAME);
        }

        if(selected == null && !playlists.isEmpty())
        {
            selected = playlists.getFirst();
        }

        if(selected == null)
        {
            selected = create(DEFAULT_PLAYLIST_NAME);
        }

        if(name != null && !name.equals(selected.name()))
        {
            LOGGER.warn("Playlist [" + name + "] doesn't exist in the database - loading playlist [" +
                selected.name() + "]");
        }

        return load(selected.name());
    }

    /**
     * Loads the named playlist and makes it the active playlist.  Applies the playlist updates for a playlist of an
     * older version and stores the updated playlist.
     * @throws PlaylistStoreException when the database isn't available or the playlist doesn't exist
     */
    public synchronized PlaylistV2 load(String name) throws PlaylistStoreException
    {
        record Loaded(PlaylistRecord playlist, List<PlaylistItem> items, CallDatabase database, int restores) {}

        Loaded loaded = call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                Optional<PlaylistRecord> playlist = repository.find(connection, name);

                if(playlist.isEmpty())
                {
                    throw new PlaylistStoreException("Playlist [" + name + "] doesn't exist");
                }

                return new Loaded(playlist.get(), repository.readItems(connection, playlist.get().id()), database,
                    mDatabaseManager.getPlaylistRestoreCount());
            }
        });

        PlaylistV2 playlist = new PlaylistV2();
        playlist.setVersion(loaded.playlist().version());
        Map<Object,StoredItem> stored = new IdentityHashMap<>();
        int unreadable = 0;

        for(PlaylistItem item: loaded.items())
        {
            try
            {
                Object object = PlaylistCodec.deserialize(item);
                PlaylistCodec.add(playlist, object);
                stored.put(object, new StoredItem(item.id(), hash(item.config())));
            }
            catch(Exception e)
            {
                //The row isn't tracked, so saves never change or delete it
                unreadable++;
                LOGGER.error("Unable to read " + item.type() + " [" + item.name() + "] (item " + item.id() +
                    ") of playlist [" + name + "] - it is left unchanged in the database: " + e.getMessage());
            }
        }

        mActive = loaded.playlist();
        mStoredDatabase = loaded.database();
        mStoredRestoreCount = loaded.restores();
        mStoredItems = stored;

        if(!name.equals(mPlaylistSettings.getActivePlaylist()))
        {
            mPlaylistSettings.setActivePlaylist(name);
        }

        LOGGER.info("Loaded playlist [" + name + "] from the database - " + playlist.getChannels().size() +
            " channels, " + playlist.getAliases().size() + " aliases" +
            (unreadable > 0 ? ", " + unreadable + " unreadable items" : ""));

        if(PlaylistUpdater.update(playlist))
        {
            LOGGER.info("Playlist [" + name + "] was updated to version " + PlaylistManager.PLAYLIST_CURRENT_VERSION);
            playlist.setVersion(PlaylistManager.PLAYLIST_CURRENT_VERSION);
            writeAll(PlaylistCodec.getModelObjects(playlist), PlaylistManager.PLAYLIST_CURRENT_VERSION);
        }

        return playlist;
    }

    /**
     * Forgets the loaded playlist, so that saves are refused until a playlist is loaded.
     */
    public synchronized void unload()
    {
        mActive = null;
        mStoredDatabase = null;
        mStoredItems = new IdentityHashMap<>();
    }

    /**
     * Saves the model objects as the content of the active playlist, writing only the items that changed since the
     * last load or save.  When the database was switched since then, all items are written to the new database.
     * @param objects of the playlist (aliases, alias lists, broadcast configurations, channel maps and channels)
     * @param version of the playlist content
     * @return true if anything was written
     * @throws PlaylistStoreException on error, in which case nothing was written
     * @throws PlaylistReplacedException when the playlists were replaced by a backup restore since the playlist was
     * loaded: the playlist has to be reloaded and nothing was written
     */
    public synchronized boolean save(List<Object> objects, int version) throws PlaylistStoreException
    {
        if(mActive == null)
        {
            throw new PlaylistStoreException("No playlist is loaded");
        }

        Map<Object,StoredItem> next = new IdentityHashMap<>();
        Map<Object,PlaylistItem> inserts = new LinkedHashMap<>();
        List<PlaylistItem> updates = new ArrayList<>();

        for(Object object: objects)
        {
            PlaylistItem item = toItem(object);
            byte[] hash = hash(item.config());
            StoredItem stored = mStoredItems.get(object);

            if(stored == null)
            {
                inserts.put(object, item);
            }
            else if(!Arrays.equals(stored.hash(), hash))
            {
                updates.add(item.withId(stored.id()));
                next.put(object, new StoredItem(stored.id(), hash));
            }
            else
            {
                next.put(object, stored);
            }
        }

        List<Long> deletes = new ArrayList<>();

        for(Map.Entry<Object,StoredItem> entry: mStoredItems.entrySet())
        {
            if(!next.containsKey(entry.getKey()))
            {
                deletes.add(entry.getValue().id());
            }
        }

        boolean versionChanged = version != mActive.version();

        if(inserts.isEmpty() && updates.isEmpty() && deletes.isEmpty() && !versionChanged &&
            mDatabaseManager.getDatabase() == mStoredDatabase)
        {
            return false;
        }

        long playlistId = mActive.id();
        List<PlaylistItem> insertItems = new ArrayList<>(inserts.values());

        record Result(List<PlaylistItem> inserted, boolean replaced, CallDatabase database) {}

        Result result = call(database -> {
            checkNotRestored();

            if(database != mStoredDatabase || insertItems.size() > MAX_INDIVIDUAL_INSERTS)
            {
                return null;
            }

            PlaylistRepository repository = database.getPlaylistRepository();

            try(Connection connection = database.getConnection())
            {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try
                {
                    if(!repository.touch(connection, playlistId, version, System.currentTimeMillis()) ||
                        repository.updateItems(connection, playlistId, updates) != updates.size())
                    {
                        //The stored playlist changed outside of this store - write all items instead
                        connection.rollback();
                        return null;
                    }

                    repository.deleteItems(connection, playlistId, deletes);
                    List<PlaylistItem> inserted = new ArrayList<>();

                    for(PlaylistItem item: insertItems)
                    {
                        inserted.add(repository.insertItem(connection, item.withPlaylistId(playlistId)));
                    }

                    connection.commit();
                    return new Result(inserted, false, database);
                }
                catch(SQLException | RuntimeException e)
                {
                    connection.rollback();
                    throw e;
                }
                finally
                {
                    connection.setAutoCommit(autoCommit);
                }
            }
        });

        if(result == null)
        {
            writeAll(objects, version);
            return true;
        }

        int x = 0;

        for(Object object: inserts.keySet())
        {
            PlaylistItem inserted = result.inserted().get(x++);
            next.put(object, new StoredItem(inserted.id(), hash(inserted.config())));
        }

        mStoredItems = next;
        mActive = withVersion(mActive, version);
        LOGGER.debug("Saved playlist [" + mActive.name() + "] - " + inserts.size() + " added, " + updates.size() +
            " changed, " + deletes.size() + " removed");
        return true;
    }

    /**
     * Replaces all items of the active playlist in the current database with the model objects.  Creates the playlist
     * in the current database if it doesn't exist (e.g. after a database switch).
     * @throws PlaylistStoreException on error
     */
    public synchronized void writeAll(List<Object> objects, int version) throws PlaylistStoreException
    {
        if(mActive == null)
        {
            throw new PlaylistStoreException("No playlist is loaded");
        }

        List<PlaylistItem> items = new ArrayList<>();

        for(Object object: objects)
        {
            items.add(toItem(object));
        }

        String name = mActive.name();

        record Result(PlaylistRecord playlist, List<PlaylistItem> stored, CallDatabase database) {}

        Result result = call(database -> {
            checkNotRestored();
            PlaylistRepository repository = database.getPlaylistRepository();

            try(Connection connection = database.getConnection())
            {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try
                {
                    long now = System.currentTimeMillis();
                    Optional<PlaylistRecord> existing = repository.find(connection, name);
                    PlaylistRecord playlist = existing.isPresent() ? existing.get() :
                        repository.insert(connection, name, version, now, now);
                    repository.touch(connection, playlist.id(), version, now);
                    List<PlaylistItem> stored = repository.replaceItems(connection, playlist.id(), items);
                    connection.commit();
                    return new Result(withVersion(playlist, version), stored, database);
                }
                catch(SQLException | RuntimeException e)
                {
                    connection.rollback();
                    throw e;
                }
                finally
                {
                    connection.setAutoCommit(autoCommit);
                }
            }
        });

        Map<Object,StoredItem> stored = new IdentityHashMap<>();

        for(int x = 0; x < objects.size(); x++)
        {
            PlaylistItem item = result.stored().get(x);
            stored.put(objects.get(x), new StoredItem(item.id(), hash(item.config())));
        }

        mActive = result.playlist();
        mStoredDatabase = result.database();
        mStoredItems = stored;
        LOGGER.info("Wrote playlist [" + name + "] to " + result.database().getConfiguration().getDescription() +
            " - " + objects.size() + " items");
    }

    /**
     * Throws when a backup restore replaced the playlists since the playlist was loaded.  Database thread.
     */
    private void checkNotRestored() throws PlaylistReplacedException
    {
        if(mDatabaseManager.getPlaylistRestoreCount() != mStoredRestoreCount)
        {
            throw new PlaylistReplacedException();
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Playlist management
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Playlists in the database, ordered by name
     */
    public List<PlaylistRecord> list() throws PlaylistStoreException
    {
        return call(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getPlaylistRepository().list(connection);
            }
        });
    }

    /**
     * Creates an empty playlist
     * @throws PlaylistStoreException on error, including a duplicate name
     */
    public PlaylistRecord create(String name) throws PlaylistStoreException
    {
        String checked = checkName(name);
        return call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();

                if(repository.find(connection, checked).isPresent())
                {
                    throw new PlaylistStoreException("A playlist named [" + checked + "] already exists");
                }

                long now = System.currentTimeMillis();
                return repository.insert(connection, checked, PlaylistManager.PLAYLIST_CURRENT_VERSION, now, now);
            }
        });
    }

    /**
     * Creates a copy of the playlist.  Save the active playlist first to include its latest changes.
     * @throws PlaylistStoreException on error, including a duplicate name
     */
    public PlaylistRecord copy(String source, String name) throws PlaylistStoreException
    {
        String checked = checkName(name);
        return call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                PlaylistRecord original = repository.find(connection, source).orElseThrow(() ->
                    new PlaylistStoreException("Playlist [" + source + "] doesn't exist"));

                if(repository.find(connection, checked).isPresent())
                {
                    throw new PlaylistStoreException("A playlist named [" + checked + "] already exists");
                }

                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try
                {
                    long now = System.currentTimeMillis();
                    PlaylistRecord copy = repository.insert(connection, checked, original.version(), now, now);
                    List<PlaylistItem> items = new ArrayList<>();

                    for(PlaylistItem item: repository.readItems(connection, original.id()))
                    {
                        items.add(item.withPlaylistId(copy.id()));
                    }

                    repository.insertBatch(connection, items);
                    connection.commit();
                    return copy;
                }
                catch(SQLException | RuntimeException e)
                {
                    connection.rollback();
                    throw e;
                }
                finally
                {
                    connection.setAutoCommit(autoCommit);
                }
            }
        });
    }

    /**
     * Renames the playlist.  Renaming the active playlist also changes the active playlist preference.
     * @throws PlaylistStoreException on error, including a duplicate name
     */
    public synchronized void rename(String name, String newName) throws PlaylistStoreException
    {
        String checked = checkName(newName);

        if(checked.equals(name))
        {
            return;
        }

        PlaylistRecord renamed = call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                PlaylistRecord playlist = repository.find(connection, name).orElseThrow(() ->
                    new PlaylistStoreException("Playlist [" + name + "] doesn't exist"));

                if(repository.find(connection, checked).isPresent())
                {
                    throw new PlaylistStoreException("A playlist named [" + checked + "] already exists");
                }

                repository.rename(connection, playlist.id(), checked, System.currentTimeMillis());
                return new PlaylistRecord(playlist.id(), checked, playlist.version(), playlist.created(),
                    playlist.modified(), 0, 0);
            }
        });

        if(mActive != null && mActive.name().equals(name))
        {
            mActive = new PlaylistRecord(mActive.id(), renamed.name(), mActive.version(), mActive.created(),
                mActive.modified(), 0, 0);
            mPlaylistSettings.setActivePlaylist(renamed.name());
        }
    }

    /**
     * Deletes the playlist.  The active playlist can't be deleted.
     * @throws PlaylistStoreException on error
     */
    public synchronized void delete(String name) throws PlaylistStoreException
    {
        if(mActive != null && mActive.name().equals(name))
        {
            throw new PlaylistStoreException("The playlist in use can't be deleted");
        }

        call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                Optional<PlaylistRecord> playlist = repository.find(connection, name);

                if(playlist.isPresent())
                {
                    boolean autoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(false);

                    try
                    {
                        repository.delete(connection, playlist.get().id());
                        connection.commit();
                    }
                    catch(SQLException | RuntimeException e)
                    {
                        connection.rollback();
                        throw e;
                    }
                    finally
                    {
                        connection.setAutoCommit(autoCommit);
                    }
                }

                return null;
            }
        });
    }

    /**
     * Imports a playlist file as a new playlist.  The file isn't changed.
     * @param file to import
     * @param name for the new playlist
     * @throws PlaylistStoreException if the file isn't a readable playlist, or on database error
     */
    public PlaylistRecord importFile(Path file, String name) throws PlaylistStoreException
    {
        String checked = checkName(name);
        PlaylistV2 playlist = readPlaylistFile(file);
        List<PlaylistItem> items = toItems(playlist);

        PlaylistRecord imported = call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();

                if(repository.find(connection, checked).isPresent())
                {
                    throw new PlaylistStoreException("A playlist named [" + checked + "] already exists");
                }

                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try
                {
                    PlaylistRecord record = insertPlaylist(repository, connection, checked, playlist.getVersion(),
                        items);
                    connection.commit();
                    return record;
                }
                catch(SQLException | RuntimeException e)
                {
                    connection.rollback();
                    throw e;
                }
                finally
                {
                    connection.setAutoCommit(autoCommit);
                }
            }
        });

        LOGGER.info("Imported playlist file [" + file + "] as playlist [" + checked + "] - " + items.size() +
            " items");
        return imported;
    }

    /**
     * Exports the stored playlist to a playlist file that this and older versions of sdrtrunk can load.  Save the
     * active playlist first to include its latest changes.
     * @throws PlaylistStoreException on error
     */
    public void exportFile(String name, Path file) throws PlaylistStoreException
    {
        record Stored(PlaylistRecord playlist, List<PlaylistItem> items) {}

        Stored stored = call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                PlaylistRecord playlist = repository.find(connection, name).orElseThrow(() ->
                    new PlaylistStoreException("Playlist [" + name + "] doesn't exist"));
                return new Stored(playlist, repository.readItems(connection, playlist.id()));
            }
        });

        try
        {
            PlaylistCodec.writeFile(PlaylistCodec.fromItems(stored.items(), stored.playlist().version()), file);
            LOGGER.info("Exported playlist [" + name + "] to file [" + file + "]");
        }
        catch(IOException ioe)
        {
            throw new PlaylistStoreException("Unable to write playlist file [" + file + "] - " + ioe.getMessage(),
                ioe);
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Import of the playlist files of older versions
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Imports the playlist files of older versions when the database has no playlists.  The files are not changed.
     * @return name of the imported playlist that should be active (from the current playlist file), or null when
     * nothing was imported
     */
    private String importPlaylistFiles() throws PlaylistStoreException
    {
        boolean empty = call(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getPlaylistRepository().count(connection) == 0;
            }
        });

        if(!empty)
        {
            return null;
        }

        Path current = mPlaylistSettings.getPlaylist();

        //Older versions load the legacy playlist when the current playlist file doesn't exist
        if(!Files.exists(current) && Files.exists(mPlaylistSettings.getLegacyPlaylist()))
        {
            current = mPlaylistSettings.getLegacyPlaylist();
        }

        List<Path> files = new ArrayList<>();
        files.add(current);

        for(Path path: mPlaylistSettings.getPlaylistList())
        {
            if(!files.contains(path) && !path.equals(mPlaylistSettings.getPlaylist()))
            {
                files.add(path);
            }
        }

        Map<String,PlaylistV2> playlists = new LinkedHashMap<>();
        Map<String,Path> sources = new LinkedHashMap<>();
        String currentName = null;

        for(Path file: files)
        {
            Path source = getReadableFile(file);

            if(source == null)
            {
                continue;
            }

            try
            {
                PlaylistV2 playlist = PlaylistCodec.readFile(source);
                String name = getUniqueName(getNameFromFile(file), playlists);
                playlists.put(name, playlist);
                sources.put(name, source);

                if(file == current)
                {
                    currentName = name;
                }
            }
            catch(Exception e)
            {
                LOGGER.error("Unable to import playlist file [" + source + "] - " + e.getMessage());
            }
        }

        if(playlists.isEmpty())
        {
            return null;
        }

        Map<String,List<PlaylistItem>> items = new LinkedHashMap<>();

        for(Map.Entry<String,PlaylistV2> entry: playlists.entrySet())
        {
            items.put(entry.getKey(), toItems(entry.getValue()));
        }

        boolean imported = call(database -> {
            try(Connection connection = database.getConnection())
            {
                PlaylistRepository repository = database.getPlaylistRepository();
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try
                {
                    //Another sdrtrunk instance could have imported its playlists in the meantime
                    if(repository.count(connection) > 0)
                    {
                        connection.rollback();
                        return false;
                    }

                    for(Map.Entry<String,List<PlaylistItem>> entry: items.entrySet())
                    {
                        insertPlaylist(repository, connection, entry.getKey(),
                            playlists.get(entry.getKey()).getVersion(), entry.getValue());
                    }

                    connection.commit();
                    return true;
                }
                catch(SQLException | RuntimeException e)
                {
                    connection.rollback();
                    throw e;
                }
                finally
                {
                    connection.setAutoCommit(autoCommit);
                }
            }
        });

        if(!imported)
        {
            return null;
        }

        for(Map.Entry<String,Path> entry: sources.entrySet())
        {
            LOGGER.info("Imported playlist file [" + entry.getValue() + "] into the database as playlist [" +
                entry.getKey() + "] - " + items.get(entry.getKey()).size() + " items.  The file is unchanged and " +
                "is no longer updated.");
        }

        return currentName != null ? currentName : playlists.keySet().iterator().next();
    }

    /**
     * File to read for the playlist file: the playlist backup when the lock file shows that an older version didn't
     * finish saving the playlist (the same recovery that the older version does), or null when there is no file.
     */
    static Path getReadableFile(Path file)
    {
        Path lock = Path.of(file.toAbsolutePath() + ".lck");
        Path backup = Path.of(file + ".backup");

        if(Files.exists(lock) && Files.exists(backup))
        {
            LOGGER.info("The last save of playlist file [" + file + "] was incomplete - importing its backup [" +
                backup + "]");
            return backup;
        }

        return Files.exists(file) ? file : null;
    }

    /**
     * Playlist name for a playlist file: the file name without the .xml extension
     */
    static String getNameFromFile(Path file)
    {
        String name = file.getFileName().toString();

        if(name.toLowerCase().endsWith(".xml"))
        {
            name = name.substring(0, name.length() - 4);
        }

        name = name.trim();

        if(name.isEmpty())
        {
            name = DEFAULT_PLAYLIST_NAME;
        }

        return name.length() > PlaylistRepository.LENGTH_NAME ? name.substring(0, PlaylistRepository.LENGTH_NAME) :
            name;
    }

    private static String getUniqueName(String name, Map<String,?> existing)
    {
        String unique = name;

        for(int x = 2; existing.containsKey(unique); x++)
        {
            unique = name + " (" + x + ")";
        }

        return unique;
    }

    //----------------------------------------------------------------------------------------------------------------
    // Support
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Reads a playlist file to import
     * @throws PlaylistStoreException if the file isn't a readable playlist
     */
    public static PlaylistV2 readPlaylistFile(Path file) throws PlaylistStoreException
    {
        try
        {
            return PlaylistCodec.readFile(file);
        }
        catch(Exception e)
        {
            throw new PlaylistStoreException("The file [" + file + "] is not a readable playlist - " +
                e.getMessage(), e);
        }
    }

    private static PlaylistRecord insertPlaylist(PlaylistRepository repository, Connection connection, String name,
                                                 int version, List<PlaylistItem> items) throws SQLException
    {
        long now = System.currentTimeMillis();
        PlaylistRecord playlist = repository.insert(connection, name, version, now, now);
        List<PlaylistItem> stored = new ArrayList<>();

        for(PlaylistItem item: items)
        {
            stored.add(item.withPlaylistId(playlist.id()));
        }

        repository.insertBatch(connection, stored);
        return playlist;
    }

    /**
     * Checks a playlist name
     * @return the trimmed name
     * @throws PlaylistStoreException if the name is empty or too long
     */
    public static String checkName(String name) throws PlaylistStoreException
    {
        String trimmed = name != null ? name.trim() : "";

        if(trimmed.isEmpty())
        {
            throw new PlaylistStoreException("Enter a playlist name");
        }

        if(trimmed.length() > PlaylistRepository.LENGTH_NAME)
        {
            throw new PlaylistStoreException("The playlist name is longer than " + PlaylistRepository.LENGTH_NAME +
                " characters");
        }

        return trimmed;
    }

    private static PlaylistRecord find(List<PlaylistRecord> playlists, String name)
    {
        if(name != null)
        {
            for(PlaylistRecord playlist: playlists)
            {
                if(playlist.name().equals(name))
                {
                    return playlist;
                }
            }
        }

        return null;
    }

    private static PlaylistRecord withVersion(PlaylistRecord playlist, int version)
    {
        return new PlaylistRecord(playlist.id(), playlist.name(), version, playlist.created(), playlist.modified(),
            0, 0);
    }

    private static PlaylistItem toItem(Object object) throws PlaylistStoreException
    {
        try
        {
            return PlaylistCodec.toItem(object);
        }
        catch(Exception e)
        {
            throw new PlaylistStoreException("Unable to serialize " + PlaylistCodec.getType(object) + " [" +
                PlaylistCodec.getName(object) + "] - " + e.getMessage(), e);
        }
    }

    private static List<PlaylistItem> toItems(PlaylistV2 playlist) throws PlaylistStoreException
    {
        List<PlaylistItem> items = new ArrayList<>();

        for(Object object: PlaylistCodec.getModelObjects(playlist))
        {
            items.add(toItem(object));
        }

        return items;
    }

    private static byte[] hash(String config)
    {
        try
        {
            return MessageDigest.getInstance("SHA-256").digest(config.getBytes(StandardCharsets.UTF_8));
        }
        catch(NoSuchAlgorithmException nsae)
        {
            throw new IllegalStateException("SHA-256 isn't available", nsae);
        }
    }

    /**
     * Runs the task on the database thread and waits for the result.
     */
    private <T> T call(DatabaseManager.DatabaseTask<T> task) throws PlaylistStoreException
    {
        try
        {
            return mDatabaseManager.submit(task).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch(ExecutionException ee)
        {
            Throwable cause = ee.getCause();

            if(cause instanceof PlaylistStoreException pse)
            {
                throw pse;
            }

            throw new PlaylistStoreException(cause != null && cause.getMessage() != null ? cause.getMessage() :
                String.valueOf(cause), cause);
        }
        catch(InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new PlaylistStoreException("Interrupted while waiting for the database", ie);
        }
        catch(Exception e)
        {
            throw new PlaylistStoreException("Timeout or error while waiting for the database - " + e.getMessage(),
                e);
        }
    }

    /**
     * Database row and content hash of a stored model object
     */
    private record StoredItem(long id, byte[] hash) {}

    /**
     * Error loading, saving or changing a stored playlist
     */
    public static class PlaylistStoreException extends Exception
    {
        public PlaylistStoreException(String message)
        {
            super(message);
        }

        public PlaylistStoreException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * The playlists were replaced by a backup restore since the playlist was loaded
     */
    public static class PlaylistReplacedException extends PlaylistStoreException
    {
        public PlaylistReplacedException()
        {
            super("The playlists were replaced by a backup restore");
        }
    }
}
