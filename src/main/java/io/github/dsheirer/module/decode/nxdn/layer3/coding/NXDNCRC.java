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

package io.github.dsheirer.module.decode.nxdn.layer3.coding;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;

/**
 * NXDN CRC check methods.
 */
public class NXDNCRC
{
    private static final IntField CHECKSUM_CAC = IntField.length16(155);
    private static final IntField CHECKSUM_LONG_CAC = IntField.length16(136);
    private static final IntField CHECKSUM_SHORT_CAC = IntField.length16(106);
    private static final int CRC_16_FEEDBACK_MASK = 0x8000;
    private static final int CRC_16_REGISTER_MASK = 0xFFFF;
    private static final int CRC_16_TAPS = 0x1021; //Taps: S12, S5 and S0

    private static final IntField CHECKSUM_FACCH2 = IntField.length15(184);
    private static final int CRC_15_FEEDBACK_MASK = 0x4000;
    private static final int CRC_15_REGISTER_MASK = 0x7FFF;
    private static final int CRC_15_TAPS = 0x4CC5; //Taps: S14, S11, S10, S7, S6, S2 and S0

    private static final IntField CHECKSUM_FACCH1 = IntField.length12(80);
    private static final int CRC_12_FEEDBACK_MASK = 0x800;
    private static final int CRC_12_REGISTER_MASK = 0xFFF;
    private static final int CRC_12_TAPS = 0x80F; //Taps: S11, S3, S2, S1 and S0

    private static final IntField CHECKSUM_SACCH = IntField.length6(26);
    private static final int CRC_6_FEEDBACK_MASK = 0x20;
    private static final int CRC_6_REGISTER_MASK = 0x3F;
    private static final int CRC_6_TAPS = 0x27; //Taps: S5, S2, S1 and S0

    private static final int CRC_MESSAGE_FEEDBACK_MASK = 0x80000000;
    private static final int CRC_MESSAGE_REGISTER_MASK = 0xFFFFFFFF;
    private static final int CRC_MESSAGE_TAPS = 0x4C11DB7; //Taps: S26, S23, S22, S16, S12, S11, S10, S8, S7, S5, S4, S2, S1, S0

    /**
     * Performs CRC 16 check against the decoded CAC message.
     * @param message to check
     * @return true if the message passes the CRC check.
     */
    public static boolean checkCAC(CorrectedBinaryMessage message)
    {
        return checkCRC(message, 155, CRC_16_REGISTER_MASK, CRC_16_FEEDBACK_MASK, CRC_16_TAPS, CHECKSUM_CAC);
    }

    /**
     * Performs CRC 16 check against the decoded long CAC message.
     * @param message to check
     * @return true if the message passes the CRC check.
     */
    public static boolean checkLongCAC(CorrectedBinaryMessage message)
    {
        return checkCRC(message, 136, CRC_16_REGISTER_MASK, CRC_16_FEEDBACK_MASK, CRC_16_TAPS, CHECKSUM_LONG_CAC);
    }

    /**
     * Performs CRC 16 check against the decoded short CAC message.
     * @param message to check
     * @return true if the message passes the CRC check.
     */
    public static boolean checkShortCAC(CorrectedBinaryMessage message)
    {
        return checkCRC(message, 136, CRC_16_REGISTER_MASK, CRC_16_FEEDBACK_MASK, CRC_16_TAPS, CHECKSUM_SHORT_CAC);
    }

    /**
     * Performs CRC 15 check against the decoded FACCH2 message.
     * @param message to check
     * @return true if the message passes the CRC check.
     */
    public static boolean checkFACCH2(CorrectedBinaryMessage message)
    {
        return checkCRC(message, 184, CRC_15_REGISTER_MASK, CRC_15_FEEDBACK_MASK, CRC_15_TAPS, CHECKSUM_FACCH2);
    }

    /**
     * Performs CRC 12 check against the decoded FACCH1 message.
     * @param message to check
     * @return true if the message passes the CRC check.
     */
    public static boolean checkFACCH1(CorrectedBinaryMessage message)
    {
        return checkCRC(message, 80, CRC_12_REGISTER_MASK, CRC_12_FEEDBACK_MASK, CRC_12_TAPS, CHECKSUM_FACCH1);
    }

    /**
     * Performs CRC 6 check against the decoded SACCH Layer 3 message.
     * @param message to check
     * @return true if the message passes the CRC check.
     */
    public static boolean checkSACCH(CorrectedBinaryMessage message)
    {
        return checkCRC(message, 26, CRC_6_REGISTER_MASK, CRC_6_FEEDBACK_MASK, CRC_6_TAPS, CHECKSUM_SACCH);
    }

    /**
     * Message CRC check used on the Digital Station ID, Short Data, User Data, etc.
     * @param message with octets including the 4-byte checksum at the end
     * @param length of the message that also serves as the first bit of the 32-bit checksum.
     * @return true if the message passes the CRC check
     */
    public static boolean checkMessage(CorrectedBinaryMessage message, int length)
    {
        IntField checksum = IntField.length32(length);
        return checkCRC(message, length, CRC_MESSAGE_REGISTER_MASK, CRC_MESSAGE_FEEDBACK_MASK, CRC_MESSAGE_TAPS, checksum);
    }

    /**
     * Calculates an checksum and compares it to the transmitted CRC value for the message using the specified initial fill value
     * @param message to check
     * @param length of the message to check (bit positions: 0 to length - 1)
     * @param registerMask for initial fill and masking the return calculated checksum
     * @param feedbackMask with a one set in the high-order feedback tap
     * @param taps to apply when the feedback is a one.
     * @param crcField containing the transmitted checksum
     * @return true if the calculated and transmitted CRC values are the same
     */
    public static boolean checkCRC(CorrectedBinaryMessage message, int length, int registerMask, int feedbackMask,
                                   int taps, IntField crcField)
    {
        int calculated = getChecksum(message, length, registerMask, feedbackMask, taps);
        int transmitted = message.getInt(crcField);
        return (calculated ^ transmitted) == 0;
    }

    /**
     * Calculates the CRC checksum for the message using the specified initial fill value.
     * @param message to check
     * @param length of the message to check (bit positions: 0 to length - 1)
     * @param registerMask for initial fill and masking the return calculated checksum
     * @param feedbackMask with a one set in the high-order feedback tap
     * @param taps to apply when the feedback is a one.
     * @return calculated checksum.
     */
    public static int getChecksum(CorrectedBinaryMessage message, int length, int registerMask, int feedbackMask, int taps)
    {
        int registers = registerMask; //Uses an all-ones initial fill value.
        boolean feedback;

        for(int x = 0; x < length; x++)
        {
            //XOR the input message bit with the high-order (feedback) register bit
            feedback = message.get(x) ^ ((registers & feedbackMask) == feedbackMask);

            registers <<= 1;

            if(feedback)
            {
                registers ^= taps;
            }
        }

        //Mask any high-order left shift leftovers before returning
        return registers & registerMask;
    }
}
