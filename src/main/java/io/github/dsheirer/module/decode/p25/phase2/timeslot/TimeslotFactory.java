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
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;

/**
 * Timeslot factory.  Constructs timeslot parsing implementations according to the timeslot's data unit ID
 */
public class TimeslotFactory
{
    /**
     * Creates a timeslot parser instance
     *
     * @param message containing 320-bit timeslot data
     * @param scramblingSequence to unscramble the transmitted message
     * @param timeslot to retrieve
     * @param timestamp for the message
     * @return timeslot parser
     */
    public static Timeslot getTimeslot(CorrectedBinaryMessage message, BinaryMessage scramblingSequence,
                                       int timeslot, long timestamp)
    {
        DataUnitID dataUnitID = Timeslot.getDuid(message);

        switch(dataUnitID)
        {
            case VOICE_4:
                return new Voice4Timeslot(message, scramblingSequence, timeslot, timestamp);
            case VOICE_2:
                return new Voice2Timeslot(message, scramblingSequence, timeslot, timestamp);
            case SCRAMBLED_FACCH:
                return new FacchTimeslot(message, scramblingSequence, timeslot, timestamp);
            case SCRAMBLED_SACCH:
                return new SacchTimeslot(message, scramblingSequence, timeslot, timestamp);
            case SCRAMBLED_DATCH:
                return new DatchTimeslot(message, scramblingSequence, timeslot, timestamp);
            case UNSCRAMBLED_FACCH:
                return new FacchTimeslot(message, timeslot, timestamp);
            case UNSCRAMBLED_SACCH:
                return new SacchTimeslot(message, timeslot, timestamp);
            case UNSCRAMBLED_LCCH:
                return new LcchTimeslot(message, timeslot, timestamp);
            default:
                return new UnknownTimeslot(message, timeslot, timestamp, dataUnitID);
        }
    }
}
