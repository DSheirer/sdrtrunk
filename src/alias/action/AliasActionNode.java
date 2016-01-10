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
package alias.action;

import playlist.PlaylistManager;
import controller.ConfigurableNode;

public class AliasActionNode extends ConfigurableNode
{
    private static final long serialVersionUID = 1L;
    
    private PlaylistManager mPlaylistManager;

    public AliasActionNode( PlaylistManager playlistManager, AliasAction action )
	{
    	super( playlistManager, action );
    	
    	mPlaylistManager = playlistManager;
	}
    
    public void save()
    {
    	mPlaylistManager.save();
    }
    
    public String getIconPath()
    {
    	return "images/action.png";
    }
}
