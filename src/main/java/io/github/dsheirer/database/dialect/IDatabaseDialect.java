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
package io.github.dsheirer.database.dialect;

import io.github.dsheirer.database.DatabaseConfiguration;

import java.nio.file.Path;

/**
 * Per-engine connection settings and SQL differences for a call database.  Everything else that sdrtrunk sends to
 * the database is portable SQL shared by all engines.
 *
 * All supported engines support schemas, so sdrtrunk always qualifies its tables with the configured schema name.
 */
public interface IDatabaseDialect
{
    /**
     * JDBC driver class name
     */
    String getDriverClass();

    /**
     * Default TCP port for a network database server, or 0 for an embedded database.
     */
    int getDefaultPort();

    /**
     * Indicates if this is a network database server that needs a host, port and credentials.
     */
    boolean isNetwork();

    /**
     * Indicates if the engine needs a database name in addition to the schema name.
     */
    boolean requiresDatabaseName();

    /**
     * JDBC URL for the configuration
     * @param configuration with the host, port and database name
     * @param databaseDirectory for embedded database files
     */
    String getJdbcUrl(DatabaseConfiguration configuration, Path databaseDirectory);

    /**
     * Column definition for an auto-generated 64-bit primary key.
     */
    String getIdentityColumn();

    /**
     * Column type for a large text value (for example a serialized playlist item), up to several megabytes.
     */
    String getLargeTextColumn();

    /**
     * Statement that creates the schema when it doesn't exist.
     */
    default String getCreateSchemaStatement(String schema)
    {
        return "CREATE SCHEMA IF NOT EXISTS " + schema;
    }

    /**
     * Statement that removes the schema and all of its tables.
     */
    String getDropSchemaStatement(String schema);
}
