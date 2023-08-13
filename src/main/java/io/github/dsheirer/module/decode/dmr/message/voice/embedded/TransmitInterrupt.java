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

package io.github.dsheirer.module.decode.dmr.message.voice.embedded;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Transmit Interrupt short burst
 * <p>
 * See: https://patents.google.com/patent/US8271009B2
 */
public class TransmitInterrupt extends ShortBurst
{
    private static final int[] DELAY = new int[]{3, 4, 5, 6, 7};

    /**
     * Constructor
     *
     * @param message containing the delinterleaved and error-corrected short burst payload.
     */
    public TransmitInterrupt(CorrectedBinaryMessage message)
    {
        super(message);
        setValid(passesCRC3());
    }

    @Override
    public String toString()
    {
        return "TRANSMIT INTERRUPT (TXI) AT " + getDelay();
    }

    /**
     * Delay to when another radio can interrupt the current call.
     * @return delay string.
     */
    public String getDelay()
    {
        int value = getMessage().getInt(DELAY);

        switch(value)
        {
            case 0:
                return "ANY TIME";
            case 2:
                return "FRAME E";
            case 4:
                return "FRAME D";
            case 6:
                return "FRAME C";
            case 8:
                return "FRAME B";
            default:
                return "UNKNOWN(" + value + ")";
        }
    }
}
