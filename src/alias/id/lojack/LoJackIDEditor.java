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
package alias.id.lojack;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import module.decode.lj1200.LJ1200Message;
import module.decode.lj1200.LJ1200Message.Function;
import net.miginfocom.swing.MigLayout;
import alias.AliasID;
import alias.ComponentEditor;

public class LoJackIDEditor extends ComponentEditor<AliasID>
{
    private static final long serialVersionUID = 1L;
    
    private JTextField mTextField;
    
    private JComboBox<LJ1200Message.Function> mFunctionCombo;

    private static final String HELP_TEXT = "LoJack function code and five character ID."
            + " The middle character in a reply ID code identifies the entity:"
            + " Tower[X,Y] Transponder[0-9,A,C-H,J-N,P-W] Not Used[B,I,O,Z]."
    		+ " Use an asterisk (*) to wildcard ID characters (e.g. AB*CD or ***12 or *****)";

    public LoJackIDEditor( AliasID aliasID )
	{
		super( aliasID );
		
		initGUI();
		
		setComponent( aliasID );
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "Function:" ) );
		mFunctionCombo = new JComboBox<LJ1200Message.Function>( Function.values() );
		mFunctionCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );
		add( mFunctionCombo, "growx, push" );
		
		add( new JLabel( "ID:" ) );
		
		MaskFormatter formatter = null;

		try
		{
			//Mask: any character or number, 5 places
			formatter = new MaskFormatter( "AAAAA" );
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
	
	public LoJackFunctionAndID getLoJackID()
	{
		if( getComponent() instanceof LoJackFunctionAndID )
		{
			return (LoJackFunctionAndID)getComponent();
		}
		
		return null;
	}

	@Override
	public void setComponent( AliasID aliasID )
	{
		mComponent = aliasID;
		
		LoJackFunctionAndID lojack = getLoJackID();
		
		if( lojack != null )
		{
			mFunctionCombo.setSelectedItem( lojack.getFunction() );
			mTextField.setText( lojack.getID() );
		}
		else
		{
			mFunctionCombo.setSelectedItem( null );
			mTextField.setText( null );
		}
		
		setModified( false );
		
		repaint();
	}

	@Override
	public void save()
	{
		LoJackFunctionAndID lojack = getLoJackID();
		
		if( lojack != null )
		{
			lojack.setID( mTextField.getText() );
			lojack.setFunction( (Function)mFunctionCombo.getSelectedItem() );
		}
		
		setModified( false );
	}
}
