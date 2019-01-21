/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1.message.pdu.block;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.message.IBitErrorProvider;

/**
 * Packet Data Unit data block.
 */
public abstract class DataBlock implements IBitErrorProvider
{
    /**
     * Decoded binary message payload.
     */
    public abstract BinaryMessage getMessage();

    /**
     * Number of bits that were processed for this data block (196).
     */
    @Override
    public int getBitsProcessedCount()
    {
        //Interleaved message length of a PDU packet is 196 bits + 6 status bits
        return 196;
    }

    /**
     * Number of bit errors detected during the decoding process
     */
    @Override
    public abstract int getBitErrorsCount();

    /**
     * Indicates if the data block passes any block level error correction
     */
    public abstract boolean isValid();
}
