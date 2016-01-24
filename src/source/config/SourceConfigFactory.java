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
package source.config;

import source.SourceType;

public class SourceConfigFactory
{
	public static SourceConfiguration getDefaultSourceConfiguration()
	{
		return getSourceConfiguration( SourceType.NONE );
	}
	
	public static SourceConfiguration 
		getSourceConfiguration( SourceType source )
	{
		SourceConfiguration retVal;

		switch( source )
		{
			case MIXER:
				retVal = new SourceConfigMixer();
				break;
			case TUNER:
				retVal = new SourceConfigTuner();
				break;
			case RECORDING:
				retVal = new SourceConfigRecording();
				break;
			case NONE:
			default:
				retVal = new SourceConfigNone();
				break;
		}
		
		return retVal;
	}

	/**
	 * Creates a copy of the configuration
	 */
	public static SourceConfiguration copy( SourceConfiguration config )
	{
		if( config != null )
		{
			switch( config.getSourceType() )
			{
				case MIXER:
					SourceConfigMixer originalMixer = (SourceConfigMixer)config;
					SourceConfigMixer copyMixer = new SourceConfigMixer();
					copyMixer.setChannel( originalMixer.getChannel() );
					copyMixer.setMixer( originalMixer.getMixer() );
					return copyMixer;
				case RECORDING:
					SourceConfigRecording originalRec = (SourceConfigRecording)config;
					SourceConfigRecording copyRec = new SourceConfigRecording();
					copyRec.setFrequency( originalRec.getFrequency() );
					copyRec.setRecordingAlias( originalRec.getRecordingAlias() );
					return copyRec;
				case TUNER:
					SourceConfigTuner originalTuner = (SourceConfigTuner)config;
					SourceConfigTuner copyTuner = new SourceConfigTuner();
					copyTuner.setFrequency( originalTuner.getFrequency() );
					return copyTuner;
				case NONE:
				default:
					return new SourceConfigNone();
			}
		}
		
		return null;
	}
}
