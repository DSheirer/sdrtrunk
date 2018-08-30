/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.identifier.integer;

import io.github.dsheirer.identifier.AbstractIdentifier;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.Role;

/**
 * Integer identifier implementation
 */
public abstract class AbstractIntegerIdentifier extends AbstractIdentifier
{
    private int mValue;

    public AbstractIntegerIdentifier(int value)
    {
        mValue = value;
    }

    /**
     * Default role for the identifier.
     *
     * @return default of ANY
     */
    @Override
    public Role getRole()
    {
        return Role.ANY;
    }

    /**
     * Integer value for this identifier
     */
    public int getValue()
    {
        return mValue;
    }

    public String toString()
    {
        return String.valueOf(getValue());
    }

    @Override
    public int compareTo(IIdentifier o)
    {
        if(o instanceof AbstractIntegerIdentifier)
        {
            AbstractIntegerIdentifier other = (AbstractIntegerIdentifier)o;

            if(other.getProtocol() == getProtocol())
            {
                if(other.getRole() == getRole())
                {
                    return Integer.compare(getValue(), other.getValue());
                }
                else
                {
                    return getRole().compareTo(other.getRole());
                }
            }
            else
            {
                return getProtocol().compareTo(other.getProtocol());
            }
        }

        return 0;
    }
}
