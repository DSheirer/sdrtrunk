/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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

package ua.in.smartjava.audio.squelch;

import ua.in.smartjava.sample.Broadcaster;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.sample.real.RealSampleListener;
import ua.in.smartjava.sample.real.RealToRealBufferAssembler;

/**
 * Squelch control - receives single ua.in.smartjava.audio samples or real ua.in.smartjava.audio buffers and
 * adjusts squelch state either based on the ua.in.smartjava.audio samples or based on external
 * control inputs.  
 * 
 * Assembles received single ua.in.smartjava.audio samples into real ua.in.smartjava.sample buffers for
 * consumption by an external ua.in.smartjava.audio processor, like the AudioPacketProcessor
 * class.
 */
public abstract class SquelchController 
				implements Listener<RealBuffer>, RealSampleListener
{
	private Broadcaster<SquelchState> mSquelchStateBroadcaster = new Broadcaster<>();
	private Listener<RealBuffer> mAudioListener;
	protected SquelchState mSquelchState = SquelchState.UNSQUELCH;
	protected SquelchMode mSquelchMode = SquelchMode.MANUAL;
	
	private RealToRealBufferAssembler mSampleAssembler = new RealToRealBufferAssembler( 512 );
	
	public SquelchController()
	{
		mSampleAssembler.setListener( this );
	}
	
	@Override
	public void receive( float sample )
	{
		mSampleAssembler.receive( sample );
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		if( mAudioListener != null )
		{
			mAudioListener.receive( buffer );
		}
	}

	public abstract void setSquelchMode( SquelchMode mode );

	public SquelchMode getSquelchMode()
	{
		return mSquelchMode;
	}

	public void setSquelchState( SquelchState state )
	{
		mSquelchState = state;
	}

	public SquelchState getSquelchState()
	{
		return mSquelchState;
	}

	public boolean isSquelched()
	{
		switch( mSquelchMode )
		{
			case AUTOMATIC:
			case MANUAL:
				return mSquelchState == SquelchState.SQUELCH;
			case NONE:
				return false;
			default:
				return false;
		}
	}
	
	public void addSquelchStateListener( Listener<SquelchState> listener )
	{
		if( listener != null )
		{
			mSquelchStateBroadcaster.addListener( listener );
			
			/* Send current squelch state to the listener */
			listener.receive( mSquelchState );
		}
	}

	public void removeSquelchStateListener( Listener<SquelchState> listener )
	{
		mSquelchStateBroadcaster.removeListener( listener );
	}
	
	public void setAudioListener( Listener<RealBuffer> listener )
	{
		mAudioListener = listener;
	}
	
	public void removeAudioListener( Listener<RealBuffer> listener )
	{
		mAudioListener = null;
	}
}
