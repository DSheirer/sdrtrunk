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
import io.github.dsheirer.module.decode.p25.phase2.LinearFeedbackShiftRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * APCO-25 Phase II scrambling sequence utility
 */
public class ScramblingSequence
{
    private LinearFeedbackShiftRegister mShiftRegister = new LinearFeedbackShiftRegister();
    private List<BinaryMessage> mScramblingSegments = new ArrayList<>();

    public ScramblingSequence()
    {
        for(int x = 0; x < 12; x++)
        {
            mScramblingSegments.add(new BinaryMessage(320));
        }
    }

    public void update(int wacn, int system, int nac)
    {
        if(!mShiftRegister.isCurrent(wacn, system, nac))
        {
            mScramblingSegments.clear();

            BinaryMessage scramblingSequence = mShiftRegister.generateScramblingSequence(wacn, system, nac);

            for(int x = 40; x < 4320; x += 360)
            {
                mScramblingSegments.add(scramblingSequence.getSubMessage(x, x + 320));
            }
        }
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
