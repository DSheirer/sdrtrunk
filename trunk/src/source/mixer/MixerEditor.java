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
package source.mixer;

import java.util.List;

import javax.sound.sampled.Mixer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import source.SourceEditor;
import source.config.SourceConfigMixer;
import source.config.SourceConfiguration;
import source.mixer.MixerManager.DiscoveredMixer;
import controller.ResourceManager;

public class MixerEditor extends SourceEditor
{
    private static final long serialVersionUID = 1L;
    private JComboBox<DiscoveredMixer> mComboMixers;
    private JComboBox<MixerChannel>mComboChannels;
    protected Mixer.Info mSelectedMixer = null;
    

	public MixerEditor( ResourceManager resourceManager, 
						SourceConfiguration config )
	{
		super( resourceManager, config );
		
		initGUI();
	}
	
	private void initGUI()
	{
		JLabel mixerLabel = new JLabel( "Mixer:" );
		add( mixerLabel, "align right" );
		
		mComboMixers = new JComboBox<DiscoveredMixer>();

		add( mComboMixers, "wrap" );

		resetMixers();

		JLabel channelLabel = new JLabel( "Channel:" );
		add( channelLabel, "align right" );

		mComboChannels = new JComboBox<MixerChannel>();

		add( mComboChannels, "wrap" );
		
		resetChannels();
	}

	public void reset()
	{
		resetMixers();
		resetChannels();
	}
	
	public void save()
	{
		SourceConfigMixer config = (SourceConfigMixer)mConfig;

		DiscoveredMixer selectedMixer = 
				mComboMixers.getItemAt( mComboMixers.getSelectedIndex() );
		
		if( selectedMixer != null )
		{
			config.setMixer( selectedMixer.getMixerName() );
		}

		MixerChannel channel = mComboChannels.getItemAt( 
				mComboChannels.getSelectedIndex() );
		
		if( channel != null )
		{
			if( config.getChannel() != channel )
			{
				config.setChannel( channel );
			}
		}
	}
	
	private void resetMixers()
	{

		javax.swing.SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
            	DiscoveredMixer[] mixers = MixerManager.getInstance().getMixers();

        		mComboMixers.setModel( 
        				new DefaultComboBoxModel<DiscoveredMixer>( mixers ) );
        		
        		SourceConfigMixer config = (SourceConfigMixer)mConfig;
        		
        		String mixer = config.getMixer();
        		
        		if( mixer != null )
        		{
            		mComboMixers.setSelectedItem( mixer );
        		}
        		else
        		{
        			//Mixer either hasn't been set yet, or it was set to a 
        			//mixer that is no longer available.  Set the mixer to the
        			//first item, and then store the value
        			if( mixers.length > 0 )
        			{
        				mComboMixers.setSelectedIndex( 0 );
        				
        				DiscoveredMixer selectedMixer = 
        						(DiscoveredMixer)mComboMixers.getSelectedItem();
        				
        				config.setMixer( selectedMixer.getMixerName() );
        			}
        		}
            }
        });
	}
	
	private void resetChannels()
	{
        javax.swing.SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
        		mComboChannels.setModel( 
	        				new DefaultComboBoxModel<MixerChannel>( MixerChannel.values() ) );
        		
        		MixerChannel channel = ((SourceConfigMixer)mConfig).getChannel();

        		mComboChannels.setSelectedItem( channel	);
            }
        });
	}
}
