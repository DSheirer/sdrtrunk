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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class EmptyEditor extends JPanel
{
    private static final long serialVersionUID = 1L;

	public EmptyEditor()
	{
		setLayout( new BorderLayout() );
		setPreferredSize( new Dimension( 200, (int)getPreferredSize().getHeight() ) );
		
		String text = "<html>"
			+ "<p>SDR Trunk</p>"
			+ "<br>"
			+ "<p>Use the tree structure to the left to configure SDRTrunk"
			+ " to use multiple SDR tuners and sound card sources to"
			+ " decode multiple trunked and digital radio channels.</p>"
			+ "<br>"
			+ "<p>Each tree branch can be accessed by clicking on the branch"
			+ " and a configuration editor will appear here.</p>"
			+ "<br>"
			+ "<p>Many of the branches contain context sensitive menus that "
			+ " you can access by right-clicking on the node.</p>"
			+ "<br>"
			+ "<p>To begin, right-click on the 'Systems' branch under 'Playlist'"
			+ "and create a new 'System'.  Right-click the 'System' node to create "
			+ "a new 'Site'.  Right-click on the 'Site' node to create a new Channel.</p>"
			+ "<br>"
			+ "<p>In the new channel, select a channel source and decoder "
			+ "and select event logging and recording options.  Check 'Enabled'"
			+ " and hit the 'Save' button to begin decoding.</p>"
			+ "</html>";
		
		add( new JLabel( text ), BorderLayout.NORTH );
	}
}
