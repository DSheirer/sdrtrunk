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
package io.github.dsheirer.database.call;

import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.database.dialect.IDatabaseDialect;

import static io.github.dsheirer.database.SqlSupport.bindParameters;
import static io.github.dsheirer.database.SqlSupport.getIdentifierClause;
import static io.github.dsheirer.database.SqlSupport.getLikePattern;
import static io.github.dsheirer.database.SqlSupport.getLong;
import static io.github.dsheirer.database.SqlSupport.setLong;
import static io.github.dsheirer.database.SqlSupport.setString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JDBC implementation of the calls table, using SQL that is portable across all supported database engines.  All
 * methods take a connection from the caller and qualify the table name with the schema, so this class has no state
 * other than the schema name.
 */
public class CallRepository implements ICallRepository
{
    public static final String TABLE = "calls";
    public static final String PATCHED_TABLE = "patched_talkgroups";

    /**
     * Columns in insert/select order, excluding the generated id.
     */
    private static final String COLUMNS = "start_time, end_time, duration_ms, call_state, protocol, system_name, " +
        "site_name, channel_name, frequency, timeslot, to_id, to_value, to_form, to_fq, from_id, from_value, " +
        "from_form, from_fq, alias_list, talker_alias, encrypted, duplicate_call, priority, alias_record, " +
        "record_format, file_path, file_size, recording_state";
    private static final int COLUMN_COUNT = 28;

    /**
     * Maximum stored lengths of the text columns, so that a long alias or channel name can't fail an insert.
     */
    private static final int LENGTH_SHORT = 32;
    private static final int LENGTH_ID = 64;
    private static final int LENGTH_NAME = 255;
    private static final int LENGTH_PATH = 1024;

    private final String mSchema;
    private final String mTable;
    private final String mPatchedTable;

    /**
     * Constructs an instance
     * @param schema that holds the calls table
     */
    public CallRepository(String schema)
    {
        mSchema = schema;
        mTable = schema + "." + TABLE;
        mPatchedTable = schema + "." + PATCHED_TABLE;
    }

    /**
     * Statements that create the calls table and its indexes.
     */
    public static List<String> getCreateStatements(IDatabaseDialect dialect, String schema)
    {
        String table = schema + "." + TABLE;
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS " + table + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "start_time BIGINT NOT NULL, " +
            "end_time BIGINT NOT NULL, " +
            "duration_ms BIGINT NOT NULL, " +
            "call_state VARCHAR(" + LENGTH_SHORT + ") NOT NULL, " +
            "protocol VARCHAR(" + LENGTH_SHORT + "), " +
            "system_name VARCHAR(" + LENGTH_NAME + "), " +
            "site_name VARCHAR(" + LENGTH_NAME + "), " +
            "channel_name VARCHAR(" + LENGTH_NAME + "), " +
            "frequency BIGINT NOT NULL, " +
            "timeslot INT NOT NULL, " +
            "to_id VARCHAR(" + LENGTH_ID + "), " +
            "to_value BIGINT, " +
            "to_form VARCHAR(" + LENGTH_SHORT + "), " +
            "to_fq VARCHAR(" + LENGTH_ID + "), " +
            "from_id VARCHAR(" + LENGTH_ID + "), " +
            "from_value BIGINT, " +
            "from_form VARCHAR(" + LENGTH_SHORT + "), " +
            "from_fq VARCHAR(" + LENGTH_ID + "), " +
            "alias_list VARCHAR(" + LENGTH_NAME + "), " +
            "talker_alias VARCHAR(" + LENGTH_NAME + "), " +
            "encrypted BOOLEAN NOT NULL, " +
            "duplicate_call BOOLEAN NOT NULL, " +
            "priority INT NOT NULL, " +
            "alias_record BOOLEAN NOT NULL, " +
            "record_format VARCHAR(" + LENGTH_SHORT + "), " +
            "file_path VARCHAR(" + LENGTH_PATH + "), " +
            "file_size BIGINT NOT NULL, " +
            "recording_state VARCHAR(" + LENGTH_SHORT + ") NOT NULL)");

        //Index names are scoped to the schema (H2, PostgreSQL) or the table (MariaDB), so prefix them with the table
        statements.add("CREATE INDEX IF NOT EXISTS calls_start_time_idx ON " + table + " (start_time)");
        statements.add("CREATE INDEX IF NOT EXISTS calls_to_value_idx ON " + table + " (to_value)");
        statements.add("CREATE INDEX IF NOT EXISTS calls_from_value_idx ON " + table + " (from_value)");
        statements.add("CREATE INDEX IF NOT EXISTS calls_recording_state_idx ON " + table +
            " (recording_state, start_time)");

        //Talkgroups patched into patch group calls.  Rows are deleted with their call.
        String patched = schema + "." + PATCHED_TABLE;
        statements.add("CREATE TABLE IF NOT EXISTS " + patched + " (" +
            "call_id BIGINT NOT NULL REFERENCES " + table + "(id) ON DELETE CASCADE, " +
            "talkgroup BIGINT NOT NULL, " +
            "fq VARCHAR(" + LENGTH_ID + "))");
        statements.add("CREATE INDEX IF NOT EXISTS patched_talkgroups_call_idx ON " + patched + " (call_id)");
        statements.add("CREATE INDEX IF NOT EXISTS patched_talkgroups_talkgroup_idx ON " + patched + " (talkgroup)");
        return statements;
    }

    /**
     * Inserts the call and assigns the generated row id to the call.
     * @return generated row id
     */
    @Override
    public long insert(Connection connection, CallRecord call) throws SQLException
    {
        String sql = "INSERT INTO " + mTable + " (" + COLUMNS + ") VALUES (" + "?, ".repeat(COLUMN_COUNT - 1) + "?)";

        try(PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"}))
        {
            bind(statement, call);
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    long id = keys.getLong(1);
                    call.setId(id);
                    insertPatchedTalkgroups(connection, id, call.getPatchedTalkgroups());
                    return id;
                }
            }
        }

        throw new SQLException("Database didn't return a generated id for the inserted call");
    }

    /**
     * Inserts the calls in a single batch without retrieving the generated ids.
     */
    @Override
    public void insertBatch(Connection connection, List<CallRecord> calls) throws SQLException
    {
        String sql = "INSERT INTO " + mTable + " (" + COLUMNS + ") VALUES (" + "?, ".repeat(COLUMN_COUNT - 1) + "?)";

        List<CallRecord> patched = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            for(CallRecord call: calls)
            {
                //Calls with patched talkgroups need their generated id for the patched talkgroup rows
                if(!call.getPatchedTalkgroups().isEmpty())
                {
                    patched.add(call);
                    continue;
                }

                bind(statement, call);
                statement.addBatch();
            }

            statement.executeBatch();
        }

        for(CallRecord call: patched)
        {
            insert(connection, call);
        }
    }

    /**
     * Inserts the patched talkgroup rows of a call
     */
    private void insertPatchedTalkgroups(Connection connection, long callId, List<PatchedTalkgroup> talkgroups)
        throws SQLException
    {
        if(talkgroups.isEmpty())
        {
            return;
        }

        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mPatchedTable +
            " (call_id, talkgroup, fq) VALUES (?, ?, ?)"))
        {
            for(PatchedTalkgroup talkgroup: talkgroups)
            {
                statement.setLong(1, callId);
                statement.setLong(2, talkgroup.value());
                setString(statement, 3, talkgroup.fullyQualified(), LENGTH_ID);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    /**
     * Replaces the patched talkgroup rows of a call
     */
    private void replacePatchedTalkgroups(Connection connection, long callId, List<PatchedTalkgroup> talkgroups)
        throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mPatchedTable +
            " WHERE call_id = ?"))
        {
            statement.setLong(1, callId);
            statement.executeUpdate();
        }

        insertPatchedTalkgroups(connection, callId, talkgroups);
    }

    /**
     * Loads the patched talkgroups of the patch group calls in the list.
     */
    private void loadPatchedTalkgroups(Connection connection, List<CallRecord> calls) throws SQLException
    {
        Map<Long,CallRecord> patchCalls = new HashMap<>();

        for(CallRecord call: calls)
        {
            if(CallRecord.FORM_PATCH_GROUP.equals(call.getToForm()))
            {
                patchCalls.put(call.getId(), call);
            }
        }

        if(patchCalls.isEmpty())
        {
            return;
        }

        Map<Long,List<PatchedTalkgroup>> talkgroups = new HashMap<>();
        String placeholders = String.join(", ", Collections.nCopies(patchCalls.size(), "?"));

        try(PreparedStatement statement = connection.prepareStatement("SELECT call_id, talkgroup, fq FROM " +
            mPatchedTable + " WHERE call_id IN (" + placeholders + ") ORDER BY call_id, talkgroup"))
        {
            int index = 1;

            for(Long id: patchCalls.keySet())
            {
                statement.setLong(index++, id);
            }

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    talkgroups.computeIfAbsent(rs.getLong(1), id -> new ArrayList<>())
                        .add(new PatchedTalkgroup(rs.getLong(2), rs.getString(3)));
                }
            }
        }

        for(Map.Entry<Long,List<PatchedTalkgroup>> entry: talkgroups.entrySet())
        {
            patchCalls.get(entry.getKey()).setPatchedTalkgroups(entry.getValue());
        }
    }

    /**
     * Updates all columns of the existing call row.
     * @return true if the row exists and was updated
     */
    @Override
    public boolean update(Connection connection, CallRecord call) throws SQLException
    {
        String assignments = String.join(" = ?, ", COLUMNS.split(", ")) + " = ?";
        String sql = "UPDATE " + mTable + " SET " + assignments + " WHERE id = ?";

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            bind(statement, call);
            statement.setLong(COLUMN_COUNT + 1, call.getId());

            if(statement.executeUpdate() == 0)
            {
                return false;
            }
        }

        if(CallRecord.FORM_PATCH_GROUP.equals(call.getToForm()) || !call.getPatchedTalkgroups().isEmpty())
        {
            replacePatchedTalkgroups(connection, call.getId(), call.getPatchedTalkgroups());
        }

        return true;
    }

    /**
     * Binds the call column values to statement parameters 1 - COLUMN_COUNT.
     */
    private static void bind(PreparedStatement statement, CallRecord call) throws SQLException
    {
        int index = 1;
        statement.setLong(index++, call.getStartTime());
        statement.setLong(index++, call.getEndTime());
        statement.setLong(index++, call.getEndTime() > 0 ? call.getDuration() : 0);
        statement.setString(index++, call.getCallState().name());
        setString(statement, index++, call.getProtocol(), LENGTH_SHORT);
        setString(statement, index++, call.getSystem(), LENGTH_NAME);
        setString(statement, index++, call.getSite(), LENGTH_NAME);
        setString(statement, index++, call.getChannel(), LENGTH_NAME);
        statement.setLong(index++, call.getFrequency());
        statement.setInt(index++, call.getTimeslot());
        setString(statement, index++, call.getToId(), LENGTH_ID);
        setLong(statement, index++, call.getToValue());
        setString(statement, index++, call.getToForm(), LENGTH_SHORT);
        setString(statement, index++, call.getToFullyQualified(), LENGTH_ID);
        setString(statement, index++, call.getFromId(), LENGTH_ID);
        setLong(statement, index++, call.getFromValue());
        setString(statement, index++, call.getFromForm(), LENGTH_SHORT);
        setString(statement, index++, call.getFromFullyQualified(), LENGTH_ID);
        setString(statement, index++, call.getAliasList(), LENGTH_NAME);
        setString(statement, index++, call.getTalkerAlias(), LENGTH_NAME);
        statement.setBoolean(index++, call.isEncrypted());
        statement.setBoolean(index++, call.isDuplicate());
        statement.setInt(index++, call.getPriority());
        statement.setBoolean(index++, call.isAliasRecord());
        setString(statement, index++, call.getRecordFormat(), LENGTH_SHORT);
        setString(statement, index++, call.getFilePath(), LENGTH_PATH);
        statement.setLong(index++, call.getFileSize());
        statement.setString(index, call.getRecordingState().name());
    }

    /**
     * Creates a call record from the current result set row.
     */
    private static CallRecord read(ResultSet rs) throws SQLException
    {
        CallRecord call = new CallRecord();
        call.setId(rs.getLong("id"));
        call.setStartTime(rs.getLong("start_time"));
        call.setEndTime(rs.getLong("end_time"));
        call.setCallState(CallState.fromValue(rs.getString("call_state")));
        call.setProtocol(rs.getString("protocol"));
        call.setSystem(rs.getString("system_name"));
        call.setSite(rs.getString("site_name"));
        call.setChannel(rs.getString("channel_name"));
        call.setFrequency(rs.getLong("frequency"));
        call.setTimeslot(rs.getInt("timeslot"));
        call.setToId(rs.getString("to_id"));
        call.setToValue(getLong(rs, "to_value"));
        call.setToForm(rs.getString("to_form"));
        call.setToFullyQualified(rs.getString("to_fq"));
        call.setFromId(rs.getString("from_id"));
        call.setFromValue(getLong(rs, "from_value"));
        call.setFromForm(rs.getString("from_form"));
        call.setFromFullyQualified(rs.getString("from_fq"));
        call.setAliasList(rs.getString("alias_list"));
        call.setTalkerAlias(rs.getString("talker_alias"));
        call.setEncrypted(rs.getBoolean("encrypted"));
        call.setDuplicate(rs.getBoolean("duplicate_call"));
        call.setPriority(rs.getInt("priority"));
        call.setAliasRecord(rs.getBoolean("alias_record"));
        call.setRecordFormat(rs.getString("record_format"));
        call.setFilePath(rs.getString("file_path"));
        call.setFileSize(rs.getLong("file_size"));
        call.setRecordingState(RecordingState.fromValue(rs.getString("recording_state")));
        return call;
    }

    /**
     * Marks any calls left in the active state, by a previous session that didn't shut down cleanly, as interrupted.
     * Recordings that were being written are left as recorded if the file has data.
     * @return count of calls updated
     */
    @Override
    public int markInterrupted(Connection connection) throws SQLException
    {
        try(Statement statement = connection.createStatement())
        {
            int count = statement.executeUpdate("UPDATE " + mTable + " SET call_state = '" +
                CallState.INTERRUPTED.name() + "' WHERE call_state = '" + CallState.ACTIVE.name() + "'");
            statement.executeUpdate("UPDATE " + mTable + " SET recording_state = '" + RecordingState.RECORDED.name() +
                "' WHERE recording_state = '" + RecordingState.RECORDING.name() + "' AND file_path IS NOT NULL");
            return count;
        }
    }

    @Override
    public int deleteAll(Connection connection) throws SQLException
    {
        try(Statement statement = connection.createStatement())
        {
            statement.executeUpdate("DELETE FROM " + mPatchedTable);
            return statement.executeUpdate("DELETE FROM " + mTable);
        }
    }

    @Override
    public int deleteActive(Connection connection) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mTable +
            " WHERE call_state = ?"))
        {
            statement.setString(1, CallState.ACTIVE.name());
            return statement.executeUpdate();
        }
    }

    /**
     * Searches for calls matching the criteria, newest first.
     * @param criteria to filter by
     * @param offset of the first row to return
     * @param limit maximum rows to return
     */
    @Override
    public List<CallRecord> search(Connection connection, CallSearchCriteria criteria, int offset, int limit)
        throws SQLException
    {
        List<Object> parameters = new ArrayList<>();
        String where = getWhereClause(criteria, parameters);
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + " c" + where +
            " ORDER BY start_time DESC, id DESC LIMIT ? OFFSET ?";
        parameters.add(limit);
        parameters.add(offset);

        List<CallRecord> calls = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            bindParameters(statement, parameters);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    calls.add(read(rs));
                }
            }
        }

        loadPatchedTalkgroups(connection, calls);
        return calls;
    }

    /**
     * Count of calls matching the criteria
     */
    @Override
    public long count(Connection connection, CallSearchCriteria criteria) throws SQLException
    {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM " + mTable + " c" + getWhereClause(criteria, parameters);

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            bindParameters(statement, parameters);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /**
     * Builds the WHERE clause (with a leading space) for the criteria and adds the parameter values to the list.
     */
    private String getWhereClause(CallSearchCriteria criteria, List<Object> parameters)
    {
        List<String> clauses = new ArrayList<>();

        if(!criteria.to().isEmpty())
        {
            clauses.add(getIdentifierClause("to", criteria.to(), "to_form IN ('" + CallRecord.FORM_TALKGROUP + "', '" +
                CallRecord.FORM_PATCH_GROUP + "')", mPatchedTable, "call_id", parameters));
        }

        if(!criteria.from().isEmpty())
        {
            clauses.add(getIdentifierClause("from", criteria.from(), "from_form = '" + CallRecord.FORM_RADIO + "'",
                null, null, parameters));
        }

        if(!criteria.talkerAlias().isEmpty())
        {
            clauses.add("LOWER(talker_alias) LIKE ? ESCAPE '!'");
            parameters.add(getLikePattern(criteria.talkerAlias()));
        }

        if(criteria.protocol() != null)
        {
            clauses.add("protocol = ?");
            parameters.add(criteria.protocol().name());
        }

        if(criteria.callType() != null)
        {
            clauses.add(criteria.callType().getWhereClause());
        }

        if(!criteria.system().isEmpty())
        {
            clauses.add("LOWER(system_name) LIKE ? ESCAPE '!'");
            parameters.add(getLikePattern(criteria.system()));
        }

        if(criteria.startTime() > 0)
        {
            clauses.add("start_time >= ?");
            parameters.add(criteria.startTime());
        }

        if(criteria.endTime() > 0)
        {
            clauses.add("start_time <= ?");
            parameters.add(criteria.endTime());
        }

        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    /**
     * Reads a page of calls in id order, for transferring calls between databases.
     * @param afterId only return calls with an id greater than this value
     * @param limit maximum number of calls
     */
    @Override
    public List<CallRecord> readPage(Connection connection, long afterId, int limit) throws SQLException
    {
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + " WHERE id > ? ORDER BY id LIMIT ?";
        List<CallRecord> calls = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, afterId);
            statement.setInt(2, limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    calls.add(read(rs));
                }
            }
        }

        loadPatchedTalkgroups(connection, calls);
        return calls;
    }

    /**
     * Total number of call rows
     */
    @Override
    public long count(Connection connection) throws SQLException
    {
        return count(connection, CallSearchCriteria.all());
    }

    /**
     * Transfer keys (see {@link CallRecord#getTransferKey()}) of all calls in the table, for skipping calls that
     * already exist when appending.
     */
    @Override
    public Set<String> getTransferKeys(Connection connection) throws SQLException
    {
        Set<String> keys = new HashSet<>();
        long afterId = 0;
        List<CallRecord> page = readPage(connection, afterId, 5000);

        while(!page.isEmpty())
        {
            for(CallRecord call: page)
            {
                keys.add(call.getTransferKey());
            }

            afterId = page.getLast().getId();
            page = readPage(connection, afterId, 5000);
        }

        return keys;
    }

    /**
     * Calls with a recording, oldest first, for age-off processing.
     * @param startedBefore only include calls that started before this time, or 0 for any time
     * @param limit maximum number of calls
     */
    @Override
    public List<CallRecord> getRecordedCalls(Connection connection, long startedBefore, int limit) throws SQLException
    {
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + " WHERE recording_state = ?" +
            (startedBefore > 0 ? " AND start_time < ?" : "") + " ORDER BY start_time, id LIMIT ?";
        List<CallRecord> calls = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            int index = 1;
            statement.setString(index++, RecordingState.RECORDED.name());

            if(startedBefore > 0)
            {
                statement.setLong(index++, startedBefore);
            }

            statement.setInt(index, limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    calls.add(read(rs));
                }
            }
        }

        return calls;
    }

    /**
     * Completed calls that started before the time, oldest first, for age-off processing.
     */
    @Override
    public List<CallRecord> getCallsStartedBefore(Connection connection, long startedBefore, int limit)
        throws SQLException
    {
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + " WHERE start_time < ? AND call_state <> ? " +
            "ORDER BY start_time, id LIMIT ?";
        List<CallRecord> calls = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, startedBefore);
            statement.setString(2, CallState.ACTIVE.name());
            statement.setInt(3, limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    calls.add(read(rs));
                }
            }
        }

        return calls;
    }

    @Override
    public List<CallRecord> getDuplicateAndDoNotMonitorCalls(Connection connection, long startedBefore, int limit)
        throws SQLException
    {
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + " WHERE start_time < ? AND call_state <> ? AND " +
            "(duplicate_call = ? OR (priority <= ? AND alias_record = ?)) ORDER BY start_time, id LIMIT ?";
        List<CallRecord> calls = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setLong(1, startedBefore);
            statement.setString(2, CallState.ACTIVE.name());
            statement.setBoolean(3, true);
            statement.setInt(4, Priority.DO_NOT_MONITOR);
            statement.setBoolean(5, false);
            statement.setInt(6, limit);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    calls.add(read(rs));
                }
            }
        }

        return calls;
    }

    /**
     * Total size of all recordings that are still available.
     */
    @Override
    public long getRecordedFileSize(Connection connection) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("SELECT SUM(file_size) FROM " + mTable +
            " WHERE recording_state = ?"))
        {
            statement.setString(1, RecordingState.RECORDED.name());

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /**
     * Deletes the call rows
     */
    @Override
    public void delete(Connection connection, List<Long> ids) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mTable + " WHERE id = ?"))
        {
            for(Long id: ids)
            {
                statement.setLong(1, id);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    /**
     * Clears the recording file of the calls and marks the recordings as deleted, keeping the call rows.
     */
    @Override
    public void markRecordingsDeleted(Connection connection, List<Long> ids) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mTable + " SET recording_state = ?, " +
            "file_path = NULL, file_size = 0 WHERE id = ?"))
        {
            for(Long id: ids)
            {
                statement.setString(1, RecordingState.DELETED.name());
                statement.setLong(2, id);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    /**
     * Schema name
     */
    @Override
    public String getSchema()
    {
        return mSchema;
    }
}
