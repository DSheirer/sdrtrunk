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

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;

/**
 * Base timeslot class.
 */
public abstract class Timeslot extends P25P2Message
{
    private static final FragmentedIntField DATA_UNIT_ID = FragmentedIntField.of(0,1,74,75,244,245,318,319);
    private DataUnitID mDataUnitID;

    /**
     * Constructs a scrambled timeslot instance and automatically descrambles the transmitted bits.
     * @param message containing transmitted bits and bit error count
     * @param dataUnitID that identifies this timeslot
     * @param scramblingSequence to descramble this timeslot
     * @param timeslot or timeslot 0 or 1
     * @param timestamp the message was received
     */
    protected Timeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID, BinaryMessage scramblingSequence,
                       int timeslot, long timestamp)
    {
        this(message, dataUnitID, timeslot, timestamp);
        getMessage().xor(scramblingSequence);
    }

    /**
     * Constructs an unscrambled timeslot instance.
     * @param message containing transmitted bits and bit error count
     * @param dataUnitID that identifies this timeslot
     * @param timeslot or timeslot 0 or 1
     * @param timestamp the message was received
     */
    protected Timeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID, int timeslot, long timestamp)
    {
        super(message, 0, timeslot, timestamp);
        mDataUnitID = dataUnitID;
    }

    public DataUnitID getDataUnitID()
    {
        return mDataUnitID;
    }

    /**
     * Lookup the Data Unit ID for this timeslot
     * @param message containing a 320-bit timeslot frame with interleaved 8-bit duid value.
     * @return data unit id or the id with the closest hamming distance to the decoded value.
     */
    public static DataUnitID getDuid(CorrectedBinaryMessage message)
    {
        return DataUnitID.fromEncodedValue(message.getInt(DATA_UNIT_ID));
    }

    public String toString()
    {
        return getDataUnitID().toString();
    }
}
