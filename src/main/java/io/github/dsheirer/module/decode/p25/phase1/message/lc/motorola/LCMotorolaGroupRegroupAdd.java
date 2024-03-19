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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Add
 */
public class LCMotorolaGroupRegroupAdd extends LinkControlWord
{
    private static final IntField SUPERGROUP = IntField.length16(OCTET_2_BIT_16);
    private static final IntField PATCHED_GROUP_1 = IntField.length16(OCTET_4_BIT_32);
    private static final IntField PATCHED_GROUP_2 = IntField.length16(OCTET_6_BIT_48);

    private APCO25PatchGroup mPatchGroup;
    private TalkgroupIdentifier mPatchedGroup1;
    private TalkgroupIdentifier mPatchedGroup2;
    private List<Identifier> mIdentifiers;

    public LCMotorolaGroupRegroupAdd(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP REGROUP ADD SUPERGROUP:").append(getPatchGroup());
        return sb.toString();
    }

    /**
     * Patch Group
     */
    public Identifier getPatchGroup()
    {
        if(mPatchGroup == null)
        {
            PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(getInt(SUPERGROUP)));
            patchGroup.addPatchedTalkgroups(getPatchedGroups());
            mPatchGroup = APCO25PatchGroup.create(patchGroup);
        }

        return mPatchGroup;
    }

    public List<TalkgroupIdentifier> getPatchedGroups()
    {
        List<TalkgroupIdentifier> patchedGroups = new ArrayList<>();

        if(hasPatchedGroup1())
        {
            patchedGroups.add(getPatchedGroup1());
        }

        if(hasPatchedGroup2())
        {
            patchedGroups.add(getPatchedGroup2());
        }

        return patchedGroups;
    }

    /**
     * Patched Group 1
     */
    public TalkgroupIdentifier getPatchedGroup1()
    {
        if(mPatchedGroup1 == null)
        {
            mPatchedGroup1 = APCO25Talkgroup.create(getInt(PATCHED_GROUP_1));
        }

        return mPatchedGroup1;
    }

    public boolean hasPatchedGroup1()
    {
        return getInt(PATCHED_GROUP_1) != 0 &&
            (getInt(SUPERGROUP) != getInt(PATCHED_GROUP_1));
    }

    /**
     * Patched Group 2
     */
    public TalkgroupIdentifier getPatchedGroup2()
    {
        if(mPatchedGroup2 == null)
        {
            mPatchedGroup2 = APCO25Talkgroup.create(getInt(PATCHED_GROUP_2));
        }

        return mPatchedGroup2;
    }

    public boolean hasPatchedGroup2()
    {
        return getInt(PATCHED_GROUP_2) != 0 && (getInt(SUPERGROUP) != getInt(PATCHED_GROUP_2));
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
            mIdentifiers.add(getPatchGroup());
        }

        return mIdentifiers;
    }
}
