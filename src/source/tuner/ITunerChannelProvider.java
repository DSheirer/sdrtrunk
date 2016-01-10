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
package source.tuner;

import java.util.concurrent.RejectedExecutionException;

import source.SourceException;
import controller.ThreadPoolManager;

public interface ITunerChannelProvider
{
	/**
	 * Returns a tuner frequency channel source, tuned to the correct frequency
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
	 * @param channel - previously obtained tuner channel
	 */
	public abstract void releaseChannel( TunerChannelSource source );
	
	
}
