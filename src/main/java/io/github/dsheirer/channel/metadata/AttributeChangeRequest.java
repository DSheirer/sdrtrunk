/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.module.decode.DecoderType;

public class AttributeChangeRequest<T>
{
    private Attribute mAttribute;
    private T mValue;
    private Alias mAlias;

    /**
     * Request to change the value of an attribute
     * @param attribute to change
     * @param value to change for the attribute
     * @param alias (optional) for the value
     */
    public AttributeChangeRequest(Attribute attribute, T value, Alias alias)
    {
        mAttribute = attribute;
        mValue = value;
        mAlias = alias;
    }

    /**
     * Request to change the value of an attribute
     * @param attribute to change
     * @param value to change for the attribute
     */
    public AttributeChangeRequest(Attribute attribute, T value)
    {
        this(attribute, value, null);
    }

    /**
     * Attribute to change
     */
    public Attribute getAttribute()
    {
        return mAttribute;
    }

    /**
     * Value to set for the attribute
     */
    public T getValue()
    {
        return mValue;
    }

    /**
     * Indicates if this change request has a non-null value for the attribute
     */
    public boolean hasValue()
    {
        return mValue != null;
    }

    /**
     * Alias for the corresponding attribute value
     */
    public Alias getAlias()
    {
        return mAlias;
    }

    /**
     * Indicates if this change request contains an alias that corresponds to the attribute value
     */
    public boolean hasAlias()
    {
        return mAlias != null;
    }

    /**
     * Returns the value as a String or null if the value is not a String instance.
     */
    public String getStringValue()
    {
        if(mValue instanceof String)
        {
            return (String)mValue;
        }

        return null;
    }

    /**
     * Returns the value as a Long or null if the value is not a Long instance.
     */
    public Long getLongValue()
    {
        if(mValue instanceof Long)
        {
            return (Long)mValue;
        }

        return null;
    }

    public Boolean getBooleanValue()
    {
        if(mValue instanceof Boolean)
        {
            return (Boolean)mValue;
        }

        return false;
    }

    /**
     * Returns the value as a DecoderType or null if the value is not a DecoderType instance.
     */
    public DecoderType getDecoderTypeValue()
    {
        if(mValue instanceof DecoderType)
        {
            return (DecoderType)mValue;
        }

        return null;
    }
}
