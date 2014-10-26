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
import sample.simplex.SimplexSampleBroadcaster;
import sample.simplex.SimplexSampleListener;
import source.Source.SampleType;
import eventlog.MessageEventLogger;

public abstract class Decoder implements Listener<Message>
{
	private MessageEventLogger mEventLogger;
	private Broadcaster<Message> mMessageBroadcaster = new Broadcaster<Message>();
	private SimplexSampleBroadcaster mSimplexBroadcaster = new SimplexSampleBroadcaster();
	private Broadcaster<ComplexSample> mComplexBroadcaster = 
							new Broadcaster<ComplexSample>();

	protected SampleType mSourceSampleType;
	protected ArrayList<Decoder> mAuxiliaryDecoders = new ArrayList<Decoder>();

	/**
	 * Abstract decoder class.
	 * 
	 * @param sampleType - complex (I/Q) or simplex (demodulated) sample type.
	 * Each decoder instance will be configured to receive either complex or 
	 * simplex samples.  When configured to receive complex samples, the
	 * decoder will implement a demodulator component and feed the output from
	 * that demodulator back onto itself via the simplex sample interface, so 
	 * that any added auxiliary decoders will then receive the demodulated 
	 * output stream.
	 */
	public Decoder( SampleType sampleType )
	{
		mSourceSampleType = sampleType;
	}
	
	/**
	 * Returns a simplex (ie demodulated) sample listener interface for 
	 * connecting this decoder to a simplex sample stream provider
	 */
	public SimplexSampleListener getSimplexReceiver()
	{
		return mSimplexBroadcaster;
	}
	
	/**
	 * Returns a complex listener interface for connecting this decoder to a 
	 * float stream provider
	 */
	public Listener<ComplexSample> getComplexReceiver()
	{
		return (Listener<ComplexSample>)mComplexBroadcaster;
	}

	/**
	 * Returns the primary decoder type for this decoder
	 */
	public abstract DecoderType getType();

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
		mSimplexBroadcaster.clear();
	}

	/**
	 * Adds the auxiliary decoder (piggyback) to this decoder.  
	 * 
	 * Note: we assume that the auxiliary decoder is designed to receive 
	 * demodulated samples, thus we automatically register the aux decoder to 
	 * receive the demodulated sample stream.
	 * 
	 * Registers this decoder to receive the message output stream from the 
	 * auxiliary decoder, so that those messages can be echoed and included in 
	 * the consolidated message stream to all message listeners registered on 
	 * the primary decoder.
	 */
	public void addAuxiliaryDecoder( Decoder decoder )
	{
		mAuxiliaryDecoders.add( decoder );
		mSimplexBroadcaster.addListener( decoder.getSimplexReceiver() );
		decoder.addMessageListener( this );
	}
	
	/**
	 * Returns a list of all auxiliary decoders that have been added to this
	 * processing chain
	 */
	public List<Decoder> getAuxiliaryDecoders()
	{
		return mAuxiliaryDecoders;
	}

	/**
	 * Sets the MessageEventLogger for this processing chain
	 */
	public void setEventLogger( MessageEventLogger eventLogger )
	{
		mEventLogger = eventLogger;
	}

	/**
	 * Returns the MessageEventLogger for this processing chain
	 */
	public MessageEventLogger getEventLogger()
	{
		return mEventLogger;
	}

	/**
	 * Main receiver method for all demodulators to send their decoded messages
	 * so that they will be broadcast to all registered listeners
	 */
	@Override
    public void receive( Message message )
    {
		send( message );
    }

	/**
	 * Broadcasts the message to all registered listeners
	 */
    public void send( Message message )
    {
    	if( mMessageBroadcaster != null )
    	{
        	mMessageBroadcaster.receive( message );
    	}
    }

    /**
     * Adds a listener to receiving decoded messages from all attached decoders
     */
    public void addMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.addListener( listener );
    }

    /**
     * Removes the listener from receiving decoded messages from all attached
     * decoders
     */
    public void removeMessageListener( Listener<Message> listener )
    {
		mMessageBroadcaster.removeListener( listener );
    }

    /**
     * Adds a simplex sample listener to receive unfiltered demodulated samples
     */
    public abstract void addUnfilteredSimplexSampleListener( SimplexSampleListener listener );

    /**
     * Adds a simplex sample listener to receive demodulated samples
     */
    public void addSimplexSampleListener( SimplexSampleListener listener )
    {
		mSimplexBroadcaster.addListener( listener );
    }

    /**
     * Remove the simplex sample listener from receiving demodulated samples
     */
    public void removeSimplexListener( SimplexSampleListener listener )
    {
		mSimplexBroadcaster.removeListener( listener );
    }

    /**
     * Adds a complex (I/Q) sample listener to receive copy of the inbound
     * complex sample stream
     */
    public void addComplexListener( Listener<ComplexSample> listener )
    {
		mComplexBroadcaster.addListener( listener );
    }

    /**
     * Removes the complex (I/Q) sample listener from receiving samples
     */
    public void removeComplexListener( Listener<ComplexSample> listener )
    {
		mComplexBroadcaster.removeListener( listener );
    }
}
