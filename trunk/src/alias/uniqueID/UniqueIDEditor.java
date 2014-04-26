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
package alias.uniqueID;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import log.Log;
import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class UniqueIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private UniqueIDNode mUniqueIDNode;
    
    private JLabel mLabelUniqueID;
    private JTextField mTextUniqueID;

	public UniqueIDEditor( UniqueIDNode uniqueIDNode )
	{
		mUniqueIDNode = uniqueIDNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][left]", "[][][][grow]" ) );

		add( new JLabel( "LTR-Net Radio Unique ID" ), "span,align center" );

		add( new JLabel( "Unique ID:" ) );
		
		mTextUniqueID = new JTextField( 
				String.valueOf( mUniqueIDNode.getUniqueID().getUid() ) );
		add( mTextUniqueID, "growx,push" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( UniqueIDEditor.this );
		add( btnSave, "growx,push" );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( UniqueIDEditor.this );
		add( btnReset, "growx,push" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String uid = mTextUniqueID.getText();
			
			if( uid != null )
			{
				try
				{
					int uidInt = Integer.parseInt( uid );
					
					mUniqueIDNode.getUniqueID().setUid( uidInt );

					((ConfigurableNode)mUniqueIDNode.getParent()).sort();

					mUniqueIDNode.save();
					
					mUniqueIDNode.show();
				}
				catch( Exception ex )
				{
					Log.error( "UniqueIDEditor - exception trying to parse " +
						"int from uid [" + uid + "]" );
				}
			}
			else
			{
				JOptionPane.showMessageDialog( UniqueIDEditor.this, 
						"Please enter a site number and unique ID" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextUniqueID.setText( 
					String.valueOf( mUniqueIDNode.getUniqueID().getUid() ) );
		}
		
		mUniqueIDNode.refresh();
    }
}
