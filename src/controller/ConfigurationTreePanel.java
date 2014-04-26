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
package controller;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import net.miginfocom.swing.MigLayout;

public class ConfigurationTreePanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	protected JTree mSystemTree;
	protected ConfigurationControllerModel mTreeModel;
	
	public ConfigurationTreePanel( ConfigurationControllerModel model )
	{
    	mTreeModel = model;

    	initGUI();
	}
	
	private void initGUI()
	{
    	setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill][]") );

		mSystemTree = new JTree( mTreeModel );
		mSystemTree.getSelectionModel().setSelectionMode( 
				TreeSelectionModel.SINGLE_TREE_SELECTION );
		mSystemTree.setShowsRootHandles( true );
		mSystemTree.addMouseListener( new Handler() );
		mSystemTree.setCellRenderer( new ControllerTreeCellRenderer() );

		//Give the model a reference to the tree, so that it can force newly
		//added nodes to be displayed
		mTreeModel.setTree( mSystemTree );

		JScrollPane treeScroll = new JScrollPane( mSystemTree );
    	add( treeScroll, "cell 0 0,span,grow" );
	}
	
	public void addTreeSelectionListener( TreeSelectionListener listener )
	{
		mSystemTree.addTreeSelectionListener( listener );
	}
	
	public class Handler implements MouseListener
	{
		public Handler()
		{
		}

		@Override
        public void mouseClicked( MouseEvent event )
        {
			if( SwingUtilities.isRightMouseButton( event ) )
			{
				int row = mSystemTree.getRowForLocation( event.getX(), 
						 								 event.getY() );
				
				if( row != -1 )
				{
					mSystemTree.setSelectionRow( row );
					
					Object selectedNode = 
							mSystemTree.getLastSelectedPathComponent();
					
					if( selectedNode instanceof BaseNode )
					{
						JPopupMenu contextMenu = 
						((BaseNode)selectedNode).getContextMenu();
						
						if( contextMenu != null )
						{
							contextMenu.show( mSystemTree, 
											  event.getX(), 
											  event.getY() );
						}
					}
				}
			}
        }

		@Override
        public void mousePressed( MouseEvent e ){}
		@Override
        public void mouseReleased( MouseEvent e ){}
		@Override
        public void mouseEntered( MouseEvent e ){}
		@Override
        public void mouseExited( MouseEvent e ){}
	}
}
