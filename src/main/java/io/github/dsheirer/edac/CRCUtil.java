/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CRCUtil
{
    private final static Logger mLog = LoggerFactory.getLogger(CRCUtil.class);

    public static long[] generate(int messageSize,
                                  int crcSize,
                                  long polynomial,
                                  long initialFill,
                                  boolean includeCRCBitErrors)
    {
        long[] crcTable = new long[messageSize + (includeCRCBitErrors ? crcSize : 0)];

        int[] checksumIndexes = new int[crcSize];

        for(int x = 0; x < crcSize; x++)
        {
            checksumIndexes[x] = messageSize + x;
        }

        for(int x = 0; x < messageSize; x++)
        {
            BinaryMessage message = new BinaryMessage(messageSize + crcSize);

            message.load(messageSize, crcSize, initialFill);

            message.set(x);

            message = decode(message, 0, messageSize, polynomial, crcSize);

            long checksum = message.getLong(checksumIndexes);

            crcTable[x] = checksum;
        }

        if(includeCRCBitErrors)
        {
            for(int x = 0; x < crcSize; x++)
            {
                crcTable[messageSize + x] = Long.rotateLeft(1, x);
            }
        }

        return crcTable;
    }

    /**
     * Generates the checksum table and treats the final bit of each check value
     * as a parity bit that complies with the request parity argument.
     */
    public static long[] generate(int messageSize,
                                  int crcSize,
                                  long polynomial,
                                  long initialFill,
                                  boolean includeCRCBitErrors,
                                  Parity parity)
    {
        long[] table = generate(messageSize, crcSize, polynomial, initialFill,
            includeCRCBitErrors);

        if(parity == Parity.NONE)
        {
            return table;
        }
        else
        {
            for(int x = 0; x < table.length; x++)
            {
                table[x] <<= 1;

                if(parity(table[x]) != parity)
                {
                    table[x] ^= 0x1;
                }
            }

            return table;
        }
    }

    public enum Parity
    {EVEN, ODD, NONE}

    ;

    /* Determines the parity (number of 1 bits) for the value */
    public static Parity parity(long value)
    {
        return Long.bitCount(value) % 2 == 0 ? Parity.EVEN : Parity.ODD;
    }


    /**
     * Performs binary division of the polynomial into the message for bits
     * in index 0 to index messageLength - 1, leaving the results in the
     * checksum filed following the message.
     *
     * IN:   MMMMMMMMMMMMMMMMCCCCCCCCCCC
     * OUT:  0000000000000000RRRRRRRRRRR
     *
     * M: Message
     * C: Checksum
     * R: Remainder
     *
     * @param message - message and crc
     * @param polynomial - crc polynomial - must be 1 bit longer than the crc width
     * @param messageSize - message length and start of the checksum
     * @return message with all message bits zeroed out, and the remainder
     * placed in the crc field which starts at index messageLength
     */
    public static BinaryMessage decode(BinaryMessage message, int messageStart, int messageSize, long polynomial, int crcSize)
    {
        for(int i = message.nextSetBit(messageStart); i >= messageStart && i < messageSize; i = message.nextSetBit(i + 1))
        {
            BinaryMessage polySet = new BinaryMessage(crcSize + i + 1);
            polySet.load(i, crcSize + 1, polynomial);
            message.xor(polySet);
            System.out.println(message.toString());
        }

        return message;
    }

    public static String toCodeArray(long[] values)
    {
        boolean integerArray = true;

        /* Determine the correct primitive type: int or long */
        for(long value : values)
        {
            if(value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
            {
                integerArray = false;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();

        if(integerArray)
        {
            sb.append("\npublic static final int[] CHECKSUMS = new int[]\n");
        }
        else
        {
            sb.append("\npublic static final long[] CHECKSUMS = new long[]\n");
        }
        sb.append("{\n");

        StringBuilder row = new StringBuilder();
        row.append("    ");

        for(long value : values)
        {
            StringBuilder element = new StringBuilder();

            element.append("0x");
            element.append(Long.toHexString(value).toUpperCase());

            if(!integerArray)
            {
                element.append("l");
            }

            element.append(", ");

            if(row.length() + element.length() <= 80)
            {
                row.append(element.toString());
            }
            else
            {
                sb.append(row.toString());
                sb.append("\n");
                row = new StringBuilder();
                row.append("    ");

                row.append(element.toString());
            }
        }

        if(row.length() > 4)
        {
            sb.append(row.toString());
            sb.append("\n");
        }

        sb.append("};\n");

        return sb.toString();
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting");

        int messageSize = 164;
        int crcSize = 16;
        long poly = 0x11021l;
        long initialFill = 0x0l;
        long[] checksums = CRCUtil.generate(messageSize, crcSize, poly, initialFill, true);

        StringBuilder sb = new StringBuilder();
        sb.append("private static int[] CHECKSUMS = new int[]{");
        for(long checksum: checksums)
        {
            sb.append("0x").append(Long.toHexString(checksum).toUpperCase());
            sb.append(",");
        }

        sb.append("};");
        System.out.println("Checksums:\n" + sb);

        String hex = "7CFC0039420170452A4F9970000000000000000000D05";
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(BinaryMessage.loadHex(hex));


        int calculated = 0xFFF; //Initial fill of all ones

        int messageStart = 0;
        /* Iterate the set bits and XOR running checksum with lookup value */
        for(int i = message.nextSetBit(messageStart);
            i >= messageStart && i < messageSize;
            i = message.nextSetBit(i + 1))
        {
            System.out.println("Bit [" + i + "] is set");
            calculated ^= checksums[i];
        }

        int checksum = message.getInt(messageSize, messageSize + crcSize - 1);
        int residual = calculated ^ checksum;

        mLog.debug("CALC:" + Integer.toHexString(calculated).toUpperCase() +
                " CHECK:" + Integer.toHexString(checksum).toUpperCase() +
                " RESIDUAL:" + Integer.toHexString(residual).toUpperCase());
    }
}
