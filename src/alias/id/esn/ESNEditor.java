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
package alias.id.esn;

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

public class ESNEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = 
    		"<html><h3>Electronic Serial Number (ESN)</h3>"
    		+ "<b>ESN:</b> hexadecimal 0-9, A-F (e.g. <u>ABCD1234</u> )<br>"
    		+ "<b>Wildcard:</b> use an asterisk (*) to wildcard individual<br>"
    		+ "digits (e.g. <u>ABCD123*</u> or <u>AB**1**4</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public ESNEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "ESN:" ) );
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
				JOptionPane.showMessageDialog( ESNEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public Esn getEsn()
	{
		if( getItem() instanceof Esn )
		{
			return (Esn)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		Esn esn = getEsn();
		
		if( esn != null )
		{
			mTextField.setText( esn.getEsn() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		Esn esn = getEsn();
		
		if( esn != null )
		{
			esn.setEsn( mTextField.getText() );
		}
		
		setModified( false );
	}
}
