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
package decode.fleetsync2;

import java.awt.EventQueue;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingsManager;
import controller.state.AuxStatePanel;
import controller.state.ChannelState.ChangedAttribute;

public class FleetsyncPanel extends AuxStatePanel
{
    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL = "FSync II";

    private JLabel mFromLabel = new JLabel( " " );
    private JLabel mFrom = new JLabel();
    private JLabel mFromAlias = new JLabel();
    
    private JLabel mToLabel = new JLabel();
    private JLabel mTo = new JLabel( " " );
    private JLabel mToAlias = new JLabel();

    private JLabel mProtocol = new JLabel( PROTOCOL );
    private JLabel mMessage = new JLabel();
	private JLabel mMessageType = new JLabel();
	
	public FleetsyncPanel( SettingsManager settingsManager, 
						   FleetsyncChannelState state )
	{
		super( settingsManager, state );
		
		init();
	}
	
	public void dispose()
	{
		super.dispose();
	}
	
	public FleetsyncChannelState getState()
	{
		return (FleetsyncChannelState)mAuxChannelState;
	}
	
	private void init()
	{
		setOpaque( false ); //Use the parent panel's background color
		
		setLayout( new MigLayout( "insets 1 0 0 0", "[grow,fill]", "[]0[]0[]") );

		addLabel( mFromLabel, false );
		addLabel( mFrom, false );
		addLabel( mFromAlias, true );

		addLabel( mToLabel, false );
		addLabel( mTo, false );
		addLabel( mToAlias, true );

		addLabel( mProtocol, false );
		addLabel( mMessageType, false );
		addLabel( mMessage, true );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				switch( changedAttribute )
				{
					case FROM_TALKGROUP:
						String from = getState().getFleetIDFrom();
						
						if( from != null )
						{
							mFromLabel.setText( "FM:" );
						}
						else
						{
							mFromLabel.setText( " " );
						}
						mFrom.setText( from );
						break;
					case FROM_TALKGROUP_ALIAS:
						updateAliasLabel( mFromAlias, 
										  getState().getFleetIDFromAlias() );
						break;
					case TO_TALKGROUP:
						String to = getState().getFleetIDTo();
						
						if( to != null )
						{
							mToLabel.setText( "TO:" );
						}
						else
						{
							mToLabel.setText( " " );
						}
						mTo.setText( to );
						break;
					case TO_TALKGROUP_ALIAS:
						updateAliasLabel( mToAlias, 
								  getState().getFleetIDToAlias() );
						break;
					case MESSAGE:
						mMessage.setText( getState().getMessage() );
						break;
					case MESSAGE_TYPE:
						mMessageType.setText( getState().getMessageType() );
						break;
				}
				
				repaint();
            }
		} );
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
				mFromLabel.setForeground( color.getColor() );
				mFrom.setForeground( color.getColor() );
				mFromAlias.setForeground( color.getColor() );
				mToLabel.setForeground( color.getColor() );
				mTo.setForeground( color.getColor() );
				mToAlias.setForeground( color.getColor() );
				mProtocol.setForeground( color.getColor() );
				mMessageType.setForeground( color.getColor() );
				mMessage.setForeground( color.getColor() );
			}
		}
    }
}
