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
package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.VendorOSPMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Add Command
 */
public class MotorolaGroupRegroupAddCommand extends VendorOSPMessage
{
    private static final IntField PATCH_GROUP_ADDRESS = IntField.length16(OCTET_2_BIT_16);
    private static final IntField GROUP_ADDRESS_1 = IntField.length16(OCTET_4_BIT_32);
    private static final IntField GROUP_ADDRESS_2 = IntField.length16(OCTET_6_BIT_48);
    private static final IntField GROUP_ADDRESS_3 = IntField.length16(OCTET_8_BIT_64);

    private APCO25PatchGroup mPatchGroup;
    private TalkgroupIdentifier mGroupAddress1;
    private TalkgroupIdentifier mGroupAddress2;
    private TalkgroupIdentifier mGroupAddress3;
    private List<TalkgroupIdentifier> mPatchedTalkgroups;
    private List<Identifier> mIdentifiers;

    public MotorolaGroupRegroupAddCommand(P25P1DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timeslot)
    {
        super(dataUnitID, message, nac, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" PATCH GROUP:").append(getPatchGroup());
        return sb.toString();
    }

    public PatchGroupIdentifier getPatchGroup()
    {
        if(mPatchGroup == null)
        {
            PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(getPatchAddress()));
            patchGroup.addPatchedTalkgroups(getPatchedTalkgroups());
            mPatchGroup = APCO25PatchGroup.create(patchGroup);
        }

        return mPatchGroup;
    }

    private int getPatchAddress()
    {
        return getInt(PATCH_GROUP_ADDRESS);
    }


    /**
     * List of de-duplicated patched talkgroups contained in this message
     */
    public List<TalkgroupIdentifier> getPatchedTalkgroups()
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

    public TalkgroupIdentifier getGroupAddress1()
    {
        if(mGroupAddress1 == null)
        {
            mGroupAddress1 = APCO25Talkgroup.create(getAddress1());
        }

        return mGroupAddress1;
    }

    public boolean hasGroupAddress1()
    {
        return getPatchAddress() != getAddress1();
    }

    private int getAddress1()
    {
        return getInt(GROUP_ADDRESS_1);
    }

    public TalkgroupIdentifier getGroupAddress2()
    {
        if(mGroupAddress2 == null)
        {
            mGroupAddress2 = APCO25Talkgroup.create(getAddress2());
        }

        return mGroupAddress2;
    }

    private int getAddress2()
    {
        return getInt(GROUP_ADDRESS_2);
    }

    public boolean hasGroupAddress2()
    {
        return getPatchAddress() != getAddress2() && getAddress1() != getAddress2();
    }

    public TalkgroupIdentifier getGroupAddress3()
    {
        if(mGroupAddress3 == null)
        {
            mGroupAddress3 = APCO25Talkgroup.create(getAddress3());
        }

        return mGroupAddress3;
    }

    private int getAddress3()
    {
        return getInt(GROUP_ADDRESS_3);
    }

    public boolean hasGroupAddress3()
    {
        return getPatchAddress() != getAddress3() && getAddress1() != getAddress3() && getAddress2() != getAddress3();
    }

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
