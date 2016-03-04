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
package alias.id.nonrecordable;

import gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.miginfocom.swing.MigLayout;
import alias.id.AliasID;

public class NonRecordableEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Audio Non-Recordable Example</h3>"
    		+ "This identifies an alias as non-recordable.<br><br>"
    		+ "The alias can still be monitored but associated audio will not be recorded<br>"
    		+ "when recording is enabled for the decoding channel.";

	public NonRecordableEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 1", "[]", "[][]" ) );

		add( new JLabel( "Non-Recordable" ), "wrap" );
		
		JLabel example = new JLabel( "Example ..." );
		example.setForeground( Color.BLUE.brighter() );
		example.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		example.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( NonRecordableEditor.this, 
					HELP_TEXT, "Example", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( example );
	}
	
	@Override
	public void save()
	{
	}
}
