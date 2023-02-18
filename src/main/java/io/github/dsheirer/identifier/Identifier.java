/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.identifier;

import io.github.dsheirer.protocol.Protocol;
import java.util.Objects;

/**
 * Identifier base class.
 *
 * Note: Identifiers are intended to be immutable.
 */
public abstract class Identifier<T>
{
    private T mValue;
    private IdentifierClass mIdentifierClass;
    private Form mForm;
    private Role mRole;

    public Identifier(T value, IdentifierClass identifierClass, Form form, Role role)
    {
        mValue = value;
        mIdentifierClass = identifierClass;
        mForm = form;
        mRole = role;
    }

    public String debug()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CLASS:").append(getIdentifierClass());
        sb.append(" FORM:").append(getForm());
        sb.append(" VALUE:").append(getValue());
        sb.append(" VALUE CLASS:").append(getValue().getClass());
        sb.append(" CLASS:").append(getClass());
        return sb.toString();
    }

    /**
     * Validity test for the identifier.  This method always returns true unless a subclass
     * implementation overrides the value for a specific validity test implementation.
     */
    public boolean isValid()
    {
        return true;
    }

    /**
     * Value of the identifier
     */
    public T getValue()
    {
        return mValue;
    }

    /**
     * Allow subclass implementations to set or update the value after construction.
     * @param value to update or set.
     */
    protected void setValue(T value)
    {
        mValue = value;
    }

    /**
     * Class/type of identifier
     */
    public IdentifierClass getIdentifierClass()
    {
        return mIdentifierClass;
    }

    public Form getForm()
    {
        return mForm;
    }

    /**
     * Role of the identifier in the communications
     */
    public Role getRole()
    {
        return mRole;
    }

    /**
     * Protocol that produces this identifier
     */
    public abstract Protocol getProtocol();

    @Override
    public String toString()
    {
        return getValue().toString();
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
        Identifier<?> that = (Identifier<?>)o;
        return Objects.equals(getValue(), that.getValue()) &&
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
