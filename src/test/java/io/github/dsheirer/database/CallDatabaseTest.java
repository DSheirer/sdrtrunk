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
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.ICallRepository;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.database.call.CallType;
import io.github.dsheirer.database.call.CallRepository;
import io.github.dsheirer.database.call.IdentifierFilter;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.call.RecordingState;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Schema installation, call insert/update/search and age-off queries against the built-in H2 database.
 *
 * The same checks run against network MariaDB and PostgreSQL servers when these environment variables are set to
 * host:port:user:password (and, for PostgreSQL, host:port:user:password:database):
 *   SDRTRUNK_TEST_MARIADB, SDRTRUNK_TEST_POSTGRES
 * The tests use (and drop) the schema 'sdrtrunk_unit_test'.
 */
public class CallDatabaseTest
{
    private static final String TEST_SCHEMA = "sdrtrunk_unit_test";

    @TempDir
    Path mTempDir;

    @Test
    public void h2()
        throws Exception
    {
        exercise(new DatabaseConfiguration(DatabaseType.H2, "", 0, "", TEST_SCHEMA, "", ""));
    }

    @Test
    public void mariadb()
        throws Exception
    {
        DatabaseConfiguration configuration = fromEnvironment("SDRTRUNK_TEST_MARIADB", DatabaseType.MARIADB);
        Assumptions.assumeTrue(configuration != null, "SDRTRUNK_TEST_MARIADB not set");
        exercise(configuration);
    }

    @Test
    public void postgres()
        throws Exception
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

    private static CallRecord call(long start, String to, long toValue, String talkerAlias, String from,
                                   long fromValue, String path, long size)
    {
        CallRecord call = new CallRecord();
        call.setStartTime(start);
        call.setProtocol(Protocol.APCO25.name());
        call.setSystem("Metro System");
        call.setSite("Site 1");
        call.setChannel("Control");
        call.setFrequency(851_012_500);
        call.setToId(to);
        call.setToValue(toValue);
        call.setToForm(CallRecord.FORM_TALKGROUP);
        call.setFromId(from);
        call.setFromValue(fromValue);
        call.setFromForm(CallRecord.FORM_RADIO);
        call.setAliasList("Metro");
        call.setTalkerAlias(talkerAlias);
        call.setRecordFormat("MP3");
        call.setFilePath(path);
        call.setFileSize(size);
        call.setRecordingState(path != null ? RecordingState.RECORDING : RecordingState.NONE);
        return call;
    }

    private void exercise(DatabaseConfiguration configuration) throws Exception
    {
        try(CallDatabase database = new CallDatabase(configuration, mTempDir))
        {
            try(Connection connection = database.getConnection();
                java.sql.Statement statement = connection.createStatement())
            {
                statement.execute(configuration.type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
            }

            assertEquals(SchemaStatus.MISSING, database.getSchemaStatus());
            database.install();
            assertEquals(SchemaStatus.SDRTRUNK, database.getSchemaStatus());
            //Installing again is a no-op
            database.install();

            ICallRepository repository = database.getCallRepository();

            try(Connection connection = database.getConnection())
            {
                CallRecord active = call(1000, "1234", 1234, "Fire Dispatch", "5551212", 5551212, "/rec/a.mp3", 0);
                long id = repository.insert(connection, active);
                assertTrue(id > 0);

                active.setEndTime(4000);
                active.setCallState(CallState.COMPLETE);
                active.setRecordingState(RecordingState.RECORDED);
                active.setFileSize(10_000);
                assertTrue(repository.update(connection, active));

                CallRecord second = call(5000, "2000", 2000, "Police_Tac 100%", "777", 777, "/rec/b.mp3", 0);
                second.setRecordingState(RecordingState.RECORDED);
                second.setFileSize(20_000);
                second.setCallState(CallState.COMPLETE);
                second.setEndTime(6000);
                repository.insert(connection, second);

                CallRecord unrecorded = call(7000, "2000", 2000, "Police_Tac 100%", "778", 778, null, 0);
                long unrecordedId = repository.insert(connection, unrecorded);
                assertNotEquals(id, unrecordedId);

                assertEquals(3, repository.count(connection));

                List<CallRecord> all = repository.search(connection, CallSearchCriteria.all(), 0, 100);
                assertEquals(3, all.size());
                assertEquals(7000, all.get(0).getStartTime(), "newest first");
                CallRecord loaded = all.get(2);
                assertEquals(3000, loaded.getDuration());
                assertEquals("Fire Dispatch", loaded.getTalkerAlias());
                assertEquals("Metro", loaded.getAliasList());
                assertEquals(1234L, loaded.getToValue());
                assertTrue(loaded.isToTalkgroup());
                assertTrue(loaded.isFromRadio());
                assertEquals(CallState.COMPLETE, loaded.getCallState());
                assertEquals(Protocol.APCO25, loaded.getProtocolValue());

                assertEquals(2, search(repository, connection, "2000", null, null));
                assertEquals(1, search(repository, connection, "123", null, null), "partial match");
                assertEquals(1, search(repository, connection, null, "5551212", null));
                assertEquals(1, search(repository, connection, null, null, "fire dis"), "case insensitive");
                assertEquals(2, search(repository, connection, null, null, "tac 100%"), "literal percent");
                assertEquals(0, search(repository, connection, null, null, "police%tac"), "escaped wildcard");
                assertEquals(0, search(repository, connection, null, null, "police_tac_"), "escaped underscore");
                assertEquals(2, repository.count(connection, new CallSearchCriteria(null, null, null,
                    Protocol.APCO25, "metro", 4000, 8000)));

                for(CallRecord call: all)
                {
                    CallSearchCriteria criteria = new CallSearchCriteria(IdentifierFilter.text("2000"), null, "tac",
                        null, null, 0, 0);
                    assertEquals(call.getToValue() == 2000, criteria.matches(call), "in-memory matches agrees");
                }

                //Roaming (fully qualified) talkgroup call: local value 2000, fully qualified 0xBEE00.1.2000
                CallRecord roaming = call(8000, "2000", 2000, null, "779", 779, null, 0);
                roaming.setToFullyQualified("781824.1.2000");
                repository.insert(connection, roaming);

                //Alias searches: the alias identifiers are translated to value, range and fully qualified filters
                Alias talkgroupAlias = new Alias("Police Tac");
                talkgroupAlias.setAliasListName("Metro");
                talkgroupAlias.addAliasID(new Talkgroup(Protocol.APCO25, 2000));
                talkgroupAlias.addAliasID(new TalkgroupRange(Protocol.APCO25, 1200, 1300));
                assertEquals(3, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(talkgroupAlias),
                    null), "talkgroup value and range, not the fully qualified call");

                Alias roamingAlias = new Alias("Roaming Tac");
                roamingAlias.setAliasListName("metro");
                roamingAlias.addAliasID(new P25FullyQualifiedTalkgroup(781824, 1, 2000));
                assertEquals(1, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(roamingAlias),
                    null), "fully qualified talkgroup only, alias list name case-insensitive");

                Alias otherList = new Alias("Police Tac");
                otherList.setAliasListName("County");
                otherList.addAliasID(new Talkgroup(Protocol.APCO25, 2000));
                assertEquals(0, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(otherList), null),
                    "alias from a different alias list");

                Alias dmrAlias = new Alias("DMR 2000");
                dmrAlias.setAliasListName("Metro");
                dmrAlias.addAliasID(new Talkgroup(Protocol.DMR, 2000));
                assertEquals(0, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(dmrAlias), null),
                    "different protocol");

                Alias radioAlias = new Alias("Engine 7");
                radioAlias.setAliasListName("Metro");
                radioAlias.addAliasID(new RadioRange(Protocol.APCO25, 776, 778));
                assertEquals(2, aliasSearch(repository, connection, null, IdentifierFilter.radioAlias(radioAlias)),
                    "radio range");
                assertEquals(2, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(talkgroupAlias),
                    IdentifierFilter.radioAlias(radioAlias)), "talkgroup and radio alias combined");

                List<CallRecord> withRoaming = repository.search(connection, CallSearchCriteria.all(), 0, 100);

                for(IdentifierFilter filter: List.of(IdentifierFilter.talkgroupAlias(talkgroupAlias),
                    IdentifierFilter.talkgroupAlias(roamingAlias), IdentifierFilter.talkgroupAlias(otherList),
                    IdentifierFilter.talkgroupAlias(dmrAlias)))
                {
                    CallSearchCriteria criteria = new CallSearchCriteria(filter, null, null, null, null, 0, 0);
                    long inMemory = withRoaming.stream().filter(criteria::matches).count();
                    assertEquals(repository.count(connection, criteria), inMemory, "in-memory alias match " + filter);
                }

                repository.delete(connection, List.of(roaming.getId()));

                //Retention rule: duplicates and Do Not Monitor calls without an alias record option
                CallRecord duplicate = call(100, "3000", 3000, null, "1", 1, null, 0);
                duplicate.setDuplicate(true);
                duplicate.setCallState(CallState.COMPLETE);
                CallRecord doNotMonitor = call(200, "3001", 3001, null, "2", 2, null, 0);
                doNotMonitor.setPriority(-1);
                doNotMonitor.setCallState(CallState.COMPLETE);
                CallRecord recordedDoNotMonitor = call(300, "3002", 3002, null, "3", 3, null, 0);
                recordedDoNotMonitor.setPriority(-1);
                recordedDoNotMonitor.setAliasRecord(true);
                recordedDoNotMonitor.setCallState(CallState.COMPLETE);
                CallRecord activeDuplicate = call(400, "3003", 3003, null, "4", 4, null, 0);
                activeDuplicate.setDuplicate(true);
                CallRecord newDuplicate = call(900_000, "3004", 3004, null, "5", 5, null, 0);
                newDuplicate.setDuplicate(true);
                newDuplicate.setCallState(CallState.COMPLETE);
                List<CallRecord> retention = List.of(duplicate, doNotMonitor, recordedDoNotMonitor, activeDuplicate,
                    newDuplicate);

                for(CallRecord call: retention)
                {
                    repository.insert(connection, call);
                }

                List<CallRecord> expired = repository.getDuplicateAndDoNotMonitorCalls(connection, 1000, 100);
                assertEquals(List.of(100L, 200L), expired.stream().map(CallRecord::getStartTime).toList(),
                    "completed duplicate and Do Not Monitor calls before the cutoff, oldest first");
                assertTrue(repository.search(connection, new CallSearchCriteria(IdentifierFilter.text("3002"), null,
                    null, null, null, 0, 0), 0, 1).getFirst().isAliasRecord());
                repository.delete(connection, retention.stream().map(CallRecord::getId).toList());

                //Patch group call: patch group 500 with patched talkgroups 2000 and 4000 (fully qualified 4001)
                CallRecord patch = call(9500, "P:500", 500, null, "9", 9, null, 0);
                patch.setToForm(CallRecord.FORM_PATCH_GROUP);
                patch.setPatchedTalkgroups(List.of(new PatchedTalkgroup(4000, null),
                    new PatchedTalkgroup(4001, "781824.1.4001")));
                repository.insert(connection, patch);
                patch.setPatchedTalkgroups(List.of(new PatchedTalkgroup(2000, null), new PatchedTalkgroup(4000, null),
                    new PatchedTalkgroup(4001, "781824.1.4001")));
                repository.update(connection, patch);

                CallRecord loadedPatch = repository.search(connection, new CallSearchCriteria(
                    IdentifierFilter.text("4000"), null, null, null, null, 0, 0), 0, 10).getFirst();
                assertEquals(patch.getId(), loadedPatch.getId(), "typed patched talkgroup finds the patch call");
                assertEquals(3, loadedPatch.getPatchedTalkgroups().size(), "patched talkgroups replaced on update");
                assertEquals(1, search(repository, connection, "500", null, null), "patch group value still matches");
                assertEquals(3, search(repository, connection, "2000", null, null),
                    "talkgroup 2000 calls plus the patch call that included it");

                Alias patchedAlias = new Alias("Tac 4000");
                patchedAlias.setAliasListName("Metro");
                patchedAlias.addAliasID(new TalkgroupRange(Protocol.APCO25, 3990, 4005));
                assertEquals(1, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(patchedAlias),
                    null), "alias range matches a patched talkgroup");
                Alias patchedFq = new Alias("Roaming 4001");
                patchedFq.setAliasListName("Metro");
                patchedFq.addAliasID(new P25FullyQualifiedTalkgroup(781824, 1, 4001));
                assertEquals(1, aliasSearch(repository, connection, IdentifierFilter.talkgroupAlias(patchedFq),
                    null), "fully qualified patched talkgroup");

                List<CallRecord> withPatch = repository.search(connection, CallSearchCriteria.all(), 0, 100);

                for(IdentifierFilter filter: List.of(IdentifierFilter.text("4000"), IdentifierFilter.text("2000"),
                    IdentifierFilter.talkgroupAlias(patchedAlias), IdentifierFilter.talkgroupAlias(patchedFq)))
                {
                    CallSearchCriteria criteria = new CallSearchCriteria(filter, null, null, null, null, 0, 0);
                    assertEquals(repository.count(connection, criteria),
                        withPatch.stream().filter(criteria::matches).count(), "in-memory patched match " + filter);
                }

                assertEquals(3, repository.readPage(connection, patch.getId() - 1, 1).getFirst()
                    .getPatchedTalkgroups().size(), "transfer page reads patched talkgroups");

                //Call type filter: one call of each type alongside the group and patch calls
                CallRecord privateCall = call(9600, "777", 777, null, "778", 778, null, 0);
                privateCall.setToForm(CallRecord.FORM_RADIO);
                CallRecord phoneTo = call(9601, "5551234", 0, null, "779", 779, null, 0);
                phoneTo.setToForm("TELEPHONE_NUMBER");
                phoneTo.setToValue(null);
                CallRecord phoneFrom = call(9602, "780", 780, null, "5559876", 0, null, 0);
                phoneFrom.setToForm(CallRecord.FORM_RADIO);
                phoneFrom.setFromForm("TELEPHONE_NUMBER");
                CallRecord analog = call(9603, null, 0, null, null, 0, null, 0);
                analog.setToForm(null);
                analog.setToValue(null);
                analog.setFromForm(null);
                List<CallRecord> typed = List.of(privateCall, phoneTo, phoneFrom, analog);

                for(CallRecord call: typed)
                {
                    repository.insert(connection, call);
                }

                List<CallRecord> everything = repository.search(connection, CallSearchCriteria.all(), 0, 1000);

                for(CallType type: CallType.values())
                {
                    CallSearchCriteria criteria = new CallSearchCriteria(null, null, null, null, type, null, 0, 0);
                    long expected = everything.stream().filter(c -> c.getCallType() == type).count();
                    assertEquals(expected, repository.count(connection, criteria), "SQL type filter " + type);
                    assertEquals(expected, everything.stream().filter(criteria::matches).count(),
                        "in-memory type filter " + type);
                    assertTrue(expected > 0, "test data has a " + type + " call");
                }

                assertEquals(2, repository.count(connection, new CallSearchCriteria(null, null, null, null,
                    CallType.TELEPHONE, null, 0, 0)), "telephone calls in both directions");
                repository.delete(connection, typed.stream().map(CallRecord::getId).toList());

                repository.delete(connection, List.of(patch.getId()));

                try(java.sql.Statement statement = connection.createStatement();
                    java.sql.ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + TEST_SCHEMA + "." +
                        CallRepository.PATCHED_TABLE))
                {
                    rs.next();
                    assertEquals(0, rs.getLong(1), "patched talkgroups deleted with the call");
                }

                assertEquals(30_000, repository.getRecordedFileSize(connection));
                List<CallRecord> recorded = repository.getRecordedCalls(connection, 0, 10);
                assertEquals(2, recorded.size());
                assertEquals(1000, recorded.get(0).getStartTime(), "oldest first");
                assertEquals(1, repository.getRecordedCalls(connection, 4000, 10).size());

                repository.markRecordingsDeleted(connection, List.of(id));
                assertEquals(20_000, repository.getRecordedFileSize(connection));
                CallRecord deleted = repository.search(connection, CallSearchCriteria.all(), 2, 1).getFirst();
                assertEquals(RecordingState.DELETED, deleted.getRecordingState());
                assertEquals(null, deleted.getFilePath());

                assertEquals(3, repository.getTransferKeys(connection).size());

                repository.delete(connection, List.of(unrecordedId));
                assertEquals(2, repository.count(connection));

                CallRecord interrupted = call(9000, "3", 3, null, "4", 4, "/rec/c.mp3", 0);
                repository.insert(connection, interrupted);
                assertEquals(1, repository.markInterrupted(connection));
                CallRecord reloaded = repository.search(connection, CallSearchCriteria.all(), 0, 1).getFirst();
                assertEquals(CallState.INTERRUPTED, reloaded.getCallState());
                assertEquals(RecordingState.RECORDED, reloaded.getRecordingState());

                List<CallRecord> transfer = repository.readPage(connection, 0, 100);
                assertEquals(3, transfer.size());
                repository.insertBatch(connection, transfer);
                assertEquals(6, repository.count(connection));
            }

            //Installed as the current schema version, and installing again leaves it unchanged
            database.install();

            try(Connection connection = database.getConnection();
                java.sql.Statement statement = connection.createStatement();
                java.sql.ResultSet rs = statement.executeQuery("SELECT version FROM " + TEST_SCHEMA + "." +
                    CallDatabase.SCHEMA_INFO_TABLE))
            {
                rs.next();
                assertEquals(CallDatabase.SCHEMA_VERSION, rs.getInt(1));
                assertFalse(rs.next(), "one version row");
                statement.executeQuery("SELECT COUNT(*) FROM " + TEST_SCHEMA + "." + CallRepository.PATCHED_TABLE)
                    .close();
            }

            //A schema from a newer version of sdrtrunk is refused
            try(Connection connection = database.getConnection();
                java.sql.Statement statement = connection.createStatement())
            {
                statement.executeUpdate("UPDATE " + TEST_SCHEMA + "." + CallDatabase.SCHEMA_INFO_TABLE +
                    " SET version = " + (CallDatabase.SCHEMA_VERSION + 1));
            }

            java.sql.SQLException newer = assertThrows(java.sql.SQLException.class, database::install);
            assertTrue(newer.getMessage().contains("newer version"), newer.getMessage());

            try(Connection connection = database.getConnection();
                java.sql.Statement statement = connection.createStatement())
            {
                statement.executeUpdate("UPDATE " + TEST_SCHEMA + "." + CallDatabase.SCHEMA_INFO_TABLE +
                    " SET version = " + CallDatabase.SCHEMA_VERSION);
            }

            String suggested = database.suggestSchemaName();
            assertEquals(TEST_SCHEMA + "_2", suggested);
            assertFalse(database.schemaExists(suggested));

            database.clear();
            assertEquals(SchemaStatus.SDRTRUNK, database.getSchemaStatus());
            assertEquals(0, database.getCallCount());

            try(Connection connection = database.getConnection();
                java.sql.Statement statement = connection.createStatement())
            {
                statement.execute(configuration.type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
            }
        }
    }

    private static long search(ICallRepository repository, Connection connection, String talkgroup, String radio,
                               String talkerAlias) throws Exception
    {
        return repository.count(connection, new CallSearchCriteria(IdentifierFilter.text(talkgroup),
            IdentifierFilter.text(radio), talkerAlias, null, null, 0, 0));
    }

    private static long aliasSearch(ICallRepository repository, Connection connection, IdentifierFilter to,
                                    IdentifierFilter from) throws Exception
    {
        return repository.count(connection, new CallSearchCriteria(to, from, null, null, null, 0, 0));
    }
}
