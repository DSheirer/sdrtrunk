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
 * Call options for Type-D
 */
public class TypeDCallOption extends Option
{
    private static final int MASK_EMERGENCY = 0x40;
    private static final int MASK_MULTI_SITE = 0x20;
    private static final int MASK_PRIORITY = 0x10;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(isEmergency())
        {
            sb.append("EMERGENCY ");
        }
        if(isPriority())
        {
            sb.append("PRIORITY ");
        }
        if(isMultiSite())
        {
            sb.append("MULTI-SITE ");
        }
        else
        {
            sb.append("SINGLE-SITE ");
        }

        sb.append("CALL");

        return sb.toString();
    }

    /**
     * Constructs an instance
     * @param value for the field
     */
    public TypeDCallOption(int value)
    {
        super(value);
    }

    /**
     * Indicates if this is an emergency call.
     */
    public boolean isEmergency()
    {
        return isSet(MASK_EMERGENCY);
    }

    /**
     * Indicates if this is a multi-site trunked system or a single-site trunked system.
     * @return true for multi-site or false for single-site
     */
    public boolean isMultiSite()
    {
        return isSet(MASK_MULTI_SITE);
    }

    /**
     * Indicates if this is a priority call (true) or normal (false).
     * @return true for priority
     */
    public boolean isPriority()
    {
        return isSet(MASK_PRIORITY);
    }
}
