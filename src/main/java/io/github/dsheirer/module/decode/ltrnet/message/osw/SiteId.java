/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.ltrnet.message.osw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.module.decode.ltrnet.identifier.SiteIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Site Identifier.
 */
public class SiteId extends LtrNetOswMessage
{
    private SiteIdentifier mSiteIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a message
     */
    public SiteId(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.OSW_SITE_ID;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SITE:").append(getSiteID());
        return sb.toString();
    }

    public SiteIdentifier getSiteID()
    {
        if(mSiteIdentifier == null)
        {
            mSiteIdentifier = SiteIdentifier.create(getMessage().getInt(23, 32));
        }

        return mSiteIdentifier;
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSiteID());
        }

        return mIdentifiers;
    }
}
