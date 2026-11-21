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

package io.github.dsheirer.database.account;

/**
 * Web API token for scripts and integrations.  Only a hash of the token is stored.
 *
 * @param id database id, or 0 before the token is stored
 * @param userId owning account
 * @param name label chosen by the owner
 * @param tokenHash hex SHA-256 of the token
 * @param prefix first characters of the token, so the owner can recognize it in a list
 * @param permissions comma separated permission keys that limit the token, or null for all of the owner's permissions
 * @param createdTime epoch milliseconds
 * @param expiresTime epoch milliseconds after which the token is refused, or null for no expiry
 * @param lastUsedTime epoch milliseconds of the last use (updated at most once a minute), or null
 */
public record ApiTokenRecord(long id, long userId, String name, String tokenHash, String prefix, String permissions,
                             long createdTime, Long expiresTime, Long lastUsedTime)
{
    /**
     * Indicates if the token has expired at the time argument
     */
    public boolean isExpired(long now)
    {
        return expiresTime != null && expiresTime <= now;
    }
}
