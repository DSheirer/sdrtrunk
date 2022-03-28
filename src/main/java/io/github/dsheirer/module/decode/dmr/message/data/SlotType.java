/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Golay24;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;

public class SlotType
{
    private static final int[] MESSAGE_INDEXES = new int[]{122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 180, 181,
        182, 183, 184, 185, 186, 187, 188, 189};

    //Note: extracted message bit index values are offset by +4 from the ICD specified index values
    private static final int[] COLOR_CODE = new int[]{4, 5, 6, 7};
    private static final int[] DATA_TYPE = new int[]{8, 9, 10, 11};

    private CorrectedBinaryMessage mMessage;
    private boolean mValid;

    public SlotType(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getColorCode());
        sb.append(" ").append(getDataType());
        return sb.toString();
    }

    /**
     * Binary message bits
     */
    private CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Sets the valid flag for this slot type.
     *
     * @param valid
     */
    void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Indicates if this message was successfully error detected and corrected
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * SlotType message extracted from the transmitted message and Golay(24) error detection and correction complete.
     *
     * Note: this message is extracted with 4 leading/padding bits plus the 20 message bits for an overall length of
     * 24 bits to support use of the existing Golay24 error correction utility.  All bit index values are incremented
     * by an offset of 4 to account for this frame offset.
     */
    public static SlotType getSlotType(BinaryMessage message)
    {
        CorrectedBinaryMessage decodedMessage = new CorrectedBinaryMessage(24);

        for(int x = 0; x < MESSAGE_INDEXES.length; x++)
        {
            if(message.get(MESSAGE_INDEXES[x]))
            {
                decodedMessage.set(x + 4);
            }
        }

        int errorCount = Golay24.checkAndCorrect(decodedMessage, 0);
        decodedMessage.setCorrectedBitCount(errorCount);
        SlotType slotType = new SlotType(decodedMessage);
        slotType.setValid(errorCount < 3);
        return slotType;
    }

    /**
     * Color code for this timeslot.
     *
     * @return color code: 0 - 15
     */
    public int getColorCode()
    {
        return getMessage().getInt(COLOR_CODE);
    }

    /**
     * Data Type for this data message
     */
    public DataType getDataType()
    {
        return DataType.fromValue(getMessage().getInt(DATA_TYPE));
    }
}