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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25Radio;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Individual paging message with priority
 */
public class IndividualPagingMessage extends MacStructure
{
    private static final int PRIORITY_ID_1 = 8;
    private static final int PRIORITY_ID_2 = 9;
    private static final int PRIORITY_ID_3 = 10;
    private static final int PRIORITY_ID_4 = 11;
    private static final int[] ID_COUNT = {14, 15};
    private static final int[] TARGET_ADDRESS_1 = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
        33, 34, 35, 36, 37, 38, 39};
    private static final int[] TARGET_ADDRESS_2 = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63};
    private static final int[] TARGET_ADDRESS_3 = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
        81, 82, 83, 84, 85, 86, 87};
    private static final int[] TARGET_ADDRESS_4 = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103,
        104, 105, 106, 107, 108, 109, 110, 111};

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress1;
    private Identifier mTargetAddress2;
    private Identifier mTargetAddress3;
    private Identifier mTargetAddress4;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public IndividualPagingMessage(CorrectedBinaryMessage message, int offset)
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
        sb.append(" ID1:").append(getTargetAddress1());

        if(isTalkgroupPriority1())
        {
            sb.append("-HIGH PRIORITY");
        }
        else
        {
            sb.append("-LOW PRIORITY");
        }

        int count = getCount();

        if(count > 1)
        {
            sb.append(" ID2:").append(getTargetAddress2());

            if(isTalkgroupPriority2())
            {
                sb.append("-HIGH PRIORITY");
            }
            else
            {
                sb.append("-LOW PRIORITY");
            }

            if(count > 2)
            {
                sb.append(" ID3:").append(getTargetAddress3());

                if(isTalkgroupPriority3())
                {
                    sb.append("-HIGH PRIORITY");
                }
                else
                {
                    sb.append("-LOW PRIORITY");
                }

                if(count > 3)
                {
                    sb.append(" ID4:").append(getTargetAddress4());

                    if(isTalkgroupPriority4())
                    {
                        sb.append("-HIGH PRIORITY");
                    }
                    else
                    {
                        sb.append("-LOW PRIORITY");
                    }
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
                return 5;
            case 2:
                return 8;
            case 3:
                return 11;
            case 4:
                return 14;
        }

        return 5;
    }

    public boolean isTalkgroupPriority1()
    {
        return getMessage().get(PRIORITY_ID_1 + getOffset());
    }

    public boolean isTalkgroupPriority2()
    {
        return getMessage().get(PRIORITY_ID_2 + getOffset());
    }

    public boolean isTalkgroupPriority3()
    {
        return getMessage().get(PRIORITY_ID_3 + getOffset());
    }

    public boolean isTalkgroupPriority4()
    {
        return getMessage().get(PRIORITY_ID_4 + getOffset());
    }

    /**
     * To Talkgroup 1
     */
    public Identifier getTargetAddress1()
    {
        if(mTargetAddress1 == null)
        {
            mTargetAddress1 = APCO25Radio.createTo(getMessage().getInt(TARGET_ADDRESS_1, getOffset()));
        }

        return mTargetAddress1;
    }

    /**
     * To Talkgroup 2
     */
    public Identifier getTargetAddress2()
    {
        if(mTargetAddress2 == null && getCount() >= 2)
        {
            mTargetAddress2 = APCO25Radio.createTo(getMessage().getInt(TARGET_ADDRESS_2, getOffset()));
        }

        return mTargetAddress2;
    }

    /**
     * To Talkgroup 3
     */
    public Identifier getTargetAddress3()
    {
        if(mTargetAddress3 == null && getCount() >= 3)
        {
            mTargetAddress3 = APCO25Radio.createTo(getMessage().getInt(TARGET_ADDRESS_3, getOffset()));
        }

        return mTargetAddress3;
    }

    /**
     * To Talkgroup 4
     */
    public Identifier getTargetAddress4()
    {
        if(mTargetAddress4 == null && getCount() >= 4)
        {
            mTargetAddress4 = APCO25Radio.createTo(getMessage().getInt(TARGET_ADDRESS_4, getOffset()));
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
