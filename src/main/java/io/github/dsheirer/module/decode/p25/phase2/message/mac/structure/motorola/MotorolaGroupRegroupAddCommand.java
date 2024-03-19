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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Add Command.
 */
public class MotorolaGroupRegroupAddCommand extends MacStructureVendor
{
    private static final IntField SUPERGROUP_ADDRESS = IntField.length16(OCTET_4_BIT_24);
    private static final IntField TALKGROUP_1 = IntField.length16(OCTET_6_BIT_40);
    private static final IntField TALKGROUP_2 = IntField.length16(OCTET_8_BIT_56);
    private static final IntField TALKGROUP_3 = IntField.length16(OCTET_10_BIT_72);
    private static final IntField TALKGROUP_4 = IntField.length16(OCTET_12_BIT_88);
    private static final IntField TALKGROUP_5 = IntField.length16(OCTET_14_BIT_104);
    private static final IntField TALKGROUP_6 = IntField.length16(OCTET_16_BIT_120);
    private PatchGroupIdentifier mPatchGroupIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupAddCommand(CorrectedBinaryMessage message, int offset)
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
        sb.append(" PATCHGROUP:").append(getPatchGroup());
        return sb.toString();
    }

    /**
     * Supergroup/Patch group referenced by this message including any patched talkgroups or individual radio identifiers.
     * @return patch group.
     */
    public PatchGroupIdentifier getPatchGroup()
    {
        if(mPatchGroupIdentifier == null)
        {
            TalkgroupIdentifier patchGroupId = APCO25Talkgroup.create(getInt(SUPERGROUP_ADDRESS));
            PatchGroup patchGroup = new PatchGroup(patchGroupId, 0);

            int tg1 = getInt(TALKGROUP_1);
            if(tg1 > 0)
            {
                patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(tg1));
            }

            int length = getLength();

            if(length >= 9)
            {
                int tg2 = getInt(TALKGROUP_2);
                if(tg2 > 0)
                {
                    patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(tg2));
                }
            }

            if(length >= 11)
            {
                int tg3 = getInt(TALKGROUP_3);
                if(tg3 > 0)
                {
                    patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(tg3));
                }
            }

            if(length >= 13)
            {
                int tg4 = getInt(TALKGROUP_4);
                if(tg4 > 0)
                {
                    patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(tg4));
                }
            }

            if(length >= 15)
            {
                int tg5 = getInt(TALKGROUP_5);
                if(tg5 > 0)
                {
                    patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(tg5));
                }
            }

            if(length >= 17)
            {
                int tg6 = getInt(TALKGROUP_6);
                if(tg6 > 0)
                {
                    patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(tg6));
                }
            }
            mPatchGroupIdentifier = APCO25PatchGroup.create(patchGroup);
        }

        return mPatchGroupIdentifier;
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
