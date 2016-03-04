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
package alias.id.status;

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

public class StatusIDEditor extends DocumentListenerEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Status Example</h3>"
    		+ "Status numbers are used in some protocols like Fleetsync.<br><br>"
    		+ "The status number is assigned a meaning in the radio.  You<br>"
    		+ "can assign a 3 digit status code (use leading zeros) to an<br>"
    		+ "alias where the alias contains the status meaning.<br><br>"
    		+ "<b>Status:</b> <u>001</u> engine start"
    		+ "</html>";

    private JTextField mTextField;

	public StatusIDEditor( AliasID aliasID )
	{
		initGUI();
		
		setItem( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][]" ) );

		add( new JLabel( "Status:" ) );

		MaskFormatter formatter = null;

		try
		{
			//Mask: 3 digits
			formatter = new MaskFormatter( "###" );
		}
		catch( Exception e )
		{
			//Do nothing, the mask was invalid
		}
		
		mTextField = new JFormattedTextField( formatter );
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
				JOptionPane.showMessageDialog( StatusIDEditor.this, 
					HELP_TEXT, "Example", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( example );
	}
	
	public StatusID getStatusID()
	{
		if( getItem() instanceof StatusID )
		{
			return (StatusID)getItem();
		}
		
		return null;
	}

	@Override
	public void setItem( AliasID aliasID )
	{
		super.setItem( aliasID );
		
		StatusID statusID = getStatusID();
		
		if( statusID != null )
		{
			mTextField.setText( String.format( "%03d", statusID.getStatus() ) );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		StatusID statusID = getStatusID();
		
		if( statusID != null )
		{
			int status = 0;
			
			try
			{
				status = Integer.valueOf( mTextField.getText() );
			}
			catch( Exception e )
			{
				//Do nothing, we couldn't parse the value
			}
			
			statusID.setStatus( status );
		}
		
		setModified( false );
	}
}
