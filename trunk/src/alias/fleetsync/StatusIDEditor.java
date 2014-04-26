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
import javax.swing.JTextField;

import log.Log;
import net.miginfocom.swing.MigLayout;
import controller.ConfigurableNode;

public class StatusIDEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private StatusIDNode mStatusIDNode;
    
    private JLabel mLabelName;
    private JTextField mTextStatusID;

	public StatusIDEditor( StatusIDNode statusIDNode )
	{
		mStatusIDNode = statusIDNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout() );
		
		setBorder( BorderFactory.createTitledBorder( "Status Code" ) );

		mLabelName = new JLabel( "Status Code:" );
		add( mLabelName, "align right" );
		
		mTextStatusID = new JTextField( 
				String.valueOf( mStatusIDNode.getStatusID().getStatus() ) );
		add( mTextStatusID, "grow, wrap" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( StatusIDEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( StatusIDEditor.this );
		add( btnReset, "wrap" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String statusIDString = mTextStatusID.getText();
			
			if( statusIDString != null )
			{
				try
				{
					int id = Integer.parseInt( statusIDString );
					
					mStatusIDNode.getStatusID().setStatus( id );
					
					((ConfigurableNode)mStatusIDNode.getParent()).sort();

					mStatusIDNode.save();
					
					mStatusIDNode.show();
				}
				catch( Exception ex )
				{
					Log.error( "StatusIDEditor - error parsing int status " +
							"id from [" + statusIDString + "]" );
				}
			}
			else
			{
				JOptionPane.showMessageDialog( StatusIDEditor.this, 
						"Please enter a status ID code" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextStatusID.setText( 
					String.valueOf( mStatusIDNode.getStatusID().getStatus() ) );
		}
		
		mStatusIDNode.refresh();
    }
}
