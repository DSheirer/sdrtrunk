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
package controller.system;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.TreePath;

import net.miginfocom.swing.MigLayout;

public class SystemEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private SystemNode mSystemNode;
    
    private JLabel mLabelSystem;
    private JTextField mTextSystem;

	public SystemEditor( SystemNode system )
	{
		mSystemNode = system;
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout() );
		
		setBorder( BorderFactory.createTitledBorder( "System" ) );

		mLabelSystem = new JLabel( "Name:" );
		add( mLabelSystem, "align right" );
		
		mTextSystem = new JTextField( mSystemNode.getSystem().getName() );
		add( mTextSystem, "grow, wrap" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( SystemEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( SystemEditor.this );
		add( btnReset, "wrap" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			boolean expanded = mSystemNode.getModel().getTree()
					.isExpanded( new TreePath( mSystemNode ) );
			
			String name = mTextSystem.getText();
			
			if( name != null )
			{
				mSystemNode.getSystem().setName( name );
				mSystemNode.save();
				mSystemNode.show();
				
				if( expanded )
				{
					mSystemNode.getModel().getTree().expandPath( new TreePath( mSystemNode ) );
				}
			}
			else
			{
				JOptionPane.showMessageDialog( SystemEditor.this, "Please enter a system name" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextSystem.setText( mSystemNode.getSystem().getName() );
		}
    }
}
