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
package io.github.dsheirer.settings;

import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.util.ColorIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * JMenuItem for selecting a color and automatically setting (saving) the
 * color selection in the settings manager
 */
public class ColorSettingMenuItem extends JMenuItem
{
    private static final long serialVersionUID = 1L;

    private ColorSettingName mColorSettingName;
    private SettingsManager mSettingsManager;
    private Color mCurrentColor;
	
	public ColorSettingMenuItem( SettingsManager settingsManager,
								 ColorSettingName colorSettingName )
	{
		super( colorSettingName.getLabel() );

		mSettingsManager = settingsManager;
		mColorSettingName = colorSettingName;

		mCurrentColor = mSettingsManager
				.getColorSetting( mColorSettingName ).getColor();

		this.setIcon( new ColorIcon( mCurrentColor ) );
		
		addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Color newColor = JColorChooser.showDialog( ColorSettingMenuItem.this,
	                     mColorSettingName.getDialogTitle(),
	                     mCurrentColor );

				if( newColor != null )
				{
					mSettingsManager.setColorSetting( mColorSettingName, newColor );
				}
            }
		} );
	}
}
