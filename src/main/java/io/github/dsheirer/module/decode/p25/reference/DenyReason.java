/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.reference;

public enum DenyReason
{
    RESERVED(0x00),
    REQUESTING_UNIT_NOT_VALID(0x10),
    REQUESTING_UNIT_NOT_AUTHORIZED_FOR_SERVICE(0x11),
    TARGET_UNIT_NOT_VALID(0x20),
    TARGET_UNIT_NOT_AUTHORIZED_FOR_SERVICE(0x21),
    TARGET_UNIT_REFUSED_CALL(0x2F),
    TARGET_GROUP_NOT_VALID(0x30),
    TARGET_GROUP_NOT_AUTHORIZED_FOR_SERVICE(0x31),
    INVALID_DIALING(0x40),
    TELEPHONE_NUMBER_NOT_AUTHORIZED(0x41),
    PSTN_NOT_VALID(0x42),
    CALL_TIMEOUT(0x50),
    LANDLINE_TERMINATED_CALL(0x51),
    SUBSCRIBER_UNIT_TERMINATED_CALL(0x52),
    CALL_PREEMPTED(0x5F),
    SITE_ACCESS_DENIAL(0x60),
    USER_OR_SYSTEM_DEFINED(0x61), //0x61 - 0xEF
    PTT_COLLIDE(0x67),
    PTT_BONK(0x77),
    CALL_OPTIONS_NOT_VALID_FOR_SERVICE(0xF0),
    PROTECTION_SERVICE_OPTION_NOT_VALID(0xF1),
    DUPLEX_SERVICE_OPTION_NOT_VALID(0xF2),
    CIRCUIT_OR_PACKET_MODE_OPTION_NOT_VALID(0xF3),
    SYSTEM_DOES_NOT_SUPPORT_SERVICE(0xFF),
    SECURE_REQUEST_ON_CLEAR_SUPERGROUP(0x00), //Custom: Motorola & L3Harris
    CLEAR_REQUEST_ON_SECURE_SUPERGROUP(0x01), //Custom: Motorola & L3Harris
    UNKNOWN(-1);

    private int mCode;

    DenyReason(int code)
    {
        mCode = code;
    }

    /**
     * Utility method to lookup the entry from the code that is custom for a vendor.
     * @param code to lookup
     * @param vendor identity
     * @return matching enum entry or UNKNOWN.
     */
    public static DenyReason fromCustomCode(int code, Vendor vendor)
    {
        switch(vendor)
        {
            case MOTOROLA:
            case HARRIS:
                if(code == 0x00)
                {
                    return SECURE_REQUEST_ON_CLEAR_SUPERGROUP;
                }
                else if(code == 0x01)
                {
                    return CLEAR_REQUEST_ON_SECURE_SUPERGROUP;
                }
                //Deliberate fall-through
            default:
                return fromCode(code);
        }
    }

    /**
     * Utility method to lookup the entry from the code.
     * @param code to lookup
     * @return matching enum entry or UNKNOWN.
     */
    public static DenyReason fromCode(int code)
    {
        if(code == 0x10)
        {
            return DenyReason.REQUESTING_UNIT_NOT_VALID;
        }
        else if(code == 0x11)
        {
            return REQUESTING_UNIT_NOT_AUTHORIZED_FOR_SERVICE;
        }
        else if(code == 0x20)
        {
            return DenyReason.TARGET_UNIT_NOT_VALID;
        }
        else if(code == 0x21)
        {
            return TARGET_UNIT_NOT_AUTHORIZED_FOR_SERVICE;
        }
        else if(code == 0x2F)
        {
            return DenyReason.TARGET_UNIT_REFUSED_CALL;
        }
        else if(code == 0x30)
        {
            return TARGET_GROUP_NOT_VALID;
        }
        else if(code == 0x31)
        {
            return DenyReason.TARGET_GROUP_NOT_AUTHORIZED_FOR_SERVICE;
        }
        else if(code == 0x40)
        {
            return DenyReason.INVALID_DIALING;
        }
        else if(code == 0x41)
        {
            return DenyReason.TELEPHONE_NUMBER_NOT_AUTHORIZED;
        }
        else if(code == 0x42)
        {
            return DenyReason.PSTN_NOT_VALID;
        }
        else if(code == 0x50)
        {
            return CALL_TIMEOUT;
        }
        else if(code == 0x51)
        {
            return LANDLINE_TERMINATED_CALL;
        }
        else if(code == 0x52)
        {
            return SUBSCRIBER_UNIT_TERMINATED_CALL;
        }
        else if(code == 0x5F)
        {
            return DenyReason.CALL_PREEMPTED;
        }
        else if(code == 0x60)
        {
            return SITE_ACCESS_DENIAL;
        }
        else if(code == 0xF0)
        {
            return CALL_OPTIONS_NOT_VALID_FOR_SERVICE;
        }
        else if(code == 0xF1)
        {
            return PROTECTION_SERVICE_OPTION_NOT_VALID;
        }
        else if(code == 0xF2)
        {
            return DenyReason.DUPLEX_SERVICE_OPTION_NOT_VALID;
        }
        else if(code == 0xF3)
        {
            return CIRCUIT_OR_PACKET_MODE_OPTION_NOT_VALID;
        }
        else if(code == 0x67)
        {
            return PTT_COLLIDE;
        }
        else if(code == 0x77)
        {
            return PTT_BONK;
        }
        else if(code <= 0x5E)
        {
            return DenyReason.RESERVED;
        }
        else if(code >= 0x61)
        {
            return DenyReason.USER_OR_SYSTEM_DEFINED;
        }

        return UNKNOWN;
    }
}
