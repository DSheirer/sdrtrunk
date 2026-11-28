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
 * Web API audit log entry: a sign-in, a refused request or an administrative change.
 *
 * @param id database id, or 0 before the entry is stored
 * @param time epoch milliseconds
 * @param username account that made the request, or the name given in a failed sign-in, or null
 * @param action what was done, for example login, login.failed, user.create
 * @param target what it was done to, or null
 * @param success false for a refused or failed request
 * @param address remote address of the request, or null
 * @param detail extra text, or null
 */
public record AuditRecord(long id, long time, String username, String action, String target, boolean success,
                          String address, String detail)
{
}
