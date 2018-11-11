/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.identifier.talkgroup;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;

public abstract class TalkgroupIdentifier extends Identifier<Integer>
{
    private boolean mIsGroup;

    public TalkgroupIdentifier(Integer value, Role role, boolean isGroup)
    {
        super(value, IdentifierClass.USER, Form.TALKGROUP, role);
        mIsGroup = isGroup;
    }

    @Override
    public boolean isValid()
    {
        return getValue() > 0;
    }

    /**
     * Indicates if this is a group or individual identifier
     */
    public boolean isGroup()
    {
        return mIsGroup;
    }
}
