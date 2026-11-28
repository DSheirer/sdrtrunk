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

/**
 * A talkgroup that is patched into the patch group of a call.  The protocol is the protocol of the call.
 *
 * @param value of the talkgroup
 * @param fullyQualified address (wacn.system.id) of a fully qualified (roaming) talkgroup, or null
 */
public record PatchedTalkgroup(long value, String fullyQualified)
{
    @Override
    public String toString()
    {
        return fullyQualified != null ? value + "(" + fullyQualified + ")" : String.valueOf(value);
    }
}
