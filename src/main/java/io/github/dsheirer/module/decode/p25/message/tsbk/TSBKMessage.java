/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.P25Utils;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.List;

/**
 * APCO 25 Trunking Signalling Block (TSBK)
 */
public abstract class TSBKMessage extends P25Message
{
    private static final int LAST_BLOCK_FLAG = 0;
    private static final int ENCRYPTION_FLAG = 1;
    private static final int[] OPCODE = {2, 3, 4, 5, 6, 7};
    private static final int[] VENDOR = {8, 9, 10, 11, 12, 13, 14, 15};

    private DataUnitID mDataUnitID;

    /**
     * Constructs a TSBK from the binary message sequence.
     *
     * @param dataUnitID TSBK1/2/3
     * @param message binary sequence
     * @param nac decoded from the NID
     * @param timestamp for the message
     */
    public TSBKMessage(DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
        mDataUnitID = dataUnitID;

        //The CRC-CCITT can correct up to 1 bit error or detect 2 or more errors.  We mark the message as
        //invalid if the algorithm detects more than 1 correctable error.
        int errors = CRCP25.correctCCITT80(message, 0, 80);
        if(errors > 1)
        {
            setValid(false);
        }
    }

    @Override
    public DataUnitID getDUID()
    {
        return mDataUnitID;
    }

    /**
     * Indicates if this is an encrypted LCW
     */
    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTION_FLAG);
    }

    /**
     * Indicates if this is the last TSBK in a sequence (1-3 blocks)
     */
    public boolean isLastBlock()
    {
        return isLastBlock(getMessage());
    }

    /**
     * Indicates if this is the last TSBK in a sequence (1-3 blocks)
     */
    public static boolean isLastBlock(BinaryMessage binaryMessage)
    {
        return binaryMessage.get(LAST_BLOCK_FLAG);
    }

    /**
     * Vendor format for this TSBK.
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Lookup the Vendor format for the specified LCW
     */
    public static Vendor getVendor(BinaryMessage binaryMessage)
    {
        return Vendor.fromValue(binaryMessage.getInt(VENDOR));
    }

    /**
     * Opcode for this TSBK
     */
    public Opcode getOpcode()
    {
        return getOpcode(getMessage(), getDirection(), getVendor());
    }

    /**
     * Opcode numeric value
     */
    public int getOpcodeNumber()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Opcode for this TSBK
     */
    public static Opcode getOpcode(BinaryMessage binaryMessage, Direction direction, Vendor vendor)
    {
        return Opcode.fromValue(binaryMessage.getInt(OPCODE), direction, vendor);
    }

    /**
     * Direction - inbound (ISP) or outbound (OSP)
     */
    public abstract Direction getDirection();

    /**
     * List of identifiers provided by the message
     */
    public abstract List<Identifier> getIdentifiers();

    /**
     * Creates a string with the basic TSBK information
     */
    public String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessageStub());

        if(isValid())
        {
            sb.append(" ").append(getOpcode().getLabel());

            P25Utils.pad(sb, 30);

            if(isEncrypted())
            {
                sb.append(" ENCRYPTED");
            }
        }
        else
        {
            sb.append("**CRC-FAILED**");
        }

        return sb.toString();
    }
}
