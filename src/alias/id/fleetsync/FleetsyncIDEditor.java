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
package alias.id.fleetsync;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.Channel;
import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;
import alias.id.esn.ESNEditor;

public class FleetsyncIDEditor extends ComponentEditor<AliasID>
{
	private final static Logger mLog = LoggerFactory.getLogger( FleetsyncIDEditor.class );

	private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "<html>"
    		+ "<h3>Fleetsync Identifier Example</h3>"
    		+ "<b>Fleetsync:</b> ggg-uuuu where g=Group and u=Unit (e.g. <u>001-0001</u>)<br>"
    		+ "<b>Wildcard:</b> use an asterisk (*) for each digit (e.g. <u>001-****</u>)"
    		+ "</html>";

    private JTextField mTextField;

	public FleetsyncIDEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
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
		
		JLabel example = new JLabel( "Example ..." );
		example.setForeground( Color.BLUE.brighter() );
		example.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		example.addMouseListener( new MouseAdapter() 
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				JOptionPane.showMessageDialog( FleetsyncIDEditor.this, 
					HELP_TEXT, "Example", JOptionPane.INFORMATION_MESSAGE );
			}
		} );
		add( example );
	}
	
	public FleetsyncID getFleetsyncID()
	{
		if( getComponent() instanceof FleetsyncID )
		{
			return (FleetsyncID)getComponent();
		}
		
		return null;
	}

	@Override
	public void setComponent( AliasID aliasID )
	{
		mComponent = aliasID;
		
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
