/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Protection (encryption) parameter broadcast
 */
public class AMBTCProtectionParameterBroadcast extends AMBTCMessage
{
    private static final int[] HEADER_ALGORITHM_ID = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_KEY_ID = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_INBOUND_MESSAGE_INDICATOR_1 = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
        28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] BLOCK_0_INBOUND_MESSAGE_INDICATOR_2 = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67,
        68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] BLOCK_0_OUTBOUND_MESSAGE_INDICATOR_1 = {88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] BLOCK_1_OUTBOUND_MESSAGE_INDICATOR_2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] BLOCK_1_OUTBOUND_MESSAGE_INDICATOR_3 = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43,
        44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};

    private Identifier mTargetAddress;
    private Identifier mEncryptionKey;
    private String mInboundMessageIndicator;
    private String mOutboundMessageIndicator;
    private List<Identifier> mIdentifiers;

    public AMBTCProtectionParameterBroadcast(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }
        if(getEncryptionKey() != null)
        {
            sb.append(" ENCRYPTION KEY:").append(getEncryptionKey());
        }
        if(getInboundMessageIndicator() != null)
        {
            sb.append(" INBOUND MI:").append(getInboundMessageIndicator());
        }
        if(getOutboundMessageIndicator() != null)
        {
            sb.append(" OUTBOUND MI:").append(getOutboundMessageIndicator());
        }

        return sb.toString();
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getDataBlock(0).getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    public Identifier getEncryptionKey()
    {
        if(mEncryptionKey == null && hasDataBlock(0))
        {
            mEncryptionKey = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(getHeader().getMessage().getInt(HEADER_ALGORITHM_ID),
                getDataBlock(0).getMessage().getInt(BLOCK_0_KEY_ID)));
        }

        return mEncryptionKey;
    }

    public String getInboundMessageIndicator()
    {
        if(mInboundMessageIndicator == null && hasDataBlock(0))
        {
            mInboundMessageIndicator = getDataBlock(0).getMessage().getHex(BLOCK_0_INBOUND_MESSAGE_INDICATOR_1, 10) +
                getDataBlock(0).getMessage().getHex(BLOCK_0_INBOUND_MESSAGE_INDICATOR_2, 8);
        }

        return mInboundMessageIndicator;
    }

    public String getOutboundMessageIndicator()
    {
        if(mOutboundMessageIndicator == null && hasDataBlock(0) && hasDataBlock(1))
        {
            mOutboundMessageIndicator = getDataBlock(0).getMessage().getHex(BLOCK_0_OUTBOUND_MESSAGE_INDICATOR_1, 2) +
                getDataBlock(1).getMessage().getHex(BLOCK_1_OUTBOUND_MESSAGE_INDICATOR_2, 8) +
                getDataBlock(1).getMessage().getHex(BLOCK_1_OUTBOUND_MESSAGE_INDICATOR_3, 8);
        }

        return mOutboundMessageIndicator;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getEncryptionKey() != null)
            {
                mIdentifiers.add(getEncryptionKey());
            }
        }

        return mIdentifiers;
    }
}
