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
package module.decode.ltrnet;

import module.decode.DecoderType;
import module.decode.event.CallEvent;
import module.decode.ltrstandard.LTRStandardMessage;
import alias.Alias;
import alias.AliasList;

public class LTRCallEvent extends CallEvent
{
	private String mChannel;
	private long mFrequency;
	private int mValidCallMessages = 1;
	
	public LTRCallEvent( DecoderType type,
						 CallEventType callEventType, 
						 AliasList aliasList,
						 String fromID,
						 String toID,
						 String channel,
						 long frequency,
						 String details )
    {
	    super( type, callEventType, aliasList, fromID, toID, details );
	    
	    mChannel = channel;
	    mFrequency = frequency;
    }

	/**
	 * LTR sends multiple CALL messages during a call.  In order to detect noise
	 * bursts from valid calls, send every valid call message to this method
	 * and the event will keep a tally of valid call messages in order to
	 * determine if the call was a valid (and loggable) call event.
	 */
	public boolean addMessage( LTRStandardMessage message )
	{
		String talkgroup = message.getTalkgroupID();
		
		if( talkgroup.contentEquals( getToID() ) )
		{
			mValidCallMessages++;
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the message's talkgroup matches this call event.  Counts the
	 * number of messages that match this call event to use in determining if
	 * a call event is valid or an isolated (errant) message.
	 * 
	 * See: isValid()
	 */
	public boolean isMatchingTalkgroup( LTRNetMessage message )
	{
		String talkgroup = message.getTalkgroupID();
		
		if( talkgroup != null && getToID() != null &&
			talkgroup.contentEquals( getToID() ) )
		{
			mValidCallMessages++;
			
			return true;
		}
		
		return false;
	}
	
	public boolean isValid()
	{
		if( getCallEventType() == CallEventType.CALL )
		{
			return mValidCallMessages > 1;
		}
		
		return true;
	}
	
	private Alias getAlias( String talkgroup )
	{
		if( hasAliasList() )
		{
			return getAliasList().getTalkgroupAlias( talkgroup );
		}

		return null;
	}

	@Override
    public Alias getFromIDAlias()
    {
		if( mCallEventType == CallEventType.CALL ||
			mCallEventType == CallEventType.REGISTER ||
			mCallEventType == CallEventType.ID_UNIQUE )
		{
			if( hasAliasList() )
			{
				try
				{
					int radioID = Integer.parseInt( getFromID() );

					return mAliasList.getUniqueID( radioID );
				}
				catch( Exception e )
				{
					//Do nothing, we couldn't parse the int radio id value
				}
			}
		}
		else if( mCallEventType == CallEventType.REGISTER_ESN )
		{
			if( hasAliasList() )
			{
				return mAliasList.getESNAlias( getFromID() );
			}
		}
		else
		{
			return getAlias( getFromID() );
		}
		
		return null;
    }

	@Override
	public Alias getToIDAlias()
    {
		return getAlias( getToID() );
    }

	@Override
    public String getChannel()
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
		private DecoderType mDecoderType;
		private CallEventType mCallEventType;

		/* Optional parameters */
		private AliasList mAliasList;
		private String mFromID;
		private String mToID;
		private String mDetails;
		private String mChannel;
		private long mFrequency;

		public Builder( DecoderType decoderType, CallEventType callEventType )
		{
			mDecoderType = decoderType;
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

		public Builder channel( String channel )
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

		public LTRCallEvent build()
		{
			return new LTRCallEvent( this );
		}
		
	}

	/**
	 * Private constructor for the builder
	 */
	private LTRCallEvent( Builder builder )
	{
		this( builder.mDecoderType,
			  builder.mCallEventType,
			  builder.mAliasList, 
			  builder.mFromID,
			  builder.mToID,
			  builder.mChannel,
			  builder.mFrequency,
			  builder.mDetails );
	}

}
