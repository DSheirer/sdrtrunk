/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package ua.in.smartjava.source.tuner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.ComplexBuffer;
import ua.in.smartjava.source.SourceException;
import ua.in.smartjava.source.tuner.frequency.FrequencyChangeEvent;
import ua.in.smartjava.source.tuner.frequency.IFrequencyChangeProcessor;

/**
 * Tuner - provides tuner ua.in.smartjava.channel sources, representing a ua.in.smartjava.channel frequency
 */
public abstract class Tuner implements ITunerChannelProvider
{
	private final static Logger mLog = LoggerFactory.getLogger( Tuner.class );
	
	private String mName;
	private TunerController mTunerController;
	/**
	 * Sample Listeners - these will typically be the DFT processor for spectral
	 * display, or it will be one or more tuner ua.in.smartjava.channel sources
	 */
	protected List<Listener<ComplexBuffer>> mSampleListeners = 
			new CopyOnWriteArrayList<>();
	
	protected List<Listener<TunerEvent>> mTunerChangeListeners =
			new ArrayList<>();
	
	public Tuner( String name, TunerController tunerController )
	{
		mName = name;
		mTunerController = tunerController;

		//Rebroadcast frequency and ua.in.smartjava.sample rate change events as tuner events
		mTunerController.addListener( new IFrequencyChangeProcessor()
		{
			@Override
			public void frequencyChanged( FrequencyChangeEvent event )
			{
				switch( event.getEvent() )
				{
					case NOTIFICATION_FREQUENCY_CHANGE:
						broadcast( new TunerEvent( Tuner.this, 
								ua.in.smartjava.source.tuner.TunerEvent.Event.FREQUENCY ));
						break;
					case NOTIFICATION_SAMPLE_RATE_CHANGE:
						broadcast( new TunerEvent( Tuner.this, 
								ua.in.smartjava.source.tuner.TunerEvent.Event.SAMPLE_RATE ));
						break;
					default:
						break;
				}
			}
		} );
	}
	
	public TunerController getTunerController()
	{
		return mTunerController;
	}
	
	public String toString()
	{
		return mName;
	}
	
	public void dispose()
	{
		mSampleListeners.clear();
	}
	
	public void setName( String name )
	{
		mName = name;
	}
	
	/**
	 * Unique identifier for this tuner, used to lookup a tuner configuration
	 * from the settings manager.
	 * 
	 * @return - unique identifier like a serial number, or a usb bus location
	 * or ip address and port.  Return some form of unique identification that 
	 * allows this tuner to be identified from among several of the same types
	 * of tuners.
	 */
	public abstract String getUniqueID();

	/**
	 * @return - tuner class enum entry
	 */
	public abstract TunerClass getTunerClass();

	/**
	 * @return - tuner type enum entry
	 */
	public abstract TunerType getTunerType();
	
	/**
	 * Name of this tuner object
	 * @return - string name of this tuner object
	 */
	public String getName()
	{
		return mName;
	}

	/**
	 * Sample size in ua.in.smartjava.bits
	 */
	public abstract double getSampleSize();
	
	/**
	 * Returns a tuner frequency ua.in.smartjava.channel ua.in.smartjava.source, tuned to the correct frequency
	 * 
	 * Note: ensure the concrete implementation registers the returned 
	 * tuner ua.in.smartjava.channel ua.in.smartjava.source as a listener on the tuner, to receive frequency
	 * change events.
	 * 
	 * @param frequency - desired frequency
	 * 
	 * @return - ua.in.smartjava.source for 48k ua.in.smartjava.sample rate
	 */
	public abstract TunerChannelSource getChannel( TunerChannel channel ) 
			throws RejectedExecutionException, SourceException;
	
	/**
	 * Releases the tuned ua.in.smartjava.channel resources
	 * 
	 * Note: ensure the concrete implementation unregisters the tuner ua.in.smartjava.channel
	 * ua.in.smartjava.source from the tuner as a frequency change listener
	 * 
	 * @param channel - previously obtained tuner ua.in.smartjava.channel
	 */
	public abstract void releaseChannel( TunerChannelSource source );
	
	/**
	 * Registers the listener to receive complex float ua.in.smartjava.sample arrays
	 */
    public void addListener( Listener<ComplexBuffer> listener )
    {
		mSampleListeners.add( listener );
    }

	/**
	 * Removes the registered listener
	 */
    public void removeListener( Listener<ComplexBuffer> listener )
    {
	    mSampleListeners.remove( listener );
    }

    /**
     * Broadcasts the samples to all registered listeners
     */
    public void broadcast( ComplexBuffer sampleBuffer )
    {
    	for( Listener<ComplexBuffer> listener: mSampleListeners )
    	{
    		listener.receive( sampleBuffer );
    	}
    }

	/**
	 * Registers the listener
	 */
    public void addTunerChangeListener( Listener<TunerEvent> listener )
    {
		mTunerChangeListeners.add( listener );
    }

	/**
	 * Removes the registered listener
	 */
    public void removeTunerChangeListener( Listener<TunerEvent> listener )
    {
	    mTunerChangeListeners.remove( listener );
    }

    /**
     * Broadcasts the tuner change event
     */
    public void broadcast( TunerEvent event )
    {
    	for( Listener<TunerEvent> listener: mTunerChangeListeners )
    	{
    		listener.receive( event );
    	}
    }
}
