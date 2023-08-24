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
package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.AbsoluteChannelParameters;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * Control Signalling Block (CSBK) Message
 *
 * ETSI 102 361-1 7.2.0
 */
public abstract class CSBKMessage extends DataMessage
{
    protected static final int CSBK_CRC_MASK = 0xA5A5;
    protected static final int MBC_HEADER_CRC_MASK = 0xAAAA;
    protected static final int MBC_LAST_BLOCK_CRC_MASK = 0x0;

    private static final int LAST_BLOCK = 0;
    private static final int PROTECT_FLAG = 1;
    private static final int[] OPCODE = new int[]{2, 3, 4, 5, 6, 7};
    private static final int[] VENDOR = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an instance
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public CSBKMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                       long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);

    }

    /**
     * Checks CRC and sets the message valid flag according to the results.
     */
    public void checkCRC()
    {
        int correctedBitCount = CRCDMR.correctCCITT80(getMessage(), 0, 80, CSBK_CRC_MASK);

        //Set message valid flag according to the corrected bit count for the CRC protected message
        setValid(correctedBitCount < 2);
    }

    /**
     * Checks CRC using an alternate mask value and sets the message valid flag according to the results.
     */
    public void checkCRC(int mask)
    {
        int correctedBitCount = CRCDMR.correctCCITT80(getMessage(), 0, 80, mask);

        //Set message valid flag according to the corrected bit count for the CRC protected message
        setValid(correctedBitCount < 2);
    }

    /**
     * Checks crc for multi-block CSBK where the continuation block is an absolute channel parameters block
     *
     * Note: this method assumes that there is just 1 continuation block and that block is used to carry
     * the absolute channel parameters structure.  If future implementations use more than 1 MBC continuation
     * block, then the CRC check has to be performed across all of the continuation blocks where the CRC checksum is
     * contained in the final continuation block.
     */
    protected void checkMultiBlockCRC(AbsoluteChannelParameters absoluteChannelParameters)
    {
        int headerCorrectedBitCount = CRCDMR.correctCCITT80(getMessage(), 0, 80, MBC_HEADER_CRC_MASK);

        int continuationCorrectedBitCount = 0;

        if(absoluteChannelParameters != null)
        {
            continuationCorrectedBitCount = CRCDMR.correctCCITT80(absoluteChannelParameters.getMessage(),
                0, 80, MBC_LAST_BLOCK_CRC_MASK);
        }

        //Set message valid flag according to the corrected bit count for the CRC protected messages
        setValid(headerCorrectedBitCount < 2 && continuationCorrectedBitCount < 2);
    }

    /**
     * Indicates if this is the last/final block of a multi-block message fragment sequence.
     */
    public boolean isLastBlock()
    {
        return getMessage().get(LAST_BLOCK);
    }

    /**
     * Indicates if this message is encrypted.
     */
    public boolean isEncrypted()
    {
        return getMessage().get(PROTECT_FLAG);
    }

    /**
     * Utility method to lookup the opcode from a CSBK message
     * @param message containing CSBK bits
     * @return opcode
     */
    public static Opcode getOpcode(BinaryMessage message)
    {
        return Opcode.fromValue(message.getInt(OPCODE), getVendor(message));
    }

    /**
     * Opcode for this CSBK message
     */
    public Opcode getOpcode()
    {
        return getOpcode(getMessage());
    }

    /**
     * Opcode numeric value
     */
    protected int getOpcodeValue()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Utility method to lookup the vendor from a CSBK message
     * @param message containing CSBK bits
     * @return vendor
     */
    public static Vendor getVendor(BinaryMessage message)
    {
        return Vendor.fromValue(message.getInt(VENDOR));
    }

    /**
     * Vendor for this message
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Numerical value for the vendor
     */
    protected int getVendorID()
    {
        return getMessage().getInt(VENDOR);
    }
}
