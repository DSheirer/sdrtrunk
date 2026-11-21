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

import io.github.dsheirer.database.dialect.H2Dialect;
import io.github.dsheirer.database.dialect.IDatabaseDialect;
import io.github.dsheirer.database.dialect.MariaDBDialect;
import io.github.dsheirer.database.dialect.PostgreSQLDialect;

/**
 * Supported call database engines.  The engine differences are implemented by each type's {@link IDatabaseDialect}.
 */
public enum DatabaseType
{
    H2("Built-in (H2)", new H2Dialect()),
    MARIADB("MariaDB", new MariaDBDialect()),
    POSTGRESQL("PostgreSQL", new PostgreSQLDialect());

    private final String mLabel;
    private final IDatabaseDialect mDialect;

    DatabaseType(String label, IDatabaseDialect dialect)
    {
        mLabel = label;
        mDialect = dialect;
    }

    /**
     * SQL dialect and connection settings for this engine
     */
    public IDatabaseDialect getDialect()
    {
        return mDialect;
    }

    /**
     * Indicates if this is a network database server that needs a host, port and credentials.
     */
    public boolean isNetwork()
    {
        return mDialect.isNetwork();
    }

    /**
     * Indicates if this engine needs a database name in addition to the schema name.
     */
    public boolean requiresDatabaseName()
    {
        return mDialect.requiresDatabaseName();
    }

    /**
     * Default TCP port for a network database, or 0 for the embedded database.
     */
    public int getDefaultPort()
    {
        return mDialect.getDefaultPort();
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
