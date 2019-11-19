/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.ISPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * SNDCP Data Channel Requenst
 */
public class SNDCPReconnectRequest extends ISPMessage
{
    private static final int[] DATA_SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] DATA_ACCESS_CONTROL = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int DATA_TO_SEND_FLAG = 40;
    private static final int[] RESERVED = {41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private DataServiceOptions mDataServiceOptions;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public SNDCPReconnectRequest(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(hasDataToSend() ? " HAS DATA TO SEND" : " HAS NO DATA TO SEND");
        sb.append(" ").append(getDataServiceOptions());
        sb.append(" DAC:").append(getDataAccessControl());
        return sb.toString();
    }

    public boolean hasDataToSend()
    {
        return getMessage().get(DATA_TO_SEND_FLAG);
    }

    public DataServiceOptions getDataServiceOptions()
    {
        if(mDataServiceOptions == null)
        {
            mDataServiceOptions = new DataServiceOptions(getMessage().getInt(DATA_SERVICE_OPTIONS));
        }

        return mDataServiceOptions;
    }

    public int getDataAccessControl()
    {
        return getMessage().getInt(DATA_ACCESS_CONTROL);
    }

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
