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
package alias.id.uniqueID;

import gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import alias.id.AliasID;

public class UniqueIDEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>LTR-Net Unique ID (UID) Example</h3>"
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

		JLabel example = new JLabel( "Example ..." );
		example.setForeground( Color.BLUE.brighter() );
		example.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		example.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( UniqueIDEditor.this, 
					HELP_TEXT, "Example", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( example );
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
