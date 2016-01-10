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
import javax.swing.JPopupMenu;

import playlist.PlaylistManager;
import settings.SettingsManager;
import controller.ConfigurableNode;

public class AliasDirectoryNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;

    private PlaylistManager mPlaylistManager;
    
    private SettingsManager mSettingsManager;
    
    public AliasDirectoryNode( PlaylistManager playlistManager,
    						   SettingsManager settingsManager, 
    						   AliasDirectory directory )
	{
        super( playlistManager, directory );

        mPlaylistManager = playlistManager;
        
        mSettingsManager = settingsManager;
	}
    
    public AliasDirectory getAliasDirectory()
    {
    	return (AliasDirectory)getUserObject();
    }
    
    public void init()
    {
    	for( AliasList list: getAliasDirectory().getAliasList() )
    	{
    		AliasListNode node = new AliasListNode( mPlaylistManager, mSettingsManager, list );
    		
    		getModel().addNode( node, AliasDirectoryNode.this, getChildCount() );
    		
    		node.init();
    	}
    	
    	sort();
    }

    public String toString()
    {
    	return "Alias Lists [" + this.getChildCount() + "]";
    }

	public JPopupMenu getContextMenu()
	{
		JPopupMenu retVal = new JPopupMenu();

		JMenuItem addItem = new JMenuItem( "Add Alias List" );
		addItem.addActionListener( new ActionListener() 
		{
			@Override
            public void actionPerformed( ActionEvent e )
            {
				AliasList list = new AliasList();
				
				getAliasDirectory().addAliasList( list );
				
				AliasListNode node = new AliasListNode( mPlaylistManager, mSettingsManager, list );
				
				getModel().addNode( node, 
									AliasDirectoryNode.this, 
									AliasDirectoryNode.this.getChildCount() );
				
				sort();
				
				getModel().showNode( node );
            }
		} );
		
		retVal.add( addItem );
		
		return retVal;
	}
}
