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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.reference.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Location registration response
 */
public class LocationRegistrationResponse extends MacStructure
{
    private static final IntField RESPONSE = IntField.range(22, 23);
    private static final IntField GROUP_ADDRESS = IntField.length16(OCTET_4_BIT_24);
    private static final IntField RFSS = IntField.length8(OCTET_6_BIT_40);
    private static final IntField SITE = IntField.length8(OCTET_7_BIT_48);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_8_BIT_56);
    private Identifier mTargetAddress;
    private Identifier mGroupAddress;
    private Identifier mRFSS;
    private Identifier mSite;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public LocationRegistrationResponse(CorrectedBinaryMessage message, int offset)
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
        sb.append(" LOCATION REGISTRATION ").append(getResponse());
        sb.append(" FOR GROUP:").append(getGroupAddress());
        sb.append(" RFSS:").append(getRFSS());
        sb.append(" SITE:").append(getSite());
        return sb.toString();
    }

    public Response getResponse()
    {
        return Response.fromValue(getInt(RESPONSE));
    }

    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.createAny(getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    public Identifier getRFSS()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getInt(RFSS));
        }

        return mRFSS;
    }

    public Identifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getInt(SITE));
        }

        return mSite;
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
        }

        return mIdentifiers;
    }
}
