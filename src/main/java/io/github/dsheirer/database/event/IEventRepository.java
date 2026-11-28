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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Storage operations for decode events in one database schema.  Each method uses the connection from the caller so
 * that the caller controls pooling and transactions.
 */
public interface IEventRepository
{
    /**
     * Inserts the event and assigns the generated row id to the event.
     * @return generated row id
     */
    long insert(Connection connection, EventRecord event) throws SQLException;

    /**
     * Inserts the events in a batch without retrieving the generated ids.
     */
    void insertBatch(Connection connection, List<EventRecord> events) throws SQLException;

    /**
     * Updates the existing event row identified by the event's id.
     * @return true if the row exists and was updated
     */
    boolean update(Connection connection, EventRecord event) throws SQLException;

    /**
     * Searches for events matching the criteria, newest first.
     * @param offset of the first row to return
     * @param limit maximum rows to return
     */
    List<EventRecord> search(Connection connection, EventSearchCriteria criteria, int offset, int limit)
        throws SQLException;

    /**
     * Count of events matching the criteria
     */
    long count(Connection connection, EventSearchCriteria criteria) throws SQLException;

    /**
     * Total number of event rows
     */
    long count(Connection connection) throws SQLException;

    /**
     * Reads a page of events in id order, for transferring events between databases.
     * @param afterId only return events with an id greater than this value
     * @param limit maximum number of events
     */
    List<EventRecord> readPage(Connection connection, long afterId, int limit) throws SQLException;

    /**
     * Transfer keys (see {@link EventRecord#getTransferKey()}) of all events, for skipping events that already exist
     * when appending.
     */
    Set<String> getTransferKeys(Connection connection) throws SQLException;

    /**
     * Deletes the events that started before the time, for the event age-off rule.
     * @return count of events deleted
     */
    int deleteStartedBefore(Connection connection, long startedBefore) throws SQLException;

    /**
     * Deletes all events (and their patched talkgroups), for restoring a backup.
     * @return count of events deleted
     */
    int deleteAll(Connection connection) throws SQLException;

    /**
     * Schema name
     */
    String getSchema();

    /**
     * Reads one row by id.
     * @return the row, or empty when there is no row with the id
     */
    default java.util.Optional<EventRecord> find(Connection connection, long id) throws SQLException
    {
        if(id <= 0)
        {
            return java.util.Optional.empty();
        }

        List<EventRecord> page = readPage(connection, id - 1, 1);
        return page.isEmpty() || page.getFirst().getId() != id ? java.util.Optional.empty() :
            java.util.Optional.of(page.getFirst());
    }
}
