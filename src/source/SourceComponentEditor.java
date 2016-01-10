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
package source;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import source.config.SourceConfigFactory;
import source.config.SourceConfiguration;
import controller.channel.AbstractChannelEditor;
import controller.channel.Channel;
import controller.channel.ConfigurationValidationException;

public class SourceComponentEditor extends AbstractChannelEditor
{
    private static final long serialVersionUID = 1L;

    private JComboBox<SourceType> mComboSources;
    private SourceEditor mEditor;
    private SourceManager mSourceManager;

    public SourceComponentEditor( SourceManager sourceManager, Channel channel )
	{
    	super( channel );
    	
    	mSourceManager = sourceManager;
    	
		setLayout( new MigLayout( "fill,wrap 2", "[right,grow][grow]", "[][][grow]" ) );

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
					
					if( selected == mChannel.getSourceConfiguration().getSourceType() )
					{
						config = mChannel.getSourceConfiguration();
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
					mEditor = mSourceManager.getPanel( config );
					
					//Add it to the jpanel
					add( mEditor, "span 2" );
					
					revalidate();
					repaint();
				}
           }
		});
		
		add( new JLabel( "Source:" ) );
		add( mComboSources, "wrap" );

		reset();
	}
    
    public SourceEditor getSourceEditor()
    {
    	return mEditor;
    }

	public void reset() 
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
    			mComboSources.setSelectedItem( 
    					mChannel.getSourceConfiguration().getSourceType() );

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
		mChannel.setSourceConfiguration( mEditor.getConfig() );
    }

	@Override
	public void setConfiguration( Channel channel )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void validateConfiguration() throws ConfigurationValidationException
	{
		// TODO Auto-generated method stub
		
	}
}
