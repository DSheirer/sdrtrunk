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
package alias.mpt1327;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class MPT1327IDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private MPT1327IDNode mMPT1327IDNode;
    
    private JTextField mTextIdent;

	public MPT1327IDEditor( MPT1327IDNode fsNode )
	{
		mMPT1327IDNode = fsNode;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "MPT-1327 Radio or Group ID" ), "span,align center" );

		add( new JLabel( "ID:" ) );
		
		mTextIdent = new JTextField( mMPT1327IDNode.getMPT1327ID().getIdent() );
		add( mTextIdent, "growx,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( MPT1327IDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( MPT1327IDEditor.this );
		add( btnReset, "growx,push" );
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
				mMPT1327IDNode.getMPT1327ID().setIdent( ident );

				((ConfigurableNode)mMPT1327IDNode.getParent()).sort();
				
				mMPT1327IDNode.save();
				
				mMPT1327IDNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( MPT1327IDEditor.this, "Please enter a MPT1327 unit ID" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextIdent.setText( mMPT1327IDNode.getMPT1327ID().getIdent() );
		}
		
		mMPT1327IDNode.refresh();
    }
}
