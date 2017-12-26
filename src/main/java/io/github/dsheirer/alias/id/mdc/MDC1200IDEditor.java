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
package io.github.dsheirer.alias.id.mdc;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MDC1200IDEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>MDC-1200 Identifier</h3>"
    		+ "<b>MDC-1200:</b> four digit [0-9] value (e.g. <u>1234</u>)<br>"
    		+ "<b>Wildcard:</b> use an asterisk (*) to wildcard individual<br>" 
    		+ "digits (e.g. <u>123*</u> or <u>**34</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public MDC1200IDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "MDC-1200 ID:" ) );

		MaskFormatter formatter = null;

		try
		{
			//Mask: 4 digits
			formatter = new MaskFormatter( "####" );
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
				JOptionPane.showMessageDialog( MDC1200IDEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public MDC1200ID getMDC1200ID()
	{
		if( getItem() instanceof MDC1200ID )
		{
			return (MDC1200ID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		MDC1200ID mdc = getMDC1200ID();
		
		if( mdc != null )
		{
			mTextField.setText( mdc.getIdent() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		MDC1200ID mdc = getMDC1200ID();
		
		if( mdc != null )
		{
			mdc.setIdent( mTextField.getText() );
		}
		
		setModified( false );
	}
}
