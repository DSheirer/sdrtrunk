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

package io.github.dsheirer.database;

import io.github.dsheirer.database.playlist.PlaylistItem;
import io.github.dsheirer.database.playlist.PlaylistItemType;
import io.github.dsheirer.database.playlist.PlaylistRecord;
import io.github.dsheirer.database.playlist.PlaylistRepository;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Playlist tables on each database engine: install, large items, batch replace with id matching, scoped updates and
 * deletes, cascade delete and copy between schemas.
 *
 * The network checks run when the SDRTRUNK_TEST_MARIADB and SDRTRUNK_TEST_POSTGRES environment variables are set (see
 * CallDatabaseTest).  They use (and drop) the schemas 'sdrtrunk_playlist_test' and 'sdrtrunk_playlist_test2'.
 */
public class PlaylistRepositoryTest
{
    private static final String TEST_SCHEMA = "sdrtrunk_playlist_test";
    private static final String TEST_SCHEMA_2 = "sdrtrunk_playlist_test2";

    @TempDir
    Path mTempDir;

    @Test
    public void h2() throws Exception
    {
        exercise(DatabaseType.H2, "", 0, "", "", "");
    }

    @Test
    public void mariadb() throws Exception
    {
        String[] parts = fromEnvironment("SDRTRUNK_TEST_MARIADB");
        Assumptions.assumeTrue(parts != null, "SDRTRUNK_TEST_MARIADB not set");
        exercise(DatabaseType.MARIADB, parts[0], Integer.parseInt(parts[1]), parts.length > 4 ? parts[4] : "postgres",
            parts[2], parts[3]);
    }

    @Test
    public void postgres() throws Exception
    {
        String[] parts = fromEnvironment("SDRTRUNK_TEST_POSTGRES");
        Assumptions.assumeTrue(parts != null, "SDRTRUNK_TEST_POSTGRES not set");
        exercise(DatabaseType.POSTGRESQL, parts[0], Integer.parseInt(parts[1]), parts.length > 4 ? parts[4] :
            "postgres", parts[2], parts[3]);
    }

    private static String[] fromEnvironment(String variable)
    {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? null : value.split(":");
    }

    private CallDatabase open(DatabaseConfiguration configuration) throws Exception
    {
        CallDatabase database = new CallDatabase(configuration, mTempDir.resolve("db"));

        try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute(configuration.type().getDialect().getDropSchemaStatement(configuration.schema()));
        }

        database.install();
        return database;
    }

    private static void drop(CallDatabase database) throws SQLException
    {
        try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute(database.getConfiguration().type().getDialect().getDropSchemaStatement(
                database.getConfiguration().schema()));
        }
    }

    private static PlaylistItem item(PlaylistItemType type, String name, String config)
    {
        return new PlaylistItem(0, 0, type, name, config);
    }

    private void exercise(DatabaseType type, String host, int port, String database, String user, String password)
        throws Exception
    {
        try(CallDatabase first = open(new DatabaseConfiguration(type, host, port, database, TEST_SCHEMA, user,
            password)); CallDatabase second = open(new DatabaseConfiguration(type, host, port, database,
            TEST_SCHEMA_2, user, password)))
        {
            try
            {
                PlaylistRepository repository = first.getPlaylistRepository();

                //Large item (several MB), unicode and XML characters
                StringBuilder large = new StringBuilder("<alias name=\"Big ✓ &amp; &lt;x&gt;\">");

                while(large.length() < 3_000_000)
                {
                    large.append("<id type=\"talkgroup\" value=\"").append(large.length()).append("\"/>");
                }

                large.append("</alias>");

                try(Connection connection = first.getConnection())
                {
                    connection.setAutoCommit(false);
                    long now = System.currentTimeMillis();
                    PlaylistRecord playlist = repository.insert(connection, "Metro ✓", 4, now, now);
                    assertThrows(SQLException.class, () -> repository.insert(connection, "", 4, now, now));

                    List<PlaylistItem> items = new ArrayList<>();
                    items.add(item(PlaylistItemType.ALIAS, "Big", large.toString()));

                    for(int x = 0; x < 1200; x++)
                    {
                        items.add(item(PlaylistItemType.ALIAS, "Alias " + x, "<alias name=\"Alias " + x + "\"/>"));
                    }

                    items.add(item(PlaylistItemType.CHANNEL, "x".repeat(400), "<channel/>"));
                    List<PlaylistItem> stored = repository.replaceItems(connection, playlist.id(), items);
                    connection.commit();

                    assertEquals(items.size(), stored.size());
                    List<PlaylistItem> read = repository.readItems(connection, playlist.id());
                    assertEquals(items.size(), read.size());

                    for(int x = 0; x < read.size(); x++)
                    {
                        assertEquals(stored.get(x).id(), read.get(x).id());
                        assertEquals(items.get(x).config(), read.get(x).config(), "item " + x);
                    }

                    assertEquals(PlaylistRepository.LENGTH_NAME, read.getLast().name().length(), "truncated name");

                    //Updates and deletes are scoped to the playlist
                    PlaylistRecord other = repository.insert(connection, "Other", 4, now, now);
                    PlaylistItem otherItem = repository.insertItem(connection, item(PlaylistItemType.CHANNEL_MAP,
                        "Map", "<channel_map/>").withPlaylistId(other.id()));
                    assertEquals(0, repository.updateItems(connection, playlist.id(),
                        List.of(otherItem.withPlaylistId(playlist.id()))));
                    repository.deleteItems(connection, playlist.id(), List.of(otherItem.id()));
                    assertEquals(1, repository.readItems(connection, other.id()).size());

                    assertEquals(1, repository.updateItems(connection, playlist.id(),
                        List.of(new PlaylistItem(read.get(1).id(), playlist.id(), PlaylistItemType.ALIAS, "Changed",
                            "<alias name=\"Changed\"/>"))));
                    repository.deleteItems(connection, playlist.id(), List.of(read.get(2).id(), read.get(3).id()));
                    connection.commit();

                    List<PlaylistItem> changed = repository.readItems(connection, playlist.id());
                    assertEquals(items.size() - 2, changed.size());
                    assertEquals("Changed", changed.get(1).name());

                    List<PlaylistRecord> playlists = repository.list(connection);
                    assertEquals(List.of("Metro ✓", "Other"), playlists.stream().map(PlaylistRecord::name).toList());
                    assertEquals(1, playlists.getFirst().channels());
                    assertEquals(1199, playlists.getFirst().aliases());

                    //Copy to the second schema, then the copy skips the existing names
                    try(Connection target = second.getConnection())
                    {
                        target.setAutoCommit(false);
                        assertEquals(2, second.getPlaylistRepository().copyMissingFrom(repository, connection,
                            target));
                        target.commit();
                        assertEquals(0, second.getPlaylistRepository().copyMissingFrom(repository, connection,
                            target));
                        PlaylistRecord copied = second.getPlaylistRepository().find(target, "Metro ✓").orElseThrow();
                        List<PlaylistItem> copiedItems = second.getPlaylistRepository().readItems(target,
                            copied.id());
                        assertEquals(changed.size(), copiedItems.size());
                        assertEquals(large.toString(), copiedItems.getFirst().config());
                    }

                    //Delete removes the items
                    repository.delete(connection, other.id());
                    connection.commit();
                    assertTrue(repository.readItems(connection, other.id()).isEmpty());
                    assertEquals(1, repository.count(connection));
                    assertEquals(1, first.getPlaylistCount());

                    repository.deleteAll(connection);
                    connection.commit();
                    assertEquals(0, repository.count(connection));
                }
            }
            finally
            {
                drop(first);
                drop(second);
            }
        }
    }
}
