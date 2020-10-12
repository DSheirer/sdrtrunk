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

package io.github.dsheirer.module;

import com.google.common.eventbus.EventBus;

/**
 * Defines the basic component level class for all processing, demodulation and decoding components that can operate
 * within a processing chain.
 *
 * Modules can optionally implement any of the following legacy interfaces:
 *
 * IAudioPacketListener				consumes audio packets
 * IAudioPacketProvider				produces audio packets
 * ICallEventListener				consumes call events
 * ICallEventProvider				provides call events
 * IChannelEventListener			consumes channel events
 * IChannelEventProvider			provides channel events (reset, selection, start, etc)
 * IFrequencyCorrectionController	provides tuned frequency error corrections
 * IMessageProvider					produces messages
 * IRealBufferListener				consumes demodulated real sample buffers
 * IRealBufferListener				produces demodulated real sample buffers
 * IReusableComplexBufferListener	consumes complex samples and normally produces demodulated real sample buffers
 * ISquelchStateListener			consumes squelch states
 * ISquelchStateProvider			provides squelch states
 *
 * TODO: convert all pub/sub interfaces to use the inter-module event bus for event broadcast/subscribe and remove
 * all of these interfaces.
 */
public abstract class Module
{

	/**
	 * Event bus for inter-module communication of processing chain events.  Note: this is an externally provided
	 * resource, typically provided by the ProcessingChain parent for each module.
	 */
	private EventBus mInterModuleEventBus;

	/**
	 * Constructs an instance
	 */
	public Module()
	{
	}

	/**
	 * Sets the event bus to be used for inter-module event broadcasting and subscribing.
	 * @param interModuleEventBus to use
	 */
	public void setInterModuleEventBus(EventBus interModuleEventBus)
	{
		//Unregister from the current event bus (if one exists)
		if(hasInterModuleEventBus())
		{
			getInterModuleEventBus().unregister(this);
		}

		mInterModuleEventBus = interModuleEventBus;

		//Auto-register with the event bus
		if(hasInterModuleEventBus())
		{
			getInterModuleEventBus().register(this);
		}
	}

	/**
	 * Event bus for inter-module communication.  Note: use hasEventBus() to check that the module is assigned a bus.
	 * @return event bus or null if one has not been established.
	 */
	protected EventBus getInterModuleEventBus()
	{
		return mInterModuleEventBus;
	}

	/**
	 * Indicates if this module has an assigned event bus.
	 * @return true if event bus is non-null.
	 */
	protected boolean hasInterModuleEventBus()
	{
		return mInterModuleEventBus != null;
	}

	/**
	 * Initialize or reset all internal states to default - prepare to start
	 * processing or resume processing, potentially with a different source.
	 * 
	 * This method is invoked after constructing a module, and following any stop()
	 * calls and before any start() calls.  This allows the module to prior to
	 * starting a module and starting it again processing a newly tuned channel,
	 * like a traffic channel.
	 */
	public abstract void reset();

	/**
	 * Start processing.
	 */
	public abstract void start();

	/**
	 * Stop processing
	 */
	public abstract void stop();
	
	/**
	 * Dispose of all resources and listeners and prepare for garbage collection
	 */
	public void dispose()
	{
		if(hasInterModuleEventBus())
		{
			getInterModuleEventBus().unregister(this);
		}
	}
}
