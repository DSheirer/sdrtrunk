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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * MAC release / call or talker preemption
 */
public class MacRelease extends MacStructure
{
    private static final int UNFORCED_FORCED_FLAG = 8;
    private static final int CALL_AUDIO_FLAG = 9;
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_3_BIT_16);
    private static final IntField COLOR_CODE = IntField.length12(OCTET_6_BIT_40 + 4);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private Identifier mNac;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MacRelease(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" NAC:").append(getNac());
        if(isForcedPreemption())
        {
            sb.append(" FORCED");
        }
        else
        {
            sb.append(" UNFORCED");
        }

        if(isTalkerPreemption())
        {
            sb.append(" TALKER");
        }
        else
        {
            sb.append(" CALL");
        }

        sb.append(" PREEMPTION");

        return sb.toString();
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    public Identifier getNac()
    {
        if(mNac == null)
        {
            mNac = APCO25Nac.create(getInt(COLOR_CODE));
        }

        return mNac;
    }

    /**
     * Indicates if this is a forced (true) or unforced (false) preemption.
     */
    public boolean isForcedPreemption()
    {
        return getMessage().get(UNFORCED_FORCED_FLAG + getOffset());
    }

    /**
     * Indicates if the call is a talker preemption (true) or call preemption (false) where a talker preemption means
     * the current talker is preempted, but the current talkgroup and call will continue.
     */
    public boolean isTalkerPreemption()
    {
        return getMessage().get(CALL_AUDIO_FLAG + getOffset());
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getNac());
        }

        return mIdentifiers;
    }
}
