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
package module.decode.lj1200;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import message.Message;
import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;
import module.decode.state.DecoderStateEvent.Event;
import module.decode.state.State;
import alias.Alias;
import alias.AliasList;

public class LJ1200DecoderState extends DecoderState
{
	private Set<String> mAddresses = new TreeSet<String>();
	
	private String mAddress;
	private Alias mAddressAlias;
	
	public LJ1200DecoderState( AliasList aliasList )
	{
		super( aliasList );
	}
	
	public String getAddress()
	{
		return mAddress;
	}
	
	public Alias getAddressAlias()
	{
		return mAddressAlias;
	}
	
	@Override
    public void receive( Message message )
    {
		if( message instanceof LJ1200Message )
		{
			LJ1200Message lj = (LJ1200Message)message;
			
			if( lj.isValid() )
			{
				setAddress( lj.getAddress() );
				setAddressAlias( lj.getAddressAlias() );
				
				mAddresses.add( mAddress );

				broadcast( LJ1200CallEvent.getLJ1200Event( lj ) );

				broadcast( new DecoderStateEvent( this, Event.DECODE, State.DATA ) );
			}
		}
		else if( message instanceof LJ1200TransponderMessage )
		{
			
		}
    }

	private void setAddress( String address )
	{
		mAddress = address;
		broadcast( ChangedAttribute.TO_TALKGROUP );
	}

	private void setAddressAlias( Alias alias )
	{
		mAddressAlias = alias;
		broadcast( ChangedAttribute.TO_TALKGROUP_ALIAS );
	}
	
	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "=============================\n" );
		sb.append( "Decoder:\tLJ-1200I\n\n" );

		if( !mAddresses.isEmpty() )
		{
			sb.append( "Transponder Addresses:\n" );
			
			List<String> addresses = new ArrayList<String>( mAddresses );
			
			Collections.sort( addresses );
			
			for( String address: addresses )
			{
				sb.append( "\t" );
				sb.append( address );
				sb.append( "\n" );
			}
		}

		return sb.toString();
    }

	@Override
	public void init()
	{
		/* No initialization steps */
	}

	@Override
	public void receiveDecoderStateEvent( DecoderStateEvent event )
	{
		if( event.getEvent() == Event.RESET )
		{
			
		}
	}

	@Override
	public void reset()
	{
		mAddresses.clear();
		
		setAddress( null );
		setAddressAlias( null );
	}
}
