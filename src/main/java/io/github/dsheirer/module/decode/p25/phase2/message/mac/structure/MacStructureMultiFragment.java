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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for a multi-fragment message.  The multi-fragment message has a data length field that indicates the total
 * byte length of this message and any MultiFragmentContinuationMessages.
 */
public abstract class MacStructureMultiFragment extends MacStructureVariableLength
{
    private static final IntField DATA_LENGTH = IntField.length8(OCTET_3_BIT_16);
    private List<MultiFragmentContinuationMessage> mContinuationMessages = new ArrayList<>();

    /**
     * Constructs a MAC structure parser
     *
     * @param message containing a MAC structure
     * @param offset in the message to the start of the structure
     */
    protected MacStructureMultiFragment(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Total length of the multi-fragment message that spans multiple mac structures.  Does not include the first two
     * octets of each fragment that carries the OPCODE and LENGTH fields in the overall count.
     * @param message containing one or more mac structures
     * @param offset to the start of the mac structure
     * @return data length for the multi-fragment message
     */
    public static int getDataLength(CorrectedBinaryMessage message, int offset)
    {
        return message.getInt(DATA_LENGTH, offset);
    }

    /**
     * Total length of this multi-fragment message
     * @return data length for this multi-fragment message
     */
    public int getDataLength()
    {
        return getInt(DATA_LENGTH);
    }

    /**
     * Adds the continuation message to this base message.
     * @param continuationMessage to add.
     */
    public void addContinuationMessage(MultiFragmentContinuationMessage continuationMessage)
    {
        mContinuationMessages.add(continuationMessage);
    }

    /**
     * Access the continuation message
     * @param index of the continuation message
     * @return continuation message if it exists, or null.
     */
    protected MultiFragmentContinuationMessage getFragment(int index)
    {
        if(hasFragment(index))
        {
            return mContinuationMessages.get(index);
        }

        return null;
    }

    /**
     * Indicates if this multi-fragment message is completely assembled with continuation messages such that the
     * combined octet length of all messages (minus first 2 octets each) is less than or equal to the data length
     * specified in the base message.
     * @return true if this message has all of its continuation fragments.
     */
    public boolean isComplete()
    {
        int dataLength = getDataLength();

        //Data length does not include first 2 octest of each message - subtract 2 from the length of each message.
        int currentLength = getLength() - 2;
        for(MultiFragmentContinuationMessage continuationMessage: mContinuationMessages)
        {
            currentLength += (continuationMessage.getLength() - 2);
        }

        return dataLength <= currentLength;
    }

    /**
     * Indicates if this message contains the fragment continuation message referenced by the index argument.
     * @param index to test for existence.
     * @return true if it exists.
     */
    protected boolean hasFragment(int index)
    {
        return index < mContinuationMessages.size();
    }
}
