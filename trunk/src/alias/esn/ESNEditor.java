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
package alias.esn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class ESNEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private ESNNode mESNNode;
    private JTextField mTextESN;

	public ESNEditor( ESNNode esnNode )
	{
		mESNNode = esnNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "Electronic Serial Number ID" ), "span,align center" );

		add( new JLabel( "ESN:" ) );
		mTextESN = new JTextField( mESNNode.getESN().getEsn() );
		add( mTextESN, "growx,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ESNEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( ESNEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String esn = mTextESN.getText();
			
			if( esn != null )
			{
				mESNNode.getESN().setEsn( esn );

				((ConfigurableNode)mESNNode.getParent()).sort();

				mESNNode.save();
				
				mESNNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( ESNEditor.this, "Please enter an ESN" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextESN.setText( mESNNode.getESN().getEsn() );
		}
		
		mESNNode.refresh();
    }
}
