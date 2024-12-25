/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCH_63_16_11 extends ReedSolomon_63_P25
{
    private final static Logger mLog = LoggerFactory.getLogger(BCH_63_16_11.class);

    /**
     * BCH( 63,16,23) decoder
     */
    public BCH_63_16_11()
    {
        super(63, 16);
    }

    /**
     * Performs error detection and correction on the first 63 bits of the
     * message argument.  If the message is correctable, only the first 16 bits
     * (information bits) are corrected.
     *
     * @return - true = success, false = failure
     */
    public BinaryMessage correctNID(BinaryMessage message)
    {
        CRC status = CRC.PASSED;

        int[] original = message.toReverseIntegerArray(0, 62);
        int[] corrected = new int[63];

        boolean irrecoverableErrors = decode(original, corrected);

        if(irrecoverableErrors)
        {
            message.setCRC(CRC.FAILED_CRC);

            return message;
        }
        else
        {
            for(int x = 0; x < 16; x++)
            {
                int index = 63 - x - 1;

                if(corrected[index] != original[index])
                {
                    status = CRC.CORRECTED;

                    if(corrected[index] == 1)
                    {
                        message.set(x);
                    }
                    else
                    {
                        message.clear(x);
                    }
                }
            }
        }

        message.setCRC(status);

        return message;
    }

    public static void main(String[] args)
    {
        BCH_63_16_11 bch = new BCH_63_16_11();
        long[] SYNDROMES = new long[]{
                Long.parseLong("6331141367235452", 8),
                Long.parseLong("5265521614723276", 8),
                Long.parseLong("4603711461164164", 8),
                Long.parseLong("2301744630472072", 8),
                Long.parseLong("7271623073000466", 8),
                Long.parseLong("5605650752635660", 8),
                Long.parseLong("2702724365316730", 8),
                Long.parseLong("1341352172547354", 8),
                Long.parseLong("0560565075263566", 8),
                Long.parseLong("6141333751704220", 8),
                Long.parseLong("3060555764742110", 8),
                Long.parseLong("1430266772361044", 8),
                Long.parseLong("0614133375170422", 8),
                Long.parseLong("6037114611641642", 8),
                Long.parseLong("5326507063515373", 8),
                Long.parseLong("4662302756473127", 8),
        };

        BinaryMessage tdulcBM = new BinaryMessage(64);
        tdulcBM.set(2);
        tdulcBM.set(4);
        tdulcBM.set(6);
        tdulcBM.set(12);
        tdulcBM.set(13);
        tdulcBM.set(14);
        tdulcBM.set(15);
        long checksum = 0;
        for(int x = tdulcBM.nextSetBit(0); x >= 0; x=tdulcBM.nextSetBit(x+1))
        {
            checksum ^= SYNDROMES[x];
        }
        tdulcBM.load(16, 48, checksum);
        System.out.println("TDULC:" + tdulcBM);

//        int[] tdulc = tdulcBM.toIntegerArray();
        int[] tdulc = tdulcBM.toReverseIntegerArray(0, 62);
        int[] corrTDULC = new int[63];
        boolean bad = bch.decode(tdulc, corrTDULC);
        System.out.println("Bad:" + bad);

        String orig = "110000101010100010110101001110000111100010000111111000001010100";
        BinaryMessage message = BinaryMessage.load(orig);
        int[] original = message.toIntegerArray();
        int[] reversed = message.toReverseIntegerArray(0, 62);
        StringBuilder sb = new StringBuilder();
        for(int i: reversed)
        {
            sb.append(i);
        }
        System.out.println("Rev:" + sb.toString());

        System.out.println("Original: " + Arrays.toString(original));
        System.out.println("Reversed: " + Arrays.toString(reversed));

        int[] corrected = new int[63];
        boolean uncorrectable = bch.decode(original, corrected);
        System.out.println("Uncorrectable: " + uncorrectable);

        corrected = new int[63];
        boolean uncorrectable2 = bch.decode(reversed, corrected);

        System.out.println("Uncorrectable2: " + uncorrectable2);

        long gen = Long.parseLong("6331141367235453", 8);
        System.out.println("Gen: " + gen);
        System.out.println("Gen: " + Long.toBinaryString(gen));


    }
}
