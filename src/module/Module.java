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

package module;

public abstract class Module
{
	/**
	 * Defines the basic component level class for all processing, demodulation
	 * and decoding components that can operate within a processing chain.
	 * 
	 * Modules can optionally implement any of the following interfaces:
	 * 
	 * IAudioPacketProvider				produces audio packets
	 * ICallEventProvider				call events
	 * IChannelEventProvider			channel events (reset, selection, start, etc)
	 * IComplexBufferListener			consumes complex samples and normally 
	 * 									produces demodulated real sample buffers
	 * IFrequencyCorrectionController	provides tuned frequency error corrections
	 * IMessageProvider					produces messages
	 * IRealBufferListener				consumes demodulated real sample buffers
	 * IRealBufferListener				produces demodulated real sample buffers
	 * ISquelchStateListener			consumes squelch states
	 * ISquelchStateProvider			provides squelch states
	 */
	public Module()
	{
	}

	/**
	 * Dispose of all resources and listeners and prepare for garbage collection
	 */
	public abstract void dispose();

	/**
	 * Initializes or reset all internal states to default - prepare to process 
	 * again.  This method is usually invoked between stopping a module and 
	 * starting it again processing a newly tuned channel, like a traffic 
	 * channel.
	 */
	public abstract void init();
}
