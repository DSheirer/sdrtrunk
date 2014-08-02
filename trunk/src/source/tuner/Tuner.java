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

import sample.Listener;
import source.SourceException;
import controller.ResourceManager;
import controller.ThreadPoolManager;

/**
 * Tuner - provides tuner channel sources, representing a channel frequency
 */
public abstract class Tuner implements FrequencyChangeBroadcaster,
									   FrequencyChangeListener,
									   TunerChannelProvider
{
	private String mName;

	protected CopyOnWriteArrayList<Listener<Float[]>> 
		mSampleListeners = new CopyOnWriteArrayList<Listener<Float[]>>();
	
	protected CopyOnWriteArrayList<FrequencyChangeListener> 
		mFrequencyChangeListeners = new CopyOnWriteArrayList<FrequencyChangeListener>();

	public Tuner( String name )
	{
		mName = name;
	}
	
	/**
	 * Return an editor panel for the tuner
	 */
	public abstract JPanel getEditor( ResourceManager resourceManager );


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
    public void addListener( Listener<Float[]> listener )
    {
		mSampleListeners.add( listener );
    }

	/**
	 * Removes the registered listener
	 */
    public void removeListener( Listener<Float[]> listener )
    {
	    mSampleListeners.remove( listener );
    }

    /**
     * Broadcasts the samples to all registered listeners
     */
    public void broadcast( Float[] samples )
    {
    	for( Listener<Float[]> listener: mSampleListeners )
    	{
    		listener.receive( samples );
    	}
    }

    /**
     * Adds the frequency change listener to receive frequency and/or bandwidth
     * change events
     */
	public void addListener( FrequencyChangeListener listener )
	{
		mFrequencyChangeListeners.add( listener );
	}

	/**
	 * Removes the frequency change listener
	 */
    public void removeListener( FrequencyChangeListener listener )
    {
    	mFrequencyChangeListeners.remove( listener );
    }

    /**
     * Broadcasts a frequency change event to all registered listeners
     */
    public void broadcastFrequencyChange( long frequency, int bandwidth )
    {
    	for( FrequencyChangeListener listener: mFrequencyChangeListeners )
    	{
    		listener.frequencyChanged( frequency, bandwidth );
    	}
    }

    /**
     * Frequency change listener method.  We receive change events from the
     * controller and rebroadcast them to all registered listeners.
     */
	public void frequencyChanged( long frequency, int bandwidth )
	{
		broadcastFrequencyChange( frequency, bandwidth );
	}
}
