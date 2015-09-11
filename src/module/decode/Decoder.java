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
package module.decode;

import message.IMessageProvider;
import message.Message;
import module.Module;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventProvider;
import module.decode.state.DecoderState;
import module.decode.state.DecoderStateEvent;
import module.decode.state.IDecoderStateEventListener;
import module.decode.state.IDecoderStateEventProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;

public abstract class Decoder extends Module 
							  implements ICallEventProvider, 
										 IDecoderStateEventListener, 
										 IDecoderStateEventProvider, 
										 IMessageProvider
{
	private final static Logger mLog = LoggerFactory.getLogger( Decoder.class );

	/**
	 * Broadcasts decoded messages both to the internal decoder state and any
	 * external registered message listeners
	 */
	protected Broadcaster<Message> mMessageBroadcaster = 
			new Broadcaster<Message>();

	protected DecoderState mDecoderState;
	
	/**
	 * Decoder - parent class for all decoders, demodulators and components.  
	 */
	public Decoder( DecoderState decoderState )
	{
		assert( decoderState != null );
		
		mDecoderState = decoderState;
		mMessageBroadcaster.addListener( mDecoderState );
	}

	@Override
	public void dispose()
	{
		mMessageBroadcaster.dispose();
		mMessageBroadcaster = null;
	}

	/**
	 * Allows decoder to broadcast initialization messages/events so that 
	 * external modules can be correctly configured.  This method is invoked
	 * prior to processing start.
	 */
	public void reset()
	{
		mDecoderState.init();
	}

	/**
	 * Start the decoder
	 */
	@Override
	public void start()
	{
	}

	/**
	 * Stop the decoder
	 */
	@Override
	public void stop()
	{
	}

	/**
	 * Identifies the decoder type (ie protocol) 
	 */
	public abstract DecoderType getDecoderType();


    /**
     * Adds a listener for receiving decoded messages from this decoder
     */
    @Override
    public void addMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.addListener( listener );
    }

    /**
     * Removes the listener from receiving decoded messages from all attached
     * decoders
     */
    @Override
    public void removeMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.removeListener( listener );
    }
    
	/**
	 * Decoder State - tracks the state of the decoder
	 */
	public DecoderState getDecoderState()
	{
		return mDecoderState;
	}
	
	@Override
	public Listener<DecoderStateEvent> getDecoderStateListener()
	{
		return mDecoderState.getDecoderStateListener();
	}

	@Override
	public void setDecoderStateListener( Listener<DecoderStateEvent> listener )
	{
		mDecoderState.setDecoderStateListener( listener );
	}

	@Override
	public void removeDecoderStateListener()
	{
		mDecoderState.removeDecoderStateListener();
	}

	@Override
	public void setCallEventListener( Listener<CallEvent> listener )
	{
		mDecoderState.setCallEventListener( listener );
	}

	@Override
	public void removeCallEventListener()
	{
		mDecoderState.removeCallEventListener();
	}
}
