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

import java.util.concurrent.ScheduledExecutorService;

import message.Message;
import module.decode.DecoderType;
import module.decode.state.DecoderStateEvent.Event;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;

/**
 * Basic decoder channel state - provides the minimum channel state functionality
 * to support an always un-squelched audio decoder.
 */
public class AlwaysUnsquelchedDecoderState extends DecoderState
{
	private DecoderType mDecoderType;
	private String mChannelName;
	
	public AlwaysUnsquelchedDecoderState( DecoderType decoderType, String channelName )
	{
		super( null );
		
		mDecoderType = decoderType;
		mChannelName = channelName;
	}
	
	@Override
	public void init()
	{
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
		sb.append( mDecoderType );
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

	@Override
	public DecoderType getDecoderType()
	{
		return mDecoderType;
	}

	@Override
	public void start( ScheduledExecutorService executor )
	{
		broadcast( new Metadata( MetadataType.TO, mChannelName, false ) );
		broadcast( new DecoderStateEvent( this, Event.ALWAYS_UNSQUELCH, State.IDLE ) );
	}

	@Override
	public void stop()
	{
	}
}
