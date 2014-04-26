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
package controller.activity;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableCellRenderer;

import settings.SettingsManager;
import alias.Alias;

/**
 * Custom renderer for JTable cells that contain Alias objects.  Renders the
 * alias name, and if there is an icon name, attempts to get that icon from
 * the settings manager and render it.
 */
public class CallEventAliasCellRenderer extends DefaultTableCellRenderer
{
	private SettingsManager mSettingsManager;
	
	public CallEventAliasCellRenderer( SettingsManager settingsManager )
	{
		super();
		mSettingsManager = settingsManager;
	}
	
	public void setValue( Object obj )
	{
		if( obj != null && obj instanceof Alias )
		{
			Alias alias = (Alias)obj;
			
			setText( alias.getName() );
			
			ImageIcon icon = getIcon( alias, 12 );
			
			if( icon != null )
			{
				setIcon( icon );
			}
		}
		else
		{
			setText( " " );
			setIcon( null );
		}
	}
	
	private ImageIcon getIcon( Alias alias, int height )
	{
		if( mSettingsManager != null && alias != null )
		{
			return mSettingsManager.getImageIcon( alias.getIconName(), height );
		}
		
		return null;
	}
}
