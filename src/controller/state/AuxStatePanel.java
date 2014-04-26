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
package controller.state;

import java.awt.Color;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import alias.Alias;
import controller.state.ChannelState.ChangedAttribute;

public abstract class AuxStatePanel extends JPanel 
						implements Listener<ChangedAttribute>, 
								   SettingChangeListener
{
    private static final long serialVersionUID = 1L;
    
    protected Font mFontDetails = new Font( Font.MONOSPACED, Font.PLAIN, 10 );
    protected Font mFontAuxDecoder = new Font( Font.MONOSPACED, Font.PLAIN, 10 );

    protected Color mColorLabelDetails;
    protected Color mColorLabelAuxDecoder;

    protected SettingsManager mSettingsManager;
    protected AuxChannelState mAuxChannelState;
    
    public AuxStatePanel( SettingsManager settingsManager,
    					  AuxChannelState auxChannelState )
    {
    	mSettingsManager = settingsManager;
    	mAuxChannelState = auxChannelState;

    	/* Register to receive event updates */
    	mAuxChannelState.addListener( this );
    	
    	/* Register to receive settings updates */
    	mSettingsManager.addListener( this );
    	
    	getColors();

    	setLayout( new MigLayout( "insets 1 0 0 0", "[grow,fill]", "[]0[]0[]") );
    }
    
    public void dispose()
    {
    	/* Deregister from changed settings events */
    	mSettingsManager.removeListener( this );
    	mSettingsManager = null;
    	
    	mAuxChannelState = null;
    }
    
	protected void addLabel( JLabel label, boolean wrap )
	{
		label.setFont( mFontAuxDecoder );
		label.setForeground( mColorLabelAuxDecoder );
		
		if( wrap )
		{
			add( label, "wrap" );
		}
		else
		{
			add( label );
		}
	}

	protected void updateAliasLabel( JLabel label, Alias alias )
	{
		if( alias != null )
		{
			label.setText( alias.getName() );

			String iconName = alias.getIconName();

			ImageIcon icon = mSettingsManager.getImageIcon( iconName, 12 );
			
			label.setIcon( icon );
		}
		else
		{
			label.setText( null );
			label.setIcon( null );
		}
	}
	
    private void getColors()
    {
        mColorLabelDetails = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ).getColor();
    	mColorLabelAuxDecoder = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER ).getColor();
    }
    
	@Override
    public void settingChanged( Setting setting )
    {
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;

			switch( colorSetting.getColorSettingName() )
			{
				case CHANNEL_STATE_LABEL_AUX_DECODER:
					mColorLabelAuxDecoder = colorSetting.getColor();
					repaint();
					break;
				case CHANNEL_STATE_LABEL_DETAILS:
					mColorLabelDetails = colorSetting.getColor();
					repaint();
					break;
			}
		}
    }

	@Override
    public void settingDeleted( Setting setting ) {}
}
