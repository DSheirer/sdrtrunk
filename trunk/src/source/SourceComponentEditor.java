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
package source;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import source.config.SourceConfigFactory;
import source.config.SourceConfiguration;
import controller.channel.AbstractChannelEditor;
import controller.channel.ChannelNode;

public class SourceComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;

    private JComboBox<SourceType> mComboSources;
    private SourceEditor mEditor;

    public SourceComponentEditor( ChannelNode channelNode )
	{
    	super( channelNode );
    	
		mComboSources = new JComboBox<SourceType>();
		mComboSources.setModel( new DefaultComboBoxModel<SourceType>( SourceType.values() ) );
		mComboSources.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				SourceType selected = mComboSources.getItemAt( mComboSources.getSelectedIndex() );
				
				if( selected != null )
				{
					SourceConfiguration config;
					
					if( selected == mChannelNode.getChannel()
							.getSourceConfiguration().getSourceType() )
					{
						config = mChannelNode.getChannel()
								.getSourceConfiguration();
					}
					else
					{
						config = SourceConfigFactory
									.getSourceConfiguration( selected );
					}
					
					//Remove the existing one
					if( mEditor != null )
					{
						remove( mEditor );
					}
					
					//Change to the new one
					mEditor = SourceEditorFactory.getPanel( 
						mChannelNode.getModel().getResourceManager(), config );
					
					//Add it to the jpanel
					add( mEditor, "span 2" );
					
					revalidate();
					repaint();
				}
           }
		});
		
		add( mComboSources, "wrap" );

		reset();
	}

	public void reset() 
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable() 
        {

            @Override
            public void run() {
    			mComboSources.setSelectedItem( 
    					mChannelNode.getChannel()
    					.getSourceConfiguration().getSourceType() );

    			mComboSources.requestFocus();

    			mComboSources.requestFocusInWindow();
            }
        });
    }

	@Override
    public void save()
    {
		mEditor.save();
		
		//Let the calling save method send the config change event
		mChannelNode.getChannel()
					.setSourceConfiguration( mEditor.getConfig(), false );
    }
}
