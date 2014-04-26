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
package decode.mdc1200;

import java.awt.EventQueue;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingsManager;
import controller.state.AuxStatePanel;
import controller.state.ChannelState.ChangedAttribute;

public class MDCPanel extends AuxStatePanel
{
    private static final long serialVersionUID = 1L;
    private static final String sPROTOCOL = "MDC-1200";

    private JLabel mFromLabel = new JLabel( " " );
    private JLabel mFrom = new JLabel();
    private JLabel mFromAlias = new JLabel();
    
    private JLabel mToLabel = new JLabel( " " );
    private JLabel mTo = new JLabel();
    private JLabel mToAlias = new JLabel();

    private JLabel mProtocol = new JLabel( sPROTOCOL );
    private JLabel mMessage = new JLabel();
	private JLabel mMessageType = new JLabel();
	
	public MDCPanel( SettingsManager settingsManager, MDCChannelState state )
	{
		super( settingsManager, state );
		
		init();
	}
	
	public MDCChannelState getState()
	{
		return (MDCChannelState)mAuxChannelState;
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
						String from = getState().getFrom();
						
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
										  getState().getFromAlias() );
						break;
					case TO_TALKGROUP:
						String to = getState().getTo();
						
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
								  getState().getToAlias() );
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
