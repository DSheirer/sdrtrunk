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
package decode;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import controller.channel.AbstractChannelEditor;
import controller.channel.ChannelNode;
import decode.config.DecodeConfigFactory;
import decode.config.DecodeConfiguration;

public class DecodeComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;

    private JComboBox<DecoderType> mComboDecoders;

    private DecodeEditor mEditor;

    public DecodeComponentEditor( ChannelNode channelNode )
	{
    	super( channelNode );

		/**
		 * ComboBox: Decoders
		 */
		mComboDecoders = new JComboBox<DecoderType>();

		DefaultComboBoxModel<DecoderType> model = 
							new DefaultComboBoxModel<DecoderType>();
		
		for( DecoderType type: DecoderType.getAvailableDecoders() )
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
					
					if( mChannelNode.getChannel()
							.getDecodeConfiguration().getDecoderType() == 
												selected )
					{
						config = mChannelNode.getChannel()
								.getDecodeConfiguration();
					}
					else
					{
						config = DecodeConfigFactory
								.getDecodeConfiguration( selected );
					}
					
					//Remove the existing one
					if( mEditor != null )
					{
						remove( mEditor );
					}
					
					//Change to the new one
					mEditor = DecodeEditorFactory.getPanel( config, mChannelNode );
					
					add( mEditor, "span 2" );
					
					revalidate();
					repaint();
				}
           }
		});
		add( mComboDecoders, "wrap" );
		
		reset();
	}

    public void reset() 
    {
    	final DecoderType decoder = mChannelNode.getChannel()
    			.getDecodeConfiguration().getDecoderType();
    	
        SwingUtilities.invokeLater(new Runnable() 
        {
            @Override
            public void run() {
    			mComboDecoders.setSelectedItem( decoder );
    			
                mComboDecoders.requestFocus();

                mComboDecoders.requestFocusInWindow();
            }
        });
    }

    @Override
    public void save()
    {
    	mEditor.save();
    	
    	//The component that calls save will invoke the change broadcast
	    mChannelNode.getChannel()
	    	.setDecodeConfiguration( mEditor.getConfig(), false );
    }
}
