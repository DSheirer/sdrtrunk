/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Current user of an APCO25 channel on both inbound and outbound channels
 */
public class LCUnitToUnitVoiceChannelUser extends LinkControlWord
{
    private static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] TARGET_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43,
            44, 45, 46, 47};
    private static final int[] SOURCE_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
            65, 66, 67, 68, 69, 70, 71};

    private VoiceServiceOptions mVoiceServiceOptions;
    private Identifier mTargetAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCUnitToUnitVoiceChannelUser(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" ").append(getVoiceServiceOptions());
        return sb.toString();
    }

    /**
     * Service Options for this channel
     */
    public VoiceServiceOptions getVoiceServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    /**
     * Talkgroup address
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * Source address
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
