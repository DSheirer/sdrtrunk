/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.pdu.confirmed;

public class SNDCPDeactivateTDSContext extends PDUConfirmedMessage
{
    public static final int[] NSAPI = {180, 181, 182, 183};
    public static final int[] DEACTIVATION_TYPE = {184, 185, 186, 187, 188, 189,
        190, 191};

    public SNDCPDeactivateTDSContext(PDUConfirmedMessage message)
    {
        super(null, null);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " PDUC LLID:" );
		sb.append( getLogicalLinkID() );
		sb.append( " REQUEST SNDCP PACKET DATA DEACTIVATE " );
		sb.append( getDeactivationType() );

	    return sb.toString();
    }

    /**
     * Network Service Access Point Identifier - up to 14 NSAPI's can be
     * allocated to the mobile with each NSAPI to be used for a specific
     * protocol layer.
     */
    public int getNSAPI()
    {
        return getMessage().getInt(NSAPI);
    }

    public String getDeactivationType()
    {
//        if(getPDUType() == PDUType.SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST)
//        {
//            return getMessage().getInt(DEACTIVATION_TYPE) == 0 ?
//                "ALL NSAPIS" : "THIS NSAPI";
//        }

        return null;
    }
}
