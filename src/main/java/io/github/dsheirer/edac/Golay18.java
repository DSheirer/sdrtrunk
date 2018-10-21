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
 * Golay 18/6/8 decoder.  Uses the Golay24 edac class to perform edac on Golay
 * 18 messages by treating the Golay 18 message as a Golay 24 message with the
 * left-most 6 bits set to zero.
 */
public class Golay18
{
    private final static Logger mLog = LoggerFactory.getLogger(Golay18.class);

    /**
     * Performs error detection and correction.
     */
    public static void checkAndCorrect(CorrectedBinaryMessage message, int startIndex)
    {
        int value = message.getInt(startIndex, startIndex + 17);

        CorrectedBinaryMessage temp = new CorrectedBinaryMessage(24);
        temp.load(6, 18, value);

        int errorsCorrected = Golay24.checkAndCorrect(temp, 0);

        int correctedValue = temp.getInt(6, 23);

        message.load(startIndex, 18, correctedValue);

        int correctedBitCount = Integer.bitCount(value ^ correctedValue);
        message.incrementCorrectedBitCount(correctedBitCount);
    }
}
