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
package io.github.dsheirer.source.mixer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.HashMap;

/**
 * THIS CLASS  IS SCHEDULED FOR DELETION *****************************
 */

public class SystemMixerDevices {

	/**
	 * Returns a hashmap of mixer names and target data lines that support the
	 * audio format argument
	 * 
	 * @param format - requested audio format
	 * @return - mixer target data lines that support the requested audio format
	 */
	public static HashMap<String,TargetDataLine> getMixers( AudioFormat format )
	{
	    HashMap<String,TargetDataLine> retVal = new HashMap<String,TargetDataLine>();
	    
        DataLine.Info datalineInfo=new DataLine.Info(TargetDataLine.class, format );

        for( Mixer.Info mixerInfo: AudioSystem.getMixerInfo() )
        {
            Mixer mixer = AudioSystem.getMixer( mixerInfo );

            if( mixer != null )
            {
                try
                {
                    TargetDataLine tdl = (TargetDataLine) mixer.getLine( datalineInfo );
                    
                    if( tdl != null )
                    {
                        retVal.put( mixerInfo.getName(), tdl );
                    }
                }
                catch( IllegalArgumentException iae )
                {
                    //Do nothing ... mixer doesn't support the audio format
                }
                catch( LineUnavailableException e )
                {
                    //Do nothing ... mixer doesn't support the audio format
                }
            }
        }
        
        return retVal;
	}

	public static String getSoundDevices()
	{
		StringBuilder sb = new StringBuilder();
		
		for( Mixer.Info mixerInfo: AudioSystem.getMixerInfo() )
		{
			sb.append( "\n--------------------------------------------------" );
			sb.append("\nMIXER name:").append(mixerInfo.getName())
					.append("\n      desc:").append(mixerInfo.getDescription())
					.append("\n      vendor:").append(mixerInfo.getVendor())
					.append("\n      version:").append(mixerInfo.getVersion())
					.append("\n");
			
			Mixer mixer = AudioSystem.getMixer( mixerInfo );

			Line.Info[] sourceLines = mixer.getSourceLineInfo();
			
			for( Line.Info lineInfo: sourceLines )
			{
				sb.append("      SOURCE LINE desc:").append(lineInfo)
						.append("\n               class:").append(lineInfo.getClass())
						.append("\n               lineclass:").append(lineInfo.getLineClass())
						.append("\n");
			}

			Line.Info[] targetLines = mixer.getTargetLineInfo();
			
			for( Line.Info lineInfo: targetLines )
			{
				sb.append("      TARGET LINE desc:").append(lineInfo)
						.append("\n                class:").append(lineInfo.getClass())
						.append("\n                lineclass:").append(lineInfo.getLineClass())
						.append("\n");
			}
		}
		
		return sb.toString();
	}
}
