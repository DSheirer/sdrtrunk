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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import java.util.List;

/**
 * Motorola TDMA Data Channel (DATCH) timeslot.  Is probably used with APX-NEXT and MDT equipment.
 *
 * Analysis:
 * - It seems to use the standard P25 Phase 2 scrambling sequence.
 * - The overall signalling doesn't appear to be encrypted because the data does not appear randomized.  There's a lot
 *   of repeated messaging with just a few bits changing.
 * - It doesn't seem to be RS encoded data, or the first nibble is not protected in the RS encoding because the overall
 *   message doesn't change with variations in the first nibble, when there's repeating messages.
 * - First 4 bits follow a pattern in repeating messages across both timeslots where the first 2 bits follows the pattern
 *   and the 3rd and 4th bits are set across sequences of 3 or 4 messages spanning both timeslots.
 * - Not sure if the timeslots are independent of each other, or if they are strapped.
 */
public class DatchTimeslot extends Timeslot
{
    /**
     * Constructs a scrambled DATCH timeslot
     *
     * @param message containing 320 scrambled bits for the timeslot
     * @param scramblingSequence to descramble the message
     * @param timeslot of the message
     * @param timestamp of the message
     */
    public DatchTimeslot(CorrectedBinaryMessage message, BinaryMessage scramblingSequence, int timeslot,
                         long timestamp)
    {
        super(message, DataUnitID.SCRAMBLED_DATCH, scramblingSequence, timeslot, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" DATCH-SC TDMA DATA TIMESLOT MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
