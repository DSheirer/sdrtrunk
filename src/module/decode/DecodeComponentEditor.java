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
package module.decode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import module.decode.config.DecodeConfigFactory;
import module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import playlist.PlaylistManager;
import controller.channel.Channel;
import controller.channel.ChannelConfigurationEditor;
import controller.channel.ConfigurationValidationException;

public class DecodeComponentEditor extends ChannelConfigurationEditor
{
	private final static Logger mLog = LoggerFactory.getLogger( DecodeComponentEditor.class );

	private static final long serialVersionUID = 1L;

    private PlaylistManager mPlaylistManager;
    private JComboBox<DecoderType> mComboDecoders;

    private DecodeEditor mEditor;

    private Channel mChannel;
    
    public DecodeComponentEditor( PlaylistManager playlistManager )
	{
    	mPlaylistManager = playlistManager;
    	
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][grow]" ) );

		mComboDecoders = new JComboBox<DecoderType>();

		DefaultComboBoxModel<DecoderType> model = 
							new DefaultComboBoxModel<DecoderType>();
		
		for( DecoderType type: DecoderType.getPrimaryDecoders() )
		{
			model.addElement( type );
		}
		
		mComboDecoders.setModel( model );

		mComboDecoders.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				DecoderType selected = mComboDecoders
						.getItemAt( mComboDecoders.getSelectedIndex() );
				
				if( selected != null )
				{
					DecodeConfiguration config;

					if( mChannel == null || 
						mChannel.getDecodeConfiguration().getDecoderType() != selected )
					{
						config = DecodeConfigFactory.getDecodeConfiguration( selected );
					}
					else
					{
						config = mChannel.getDecodeConfiguration();
					}
					
					//Remove the existing one
					if( mEditor != null )
					{
						remove( mEditor );
					}
					
					//Change to the new one
					mEditor = DecoderFactory.getEditorPanel( config,
							mChannel, mPlaylistManager );
					
					add( mEditor, "span 2,center" );
					
					revalidate();
					repaint();
				}
           }
		});
		
    	add( new JLabel( "Decoder:" ) );
		add( mComboDecoders, "wrap" );
	}

    public DecodeConfiguration getDecodeConfig()
    {
    	return mEditor.getConfig();
    }

    @Override
    public void save()
    {
    	if( mEditor != null )
    	{
        	mEditor.save();
    	}
    	
	    mChannel.setDecodeConfiguration( mEditor.getConfig() );
    }

//    /**
//     * Validates the editor against the current decode editor 
//     */
//	@Override
//	public void validate( ChannelConfigurationEditor editor ) throws ConfigurationValidationException
//	{
//		super.validate( editor );
//		
//		mEditor.validate( editor );
//	}

	@Override
	public void setConfiguration( Channel channel )
	{
		mChannel = channel;

		if( mChannel != null )
		{
	    	final DecoderType decoder = mChannel.getDecodeConfiguration().getDecoderType();

			mComboDecoders.setSelectedItem( decoder );
			
	        mComboDecoders.requestFocus();

	        mComboDecoders.requestFocusInWindow();
		}
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
		// TODO Auto-generated method stub
	}
}
