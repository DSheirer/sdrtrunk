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

package io.github.dsheirer.edac.bch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;

/**
 * APCO 25 BCH(63,16,23) code with T=11 error bit correction capacity.
 */
public class BCH_63_16_23_P25 extends BCH_63
{
    public static final int K = 16;
    private static final int T = 11; // Error-correcting capability
    public static final IntField NAC_FIELD = IntField.length12(0);
    public static final IntField DUID_FIELD = IntField.length4(12);

    /**
     * Constructs a BCH decoder instance for processing APCO25 BCH(63,16,23) protected NID codewords
     */
    public BCH_63_16_23_P25()
    {
        super(K, T);
    }

    @Override
    public void decode(CorrectedBinaryMessage message)
    {
        super.decode(message);
    }

    /**
     * Attempts to error correct the NID message.  If the message is uncorrectable, overwrite the message NAC field with
     * the most frequently/recently observed NAC value and attempt to correct the message a second time.
     * @param message to correct
     * @param observedNAC that can be used to attempt a second correction on an uncorrectable message.
     */
    public void decode(CorrectedBinaryMessage message, int observedNAC)
    {
        decode(message);

        if(message.getCorrectedBitCount() == BCH.MESSAGE_NOT_CORRECTED && observedNAC > 0)
        {
            //Check to see if the message NAC is different than the observed NAC ... overwrite and try again.
            if(message.getInt(NAC_FIELD) != observedNAC)
            {
                message.setInt(observedNAC, NAC_FIELD);
                decode(message);
            }
        }
    }
}
