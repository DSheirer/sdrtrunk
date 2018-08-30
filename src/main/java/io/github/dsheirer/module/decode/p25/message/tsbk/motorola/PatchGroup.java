/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk.motorola;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public abstract class PatchGroup extends MotorolaTSBKMessage
{
    public static final int[] PATCH_GROUP_ADDRESS = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] GROUP_ADDRESS_1 = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] GROUP_ADDRESS_2 = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};
    public static final int[] GROUP_ADDRESS_3 = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mPatchGroupAddress;
    private IIdentifier mGroupAddress1;
    private IIdentifier mGroupAddress2;
    private IIdentifier mGroupAddress3;
    private List<IIdentifier> mPatchedTalkgroups;

    public PatchGroup(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ");
        sb.append(getPatchGroupAddress());
        sb.append(" ");
        sb.append(getPatchedTalkgroups());

        return sb.toString();
    }

    public IIdentifier getPatchGroupAddress()
    {
        if(mPatchGroupAddress == null)
        {
            mPatchGroupAddress = APCO25ToTalkgroup.createGroup(mMessage.getInt(PATCH_GROUP_ADDRESS));
        }

        return mPatchGroupAddress;
    }

    /**
     * List of de-deplicated patched talkgroups contained in this message
     */
    public List<IIdentifier> getPatchedTalkgroups()
    {
        if(mPatchedTalkgroups == null)
        {
            mPatchedTalkgroups = new ArrayList<>();

            mPatchedTalkgroups.add(getGroupAddress1());

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
            mGroupAddress1 = APCO25ToTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_1));
        }

        return mGroupAddress1;
    }

    public IIdentifier getGroupAddress2()
    {
        if(mGroupAddress2 == null)
        {
            mGroupAddress2 = APCO25ToTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_2));
        }

        return mGroupAddress2;
    }

    public boolean hasGroupAddress2()
    {
        return ((APCO25Talkgroup)getGroupAddress2()).getValue() != 0 && !getGroupAddress1().equals(getGroupAddress2());
    }

    public IIdentifier getGroupAddress3()
    {
        if(mGroupAddress3 == null)
        {
            mGroupAddress3 = APCO25ToTalkgroup.createGroup(mMessage.getInt(GROUP_ADDRESS_3));
        }

        return mGroupAddress3;
    }

    public boolean hasGroupAddress3()
    {
        return ((APCO25Talkgroup)getGroupAddress3()).getValue() != 0 &&
            !getGroupAddress3().equals(getGroupAddress1()) &&
            !getGroupAddress3().equals(getGroupAddress2());
    }
}
