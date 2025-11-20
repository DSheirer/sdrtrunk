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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Group registration option
 */
public class GroupRegistrationOption extends Option
{
    private static final int MASK_EMERGENCY = 0x80;
    private static final int MASK_VISITOR = 0x40;

    /**
     * Constructs an instance
     *
     * @param value for the field
     */
    public GroupRegistrationOption(int value)
    {
        super(value);
    }

    /**
     * Indicates if the emergency flag is set
     */
    public boolean isEmergency()
    {
        return (mValue & MASK_EMERGENCY) == MASK_EMERGENCY;
    }

    /**
     * Indicates if the registrant is a priority station
     */
    public boolean isVisitor()
    {
        return (mValue & MASK_VISITOR) == MASK_VISITOR;
    }

    /**
     * Indicates if this registration includes the visitor's home location ID
     */
    public boolean hasLocationID()
    {
        return isVisitor();
    }
}
