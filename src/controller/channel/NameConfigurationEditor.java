/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package controller.channel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import playlist.PlaylistManager;
import alias.AliasList;

public class NameConfigurationEditor extends ChannelConfigurationEditor
{
	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_NAME = "Please select a channel to view/edit";
	
	private static ComboBoxModel<String> EMPTY_MODEL = new DefaultComboBoxModel<>();

	private ChannelModel mChannelModel;
	private PlaylistManager mPlaylistManager;
	
	private JTextField mChannelName = new JTextField( DEFAULT_NAME );
	private JComboBox<String> mSystemNameCombo = new JComboBox<>( EMPTY_MODEL );
	private JComboBox<String> mSiteNameCombo = new JComboBox<>( EMPTY_MODEL );
	private JComboBox<AliasList> mAliasListCombo;
	
	private Channel mChannel;

	public NameConfigurationEditor( ChannelModel model, 
									PlaylistManager playlistManager )
	{
		mChannelModel = model;
		mPlaylistManager = playlistManager;
		
		setLayout( new MigLayout( "fill,wrap 2", "[right][]", "[][][][][][grow]" ) );

		add( new JLabel( "Name:" ) );
		add( mChannelName, "growx" );
		
		mSystemNameCombo.setEditable( true );
		mSystemNameCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				Object selected = mSystemNameCombo.getSelectedItem();
				
				if( selected != null )
				{
					String system = (String)selected;
					
					List<String> sites = mChannelModel.getSites( system );

					if( !sites.contains( mChannel.getSite() ) )
					{
						sites.add( mChannel.getSite() );
					}
					
					if( sites.isEmpty() )
					{
						mSiteNameCombo.setModel( EMPTY_MODEL );
					}
					else
					{
						mSiteNameCombo.setModel( new DefaultComboBoxModel<String>( 
								sites.toArray( new String[ sites.size() ] ) ) );;
					}
							
					mSiteNameCombo.setSelectedItem( mChannel.getSite() );
				}
			}
		} );

		add( new JLabel( "System:" ) );
		add( mSystemNameCombo, "growx" );
		
		mSiteNameCombo.setEditable( true );
		
		add( new JLabel( "Site:" ) );
		add( mSiteNameCombo, "growx" );
		
		/**
		 * ComboBox: Alias Lists
		 */
		add( new JLabel( "Alias List:" ) );

		mAliasListCombo = new JComboBox<AliasList>();

		List<AliasList> lists = mPlaylistManager.getPlayist()
				.getAliasDirectory().getAliasList();

		Collections.sort( lists );
		
		mAliasListCombo.setModel( new DefaultComboBoxModel<AliasList>( 
				lists.toArray( new AliasList[ lists.size() ] ) ) );
		
		add( mAliasListCombo, "growx" );
		
		add( new JLabel() );
		
		final JButton aliasListButton = new JButton( "New Alias List ..." );
		
		aliasListButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				String name = JOptionPane.showInputDialog( aliasListButton, 
						"Please enter an Alias List name:" );

				if( name != null && !name.isEmpty() )
				{
					AliasList aliasList = new AliasList( name );
					
					mPlaylistManager.getPlayist().getAliasDirectory()
							.addAliasList( aliasList );
					
					mPlaylistManager.save();
					
					List<AliasList> lists = mPlaylistManager.getPlayist()
							.getAliasDirectory().getAliasList();
					
					Collections.sort( lists );

					mAliasListCombo.setModel( new DefaultComboBoxModel<AliasList>( 
							lists.toArray( new AliasList[ lists.size() ] ) ) );
					
					mAliasListCombo.setSelectedItem( aliasList );
				}
			}
		} );
		
		add( aliasListButton, "growx" );
	}
	
	@Override
	public void save()
	{
		if( mChannel != null )
		{
			mChannel.setName( mChannelName.getText() );
			
			Object system = mSystemNameCombo.getSelectedItem();
			mChannel.setSystem( ( system == null ? null : (String)system ) );

			Object site = mSiteNameCombo.getSelectedItem();
			mChannel.setSite( ( site == null ? null : (String)site ) );

			Object aliasList = mAliasListCombo.getSelectedItem();
			mChannel.setAliasListName( aliasList == null ? null : 
							((AliasList)aliasList).getName() );
		}
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
		//System and site can be null, but we must (should?) have a channel name
		String name = mChannelName.getText();
		
		if( name == null || name.isEmpty() )
		{
			throw new ConfigurationValidationException( 
					mChannelName, "Channel name cannot be empty" );
		}
	}

	@Override
	public void setConfiguration( Channel channel )
	{
		mChannel = channel;
		
		if( mChannel != null )
		{
			mChannelName.setText( channel.getName() );

			List<String> systems = mChannelModel.getSystems();
			
			if( systems.isEmpty() )
			{
				mSystemNameCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mSystemNameCombo.setModel( new DefaultComboBoxModel<String>( 
						systems.toArray( new String[ systems.size() ] ) ) );;
			}
			
			mSystemNameCombo.setSelectedItem( channel.getSystem() );

			List<String> sites = mChannelModel.getSites( channel.getSystem() );

			if( sites.isEmpty() )
			{
				mSiteNameCombo.setModel( EMPTY_MODEL );
			}
			else
			{
				mSiteNameCombo.setModel( new DefaultComboBoxModel<String>( 
						sites.toArray( new String[ sites.size() ] ) ) );;
			}
					
			mSiteNameCombo.setSelectedItem( channel.getSite() );
			
			String aliasListName = mChannel.getAliasListName();

		   	if( aliasListName != null )
		   	{
		   		AliasList selected = mPlaylistManager.getPlayist().getAliasDirectory()
		   			.getAliasList( aliasListName );
		
		   		mAliasListCombo.setSelectedItem( selected );
		   	}
		}
		else
		{
			mChannelName.setText( DEFAULT_NAME );
			mSystemNameCombo.setModel( EMPTY_MODEL );
			mSiteNameCombo.setModel( EMPTY_MODEL );
			mAliasListCombo.setSelectedIndex( 0 );
		}
	}
}
