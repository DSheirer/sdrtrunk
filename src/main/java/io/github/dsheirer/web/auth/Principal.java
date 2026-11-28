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

package io.github.dsheirer.web.auth;

import java.util.EnumSet;
import java.util.Set;

/**
 * Who is making a web API request, resolved by the {@link AccessManager} from the session cookie or the bearer token.
 *
 * @param userId account id
 * @param username sign-in name
 * @param displayName for the web client, or null
 * @param role of the account
 * @param permissions effective permissions: the role's permissions, limited further for an API token with a reduced
 * permission set, and empty while the account must change its password
 * @param method how the request was authenticated
 * @param sessionId browser session id, or null for an API token
 * @param tokenId API token id, or 0 for a browser session
 * @param csrfToken that state-changing requests in this browser session must send, or null for an API token
 * @param mustChangePassword true until the temporary password is changed
 */
public record Principal(long userId, String username, String displayName, Role role, Set<Permission> permissions,
                        Method method, String sessionId, long tokenId, String csrfToken, boolean mustChangePassword)
{
    /**
     * The sdrtrunk desktop application's settings dialog, which administers accounts without signing in.  Its
     * changes are recorded in the audit log as made by 'desktop'.
     */
    public static final Principal DESKTOP = new Principal(0, "desktop", "sdrtrunk desktop", Role.ADMIN,
        Set.copyOf(EnumSet.copyOf(Role.ADMIN.getPermissions())), Method.DESKTOP, null, 0, null, false);

    /**
     * Indicates if the request has the permission
     */
    public boolean has(Permission permission)
    {
        return permission == Permission.PUBLIC || permission == Permission.AUTHENTICATED ||
            permissions.contains(permission);
    }

    /**
     * How the request was authenticated
     */
    public enum Method
    {
        SESSION,
        TOKEN,
        DESKTOP
    }
}
