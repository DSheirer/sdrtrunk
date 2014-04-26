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
package record;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import properties.SystemProperties;
import record.config.RecordConfiguration;
import record.wave.ComplexWaveRecorder;
import record.wave.FloatWaveRecorder;
import util.TimeStamp;
import controller.channel.ProcessingChain;

public class RecorderManager
{
	public static final int sSAMPLE_RATE = 48000;

	public RecorderManager()
	{
	}
	
	public List<Recorder> getRecorder( ProcessingChain channel )
	{
		/* Note: the file suffix (ie .wav) gets added by the recorder */

    	//Get the base recording filename
        StringBuilder sb = new StringBuilder();
        sb.append( SystemProperties.getInstance()
        					.getApplicationFolder( "recordings" ) );
        sb.append( File.separator );
        sb.append( TimeStamp.getTimeStamp( "_" ) );
        sb.append(  "_" );
        sb.append( channel.getChannel().getName() );
        
		ArrayList<Recorder> retVal = new ArrayList<Recorder>();

		RecordConfiguration config = channel.getChannel().getRecordConfiguration();
		
		for( RecorderType recorder: config.getRecorders() )
		{
			switch( recorder )
			{
				case AUDIO:
					retVal.add( new FloatWaveRecorder( sSAMPLE_RATE, 
							sb.toString() + "_audio" ) );
					break;
				case BASEBAND:
					retVal.add( new ComplexWaveRecorder( sSAMPLE_RATE, 
							sb.toString() + "_baseband" ) );
					break;
			}
		}

		
		return retVal;
	}
}
