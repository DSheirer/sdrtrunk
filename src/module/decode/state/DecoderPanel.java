/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package module.decode.state;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;

import module.decode.Decoder;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;

public abstract class DecoderPanel extends JPanel 
		implements Listener<ChangedAttribute>, SettingChangeListener
{
	private static final long serialVersionUID = 1L;

    protected Font mFontDetails = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    protected Font mFontDecoder = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    protected Font mFontAuxDecoder = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    public static final int ICON_SIZE = 12;
    
    protected Color mColorLabelDetails;
    protected Color mColorLabelDecoder;
    protected Color mColorLabelAuxDecoder;
	
    protected SettingsManager mSettingsManager; 
    protected Decoder mDecoder;
	
	public DecoderPanel( SettingsManager settingsManager, Decoder decoder )
	{
		mSettingsManager = settingsManager;
		mSettingsManager.addListener( this );
		
		mDecoder = decoder;
		
		mDecoder.getDecoderState().setChangedAttributeListener( this );
	}
	
	public void dispose()
	{
		mSettingsManager.removeListener( this );
		mSettingsManager = null;
		
		mDecoder.getDecoderState().removeChangedAttributeListener();
		mDecoder = null;
	}
	
	public Decoder getDecoder()
	{
		return mDecoder;
	}
	
	protected void init()
	{
		getColors();
		
		setOpaque( false ); //Use the parent panel's background color
	}
	
    private void getColors()
    {
        mColorLabelDetails = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ).getColor();
    	mColorLabelDecoder = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DECODER ).getColor();
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
			case CHANNEL_STATE_LABEL_DECODER:
				mColorLabelDecoder = colorSetting.getColor();
				repaint();
				break;
			case CHANNEL_STATE_LABEL_DETAILS:
				mColorLabelDetails = colorSetting.getColor();
				repaint();
				break;
				default:
					break;
			}
		}
    }

	@Override
    public void settingDeleted( Setting setting ) {}
}
