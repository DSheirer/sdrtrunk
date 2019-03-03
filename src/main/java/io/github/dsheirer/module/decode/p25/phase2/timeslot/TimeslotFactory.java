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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;

/**
 * Timeslot factory.  Constructs timeslot parsing implementations according to the timeslot's data unit ID
 */
public class TimeslotFactory
{
    /**
     * Creates a timeslot parser instance
     * @param message containing 320-bit timeslot data
     * @return timeslot parser
     */
    public static Timeslot getTimeslot(CorrectedBinaryMessage message)
    {
        DataUnitID dataUnitID = Timeslot.getDuid(message);

        switch(dataUnitID)
        {
            case VOICE_4:
                return new Voice4Timeslot(message);
            case VOICE_2:
                return new Voice2Timeslot(message);
            case SCRAMBLED_SACCH:
                return new ScrambledSacchTimeslot(message);
            case SCRAMBLED_FACCH:
                return new ScrambledFacchTimeslot(message);
            case UNSCRAMBLED_SACCH:
                return new UnScrambledSacchTimeslot(message);
            case UNSCRAMBLED_FACCH:
                return new UnScrambledFacchTimeslot(message);
            case UNKNOWN:
            default:
                return new UnknownTimeslot(message);
        }
    }
}
