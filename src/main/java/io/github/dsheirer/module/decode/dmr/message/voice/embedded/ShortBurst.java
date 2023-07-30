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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Base class for DMR Voice Frame F Short Burst from the EMB payload.
 */
public abstract class ShortBurst
{
    private static final int[] CRC3 = new int[]{0, 1, 2};
    private static final int[] OPCODE = new int[]{8, 9, 10};
    private static final int[] FULL_MESSAGE = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    private CorrectedBinaryMessage mMessage;
    private boolean mValid = true;

    /**
     * Constructor
     *
     * @param message containing the delinterleaved and error-corrected short burst payload.
     */
    public ShortBurst(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    /**
     * Access the underlying message.
     *
     * @return message
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if this message passes any CRC checks.
     * @return
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Sets the valid CRC flag for this message
     * @param valid true or false
     */
    protected void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Numeric opcode value.
     *
     * @return value.
     */
    public int getOpcodeValue()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Opcode for this message.
     *
     * @return opcode
     */
    public ShortBurstOpcode getOpcode()
    {
        return getOpcode(getMessage());
    }

    /**
     * Static utility method to lookup the opcode from a short burst message.
     *
     * @param message containing a short burst
     * @return opcode
     */
    public static ShortBurstOpcode getOpcode(BinaryMessage message)
    {
        return ShortBurstOpcode.fromValue(message.getInt(OPCODE));
    }

    /**
     * Checks the message to determine if it passes for a CRC3.  Not all Short Burst messages use the CRC3 check.
     *
     * @return true if it passes.
     */
    public boolean passesCRC3()
    {
        int checksum = getMessage().getInt(FULL_MESSAGE);
        int polynomial = 0xB << 7;
        int checkBit = 0x1 << 10;

        for(int x = 10; x >= 0; x--)
        {
            if(checksum == 0)
            {
                return true;
            }

            if((checksum & checkBit) == checkBit)
            {
                checksum ^= polynomial;
            }
            polynomial >>= 1;
            checkBit >>= 1;
        }

        return checksum == 0;
    }
}
