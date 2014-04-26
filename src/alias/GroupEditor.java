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
package alias;

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
import controller.ConfigurableNode;

public class GroupEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private GroupNode mGroupNode;
    
    private JLabel mLabelName;
    private JTextField mTextGroup;

	public GroupEditor( GroupNode groupNode )
	{
		mGroupNode = groupNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout() );
		
		setBorder( BorderFactory.createTitledBorder( "Group" ) );

		mLabelName = new JLabel( "Name:" );
		add( mLabelName, "align right" );
		
		mTextGroup = new JTextField( mGroupNode.getGroup().getName() );
		add( mTextGroup, "grow, wrap" );
		
		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( GroupEditor.this );
		add( btnSave );

		JButton btnReset = new JButton( "Reset" );
		btnReset.addActionListener( GroupEditor.this );
		add( btnReset, "wrap" );
	}

	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();
		
		if( command.contentEquals( "Save" ) )
		{
			String group = mTextGroup.getText();
			
			if( group != null )
			{
				boolean expanded = mGroupNode.getModel().getTree()
						.isExpanded( new TreePath( mGroupNode ) );

				mGroupNode.getGroup().setName( group );

				((ConfigurableNode)mGroupNode.getParent()).sort();

				mGroupNode.save();

				mGroupNode.show();
				
				if( expanded )
				{
					mGroupNode.getModel().getTree()
						.expandPath( new TreePath( mGroupNode ) );
				}
			}
			else
			{
				JOptionPane.showMessageDialog( GroupEditor.this, "Please enter a group name" );
			}
		}
		else if( command.contentEquals( "Reset" ) )
		{
			mTextGroup.setText( mGroupNode.getGroup().getName() );
		}
		
		mGroupNode.refresh();
    }
}
