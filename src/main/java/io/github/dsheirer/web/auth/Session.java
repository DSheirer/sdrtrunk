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

/**
 * Browser session, held in memory.  Sessions end when the application restarts.
 *
 * @param id random session id, sent in the session cookie
 * @param userId account that signed in
 * @param username at sign-in, checked again on each request so that a session can't move to a different account
 * after a database switch reuses the account id
 * @param csrfToken that state-changing requests must send in the X-CSRF-Token header
 * @param createdTime epoch milliseconds
 * @param address remote address at sign-in
 */
public record Session(String id, long userId, String username, String csrfToken, long createdTime, String address)
{
}
