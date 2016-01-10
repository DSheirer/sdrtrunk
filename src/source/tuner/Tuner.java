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
package source.tuner;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import settings.SettingsManager;
import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeProcessor;
import controller.ThreadPoolManager;

/**
 * Tuner - provides tuner channel sources, representing a channel frequency
 */
public abstract class Tuner implements IFrequencyChangeProcessor,
									   ITunerChannelProvider
{
	private final static Logger mLog = LoggerFactory.getLogger( Tuner.class );
	
	private String mName;

	/**
	 * Sample Listeners - these will typically be the DFT processor for spectral
	 * display, or it will be one or more tuner channel sources
	 */
	protected CopyOnWriteArrayList<Listener<ComplexBuffer>> 
		mSampleListeners = new CopyOnWriteArrayList<Listener<ComplexBuffer>>();
	
	protected CopyOnWriteArrayList<IFrequencyChangeProcessor> 
		mFrequencyChangeProcessors = new CopyOnWriteArrayList<IFrequencyChangeProcessor>();

	public Tuner( String name )
	{
		mName = name;
	}
	
	public String toString()
	{
		return mName;
	}
	
	public void dispose()
	{
		mSampleListeners.clear();
		mFrequencyChangeProcessors.clear();
	}
	
	public void setName( String name )
	{
		mName = name;
	}
	
	/**
	 * Return an editor panel for the tuner
	 */
	public abstract JPanel getEditor( SettingsManager settingsManager );


	/**
	 * Applies the settings from a saved tuner configuration setting object
	 */
	public abstract void apply( TunerConfiguration config )
					throws SourceException;

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
	 * Sample rate of complex data coming from the Tuner
	 * @return
	 */
	public abstract int getSampleRate();

	/**
	 * Sample size in bits
	 */
	public abstract double getSampleSize();
	
	/**
	 * Tuner Local Oscillator Frequency
	 * @return - frequency in Hertz
	 */
	public abstract long getFrequency() throws SourceException;

	/**
	 * Returns a tuner frequency channel source, tuned to the correct frequency
	 * 
	 * Note: ensure the concrete implementation registers the returned 
	 * tuner channel source as a listener on the tuner, to receive frequency
	 * change events.
	 * 
	 * @param frequency - desired frequency
	 * 
	 * @return - source for 48k sample rate
	 */
	public abstract TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
		TunerChannel channel ) throws RejectedExecutionException, SourceException;
	
	/**
	 * Releases the tuned channel resources
	 * 
	 * Note: ensure the concrete implementation unregisters the tuner channel
	 * source from the tuner as a frequency change listener
	 * 
	 * @param channel - previously obtained tuner channel
	 */
	public abstract void releaseChannel( TunerChannelSource source );
	
	/**
	 * Registers the listener to receive complex float sample arrays
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
     * Adds the frequency change listener to receive frequency and/or bandwidth
     * change events
     */
	public void addFrequencyChangeProcessor( IFrequencyChangeProcessor processor )
	{
		mFrequencyChangeProcessors.add( processor );
	}

	/**
	 * Removes the frequency change listener
	 */
    public void removeFrequencyChangeProcessor( IFrequencyChangeProcessor processor )
    {
    	mFrequencyChangeProcessors.remove( processor );
    }

    /**
     * Broadcasts a frequency change event to all registered listeners
     */
    public void broadcastFrequencyChange( long frequency )
    {
    	broadcastFrequencyChangeEvent( 
				new FrequencyChangeEvent( Event.NOTIFICATION_FREQUENCY_CHANGE, frequency ) );
    }

    /**
     * Broadcasts a sample rate change event to all registered listeners
     */
    public void broadcastSampleRateChange( int sampleRate )
    {
    	broadcastFrequencyChangeEvent( 
				new FrequencyChangeEvent( Event.NOTIFICATION_SAMPLE_RATE_CHANGE, sampleRate ) );
    }
    
    /**
     * Broadcasts actual sample rate change event to all registered listeners
     */
    public void broadcastActualSampleRateChange( int actualSampleRate )
    {
    	broadcastFrequencyChangeEvent( 
			new FrequencyChangeEvent( Event.NOTIFICATION_SAMPLE_RATE_CHANGE, actualSampleRate ) );
    }

    /**
     * Broadcasts a frequency change event to all registered listeners
     */
    public void broadcastFrequencyChangeEvent( FrequencyChangeEvent event )
    {
    	for( IFrequencyChangeProcessor processor: mFrequencyChangeProcessors )
    	{
    		processor.frequencyChanged( event );
    	}
    }

    /**
     * Frequency change listener method.  We receive change events from the
     * controller and rebroadcast them to all registered listeners.
     */
	public void frequencyChanged( FrequencyChangeEvent event )
	{
		broadcastFrequencyChangeEvent( event );
	}
}
