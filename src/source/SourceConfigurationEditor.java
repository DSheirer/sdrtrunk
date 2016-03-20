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

import gui.editor.Editor;
import gui.editor.EmptyEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import source.mixer.MixerSourceEditor;
import source.tuner.TunerSourceEditor;
import controller.channel.Channel;

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
    	mTunerSourceEditor = new TunerSourceEditor();
    	init();
	}

    private void init()
    {
		setLayout( new MigLayout( "fill,wrap 3", "[right][left][grow,fill]", "[][grow]" ) );

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
		
		add( new JLabel( "Source:" ) );
		add( mComboSources );
		
		mCurrentEditor = mTunerSourceEditor;
		add( mCurrentEditor );
    }
    
    private void setEditor( Editor<Channel> editor )
    {
    	if( mCurrentEditor != editor )
		{
    		remove( mCurrentEditor );
    		mCurrentEditor = editor;
    		add( mCurrentEditor );
    		
    		revalidate();
		}
    }
    
	@Override
    public void save()
    {
		if( hasItem() && mCurrentEditor.isModified() )
		{
			mCurrentEditor.save();
		}
    }

	@Override
	public boolean isModified()
	{
		return super.isModified() || mCurrentEditor.isModified();
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
			mMixerSourceEditor.setItem( channel );
			mTunerSourceEditor.setItem( channel );
		}
	}
}
