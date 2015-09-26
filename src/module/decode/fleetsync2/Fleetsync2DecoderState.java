/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode.fleetsync2;

import java.util.Iterator;
import java.util.TreeSet;

import message.Message;
import module.decode.DecoderType;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;
import module.decode.state.DecoderStateEvent.Event;
import module.decode.state.State;
import alias.Alias;
import alias.AliasList;

public class Fleetsync2DecoderState extends DecoderState
{
	private TreeSet<String> mIdents = new TreeSet<String>();
	private TreeSet<String> mEmergencyIdents = new TreeSet<String>();

	private String mFrom;
	private Alias mFromAlias;

	private String mTo;
	private Alias mToAlias;

	private String mMessage;
	private String mMessageType;

	public Fleetsync2DecoderState( AliasList aliasList )
	{
		super( aliasList );
	}
	
	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.FLEETSYNC2;
	}

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
	}

	@Override
	public void dispose()
	{
		super.dispose();
	}
	
	@Override
	public void init()
	{
	}

	/**
	 * Resets the overall decoder state and clears any accumulated event details
	 */
	@Override
	public void reset()
	{
		mIdents.clear();
		mEmergencyIdents.clear();

		resetState();
	}

	/**
	 * Resets this decoder state
	 */
	public void resetState()
	{
		setFromID( null );
		setFromIDAlias( null );
		setToID( null );
		setToIDAlias( null );
		setMessage( null );
		setMessageType( null );
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof FleetsyncMessage )
		{
			FleetsyncMessage fleetsync = (FleetsyncMessage)message;
			
			if( fleetsync.isValid() )
			{
				State state = State.CALL;
				
				setFromID( fleetsync.getFromID() );
				setFromIDAlias( fleetsync.getFromIDAlias() );
				mIdents.add( fleetsync.getFromID() );
				
				FleetsyncMessageType type = fleetsync.getMessageType();

				if( type != FleetsyncMessageType.ANI )
				{
					setToID( fleetsync.getToID() );
					setToIDAlias( fleetsync.getToIDAlias() );
					mIdents.add( fleetsync.getToID() );
				}
				
				setMessageType( type.getLabel() );

				switch( type )
				{
					case GPS:
						setMessage( fleetsync.getGPSLocation() );
						state = State.DATA;
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
						state = State.DATA;
						break;
					case EMERGENCY:
					case LONE_WORKER_EMERGENCY:
						mEmergencyIdents.add( fleetsync.getFromID() );
						state = State.DATA;
						break;
					default:
						break;
				}

			    FleetsyncCallEvent fsCallEvent = 
			            FleetsyncCallEvent.getFleetsync2Event( fleetsync );

			    fsCallEvent.setAliasList( getAliasList() );

			    broadcast( fsCallEvent );

			    /* Broadcast decode event so that the channel state will 
			     * kick in and reset everything */
			    broadcast( new DecoderStateEvent( this, Event.DECODE, state ) );
			}
		}
    }
	
	public String getFromID()
	{
		return mFrom;
	}
	
	public void setFromID( String from )
	{
		mFrom = from;
		broadcast( ChangedAttribute.FROM_TALKGROUP );
	}
	
	public Alias getFromIDAlias()
	{
		return mFromAlias;
	}

	public void setFromIDAlias( Alias alias )
	{
		mFromAlias = alias;
		broadcast( ChangedAttribute.FROM_TALKGROUP_ALIAS );
	}
	
	public String getToID()
	{
		return mTo;
	}
	
	public void setToID( String to )
	{
		mTo = to;
		broadcast( ChangedAttribute.TO_TALKGROUP );
	}
	
	public Alias getToIDAlias()
	{
		return mToAlias;
	}
	
	public void setToIDAlias( Alias alias )
	{
		mToAlias = alias;
		broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );
	}
	
	public String getMessage()
	{
		return mMessage;
	}

	public void setMessage( String message )
	{
		mMessage = message;
		broadcast( ChangedAttribute.MESSAGE );
	}
	
	public String getMessageType()
	{
		return mMessageType;
	}

	public void setMessageType( String type )
	{
		mMessageType = type;
		broadcast( ChangedAttribute.MESSAGE_TYPE );
	}
	
	/**
	 * Responds to reset events issued by the channel state
	 */
	@Override
	public void receiveDecoderStateEvent( DecoderStateEvent event )
	{
		switch( event.getEvent() )
		{
			case RESET:
				resetState();
				break;
			default:
				break;
		}
	}
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tFleetsync II\n\n" );
		sb.append( "Fleetsync Idents\n" );

		if( mIdents.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mIdents.iterator();
			
			while( it.hasNext() )
			{
				String ident = it.next();
				
				sb.append( "  " );
				sb.append( formatIdent( ident ) );

				if( hasAliasList() )
				{
					Alias alias = getAliasList().getFleetsyncAlias( ident );
					
					if( alias != null )
					{
						sb.append( " " );
						sb.append( alias.getName() );
					}
				}
				
				sb.append( "\n" );
			}
		}
		
		sb.append( "\nFleetsync Emergency Activations\n" );

		if( mEmergencyIdents.isEmpty() )
		{
			sb.append( "  None\n" );
		}
		else
		{
			Iterator<String> it = mEmergencyIdents.iterator();
			
			while( it.hasNext() )
			{
				String ident = it.next();
				
				sb.append( "  " );
				sb.append( formatIdent( ident ) );

				if( hasAliasList() )
				{
					Alias alias = getAliasList().getFleetsyncAlias( ident );
					
					if( alias != null )
					{
						sb.append( " " );
						sb.append( alias.getName() );
					}
					
					sb.append( "\n" );
				}
			}
		}

		return sb.toString();
    }

	public static String formatIdent( String ident )
	{
		StringBuilder sb = new StringBuilder();
		
		if( ident.length() == 7 )
		{
			sb.append( ident.substring( 0, 3 ) );
			sb.append( "-" );
			sb.append( ident.substring( 3, 7 ) );

			return sb.toString();
		}
		else
		{
			return ident;
		}
	}
}
