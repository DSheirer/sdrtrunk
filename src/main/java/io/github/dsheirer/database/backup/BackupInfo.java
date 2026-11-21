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
package io.github.dsheirer.database.backup;

import java.nio.file.Path;

/**
 * A backup file in the backup directory
 *
 * @param path of the backup file
 * @param created time of the backup, milliseconds since epoch (from the file name)
 * @param size of the file in bytes
 * @param tag optional tag from the file name (e.g. pre-restore), or null
 * @param metadata from the file, or null if the file can't be read
 */
public record BackupInfo(Path path, long created, long size, String tag, Metadata metadata)
{
    /**
     * Backup file contents summary
     *
     * @param format version of the backup file format
     * @param schemaVersion of the database tables the backup was made from
     * @param created time of the backup, milliseconds since epoch
     * @param source description of the database that was backed up
     * @param calls count of calls in the backup
     * @param events count of events in the backup
     * @param playlists count of playlists in the backup, or -1 for a backup made before playlists were stored in the
     * database
     */
    public record Metadata(int format, int schemaVersion, long created, String source, long calls, long events,
                           long playlists)
    {
        /**
         * Indicates if the backup holds the playlists
         */
        public boolean hasPlaylists()
        {
            return playlists >= 0;
        }
    }

    /**
     * Indicates if the file is a readable backup
     */
    public boolean isValid()
    {
        return metadata != null;
    }

    /**
     * Indicates if this backup was made automatically before restoring another backup
     */
    public boolean isPreRestore()
    {
        return BackupManager.TAG_PRE_RESTORE.equals(tag);
    }
}
