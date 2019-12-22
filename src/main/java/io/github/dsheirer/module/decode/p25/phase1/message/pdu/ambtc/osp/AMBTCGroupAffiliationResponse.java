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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25Radio;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25AnnouncementTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Group affiliation response
 */
public class AMBTCGroupAffiliationResponse extends AMBTCMessage
{
    private static final int[] HEADER_SOURCE_WACN = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_SOURCE_WACN = {0, 1, 2, 3};
    private static final int[] BLOCK_0_SOURCE_SYSTEM = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_SOURCE_ID = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
        33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_GROUP_WACN = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
        56, 57, 58, 59};
    private static final int[] BLOCK_0_GROUP_SYSTEM = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] BLOCK_0_GROUP_ID = {72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] BLOCK_0_ANNOUNCEMENT_GROUP_ID = {88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] BLOCK_1_ANNOUNCEMENT_GROUP_ID = {0, 1, 2, 3, 4, 5, 6, 7};

    private Identifier mTargetAddress;
    private APCO25FullyQualifiedRadioIdentifier mSourceId;
    private APCO25FullyQualifiedTalkgroupIdentifier mGroupId;
    private Identifier mAnnouncementGroupId;
    private List<Identifier> mIdentifiers;

    public AMBTCGroupAffiliationResponse(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }
        if(getSourceId() != null)
        {
            sb.append(" FM:").append(getSourceId());
        }
        if(getGroupId() != null)
        {
            sb.append(" GROUP:").append(getGroupId());
        }
        if(getAnnouncementGroupId() != null)
        {
            sb.append(" ANNOUNCEMENT GROUP:").append(getAnnouncementGroupId());
        }

        return sb.toString();
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            mTargetAddress = APCO25Radio.createTo(getDataBlock(0).getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    public APCO25FullyQualifiedRadioIdentifier getSourceId()
    {
        if(mSourceId == null && hasDataBlock(0))
        {
            int wacn = getHeader().getMessage().getInt(HEADER_SOURCE_WACN);
            wacn <<= 4;
            wacn += getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ID);

            mSourceId = APCO25FullyQualifiedRadioIdentifier.createFrom(wacn, system, id);
        }

        return mSourceId;
    }

    public APCO25FullyQualifiedTalkgroupIdentifier getGroupId()
    {
        if(mGroupId == null && hasDataBlock(0))
        {
            int wacn = getDataBlock(0).getMessage().getInt(BLOCK_0_GROUP_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_GROUP_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_GROUP_ID);
            mGroupId = APCO25FullyQualifiedTalkgroupIdentifier.createAny(wacn, system, id);
        }

        return mGroupId;
    }

    public Identifier getAnnouncementGroupId()
    {
        if(mAnnouncementGroupId == null && hasDataBlock(0) && hasDataBlock(1))
        {
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_ANNOUNCEMENT_GROUP_ID);
            id <<= 8;
            id += getDataBlock(1).getMessage().getInt(BLOCK_1_ANNOUNCEMENT_GROUP_ID);

            mAnnouncementGroupId = APCO25AnnouncementTalkgroup.create(id);
        }

        return mAnnouncementGroupId;
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getSourceId() != null)
            {
                mIdentifiers.add(getSourceId());
            }
            if(getGroupId() != null)
            {
                mIdentifiers.add(getGroupId());
            }
            if(getAnnouncementGroupId() != null)
            {
                mIdentifiers.add(getAnnouncementGroupId());
            }
        }

        return mIdentifiers;
    }
}
