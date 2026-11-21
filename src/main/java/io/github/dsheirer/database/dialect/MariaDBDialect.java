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
 * MariaDB network database server.  In MariaDB a schema is a database.
 */
public class MariaDBDialect implements IDatabaseDialect
{
    @Override
    public String getDriverClass()
    {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public int getDefaultPort()
    {
        return 3306;
    }

    @Override
    public boolean isNetwork()
    {
        return true;
    }

    @Override
    public boolean requiresDatabaseName()
    {
        return false;
    }

    @Override
    public String getJdbcUrl(DatabaseConfiguration configuration, Path databaseDirectory)
    {
        return "jdbc:mariadb://" + configuration.host() + ":" + configuration.port() + "/";
    }

    @Override
    public String getIdentityColumn()
    {
        return "BIGINT AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    public String getDropSchemaStatement(String schema)
    {
        //Dropping a MariaDB schema (database) drops all of its tables
        return "DROP SCHEMA IF EXISTS " + schema;
    }
}
