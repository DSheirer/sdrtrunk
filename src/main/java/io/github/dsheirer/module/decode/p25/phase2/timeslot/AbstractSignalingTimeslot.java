/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;

/**
 * Abstract class for FACCH and SACCH signaling bearer messages
 */
public abstract class AbstractSignalingTimeslot extends Timeslot
{
    /**
     * Constructs a scrambled signaling timeslot
     *
     * @param message with scrambled timeslot data
     * @param dataUnitID for the timeslot
     * @param scramblingSequence to unscramble the message
     */
    protected AbstractSignalingTimeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID, BinaryMessage scramblingSequence)
    {
        super(message, dataUnitID, scramblingSequence);
    }

    /**
     * Constructs an un-scrambled signaling timeslot
     *
     * @param message that is un-scrambled
     * @param dataUnitID for the timeslot
     */
    protected AbstractSignalingTimeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID)
    {
        super(message, dataUnitID);
    }

    /**
     * Access Encoded MAC Information (EMI) message carried by the signalling timeslot
     */
    public abstract MacMessage getMacMessage();
}
