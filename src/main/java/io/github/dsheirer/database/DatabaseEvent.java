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

/**
 * Call database status change notification
 *
 * @param type of event
 * @param configuration of the current database
 * @param errorMessage for an ERROR event, otherwise null
 */
public record DatabaseEvent(Type type, DatabaseConfiguration configuration, String errorMessage)
{
    public enum Type
    {
        /**
         * Database opened, or a different database was selected
         */
        CONNECTED,
        /**
         * Database couldn't be opened or the connection was lost
         */
        ERROR,
        /**
         * Stored calls changed outside of the live call updates, for example by an age-off rule
         */
        CALLS_CHANGED
    }
}
