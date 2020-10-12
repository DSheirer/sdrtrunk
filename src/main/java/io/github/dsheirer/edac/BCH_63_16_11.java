/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCH_63_16_11 extends ReedSolomon_63_P25
{
    private final static Logger mLog = LoggerFactory.getLogger(BCH_63_16_11.class);

    /**
     * BCH( 63,16,11) decoder
     */
    public BCH_63_16_11()
    {
        super(63, 41);
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
        String orig = "0010011000000011010010100000000110000111110011101010001010110000";
        String error = "0001010100000011010010100000000110000111100011001010001010110000";

        BinaryMessage errorMessage = BinaryMessage.load(error);

        BCH_63_16_11 bch = new BCH_63_16_11();

        mLog.debug("ORIG:" + orig);
        mLog.debug(" ERR:" + errorMessage.toString());
        mLog.debug(" ");

        errorMessage = bch.correctNID(errorMessage);

        mLog.debug(" CRC: " + errorMessage.getCRC().name());
        mLog.debug("CORR:" + errorMessage.toString());
        mLog.debug("ORIG:" + orig);
    }
}
