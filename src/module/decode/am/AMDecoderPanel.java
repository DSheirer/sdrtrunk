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
package module.decode.am;

import javax.swing.JLabel;

import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;

public class AMDecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol = new JLabel( "AM" );
	
	public AMDecoderPanel( SettingsManager settingsManager, AMDecoder decoder )
	{
		super( settingsManager, decoder );
		
		init();
	}
	
	public void init()
	{
		mProtocol.setFont( mFontDecoder );

		add( mProtocol, "wrap" );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
    }

	@Override
    public void settingChanged( Setting setting )
    {
		super.settingChanged( setting );
		
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;
			
			switch( colorSetting.getColorSettingName() )
			{
				case CHANNEL_STATE_LABEL_DECODER:
					if( mProtocol != null )
					{
						mProtocol.setForeground( mColorLabelDecoder );
					}
					break;
				default:
					break;
			}
		}
    }
}
