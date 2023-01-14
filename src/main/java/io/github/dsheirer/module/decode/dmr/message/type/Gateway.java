/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
 * DMR Gateway Identifiers
 *
 * See: TS 102 361.4 paragraph A.4
 */
public enum Gateway
{
    PSTNI(0xFFFEC0, "PSTN SERVICES - ALIGNED TIMING"),
    PABXI(0xFFFEC1, "PABX SERVICES - ALIGNED TIMING"),
    LINEI(0xFFFEC2, "LINE GATEWAY - ALIGNED TIMING"),
    IPI(0xFFFEC3, "IP GATEWAY - ALIGNED TIMING"),
    SUPLI(0xFFFEC4, "SUPPLEMENTARY DATA SERVICE"),
    SDMI(0xFFFEC5, "UDT SHORT DATA SERVICE"),
    REGI(0xFFFEC6, "REGISTRATION SERVICE"),
    MSI(0xFFFEC7, "CALL DIVERSION TO MS"),
    DIVERTI(0xFFFEC9, "CALL DIVERSION CANCEL"),
    TSI(0xFFFECA, "TRUNK SYSTEM CONTROLLER"),
    DISPATI(0xFFFECB, "SYSTEM DISPATCHER - ALIGNED TIMING"),
    STUNI(0xFFFECC, "STUN/REVIVE MS ID"),
    AUTHI(0xFFFECD, "AUTHENTICATION ID"),
    GPI(0xFFFECE, "CALL DIVERSION TO TALKGROUP"),
    KILLI(0xFFFECF, "KILL MS ID"),
    PSTNDI(0xFFFED0, "PSTN SERVICES - OFFSET TIMING"),
    PABXDI(0xFFFED1, "PABX SERVICES - OFFSET TIMING"),
    LINEDI(0xFFFED2, "LINE GATEWAY - OFFSET TIMING"),
    DISPATDI(0xFFFED3, "SYSTEM DISPATCHER - OFFSET TIMING"),
    ALLMIS(0xFFFED4, "ALL MS AND TALKGROUPS"),
    IPDI(0xFFFED5, "IP GATEWAY - OFFSET TIMING"),
    DGNAI(0xFFFED6, "DYNAMIC GROUP NUMBER ASSIGNMENT"),
    TATTSI(0xFFFED7, "TALKGROUP SUBSCRIBE/ATTACH SERVICE"),
    ALLMSIDL(0xFFFFFD, "ALL SITE MS AS TALKGROUP"),
    ALLMSIDZ(0xFFFFFE, "ALL SITE SUBSET MS AS TALKGROUP"),
    ALLMSID(0xFFFFFF, "ALL MS AS TALKGROUP"),
    UNKNOWN(-1, "UNKNOWN");

    private int mAddress;
    private String mLabel;

    /**
     * Constructs an instance
     * @param address of the entry
     * @param label for the entry
     */
    Gateway(int address, String label)
    {
        mAddress = address;
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Address of the gateway identifier
     */
    public int getAddress()
    {
        return mAddress;
    }

    /**
     * Utility method to lookup the gateway from a value.
     * @param value of the service type
     * @return type or UNKNOWN
     */
    public static Gateway fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return Gateway.values()[value];
        }

        return UNKNOWN;
    }
}
