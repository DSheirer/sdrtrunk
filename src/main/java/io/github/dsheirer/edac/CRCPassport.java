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
package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

import java.util.BitSet;

/**
 * Passport CRC checksum utility
 *
 * Passport message blocks are 68 bits in length as follows:
 * 0 -  8: Sync
 * 9 - 59: message bits
 * 60 - 67: CRC-7 check bits plus 1 parity
 *
 * Passport uses a CRC-7 (0x44) with an initial fill of 0x44 plus parity set.
 *
 * CRC-7 Generating Polynomial: x6 + x2 + 1 (0x44)
 */
public class CRCPassport
{
    private static final byte sFILL_00 = (byte)0x00;

    private static byte[] sCHECKSUMS = new byte[]
        {
            (byte)0x6E, //DCC 1
            (byte)0xBF, //DCC 0
            (byte)0xD6, //LCN 10
            (byte)0xE3, //LCN 9
            (byte)0xF8, //LCN 8
            (byte)0x7C, //LCN 7
            (byte)0x3E, //LCN 6
            (byte)0x97, //LCN 5
            (byte)0xC2, //LCN 4
            (byte)0xE9, //LCN 3
            (byte)0x75, //LCN 2
            (byte)0x3B, //LCN 1
            (byte)0x94, //LCN 0
            (byte)0x4A, //SITE 6
            (byte)0xAD, //SITE 5
            (byte)0x57, //SITE 4
            (byte)0xA2, //SITE 3
            (byte)0xD9, //SITE 2
            (byte)0x6D, //SITE 1
            (byte)0x37, //SITE 0
            (byte)0x92, //GROUP 15
            (byte)0xC1, //GROUP 14
            (byte)0x61, //GROUP 13
            (byte)0x31, //GROUP 12
            (byte)0x19, //GROUP 11
            (byte)0x0D, //GROUP 10
            (byte)0x07, //GROUP 9
            (byte)0x8A, //GROUP 8
            (byte)0xCD, //GROUP 7
            (byte)0x67, //GROUP 6
            (byte)0xBA, //GROUP 5
            (byte)0xD5, //GROUP 4
            (byte)0x6B, //GROUP 3
            (byte)0xBC, //GROUP 2
            (byte)0x5E, //GROUP 1
            (byte)0xA7, //GROUP 0
            (byte)0xDA, //TYPE 3
            (byte)0xE5, //TYPE 2
            (byte)0x73, //TYPE 1
            (byte)0xB0, //TYPE 0
            (byte)0x58, //FREE 10
            (byte)0x2C, //FREE 9
            (byte)0x16, //FREE 8
            (byte)0x83, //FREE 7
            (byte)0xC8, //FREE 6
            (byte)0x64, //FREE 5
            (byte)0x32, //FREE 4
            (byte)0x91, //FREE 3
            (byte)0x49, //FREE 2
            (byte)0x25, //FREE 1
            (byte)0x13 //FREE 0
        };

    /**
     * Determines if message bits 9 - 59 pass the CRC checksum
     * contained in bits 60 - 68, using a lookup table of CRC checksum values
     * derived from the CRC-7 value and the final parity bit
     */
    public static CRC check(BitSet msg)
    {
        CRC crc = CRC.UNKNOWN;

        byte calculated = 0x0; //Starting value for an OSW

        //Iterate bits that are set and XOR running checksum with lookup value
        for(int i = msg.nextSetBit(9); i >= 9 && i <= 59; i = msg.nextSetBit(i + 1))
        {
            calculated ^= sCHECKSUMS[i - 9];
        }

        //Apply the message checksum to derive the residual
        calculated ^= getChecksum(msg);

        switch((byte)calculated)
        {
            case sFILL_00:
                crc = CRC.PASSED;
                break;
            default:
                crc = CRC.FAILED_CRC;
        }

        return crc;
    }

    public static byte getResidual(BitSet msg)
    {
        byte calculated = 0x0; //Initial fill of zero

        //Iterate bits that are set and XOR running checksum with lookup value
        for(int i = msg.nextSetBit(9); i >= 9 && i <= 59; i = msg.nextSetBit(i + 1))
        {
            calculated ^= sCHECKSUMS[i - 9];
        }

        calculated ^= getChecksum(msg);

        return calculated;
    }

    public static byte[] getChecks()
    {
        return sCHECKSUMS;
    }

    /**
     * Returns the integer value of the 7 bit crc checksum
     */
    public static byte getChecksum(BitSet msg)
    {
        byte retVal = 0x0;

        for(int x = 0; x < 8; x++)
        {
            if(msg.get(x + 60))
            {
                retVal ^= (byte)(1 << (7 - x));
            }
        }

        return retVal;
    }

    /**
     * Initial take on error correction.  The majority of the errors detected
     * are in the final 2 bits, owing to the forward looking soft bit detection
     * used in the decoder.  For right now, correct any messages where the
     * residual indicates that the final 1 or 2 bits are bad.
     */
    public static CorrectedBinaryMessage correct(CorrectedBinaryMessage msg)
    {
        int residual = (int)(0xFF & getResidual(msg));

        switch(residual)
        {
            case 1:
            case 136:
                msg.flip(67);
                msg.incrementCorrectedBitCount(1);
                break;
            case 3:
            case 138:
                msg.flip(66);
                msg.flip(67);
                msg.incrementCorrectedBitCount(2);
                break;
            case 2:
            case 139:
                msg.flip(66);
                msg.incrementCorrectedBitCount(1);
                break;
            case 140:
                msg.flip(65);
                msg.flip(67);
                msg.incrementCorrectedBitCount(2);
                break;
            case 153:
                msg.flip(67);
                msg.incrementCorrectedBitCount(1);
                break;
            case 142:
                msg.flip(65);
                msg.flip(66);
                msg.flip(67);
                msg.incrementCorrectedBitCount(3);
                break;
        }

        return msg;
    }

    /**
     * Performs an even parity check on the bitset.  If the number of bits that
     * are set is an even number, then it passes the even parity check
     */
    public static boolean isEvenParity(BinaryMessage bits)
    {
        return (bits.cardinality() % 2 == 0);
    }
}
