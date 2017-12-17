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
package io.github.dsheirer.alias.id.uniqueID;

import io.github.dsheirer.gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;
import io.github.dsheirer.alias.id.AliasID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UniqueIDEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>LTR-Net Unique ID (UID)</h3>"
    		+ "<b>UID:</b> identifier assigned to each radio in<br>"
    		+ "the range <u>1 - 2097152</u>"
    		+ "</html>";

    private JTextField mTextField;

	public UniqueIDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "Unique ID:" ) );
		mTextField = new JTextField();
		mTextField.getDocument().addDocumentListener( this );
		mTextField.setToolTipText( HELP_TEXT );
		add( mTextField, "growx,push" );

		JLabel help = new JLabel( "Help ..." );
		help.setForeground( Color.BLUE.brighter() );
		help.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		help.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( UniqueIDEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public UniqueID getUniqueID()
	{
		if( getItem() instanceof UniqueID )
		{
			return (UniqueID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		UniqueID uid = getUniqueID();
		
		if( uid != null )
		{
			mTextField.setText( String.valueOf( uid.getUid() ) );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		UniqueID uid = getUniqueID();
		
		if( uid != null )
		{
			int id = 0;
			
			try
			{
				id = Integer.parseInt( mTextField.getText() );
			}
			catch( Exception e )
			{
				//Do nothing, we couldn't parse the value
			}
			
			uid.setUid( id );
		}
		
		setModified( false );
	}
}
