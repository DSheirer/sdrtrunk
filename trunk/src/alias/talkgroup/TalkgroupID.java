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
package alias.talkgroup;

import javax.xml.bind.annotation.XmlAttribute;

import alias.AliasID;
import alias.AliasIDType;
import audio.inverted.AudioType;


public class TalkgroupID extends AliasID
{
	private String mTalkgroup;
	private AudioType mAudioType;
	
	public TalkgroupID()
	{
	}

	@XmlAttribute
	public String getTalkgroup()
	{
		return mTalkgroup;
	}

	public void setTalkgroup( String talkgroup )
	{
		mTalkgroup = talkgroup;
	}

	@XmlAttribute
	public AudioType getAudioType()
	{
		return mAudioType;
	}
	
	public void setAudioType( AudioType type )
	{
		mAudioType = type;
	}
	
	public boolean hasAudioType()
	{
		return mAudioType != null;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Talkgroup: " + mTalkgroup );
		
		if( mAudioType != null )
		{
			sb.append( " [Audio " );
			sb.append( mAudioType.getShortDisplayString() );
			sb.append( "]" );
		}
		
		return sb.toString();
	}

	@Override
    public boolean matches( AliasID id )
    {
		boolean retVal = false;
		
		if( id instanceof TalkgroupID )
		{
			TalkgroupID tgid = (TalkgroupID)id;

			//Create a pattern - replace * wildcards with regex single-char wildcard
			String pattern = mTalkgroup.replace( "*", ".?" );
			
			retVal = tgid.getTalkgroup().matches( pattern );
		}
		
	    return retVal;
    }

	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.Talkgroup;
    }
}
