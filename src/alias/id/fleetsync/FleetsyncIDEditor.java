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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.Channel;
import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;

public class FleetsyncIDEditor extends ComponentEditor<AliasID>
{
	private final static Logger mLog = LoggerFactory.getLogger( FleetsyncIDEditor.class );

	private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "A Fleetsync identifier is a"
    		+ " composite decimal [0-9] value formatted as ggg-uuuu where"
    		+ " g=Group and u=Unit.";

    private JTextField mTextField;

	public FleetsyncIDEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][grow]" ) );

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

		add( mTextField, "growx,push" );
		
		JTextArea helpText = new JTextArea( HELP_TEXT );
		helpText.setLineWrap( true );
		helpText.setBackground( getBackground() );
		add( helpText, "span,grow,push" );
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
