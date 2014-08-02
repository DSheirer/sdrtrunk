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

import source.config.SourceConfiguration;
import source.mixer.MixerEditor;
import source.recording.RecordingEditor;
import source.tuner.TunerEditor;
import controller.ResourceManager;

public class SourceEditorFactory
{
	public static SourceEditor getPanel( ResourceManager resourceManager,
										 SourceConfiguration config )
	{
		SourceEditor configuredPanel;
		
		switch( config.getSourceType() )
		{
			case MIXER:
				configuredPanel = new MixerEditor( resourceManager, config );
				break;
			case TUNER:
				configuredPanel = new TunerEditor( resourceManager, config );
				break;
			case RECORDING:
				configuredPanel = new RecordingEditor( resourceManager, config );
				break;
			case NONE:
			default:
				configuredPanel = new EmptySourceEditor( resourceManager, config );
				break;
		}
		
		return configuredPanel;
	}
}
