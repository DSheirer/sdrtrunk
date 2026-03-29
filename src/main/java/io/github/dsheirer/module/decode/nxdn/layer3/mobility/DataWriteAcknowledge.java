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

package io.github.dsheirer.module.decode.nxdn.layer3.mobility;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ErrorBlockFlag;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ResponseInformation;

/**
 * Acknowledge to data write operation
 */
public class DataWriteAcknowledge extends DataWrite
{
    private static final IntField RESPONSE_INFORMATION = IntField.length16(OCTET_7);
    private static final IntField ERROR_BLOCK_FLAG = IntField.length16(OCTET_9);
    private ResponseInformation mResponseInformation;
    private ErrorBlockFlag mErrorBlockFlag;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public DataWriteAcknowledge(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append(getMessageType());
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getResponseInformation());
        sb.append(" ").append(getErrorBlockFlag());
        return super.toString();
    }

    /**
     * Response information
     */
    public ResponseInformation getResponseInformation()
    {
        if(mResponseInformation == null)
        {
            mResponseInformation = new ResponseInformation(getMessage().getInt(RESPONSE_INFORMATION));
        }

        return mResponseInformation;
    }

    /**
     * Error block flags
     */
    public ErrorBlockFlag getErrorBlockFlag()
    {
        if(mErrorBlockFlag == null)
        {
            mErrorBlockFlag = new ErrorBlockFlag(getMessage().getInt(ERROR_BLOCK_FLAG));
        }

        return mErrorBlockFlag;
    }
}
