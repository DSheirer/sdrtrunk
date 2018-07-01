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

public abstract class Module
{
	/**
	 * Defines the basic component level class for all processing, demodulation
	 * and decoding components that can operate within a processing chain.
	 * 
	 * Modules can optionally implement any of the following interfaces:
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
	 */
	public Module()
	{
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
	public abstract void dispose();
	
}
