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

package io.github.dsheirer.database.playlist;

import io.github.dsheirer.database.SqlSupport;
import io.github.dsheirer.database.dialect.IDatabaseDialect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Playlists stored in the sdrtrunk schema.  A playlist row holds the name and version, and each channel, alias, alias
 * list configuration, broadcast configuration and channel map is an item row that holds the serialized item.  Items
 * are read in row id order, which is the order they were added.
 */
public class PlaylistRepository
{
    public static final String PLAYLIST_TABLE = "playlist";
    public static final String ITEM_TABLE = "playlist_item";
    public static final int LENGTH_NAME = 255;
    public static final int LENGTH_TYPE = 16;
    private static final int BATCH_SIZE = 500;

    private final String mPlaylistTable;
    private final String mItemTable;

    /**
     * Constructs an instance
     * @param schema that holds the tables
     */
    public PlaylistRepository(String schema)
    {
        mPlaylistTable = schema + "." + PLAYLIST_TABLE;
        mItemTable = schema + "." + ITEM_TABLE;
    }

    /**
     * Statements that create the playlist tables.
     */
    public static List<String> getCreateStatements(IDatabaseDialect dialect, String schema)
    {
        String playlists = schema + "." + PLAYLIST_TABLE;
        String items = schema + "." + ITEM_TABLE;
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS " + playlists + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "name VARCHAR(" + LENGTH_NAME + ") NOT NULL UNIQUE, " +
            "version INT NOT NULL, " +
            "created_time BIGINT NOT NULL, " +
            "modified_time BIGINT NOT NULL)");
        statements.add("CREATE TABLE IF NOT EXISTS " + items + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "playlist_id BIGINT NOT NULL REFERENCES " + playlists + "(id) ON DELETE CASCADE, " +
            "item_type VARCHAR(" + LENGTH_TYPE + ") NOT NULL, " +
            "name VARCHAR(" + LENGTH_NAME + "), " +
            "config " + dialect.getLargeTextColumn() + " NOT NULL)");
        statements.add("CREATE INDEX IF NOT EXISTS playlist_item_playlist_idx ON " + items + " (playlist_id)");
        return statements;
    }

    //----------------------------------------------------------------------------------------------------------------
    // Playlists
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Count of playlists
     */
    public long count(Connection connection) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + mPlaylistTable);
            ResultSet rs = statement.executeQuery())
        {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /**
     * Playlists ordered by name (not case sensitive), with their channel and alias counts.
     */
    public List<PlaylistRecord> list(Connection connection) throws SQLException
    {
        Map<Long,long[]> counts = new HashMap<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT playlist_id, item_type, COUNT(*) FROM " +
            mItemTable + " WHERE item_type IN (?, ?) GROUP BY playlist_id, item_type"))
        {
            statement.setString(1, PlaylistItemType.CHANNEL.name());
            statement.setString(2, PlaylistItemType.ALIAS.name());

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    long[] count = counts.computeIfAbsent(rs.getLong(1), id -> new long[2]);
                    count[PlaylistItemType.CHANNEL.name().equals(rs.getString(2)) ? 0 : 1] = rs.getLong(3);
                }
            }
        }

        List<PlaylistRecord> playlists = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT id, name, version, created_time, " +
            "modified_time FROM " + mPlaylistTable);
            ResultSet rs = statement.executeQuery())
        {
            while(rs.next())
            {
                long id = rs.getLong(1);
                long[] count = counts.getOrDefault(id, new long[2]);
                playlists.add(new PlaylistRecord(id, rs.getString(2), rs.getInt(3), rs.getLong(4), rs.getLong(5),
                    count[0], count[1]));
            }
        }

        //Sorted here, since the engines order names differently
        playlists.sort(Comparator.comparing(PlaylistRecord::name, String.CASE_INSENSITIVE_ORDER));
        return playlists;
    }

    /**
     * Finds the playlist by name (exact match).
     */
    public Optional<PlaylistRecord> find(Connection connection, String name) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT id, name, version, created_time, " +
            "modified_time FROM " + mPlaylistTable + " WHERE name = ?"))
        {
            statement.setString(1, name);

            try(ResultSet rs = statement.executeQuery())
            {
                if(rs.next())
                {
                    return Optional.of(new PlaylistRecord(rs.getLong(1), rs.getString(2), rs.getInt(3),
                        rs.getLong(4), rs.getLong(5), 0, 0));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Stores a new, empty playlist.
     * @return the playlist with its row id
     * @throws SQLException on error, including a duplicate name
     */
    public PlaylistRecord insert(Connection connection, String name, int version, long created, long modified)
        throws SQLException
    {
        if(name == null || name.isBlank() || name.length() > LENGTH_NAME)
        {
            throw new SQLException("Playlist name must be 1-" + LENGTH_NAME + " characters");
        }

        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mPlaylistTable +
            " (name, version, created_time, modified_time) VALUES (?, ?, ?, ?)", new String[]{"id"}))
        {
            statement.setString(1, name);
            statement.setInt(2, version);
            statement.setLong(3, created);
            statement.setLong(4, modified);
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    return new PlaylistRecord(keys.getLong(1), name, version, created, modified, 0, 0);
                }
            }
        }

        throw new SQLException("Database didn't return the id of the new playlist");
    }

    /**
     * Renames the playlist
     * @return true if the playlist exists
     */
    public boolean rename(Connection connection, long id, String name, long modified) throws SQLException
    {
        if(name == null || name.isBlank() || name.length() > LENGTH_NAME)
        {
            throw new SQLException("Playlist name must be 1-" + LENGTH_NAME + " characters");
        }

        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mPlaylistTable +
            " SET name = ?, modified_time = ? WHERE id = ?"))
        {
            statement.setString(1, name);
            statement.setLong(2, modified);
            statement.setLong(3, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Updates the version and modified time of the playlist
     * @return true if the playlist exists
     */
    public boolean touch(Connection connection, long id, int version, long modified) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mPlaylistTable +
            " SET version = ?, modified_time = ? WHERE id = ?"))
        {
            statement.setInt(1, version);
            statement.setLong(2, modified);
            statement.setLong(3, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Deletes the playlist and its items
     */
    public void delete(Connection connection, long id) throws SQLException
    {
        deleteItems(connection, id);

        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mPlaylistTable +
            " WHERE id = ?"))
        {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    /**
     * Deletes all playlists and items
     */
    public void deleteAll(Connection connection) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mItemTable))
        {
            statement.executeUpdate();
        }

        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mPlaylistTable))
        {
            statement.executeUpdate();
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Items
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Items of the playlist, in the order they were added.  Rows with an item type that this version doesn't know
     * are skipped.
     */
    public List<PlaylistItem> readItems(Connection connection, long playlistId) throws SQLException
    {
        List<PlaylistItem> items = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT id, playlist_id, item_type, name, " +
            "config FROM " + mItemTable + " WHERE playlist_id = ? ORDER BY id"))
        {
            statement.setLong(1, playlistId);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    PlaylistItem item = readItem(rs);

                    if(item != null)
                    {
                        items.add(item);
                    }
                }
            }
        }

        return items;
    }

    /**
     * Page of the items of all playlists, in row id order.
     * @param afterId exclusive starting row id
     * @param limit maximum number of items
     */
    public List<PlaylistItem> readItemPage(Connection connection, long afterId, int limit) throws SQLException
    {
        List<PlaylistItem> items = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT id, playlist_id, item_type, name, " +
            "config FROM " + mItemTable + " WHERE id > ? ORDER BY id"))
        {
            statement.setLong(1, afterId);
            statement.setMaxRows(limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next() && items.size() < limit)
                {
                    PlaylistItem item = readItem(rs);

                    //Keep unknown types in the page so that the caller's paging continues past them
                    items.add(item != null ? item : new PlaylistItem(rs.getLong(1), rs.getLong(2), null, null,
                        null));
                }
            }
        }

        return items;
    }

    private static PlaylistItem readItem(ResultSet rs) throws SQLException
    {
        PlaylistItemType type = PlaylistItemType.fromName(rs.getString(3));

        if(type == null)
        {
            return null;
        }

        return new PlaylistItem(rs.getLong(1), rs.getLong(2), type, rs.getString(4), rs.getString(5));
    }

    /**
     * Stores one new item.
     * @return the item with its row id
     */
    public PlaylistItem insertItem(Connection connection, PlaylistItem item) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mItemTable +
            " (playlist_id, item_type, name, config) VALUES (?, ?, ?, ?)", new String[]{"id"}))
        {
            bindItem(statement, item);
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    return item.withId(keys.getLong(1));
                }
            }
        }

        throw new SQLException("Database didn't return the id of the new playlist item");
    }

    /**
     * Replaces all items of the playlist with the items, in a batch.  Invoke inside a transaction, since the row ids
     * are matched to the items by reading the new rows back in id order.
     * @param items to store, in order.  The playlist id of each item is ignored.
     * @return the items with their row ids, in the same order
     */
    public List<PlaylistItem> replaceItems(Connection connection, long playlistId, List<PlaylistItem> items)
        throws SQLException
    {
        deleteItems(connection, playlistId);

        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mItemTable +
            " (playlist_id, item_type, name, config) VALUES (?, ?, ?, ?)"))
        {
            int pending = 0;

            for(PlaylistItem item: items)
            {
                bindItem(statement, item.withPlaylistId(playlistId));
                statement.addBatch();

                if(++pending >= BATCH_SIZE)
                {
                    statement.executeBatch();
                    pending = 0;
                }
            }

            if(pending > 0)
            {
                statement.executeBatch();
            }
        }

        List<Long> ids = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT id FROM " + mItemTable +
            " WHERE playlist_id = ? ORDER BY id"))
        {
            statement.setLong(1, playlistId);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    ids.add(rs.getLong(1));
                }
            }
        }

        if(ids.size() != items.size())
        {
            throw new SQLException("Stored " + ids.size() + " playlist items but expected " + items.size());
        }

        List<PlaylistItem> stored = new ArrayList<>();

        for(int x = 0; x < items.size(); x++)
        {
            stored.add(items.get(x).withPlaylistId(playlistId).withId(ids.get(x)));
        }

        return stored;
    }

    /**
     * Stores items of any playlists without returning their row ids (restore and transfer).
     */
    public void insertBatch(Connection connection, List<PlaylistItem> items) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mItemTable +
            " (playlist_id, item_type, name, config) VALUES (?, ?, ?, ?)"))
        {
            int pending = 0;

            for(PlaylistItem item: items)
            {
                bindItem(statement, item);
                statement.addBatch();

                if(++pending >= BATCH_SIZE)
                {
                    statement.executeBatch();
                    pending = 0;
                }
            }

            if(pending > 0)
            {
                statement.executeBatch();
            }
        }
    }

    /**
     * Updates the name and serialized content of stored items of the playlist
     * @return count of items that were updated (less than the argument count if some no longer exist)
     */
    public int updateItems(Connection connection, long playlistId, Collection<PlaylistItem> items)
        throws SQLException
    {
        int updated = 0;

        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mItemTable +
            " SET name = ?, config = ? WHERE id = ? AND playlist_id = ?"))
        {
            for(PlaylistItem item: items)
            {
                SqlSupport.setString(statement, 1, item.name(), LENGTH_NAME);
                statement.setString(2, item.config());
                statement.setLong(3, item.id());
                statement.setLong(4, playlistId);
                updated += statement.executeUpdate();
            }
        }

        return updated;
    }

    /**
     * Deletes items of the playlist by row id
     */
    public void deleteItems(Connection connection, long playlistId, Collection<Long> ids) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mItemTable +
            " WHERE id = ? AND playlist_id = ?"))
        {
            int pending = 0;

            for(Long id: ids)
            {
                statement.setLong(1, id);
                statement.setLong(2, playlistId);
                statement.addBatch();

                if(++pending >= BATCH_SIZE)
                {
                    statement.executeBatch();
                    pending = 0;
                }
            }

            if(pending > 0)
            {
                statement.executeBatch();
            }
        }
    }

    /**
     * Deletes all items of the playlist
     */
    public void deleteItems(Connection connection, long playlistId) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mItemTable +
            " WHERE playlist_id = ?"))
        {
            statement.setLong(1, playlistId);
            statement.executeUpdate();
        }
    }

    private static void bindItem(PreparedStatement statement, PlaylistItem item) throws SQLException
    {
        statement.setLong(1, item.playlistId());
        statement.setString(2, item.type().name());
        SqlSupport.setString(statement, 3, item.name(), LENGTH_NAME);
        statement.setString(4, item.config());
    }

    //----------------------------------------------------------------------------------------------------------------
    // Transfer
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Copies each playlist of the source whose name doesn't exist in this repository's schema.  The target connection
     * should be in a transaction.
     * @return count of playlists copied
     */
    public int copyMissingFrom(PlaylistRepository source, Connection sourceConnection, Connection targetConnection)
        throws SQLException
    {
        int copied = 0;

        for(PlaylistRecord playlist: source.list(sourceConnection))
        {
            if(find(targetConnection, playlist.name()).isEmpty())
            {
                PlaylistRecord inserted = insert(targetConnection, playlist.name(), playlist.version(),
                    playlist.created(), playlist.modified());
                List<PlaylistItem> items = new ArrayList<>();

                for(PlaylistItem item: source.readItems(sourceConnection, playlist.id()))
                {
                    items.add(item.withPlaylistId(inserted.id()));
                }

                insertBatch(targetConnection, items);
                copied++;
            }
        }

        return copied;
    }
}
