/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import module.decode.lj1200.LJ1200Message;
import module.decode.lj1200.LJ1200Message.Function;
import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class LoJackIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private LoJackIDNode mLoJackIDNode;

    private JComboBox<LJ1200Message.Function> mFunctionCombo;
    private JTextField mTextIdent;

    private String mHelpText = "Select a LoJack message function code and enter"
    		+ " a five character reply code/identifier in the ID field.\n\n"
            + "Wildcard: use an asterisk (*) to wildcard any character in the "
            + "five character reply code (e.g. AB*CD or ***12 or *****)\n\n"
            + "Reply codes are five characters where the middle character"
            + " identifies the entity (tower/site or transponder) as follows:\n"
            + "X,Y\t\tTower\n"
            + "0-9,A,C-H,J-N,P-W\tTransponder\n"
            + "B,I,O,Z\t\tNot Used";

	public LoJackIDEditor( LoJackIDNode lojackNode )
	{
		mLoJackIDNode = lojackNode;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "LoJack Function and ID" ), "span,align center" );

		LoJackFunctionAndID lojack = mLoJackIDNode.getLoJackID();

		add( new JLabel( "Function:" ) );
		mFunctionCombo = new JComboBox<LJ1200Message.Function>( Function.values() );
		mFunctionCombo.setSelectedItem( lojack.getFunction() );
		add( mFunctionCombo, "grow, wrap" );
		
		add( new JLabel( "ID:" ) );
		mTextIdent = new JTextField( lojack.getID() );

		add( mTextIdent, "grow, wrap" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( LoJackIDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( LoJackIDEditor.this );
		add( btnReset, "growx,push" );
		
		JTextArea helpText = new JTextArea( mHelpText );
		helpText.setLineWrap( true );
		helpText.setBackground( getBackground() );
		add( helpText, "span,grow,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		final LoJackFunctionAndID lojack = mLoJackIDNode.getLoJackID();

		if( command.contentEquals( "Save" ) )
		{
			String id = mTextIdent.getText();
			
			if( id != null )
			{
				
				lojack.setID( id );
				lojack.setFunction( (Function)mFunctionCombo.getSelectedItem() );

				((ConfigurableNode)mLoJackIDNode.getParent()).sort();
				
				mLoJackIDNode.save();
				
				mLoJackIDNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( LoJackIDEditor.this, "Please enter an ident" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mFunctionCombo.setSelectedItem( lojack.getFunction() );
			mTextIdent.setText( lojack.getID() );
		}
		
		mLoJackIDNode.refresh();
    }
}
