/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25PatchGroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public class PatchGroupAdd extends OSPMessage
{
    public static final int[] PATCH_GROUP_ADDRESS = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    public static final int[] GROUP_ADDRESS_1 = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    public static final int[] GROUP_ADDRESS_2 = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    public static final int[] GROUP_ADDRESS_3 = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private APCO25PatchGroup mPatchGroupAddress;
    private IIdentifier mGroupAddress1;
    private IIdentifier mGroupAddress2;
    private IIdentifier mGroupAddress3;
    private List<IIdentifier> mPatchedTalkgroups;
    private List<IIdentifier> mIdentifiers;

    public PatchGroupAdd(DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timeslot)
    {
        super(dataUnitID, message, nac, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" PATCH GROUP:").append(getPatchGroupAddress());
        return sb.toString();
    }

    public IIdentifier getPatchGroupAddress()
    {
        if(mPatchGroupAddress == null)
        {
            mPatchGroupAddress = APCO25PatchGroup.create(getMessage().getInt(PATCH_GROUP_ADDRESS));
            mPatchGroupAddress.addPatchedGroups(getPatchedTalkgroups());
        }

        return mPatchGroupAddress;
    }

    /**
     * List of de-duplicated patched talkgroups contained in this message
     */
    public List<IIdentifier> getPatchedTalkgroups()
    {
        if(mPatchedTalkgroups == null)
        {
            mPatchedTalkgroups = new ArrayList<>();

            if(hasGroupAddress1())
            {
                mPatchedTalkgroups.add(getGroupAddress1());
            }

            if(hasGroupAddress2())
            {
                mPatchedTalkgroups.add(getGroupAddress2());
            }

            if(hasGroupAddress3())
            {
                mPatchedTalkgroups.add(getGroupAddress3());
            }
        }

        return mPatchedTalkgroups;
    }

    public IIdentifier getGroupAddress1()
    {
        if(mGroupAddress1 == null)
        {
            mGroupAddress1 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_1));
        }

        return mGroupAddress1;
    }

    public boolean hasGroupAddress1()
    {
        return getMessage().getInt(GROUP_ADDRESS_1) != 0 &&
            getMessage().getInt(GROUP_ADDRESS_1) != getMessage().getInt(PATCH_GROUP_ADDRESS);
    }

    public IIdentifier getGroupAddress2()
    {
        if(mGroupAddress2 == null)
        {
            mGroupAddress2 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_2));
        }

        return mGroupAddress2;
    }

    public boolean hasGroupAddress2()
    {
        return getMessage().getInt(GROUP_ADDRESS_2) != 0 &&
            getMessage().getInt(GROUP_ADDRESS_1) != getMessage().getInt(GROUP_ADDRESS_2);
    }

    public IIdentifier getGroupAddress3()
    {
        if(mGroupAddress3 == null)
        {
            mGroupAddress3 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_3));
        }

        return mGroupAddress3;
    }

    public boolean hasGroupAddress3()
    {
        return getMessage().getInt(GROUP_ADDRESS_3) != 0 &&
            getMessage().getInt(GROUP_ADDRESS_1) != getMessage().getInt(GROUP_ADDRESS_3) &&
            getMessage().getInt(GROUP_ADDRESS_2) != getMessage().getInt(GROUP_ADDRESS_3);
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchGroupAddress());
        }

        return mIdentifiers;
    }
}
