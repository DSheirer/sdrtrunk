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
package io.github.dsheirer.source;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EmptyEditor;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.mixer.MixerSourceEditor;
import io.github.dsheirer.source.tuner.TunerSourceEditor;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SourceConfigurationEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;

    private JComboBox<SourceType> mComboSources;
    private MixerSourceEditor mMixerSourceEditor;
    private TunerSourceEditor mTunerSourceEditor;
    private Editor<Channel> mCurrentEditor;
    
    public SourceConfigurationEditor( SourceManager sourceManager )
	{
    	mMixerSourceEditor = new MixerSourceEditor( sourceManager );
    	mMixerSourceEditor.setSaveRequestListener( this );
    	
    	mTunerSourceEditor = new TunerSourceEditor();
    	mTunerSourceEditor.setSaveRequestListener( this );
    	
    	init();
	}
    
    public SourceConfiguration getSourceConfiguration()
    {
    	return hasItem() ? getItem().getSourceConfiguration() : null;
    }

    private void init()
    {
		setLayout( new MigLayout( "wrap 2", "[][grow,fill]", "[align top][grow]" ) );

    	mComboSources = new JComboBox<SourceType>();
		mComboSources.setModel( new DefaultComboBoxModel<SourceType>( SourceType.getTypes() ) );
		mComboSources.addActionListener( new ActionListener()
		{
			@Override
           public void actionPerformed( ActionEvent e )
           {
				SourceType selected = (SourceType)mComboSources.getSelectedItem();
				
				switch( selected )
				{
					case MIXER:
						setEditor( mMixerSourceEditor );
						break;
					case TUNER:
						setEditor( mTunerSourceEditor );
						break;
					default:
						setEditor( new EmptyEditor<Channel>() );
						break;
				}
           }
		});
		
		add( mComboSources );
		
		mCurrentEditor = mTunerSourceEditor;
		add( mCurrentEditor );
    }
    
    private void setEditor( Editor<Channel> editor )
    {
    	if( mCurrentEditor != editor )
		{
    		//Set channel to null to force a save prompt as required
    		mCurrentEditor.setItem( null );
    		
    		remove( mCurrentEditor );
    		mCurrentEditor = editor;
    		mCurrentEditor.setSaveRequestListener( this );
    		add( mCurrentEditor );
    		
    		mCurrentEditor.setItem( getItem() );
    		
    		revalidate();
		}
    }
    
	@Override
    public void save()
    {
		if( hasItem() )
		{
			mCurrentEditor.save();
		}
    }

	/**
	 * Sets the channel configuration.  Note: this method must be invoked from
	 * the swing event dispatch thread.
	 */
	@Override
	public void setItem( Channel channel )
	{
		super.setItem( channel );
		
		if( hasItem() )
		{
			mComboSources.setSelectedItem( getItem().getSourceConfiguration().getSourceType() );
			repaint();
			mCurrentEditor.setItem( channel );
		}
	}
}
