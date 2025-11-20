/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
 * Call control options field.
 * @param value from the field.
 */
public record CallControlOption(int value)
{
    private static final int MASK_EMERGENCY = 0x80;
    private static final int MASK_LOCATION_ID = 0x40;
    private static final int MASK_PRIORITY_PAGING = 0x20;
    private static final int MASK_SUPPLEMENTARY_PROCEDURES = 0x07;

    /**
     * Emergency indicator.
     *
     * @return true if emergency message or false if normal message.
     */
    public boolean isEmergency()
    {
        return (value & MASK_EMERGENCY) == MASK_EMERGENCY;
    }

    /**
     * Indicates if the message has a location ID field included.  Incoming call on CC by a visiting SU will
     * include the location ID in the message.
     *
     * @return location ID included in the message
     */
    public boolean hasLocationId()
    {
        return (value & MASK_LOCATION_ID) == MASK_LOCATION_ID;
    }

    /**
     * Indicates if this is priority or normal paging.
     */
    public boolean isPriorityPaging()
    {
        return (value & MASK_PRIORITY_PAGING) == MASK_PRIORITY_PAGING;
    }

    /**
     * Value for supplementary procedures.
     *
     * @return value.
     */
    public int getMaskSupplementaryProceduresValue()
    {
        return value & MASK_SUPPLEMENTARY_PROCEDURES;
    }
}
