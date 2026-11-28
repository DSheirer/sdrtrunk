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
 * Web API user account.
 *
 * @param id database id, or 0 before the account is stored
 * @param username unique sign-in name, stored in lower case
 * @param displayName shown in the web client, or null
 * @param role name of the account's role
 * @param passwordHash encoded password hash (see the web PasswordHasher)
 * @param mustChangePassword true when the password is temporary and must be changed at the next sign-in
 * @param enabled false when the account can't sign in
 * @param createdTime epoch milliseconds
 * @param lastLoginTime epoch milliseconds of the last successful sign-in, or null
 * @param failedLogins consecutive failed sign-ins since the last success
 * @param lockedUntil epoch milliseconds until which sign-in is refused, or null
 * @param pendingApproval true for an account that a user requested from the web page and that an administrator
 * hasn't approved yet.  Such an account is also disabled.
 * @param lastWebTime epoch milliseconds of the last request from a signed-in browser, or null
 * @param lastApiTime epoch milliseconds of the last request with one of the account's API tokens, or null
 */
public record UserRecord(long id, String username, String displayName, String role, String passwordHash,
                         boolean mustChangePassword, boolean enabled, long createdTime, Long lastLoginTime,
                         int failedLogins, Long lockedUntil, boolean pendingApproval, Long lastWebTime,
                         Long lastApiTime)
{
    /**
     * New account that an administrator creates, or that a user requests when pendingApproval is true
     */
    public static UserRecord create(String username, String displayName, String role, String passwordHash,
                                    boolean mustChangePassword, boolean enabled, long createdTime,
                                    boolean pendingApproval)
    {
        return new UserRecord(0, username, displayName, role, passwordHash, mustChangePassword, enabled, createdTime,
            null, 0, null, pendingApproval, null, null);
    }

    public UserRecord withId(long newId)
    {
        return new UserRecord(newId, username, displayName, role, passwordHash, mustChangePassword, enabled,
            createdTime, lastLoginTime, failedLogins, lockedUntil, pendingApproval, lastWebTime, lastApiTime);
    }

    public UserRecord withPassword(String newPasswordHash, boolean newMustChangePassword)
    {
        return new UserRecord(id, username, displayName, role, newPasswordHash, newMustChangePassword, enabled,
            createdTime, lastLoginTime, 0, null, pendingApproval, lastWebTime, lastApiTime);
    }

    public UserRecord withLoginSuccess(long time)
    {
        return new UserRecord(id, username, displayName, role, passwordHash, mustChangePassword, enabled,
            createdTime, time, 0, null, pendingApproval, lastWebTime, lastApiTime);
    }

    public UserRecord withLoginFailure(int newFailedLogins, Long newLockedUntil)
    {
        return new UserRecord(id, username, displayName, role, passwordHash, mustChangePassword, enabled,
            createdTime, lastLoginTime, newFailedLogins, newLockedUntil, pendingApproval, lastWebTime, lastApiTime);
    }

    public UserRecord withDetails(String newDisplayName, String newRole, boolean newEnabled)
    {
        return new UserRecord(id, username, newDisplayName, newRole, passwordHash, mustChangePassword, newEnabled,
            createdTime, lastLoginTime, failedLogins, newEnabled ? lockedUntil : null, pendingApproval && !newEnabled,
            lastWebTime, lastApiTime);
    }

    /**
     * Approved account: enabled with the role chosen by the administrator
     */
    public UserRecord withApproval(String newRole)
    {
        return new UserRecord(id, username, displayName, newRole, passwordHash, mustChangePassword, true,
            createdTime, lastLoginTime, 0, null, false, lastWebTime, lastApiTime);
    }

    /**
     * Most recent of the last browser and API token access times, or null when the account was never used
     */
    public Long lastAccessTime()
    {
        if(lastWebTime == null)
        {
            return lastApiTime;
        }

        return lastApiTime == null ? lastWebTime : Math.max(lastWebTime, lastApiTime);
    }
}
