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
package io.github.dsheirer.module.decode.fleetsync2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCFleetsync;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.fleetsync2.FleetsyncMessageType;
import io.github.dsheirer.module.decode.fleetsync2.identifier.FleetsyncIdentifier;
import io.github.dsheirer.protocol.Protocol;

import java.util.BitSet;

public abstract class Fleetsync2Message extends Message
{
    //Message Header
    private static int[] BIT_REVERSALS = {0, 1, 2, 3, 4};
    private static int[] SYNC_PATTERN = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

    //Message Block 1
    private static int[] STATUS = {21, 22, 23, 24, 25, 26, 27};
    private static int[] MESSAGE_TYPE = {29, 30, 31, 32, 33};

    //Message Type Flags
    private static int FLAG_UNKNOWN_1 = 21;
    private static int FLAG_EMERGENCY_WORKER = 22;
    private static int FLAG_UNKNOWN_2 = 23;
    private static int FLAG_LONE_WORKER = 24;
    private static int FLAG_UNKNOWN_3 = 25;
    private static int FLAG_PAGING = 26;
    private static int FLAG_END_OF_TRANSMISSION = 27;
    private static int FLAG_MANUAL = 28;
    private static int FLAG_AUTOMATIC_NUMBER_IDENTIFIER = 29;
    private static int FLAG_STATUS = 30;
    private static int FLAG_ACKNOWLEDGE = 31;
    private static int FLAG_UNKNOWN_4 = 32;
    private static int FLAG_UNKNOWN_5 = 33;
    private static int FLAG_UNKNOWN_6 = 34; //Always set for Acknowledge
    private static int FLAG_GPS_EXTENSION = 35;
    private static int FLAG_FLEET_EXTENSION = 36;

    //From and To talkgroups share a common fleet value for single-block messages.
    private static int[] FLEET = {37, 38, 39, 40, 41, 42, 43, 44};
    private static int[] FROM_IDENT = {45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56};
    private static int[] TO_IDENT = {57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68};

    private static int[] CRC_BLOCK_1 = {69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84};

    //Message Block 2
    private static int[] FLEET_TO = {85, 86, 87, 88, 89, 90, 91, 92};
    private static int[] CRC_BLOCK_2 = {132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148};

    private CorrectedBinaryMessage mMessage;
    private CRC[] mCRC = new CRC[2];
    private FleetsyncIdentifier mFromIdentifier;
    private FleetsyncIdentifier mToIdentifier;

    public Fleetsync2Message(CorrectedBinaryMessage message, long timestamp)
    {
        super(timestamp);
        mMessage = message;
        checkParity();
    }

    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.FLEETSYNC;
    }

    /**
     * Indicates the number of 64-bit blocks that makeup this message.
     */
    protected abstract int getBlockCount();

    /**
     * Fleetsync identifier for the from or transmitting radio.
     */
    public FleetsyncIdentifier getFromIdentifier()
    {
        if(mFromIdentifier == null)
        {
            mFromIdentifier = FleetsyncIdentifier.createFromUser(getCommonFleet() + getFromIdent());
        }

        return mFromIdentifier;
    }

    /**
     * Fleetsync identifier for the to or receiving radio
     */
    public FleetsyncIdentifier getToIdentifier()
    {
        if(mToIdentifier == null)
        {
            int value = (hasFleetExtensionFlag(getMessage()) ? getExtendedFleet() : getCommonFleet()) + getToIdent();
            mToIdentifier = FleetsyncIdentifier.createToUser(value);
        }

        return mToIdentifier;
    }

    /**
     * Shared or common fleet value.  Note: this value is added to 99 and then left shifted by 12 to obtain fleets in
     * the range of 1 - 127.
     */
    private int getCommonFleet()
    {
        return (getMessage().getInt(FLEET) + 99) << 12;
    }

    /**
     * Extended fleet for the TO identifier.  This value is only valid when the extended fleet flag is set.
     * Note: this value is added to 99 and then left shifted by 12 to obtain fleets in the range of 1 - 127.
     */
    private int getExtendedFleet()
    {
        return (getMessage().getInt(FLEET_TO) + 99) << 12;
    }

    /**
     * From ident value.  Transmitted value is added to 999 to obtain ident values in range of 1000 - 4996
     */
    private int getFromIdent()
    {
        return getMessage().getInt(FROM_IDENT) + 999;
    }

    /**
     * To ident value. Transmitted value is added to 999 to obtain ident values in range of 1000 - 4996
     */
    private int getToIdent()
    {
        return getMessage().getInt(TO_IDENT) + 999;
    }

    private void checkParity()
    {
        //Check message block 1
        CRC block1Crc  = detectAndCorrect(getMessage(), 21, 85);

        mCRC = new CRC[getBlockCount()];
        mCRC[0] = block1Crc;

        for(int x = 1; x < getBlockCount(); x++)
        {
            int blockStart = 21 + (x * 64);
            mCRC[x] = detectAndCorrect(getMessage(), blockStart, blockStart + 64);
        }
    }

    public static CRC detectAndCorrect(CorrectedBinaryMessage message, int start, int end)
    {
        BitSet original = message.get(start, end);

        CRC retVal = CRCFleetsync.check(original);

        //Attempt to correct single-bit errors
        if(retVal == CRC.FAILED_PARITY)
        {
            int[] errorBitPositions = CRCFleetsync.findBitErrors(original);

            if(errorBitPositions != null)
            {
                for(int errorBitPosition : errorBitPositions)
                {
                    message.flip(start + errorBitPosition);
                    message.incrementCorrectedBitCount(1);
                }

                retVal = CRC.CORRECTED;
            }
        }

        return retVal;
    }

    public boolean isValid()
    {
        return mCRC[0].passes();
    }

    /**
     * Type of message
     */
    public FleetsyncMessageType getMessageType()
    {
        return getMessageType(getMessage());
    }

    public static FleetsyncMessageType getMessageType(BinaryMessage message)
    {
        if(hasAcknowledgeFlag(message))
        {
            return FleetsyncMessageType.ACKNOWLEDGE;
        }

        if(hasGPSFlag(message))
        {
            return FleetsyncMessageType.GPS;
        }

        if(hasStatusFlag(message))
        {
            return FleetsyncMessageType.STATUS;
        }

        if(hasANIFlag(message))
        {
            return FleetsyncMessageType.ANI;
        }

        if(hasPagingFlag(message))
        {
            return FleetsyncMessageType.PAGING;
        }

        if(hasLoneWorkerFlag(message) && hasEmergencyFlag(message))
        {
            return FleetsyncMessageType.LONE_WORKER_EMERGENCY;
        }

        return FleetsyncMessageType.UNKNOWN;
    }

    /**
     * Message Type Indicator Flags.  The flags have inverted values where 0 = flag is true
     */
    public static boolean hasEndOfTransmissionFlag(BinaryMessage message)
    {
        return !message.get(FLAG_END_OF_TRANSMISSION);
    }

    public static boolean hasEmergencyFlag(BinaryMessage message)
    {
        return !message.get(FLAG_EMERGENCY_WORKER);
    }

    public static boolean hasLoneWorkerFlag(BinaryMessage message)
    {
        return !message.get(FLAG_LONE_WORKER);
    }

    public static boolean hasPagingFlag(BinaryMessage message)
    {
        return !message.get(FLAG_PAGING);
    }


    /**
     * Message Type Indicator Flags.  The flags have normal values where 1 = true
     *
     * @return
     */
    public static boolean hasANIFlag(BinaryMessage message)
    {
        return message.get(FLAG_AUTOMATIC_NUMBER_IDENTIFIER);
    }

    public static boolean hasAcknowledgeFlag(BinaryMessage message)
    {
        return message.get(FLAG_ACKNOWLEDGE);
    }

    public static boolean hasFleetExtensionFlag(BinaryMessage message)
    {
        return message.get(FLAG_FLEET_EXTENSION);
    }

    public static boolean hasGPSFlag(BinaryMessage message)
    {
        return message.get(FLAG_GPS_EXTENSION);
    }

    public static boolean hasStatusFlag(BinaryMessage message)
    {
        return message.get(FLAG_STATUS);
    }

}
