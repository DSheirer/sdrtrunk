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
package decode.lj1200;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingsManager;
import controller.state.AuxStatePanel;
import controller.state.ChannelState.ChangedAttribute;

public class LJ1200Panel extends AuxStatePanel
{
    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL = "LJ-1200";

    private JLabel mProtocol = new JLabel( PROTOCOL );
	
	public LJ1200Panel( SettingsManager settingsManager, LJ1200ChannelState state )
	{
		super( settingsManager, state );
		
		init();
	}
	
	public void dispose()
	{
		super.dispose();
	}
	
	public LJ1200ChannelState getState()
	{
		return (LJ1200ChannelState)mAuxChannelState;
	}
	
	private void init()
	{
		setOpaque( false ); //Use the parent panel's background color
		
		setLayout( new MigLayout( "insets 1 0 0 0", "[grow,fill]", "[]0[]0[]") );

		addLabel( mProtocol, false );
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
			ColorSetting color = (ColorSetting)setting;

			if( color.getColorSettingName() == 
					ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER )
			{
				mProtocol.setForeground( color.getColor() );
			}
		}
    }
}
