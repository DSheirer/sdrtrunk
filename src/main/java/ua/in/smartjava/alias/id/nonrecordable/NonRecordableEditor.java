/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package ua.in.smartjava.alias.id.nonrecordable;

import ua.in.smartjava.gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.alias.id.AliasID;

public class NonRecordableEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Audio Non-Recordable</h3>"
    		+ "This identifies an ua.in.smartjava.alias as non-recordable.<br><br>"
    		+ "The ua.in.smartjava.alias can still be monitored but associated ua.in.smartjava.audio will<br>"
    		+ "not be recorded.<br><br>"
    		+ "The default behavior when ua.in.smartjava.audio recording is enabled for a<br>"
    		+ "ua.in.smartjava.channel is to ua.in.smartjava.record all aliases except any ua.in.smartjava.alias with a<br>"
    		+ "non-recordable identifier";

	public NonRecordableEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 1", "[]", "[][]" ) );

		add( new JLabel( "Non-Recordable" ), "wrap" );
		
		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( NonRecordableEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	@Override
	public void save()
	{
	}
}
