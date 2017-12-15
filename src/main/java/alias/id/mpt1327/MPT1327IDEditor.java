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
package alias.id.mpt1327;

import gui.editor.DocumentListenerEditor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import net.miginfocom.swing.MigLayout;
import alias.id.AliasID;

public class MPT1327IDEditor extends DocumentListenerEditor<AliasID>
{
	private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>MPT-1327 Identifier</h3>"
    		+ "<b>MPT-1327:</b> decimal (0-9) format ppp-iiii where<br>"
    		+ "p=Prefix and i=Ident (e.g. <u>123-0001</u>)<br>"
    		+ "<b>Wildcard:</b> use an asterisk (*) to wildcard individual<br>"
    		+ "digits (e.g. <u>ABCD123*</u> or <u>AB**1**4</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public MPT1327IDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "MPT-1327 ID:" ) );

		MaskFormatter formatter = null;

		try
		{
			//Mask: 3 digits - 4 digits
			formatter = new MaskFormatter( "***-****" );
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
				JOptionPane.showMessageDialog( MPT1327IDEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public MPT1327ID getMPT1327ID()
	{
		if( getItem() instanceof MPT1327ID )
		{
			return (MPT1327ID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		MPT1327ID mpt = getMPT1327ID();
		
		if( mpt != null )
		{
			mTextField.setText( mpt.getIdent() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		MPT1327ID mpt = getMPT1327ID();
		
		if( mpt != null )
		{
			mpt.setIdent( mTextField.getText() );
		}
		
		setModified( false );
	}
}
