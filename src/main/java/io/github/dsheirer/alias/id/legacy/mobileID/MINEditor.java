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
package io.github.dsheirer.alias.id.legacy.mobileID;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MINEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Passport Mobile ID (MIN)</h3>"
    		+ "<b>MIN:</b> six character hex value (e.g. <u>AB12CD</u>)<br>"
    		+ "<b>Wildcard:</b> use an asterisk (*) to wildcard individual<br>"
    		+ "digits (e.g. <u>AB**CD</u> or <u>AB12**</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public MINEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "MIN:" ) );

		MaskFormatter formatter = null;

		try
		{
			//Mask: 6 hex characters
			formatter = new MaskFormatter( "HHHHHH" );
		}
		catch( Exception e )
		{
			//Do nothing, the mask was invalid
		}
		
		mTextField = new JFormattedTextField( formatter );
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
				JOptionPane.showMessageDialog( MINEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public Min getMin()
	{
		if( getItem() instanceof Min )
		{
			return (Min)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		Min min = getMin();
		
		if( min != null )
		{
			mTextField.setText( min.getMin() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		Min min = getMin();
		
		if( min != null )
		{
			min.setMin( mTextField.getText() );
		}
		
		setModified( false );
	}
}
