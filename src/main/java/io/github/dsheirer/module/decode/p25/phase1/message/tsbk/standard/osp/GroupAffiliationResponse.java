/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25AnnouncementTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Group affiliation response.
 */
public class GroupAffiliationResponse extends OSPMessage
{
    private static final int GLOBAL_LOCAL_FLAG = 16;
    private static final int[] RESERVED = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] RESPONSE = {22, 23};
    private static final int[] ANNOUNCEMENT_GROUP_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
            39};
    private static final int[] GROUP_ADDRESS = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private Response mAffiliationResponse;
    private Identifier mAnnouncementGroupAddress;
    private Identifier mGroupAddress;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public GroupAffiliationResponse(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" GROUP ADDRESS:").append(getGroupAddress());
        sb.append(" (").append(isGlobalAffiliation() ? "GLOBAL" : "LOCAL").append(")");
        sb.append(" AFFILIATION:").append(getAffiliationResponse());
        sb.append(" ANNOUNCEMENT GROUP:").append(getAnnouncementGroupAddress());
        return sb.toString();
    }

    public boolean isGlobalAffiliation()
    {
        return getMessage().get(GLOBAL_LOCAL_FLAG);
    }

    public Response getAffiliationResponse()
    {
        if(mAffiliationResponse == null)
        {
            mAffiliationResponse = Response.fromValue(getMessage().getInt(RESPONSE));
        }

        return mAffiliationResponse;
    }

    public Identifier getAnnouncementGroupAddress()
    {
        if(mAnnouncementGroupAddress == null)
        {
            mAnnouncementGroupAddress = APCO25AnnouncementTalkgroup.create(getMessage().getInt(ANNOUNCEMENT_GROUP_ADDRESS));
        }

        return mAnnouncementGroupAddress;
    }

    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.createAny(getMessage().getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getAnnouncementGroupAddress());
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
