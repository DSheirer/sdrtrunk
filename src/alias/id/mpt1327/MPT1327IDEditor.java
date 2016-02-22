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

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;

public class MPT1327IDEditor extends ComponentEditor<AliasID>
{
	private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "An MPT-1327 identifier is a"
    		+ " composite decimal [0-9] value formatted as ppp-iiii where"
    		+ " p=Prefix and i=Ident.";

    private JTextField mTextField;

	public MPT1327IDEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][grow]" ) );

		add( new JLabel( "MPT-1327 ID:" ) );

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
	
	public MPT1327ID getMPT1327ID()
	{
		if( getComponent() instanceof MPT1327ID )
		{
			return (MPT1327ID)getComponent();
		}
		
		return null;
	}

	@Override
	public void setComponent( AliasID aliasID )
	{
		mComponent = aliasID;
		
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
