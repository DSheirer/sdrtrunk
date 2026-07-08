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
 * Cause values for call control in status call, short data call, simultaneous data call and remote control.
 */
public enum CauseSS
{
    RECEIVE_SUCCESS(0x01, "RECEIVE SUCCESS"),
    SEND_SUCCESS(0x02, "SEND SUCCESS"),
    REQUEST_FULL_RETRY(0x08, "REQUEST FULL RETRY"),
    MEMORY_FULL(0x09, "MEMORY FULL"),
    ABORT(0x0A, "ABORT"),
    ACCEPTED(0x10, "ACCEPTED"),
    CALLED_GROUP_NOT_PERMITTED_FOR_SERVICE(0x11, "CALLED GROUP NOT PERMITTED FOR SERVICE"),
    CALLING_SU_NOT_PERMITTED_FOR_SERVICE(0x12, "CALLING RADIO NOT PERMITTED FOR SERVICE"),
    CALLED_SU_NOT_PERMITTED_FOR_SERVICE(0x13, "CALLED RADIO NOT PERMITTED FOR SERVICE"),
    CALLED_SU_NOT_REGISTERED(0x14, "CALLED RADIO NOT REGISTERED"),
    CALLED_SU_NO_RESPONSE(0x15, "CALLED RADIO NO RESPONSE"),
    INCOMING_CALL_REJECTION_FOR_CALLED_SU(0x16, "INCOMING CALL REJECTION FOR CALLED RADIO"),
    CALLED_SU_BUSY(0x18, "CALLED RADIO BUSY"),
    CALLED_GROUP_BUSY(0x19, "CALLED GROUP BUSY"),
    CALLING_SU_BUSY(0x1A, "CALLED RADIO BUSY"),
    SU_NOT_REGISTERED(0x1C, "RADIO NOT REGISTERED"),
    GROUP_NOT_REGISTERED(0x1D, "GROUP NOT REGISTERED"),
    CHANNEL_UNAVAILABLE(0x50, "CHANNEL UNAVAILABLE"),
    NETWORK_FAILURE(0x51, "NETWORK FAILURE"),
    TEMPORARY_FAILURE(0x52, "TEMPORARY FAILURE"),
    EQUIPMENT_CONGESTION(0x53, "EQUIPMENT CONGESTION"),
    RESOURCE_UNAVAILABLE(0x5F, "RESOURCE UNAVAILABLE"),
    SERVICE_UNAVAILABLE(0x60, "SERVICE UNAVAILABLE"),
    SERVICE_UNSUPPORTED(0x61, "SERVICE UNSUPPORTED"),
    SERVICE_UNAVAILABLE_OR_UNSUPPORTED(0x6F, "SERVICE UNAVAILABLE OR UNSUPPORTED"),
    MISSING_MANDATORY_INFORMATION_ELEMENTS(0x70, "MISSING MANDATORY INFORMATION ELEMENTS"),
    UNDEFINED_INFORMATION_ELEMENT_OR_INVALID_CONTENTS(0x71, "UNDEFINED INFORMATION ELEMENTS OR INVALID_CONTENTS"),
    PROCEDURE_ERROR(0x7F, "PROCEDURE ERROR"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    /**
     * Constructs an instance
     *
     * @param value for the entry
     * @param label to display
     */
    CauseSS(int value, String label)
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
    public static CauseSS fromValue(int value)
    {
        for(CauseSS cause : CauseSS.values())
        {
            if(cause.getValue() == value)
            {
                return cause;
            }
        }

        return UNKNOWN;
    }
}
