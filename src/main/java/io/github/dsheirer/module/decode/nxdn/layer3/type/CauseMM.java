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
 * Cause values for Mobility Management (MM)
 */
public enum CauseMM
{
    REGISTRATION_ACCEPTED(0x01, "REGISTRATION:ACCEPTED"),
    LOCATION_ACCEPTED_GROUP_FAILED(0x04, "LOCATION:ACCEPTED GROUP:FAILED"),
    LOCATION_ACCEPTED_GROUP_REFUSED(0x05, "LOCATION:ACCEPTED GROUP:REFUSED"),
    REGISTRATION_FAILED(0x06, "REGISTRATION:FAILED"),
    REGISTRATION_REFUSED(0x08, "REGISTRATION:REFUSED"),
    REGISTRATION_ACCEPTED_VALID_ESN(0x21, "REGISTRATION:ACCEPTED ESN:VALID"),
    LOCATION_ACCEPTED_GROUP_FAILED_VALID_ESN(0x24, "LOCATION:ACCEPTED GROUP:FAILED ESN:VALID"),
    LOCATION_ACCEPTED_GROUP_REFUSED_VALID_ESN(0x25, "LOCATION:ACCEPTED GROUP:REFUSED ESN:VALID"),
    REGISTRATION_REFUSED_VALID_ESN(0x28, "REGISTRATION:REFUSED ESN:VALID"),
    NETWORK_FAILURE(0x51, "NETWORK FAILURE"),
    TEMPORARY_FAILURE(0x52, "TEMPORARY FAILURE"),
    EQUIPMENT_CONGESTION(0x53, "EQUIPMENT CONGESTION"),
    RESOURCE_UNAVAILABLE(0x5F, "RESOURCE UNAVAILABLE"),
    SERVICE_UNAVAILABLE(0x60, "SERVICE UNAVAILABLE"),
    SERVICE_UNAVAILABLE_OR_UNSUPPORTED(0x6F, "SERVICE UNAVAILABLE OR UNSUPPORTED"),
    MISSING_MANDATORY_INFORMATION_ELEMENTS(0x70, "MISSING MANDATORY INFORMATION ELEMENTS"),
    UNDEFINED_INFORMATION_ELEMENT_OR_INVALID_CONTENTS(0x71, "UNDEFINED INFORMATION ELEMENT OR INVALID CONTENTS"),
    PROCEDURE_ERROR(0x7F, "PROCEDURE ERROR"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    /**
     * Constructs an instance
     * @param value for the entry
     * @param label to display
     */
    CauseMM(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Transmitted value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Display label.
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the matching entry.
     *
     * @param value to look up
     * @return matching entry or UNKNOWN
     */
    public static CauseMM fromValue(int value)
    {
        for(CauseMM cause : CauseMM.values())
        {
            if(cause.getValue() == value)
            {
                return cause;
            }
        }

        return UNKNOWN;
    }
}
