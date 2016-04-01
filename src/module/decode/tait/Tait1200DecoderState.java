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
package module.decode.tait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import message.Message;
import module.decode.DecoderType;
import module.decode.event.CallEvent.CallEventType;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;
import module.decode.state.State;
import module.decode.state.DecoderStateEvent.Event;
import alias.Alias;
import alias.AliasList;

public class Tait1200DecoderState extends DecoderState
{
	private TreeSet<String> mIdents = new TreeSet<String>();
	
	private String mFrom;
	private Alias mFromAlias;

	private String mTo;
	private Alias mToAlias;

	private String mMessage;
	private String mMessageType;

	public Tait1200DecoderState( AliasList aliasList )
	{
		super( aliasList );
	}
	
	@Override
	public DecoderType getDecoderType()
	{
		return DecoderType.TAIT_1200;
	}

	@Override
	public void start( ScheduledExecutorService executor )
	{
	}

	@Override
	public void stop()
	{
	}

	@Override
	public void reset()
	{
		mIdents.clear();
		
		resetState();
	}
	
	private void resetState()
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
		if( message instanceof Tait1200GPSMessage )
		{
			Tait1200GPSMessage gps = (Tait1200GPSMessage)message;
			
			setFromID( gps.getFromID() );
			setFromIDAlias( gps.getFromIDAlias() );
			mIdents.add( gps.getFromID() );
			
			setToID( gps.getToID() );
			setToIDAlias( gps.getToIDAlias() );
			mIdents.add( gps.getToID());
			
			GeoPosition position = gps.getGPSLocation();
			
			if( position != null )
			{
				setMessage( gps.getGPSLocation().toString().replace( "[", "" )
						.replace( "]", "" ) );
			}
			
			setMessageType( "GPS" );
			
			broadcast( new Tait1200CallEvent( CallEventType.GPS, getAliasList(), 
				gps.getFromID(), gps.getToID(), gps.getGPSLocation().toString() ) );

			broadcast( new DecoderStateEvent( this, Event.DECODE, State.DATA ) );
		}
		else if( message instanceof Tait1200ANIMessage )
		{
			Tait1200ANIMessage ani = (Tait1200ANIMessage)message;
			
			setFromID( ani.getFromID() );
			setFromIDAlias( ani.getFromIDAlias() );
			mIdents.add( ani.getFromID() );

			setToID( ani.getToID() );
			setToIDAlias( ani.getToIDAlias() );
			mIdents.add( ani.getToID());
			
			setMessage( null );
			setMessageType( "ANI" );

			broadcast( new Tait1200CallEvent( CallEventType.ID_ANI, getAliasList(), 
				ani.getFromID(), ani.getToID(), "ANI" ) );
			
			broadcast( new DecoderStateEvent( this, Event.DECODE, State.CALL ) );
		}
    }
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tTait-1200I\n\n" );
		
		if( !mIdents.isEmpty() )
		{
			sb.append( "Radio Identifiers:\n" );
			
			List<String> idents = new ArrayList<String>( mIdents );
			
			Collections.sort( idents );
			
			for( String ident: idents )
			{
				sb.append( "\t" );
				sb.append( ident );
				
				if( hasAliasList() )
				{
					Alias alias = getAliasList().getTalkgroupAlias( ident );
					
					if( alias != null )
					{
						sb.append( "\t" );
						sb.append( alias.getName() );
					}
				}
				sb.append( "\n" );
			}
		}
		
		return sb.toString();
    }

	@Override
	public void init()
	{
		/* No initialization required */
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
}
