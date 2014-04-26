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
package decode;

import controller.channel.ChannelNode;
import decode.config.DecodeConfiguration;
import decode.ltrnet.LTRNetEditor;
import decode.ltrstandard.LTRStandardEditor;
import decode.mpt1327.MPT1327Editor;
import decode.nbfm.NBFMEditor;
import decode.passport.PassportEditor;

public class DecodeEditorFactory
{
	public static DecodeEditor getPanel( DecodeConfiguration config, 
										 ChannelNode channelNode )
	{
		DecodeEditor configuredPanel;
		
		switch( config.getDecoderType() )
		{
			case NBFM:
				configuredPanel = new NBFMEditor( config );
				break;
			case LTR_STANDARD:
				configuredPanel = new LTRStandardEditor( config );
				break;
			case LTR_NET:
				configuredPanel = new LTRNetEditor( config );
				break;
			case MPT1327:
				configuredPanel = new MPT1327Editor( config, channelNode );
				break;
			case PASSPORT:
				configuredPanel = new PassportEditor( config );
				break;
			default:
				configuredPanel = new DecodeEditor( config );
				break;
		}
		
		return configuredPanel;
	}
}
