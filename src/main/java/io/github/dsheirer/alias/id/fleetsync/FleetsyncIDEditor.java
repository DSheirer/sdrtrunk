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
package io.github.dsheirer.alias.id.fleetsync;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.gui.editor.DocumentListenerEditor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Deprecated //Use Talkgroup instead
public class FleetsyncIDEditor extends DocumentListenerEditor<AliasID>
{
	private final static Logger mLog = LoggerFactory.getLogger( FleetsyncIDEditor.class );

	private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Fleetsync Identifier</h3>"
    		+ "<b>Fleetsync:</b> ggg-uuuu where g=Group and u=Unit (e.g. <u>001-0001</u>)<br>"
    		+ "<b>Wildcard:</b> use an asterisk (*) for each digit (e.g. <u>001-****</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public FleetsyncIDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "Fleetsync ID:" ) );

		MaskFormatter formatter = null;

		try
		{
			//Mask: 3 digits - 4 digits
			formatter = new MaskFormatter( "###-####" );
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
				JOptionPane.showMessageDialog( FleetsyncIDEditor.this, 
					HELP_TEXT, "Help", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( help, "align left" );
	}
	
	public FleetsyncID getFleetsyncID()
	{
		if( getItem() instanceof FleetsyncID )
		{
			return (FleetsyncID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		FleetsyncID fleetsync = getFleetsyncID();
		
		if( fleetsync != null )
		{
			mTextField.setText( fleetsync.getIdent() );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		FleetsyncID fleetsync = getFleetsyncID();
		
		if( fleetsync != null )
		{
			fleetsync.setIdent( mTextField.getText() );
		}
		
		setModified( false );
	}
}
