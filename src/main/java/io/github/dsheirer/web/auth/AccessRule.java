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

import io.javalin.security.RouteRole;

/**
 * Route markers that adjust the access check, declared next to the route's {@link Permission}.
 */
public enum AccessRule implements RouteRole
{
    /**
     * The endpoint stays available while the account must change its temporary password: reading the account,
     * changing the password and signing out.  Every other endpoint is refused until the password is changed.
     */
    ALLOW_PENDING_PASSWORD_CHANGE
}
