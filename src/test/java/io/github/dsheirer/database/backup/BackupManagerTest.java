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
package io.github.dsheirer.database.backup;

import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.IdentifierRecord;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.database.call.IdentifierFilter;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.call.RecordingState;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.EventSearchCriteria;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database backup and restore: backup file contents, restore (including across database types), retention and the
 * automatic backup schedule.
 *
 * The restore checks also run against network MariaDB and PostgreSQL servers when the SDRTRUNK_TEST_MARIADB and
 * SDRTRUNK_TEST_POSTGRES environment variables are set (see CallDatabaseTest).  They use (and drop) the schema
 * 'sdrtrunk_backup_test'.
 */
public class BackupManagerTest
{
    private static final String TEST_SCHEMA = "sdrtrunk_backup_test";

    @TempDir
    Path mTempDir;

    private static DatabaseConfiguration h2()
    {
        return new DatabaseConfiguration(DatabaseType.H2, "", 0, "", TEST_SCHEMA, "", "");
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

    private CallDatabase open(DatabaseConfiguration configuration) throws Exception
    {
        CallDatabase database = new CallDatabase(configuration, mTempDir.resolve("db"));

        try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute(configuration.type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
        }

        database.install();
        return database;
    }

    private static void drop(CallDatabase database) throws Exception
    {
        try(Connection connection = database.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute(database.getConfiguration().type().getDialect().getDropSchemaStatement(TEST_SCHEMA));
        }
    }

    private static CallRecord call(long start, long talkgroup, String path, RecordingState state, CallState callState)
    {
        CallRecord call = new CallRecord();
        call.setStartTime(start);
        call.setEndTime(callState == CallState.ACTIVE ? 0 : start + 2000);
        call.setCallState(callState);
        call.setProtocol(Protocol.APCO25.name());
        call.setSystem("Metro");
        call.setChannel("Control");
        call.setFrequency(851_012_500);
        call.setToId(String.valueOf(talkgroup));
        call.setToValue(talkgroup);
        call.setToForm(IdentifierRecord.FORM_TALKGROUP);
        call.setFromId("5551212");
        call.setFromValue(5551212L);
        call.setFromForm(IdentifierRecord.FORM_RADIO);
        call.setAliasList("Metro");
        call.setTalkerAlias("Engine \"4\" ✓");
        call.setEncrypted(talkgroup == 3);
        call.setPriority(-1);
        call.setRecordFormat("MP3");
        call.setFilePath(path);
        call.setFileSize(path != null ? 1234 : 0);
        call.setRecordingState(state);
        return call;
    }

    private static EventRecord event(long start, DecodeEventType type, String details)
    {
        EventRecord event = new EventRecord();
        event.setStartTime(start);
        event.setEndTime(start + 500);
        event.setDuration(500);
        event.setEventType(type.name());
        event.setProtocol(Protocol.APCO25.name());
        event.setChannelDescriptor("1-0234");
        event.setTimeslot(-1);
        event.setToId("P:500");
        event.setToValue(500L);
        event.setToForm(IdentifierRecord.FORM_PATCH_GROUP);
        event.setPatchedTalkgroups(List.of(new PatchedTalkgroup(2001, null), new PatchedTalkgroup(2002, "1.2.3")));
        event.setAliasList("Metro");
        event.setDetails(details);
        return event;
    }

    /**
     * Fills the database: a call with an existing recording, a call whose recording was deleted, a call in progress,
     * and two events with patched talkgroups.
     */
    private void populate(CallDatabase database, Path recording) throws Exception
    {
        try(Connection connection = database.getConnection())
        {
            database.getCallRepository().insert(connection, call(1000, 1, recording.toString(),
                RecordingState.RECORDED, CallState.COMPLETE));
            database.getCallRepository().insert(connection, call(2000, 2, mTempDir.resolve("gone.mp3").toString(),
                RecordingState.RECORDED, CallState.COMPLETE));
            database.getCallRepository().insert(connection, call(3000, 3, null, RecordingState.NONE,
                CallState.ACTIVE));
            database.getEventRepository().insert(connection, event(1500, DecodeEventType.CALL_PATCH_GROUP,
                "Line 1\nLine \"2\""));
            database.getEventRepository().insert(connection, event(2500, DecodeEventType.TEXT_MESSAGE, null));
        }
    }

    @Test
    public void h2BackupAndRestore() throws Exception
    {
        backupAndRestore(h2());
    }

    @Test
    public void mariadbBackupAndRestore() throws Exception
    {
        DatabaseConfiguration configuration = fromEnvironment("SDRTRUNK_TEST_MARIADB", DatabaseType.MARIADB);
        Assumptions.assumeTrue(configuration != null, "SDRTRUNK_TEST_MARIADB not set");
        backupAndRestore(configuration);
    }

    @Test
    public void postgresBackupAndRestore() throws Exception
    {
        DatabaseConfiguration configuration = fromEnvironment("SDRTRUNK_TEST_POSTGRES", DatabaseType.POSTGRESQL);
        Assumptions.assumeTrue(configuration != null, "SDRTRUNK_TEST_POSTGRES not set");
        backupAndRestore(configuration);
    }

    private void backupAndRestore(DatabaseConfiguration configuration) throws Exception
    {
        Path recording = Files.writeString(mTempDir.resolve("call.mp3"), "audio");
        Path backups = mTempDir.resolve("backups");

        try(CallDatabase database = open(configuration))
        {
            populate(database, recording);
            BackupInfo backup = BackupManager.writeBackup(database, backups, null, System.currentTimeMillis());
            assertTrue(backup.isValid());
            assertEquals(3, backup.metadata().calls());
            assertEquals(2, backup.metadata().events());
            assertEquals(BackupManager.FORMAT_VERSION, backup.metadata().format());
            assertEquals(CallDatabase.SCHEMA_VERSION, backup.metadata().schemaVersion());
            assertNull(backup.tag());
            assertFalse(Files.exists(backups.resolve(backup.path().getFileName() + ".part")), "no partial file");

            //Change the database after the backup
            try(Connection connection = database.getConnection())
            {
                database.getCallRepository().insert(connection, call(9000, 9, null, RecordingState.NONE,
                    CallState.COMPLETE));
                database.getEventRepository().deleteAll(connection);
            }

            BackupManager.RestoreResult result = BackupManager.restore(database, backup.path(), status -> {});
            assertEquals(3, result.calls());
            assertEquals(2, result.events());
            assertEquals(1, result.missingRecordings());
            verifyRestored(database, recording);
            drop(database);
        }

        //The same backup restores into a different database type
        if(configuration.type() != DatabaseType.H2)
        {
            try(CallDatabase other = new CallDatabase(h2().withSchema("sdrtrunk_backup_other"),
                mTempDir.resolve("other")))
            {
                other.install();
                BackupInfo backup = BackupManager.list(backups).getFirst();
                BackupManager.restore(other, backup.path(), status -> {});
                verifyRestored(other, recording);
            }
        }
    }

    private static void verifyRestored(CallDatabase database, Path recording) throws Exception
    {
        try(Connection connection = database.getConnection())
        {
            List<CallRecord> calls = database.getCallRepository().search(connection, CallSearchCriteria.all(), 0, 10);
            assertEquals(3, calls.size(), "calls added after the backup are removed");
            CallRecord active = calls.get(0);
            CallRecord missing = calls.get(1);
            CallRecord recorded = calls.get(2);
            assertEquals(CallState.INTERRUPTED, active.getCallState(), "call in progress at backup is interrupted");
            assertTrue(active.isEncrypted());
            assertEquals(RecordingState.DELETED, missing.getRecordingState(), "missing recording marked deleted");
            assertNull(missing.getFilePath());
            assertEquals(RecordingState.RECORDED, recorded.getRecordingState());
            assertEquals(recording.toString(), recorded.getFilePath());
            assertEquals(1234, recorded.getFileSize());
            assertEquals(2000, recorded.getEndTime() - recorded.getStartTime());
            assertEquals("Engine \"4\" ✓", recorded.getTalkerAlias());
            assertEquals(-1, recorded.getPriority());
            assertEquals("Metro", recorded.getAliasList());
            assertEquals(5551212L, recorded.getFromValue());

            List<EventRecord> events = database.getEventRepository().search(connection, EventSearchCriteria.all(),
                0, 10);
            assertEquals(2, events.size());
            EventRecord patch = events.get(1);
            assertEquals("Line 1\nLine \"2\"", patch.getDetails());
            assertEquals(500, patch.getDuration());
            assertEquals("1-0234", patch.getChannelDescriptor());
            assertEquals(-1, patch.getTimeslot());
            assertEquals(List.of(new PatchedTalkgroup(2001, null), new PatchedTalkgroup(2002, "1.2.3")),
                patch.getPatchedTalkgroups());
            assertNull(events.get(0).getDetails());
            assertEquals(2, database.getEventRepository().count(connection, new EventSearchCriteria(
                IdentifierFilter.text("2002"), null, null, null, null, null, null, 0, 0)), "patched search works");
        }
    }

    @Test
    public void failedRestoreLeavesDatabaseUnchanged() throws Exception
    {
        Path recording = Files.writeString(mTempDir.resolve("call.mp3"), "audio");
        Path backups = mTempDir.resolve("backups");

        try(CallDatabase database = open(h2()))
        {
            populate(database, recording);
            Files.createDirectories(backups);

            //Valid metadata, but the calls entry is truncated JSON
            Path broken = backups.resolve(BackupManager.getFileName(System.currentTimeMillis(), null));
            writeZip(broken, "{\"format\":1,\"schema_version\":3,\"created\":1,\"calls\":2,\"events\":0}",
                "[{\"start_time\":1,\"call_state\":\"COMPLETE\",\"recording_state\":\"NONE\"},{\"start_ti", "[]");
            assertThrows(IOException.class, () -> BackupManager.restore(database, broken, status -> {}));

            try(Connection connection = database.getConnection())
            {
                assertEquals(3, database.getCallRepository().count(connection), "rolled back");
                assertEquals(2, database.getEventRepository().count(connection));
            }

            //Newer backup format is rejected before anything is changed
            Path newer = backups.resolve(BackupManager.getFileName(System.currentTimeMillis() + 5000, null));
            writeZip(newer, "{\"format\":99,\"schema_version\":3,\"created\":1,\"calls\":0,\"events\":0}", "[]",
                "[]");
            IOException error = assertThrows(IOException.class, () -> BackupManager.restore(database, newer,
                status -> {}));
            assertTrue(error.getMessage().contains("newer version"));

            //Not a zip file: listed as unreadable
            Files.writeString(backups.resolve(BackupManager.getFileName(System.currentTimeMillis() + 10_000, null)),
                "junk");
            List<BackupInfo> listed = BackupManager.list(backups);
            assertEquals(3, listed.size());
            assertFalse(listed.getFirst().isValid());
        }
    }

    private static void writeZip(Path file, String metadata, String calls, String events) throws IOException
    {
        try(OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out))
        {
            for(String[] entry: new String[][]{{BackupManager.ENTRY_METADATA, metadata},
                {BackupManager.ENTRY_CALLS, calls}, {BackupManager.ENTRY_EVENTS, events}})
            {
                zip.putNextEntry(new ZipEntry(entry[0]));
                zip.write(entry[1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    @Test
    public void retentionKeepsNewestBackups() throws Exception
    {
        Path backups = mTempDir.resolve("backups");

        try(CallDatabase database = open(h2()))
        {
            long now = System.currentTimeMillis();

            for(int x = 0; x < 5; x++)
            {
                BackupManager.writeBackup(database, backups, x == 2 ? BackupManager.TAG_PRE_RESTORE : null,
                    now + x * 60_000L);
            }

            //Same second as an existing backup gets a unique name
            BackupInfo same = BackupManager.writeBackup(database, backups, null, now + 4 * 60_000L);
            assertEquals(6, BackupManager.list(backups).size());
            assertEquals(now - now % 1000 + 4 * 60_000L + 1000, same.created());

            Files.writeString(backups.resolve("notes.txt"), "not a backup");
            List<Path> deleted = BackupManager.applyRetention(backups, 3);
            assertEquals(3, deleted.size());
            List<BackupInfo> remaining = BackupManager.list(backups);
            assertEquals(3, remaining.size());
            assertEquals(same.path(), remaining.getFirst().path(), "newest first");
            assertTrue(remaining.stream().noneMatch(BackupInfo::isPreRestore), "oldest deleted, including tagged");
            assertTrue(Files.exists(backups.resolve("notes.txt")), "other files are never deleted");

            assertEquals(2, BackupManager.applyRetention(backups, 0).size());
            assertEquals(1, BackupManager.list(backups).size(), "retention is at least 1");
        }
    }

    @Test
    public void scheduleIsDueAfterInterval()
    {
        long now = System.currentTimeMillis();
        long day = TimeUnit.DAYS.toMillis(1);
        assertTrue(BackupManager.isDue(0, BackupInterval.DAY_1, now), "no backups yet");
        assertFalse(BackupManager.isDue(now - day + 60_000, BackupInterval.DAY_1, now));
        assertTrue(BackupManager.isDue(now - day, BackupInterval.DAY_1, now));
        assertFalse(BackupManager.isDue(0, BackupInterval.NEVER, now), "automatic backups off");
        assertTrue(BackupManager.isDue(now - TimeUnit.HOURS.toMillis(7), BackupInterval.HOURS_6, now));
    }

    @Test
    public void fileNameRoundTrip() throws Exception
    {
        long created = 1_700_000_000_000L;
        String name = BackupManager.getFileName(created, BackupManager.TAG_PRE_RESTORE);
        assertTrue(name.startsWith("sdrtrunk-backup-") && name.endsWith("-pre-restore.zip"), name);
        BackupInfo info = BackupManager.read(Files.writeString(mTempDir.resolve(name), "x"));
        assertNotNull(info);
        assertEquals(created, info.created());
        assertTrue(info.isPreRestore());
        assertFalse(info.isValid());
        assertNull(BackupManager.read(Files.writeString(mTempDir.resolve("sdrtrunk-backup-x.zip"), "x")));
    }
}
