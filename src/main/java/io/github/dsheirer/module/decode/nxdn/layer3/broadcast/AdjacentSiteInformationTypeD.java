/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNSite;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AdjacentSiteOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.SystemID;
import java.util.List;

/**
 * Type-D adjacent (ie neighbor) site information
 */
public class AdjacentSiteInformationTypeD extends NXDNLayer3Message
{
    private static final int INDEX = OCTET_1 + 7;
    private static final IntField ADJACENT_SITE_OPTION_1 = IntField.length5(OCTET_2);
    private static final int ADJACENT_SITE_SYSTEM_ID_1 = OCTET_2 + 5;
    private static final IntField ADJACENT_SITE_CODE_1 = IntField.length8(OCTET_5);
    private static final IntField ADJACENT_SITE_OPTION_2 = IntField.length5(OCTET_6);
    private static final int ADJACENT_SITE_SYSTEM_ID_2 = OCTET_6 + 5;
    private static final IntField ADJACENT_SITE_CODE_2 = IntField.length8(OCTET_9);

    private AdjacentSiteOption mSiteOption1;
    private AdjacentSiteOption mSiteOption2;
    private SystemID mSystemID1;
    private SystemID mSystemID2;
    private NXDNSite mSite1;
    private NXDNSite mSite2;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type message
     * @param ran is always 0
     * @param lich for the fragment
     */
    public AdjacentSiteInformationTypeD(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("TYPE-D NEIGHBOR-").append(isIndex() ? "3:" : "1:");
        sb.append(getSystemID1()).append(" SITE:").append(getSite1());

        if(getSiteOption1().isIsolatedSite())
        {
            sb.append(" (ISOLATED)");
        }

        if(hasSite2())
        {
            sb.append(" NEIGHBOR-").append(isIndex() ? "4:" : "2:");
            sb.append(getSystemID2()).append(" SITE:").append(getSite2());

            if(getSiteOption2().isIsolatedSite())
            {
                sb.append(" (ISOLATED)");
            }
        }

        return sb.toString();
    }

    /**
     * Indicates if the site 2 field is empty (false) or not (true)
     */
    public boolean hasSite2()
    {
        return getSystemID2().getSystem().getValue() != 0 && getSite2().getValue() != 0;
    }

    /**
     * Indicates if this is adjacent site information message 1 (false) or 2 (true) when transmitting up
     * to four adjacent sites with two sites carried in message 1 and two carried in message 2.
     */
    public boolean isIndex()
    {
        return getMessage().get(INDEX);
    }

    /**
     * Adjacent site option 1
     */
    public AdjacentSiteOption getSiteOption1()
    {
        if(mSiteOption1 == null)
        {
            mSiteOption1 = new AdjacentSiteOption(getMessage().getInt(ADJACENT_SITE_OPTION_1));
        }

        return mSiteOption1;
    }

    /**
     * Adjacent site option 2
     */
    public AdjacentSiteOption getSiteOption2()
    {
        if(mSiteOption2 == null)
        {
            mSiteOption2 = new AdjacentSiteOption(getMessage().getInt(ADJACENT_SITE_OPTION_2));
        }

        return mSiteOption2;
    }

    /**
     * Adjacent site system ID 1
     */
    public SystemID getSystemID1()
    {
        if(mSystemID1 == null)
        {
            mSystemID1 = new SystemID(getMessage(), ADJACENT_SITE_SYSTEM_ID_1);
        }

        return mSystemID1;
    }

    /**
     * Adjacent site system ID 2
     */
    public SystemID getSystemID2()
    {
        if(mSystemID2 == null)
        {
            mSystemID2 = new SystemID(getMessage(), ADJACENT_SITE_SYSTEM_ID_2);
        }

        return mSystemID2;
    }

    /**
     * Adjacent site 1 ID
     */
    public NXDNSite getSite1()
    {
        if(mSite1 == null)
        {
            mSite1 = NXDNSite.create(getMessage().getInt(ADJACENT_SITE_CODE_1));
        }

        return mSite1;
    }

    /**
     * Adjacent site 2 ID
     */
    public NXDNSite getSite2()
    {
        if(mSite2 == null)
        {
            mSite2 = NXDNSite.create(getMessage().getInt(ADJACENT_SITE_CODE_2));
        }

        return mSite2;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
