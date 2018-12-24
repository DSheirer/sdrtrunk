/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.module.decode.mpt1327.identifier;

/**
 * MPT-1327 Ident Types enumeration
 */
public enum IdentType
{
    ALLI_ALL_CALL,
    CUSTOM_IDENT,
    DIVERTI_DIVERT_IDENT,
    DNI_DATA_NETWORK_GATEWAY_IDENT,
    DUMMI,
    INCI_INCLUDE_IDENT,
    IPFIXI_INTER_PREFIX_IDENT,
    PABXI_PABX_GATEWAY_IDENT,
    PSTNGI_PSTN_GATEWAY_IDENT,
    PSTNSI_AND_NETSI_SHORT_FORM_IDENT,
    REGI_REGISTRATION_IDENT,
    RESERVED,
    SDMI_SHORT_DATA_MESSAGE,
    TSC_TRUNKING_SYSTEM_CONTROLLER,
    USER_IDENT,
    UNKNOWN;

//        User idents 1 - 8100(individual and group idents)

    public static IdentType fromIdent(int ident)
    {
        if(1 <= ident && ident <= 8100)
        {
            return USER_IDENT;
        }

        switch(ident)
        {
            case 0:
                return DUMMI;
            case 8101:
                return PSTNGI_PSTN_GATEWAY_IDENT;
            case 8102:
                return PABXI_PABX_GATEWAY_IDENT;
            case 8103:
                return DNI_DATA_NETWORK_GATEWAY_IDENT;
            case 8185:
                return REGI_REGISTRATION_IDENT;
            case 8186:
                return INCI_INCLUDE_IDENT;
            case 8187:
                return DIVERTI_DIVERT_IDENT;
            case 8188:
                return SDMI_SHORT_DATA_MESSAGE;
            case 8189:
                return IPFIXI_INTER_PREFIX_IDENT;
            case 8190:
                return TSC_TRUNKING_SYSTEM_CONTROLLER;
            case 8191:
                return ALLI_ALL_CALL;
        }

        if(8136 <= ident && ident <= 8180)
        {
            return CUSTOM_IDENT;
        }
        else if(8121 <= ident && ident <= 8135)
        {
            return PSTNSI_AND_NETSI_SHORT_FORM_IDENT;
        }
        else if((8181 <= ident && ident <= 8184) || (8104 <= ident && ident <= 8120))
        {
            return RESERVED;
        }

        return UNKNOWN;
    }
}
