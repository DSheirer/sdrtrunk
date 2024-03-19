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
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Indirect group paging without priority
 */
public class IndirectGroupPagingWithoutPriority extends MacStructureVariableLength
{
    private static final IntField ID_COUNT = IntField.range(14, 15);
    private static final IntField GROUP_ADDRESS_1 = IntField.length24(OCTET_3_BIT_16);
    private static final IntField GROUP_ADDRESS_2 = IntField.length24(OCTET_6_BIT_40);
    private static final IntField GROUP_ADDRESS_3 = IntField.length24(OCTET_9_BIT_64);
    private static final IntField GROUP_ADDRESS_4 = IntField.length24(OCTET_12_BIT_88);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetGroup1;
    private Identifier mTargetGroup2;
    private Identifier mTargetGroup3;
    private Identifier mTargetGroup4;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public IndirectGroupPagingWithoutPriority(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Length of this type of message in octets.
     * @param message containing bits
     * @param offset to the start of this message.
     * @return length in octets.
     */
    public static int getLength(CorrectedBinaryMessage message, int offset)
    {
        int count = message.getInt(ID_COUNT, offset);

        switch(count)
        {
            case 1:
                return 4;
            case 2:
                return 6;
            case 3:
                return 8;
            case 4:
            default:
                return 10;
        }
    }

    @Override
    public int getLength()
    {
        return getLength(getMessage(), getOffset());
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" GROUP1:").append(getTargetGroup1());
        int count = getCount();

        if(count > 1)
        {
            sb.append(" GROUP2:").append(getTargetGroup2());

            if(count > 2)
            {
                sb.append(" GROUP3:").append(getTargetGroup3());

                if(count > 3)
                {
                    sb.append(" GROUP4:").append(getTargetGroup4());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Number of paging target addresses contained in this message
     * @return addresses count (1 - 4)
     */
    public int getCount()
    {
        return getInt(ID_COUNT);
    }

    /**
     * To Talkgroup 1
     */
    public Identifier getTargetGroup1()
    {
        if(mTargetGroup1 == null)
        {
            mTargetGroup1 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_1));
        }

        return mTargetGroup1;
    }

    /**
     * To Talkgroup 2
     */
    public Identifier getTargetGroup2()
    {
        if(mTargetGroup2 == null && getCount() >= 2)
        {
            mTargetGroup2 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_2));
        }

        return mTargetGroup2;
    }

    /**
     * To Talkgroup 3
     */
    public Identifier getTargetGroup3()
    {
        if(mTargetGroup3 == null && getCount() >= 3)
        {
            mTargetGroup3 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_3));
        }

        return mTargetGroup3;
    }

    /**
     * To Talkgroup 4
     */
    public Identifier getTargetGroup4()
    {
        if(mTargetGroup4 == null && getCount() >= 4)
        {
            mTargetGroup4 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_4));
        }

        return mTargetGroup4;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            int count = getCount();

            mIdentifiers.add(getTargetGroup1());

            if(count > 1)
            {
                mIdentifiers.add(getTargetGroup2());

                if(count > 2)
                {
                    mIdentifiers.add(getTargetGroup3());

                    if(count > 3)
                    {
                        mIdentifiers.add(getTargetGroup4());
                    }
                }
            }
        }

        return mIdentifiers;
    }
}
