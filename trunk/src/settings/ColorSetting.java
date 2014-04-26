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
package settings;

import java.awt.Color;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

public class ColorSetting extends Setting
{
	private static final int NO_TRANSLUCENCY = 255;
	private static final int SPECTRUM_TRANSLUCENCY = 128;
	private static final int CONFIG_TRANSLUCENCY = 60;

	private String mRGB;
	private int mAlpha;
	private ColorSettingName mColorSettingName = ColorSettingName.UNKNOWN;
	
	public ColorSetting()
	{
		setColor( mColorSettingName.getDefaultColor() );
	}
	
	public ColorSetting( ColorSettingName name )
	{
		setColor( name.getDefaultColor() );
		setColorSettingName( name );
	}

	@XmlAttribute
	public ColorSettingName getColorSettingName()
	{
		return mColorSettingName;
	}
	
	public void setColorSettingName( ColorSettingName name )
	{
		mColorSettingName = name;
	}
	
	@XmlAttribute
	public String getRgb()
	{
		return mRGB;
	}
	
	public void setRgb( String value )
	{
		mRGB = value;
	}
	
	@XmlAttribute
	public int getAlpha()
	{
		return mAlpha;
	}
	
	public void setAlpha( int value )
	{
		mAlpha = value;
	}

	@XmlTransient
	public Color getColor()
	{
		Color temp = Color.decode( mRGB );
		
		return new Color( temp.getRed(), 
						  temp.getGreen(), 
						  temp.getBlue(), 
						  mAlpha );
	}
	
	public void setColor( Color color )
	{
		mRGB = Integer.toHexString( color.getRGB() );
		mRGB = "#" + mRGB.substring( 2, mRGB.length() );
		mAlpha = color.getAlpha();
	}
	
	public static Color getTranslucent( Color color, int translucency )
	{
		return new Color( color.getRed(),
						  color.getGreen(),
						  color.getBlue(),
						  translucency );
	}
	
	public enum ColorSettingName
	{
		CHANNEL_CONFIG( getTranslucent( Color.LIGHT_GRAY, CONFIG_TRANSLUCENCY ), 
				"Channel", "Channel Color" ),
		CHANNEL_CONFIG_PROCESSING( getTranslucent( Color.GREEN, CONFIG_TRANSLUCENCY ),
				"Channel Processing", "Processing Channel Color" ),
		CHANNEL_CONFIG_SELECTED( getTranslucent( Color.BLUE, CONFIG_TRANSLUCENCY ),
				"Channel Selected", "Selected Channel Color" ),

		CHANNEL_STATE_BACKGROUND( Color.BLACK, "Background", 
				"Channel State Background" ),
		CHANNEL_STATE_GRADIENT_TOP_CALL( Color.BLACK, 
				"Call Gradient Top", "Channel Call State Gradient Top" ),
		CHANNEL_STATE_GRADIENT_MIDDLE_CALL( Color.BLUE, 
				"Call Gradient Middle", "Channel Call State Gradient Middle" ),
		CHANNEL_STATE_GRADIENT_TOP_CONTROL( Color.BLACK, 
				"Control Gradient Top", "Channel Control State Gradient Top" ),
		CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL( new Color( 0xC64F00 ), 
				"Control Gradient Middle", "Channel Control State Gradient Middle" ),
		CHANNEL_STATE_GRADIENT_TOP_DATA( Color.BLACK, 
				"Data Gradient Top", "Channel Data State Gradient Top" ),
		CHANNEL_STATE_GRADIENT_MIDDLE_DATA( Color.BLUE, 
				"Data Gradient Middle", "Channel Data State Gradient Middle" ),
		CHANNEL_STATE_GRADIENT_TOP_FADE( Color.BLACK, 
				"Fade Gradient Top", "Channel Fade State Gradient Top" ),
		CHANNEL_STATE_GRADIENT_MIDDLE_FADE( Color.DARK_GRAY, 
				"Fade Gradient Middle", "Channel Fade State Gradient Middle" ),
		CHANNEL_STATE_GRADIENT_TOP_IDLE( Color.BLACK, 
				"Idle Gradient Top", "Channel Idle State Gradient Top" ),
		CHANNEL_STATE_GRADIENT_MIDDLE_IDLE( Color.DARK_GRAY, 
				"Idle Gradient Middle", "Channel Idle State Gradient Middle" ),
		CHANNEL_STATE_GRADIENT_TOP_NO_TUNER( Color.RED, 
				"No Tuner Gradient Top", "Channel No Tuner State Gradient Top" ),
		CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER( new Color( 0x990000 ), 
				"No Tuner Gradient Middle", "Channel No Tuner State Gradient Middle" ),
		CHANNEL_STATE_LABEL_DETAILS( Color.LIGHT_GRAY, 
				"Details", "Details Label Color" ),
		CHANNEL_STATE_LABEL_DECODER( Color.GREEN,
				"Decoder", "Decoder Label Color" ),
		CHANNEL_STATE_LABEL_AUX_DECODER( Color.YELLOW, 
				"Aux Decoder", "Aux Decoder Label Color" ),
		CHANNEL_STATE_SELECTED_CHANNEL( Color.YELLOW, 
				"Selected Channel Indicator", "Selected Channel Indicator Color" ),

		SPECTRUM_BACKGROUND( Color.BLACK, 
				"Background", "Spectrum Background Color" ),
		SPECTRUM_CURSOR( Color.ORANGE, 
				"Cursor", "Spectrum Cursor Color" ),
		SPECTRUM_GRADIENT_BOTTOM( getTranslucent( Color.GREEN, SPECTRUM_TRANSLUCENCY ),
				"Gradient Bottom", "Spectrum Gradient Bottom Color" ),
		SPECTRUM_GRADIENT_TOP( getTranslucent( Color.WHITE, SPECTRUM_TRANSLUCENCY ),
				"Gradient Top", "Spectrum Gradient Top Color" ),
		SPECTRUM_LINE( getTranslucent( Color.LIGHT_GRAY, SPECTRUM_TRANSLUCENCY ), 
				"Line", "Spectrum Lines and Text Color" ),
		
		UNKNOWN( Color.RED, "Unknown", "Unknown Setting Color" );

		private Color mDefaultColor;
		private String mLabel;
		private String mDialogTitle;
		
		private ColorSettingName( Color defaultColor, 
								  String label, 
								  String dialogTitle )
		{
			mDefaultColor = defaultColor;
			mLabel = label;
			mDialogTitle = dialogTitle;
		}
		
		public Color getDefaultColor()
		{
			return mDefaultColor;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String getDialogTitle()
		{
			return mDialogTitle;
		}
	}
}
