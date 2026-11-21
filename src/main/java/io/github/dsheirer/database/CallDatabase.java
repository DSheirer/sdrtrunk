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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.dsheirer.database.call.CallRepository;
import io.github.dsheirer.database.call.ICallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An open connection pool to one database and schema, with the sdrtrunk schema installation logic.
 */
public class CallDatabase implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CallDatabase.class);

    /**
     * Version of the sdrtrunk tables that this release creates.  Increment and add an upgrade step to
     * {@link #getUpgradeStatements(int)} when the tables change.
     */
    public static final int SCHEMA_VERSION = 2;
    public static final String SCHEMA_INFO_TABLE = "schema_info";

    private final DatabaseConfiguration mConfiguration;
    private final HikariDataSource mDataSource;
    private final ICallRepository mCallRepository;

    /**
     * Opens a connection pool for the configuration.  The schema isn't created or checked.
     * @param configuration for the database
     * @param databaseDirectory for the built-in database files
     * @throws SQLException if the database can't be reached
     */
    public CallDatabase(DatabaseConfiguration configuration, Path databaseDirectory) throws SQLException
    {
        mConfiguration = configuration;
        mCallRepository = new CallRepository(configuration.schema());

        if(configuration.type() == DatabaseType.H2)
        {
            try
            {
                Files.createDirectories(databaseDirectory);
            }
            catch(Exception e)
            {
                throw new SQLException("Unable to create the database directory [" + databaseDirectory + "]", e);
            }
        }

        try
        {
            Class.forName(configuration.type().getDialect().getDriverClass());
        }
        catch(ClassNotFoundException cnfe)
        {
            throw new SQLException("Database driver isn't available: " + configuration.type().getDialect().getDriverClass(), cnfe);
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("sdrtrunk-db-" + configuration.type().name().toLowerCase(Locale.ROOT));
        config.setJdbcUrl(configuration.getJdbcUrl(databaseDirectory));
        config.setDriverClassName(configuration.type().getDialect().getDriverClass());

        if(configuration.type().isNetwork())
        {
            config.setUsername(configuration.user());
            config.setPassword(configuration.password());
        }
        else
        {
            config.setUsername("sa");
            config.setPassword("");
        }

        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(5_000);
        //Fail immediately when the database can't be reached, so the user gets an answer
        config.setInitializationFailTimeout(1);

        try
        {
            mDataSource = new HikariDataSource(config);
        }
        catch(Exception e)
        {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new SQLException("Unable to connect to " + configuration.getDescription() + ": " +
                cause.getMessage(), cause);
        }
    }

    /**
     * Configuration for this database
     */
    public DatabaseConfiguration getConfiguration()
    {
        return mConfiguration;
    }

    /**
     * Repository for the calls table in this schema
     */
    public ICallRepository getCallRepository()
    {
        return mCallRepository;
    }

    /**
     * Pooled connection.  Close it to return it to the pool.
     */
    public Connection getConnection() throws SQLException
    {
        return mDataSource.getConnection();
    }

    /**
     * Determines whether the configured schema exists and whether it holds sdrtrunk tables.
     */
    public SchemaStatus getSchemaStatus() throws SQLException
    {
        try(Connection connection = getConnection())
        {
            return getSchemaStatus(connection, mConfiguration.schema());
        }
    }

    /**
     * Determines the status of the named schema.
     */
    public static SchemaStatus getSchemaStatus(Connection connection, String schema) throws SQLException
    {
        boolean schemaExists;

        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " +
            "information_schema.schemata WHERE LOWER(schema_name) = ?"))
        {
            statement.setString(1, schema.toLowerCase(Locale.ROOT));

            try(ResultSet rs = statement.executeQuery())
            {
                schemaExists = rs.next() && rs.getLong(1) > 0;
            }
        }

        if(!schemaExists)
        {
            return SchemaStatus.MISSING;
        }

        try(PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " +
            "information_schema.tables WHERE LOWER(table_schema) = ? AND LOWER(table_name) = ?"))
        {
            statement.setString(1, schema.toLowerCase(Locale.ROOT));
            statement.setString(2, SCHEMA_INFO_TABLE);

            try(ResultSet rs = statement.executeQuery())
            {
                return rs.next() && rs.getLong(1) > 0 ? SchemaStatus.SDRTRUNK : SchemaStatus.EMPTY;
            }
        }
    }

    /**
     * Number of calls stored in the schema, or 0 if the schema doesn't have sdrtrunk tables.
     */
    public long getCallCount() throws SQLException
    {
        if(getSchemaStatus() != SchemaStatus.SDRTRUNK)
        {
            return 0;
        }

        try(Connection connection = getConnection())
        {
            return mCallRepository.count(connection);
        }
    }

    /**
     * Creates the schema and tables when they don't exist and upgrades the tables of an older version.
     * @throws SQLException on error, or if the schema was created by a newer version of sdrtrunk
     */
    public void install() throws SQLException
    {
        DatabaseType type = mConfiguration.type();
        String schema = mConfiguration.schema();

        try(Connection connection = getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute(type.getDialect().getCreateSchemaStatement(schema));
            statement.execute("CREATE TABLE IF NOT EXISTS " + schema + "." + SCHEMA_INFO_TABLE +
                " (version INT NOT NULL)");

            int version = getVersion(connection);

            if(version > SCHEMA_VERSION)
            {
                throw new SQLException("Schema [" + schema + "] was created by a newer version of sdrtrunk (schema " +
                    "version " + version + ").  This version supports schema version " + SCHEMA_VERSION + ".");
            }

            if(version < SCHEMA_VERSION)
            {
                for(String sql: getUpgradeStatements(version))
                {
                    statement.execute(sql);
                }

                if(version == 0)
                {
                    statement.executeUpdate("INSERT INTO " + schema + "." + SCHEMA_INFO_TABLE +
                        " (version) VALUES (" + SCHEMA_VERSION + ")");
                }
                else
                {
                    statement.executeUpdate("UPDATE " + schema + "." + SCHEMA_INFO_TABLE + " SET version = " +
                        SCHEMA_VERSION);
                }

                LOGGER.info("Installed sdrtrunk schema version " + SCHEMA_VERSION + " in " +
                    mConfiguration.getDescription() + (version > 0 ? " (upgraded from version " + version + ")" : ""));
            }
        }
    }

    /**
     * Statements that bring the tables from the version argument up to the current version.
     */
    private List<String> getUpgradeStatements(int fromVersion)
    {
        List<String> statements = new ArrayList<>();

        if(fromVersion < 1)
        {
            statements.addAll(CallRepository.getCreateStatements(mConfiguration.type().getDialect(), mConfiguration.schema()));
        }

        //Version 2: talkgroups patched into patch group calls
        if(fromVersion < 2)
        {
            statements.addAll(CallRepository.getPatchedTalkgroupCreateStatements(mConfiguration.schema()));
        }

        return statements;
    }

    /**
     * Installed schema version, or 0 when nothing is installed.
     */
    private int getVersion(Connection connection) throws SQLException
    {
        try(Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT MAX(version) FROM " + mConfiguration.schema() + "." +
                SCHEMA_INFO_TABLE))
        {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Drops the schema with all of its tables and then installs an empty sdrtrunk schema.
     */
    public void clear() throws SQLException
    {
        try(Connection connection = getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute(mConfiguration.type().getDialect().getDropSchemaStatement(mConfiguration.schema()));
        }

        install();
    }

    /**
     * Suggests an unused schema name derived from the configured schema name (e.g. sdrtrunk_2).
     */
    public String suggestSchemaName() throws SQLException
    {
        try(Connection connection = getConnection())
        {
            String base = mConfiguration.schema();

            for(int x = 2; x < 1000; x++)
            {
                String candidate = base + "_" + x;

                if(candidate.length() <= 63 && getSchemaStatus(connection, candidate) == SchemaStatus.MISSING)
                {
                    return candidate;
                }
            }
        }

        return mConfiguration.schema() + "_new";
    }

    /**
     * Indicates if the named schema exists in this database.
     */
    public boolean schemaExists(String schema) throws SQLException
    {
        try(Connection connection = getConnection())
        {
            return getSchemaStatus(connection, schema) != SchemaStatus.MISSING;
        }
    }

    @Override
    public void close()
    {
        if(!mDataSource.isClosed())
        {
            mDataSource.close();
        }
    }
}
