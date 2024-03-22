/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.identifier.talkgroup;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;

/**
 * Talkgroup identifier.
 *
 * Note: this class overrides the .equals method to ensure that all talkgroups and subclasses can be compared using
 * the talkgroup value, regardless if it is a simple talkgroup or a fully qualified talkgroup identifier.
 */
public abstract class TalkgroupIdentifier extends IntegerIdentifier
{
    /**
     * Constructs an instance
     * @param value for the talkgroup
     * @param role for the talkgroup
     */
    public TalkgroupIdentifier(Integer value, Role role)
    {
        super(value, IdentifierClass.USER, Form.TALKGROUP, role);
    }

    @Override
    public boolean isValid()
    {
        return getValue() > 0;
    }

    /**
     * Overrides to compare just the talkgroup value, class, form, role and protocol.  This allows a fully qualified
     * talkgroup identifier to be equivalent to a standard talkgroup identifier for the traffic channel manager.
     */
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }

        if(o instanceof TalkgroupIdentifier tg)
        {
            return (getValue().intValue() == tg.getValue().intValue()) &&
                    getIdentifierClass() == tg.getIdentifierClass() &&
                    getForm() == tg.getForm() &&
                    getRole() == tg.getRole() &&
                    getProtocol() == tg.getProtocol();
        }

        return false;
    }
}
