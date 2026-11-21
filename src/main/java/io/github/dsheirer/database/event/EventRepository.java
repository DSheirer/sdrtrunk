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
package io.github.dsheirer.database.event;

import io.github.dsheirer.database.IdentifierRecord;
import io.github.dsheirer.database.call.PatchedTalkgroup;
import io.github.dsheirer.database.dialect.IDatabaseDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.dsheirer.database.SqlSupport.bindParameters;
import static io.github.dsheirer.database.SqlSupport.getIdentifierClause;
import static io.github.dsheirer.database.SqlSupport.getLikePattern;
import static io.github.dsheirer.database.SqlSupport.getLong;
import static io.github.dsheirer.database.SqlSupport.setLong;
import static io.github.dsheirer.database.SqlSupport.setString;

/**
 * JDBC implementation of the events table, using SQL that is portable across all supported database engines.  All
 * methods take a connection from the caller and qualify the table names with the schema.
 */
public class EventRepository implements IEventRepository
{
    public static final String TABLE = "events";
    public static final String PATCHED_TABLE = "event_patched_talkgroups";

    /**
     * Columns in insert/select order, excluding the generated id.
     */
    private static final String COLUMNS = "start_time, end_time, duration_ms, event_type, protocol, system_name, " +
        "site_name, channel_name, channel_descriptor, frequency, timeslot, to_id, to_value, to_form, to_fq, from_id, " +
        "from_value, from_form, from_fq, alias_list, talker_alias, details";
    private static final int COLUMN_COUNT = 22;

    /**
     * Maximum stored lengths of the text columns, so that a long value can't fail an insert.  Events can have more
     * than one TO or FROM identifier, so the identifier columns are longer than in the calls table.
     */
    private static final int LENGTH_SHORT = 48;
    private static final int LENGTH_ID = 64;
    private static final int LENGTH_NAME = 255;
    static final int LENGTH_DETAILS = 2048;

    private static final String TO_TALKGROUP_FORMS = "to_form IN ('" + IdentifierRecord.FORM_TALKGROUP + "', '" +
        IdentifierRecord.FORM_PATCH_GROUP + "')";
    private static final String TO_RADIO_FORM = "to_form = '" + IdentifierRecord.FORM_RADIO + "'";
    private static final String FROM_RADIO_FORM = "from_form = '" + IdentifierRecord.FORM_RADIO + "'";

    private final String mSchema;
    private final String mTable;
    private final String mPatchedTable;

    /**
     * Constructs an instance
     * @param schema that holds the events tables
     */
    public EventRepository(String schema)
    {
        mSchema = schema;
        mTable = schema + "." + TABLE;
        mPatchedTable = schema + "." + PATCHED_TABLE;
    }

    /**
     * Statements that create the events tables and their indexes.
     */
    public static List<String> getCreateStatements(IDatabaseDialect dialect, String schema)
    {
        String table = schema + "." + TABLE;
        String patchedTable = schema + "." + PATCHED_TABLE;
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS " + table + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "start_time BIGINT NOT NULL, " +
            "end_time BIGINT NOT NULL, " +
            "duration_ms BIGINT NOT NULL, " +
            "event_type VARCHAR(" + LENGTH_SHORT + ") NOT NULL, " +
            "protocol VARCHAR(" + LENGTH_SHORT + "), " +
            "system_name VARCHAR(" + LENGTH_NAME + "), " +
            "site_name VARCHAR(" + LENGTH_NAME + "), " +
            "channel_name VARCHAR(" + LENGTH_NAME + "), " +
            "channel_descriptor VARCHAR(" + LENGTH_NAME + "), " +
            "frequency BIGINT NOT NULL, " +
            "timeslot INT NOT NULL, " +
            "to_id VARCHAR(" + LENGTH_NAME + "), " +
            "to_value BIGINT, " +
            "to_form VARCHAR(" + LENGTH_SHORT + "), " +
            "to_fq VARCHAR(" + LENGTH_ID + "), " +
            "from_id VARCHAR(" + LENGTH_NAME + "), " +
            "from_value BIGINT, " +
            "from_form VARCHAR(" + LENGTH_SHORT + "), " +
            "from_fq VARCHAR(" + LENGTH_ID + "), " +
            "alias_list VARCHAR(" + LENGTH_NAME + "), " +
            "talker_alias VARCHAR(" + LENGTH_NAME + "), " +
            "details VARCHAR(" + LENGTH_DETAILS + "))");

        //Index names are scoped to the schema (H2, PostgreSQL) or the table (MariaDB), so prefix them with the table
        statements.add("CREATE INDEX IF NOT EXISTS events_start_time_idx ON " + table + " (start_time)");
        statements.add("CREATE INDEX IF NOT EXISTS events_to_value_idx ON " + table + " (to_value)");
        statements.add("CREATE INDEX IF NOT EXISTS events_from_value_idx ON " + table + " (from_value)");
        statements.add("CREATE INDEX IF NOT EXISTS events_event_type_idx ON " + table + " (event_type, start_time)");

        statements.add("CREATE TABLE IF NOT EXISTS " + patchedTable + " (" +
            "event_id BIGINT NOT NULL REFERENCES " + table + "(id) ON DELETE CASCADE, " +
            "talkgroup BIGINT NOT NULL, " +
            "fq VARCHAR(" + LENGTH_ID + "))");
        statements.add("CREATE INDEX IF NOT EXISTS event_patched_event_idx ON " + patchedTable + " (event_id)");
        statements.add("CREATE INDEX IF NOT EXISTS event_patched_talkgroup_idx ON " + patchedTable + " (talkgroup)");
        return statements;
    }

    private String getInsertStatement()
    {
        return "INSERT INTO " + mTable + " (" + COLUMNS + ") VALUES (" + "?, ".repeat(COLUMN_COUNT - 1) + "?)";
    }

    @Override
    public long insert(Connection connection, EventRecord event) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement(getInsertStatement(), new String[]{"id"}))
        {
            bind(statement, event);
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    long id = keys.getLong(1);
                    event.setId(id);
                    insertPatchedTalkgroups(connection, id, event.getPatchedTalkgroups());
                    return id;
                }
            }
        }

        throw new SQLException("Database didn't return a generated id for the inserted event");
    }

    @Override
    public void insertBatch(Connection connection, List<EventRecord> events) throws SQLException
    {
        List<EventRecord> patched = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(getInsertStatement()))
        {
            for(EventRecord event: events)
            {
                //Events with patched talkgroups need their generated id for the patched talkgroup rows
                if(!event.getPatchedTalkgroups().isEmpty())
                {
                    patched.add(event);
                    continue;
                }

                bind(statement, event);
                statement.addBatch();
            }

            statement.executeBatch();
        }

        for(EventRecord event: patched)
        {
            insert(connection, event);
        }
    }

    @Override
    public boolean update(Connection connection, EventRecord event) throws SQLException
    {
        String assignments = String.join(" = ?, ", COLUMNS.split(", ")) + " = ?";

        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mTable + " SET " + assignments +
            " WHERE id = ?"))
        {
            bind(statement, event);
            statement.setLong(COLUMN_COUNT + 1, event.getId());

            if(statement.executeUpdate() == 0)
            {
                return false;
            }
        }

        if(IdentifierRecord.FORM_PATCH_GROUP.equals(event.getToForm()) || !event.getPatchedTalkgroups().isEmpty())
        {
            try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mPatchedTable +
                " WHERE event_id = ?"))
            {
                statement.setLong(1, event.getId());
                statement.executeUpdate();
            }

            insertPatchedTalkgroups(connection, event.getId(), event.getPatchedTalkgroups());
        }

        return true;
    }

    private void insertPatchedTalkgroups(Connection connection, long eventId, List<PatchedTalkgroup> talkgroups)
        throws SQLException
    {
        if(talkgroups.isEmpty())
        {
            return;
        }

        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mPatchedTable +
            " (event_id, talkgroup, fq) VALUES (?, ?, ?)"))
        {
            for(PatchedTalkgroup talkgroup: talkgroups)
            {
                statement.setLong(1, eventId);
                statement.setLong(2, talkgroup.value());
                setString(statement, 3, talkgroup.fullyQualified(), LENGTH_ID);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    /**
     * Loads the patched talkgroups of the patch group events in the list.
     */
    private void loadPatchedTalkgroups(Connection connection, List<EventRecord> events) throws SQLException
    {
        Map<Long,EventRecord> patchEvents = new HashMap<>();

        for(EventRecord event: events)
        {
            if(IdentifierRecord.FORM_PATCH_GROUP.equals(event.getToForm()))
            {
                patchEvents.put(event.getId(), event);
            }
        }

        if(patchEvents.isEmpty())
        {
            return;
        }

        Map<Long,List<PatchedTalkgroup>> talkgroups = new HashMap<>();
        String placeholders = String.join(", ", Collections.nCopies(patchEvents.size(), "?"));

        try(PreparedStatement statement = connection.prepareStatement("SELECT event_id, talkgroup, fq FROM " +
            mPatchedTable + " WHERE event_id IN (" + placeholders + ") ORDER BY event_id, talkgroup"))
        {
            int index = 1;

            for(Long id: patchEvents.keySet())
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
            patchEvents.get(entry.getKey()).setPatchedTalkgroups(entry.getValue());
        }
    }

    /**
     * Binds the event column values to statement parameters 1 - COLUMN_COUNT.
     */
    private static void bind(PreparedStatement statement, EventRecord event) throws SQLException
    {
        int index = 1;
        statement.setLong(index++, event.getStartTime());
        statement.setLong(index++, event.getEndTime());
        statement.setLong(index++, event.getDuration());
        statement.setString(index++, event.getEventTypeValue().name());
        setString(statement, index++, event.getProtocol(), LENGTH_SHORT);
        setString(statement, index++, event.getSystem(), LENGTH_NAME);
        setString(statement, index++, event.getSite(), LENGTH_NAME);
        setString(statement, index++, event.getChannel(), LENGTH_NAME);
        setString(statement, index++, event.getChannelDescriptor(), LENGTH_NAME);
        statement.setLong(index++, event.getFrequency());
        statement.setInt(index++, event.getTimeslot());
        setString(statement, index++, event.getToId(), LENGTH_NAME);
        setLong(statement, index++, event.getToValue());
        setString(statement, index++, event.getToForm(), LENGTH_SHORT);
        setString(statement, index++, event.getToFullyQualified(), LENGTH_ID);
        setString(statement, index++, event.getFromId(), LENGTH_NAME);
        setLong(statement, index++, event.getFromValue());
        setString(statement, index++, event.getFromForm(), LENGTH_SHORT);
        setString(statement, index++, event.getFromFullyQualified(), LENGTH_ID);
        setString(statement, index++, event.getAliasList(), LENGTH_NAME);
        setString(statement, index++, event.getTalkerAlias(), LENGTH_NAME);
        setString(statement, index, event.getDetails(), LENGTH_DETAILS);
    }

    /**
     * Creates an event record from the current result set row.
     */
    private static EventRecord read(ResultSet rs) throws SQLException
    {
        EventRecord event = new EventRecord();
        event.setId(rs.getLong("id"));
        event.setStartTime(rs.getLong("start_time"));
        event.setEndTime(rs.getLong("end_time"));
        event.setDuration(rs.getLong("duration_ms"));
        event.setEventType(rs.getString("event_type"));
        event.setProtocol(rs.getString("protocol"));
        event.setSystem(rs.getString("system_name"));
        event.setSite(rs.getString("site_name"));
        event.setChannel(rs.getString("channel_name"));
        event.setChannelDescriptor(rs.getString("channel_descriptor"));
        event.setFrequency(rs.getLong("frequency"));
        event.setTimeslot(rs.getInt("timeslot"));
        event.setToId(rs.getString("to_id"));
        event.setToValue(getLong(rs, "to_value"));
        event.setToForm(rs.getString("to_form"));
        event.setToFullyQualified(rs.getString("to_fq"));
        event.setFromId(rs.getString("from_id"));
        event.setFromValue(getLong(rs, "from_value"));
        event.setFromForm(rs.getString("from_form"));
        event.setFromFullyQualified(rs.getString("from_fq"));
        event.setAliasList(rs.getString("alias_list"));
        event.setTalkerAlias(rs.getString("talker_alias"));
        event.setDetails(rs.getString("details"));
        return event;
    }

    private List<EventRecord> query(Connection connection, String sql, List<Object> parameters) throws SQLException
    {
        List<EventRecord> events = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            bindParameters(statement, parameters);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    events.add(read(rs));
                }
            }
        }

        loadPatchedTalkgroups(connection, events);
        return events;
    }

    @Override
    public List<EventRecord> search(Connection connection, EventSearchCriteria criteria, int offset, int limit)
        throws SQLException
    {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + " c" + getWhereClause(criteria, parameters) +
            " ORDER BY start_time DESC, id DESC LIMIT ? OFFSET ?";
        parameters.add(limit);
        parameters.add(offset);
        return query(connection, sql, parameters);
    }

    @Override
    public long count(Connection connection, EventSearchCriteria criteria) throws SQLException
    {
        List<Object> parameters = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + mTable + " c" +
            getWhereClause(criteria, parameters)))
        {
            bindParameters(statement, parameters);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    @Override
    public long count(Connection connection) throws SQLException
    {
        return count(connection, EventSearchCriteria.all());
    }

    /**
     * Builds the WHERE clause (with a leading space) for the criteria and adds the parameter values to the list.
     */
    private String getWhereClause(EventSearchCriteria criteria, List<Object> parameters)
    {
        List<String> clauses = new ArrayList<>();

        if(!criteria.talkgroup().isEmpty())
        {
            clauses.add(getIdentifierClause("to", criteria.talkgroup(), TO_TALKGROUP_FORMS, mPatchedTable,
                "event_id", parameters));
        }

        if(!criteria.radio().isEmpty())
        {
            //A radio can be the FROM identifier or the TO identifier (e.g. unit-to-unit call, page, radio check)
            String from = getIdentifierClause("from", criteria.radio(), FROM_RADIO_FORM, null, null, parameters);
            String to = getIdentifierClause("to", criteria.radio(), TO_RADIO_FORM, null, null, parameters);
            clauses.add("(" + from + " OR (" + TO_RADIO_FORM + " AND " + to + "))");
        }

        if(criteria.category() != null)
        {
            clauses.add(criteria.category().getWhereClause());
        }

        if(criteria.eventType() != null)
        {
            clauses.add("event_type = ?");
            parameters.add(criteria.eventType().name());
        }

        if(criteria.protocol() != null)
        {
            clauses.add("protocol = ?");
            parameters.add(criteria.protocol().name());
        }

        if(!criteria.system().isEmpty())
        {
            clauses.add("LOWER(system_name) LIKE ? ESCAPE '!'");
            parameters.add(getLikePattern(criteria.system()));
        }

        if(!criteria.details().isEmpty())
        {
            clauses.add("LOWER(details) LIKE ? ESCAPE '!'");
            parameters.add(getLikePattern(criteria.details()));
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

    @Override
    public List<EventRecord> readPage(Connection connection, long afterId, int limit) throws SQLException
    {
        List<Object> parameters = new ArrayList<>();
        parameters.add(afterId);
        parameters.add(limit);
        return query(connection, "SELECT id, " + COLUMNS + " FROM " + mTable + " WHERE id > ? ORDER BY id LIMIT ?",
            parameters);
    }

    @Override
    public Set<String> getTransferKeys(Connection connection) throws SQLException
    {
        Set<String> keys = new HashSet<>();
        List<EventRecord> page = readPage(connection, 0, 5000);

        while(!page.isEmpty())
        {
            for(EventRecord event: page)
            {
                keys.add(event.getTransferKey());
            }

            page = readPage(connection, page.getLast().getId(), 5000);
        }

        return keys;
    }

    @Override
    public int deleteStartedBefore(Connection connection, long startedBefore) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mTable +
            " WHERE start_time < ?"))
        {
            statement.setLong(1, startedBefore);
            return statement.executeUpdate();
        }
    }

    @Override
    public int deleteAll(Connection connection) throws SQLException
    {
        try(PreparedStatement patched = connection.prepareStatement("DELETE FROM " + mPatchedTable);
            PreparedStatement events = connection.prepareStatement("DELETE FROM " + mTable))
        {
            patched.executeUpdate();
            return events.executeUpdate();
        }
    }

    @Override
    public String getSchema()
    {
        return mSchema;
    }
}
