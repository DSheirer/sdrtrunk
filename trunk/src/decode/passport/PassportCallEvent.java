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
package decode.passport;

import alias.Alias;
import alias.AliasList;
import controller.activity.CallEvent;
import decode.DecoderType;

public class PassportCallEvent extends CallEvent
{
	private int mChannel;
	private long mFrequency;
	
	public PassportCallEvent( CallEventType callEventType, 
							 AliasList aliasList,
							 String fromID,
							 String toID,
							 int channel,
							 long frequency,
							 String details )
    {
	    super( DecoderType.PASSPORT, callEventType, 
	    		aliasList, fromID, toID, details );
	    
	    mChannel = channel;
	    mFrequency = frequency;
    }
	
	private Alias getAlias( String talkgroup )
	{
		if( hasAliasList() )
		{
			return getAliasList().getTalkgroupAlias( talkgroup );
		}

		return null;
	}

	private Alias getMobileIDAlias( String mobileID )
	{
		if( hasAliasList() )
		{
			return getAliasList().getMobileIDNumberAlias( mobileID );
		}

		return null;
	}

	@Override
    public Alias getFromIDAlias()
    {
		return getMobileIDAlias( getFromID() );
    }

	@Override
    public Alias getToIDAlias()
    {
		return getAlias( getToID() );
    }

	@Override
    public int getChannel()
    {
	    return mChannel;
    }

	@Override
    public long getFrequency()
    {
	    return mFrequency;
    }

	public static class Builder
	{
		/* Required parameters */
		private CallEventType mCallEventType;

		/* Optional parameters */
		private AliasList mAliasList;
		private String mFromID;
		private String mToID;
		private String mDetails;
		private int mChannel;
		private long mFrequency;

		public Builder( CallEventType callEventType )
		{
			mCallEventType = callEventType;
		}
		
		public Builder aliasList( AliasList aliasList )
		{
			mAliasList = aliasList;
			return this;
		}
		
		public Builder from( String val )
		{
			mFromID = val;
			return this;
		}

		public Builder channel( int channel )
		{
			mChannel = channel;
			return this;
		}

		public Builder details( String details )
		{
			mDetails = details;
			return this;
		}
		
		public Builder frequency( long frequency )
		{
			mFrequency = frequency;
			return this;
		}

		public Builder to( String toID )
		{
			mToID = toID;
			return this;
		}

		public PassportCallEvent build()
		{
			return new PassportCallEvent( this );
		}
		
	}

	/**
	 * Private constructor for the builder
	 */
	private PassportCallEvent( Builder builder )
	{
		this( builder.mCallEventType,
			  builder.mAliasList, 
			  builder.mFromID,
			  builder.mToID,
			  builder.mChannel,
			  builder.mFrequency,
			  builder.mDetails );
	}

}
