/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Call end.
 *
 * Similar to a P25 Phase 1 Terminator Data Unit (TDU).
 */
public class EndPushToTalk extends MacStructure
{
    private static final int SYSTEM_CONTROLLER = 16777215;
    private static int[] COLOR_CODE = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static int[] SOURCE_ADDRESS = {104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
        119, 120, 121, 122, 123, 124, 125, 126, 127};
    private static int[] GROUP_ADDRESS = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private Identifier mColorCode;
    private Identifier mSourceAddress;
    private Identifier mGroupAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     */
    public EndPushToTalk(CorrectedBinaryMessage message)
    {
        super(message, 0);
    }

    @Override
    public MacOpcode getOpcode()
    {
        return MacOpcode.END_PUSH_TO_TALK;
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FM:").append(getSourceAddress());
        sb.append(" TO:").append(getGroupAddress());
        sb.append(" NAC:").append(getNAC());

        return sb.toString();
    }

    /**
     * NAC or Color Code
     */
    public Identifier getNAC()
    {
        if(mColorCode == null)
        {
            mColorCode = APCO25Nac.create(getMessage().getInt(COLOR_CODE, getOffset()));
        }

        return mColorCode;
    }

    /**
     * Calling (source) radio identifier
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceAddress;
    }

    /**
     * Called (destination) group identifier
     */
    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.create(getMessage().getInt(GROUP_ADDRESS, getOffset()));
        }

        return mGroupAddress;
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
            mIdentifiers.add(getSourceAddress());
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getNAC());
        }

        return mIdentifiers;
    }
}
