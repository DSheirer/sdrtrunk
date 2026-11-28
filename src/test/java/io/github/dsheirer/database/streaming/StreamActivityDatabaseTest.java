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

package io.github.dsheirer.database.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseType;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stream activity table: schema upgrade, insert/update round trip, search filters (and agreement of the in-memory
 * filter with the SQL filter), interrupted rows and the age-off delete.  Runs against the built-in H2 database, and
 * against MariaDB and PostgreSQL when SDRTRUNK_TEST_MARIADB / SDRTRUNK_TEST_POSTGRES are set (host:port:user:password
 * [:database]).  Uses (and drops) the schema 'sdrtrunk_stream_test'.
 */
public class StreamActivityDatabaseTest
{
    private static final String TEST_SCHEMA = "sdrtrunk_stream_test";

    @TempDir
    Path mTempDir;

    @Test
    public void h2() throws Exception
    {
        exercise(new DatabaseConfiguration(DatabaseType.H2, "", 0, "", TEST_SCHEMA, "", ""));
    }

    @Test
    public void mariadb() throws Exception
    {
        DatabaseConfiguration configuration = fromEnvironment("SDRTRUNK_TEST_MARIADB", DatabaseType.MARIADB);
        Assumptions.assumeTrue(configuration != null, "SDRTRUNK_TEST_MARIADB not set");
        exercise(configuration);
    }

    @Test
    public void postgres() throws Exception
    {
        DatabaseConfiguration configuration = fromEnvironment("SDRTRUNK_TEST_POSTGRES", DatabaseType.POSTGRESQL);
        Assumptions.assumeTrue(configuration != null, "SDRTRUNK_TEST_POSTGRES not set");
        exercise(configuration);
    }

    @Test
    public void aliasEncoding()
    {
        List<StreamActivity.SelectingAlias> aliases = List.of(new StreamActivity.SelectingAlias("Fire\tDispatch",
            "To 1001"), new StreamActivity.SelectingAlias("Engine 4", null));
        List<StreamActivity.SelectingAlias> decoded =
            StreamActivityRepository.decode(StreamActivityRepository.encode(aliases));
        assertEquals(List.of(new StreamActivity.SelectingAlias("Fire Dispatch", "To 1001"),
            new StreamActivity.SelectingAlias("Engine 4", null)), decoded);
        assertNull(StreamActivityRepository.encode(List.of()));

        //A long list is cut at an alias
        List<StreamActivity.SelectingAlias> many = new ArrayList<>();

        for(int x = 0; x < 500; x++)
        {
            many.add(new StreamActivity.SelectingAlias("Alias number " + x, "To " + x));
        }

        String encoded = StreamActivityRepository.encode(many);
        assertTrue(encoded.length() <= StreamActivityRepository.LENGTH_ALIASES);
        List<StreamActivity.SelectingAlias> cut = StreamActivityRepository.decode(encoded);
        assertEquals(many.subList(0, cut.size()), cut);
    }

    private static DatabaseConfiguration fromEnvironment(String variable, DatabaseType type)
    {
        String value = System.getenv(variable);

        if(value == null || value.isBlank())
        {
            return null;
        }

        String[] parts = value.split(":");
        String database = parts.length > 4 ? parts[4] : "postgres";
        return new DatabaseConfiguration(type, parts[0], Integer.parseInt(parts[1]), database, TEST_SCHEMA, parts[2],
            parts[3]);
    }

    private static StreamActivity activity(StreamActivityModel model, long callStart, String stream, String to,
                                           String alias, StreamStatus status, String detail)
    {
        StreamActivity.Builder call = new StreamActivity.Builder().callKey(callStart / 1000).callStart(callStart)
            .duration(4200).to(to).from("2004").aliasList("Metro").protocol("APCO-25").system("Metro P25")
            .site("Site 1").channel("Control").selectingAliases(List.of(new StreamActivity.SelectingAlias(alias,
                "To " + to)));
        StreamActivity activity = model.add(call, stream, BroadcastServerType.BROADCASTIFY_CALL,
            StreamStatus.PENDING, null);
        model.update(activity, status, detail);
        return activity;
    }

    private void exercise(DatabaseConfiguration configuration) throws Exception
    {
        try(CallDatabase database = new CallDatabase(configuration, mTempDir))
        {
            try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
            {
                statement.execute(configuration.type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
            }

            database.install();
            StreamActivityRepository repository = database.getStreamActivityRepository();
            StreamActivityModel model = new StreamActivityModel();
            long now = System.currentTimeMillis();
            List<StreamActivity> activities = List.of(
                activity(model, now - 50_000, "County Calls", "1001", "Fire Dispatch", StreamStatus.SENT, null),
                activity(model, now - 40_000, "Fire Feed", "1001", "Fire Dispatch", StreamStatus.ERROR, "HTTP 403"),
                activity(model, now - 30_000, "County Calls", "1002", "Law Main", StreamStatus.NOT_SENT,
                    "No stream named 'County Feed'"),
                activity(model, now - 20_000, "County Calls", "1003", "Medic 2", StreamStatus.DUPLICATE,
                    "1 SKIPPED"),
                activity(model, now - 10_000, "Fire Feed", "1004", "Engine 4", StreamStatus.PENDING, null));

            try(Connection connection = database.getConnection())
            {
                List<StreamActivityRecord> records = new ArrayList<>();

                for(StreamActivity activity: activities)
                {
                    StreamActivityRecord record = StreamActivityRecorder.toRecord(activity);
                    repository.insert(connection, record);
                    assertTrue(record.getId() > 0);
                    records.add(record);
                }

                //Update: the pending row is uploaded
                StreamActivityRecord pending = records.getLast();
                pending.setStatus(StreamStatus.UPLOADING);
                assertTrue(repository.update(connection, pending));
                StreamActivityRecord missing = StreamActivityRecorder.toRecord(activities.getFirst());
                missing.setId(Long.MAX_VALUE);
                assertFalse(repository.update(connection, missing));

                //Round trip
                List<StreamActivityRecord> all = repository.search(connection, StreamActivitySearchCriteria.all(),
                    0, 100);
                assertEquals(5, all.size());
                assertEquals(pending.getId(), all.getFirst().getId(), "newest first");
                StreamActivity restored = StreamActivityRecorder.toActivity(all.get(1));
                StreamActivity original = activities.get(3);
                assertEquals(original.getKey(), restored.getKey());
                assertEquals(original.getSelectingAliases(), restored.getSelectingAliases());
                assertEquals(BroadcastServerType.BROADCASTIFY_CALL, restored.getServerType());
                assertEquals(StreamStatus.DUPLICATE, restored.getStatus());
                assertEquals("1 SKIPPED", restored.getDetail());
                assertEquals(original.getStatusTime(), restored.getStatusTime());
                assertEquals("Site 1", restored.getSite());
                assertEquals(List.of("County Calls", "Fire Feed"), repository.getStreamNames(connection));

                //Search filters agree with the in-memory filter of the live rows
                List<StreamActivitySearchCriteria> criteria = List.of(
                    new StreamActivitySearchCriteria("County Calls", Set.of(), ""),
                    new StreamActivitySearchCriteria(null, Set.of(StreamStatus.ERROR, StreamStatus.NOT_SENT), ""),
                    new StreamActivitySearchCriteria(null, Set.of(), "fire disp"),
                    new StreamActivitySearchCriteria(null, Set.of(), "skipped"),
                    new StreamActivitySearchCriteria("Fire Feed", Set.of(StreamStatus.ERROR), "403"),
                    new StreamActivitySearchCriteria(null, Set.of(), "100%"));

                for(StreamActivitySearchCriteria c: criteria)
                {
                    List<String> expected = new ArrayList<>();

                    for(StreamActivity activity: activities)
                    {
                        StreamActivity current = activity == activities.getLast() ?
                            StreamActivityRecorder.toActivity(pending) : activity;

                        if(c.matches(current))
                        {
                            expected.addFirst(current.getKey());
                        }
                    }

                    List<String> found = repository.search(connection, c, 0, 100).stream()
                        .map(record -> StreamActivityRecorder.toActivity(record).getKey()).toList();
                    assertEquals(expected, found, c.toString());
                    assertEquals(expected.size(), repository.count(connection, c), c.toString());
                }

                //Rows left in progress by an earlier run are marked not sent
                assertEquals(1, repository.markInterrupted(connection, now + 60_000,
                    StreamActivityRecorder.INTERRUPTED_DETAIL));
                StreamActivityRecord interrupted = repository.search(connection, StreamActivitySearchCriteria.all(),
                    0, 1).getFirst();
                assertEquals(StreamStatus.NOT_SENT, interrupted.getStatus());
                assertEquals(StreamActivityRecorder.INTERRUPTED_DETAIL, interrupted.getDetail());
                assertEquals(0, repository.markInterrupted(connection, now + 60_000, "again"));

                //Age-off deletes the rows of calls that started before the cutoff
                assertEquals(2, repository.deleteStartedBefore(connection, now - 35_000));
                assertEquals(3, repository.count(connection));
            }

            try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
            {
                statement.execute(configuration.type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
            }
        }
    }
}
