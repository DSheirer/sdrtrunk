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

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Connection settings for a call database.
 *
 * @param type of database engine
 * @param host name or address of a network database server (ignored for H2)
 * @param port of the network database server (ignored for H2)
 * @param database name to connect to (PostgreSQL only)
 * @param schema name that holds the sdrtrunk tables
 * @param user name for a network database server
 * @param password for a network database server
 */
public record DatabaseConfiguration(DatabaseType type, String host, int port, String database, String schema,
                                    String user, String password)
{
    public static final String DEFAULT_SCHEMA = "sdrtrunk";

    /**
     * Schema names are restricted to unquoted SQL identifiers so that they can be used in statements without quoting
     * and so that they mean the same thing across all engines (H2 upper-cases unquoted names, the others don't).
     */
    private static final Pattern SCHEMA_PATTERN = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    public DatabaseConfiguration
    {
        Objects.requireNonNull(type, "type");
        host = host == null ? "" : host.trim();
        database = database == null ? "" : database.trim();
        schema = schema == null || schema.isBlank() ? DEFAULT_SCHEMA : schema.trim().toLowerCase();
        user = user == null ? "" : user;
        password = password == null ? "" : password;
    }

    /**
     * Default configuration: the built-in H2 database with the default schema.
     */
    public static DatabaseConfiguration getDefault()
    {
        return new DatabaseConfiguration(DatabaseType.H2, "", 0, "", DEFAULT_SCHEMA, "", "");
    }

    /**
     * Copy of this configuration with a different schema name.
     */
    public DatabaseConfiguration withSchema(String schemaName)
    {
        return new DatabaseConfiguration(type, host, port, database, schemaName, user, password);
    }

    /**
     * Indicates if the schema name is a valid, unquoted identifier.
     */
    public static boolean isValidSchemaName(String schema)
    {
        return schema != null && SCHEMA_PATTERN.matcher(schema).matches();
    }

    /**
     * Validates this configuration.
     * @return null if valid, or a description of the problem.
     */
    public String validate()
    {
        if(!isValidSchemaName(schema))
        {
            return "Schema name must start with a letter and contain only lower case letters, digits and underscores " +
                "(max 63 characters)";
        }

        if(type.isNetwork())
        {
            if(host.isEmpty())
            {
                return "Host is required";
            }

            if(port <= 0 || port > 65535)
            {
                return "Port must be in the range 1 - 65535";
            }

            if(user.isEmpty())
            {
                return "User name is required";
            }
        }

        if(type.requiresDatabaseName() && database.isEmpty())
        {
            return "Database name is required";
        }

        return null;
    }

    /**
     * JDBC URL for this configuration.
     * @param databaseDirectory for the built-in H2 database files
     */
    public String getJdbcUrl(Path databaseDirectory)
    {
        return type.getDialect().getJdbcUrl(this, databaseDirectory);
    }

    /**
     * Short description for the user, without the password.
     */
    public String getDescription()
    {
        return switch(type)
        {
            case H2 -> type + " - schema [" + schema + "]";
            case MARIADB -> type + " " + user + "@" + host + ":" + port + " - schema [" + schema + "]";
            case POSTGRESQL -> type + " " + user + "@" + host + ":" + port + "/" + database + " - schema [" +
                schema + "]";
        };
    }

    /**
     * Indicates if this configuration refers to the same database and schema as the other configuration (ignoring
     * credentials).
     */
    public boolean isSameLocation(DatabaseConfiguration other)
    {
        return other != null && type == other.type && schema.equals(other.schema) &&
            (!type.isNetwork() || (host.equalsIgnoreCase(other.host) && port == other.port &&
                database.equals(other.database)));
    }

    @Override
    public String toString()
    {
        return getDescription();
    }
}
