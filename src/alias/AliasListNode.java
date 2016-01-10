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

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import playlist.PlaylistManager;
import settings.SettingsManager;
import controller.ConfigurableNode;

public class AliasListNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    
    public AliasListNode( PlaylistManager playlistManager, 
    					  SettingsManager settingsManager, 
    					  AliasList list )
	{
    	super( playlistManager, list );

    	mPlaylistManager = playlistManager;
    	mSettingsManager = settingsManager;
	}
    
    @Override
    public JPanel getEditor()
    {
        return new ListEditor( this );
    }
    
    public AliasList getList()
    {
    	return (AliasList)getUserObject();
    }
    
    public void init()
    {
    	for( Group group: getList().getGroup() )
    	{
    		GroupNode node = new GroupNode( mPlaylistManager, mSettingsManager, group );

    		getModel().addNode( node, AliasListNode.this, getChildCount() );
    		
    		node.init();
    	}
    	
    	sort();
    }

    public String toString()
    {
    	return getList().getName() + " [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();

		JMenuItem addItem = new JMenuItem( "Add Group" );
		addItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Group group = new Group();
				
				getList().addGroup( group );
				
				GroupNode node = new GroupNode( mPlaylistManager, mSettingsManager, group );
				
				getModel().addNode( node, 
									AliasListNode.this, 
									AliasListNode.this.getChildCount() );
				
				sort();
				
				getModel().showNode( node );
            }
		} );
		
		retVal.add( addItem );
		
		retVal.add(  new JSeparator() );
		
		JMenuItem deleteItem = new JMenuItem( "Delete" );
		deleteItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				int n = JOptionPane.showConfirmDialog( getModel().getTree(),
				    "Are you sure you want to permanently delete this node?" );				
				
				if( n == JOptionPane.YES_OPTION )
				{
					AliasDirectoryNode parent = (AliasDirectoryNode)getParent();
					
					parent.getAliasDirectory().removeAliasList( getList() );

					save();

					getModel().removeNodeFromParent( AliasListNode.this );
				}
            }
		} );
		
		retVal.add( deleteItem );
		
		return retVal;
	}
}
