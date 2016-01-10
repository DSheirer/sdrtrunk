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

import playlist.PlaylistManager;

public abstract class ConfigurableNode extends BaseNode
{
    private static final long serialVersionUID = 1L;
    
    private PlaylistManager mPlaylistManager;

    public ConfigurableNode( PlaylistManager playlistManager, Object object )
    {
        super( object );
        
        mPlaylistManager = playlistManager;
    }
    
    protected PlaylistManager getPlaylistManager()
    {
    	return mPlaylistManager;
    }
    
    /**
     * Saves changes to the node
     */
    public void save()
    {
    	mPlaylistManager.save();

        BaseNode parent = (BaseNode)getParent();

        if( parent instanceof ConfigurableNode )
        {
        	((ConfigurableNode)parent).sort();
        }
    }
}
