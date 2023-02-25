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

package io.github.dsheirer.module.decode.ip.mototrbo.ars;

import io.github.dsheirer.bits.BinaryMessage;

public class ARSHeaderFactory
{
    public static ARSHeader create(ARSPDUType pduType, BinaryMessage message, int offset)
    {
        switch(pduType)
        {
            case DEVICE_REGISTRATION:
                return new DeviceRegistration(message, offset);
            case DEVICE_DEREGISTRATION:
                return new DeviceDeRegistration(message, offset);
            case QUERY:
                return new QueryMessage(message, offset);
            case USER_REGISTRATION:
                return new UserRegistration(message, offset);
            case USER_DEREGISTRATION:
                return new UserDeRegistration(message, offset);
            case USER_REGISTRATION_ACKNOWLEDGEMENT:
                return new UserRegistrationAcknowledge(message, offset);
            case REGISTRATION_ACKNOWLEDGEMENT:
                return new RegistrationAcknowledgement(message, offset);
            case UNKNOWN:
            default:
                return new UnknownARSHeader(message, offset);
        }
    }
}
