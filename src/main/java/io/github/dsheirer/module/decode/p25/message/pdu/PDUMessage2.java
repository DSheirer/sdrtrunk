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
package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class PDUMessage2 extends P25Message implements IBitErrorProvider
{
    private int mNAC;
    private PDUHeader mHeader;

    public PDUMessage2(CorrectedBinaryMessage originalMessage, long timestamp, int nac, PDUHeader pduHeader)
    {
        super(originalMessage, DataUnitID.PACKET_HEADER_DATA_UNIT, null, timestamp);
        mNAC = nac;
        mHeader = pduHeader;
    }

    public PDUHeader getHeader()
    {
        return mHeader;
    }

    @Override
    public int getBitsProcessedCount()
    {
        //TODO: add bits processed from data blocks too.
        return mHeader.getBitsProcessedCount();
    }

    @Override
    public int getBitErrorsCount()
    {
        //TODO: add bit errors from data blocks too
        return mHeader.getBitErrorsCount();
    }

    public String toString()
    {
        return getMessage();
    }

    @Override
    public boolean isValid()
    {
        return mHeader.isValid();
    }

    @Override
    public String getErrorStatus()
    {
        return null;
    }

    @Override
    public String getMessage()
    {
        return "PDU ...";
    }

    @Override
    public String getBinaryMessage()
    {
        return null;
    }

    @Override
    public String getProtocol()
    {
        return null;
    }

    @Override
    public String getEventType()
    {
        return null;
    }

    @Override
    public String getFromID()
    {
        return null;
    }

    @Override
    public Alias getFromIDAlias()
    {
        return null;
    }

    @Override
    public String getToID()
    {
        return null;
    }

    @Override
    public Alias getToIDAlias()
    {
        return null;
    }
}
