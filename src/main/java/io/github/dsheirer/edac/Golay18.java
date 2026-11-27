package io.github.dsheirer.edac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 *     -----------------------------------------------------------------------
 *     Galois24 decoder based on Hank Wallace's tutorial/algorithm located at:
 *     http://www.aqdi.com/golay.htm
 ******************************************************************************/

/**
 * Golay(18,6,8) shortened code decoder (TIA-102.BAAA-B 4.7).  The code is the (24,12,8) extended Golay code with the
 * left-most 6 bits deleted, so the word is decoded as a Golay(24,12) word with those 6 bits set to zero.
 */
public class Golay18
{
    private final static Logger mLog = LoggerFactory.getLogger(Golay18.class);

    /**
     * Performs error detection and correction of the 18-bit word that starts at the start index.  Corrects up to three
     * bit errors and detects four.  An uncorrectable word is left unchanged.
     *
     * @param message containing the word
     * @param startIndex of the 18-bit word
     * @return 0 = no errors, 1 = corrected, 2 = uncorrectable
     */
    public static int checkAndCorrect(CorrectedBinaryMessage message, int startIndex)
    {
        int value = message.getInt(startIndex, startIndex + 17);

        CorrectedBinaryMessage word = new CorrectedBinaryMessage(24);
        word.load(6, 18, value);

        //The 6 deleted bits are zero in every codeword, so a correction that sets any of them is a miscorrection.
        if(Golay24.checkAndCorrect(word, 0) == 2 || word.getInt(0, 5) != 0)
        {
            return 2;
        }

        int corrected = word.getInt(6, 23);
        int bitsCorrected = Integer.bitCount(value ^ corrected);

        //Every codeword has even weight.  Golay24 doesn't check the overall parity bit after it corrects the other
        //bits, so odd weight here means the parity bit is also in error.  Correct it, unless that is a fourth error,
        //which the (18,6,8) code detects but can't correct.
        if(Integer.bitCount(corrected) % 2 != 0)
        {
            if(bitsCorrected >= 3)
            {
                return 2;
            }

            corrected ^= 1;
            bitsCorrected++;
        }

        if(bitsCorrected == 0)
        {
            return 0;
        }

        message.load(startIndex, 18, corrected);
        message.incrementCorrectedBitCount(bitsCorrected);
        return 1;
    }
}
