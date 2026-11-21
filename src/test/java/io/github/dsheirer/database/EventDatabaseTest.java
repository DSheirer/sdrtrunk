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

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.database.call.IdentifierFilter;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.event.EventCategory;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.EventSearchCriteria;
import io.github.dsheirer.database.event.IEventRepository;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Events table: schema upgrade, insert/update, search filters (and agreement of the in-memory filter with the SQL
 * filter), transfer keys and the event age-off delete, against the built-in H2 database.
 *
 * The same checks run against network MariaDB and PostgreSQL servers when the SDRTRUNK_TEST_MARIADB and
 * SDRTRUNK_TEST_POSTGRES environment variables are set (see {@link CallDatabaseTest}).  The tests use (and drop) the
 * schema 'sdrtrunk_event_test'.
 */
public class EventDatabaseTest
{
    private static final String TEST_SCHEMA = "sdrtrunk_event_test";

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

    private static EventRecord event(long start, DecodeEventType type, String to, Long toValue, String toForm,
                                     String from, Long fromValue, String details)
    {
        EventRecord event = new EventRecord();
        event.setStartTime(start);
        event.setEventType(type.name());
        event.setProtocol(Protocol.APCO25.name());
        event.setSystem("Metro System");
        event.setSite("Site 1");
        event.setChannel("Control");
        event.setChannelDescriptor("1-0234");
        event.setFrequency(851_012_500);
        event.setTimeslot(-1);
        event.setToId(to);
        event.setToValue(toValue);
        event.setToForm(toForm);
        event.setFromId(from);
        event.setFromValue(fromValue);
        event.setFromForm(from != null ? IdentifierRecord.FORM_RADIO : null);
        event.setAliasList("Metro");
        event.setDetails(details);
        return event;
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
            assertEquals(0, database.getEventCount());

            IEventRepository repository = database.getEventRepository();

            try(Connection connection = database.getConnection())
            {
                EventRecord call = event(1000, DecodeEventType.CALL_GROUP, "1234", 1234L,
                    IdentifierRecord.FORM_TALKGROUP, "5551212", 5551212L, null);
                long callId = repository.insert(connection, call);
                assertTrue(callId > 0);
                call.setEndTime(4000);
                call.setDuration(3000);
                call.setDetails("Emergency 100%");
                assertTrue(repository.update(connection, call));

                EventRecord patch = event(2000, DecodeEventType.CALL_PATCH_GROUP, "P:500", 500L,
                    IdentifierRecord.FORM_PATCH_GROUP, "777", 777L, null);
                patch.setPatchedTalkgroups(List.of(new PatchedTalkgroup(2000, null), new PatchedTalkgroup(2001,
                    null)));
                repository.insert(connection, patch);

                EventRecord page = event(3000, DecodeEventType.PAGE, "888", 888L, IdentifierRecord.FORM_RADIO,
                    "777", 777L, "Page radio");
                repository.insert(connection, page);

                EventRecord registration = event(4000, DecodeEventType.REGISTER, null, null, null, "999", 999L,
                    "D".repeat(5000));
                registration.setProtocol(Protocol.DMR.name());
                repository.insert(connection, registration);

                assertEquals(4, repository.count(connection));
                assertEquals(4, database.getEventCount());

                List<EventRecord> all = repository.search(connection, EventSearchCriteria.all(), 0, 100);
                assertEquals(4000, all.getFirst().getStartTime(), "newest first");
                assertEquals(2048, all.getFirst().getDetails().length(), "long details truncated");
                EventRecord loadedCall = all.getLast();
                assertEquals(3000, loadedCall.getDuration());
                assertEquals(4000, loadedCall.getEndTime());
                assertEquals(DecodeEventType.CALL_GROUP, loadedCall.getEventTypeValue());
                assertEquals(EventCategory.VOICE_CALL, loadedCall.getCategory());
                assertEquals("1-0234", loadedCall.getChannelDescriptor());
                assertEquals(-1, loadedCall.getTimeslot());
                assertEquals(List.of(new PatchedTalkgroup(2000, null), new PatchedTalkgroup(2001, null)),
                    all.get(2).getPatchedTalkgroups(), "patched talkgroups loaded");

                List<EventRecord> loaded = new ArrayList<>(all);
                Alias patchedAlias = new Alias("Tac 2");
                patchedAlias.setAliasListName("Metro");
                patchedAlias.addAliasID(new Talkgroup(Protocol.APCO25, 2001));
                Alias radioAlias = new Alias("Engine 7");
                radioAlias.setAliasListName("Metro");
                radioAlias.addAliasID(new Radio(Protocol.APCO25, 888));

                check(repository, connection, loaded, criteria(IdentifierFilter.text("1234"), null), 1);
                check(repository, connection, loaded, criteria(IdentifierFilter.text("2001"), null), 1);
                check(repository, connection, loaded, criteria(IdentifierFilter.talkgroupAlias(patchedAlias), null),
                    1);
                check(repository, connection, loaded, criteria(null, IdentifierFilter.text("777")), 2);
                check(repository, connection, loaded, criteria(null, IdentifierFilter.text("888")), 1);
                check(repository, connection, loaded, criteria(null, IdentifierFilter.radioAlias(radioAlias)), 1);
                check(repository, connection, loaded, new EventSearchCriteria(null, null, EventCategory.VOICE_CALL,
                    null, null, null, null, 0, 0), 2);
                check(repository, connection, loaded, new EventSearchCriteria(null, null, null,
                    DecodeEventType.PAGE, null, null, null, 0, 0), 1);
                check(repository, connection, loaded, new EventSearchCriteria(null, null, EventCategory.REGISTRATION,
                    null, Protocol.DMR, "metro", null, 0, 0), 1);
                check(repository, connection, loaded, new EventSearchCriteria(null, null, null, null, null, null,
                    "emergency 100%", 0, 0), 1);
                check(repository, connection, loaded, new EventSearchCriteria(null, null, null, null, null, null,
                    "emergency%", 0, 0), 0);
                check(repository, connection, loaded, new EventSearchCriteria(null, null, null, null, null, null,
                    null, 1500, 3500), 2);

                assertEquals(2, repository.search(connection, EventSearchCriteria.all(), 1, 2).size(), "paging");

                Set<String> keys = repository.getTransferKeys(connection);
                assertEquals(4, keys.size());
                assertTrue(keys.contains(call.getTransferKey()));

                //Transfer: a batch insert keeps the patched talkgroups
                EventRecord copy = all.get(2).copy();
                repository.insertBatch(connection, List.of(copy, all.get(0).copy()));
                assertEquals(6, repository.count(connection));
                assertEquals(2, repository.count(connection, criteria(IdentifierFilter.text("2001"), null)));

                //Age-off deletes the old events and their patched talkgroups
                assertEquals(3, repository.deleteStartedBefore(connection, 3000));
                assertEquals(3, repository.count(connection));
                assertEquals(0, repository.count(connection, criteria(IdentifierFilter.text("2001"), null)));

                try(Statement statement = connection.createStatement();
                    var rs = statement.executeQuery("SELECT COUNT(*) FROM " + TEST_SCHEMA +
                        ".event_patched_talkgroups"))
                {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getLong(1), "patched rows deleted with their event");
                }

                assertFalse(repository.update(connection, loadedCall), "deleted row isn't updated");
            }

            try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
            {
                statement.execute(configuration.type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
            }
        }
    }

    private static EventSearchCriteria criteria(IdentifierFilter talkgroup, IdentifierFilter radio)
    {
        return new EventSearchCriteria(talkgroup, radio, null, null, null, null, null, 0, 0);
    }

    /**
     * Checks the database count for the criteria and that the in-memory filter selects the same events.
     */
    private static void check(IEventRepository repository, Connection connection, List<EventRecord> events,
                              EventSearchCriteria criteria, long expected) throws Exception
    {
        assertEquals(expected, repository.count(connection, criteria), "database count for " + criteria);
        assertEquals(expected, events.stream().filter(criteria::matches).count(), "in-memory count for " + criteria);
    }
}
