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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Known DMR gateways and system identities defined in ETSI TS 102 361-4 Table A.4 Tier III Gateways/Identifiers
 */
public enum Tier3Gateway
{
    PSTNI(0xFFFEC0, "PSTN GATEWAY"),
    PABXI(0xFFFEC1, "PABX GATEWAY"),
    LINEI(0xFFFEC2, "LINE GATEWAY"),
    IPI(0xFFFEC3, "IP GATEWAY"),
    SUPLI(0xFFFEC4, "SUPPLEMENTARY DATA SERVICE"),
    SDMI(0xFFFEC5, "UDT SHORT DATA SERVICE"),
    REGI(0xFFFEC6, "REGISTRATION SERVICE"),
    MSI(0xFFFEC7, "CALL DIVERSION TO MS GATEWAY"),
    DIVERTI(0xFFFEC9, "CALL DIVERSION CANCELLATION"),
    TSI(0xFFFECA, "TRUNKING SYSTEM CONTROLLER"),
    DISPATI(0xFFFECB, "SYSTEM DISPATCHER"),
    STUNI(0xFFFECC, "MS STUN/REVIVE"),
    AUTHI(0xFFFECD, "AUTHENTICATION"),
    GPI(0xFFFECE, "CALL DIVERSION TO TALKGROUP GATEWAY"),
    KILLI(0xFFFECF, "MS KILL"),
    PSTNDI(0xFFFED0, "PSTN-D GATEWAY"),
    PABXDI(0xFFFED1, "PABX-D GATEWAY"),
    LINEDI(0xFFFED2, "LINE-D GATEWAY"),
    DISPATDI(0xFFFED3, "SYSTEM DISPATCHER-D"),
    ALLMSI(0xFFFED4, "ALL RADIOS/TALKGROUPS"),
    IPDI(0xFFFED5, "IP-D GATEWAY"),
    DGNAI(0xFFFED6, "DYNAMIC GROUP NUMBER ASSIGNMENT"),
    TATTSI(0xFFFED7, "TALKGROUP SUBSCRIBE/ATTACH SERVICE"),
    ALLMSIDL(0xFFFFFD, "ALL RADIOS AT SITE"),
    ALLMSIDZ(0xFFFFFE, "ALL RADIOS IN ZONE"),
    ALLMSID(0xFFFFFF, "ALL RADIOS IN SYSTEM"),
    RESERVED(0x0, "RESERVED");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the identifier
     * @param label to describe the identifier
     */
    Tier3Gateway(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Indicates if the radio is a known gateway identifier
     * @param value to test
     * @return true if the value is a known gateway identifier
     */
    public static boolean isGateway(int value)
    {
        if(value >= PSTNI.getValue())
        {
            for(Tier3Gateway gateway: Tier3Gateway.values())
            {
                if(gateway.matches(value))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Lookup the DMR Tier-III gateway from a value.
     * @param value of the radio identifier
     * @return the known gateway or RESERVED if the value doesn't match a known gateway.
     */
    public static Tier3Gateway fromValue(int value)
    {
        for(Tier3Gateway gateway: Tier3Gateway.values())
        {
            if(gateway.matches(value))
            {
                return gateway;
            }
        }

        return RESERVED;
    }

    /**
     * Indicates if this entry matches the specified value.
     * @param value to test for match
     * @return true if the value matches.
     */
    public boolean matches(int value)
    {
        return mValue == value;
    }

    /**
     * Numeric value for the entry.
     * @return value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Print friendly label for the value.
     * @return label
     */
    public String getLabel()
    {
        return mLabel;
    }
}
