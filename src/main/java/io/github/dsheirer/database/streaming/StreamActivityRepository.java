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
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.database.dialect.IDatabaseDialect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.github.dsheirer.database.SqlSupport.bindParameters;
import static io.github.dsheirer.database.SqlSupport.getLikePattern;
import static io.github.dsheirer.database.SqlSupport.setString;

/**
 * JDBC implementation of the stream activity table: one row per call and stream that the call was selected for.
 * Portable SQL across the supported database engines.  All methods take a connection from the caller.
 */
public class StreamActivityRepository
{
    public static final String TABLE = "stream_activity";

    private static final String COLUMNS = "call_key, stream_name, server_type, call_start, duration_ms, to_id, " +
        "from_id, alias_list, selecting_aliases, protocol, system_name, site_name, channel_name, status, detail, " +
        "status_time";
    private static final int COLUMN_COUNT = 16;
    private static final int LENGTH_SHORT = 48;
    private static final int LENGTH_NAME = 255;
    static final int LENGTH_ALIASES = 4000;
    static final int LENGTH_DETAIL = 1000;
    private static final String ALIAS_SEPARATOR = "\t";
    private static final String LINE_SEPARATOR = "\n";

    private final String mSchema;
    private final String mTable;

    /**
     * Constructs an instance
     * @param schema that holds the table
     */
    public StreamActivityRepository(String schema)
    {
        mSchema = schema;
        mTable = schema + "." + TABLE;
    }

    /**
     * Statements that create the stream activity table and its indexes.
     */
    public static List<String> getCreateStatements(IDatabaseDialect dialect, String schema)
    {
        String table = schema + "." + TABLE;
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS " + table + " (" +
            "id " + dialect.getIdentityColumn() + ", " +
            "call_key BIGINT NOT NULL, " +
            "stream_name VARCHAR(" + LENGTH_NAME + ") NOT NULL, " +
            "server_type VARCHAR(" + LENGTH_SHORT + "), " +
            "call_start BIGINT NOT NULL, " +
            "duration_ms BIGINT NOT NULL, " +
            "to_id VARCHAR(" + LENGTH_NAME + "), " +
            "from_id VARCHAR(" + LENGTH_NAME + "), " +
            "alias_list VARCHAR(" + LENGTH_NAME + "), " +
            "selecting_aliases VARCHAR(" + LENGTH_ALIASES + "), " +
            "protocol VARCHAR(" + LENGTH_SHORT + "), " +
            "system_name VARCHAR(" + LENGTH_NAME + "), " +
            "site_name VARCHAR(" + LENGTH_NAME + "), " +
            "channel_name VARCHAR(" + LENGTH_NAME + "), " +
            "status VARCHAR(" + LENGTH_SHORT + ") NOT NULL, " +
            "detail VARCHAR(" + LENGTH_DETAIL + "), " +
            "status_time BIGINT NOT NULL)");
        statements.add("CREATE INDEX IF NOT EXISTS stream_activity_call_start_idx ON " + table + " (call_start)");
        statements.add("CREATE INDEX IF NOT EXISTS stream_activity_stream_idx ON " + table +
            " (stream_name, call_start)");
        statements.add("CREATE INDEX IF NOT EXISTS stream_activity_status_idx ON " + table + " (status, call_start)");
        return statements;
    }

    public long insert(Connection connection, StreamActivityRecord record) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO " + mTable + " (" + COLUMNS +
            ") VALUES (" + "?, ".repeat(COLUMN_COUNT - 1) + "?)", new String[]{"id"}))
        {
            bind(statement, record);
            statement.executeUpdate();

            try(ResultSet keys = statement.getGeneratedKeys())
            {
                if(keys.next())
                {
                    record.setId(keys.getLong(1));
                    return record.getId();
                }
            }
        }

        throw new SQLException("Database didn't return a generated id for the inserted stream activity");
    }

    /**
     * Updates the row of the record
     * @return true if the row exists
     */
    public boolean update(Connection connection, StreamActivityRecord record) throws SQLException
    {
        String assignments = String.join(" = ?, ", COLUMNS.split(", ")) + " = ?";

        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mTable + " SET " + assignments +
            " WHERE id = ?"))
        {
            bind(statement, record);
            statement.setLong(COLUMN_COUNT + 1, record.getId());
            return statement.executeUpdate() > 0;
        }
    }

    private static void bind(PreparedStatement statement, StreamActivityRecord record) throws SQLException
    {
        int index = 1;
        statement.setLong(index++, record.getCallKey());
        setString(statement, index++, record.getStreamName() != null ? record.getStreamName() : "", LENGTH_NAME);
        setString(statement, index++, record.getServerType() != null ? record.getServerType().name() : null,
            LENGTH_SHORT);
        statement.setLong(index++, record.getCallStart());
        statement.setLong(index++, record.getDuration());
        setString(statement, index++, record.getTo(), LENGTH_NAME);
        setString(statement, index++, record.getFrom(), LENGTH_NAME);
        setString(statement, index++, record.getAliasList(), LENGTH_NAME);
        setString(statement, index++, encode(record.getSelectingAliases()), LENGTH_ALIASES);
        setString(statement, index++, record.getProtocol(), LENGTH_SHORT);
        setString(statement, index++, record.getSystem(), LENGTH_NAME);
        setString(statement, index++, record.getSite(), LENGTH_NAME);
        setString(statement, index++, record.getChannel(), LENGTH_NAME);
        statement.setString(index++, record.getStatus().name());
        setString(statement, index++, record.getDetail(), LENGTH_DETAIL);
        statement.setLong(index, record.getStatusTime());
    }

    private static StreamActivityRecord read(ResultSet rs) throws SQLException
    {
        StreamActivityRecord record = new StreamActivityRecord();
        record.setId(rs.getLong("id"));
        record.setCallKey(rs.getLong("call_key"));
        record.setStreamName(rs.getString("stream_name"));
        record.setServerType(parse(BroadcastServerType.class, rs.getString("server_type"), null));
        record.setCallStart(rs.getLong("call_start"));
        record.setDuration(rs.getLong("duration_ms"));
        record.setTo(rs.getString("to_id"));
        record.setFrom(rs.getString("from_id"));
        record.setAliasList(rs.getString("alias_list"));
        record.setSelectingAliases(decode(rs.getString("selecting_aliases")));
        record.setProtocol(rs.getString("protocol"));
        record.setSystem(rs.getString("system_name"));
        record.setSite(rs.getString("site_name"));
        record.setChannel(rs.getString("channel_name"));
        record.setStatus(parse(StreamStatus.class, rs.getString("status"), StreamStatus.ERROR));
        record.setDetail(rs.getString("detail"));
        record.setStatusTime(rs.getLong("status_time"));
        return record;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E defaultValue)
    {
        if(name != null)
        {
            try
            {
                return Enum.valueOf(type, name);
            }
            catch(IllegalArgumentException iae)
            {
                //Written by a newer version
            }
        }

        return defaultValue;
    }

    /**
     * Encodes the selecting aliases as one line per alias: name TAB identifier.  The lines that fit in the column are
     * kept whole, so that a long list is cut at an alias rather than in the middle of one.
     */
    static String encode(List<StreamActivity.SelectingAlias> aliases)
    {
        if(aliases == null || aliases.isEmpty())
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for(StreamActivity.SelectingAlias alias: aliases)
        {
            String line = clean(alias.alias()) + ALIAS_SEPARATOR + clean(alias.identifier());

            if(sb.length() + line.length() + 1 > LENGTH_ALIASES)
            {
                break;
            }

            if(!sb.isEmpty())
            {
                sb.append(LINE_SEPARATOR);
            }

            sb.append(line);
        }

        return sb.toString();
    }

    static List<StreamActivity.SelectingAlias> decode(String value)
    {
        if(value == null || value.isEmpty())
        {
            return List.of();
        }

        List<StreamActivity.SelectingAlias> aliases = new ArrayList<>();

        for(String line: value.split(LINE_SEPARATOR))
        {
            int separator = line.indexOf(ALIAS_SEPARATOR);
            String name = separator >= 0 ? line.substring(0, separator) : line;
            String identifier = separator >= 0 ? line.substring(separator + 1) : "";
            aliases.add(new StreamActivity.SelectingAlias(name, identifier.isEmpty() ? null : identifier));
        }

        return aliases;
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Rows that match the criteria, newest first.
     */
    public List<StreamActivityRecord> search(Connection connection, StreamActivitySearchCriteria criteria, int offset,
                                             int limit) throws SQLException
    {
        List<Object> parameters = new ArrayList<>();
        String sql = "SELECT id, " + COLUMNS + " FROM " + mTable + getWhereClause(criteria, parameters) +
            " ORDER BY call_start DESC, id DESC LIMIT ? OFFSET ?";
        parameters.add(limit);
        parameters.add(offset);
        List<StreamActivityRecord> records = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(sql))
        {
            bindParameters(statement, parameters);

            try(ResultSet rs = statement.executeQuery())
            {
                while(rs.next())
                {
                    records.add(read(rs));
                }
            }
        }

        return records;
    }

    /**
     * Number of rows that match the criteria
     */
    public long count(Connection connection, StreamActivitySearchCriteria criteria) throws SQLException
    {
        List<Object> parameters = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + mTable +
            getWhereClause(criteria, parameters)))
        {
            bindParameters(statement, parameters);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    public long count(Connection connection) throws SQLException
    {
        return count(connection, StreamActivitySearchCriteria.all());
    }

    /**
     * Names of the streams that have rows, sorted
     */
    public List<String> getStreamNames(Connection connection) throws SQLException
    {
        List<String> names = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement("SELECT DISTINCT stream_name FROM " + mTable +
            " ORDER BY stream_name"); ResultSet rs = statement.executeQuery())
        {
            while(rs.next())
            {
                names.add(rs.getString(1));
            }
        }

        return names;
    }

    private static String getWhereClause(StreamActivitySearchCriteria criteria, List<Object> parameters)
    {
        List<String> clauses = new ArrayList<>();

        if(criteria.streamName() != null)
        {
            clauses.add("stream_name = ?");
            parameters.add(criteria.streamName());
        }

        if(!criteria.statuses().isEmpty())
        {
            List<String> placeholders = new ArrayList<>();

            for(StreamStatus status: criteria.statuses())
            {
                placeholders.add("?");
                parameters.add(status.name());
            }

            clauses.add("status IN (" + String.join(", ", placeholders) + ")");
        }

        if(!criteria.text().isEmpty())
        {
            List<String> likes = new ArrayList<>();

            for(String column: new String[]{"to_id", "from_id", "selecting_aliases", "detail", "stream_name",
                "alias_list", "system_name", "channel_name"})
            {
                likes.add("LOWER(" + column + ") LIKE ? ESCAPE '!'");
                parameters.add(getLikePattern(criteria.text()));
            }

            clauses.add("(" + String.join(" OR ", likes) + ")");
        }

        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    /**
     * Marks the rows that were still pending or in progress when the application stopped (status changed before the
     * time) as not sent.
     * @return number of rows updated
     */
    public int markInterrupted(Connection connection, long before, String detail) throws SQLException
    {
        List<String> placeholders = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        parameters.add(StreamStatus.NOT_SENT.name());
        parameters.add(detail);
        parameters.add(System.currentTimeMillis());

        for(StreamStatus status: StreamStatus.values())
        {
            if(!status.isFinal())
            {
                placeholders.add("?");
                parameters.add(status.name());
            }
        }

        parameters.add(before);

        try(PreparedStatement statement = connection.prepareStatement("UPDATE " + mTable +
            " SET status = ?, detail = ?, status_time = ? WHERE status IN (" + String.join(", ", placeholders) +
            ") AND status_time < ?"))
        {
            bindParameters(statement, parameters);
            return statement.executeUpdate();
        }
    }

    /**
     * Deletes the rows of calls that started before the time
     * @return number of rows deleted
     */
    public int deleteStartedBefore(Connection connection, long startedBefore) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement("DELETE FROM " + mTable +
            " WHERE call_start < ?"))
        {
            statement.setLong(1, startedBefore);
            return statement.executeUpdate();
        }
    }

    public String getSchema()
    {
        return mSchema;
    }
}
