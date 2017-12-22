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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * JMenuItem for selecting a color and automatically setting (saving) the
 * color selection in the settings manager
 */
public class ColorSettingResetMenuItem extends JMenuItem
{
    private static final long serialVersionUID = 1L;

    private ColorSetting.ColorSettingName mColorSettingName;
    private SettingsManager mSettingsManager;
	
	public ColorSettingResetMenuItem( SettingsManager settingsManager,
								 ColorSetting.ColorSettingName colorSettingName )
	{
		super( "Reset - " + colorSettingName.getLabel() );

		mSettingsManager = settingsManager;
		mColorSettingName = colorSettingName;

		addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				mSettingsManager.resetColorSetting( mColorSettingName );
            }
		} );
	}
}
