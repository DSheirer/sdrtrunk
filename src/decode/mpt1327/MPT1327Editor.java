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
package decode.mpt1327;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import controller.channel.ChannelMap;
import controller.channel.ChannelNode;
import decode.DecodeEditor;
import decode.config.DecodeConfigMPT1327;
import decode.config.DecodeConfiguration;
import decode.mpt1327.MPT1327Decoder.Sync;

public class MPT1327Editor extends DecodeEditor
{
    private static final long serialVersionUID = 1L;
    
    private JComboBox<ChannelMap> mComboChannelMaps;
    private JComboBox<Sync> mComboSync;
    private ChannelNode mChannelNode;

	public MPT1327Editor( DecodeConfiguration config, ChannelNode channelNode )
	{
		super( config );
		
		mChannelNode = channelNode;
		
		initGUI();
	}
	
	private void initGUI()
	{
		/**
		 * ComboBox: Alias Lists
		 */
		ArrayList<ChannelMap> maps = mChannelNode.getModel().getResourceManager()
			.getPlaylistManager().getPlayist().getChannelMapList().getChannelMap();
		
		ChannelMap[] mapArray = maps.toArray( new ChannelMap[ maps.size() ] );
		
		mComboChannelMaps = new JComboBox<ChannelMap>();
		mComboChannelMaps.setModel( 
				new DefaultComboBoxModel<ChannelMap>( mapArray ) );
		
		add( new JLabel( "Channel Map" ), "wrap" );
		add( mComboChannelMaps, "wrap" );
		
		mComboSync = new JComboBox<Sync>();
		mComboSync.setModel( new DefaultComboBoxModel<Sync>( Sync.values() ) );
		
		add( new JLabel( "Sync" ), "wrap" );
		add( mComboSync, "wrap" );
		
		reset();
	}
	
	@Override
    public void save()
	{
		DecodeConfigMPT1327 config = (DecodeConfigMPT1327)mConfig;
		
		ChannelMap selected = (ChannelMap)mComboChannelMaps.getSelectedItem();
		
		if( selected != null )
		{
			config.setChannelMapName( selected.getName() );
		}
		
		Sync selectedSync = (Sync)mComboSync.getSelectedItem();
		
		if( selectedSync != null )
		{
			config.setSync( selectedSync );
		}
    }

	@Override
    public void reset()
    {
		DecodeConfigMPT1327 config = (DecodeConfigMPT1327)mConfig;
		
		String name = config.getChannelMapName();

		ChannelMap selected = null;

		if( name != null )
    	{
			for( ChannelMap map: mChannelNode.getModel().getResourceManager()
					.getPlaylistManager().getPlayist().getChannelMapList()
						.getChannelMap() )
        	{
				if( map.getName().equalsIgnoreCase( name ) )
        		{
        			selected = map;
        		}
        	}
    	}

		mComboChannelMaps.setSelectedItem( selected );
		mComboChannelMaps.requestFocus();
		mComboChannelMaps.requestFocusInWindow();
		
		mComboSync.setSelectedItem( config.getSync() );
    }
}
