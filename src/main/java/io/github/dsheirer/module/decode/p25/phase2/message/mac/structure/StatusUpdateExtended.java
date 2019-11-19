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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.status.APCO25UnitStatus;
import io.github.dsheirer.module.decode.p25.identifier.status.APCO25UserStatus;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FullyQualifiedIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Status update - abbreviated format
 */
public class StatusUpdateExtended extends MacStructure
{
    private static final int[] UNIT_STATUS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] USER_STATUS = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] TARGET_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
        49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_WACN = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75};
    private static final int[] SOURCE_SYSTEM = {76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] SOURCE_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103,
        104, 105, 106, 107, 108, 109, 110, 111};

    private List<Identifier> mIdentifiers;
    private Identifier mUnitStatus;
    private Identifier mUserStatus;
    private TalkgroupIdentifier mTargetAddress;
    private APCO25FullyQualifiedIdentifier mSourceSuid;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public StatusUpdateExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" FM:").append(getSourceSuid());
        sb.append(" UNIT:").append(getUnitStatus());
        sb.append(" USER:").append(getUserStatus());
        return sb.toString();
    }

    public Identifier getUnitStatus()
    {
        if(mUnitStatus == null)
        {
            mUnitStatus = APCO25UnitStatus.create(getMessage().getInt(UNIT_STATUS));
        }

        return mUnitStatus;
    }

    public Identifier getUserStatus()
    {
        if(mUserStatus == null)
        {
            mUserStatus = APCO25UserStatus.create(getMessage().getInt(USER_STATUS));
        }

        return mUserStatus;
    }

    /**
     * To Talkgroup
     */
    public TalkgroupIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS, getOffset()));
        }

        return mTargetAddress;
    }

    /**
     * From Radio Unit
     */
    public APCO25FullyQualifiedIdentifier getSourceSuid()
    {
        if(mSourceSuid == null)
        {
            mSourceSuid = APCO25FullyQualifiedIdentifier.createFrom(getMessage().getInt(SOURCE_WACN, getOffset()),
                getMessage().getInt(SOURCE_SYSTEM, getOffset()), getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceSuid;
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceSuid());
            mIdentifiers.add(getUnitStatus());
            mIdentifiers.add(getUserStatus());
        }

        return mIdentifiers;
    }
}
