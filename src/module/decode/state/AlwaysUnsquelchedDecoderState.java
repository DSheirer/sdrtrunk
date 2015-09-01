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
package module.decode.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import message.Message;
import module.ProcessingChain;
import module.decode.state.DecoderStateEvent.Event;

/**
 * Basic decoder channel state - provides the minimum channel state functionality
 * to support an always un-squelched audio decoder.
 */
public class AlwaysUnsquelchedDecoderState extends DecoderState
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AlwaysUnsquelchedDecoderState.class );
	
	private String mProtocol;
	
	public AlwaysUnsquelchedDecoderState( String protocol )
	{
		super( null );
		
		mProtocol = protocol;
	}
	
	@Override
	public void init()
	{
		mLog.debug( "init() - broadcasting unsquelch event" );
		
		broadcast( new DecoderStateEvent( this, Event.ALWAYS_UNSQUELCH, State.IDLE ) );
	}
	
	@Override
	public void reset()
	{
		/* No reset actions needed */
	}

	@Override
    public String getActivitySummary()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Activity Summary\n" );
		sb.append( "\tDecoder:\t" );
		sb.append( mProtocol );
		sb.append( "\n\n" );
		
		return sb.toString();
    }

	@Override
	public void receive( Message t )
	{
		/* Not implemented */
	}

	@Override
	public void receiveDecoderStateEvent( DecoderStateEvent event )
	{
		/* Not implemented */
	}
}
