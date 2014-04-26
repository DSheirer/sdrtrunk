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
package alias.mobileID;

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

public class MINEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private MINNode mMINNode;
    
    private JLabel mLabelName;
    private JTextField mTextMIN;

	public MINEditor( MINNode minNode )
	{
		mMINNode = minNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "Mobile ID Number (MIN)" ), "span,align center" );

		add( new JLabel( "MIN:" ) );
		
		mTextMIN = new JTextField( mMINNode.getMIN().getMin() );
		add( mTextMIN, "growx,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( MINEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( MINEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String esn = mTextMIN.getText();
			
			if( esn != null )
			{
				mMINNode.getMIN().setMin( esn );

				((ConfigurableNode)mMINNode.getParent()).sort();

				mMINNode.save();
				
				mMINNode.show();
			}
			else
			{
				JOptionPane.showMessageDialog( MINEditor.this, "Please enter an ESN" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextMIN.setText( mMINNode.getMIN().getMin() );
		}
		
		mMINNode.refresh();
    }
}
