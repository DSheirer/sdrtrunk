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

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;

public class UniqueIDEditor extends ComponentEditor<AliasID>
{
    private static final long serialVersionUID = 1L;

    private static final String HELP_TEXT = "LTR-Net Unique ID (UID) is a system"
		+ " identifier assigned to each radio in the range 1 - 2097152";

    private JTextField mTextField;

	public UniqueIDEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][grow]" ) );

		add( new JLabel( "Unique ID:" ) );
		mTextField = new JTextField();
		mTextField.getDocument().addDocumentListener( this );
		add( mTextField, "growx,push" );

		JTextArea helpText = new JTextArea( HELP_TEXT );
		helpText.setLineWrap( true );
		helpText.setBackground( getBackground() );
		add( helpText, "span,grow,push" );
	}
	
	public UniqueID getUniqueID()
	{
		if( getComponent() instanceof UniqueID )
		{
			return (UniqueID)getComponent();
		}
		
		return null;
	}

	@Override
	public void setComponent( AliasID aliasID )
	{
		mComponent = aliasID;
		
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
