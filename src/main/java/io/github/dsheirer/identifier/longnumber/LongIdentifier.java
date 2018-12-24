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

package io.github.dsheirer.identifier.longnumber;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;

import java.util.Objects;

/**
 * Long identifier base class.
 */
public abstract class LongIdentifier extends Identifier<Long>
{
    public LongIdentifier(Long value, IdentifierClass identifierClass, Form form, Role role)
    {
        super(value, identifierClass, form, role);
    }


    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }

        LongIdentifier that = (LongIdentifier)o;

        return (getValue().longValue() == that.getValue().longValue()) &&
            getIdentifierClass() == that.getIdentifierClass() &&
            getForm() == that.getForm() &&
            getRole() == that.getRole();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getValue(), getIdentifierClass(), getForm(), getRole());
    }
}
