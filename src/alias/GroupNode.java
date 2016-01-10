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

public class GroupNode extends ConfigurableNode 
{
    private static final long serialVersionUID = 1L;

    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    
    public GroupNode( PlaylistManager playlistManager, 
    				  SettingsManager settingsManager, 
    				  Group group )
	{
    	super( playlistManager, group );
    	
    	mSettingsManager = settingsManager;
	}
    
    @Override
    public JPanel getEditor()
    {
        return new GroupEditor( this );
    }
    
    public Group getGroup()
    {
    	return (Group)getUserObject();
    }
    
    public void init()
    {
    	for( Alias alias: getGroup().getAlias() )
    	{
    		AliasNode node = new AliasNode( mPlaylistManager, mSettingsManager, alias );
    		
    		getModel().addNode( node, GroupNode.this, getChildCount() );
    		
    		node.init();
    	}
    	
    	sort();
    }

    public String toString()
    {
    	return getGroup().getName() + " [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();

		JMenuItem addItem = new JMenuItem( "Add Alias" );
		addItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				Alias alias = new Alias();
				
				getGroup().addAlias( alias );
				
				AliasNode node = new AliasNode( mPlaylistManager, mSettingsManager, alias );
				
				getModel().addNode( node, 
									GroupNode.this, 
									GroupNode.this.getChildCount() );
				
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
					AliasListNode parent = (AliasListNode)getParent();
					
					parent.getList().removeGroup( getGroup() );

					save();

					getModel().removeNodeFromParent( GroupNode.this );
				}
            }
		} );
		
		retVal.add( deleteItem );
		
		return retVal;
	}
}
