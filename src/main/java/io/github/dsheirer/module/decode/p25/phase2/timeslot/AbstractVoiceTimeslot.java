/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jmbe.frame.CorrectedAMBEFrame;

public abstract class AbstractVoiceTimeslot extends Timeslot
{
    protected AbstractVoiceTimeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID,
                                    BinaryMessage scramblingSequence, int timeslot, long timestamp)
    {
        super(message, dataUnitID, scramblingSequence, timeslot, timestamp);
    }

    private List<CorrectedAMBEFrame> mCorrectedVoiceFrames;

    public abstract List<BinaryMessage> getVoiceFrames();

    /**
     * Error corrected versions of the AMBE voice frames from getVoiceFrames(), in the same order.  The frames are
     * error corrected once and cached.
     */
    public synchronized List<CorrectedAMBEFrame> getCorrectedVoiceFrames()
    {
        if(mCorrectedVoiceFrames == null)
        {
            List<CorrectedAMBEFrame> corrected = new ArrayList<>();

            for(BinaryMessage frame: getVoiceFrames())
            {
                corrected.add(CorrectedAMBEFrame.correct(frame.getBytes()));
            }

            mCorrectedVoiceFrames = corrected;
        }

        return mCorrectedVoiceFrames;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" ").append(getDataUnitID().toString());

        for(int x = 0; x < getVoiceFrames().size(); x++)
        {
            sb.append(" ").append(x).append(":").append(getVoiceFrames().get(x).toHexString());
        }
        return sb.toString();

    }
}
