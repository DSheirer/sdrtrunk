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
package source;

import source.mixer.MixerManager;
import controller.ResourceManager;
import controller.channel.ProcessingChain;

public class SourceManager {
	/**
	 * NOTE: each source will auto-start once a listener registers on it to 
	 * receive samples and should auto-stop once a listener de-registers
	 * 
	 * Each channel object will handle the starting and stopping of each channel 
	 * processing chain.
	 * 
	 * Source Manager will reach out to the MixerManager or the TunerManager
	 * to get source objects to pass off to the channel object, as requested.
	 */
	
	private ResourceManager mResourceManager;

	public SourceManager( ResourceManager resourceManager )
	{
		mResourceManager = resourceManager;
	}
	
	public Source getSource( ProcessingChain channel )
	{
		Source retVal = null;

		if( channel != null )
		{
			switch( channel.getChannel()
					.getSourceConfiguration().getSourceType() )
			{
				case MIXER:
					retVal = MixerManager.getInstance().getSource( channel ); 
					break;
				case TUNER:
					retVal = mResourceManager.getTunerManager().getSource( channel );
					break;
				case NONE:
				default:
					break;
			}
		}
		
		return retVal;
	}
}
