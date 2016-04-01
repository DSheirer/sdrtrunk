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
package source.mixer;

import gui.editor.Editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import source.SourceManager;
import source.config.SourceConfigMixer;
import source.config.SourceConfiguration;
import source.mixer.MixerManager.InputMixerConfiguration;
import controller.channel.Channel;

public class MixerSourceEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;
    private JComboBox<InputMixerConfiguration> mComboMixers;
    private JComboBox<MixerChannel>mComboChannels;
    
    private SourceManager mSourceManager;

	public MixerSourceEditor( SourceManager sourceManager )
	{
		mSourceManager = sourceManager;
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0,wrap 4", "[right][grow,fill][right][grow,fill]", "" ) );

		mComboMixers = new JComboBox<InputMixerConfiguration>();
    	InputMixerConfiguration[] inputMixers = mSourceManager.getMixerManager().getInputMixers();
		mComboMixers.setModel( new DefaultComboBoxModel<InputMixerConfiguration>( inputMixers ) );
		mComboMixers.setEnabled( false );
		mComboMixers.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				InputMixerConfiguration selected = (InputMixerConfiguration)mComboMixers.getSelectedItem();
				
            	EnumSet<MixerChannel> channels = selected.getChannels();
            	
        		mComboChannels.setModel( new DefaultComboBoxModel<MixerChannel>( 
    				channels.toArray( new MixerChannel[ channels.size() ] ) ) );
        		
        		repaint();
        		
        		setModified( true );
			}
		} );

		add( new JLabel( "Mixer:" ) );
		add( mComboMixers );

		mComboChannels = new JComboBox<MixerChannel>();

		InputMixerConfiguration selected = (InputMixerConfiguration)mComboMixers.getSelectedItem();

		if( selected != null )
		{
	    	EnumSet<MixerChannel> channels = selected.getChannels();
	    	
			mComboChannels.setModel( new DefaultComboBoxModel<MixerChannel>( 
				channels.toArray( new MixerChannel[ channels.size() ] ) ) );
		}
		
		mComboChannels.setEnabled( false );
		mComboChannels.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				setModified( true );
			}
		} );

		add( new JLabel( "Channel:" ) );
		add( mComboChannels );
	}

	@Override
	public void setItem( Channel item )
	{
		super.setItem( item );
		
		if( hasItem() )
		{
			mComboMixers.setEnabled( true );
			mComboChannels.setEnabled( true );
			
			SourceConfiguration config = item.getSourceConfiguration();
			
			if( config instanceof SourceConfigMixer )
			{
				SourceConfigMixer mixer = (SourceConfigMixer)config;

				for( InputMixerConfiguration inputMixer: mSourceManager.getMixerManager().getInputMixers() )
				{
					if( inputMixer.getMixerName().equalsIgnoreCase( mixer.getMixer() ) )
					{
						mComboMixers.setSelectedItem( inputMixer );
						
						for( MixerChannel channel: inputMixer.getChannels() )
						{
							if( channel.getLabel() != null && mixer.getChannel() != null )
							{
								if( channel.getLabel().equalsIgnoreCase( mixer.getChannel().getLabel() ) )
								{
									mComboChannels.setSelectedItem( channel );
								}
							}
						}
						
						continue;
					}
				}
				
				setModified( false );
			}
			else
			{
				setModified( true );
			}
		}
		else
		{
			mComboMixers.setEnabled( false );
			mComboChannels.setEnabled( false );
			
			setModified( false );
		}
	}

	public void save()
	{
		if( hasItem() && isModified() )
		{
			SourceConfigMixer config = new SourceConfigMixer();
			
			InputMixerConfiguration selected = 
					(InputMixerConfiguration)mComboMixers.getSelectedItem();
			config.setMixer( selected.getMixerName() );
			
			MixerChannel channel = (MixerChannel)mComboChannels.getSelectedItem();
			config.setChannel( channel );
			
			getItem().setSourceConfiguration( config );
		}
		
		setModified( false );
	}
}
