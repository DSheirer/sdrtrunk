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
import java.text.DecimalFormat;

/**
 * NXDN Radio Identifier
 */
public class NXDNRadioIdentifier extends RadioIdentifier
{
    private static final DecimalFormat REPEATER_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat RADIO_FORMAT = new DecimalFormat("0000");
    private boolean mTypeD = false;

    /**
     * Constructs an instance
     * @param value for the ID
     * @param role played by the ID
     */
    public NXDNRadioIdentifier(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    /**
     * Indicates if the identifier is for a Type-D system.
     */
    public boolean isTypeD()
    {
        return mTypeD;
    }

    /**
     * Home repeater value for a Type-D identifier.
     */
    public int getTypeDHomeRepeater()
    {
        return (getValue() >> 11) & 0x1F;
    }

    /**
     * Radio ID for a Type-D identifier.
     * @return radio ID.
     */
    public int getTypeDRadio()
    {
        return getValue() & 0x7FF;
    }

    /**
     * Sets the Type-D flag for this ID.
     * @param typeD true to indicate that this is a Type-D identifier.
     */
    public void setTypeD(boolean typeD)
    {
        mTypeD = typeD;
    }

    @Override
    public String toString()
    {
        if(isTypeD())
        {
            return REPEATER_FORMAT.format(getTypeDHomeRepeater()) + "-" + RADIO_FORMAT.format(getTypeDRadio());
        }
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

    /**
     * Creates a Type-D radio identifier with the FROM role.
     * @param radio value that combines the home repeater and the radio
     * @return identifier
     */
    public static NXDNRadioIdentifier createTypeDFrom(int radio)
    {
        NXDNRadioIdentifier identifier = new NXDNRadioIdentifier(radio, Role.FROM);
        identifier.setTypeD(true);
        return identifier;
    }

    /**
     * Creates a Type-D radio identifier with the TO role.
     * @param radio value that combines the home repeater and the radio
     * @return identifier
     */
    public static NXDNRadioIdentifier createTypeDTo(int radio)
    {
        NXDNRadioIdentifier identifier = new NXDNRadioIdentifier(radio, Role.TO);
        identifier.setTypeD(true);
        return identifier;
    }
    /**
     * Creates a Type-D radio identifier with the ANY role.
     * @param radio value that combines the home repeater and the radio
     * @return identifier
     */
    public static NXDNRadioIdentifier createTypeDAny(int radio)
    {
        NXDNRadioIdentifier identifier = new NXDNRadioIdentifier(radio, Role.ANY);
        identifier.setTypeD(true);
        return identifier;
    }
}
