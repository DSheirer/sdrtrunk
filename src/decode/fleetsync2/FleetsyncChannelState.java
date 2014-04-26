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
package decode.fleetsync2;

import message.Message;
import alias.Alias;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelState.State;

public class FleetsyncChannelState extends AuxChannelState
{
	private static final String PROTOCOL = "FSync II";

	private String mFleetFrom;
	private Alias mFleetFromAlias;

	private String mFleetTo;
	private Alias mFleetToAlias;

	private String mMessage;
	private String mMessageType;

	private FleetsyncActivitySummary mActivitySummary;

	public FleetsyncChannelState( ChannelState parentState )
	{
		super( parentState );
		
		mActivitySummary = 
				new FleetsyncActivitySummary( parentState.getAliasList() );
	}
	
	@Override
	public void reset()
	{
		setFleetIDFrom( null );
		setFleetIDFromAlias( null );
		setFleetIDTo( null );
		setFleetIDToAlias( null );
		setMessage( null );
		setMessageType( null );
	}
	
	@Override
    public void fade()
    {
    }
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof FleetsyncMessage )
		{
			mActivitySummary.receive( message );
			
			FleetsyncMessage fleetsync = (FleetsyncMessage)message;

			setFleetIDFrom( fleetsync.getFromID() );
			setFleetIDFromAlias( fleetsync.getFromIDAlias() );
			
			FleetsyncMessageType type = fleetsync.getMessageType();

			if( type != FleetsyncMessageType.ANI )
			{
				setFleetIDTo( fleetsync.getToID() );
				setFleetIDToAlias( fleetsync.getToIDAlias() );
			}
			
			setMessageType( type.getLabel() );

			switch( type )
			{
				case GPS:
					setMessage( fleetsync.getGPSLocation() );
					break;
				case STATUS:
					StringBuilder sb = new StringBuilder();
					sb.append( fleetsync.getStatus() );
					
					Alias status = fleetsync.getStatusAlias();
					
					if( status != null )
					{
						sb.append( "/" );
						sb.append( status.getName() );
					}

					setMessage( sb.toString() );
					break;
				default:
					break;
			}

			/* Set the state to CALL on the parent channel state, so that the
			 * parent state timer can track turning off any updates we apply */
			mParentChannelState.setState( State.CALL );
			
		    FleetsyncCallEvent fsCallEvent = 
		            FleetsyncCallEvent.getFleetsync2Event( fleetsync );

		    fsCallEvent.setAliasList( mParentChannelState.getAliasList() );

		    mParentChannelState.receiveCallEvent( fsCallEvent );
		}
    }
	
	public String getFleetIDFrom()
	{
		return mFleetFrom;
	}
	
	public void setFleetIDFrom( String from )
	{
		mFleetFrom = from;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP );
	}
	
	public Alias getFleetIDFromAlias()
	{
		return mFleetFromAlias;
	}

	public void setFleetIDFromAlias( Alias alias )
	{
		mFleetFromAlias = alias;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );
	}
	
	public String getFleetIDTo()
	{
		return mFleetTo;
	}
	
	public void setFleetIDTo( String to )
	{
		mFleetTo = to;
		broadcastChange( ChangedAttribute.TO_TALKGROUP );
	}
	
	public Alias getFleetIDToAlias()
	{
		return mFleetToAlias;
	}
	
	public void setFleetIDToAlias( Alias alias )
	{
		mFleetToAlias = alias;
		broadcastChange( ChangedAttribute.TO_TALKGROUP_ALIAS );
	}
	
	public String getMessage()
	{
		return mMessage;
	}

	public void setMessage( String message )
	{
		mMessage = message;
		broadcastChange( ChangedAttribute.MESSAGE );
	}
	
	public String getMessageType()
	{
		return mMessageType;
	}

	public void setMessageType( String type )
	{
		mMessageType = type;
		broadcastChange( ChangedAttribute.MESSAGE_TYPE );
	}
	
	@Override
    public String getActivitySummary()
    {
		return mActivitySummary.getSummary();
    }
}
