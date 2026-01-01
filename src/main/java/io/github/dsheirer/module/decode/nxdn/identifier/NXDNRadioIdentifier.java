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

package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * NXDN Radio Identifier
 */
public class NXDNRadioIdentifier extends RadioIdentifier
{
    public NXDNRadioIdentifier(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    @Override
    public String toString()
    {
        return switch(getValue())
        {
            case 0xFFF0 -> "0xFFF0 TRUNKING CONTROLLER";
            case 0xFFF1 -> "0xFFF1 PSTN ID";
            case 0xFFF2 -> "0xFFF2 SPECIAL ID";
            case 0xFFF3 -> "0xFFF3 SPECIAL ID";
            case 0xFFF4 -> "0xFFF4 SPECIAL ID";
            case 0xFFF5 -> "0xFFF5 CONVENTIONAL PSTN ID";
            case 0xFFFF -> "0xFFFF ALL UNITS";
            default -> super.toString();
        };
    }

    /**
     * Creates an NXDN radio identifier with the FROM role.
     * @param value of the radio ID
     * @return identifier
     */
    public static NXDNRadioIdentifier createFrom(int value)
    {
        return new NXDNRadioIdentifier(value, Role.FROM);
    }

    /**
     * Creates an NXDN radio identifier with the TO role.
     * @param value of the radio ID
     * @return identifier
     */
    public static NXDNRadioIdentifier createTo(int value)
    {
        return new NXDNRadioIdentifier(value, Role.TO);
    }

    /**
     * Creates an NXDN radio identifier with the ANY role.
     * @param value of the radio ID
     * @return identifier
     */
    public static NXDNRadioIdentifier createAny(int value)
    {
        return new NXDNRadioIdentifier(value, Role.ANY);
    }
}
