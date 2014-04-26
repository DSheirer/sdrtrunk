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
package decode.mdc1200;

import message.Message;
import alias.Alias;
import controller.state.AuxChannelState;
import controller.state.ChannelState;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelState.State;

public class MDCChannelState extends AuxChannelState
{
	private String mFrom;
	private Alias mFromAlias;
	private String mTo;
	private Alias mToAlias;
	private String mMessage;
	private String mMessageType;

	private MDCActivitySummary mActivitySummary;

	public MDCChannelState( ChannelState parentState )
	{
		super( parentState );
		
		mActivitySummary = new MDCActivitySummary( parentState.getAliasList() );
	}
	
	@Override
	public void reset()
	{
		setFrom( null );
		setFromAlias( null );
		setTo( null);
		setToAlias( null );
		setMessage( null );
		setMessageType( null );
	}
	
	@Override
    public void fade() {}

	@Override
    public void receive( Message message )
    {
		if( message instanceof MDCMessage )
		{
			mActivitySummary.receive( message );
			
			MDCMessage mdc = (MDCMessage)message;
			
			setFrom( mdc.getFromID() );
			setFromAlias( mdc.getFromIDAlias() );
			
			MDCMessageType type = mdc.getMessageType();
			
			setMessageType( type.getLabel() );

			StringBuilder sb = new StringBuilder();
			
			switch( type )
			{
				case ACKNOWLEDGE:
				case ANI:
				case EMERGENCY:
				case PAGING:
				case STATUS:
				default:
					sb.append( "OPCODE " );
					sb.append( String.valueOf( mdc.getOpcode() ) );
					
					if( mdc.isBOT() )
					{
						sb.append( " TYPE:BOT" );
					}
					
					if( mdc.isEOT() )
					{
						sb.append( " TYPE:EOT" );
					}
					break;
			}
			
			setMessage( sb.toString() );

			//Set the state to CALL on the parent channel state, so that the
			//timer can track turning off any updates we apply
			mParentChannelState.setState( State.CALL );

			MDCCallEvent event = MDCCallEvent.getMDCCallEvent( mdc );
			event.setAliasList( mParentChannelState.getAliasList() );
			mParentChannelState.receiveCallEvent( event );
		}
    }
	
	public String getFrom()
	{
		return mFrom;
	}
	
	public void setFrom( String from )
	{
		mFrom = from;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP );
	}
	
	public Alias getFromAlias()
	{
		return mFromAlias;
	}
	
	public void setFromAlias( Alias alias )
	{
		mFromAlias = alias;
		broadcastChange( ChangedAttribute.FROM_TALKGROUP_ALIAS );
	}
	
	public String getTo()
	{
		return mTo;
	}
	
	public void setTo( String to )
	{
		mTo = to;
		broadcastChange( ChangedAttribute.TO_TALKGROUP );
	}
	
	public Alias getToAlias()
	{
		return mToAlias;
	}
	
	public void setToAlias( Alias alias )
	{
		mToAlias = alias;
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
