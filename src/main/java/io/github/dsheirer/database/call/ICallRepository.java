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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Storage operations for call audio events in one database schema.  Each method uses the connection from the caller
 * so that the caller controls pooling and transactions.
 */
public interface ICallRepository
{
    /**
     * Inserts the call and assigns the generated row id to the call.
     * @return generated row id
     */
    long insert(Connection connection, CallRecord call) throws SQLException;

    /**
     * Inserts the calls in a single batch without retrieving the generated ids.
     */
    void insertBatch(Connection connection, List<CallRecord> calls) throws SQLException;

    /**
     * Updates the existing call row identified by the call's id.
     * @return true if the row exists and was updated
     */
    boolean update(Connection connection, CallRecord call) throws SQLException;

    /**
     * Marks calls left in the active state by a previous session as interrupted.
     * @return count of calls updated
     */
    int markInterrupted(Connection connection) throws SQLException;

    /**
     * Deletes the calls that are in progress.  Used when switching databases: calls in progress continue in the new
     * database, so their rows are removed from the old database.
     * @return count of calls deleted
     */
    int deleteActive(Connection connection) throws SQLException;

    /**
     * Calls matching the criteria, newest first.
     */
    List<CallRecord> search(Connection connection, CallSearchCriteria criteria, int offset, int limit)
        throws SQLException;

    /**
     * Count of calls matching the criteria
     */
    long count(Connection connection, CallSearchCriteria criteria) throws SQLException;

    /**
     * Total number of calls
     */
    long count(Connection connection) throws SQLException;

    /**
     * Page of calls in id order with an id greater than the argument, for transfers between databases.
     */
    List<CallRecord> readPage(Connection connection, long afterId, int limit) throws SQLException;

    /**
     * Transfer keys of all calls (see {@link CallRecord#getTransferKey()})
     */
    Set<String> getTransferKeys(Connection connection) throws SQLException;

    /**
     * Calls with an available recording, oldest first.
     * @param startedBefore only calls that started before this time, or 0 for any time
     */
    List<CallRecord> getRecordedCalls(Connection connection, long startedBefore, int limit) throws SQLException;

    /**
     * Calls that are not in progress and started before the time, oldest first.
     */
    List<CallRecord> getCallsStartedBefore(Connection connection, long startedBefore, int limit) throws SQLException;

    /**
     * Completed calls that started before the time and are duplicates, or Do Not Monitor calls without an alias record
     * option, oldest first.
     */
    List<CallRecord> getDuplicateAndDoNotMonitorCalls(Connection connection, long startedBefore, int limit)
        throws SQLException;

    /**
     * Total size in bytes of all available recordings
     */
    long getRecordedFileSize(Connection connection) throws SQLException;

    /**
     * Deletes the call rows
     */
    void delete(Connection connection, List<Long> ids) throws SQLException;

    /**
     * Clears the recording file of the calls and marks their recordings deleted, keeping the call rows.
     */
    void markRecordingsDeleted(Connection connection, List<Long> ids) throws SQLException;

    /**
     * Schema that holds the calls table
     */
    String getSchema();
}
