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
package decode;

import java.util.ArrayList;
import java.util.List;

import message.Message;
import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexSample;
import source.Source.SampleType;
import eventlog.MessageEventLogger;

public abstract class Decoder implements Listener<Message>
{
	private MessageEventLogger mEventLogger;
	private Broadcaster<Message> mMessageBroadcaster = new Broadcaster<Message>();
	private Broadcaster<Float> mFloatBroadcaster = new Broadcaster<Float>();
	private Broadcaster<ComplexSample> mComplexBroadcaster = 
							new Broadcaster<ComplexSample>();

	protected SampleType mSourceSampleType;
	protected ArrayList<Decoder> mAuxiliaryDecoders = new ArrayList<Decoder>();
	
	public Decoder( SampleType sampleType )
	{
		mSourceSampleType = sampleType;
	}
	
	/**
	 * Returns a float listener interface for connecting this decoder to a 
	 * float stream provider
	 */
	public Listener<Float> getFloatReceiver()
	{
		return (Listener<Float>)mFloatBroadcaster;
	}
	
	/**
	 * Returns a complex listener interface for connecting this decoder to a 
	 * float stream provider
	 */
	public Listener<ComplexSample> getComplexReceiver()
	{
		return (Listener<ComplexSample>)mComplexBroadcaster;
	}
	
	public abstract DecoderType getType();
	
	public void addAuxiliaryDecoder( Decoder decoder )
	{
		mAuxiliaryDecoders.add( decoder );
		mFloatBroadcaster.addListener( decoder.getFloatReceiver() );
		decoder.addMessageListener( this );
	}
	
	/**
	 * Cleanup method.  Invoke this method after stop and before delete.
	 */
	public void dispose()
	{
		for( Decoder auxiliaryDecoder: mAuxiliaryDecoders )
		{
			auxiliaryDecoder.dispose();
		}
		
		mAuxiliaryDecoders.clear();

		mMessageBroadcaster.clear();
		mComplexBroadcaster.clear();
		mFloatBroadcaster.clear();
	}
	
	public List<Decoder> getAuxiliaryDecoders()
	{
		return mAuxiliaryDecoders;
	}
	
	public void setEventLogger( MessageEventLogger eventLogger )
	{
		mEventLogger = eventLogger;
	}
	
	public MessageEventLogger getEventLogger()
	{
		return mEventLogger;
	}

	@Override
    public void receive( Message message )
    {
		send( message );
    }

    public void send( Message message )
    {
    	if( mMessageBroadcaster != null )
    	{
        	mMessageBroadcaster.receive( message );
    	}
    }
	
    public void addMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.addListener( listener );
    }

    public void removeMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.removeListener( listener );
    }

    public void addFloatListener( Listener<Float> listener )
    {
		mFloatBroadcaster.addListener( listener );
    }

    public void removeFloatListener( Listener<Float> listener )
    {
		mFloatBroadcaster.removeListener( listener );
    }

    public void addComplexListener( Listener<ComplexSample> listener )
    {
		mComplexBroadcaster.addListener( listener );
    }

    public void removeComplexListener( Listener<ComplexSample> listener )
    {
		mComplexBroadcaster.removeListener( listener );
    }
}
