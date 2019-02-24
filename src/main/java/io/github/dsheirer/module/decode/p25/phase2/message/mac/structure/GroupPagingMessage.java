/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Group paging message
 */
public class GroupPagingMessage extends MacStructure
{
    private static final int[] ID_COUNT = {14, 15};
    private static final int[] GROUP_ADDRESS_1 = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] GROUP_ADDRESS_2 = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] GROUP_ADDRESS_3 = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] GROUP_ADDRESS_4 = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private List<Identifier> mIdentifiers;
    private TalkgroupIdentifier mTargetAddress1;
    private TalkgroupIdentifier mTargetAddress2;
    private TalkgroupIdentifier mTargetAddress3;
    private TalkgroupIdentifier mTargetAddress4;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupPagingMessage(CorrectedBinaryMessage message, int offset)
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
        sb.append(" GROUP1:").append(getTargetAddress1());

        int count = getCount();

        if(count > 1)
        {
            sb.append(" GROUP2:").append(getTargetAddress2());

            if(count > 2)
            {
                sb.append(" GROUP3:").append(getTargetAddress3());

                if(count > 3)
                {
                    sb.append(" GROUP4:").append(getTargetAddress4());
                }
            }
        }

        return sb.toString();
    }

    public static int getIdCount(BinaryMessage message, int offset)
    {
        return message.getInt(ID_COUNT, offset);
    }

    /**
     * Number of paging target addresses contained in this message
     * @return addresses count (1 - 4)
     */
    public int getCount()
    {
        return getIdCount(getMessage(), getOffset());
    }

    /**
     * Length of the individual paging message in bytes
     *
     * @return
     */
    public static int getLength(BinaryMessage message, int offset)
    {
        int count = getIdCount(message, offset);

        switch(count)
        {
            case 1:
                return 4;
            case 2:
                return 6;
            case 3:
                return 8;
            case 4:
                return 10;
        }

        return 4;
    }

    /**
     * To Talkgroup 1
     */
    public TalkgroupIdentifier getTargetAddress1()
    {
        if(mTargetAddress1 == null)
        {
            mTargetAddress1 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_1, getOffset()));
        }

        return mTargetAddress1;
    }

    /**
     * To Talkgroup 2
     */
    public TalkgroupIdentifier getTargetAddress2()
    {
        if(mTargetAddress2 == null && getCount() >= 2)
        {
            mTargetAddress2 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_2, getOffset()));
        }

        return mTargetAddress2;
    }

    /**
     * To Talkgroup 3
     */
    public TalkgroupIdentifier getTargetAddress3()
    {
        if(mTargetAddress3 == null && getCount() >= 3)
        {
            mTargetAddress3 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_3, getOffset()));
        }

        return mTargetAddress3;
    }

    /**
     * To Talkgroup 4
     */
    public TalkgroupIdentifier getTargetAddress4()
    {
        if(mTargetAddress4 == null && getCount() >= 4)
        {
            mTargetAddress4 = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_4, getOffset()));
        }

        return mTargetAddress4;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            int count = getCount();

            mIdentifiers.add(getTargetAddress1());

            if(count > 1)
            {
                mIdentifiers.add(getTargetAddress2());

                if(count > 2)
                {
                    mIdentifiers.add(getTargetAddress3());

                    if(count > 3)
                    {
                        mIdentifiers.add(getTargetAddress4());
                    }
                }
            }
        }

        return mIdentifiers;
    }
}
