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
package io.github.dsheirer.bits;

import java.util.BitSet;

public class CorrectedBinaryMessage extends BinaryMessage
{
    private int mCorrectedBitCount;

    /**
     * Subclass of binary message class to allow capturing a corrected bits metric.
     * @param size of message to create.
     */
    public CorrectedBinaryMessage(int size)
    {
        super(size);
    }

    public CorrectedBinaryMessage(int size, boolean[] bitsToPreload)
    {
        super(size, bitsToPreload);
    }

    public CorrectedBinaryMessage(BitSet bitset, int size)
    {
        super(bitset, size);
    }

    public CorrectedBinaryMessage(byte[] data)
    {
        super(data);
    }

    public CorrectedBinaryMessage(BinaryMessage message)
    {
        this(message.size());
        this.xor(message);
    }

    @Override
    public int getCorrectedBitCount()
    {
        return mCorrectedBitCount;
    }

    /**
     * Sets the number of bits that were corrected in this message
     * @param count of corrected bits
     */
    public void setCorrectedBitCount(int count)
    {
        mCorrectedBitCount = count;
    }

    /**
     * Increases the current corrected bit count by the argument value.
     * @param additionalCount to add to the current corrected bit count
     */
    public void incrementCorrectedBitCount(int additionalCount)
    {
        mCorrectedBitCount += additionalCount;
    }

    /**
     * Returns a new binary message containing the bits from (inclusive) to end (exclusive).
     *
     * @param start bit inclusive
     * @param end bit exclusive
     * @return message with sub message bits
     */
    public CorrectedBinaryMessage getSubMessage(int start, int end)
    {
        BitSet subset = this.get(start, end);
        return new CorrectedBinaryMessage(subset, end - start);
    }
}
