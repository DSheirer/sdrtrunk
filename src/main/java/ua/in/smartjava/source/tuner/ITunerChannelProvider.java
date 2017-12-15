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

import java.util.concurrent.RejectedExecutionException;

import ua.in.smartjava.source.SourceException;

public interface ITunerChannelProvider
{
	/**
	 * Returns a tuner frequency ua.in.smartjava.channel ua.in.smartjava.source, tuned to the correct frequency
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
	 * @param channel - previously obtained tuner ua.in.smartjava.channel
	 */
	public abstract void releaseChannel( TunerChannelSource source );
	
	
}
