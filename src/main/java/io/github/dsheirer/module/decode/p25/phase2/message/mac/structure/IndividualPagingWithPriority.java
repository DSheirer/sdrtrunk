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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Individual paging with priority
 */
public class IndividualPagingWithPriority extends MacStructureVariableLength
{
    private static final int PRIORITY_ID_1 = 8;
    private static final int PRIORITY_ID_2 = 9;
    private static final int PRIORITY_ID_3 = 10;
    private static final int PRIORITY_ID_4 = 11;
    private static final IntField ID_COUNT = IntField.range(14, 15);
    private static final IntField TARGET_ADDRESS_1 = IntField.length24(OCTET_3_BIT_16);
    private static final IntField TARGET_ADDRESS_2 = IntField.length24(OCTET_6_BIT_40);
    private static final IntField TARGET_ADDRESS_3 = IntField.length24(OCTET_9_BIT_64);
    private static final IntField TARGET_ADDRESS_4 = IntField.length24(OCTET_12_BIT_88);

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
    public IndividualPagingWithPriority(CorrectedBinaryMessage message, int offset)
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
                return 5;
            case 2:
                return 8;
            case 3:
                return 11;
            case 4:
            default:
                return 14;
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
            mTargetAddress1 = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS_1));
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
            mTargetAddress2 = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS_2));
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
            mTargetAddress3 = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS_3));
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
            mTargetAddress4 = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS_4));
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
