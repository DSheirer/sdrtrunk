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
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO-25 Phase II scrambling sequence utility that provides scrambling sequence snippets for each of the 12 timeslots
 * in a 12-timeslot super frame.
 */
public class ScramblingSequence
{
    private final static Logger mLog = LoggerFactory.getLogger(ScramblingSequence.class);

    private LinearFeedbackShiftRegister mShiftRegister = new LinearFeedbackShiftRegister();
    private List<BinaryMessage> mScramblingSegments = new ArrayList<>();

    /**
     * Constructs an instance
     */
    public ScramblingSequence()
    {
        for(int x = 0; x < 12; x++)
        {
            mScramblingSegments.add(new BinaryMessage(320));
        }
    }

    /**
     * Updates this scrambling sequence with the specified seed parameters
     */
    public boolean update(ScrambleParameters parameters)
    {
        if(parameters != null)
        {
            return update(parameters.getWACN(), parameters.getSystem(), parameters.getNAC());
        }

        return false;
    }

    /**
     * Updates this scrambling sequence with the specified parameters from the Network Broadcast Status message and
     * generates 12 x 320-bit scrambling sequences for each of the superframe's 12 timeslots.
     */
    public boolean update(int wacn, int system, int nac)
    {
        if(!mShiftRegister.isCurrent(wacn, system, nac))
        {
            mScramblingSegments.clear();

            BinaryMessage scramblingSequence = mShiftRegister.generateScramblingSequence(wacn, system, nac);

            //Note: the scrambling sequence starts at halfway through the first ISCH of the superframe, so we start
            //chopping the LFSR sequence using 320 of each 360 bits starting at bit 20 of 40 of the first ISCH.
            for(int x = 20; x < 4320; x += 360)
            {
                mScramblingSegments.add(scramblingSequence.getSubMessage(x, x + 320));
            }

            //Return true to indicate that the sequence was updated
            return true;
        }

        return false;
    }

    /**
     * Accesses the scrambling sequence for the specified timeslot index
     * @param timeslot 0 - 11
     * @return scrambling sequence (320-bits) for the specified timeslot
     */
    public BinaryMessage getTimeslotSequence(int timeslot)
    {
        if(0 <= timeslot && timeslot <= 11)
        {
            return mScramblingSegments.get(timeslot);
        }

        throw new IllegalArgumentException("Unrecognized timeslot index: " + timeslot);
    }
}
