/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package alias.id.talkgroup;

import alias.id.AliasID;
import alias.id.AliasIDType;

import javax.xml.bind.annotation.XmlAttribute;


public class TalkgroupID extends AliasID
{
    private String mTalkgroup;

    public TalkgroupID()
    {
        //No arg JAXB constructor
    }

    public TalkgroupID(String talkgroup)
    {
        mTalkgroup = talkgroup;
    }

    @XmlAttribute
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

        sb.append("Talkgroup: " + mTalkgroup);

        return sb.toString();
    }

    @Override
    public boolean matches(AliasID id)
    {
        boolean retVal = false;

        if(id instanceof TalkgroupID)
        {
            TalkgroupID tgid = (TalkgroupID) id;

            //Create a pattern - replace * wildcards with regex single-char wildcard
            String pattern = mTalkgroup.replace("*", ".?");

            retVal = tgid.getTalkgroup().matches(pattern);
        }

        return retVal;
    }

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.TALKGROUP;
    }
}
