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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25AnnouncementTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.reference.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Group affiliation response abbreviated
 */
public class GroupAffiliationResponseExtended extends MacStructure
{
    private static final int LOCAL_OR_GLOBAL_AFFILIATION_FLAG = 16;
    private static final IntField RESPONSE = IntField.range(22, 23);
    private static final IntField ANNOUNCEMENT_GROUP_ADDRESS = IntField.length16(OCTET_4_BIT_24);
    private static final IntField GROUP_ADDRESS = IntField.length16(OCTET_6_BIT_40);
    private static final IntField SOURCE_GID_WACN = IntField.length20(OCTET_8_BIT_56);
    private static final IntField SOURCE_GID_SYSTEM = IntField.length12(OCTET_10_BIT_72 + 4);
    private static final IntField SOURCE_GID_ID = IntField.length16(OCTET_12_BIT_88);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_14_BIT_104);

    private Identifier mAnnouncementGroupAddress;
    private APCO25FullyQualifiedTalkgroupIdentifier mSourceGID;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupAffiliationResponseExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" AFFILIATION ").append(getResponse());
        sb.append(" FOR GROUP:").append(getSourceGID());
        sb.append(" AFFILIATION GROUP:").append(getAnnouncementGroupAddress());

        return sb.toString();
    }

    public Response getResponse()
    {
        return Response.fromValue(getInt(RESPONSE));
    }

    public Identifier getSourceGID()
    {
        if(mSourceGID == null)
        {
            int address = getInt(GROUP_ADDRESS);
            int wacn = getInt(SOURCE_GID_WACN);
            int system = getInt(SOURCE_GID_SYSTEM);
            int id = getInt(SOURCE_GID_ID);

            mSourceGID = APCO25FullyQualifiedTalkgroupIdentifier.createFrom(address, wacn, system, id);
        }

        return mSourceGID;
    }

    public Identifier getAnnouncementGroupAddress()
    {
        if(mAnnouncementGroupAddress == null)
        {
            mAnnouncementGroupAddress = APCO25AnnouncementTalkgroup.create(getInt(ANNOUNCEMENT_GROUP_ADDRESS));
        }

        return mAnnouncementGroupAddress;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceGID());
            mIdentifiers.add(getAnnouncementGroupAddress());
        }

        return mIdentifiers;
    }
}
