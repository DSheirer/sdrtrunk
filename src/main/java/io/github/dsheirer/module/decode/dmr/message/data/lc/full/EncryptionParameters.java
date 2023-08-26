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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import io.github.dsheirer.module.decode.dmr.message.type.EncryptionAlgorithm;
import java.util.ArrayList;
import java.util.List;

/**
 * Encryption Parameters, carried by a PI_HEADER.
 *
 * Note: this message repurposes the OPCODE field to carry the encryption algorithm.
 */
public class EncryptionParameters extends FullLCMessage
{
    private static final int[] KEY_ID = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] INITIALIZATION_VECTOR = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37,
            38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] DESTINATION_GROUP = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
            71, 72, 73, 74, 75, 76, 77, 78, 79};
    //CRC-CCITT with 0x9696 initial fill, bits: 80-95

    private DMRTalkgroup mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructor
     * @param message bits
     * @param timestamp for the message
     * @param timeslot of the message
     */
    public EncryptionParameters(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
        int bitErrors = CRCDMR.correctCCITT80(message, 0, 80, 0x9696);
        setValid(bitErrors < 2);
    }

    /**
     * Overrides the default method for opcode to return a static value, because the opcode field is repurposed to
     * be the algorithm field in both Motorola and Hytera variants of this message, both carried by the Encryption
     * Header message.
     * @return full encryption parameters opcode.
     */
    @Override
    public LCOpcode getOpcode()
    {
        return LCOpcode.FULL_ENCRYPTION_PARAMETERS;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        if(isEncrypted())
        {
            sb.append(" *ENCRYPTED*");
        }

        if(isReservedBitSet())
        {
            sb.append(" *RESERVED-BIT*");
        }

        sb.append("FLC ENCRYPTION PARAMETERS");
        sb.append(" VENDOR:").append(getVendor());
        if(getAlgorithm() == EncryptionAlgorithm.UNKNOWN)
        {
            //List the algorithm (aka opcod) value for unknown algorithms
            sb.append(" ALGORITHM:").append(getOpcodeValue());
        }
        else
        {
            sb.append(" ALGORITHM:").append(getAlgorithm());
        }
        sb.append(" KEY:").append(getKeyId());
        sb.append(" IV:").append(getInitializationVector());
        sb.append(" TALKGROUP:").append(getTalkgroup());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Encryption parameter details
     * @return details
     */
    public String getDetails()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ENCRYPTION ALGORITHM:").append(getAlgorithm());
        sb.append(" KEY:").append(getKeyId());
        sb.append(" IV:").append(getInitializationVector());
        sb.append(" VENDOR:").append(getVendor());
        return sb.toString();
    }

    /**
     * Encryption algorithm.  Note: this message repurposes the OPCODE field to carry the algorithm ID.
     * @return algorithm or UNKNOWN.
     */
    public EncryptionAlgorithm getAlgorithm()
    {
        return EncryptionAlgorithm.fromValue(getOpcodeValue());
    }

    public DMRTalkgroup getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = new DMRTalkgroup(getMessage().getInt(DESTINATION_GROUP));
        }

        return mTalkgroup;
    }

    public int getKeyId()
    {
        return getMessage().getInt(KEY_ID);
    }

    public String getInitializationVector()
    {
        return getMessage().getHex(INITIALIZATION_VECTOR, 8);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
        }

        return mIdentifiers;
    }
}
