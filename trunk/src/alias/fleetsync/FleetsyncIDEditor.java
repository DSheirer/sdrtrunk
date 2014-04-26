/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
package alias.fleetsync;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class FleetsyncIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private FleetsyncIDNode mFleetsyncIDNode;
    
    private JLabel mLabelName;
    private JTextField mTextIdent;

    private String mHelpText = "Enter a formatted fleetsync identifier.\n\n"
    		+ "Format: PPP-IIII (P=Prefix, I=Identifier)\n\n"
            + "Wildcard: use one or more asterisks (*) for any talkgroup "
            + "digits.\n\n";

	public FleetsyncIDEditor( FleetsyncIDNode fsNode )
	{
		mFleetsyncIDNode = fsNode;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "Fleetsync ID" ), "span,align center" );

		add( new JLabel( "Ident:" ) );
		mTextIdent = new JTextField( mFleetsyncIDNode.getFleetsyncID().getIdent() );

		add( mTextIdent, "grow, wrap" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( FleetsyncIDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( FleetsyncIDEditor.this );
		add( btnReset, "growx,push" );
		
		JTextArea helpText = new JTextArea( mHelpText );
		helpText.setLineWrap( true );
		add( helpText, "span,grow,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String ident = mTextIdent.getText();
			
			if( ident != null )
			{
				mFleetsyncIDNode.getFleetsyncID().setIdent( ident );

				((ConfigurableNode)mFleetsyncIDNode.getParent()).sort();
				
				mFleetsyncIDNode.save();
				
				mFleetsyncIDNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( FleetsyncIDEditor.this, "Please enter an ident" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextIdent.setText( mFleetsyncIDNode.getFleetsyncID().getIdent() );
		}
		
		mFleetsyncIDNode.refresh();
    }
}
