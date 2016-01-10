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
package module.decode.mpt1327;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import module.decode.DecodeEditor;
import module.decode.config.DecodeConfiguration;
import module.decode.mpt1327.MPT1327Decoder.Sync;
import playlist.PlaylistManager;
import controller.channel.Channel;
import controller.channel.map.ChannelMap;

public class MPT1327ConfigEditor extends DecodeEditor
{
    private static final long serialVersionUID = 1L;
    
    private JComboBox<ChannelMap> mComboChannelMaps;
    private JComboBox<Sync> mComboSync;
    private JLabel mCallTimeoutLabel;
    private JSlider mCallTimeout;
    private JLabel mTrafficChannelPoolSizeLabel;
    private JSlider mTrafficChannelPoolSize;
    private Channel mChannel;
    private PlaylistManager mPlaylistManager;

	public MPT1327ConfigEditor( DecodeConfiguration config, 
								Channel channel,
								PlaylistManager playlistManager )
	{
		super( config );
		
		mChannel = channel;
		mPlaylistManager = playlistManager;
		
		initGUI();
	}
	
	private DecodeConfigMPT1327 getMPTConfig()
	{
		return (DecodeConfigMPT1327)mConfig;
	}
	
	private void initGUI()
	{
		/**
		 * ComboBox: Alias Lists
		 */
		List<ChannelMap> maps = ( mPlaylistManager == null ? 
				new ArrayList<ChannelMap>() : mPlaylistManager.getPlayist()
				.getChannelMapList().getChannelMap() );
		
		ChannelMap[] mapArray = maps.toArray( new ChannelMap[ maps.size() ] );
		
		mComboChannelMaps = new JComboBox<ChannelMap>();
		mComboChannelMaps.setModel( 
				new DefaultComboBoxModel<ChannelMap>( mapArray ) );
		
		add( new JLabel( "Channel Map:" ) );
		add( mComboChannelMaps, "wrap" );
		
		mComboSync = new JComboBox<Sync>();
		mComboSync.setModel( new DefaultComboBoxModel<Sync>( Sync.values() ) );
		
		add( new JLabel( "Sync:" ) );
		add( mComboSync, "wrap" );
		
		mCallTimeout = new JSlider( JSlider.HORIZONTAL,
							DecodeConfiguration.CALL_TIMEOUT_MINIMUM,
							DecodeConfiguration.CALL_TIMEOUT_MAXIMUM,
							DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS );
		
		mCallTimeout.setMajorTickSpacing( 90 );
		mCallTimeout.setMinorTickSpacing( 10 );
		mCallTimeout.setPaintTicks( true );
		
		mCallTimeout.setLabelTable( mCallTimeout.createStandardLabels( 100, 100 ) );
		mCallTimeout.setPaintLabels( true );

		mCallTimeoutLabel = new JLabel( "Call Timeout: " + mCallTimeout.getValue() + " " );
		
		mCallTimeout.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				mCallTimeoutLabel.setText( "Call Timeout: " + mCallTimeout.getValue() );
			}
		} );

		add( mCallTimeoutLabel );
		add( mCallTimeout, "wrap,grow" );

		mTrafficChannelPoolSize = new JSlider( JSlider.HORIZONTAL,
				DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MINIMUM,
				DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_MAXIMUM,
				DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT );

		mTrafficChannelPoolSize.setMajorTickSpacing( 10 );
		mTrafficChannelPoolSize.setMinorTickSpacing( 1 );
		mTrafficChannelPoolSize.setPaintTicks( true );
		
		mTrafficChannelPoolSize.setLabelTable( mTrafficChannelPoolSize.createStandardLabels( 10, 10 ) );
		mTrafficChannelPoolSize.setPaintLabels( true );
		
		mTrafficChannelPoolSizeLabel = new JLabel( "Traffic Channel Pool: " + mTrafficChannelPoolSize.getValue() + " " );
		
		mTrafficChannelPoolSize.addChangeListener( new ChangeListener()
		{
		@Override
		public void stateChanged( ChangeEvent e )
		{
			mTrafficChannelPoolSizeLabel.setText( "Traffic Channel Pool: " + mTrafficChannelPoolSize.getValue() );
		}
		} );
		
		add( mTrafficChannelPoolSizeLabel );
		add( mTrafficChannelPoolSize, "wrap,grow" );
		
//		reset();
	}
	
	@Override
    public void save()
	{
		DecodeConfigMPT1327 config = getMPTConfig();
		
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
		
		config.setCallTimeout( mCallTimeout.getValue() );
		
		config.setTrafficChannelPoolSize( mTrafficChannelPoolSize.getValue() );
    }

//	@Override
//    public void reset()
//    {
//		DecodeConfigMPT1327 config = (DecodeConfigMPT1327)mConfig;
//		
//		String name = config.getChannelMapName();
//
//		ChannelMap selected = null;
//
//		if( name != null )
//    	{
//			for( ChannelMap map: mPlaylistManager.getPlayist().getChannelMapList()
//						.getChannelMap() )
//        	{
//				if( map.getName().equalsIgnoreCase( name ) )
//        		{
//        			selected = map;
//        		}
//        	}
//    	}
//
//		mComboChannelMaps.setSelectedItem( selected );
//		mComboChannelMaps.requestFocus();
//		mComboChannelMaps.requestFocusInWindow();
//		
//		mComboSync.setSelectedItem( config.getSync() );
//		
//		mCallTimeout.setValue( config.getCallTimeout() );
//		
//		mTrafficChannelPoolSize.setValue( config.getTrafficChannelPoolSize() );
//    }
}
