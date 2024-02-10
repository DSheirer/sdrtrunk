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
import java.util.ArrayList;
import java.util.List;

/**
 * Timeslot containing two voice frames and Encryption Sync Signalling (ESS-A) fragment.
 */
public class Voice2Timeslot extends AbstractVoiceTimeslot
{
    private static final int FRAME_LENGTH = 72;
    private static final int FRAME_1_START = 2;
    private static final int FRAME_2_START = 76;
    private static final int ESS_A1_START = 148;
    private static final int ESS_A1_LENGTH = 96;
    private static final int ESS_A2_START = 246;
    private static final int ESS_A2_LENGTH = 72;

    private List<BinaryMessage> mVoiceFrames;
    private BinaryMessage mEssA;

    /**
     * Constructs a 2-Voice timeslot
     * @param message containing 320 scrambled bits for the timeslot
     * @param scramblingSequence to de-scramble the message
     * @param timeslot for the message
     * @param timestamp of the last transmitted bit
     */
    public Voice2Timeslot(CorrectedBinaryMessage message, BinaryMessage scramblingSequence, int timeslot,
                          long timestamp)
    {
        super(message, DataUnitID.VOICE_2, scramblingSequence, timeslot, timestamp);
    }

    /**
     * Voice frames contained in this timeslot
     */
    public synchronized List<BinaryMessage> getVoiceFrames()
    {
        if(mVoiceFrames == null)
        {
            mVoiceFrames = new ArrayList<>();
            mVoiceFrames.add(getMessage().getSubMessage(FRAME_1_START, FRAME_1_START + FRAME_LENGTH));
            mVoiceFrames.add(getMessage().getSubMessage(FRAME_2_START, FRAME_2_START + FRAME_LENGTH));
            return mVoiceFrames;
        }

        return mVoiceFrames;
    }

    /**
     * Encryption Synchronization Signaling (ESS-A) segment
     */
    public BinaryMessage getEssA()
    {
        if(mEssA == null)
        {
            BinaryMessage segment1 = getMessage().getSubMessage(ESS_A1_START, ESS_A1_START + ESS_A1_LENGTH);
            BinaryMessage segment2 = getMessage().getSubMessage(ESS_A2_START, ESS_A2_START + ESS_A2_LENGTH);
            mEssA = new BinaryMessage(ESS_A1_LENGTH + ESS_A2_LENGTH);
            mEssA.load(0, segment1);
            mEssA.load(ESS_A1_LENGTH, segment2);
        }

        return mEssA;
    }
}
