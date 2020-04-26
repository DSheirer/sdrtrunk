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
package io.github.dsheirer.alias.id.legacy.talkgroup;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

public class LegacyTalkgroupID extends AliasID
{
    private String mTalkgroup;

    public LegacyTalkgroupID()
    {
        //No arg JAXB constructor
    }

    public LegacyTalkgroupID(String talkgroup)
    {
        mTalkgroup = talkgroup;
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "talkgroup")
    public String getTalkgroup()
    {
        return mTalkgroup;
    }

    public void setTalkgroup(String talkgroup)
    {
        mTalkgroup = talkgroup;
    }

    @Override
    public boolean isValid()
    {
        return mTalkgroup != null;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Legacy Talkgroup:").append(mTalkgroup).append(" *Disabled - Please use Talkgroup instead");

        return sb.toString();
    }

    @Override
    public boolean matches(AliasID id)
    {
        boolean retVal = false;

        if(id instanceof LegacyTalkgroupID)
        {
            LegacyTalkgroupID tgid = (LegacyTalkgroupID)id;

            //Create a pattern - replace * wildcards with regex single-char wildcard
            String pattern = mTalkgroup.replace("*", ".?");

            retVal = tgid.getTalkgroup().matches(pattern);
        }

        return retVal;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.LEGACY_TALKGROUP;
    }
}
