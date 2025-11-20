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

package io.github.dsheirer.module.decode.nxdn.layer3.coding;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Puncture provider base implementation
 */
public abstract class PunctureProvider
{
    private final int mPunctureBitCount;
    private final int mPunctureBlockSize;

    /**
     * Constructs an instance
     * @param punctureBlockSize block size in bits for puncturing
     * @param punctureBitCount count of bits that will be punctured -- should be less than the block size
     */
    public PunctureProvider(int punctureBlockSize, int punctureBitCount)
    {
        mPunctureBlockSize = punctureBlockSize;
        mPunctureBitCount = punctureBitCount;
    }

    public String visualize(CorrectedBinaryMessage cbm)
    {
        StringBuilder sb = new StringBuilder();
        for(int x = 0; x < cbm.size(); x++)
        {
            if(isPunctured(x))
            {
                sb.append(".");
            }
            else
            {
                sb.append(cbm.get(x) ? "1" : "0");
            }
        }

        return sb.toString();
    }

    /**
     * Indicates if the original message index is preserved (ie not punctured).
     * @param index to test
     * @return true if the message index is preserved
     */
    public abstract boolean isPreserved(int index);

    public abstract boolean isPunctured(int index);

    /**
     * Punctures the original message
     * @param original message to be punctured
     * @return punctured message
     */
    public CorrectedBinaryMessage puncture(CorrectedBinaryMessage original)
    {
        CorrectedBinaryMessage punctured = new CorrectedBinaryMessage(calculatePuncturedLength(original.length()));

        int puncturedPointer = 0;

        for(int index = 0; index < original.length(); index++)
        {
            if(isPreserved(index))
            {
                punctured.set(puncturedPointer++, original.get(index));
            }
        }

        return punctured;
    }

    /**
     * De-punctures the punctured message
     * @param punctured message to be de-punctured
     * @return punctured message
     */
    public CorrectedBinaryMessage depuncture(CorrectedBinaryMessage punctured)
    {
        int depunctureLength = calculateDepuncturedLength(punctured.size());

        CorrectedBinaryMessage depunctured = new CorrectedBinaryMessage(depunctureLength);

        int depuncturedPointer = 0;

        for(int puncturedPointer = 0; puncturedPointer < punctured.size(); puncturedPointer++)
        {
            depunctured.set(depuncturedPointer++, punctured.get(puncturedPointer));

            if(isPunctured(depuncturedPointer))
            {
                depuncturedPointer++;
            }
        }

        return depunctured;
    }

    /**
     * Calculates the length of punctured message from the original depunctured length.
     * @param depunctured message length
     * @return punctured message length.
     */
    private int calculatePuncturedLength(int depunctured)
    {
        int groups = depunctured / mPunctureBlockSize;
        int residual = depunctured - (groups * mPunctureBlockSize);
        return groups * (mPunctureBlockSize - mPunctureBitCount) + residual;
    }

    /**
     * Calculates the length of the depunctured message from the original punctured length.
     * @param punctured message length
     * @return depunctured message length.
     */
    private int calculateDepuncturedLength(int punctured)
    {
        int reducedBlockSize = mPunctureBlockSize - mPunctureBitCount;
        int groups = punctured / reducedBlockSize;
        int residual = punctured - (groups * reducedBlockSize);
        return groups * mPunctureBlockSize + residual;
    }
}
