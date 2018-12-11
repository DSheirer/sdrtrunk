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
package io.github.dsheirer.module.decode.mpt1327.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.mpt1327.MPTMessageType;
import io.github.dsheirer.module.decode.mpt1327.identifier.MPT1327SiteIdentifier;
import io.github.dsheirer.protocol.Protocol;

public abstract class MPT1327BaseMessage extends Message
{
    private static int ADDRESS_DATA_FLAG = 0;
    private static int[] SYSTEM_IDENTITY_CODE = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static int[] CODEWORD_COMPLETION_SEQUENCE = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static int[] PREAMBLE = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static int[] PARITY = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static int ADDRESS_BLOCK_FLAG = 64;
    private static int[] MESSAGE_TYPE = {85, 86, 87, 88, 89, 90, 91, 92, 93};

    private CorrectedBinaryMessage mMessage;
    private CRC[] mCRC = new CRC[5];
    private MPT1327SiteIdentifier mSystemIdentityCode;

    public MPT1327BaseMessage(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MPT1327;
    }

    /**
     * Raw binary underlying message
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Message Type
     */
    public abstract MPTMessageType getMessageType();

    /**
     * System/Site identifier
     */
    public MPT1327SiteIdentifier getSystemIdentityCode()
    {
        if(mSystemIdentityCode == null)
        {
            mSystemIdentityCode = MPT1327SiteIdentifier.create(getMessage().getInt(SYSTEM_IDENTITY_CODE));
        }

        return mSystemIdentityCode;
    }
}
