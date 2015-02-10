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
package controller;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import controller.channel.ChannelValidationException;

public abstract class Editor extends JPanel
{
    private static final long serialVersionUID = 1L;

    public Editor()
	{
    	setLayout( new MigLayout() );
	}
    
    public abstract void save();
    
    public abstract void reset();

    /**
     * Override this method to allow the editor to inspect the argument for
     * valid configuration
     * 
     * @param editor - any editor
     * 
     * @throws ChannelValidationException if the configuration is not compatible
     * with this editor's configuration.  Supplies a validation text value for
     * display to the user 
     */
    public void validate( Editor editor ) throws ChannelValidationException
    {
    }
}
